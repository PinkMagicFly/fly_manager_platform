import axios from 'axios'

const request = axios.create({
  baseURL: '',
  timeout: 8000
})

// 移动端请求拦截器：核心注入 Client-Kind: app
request.interceptors.request.use(
  (config) => {
    config.headers['Client-Kind'] = 'app'
    const token = localStorage.getItem('soarcloud_mobile_token')
    if (token) {
      config.headers['satoken'] = token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 移动端响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    } else {
      alert(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('soarcloud_mobile_token')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message || 'Error'))
    }
  },
  (error) => {
    alert(error.response?.data?.message || '网络通讯异常，请稍后重试')
    return Promise.reject(error)
  }
)

export default request
