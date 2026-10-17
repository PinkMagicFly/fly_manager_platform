import axios, { AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

// 创建统一的 Axios 实例
const request = axios.create({
  baseURL: '',
  timeout: 10000
})

// 请求拦截器：注入 Client-Kind 与 satoken
request.interceptors.request.use(
  (config) => {
    // 强制声明 PC 端身份，供服务端实现报文白名单自适应
    config.headers['Client-Kind'] = 'pc'
    const token = localStorage.getItem('soarcloud_token')
    if (token) {
      config.headers['satoken'] = token
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器：统一出参解析与错误弹窗
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    // 如果返回的是二进制流文件，直接透传
    if (response.config.responseType === 'blob') {
      return response
    }
    // 标准 Result 出参校验
    if (res.code === 200) {
      return res.data
    } else {
      ElMessage.error(res.message || '业务执行异常')
      // 401 凭证失效统一登出
      if (res.code === 401) {
        localStorage.removeItem('soarcloud_token')
        localStorage.removeItem('soarcloud_user')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message || 'Error'))
    }
  },
  (error) => {
    const msg = error.response?.data?.message || error.message || '网络通讯异常，请稍后重试'
    ElMessage.error(msg)
    if (error.response?.status === 401) {
      localStorage.removeItem('soarcloud_token')
      localStorage.removeItem('soarcloud_user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default request
