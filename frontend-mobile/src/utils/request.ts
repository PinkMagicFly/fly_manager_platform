// ====================================================================
// 翔云·低空智慧物流运营平台 移动作业端 跨端统一请求封装
// 原生支持: Android App / iOS App / 微信小程序 / H5 移动视口
// ====================================================================

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  header?: Record<string, string>
}

export function request<T = any>(options: RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('soarcloud_mobile_token')
    
    // 自适应 baseURL:
    // #ifdef H5
    const baseURL = ''
    // #endif
    // #ifndef H5
    const baseURL = 'http://120.55.114.44'
    // #endif

    const finalUrl = options.url.startsWith('http') ? options.url : `${baseURL}${options.url}`

    uni.request({
      url: finalUrl,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        'Client-Kind': 'app', // 核心注入移动端特征头，触发双端报文裁剪器
        'satoken': token || '',
        ...options.header
      },
      timeout: 8000,
      success: (res: any) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          const body = res.data
          if (body && body.code === 200) {
            resolve(body.data as T)
          } else {
            const errorMsg = body?.message || '请求业务处理失败'
            uni.showToast({
              title: errorMsg,
              icon: 'none',
              duration: 2500
            })
            if (body?.code === 401) {
              uni.removeStorageSync('soarcloud_mobile_token')
              uni.reLaunch({ url: '/pages/login/index' })
            }
            reject(new Error(errorMsg))
          }
        } else {
          const msg = res.data?.message || `网络通讯异常 (HTTP ${res.statusCode})`
          uni.showToast({
            title: msg,
            icon: 'none',
            duration: 2500
          })
          reject(new Error(msg))
        }
      },
      fail: (err) => {
        uni.showToast({
          title: '网络连接超时或不可达',
          icon: 'none',
          duration: 2500
        })
        reject(err)
      }
    })
  })
}

request.get = <T = any>(url: string, data?: any) => request<T>({ url, method: 'GET', data })
request.post = <T = any>(url: string, data?: any) => request<T>({ url, method: 'POST', data })
request.put = <T = any>(url: string, data?: any) => request<T>({ url, method: 'PUT', data })
request.delete = <T = any>(url: string, data?: any) => request<T>({ url, method: 'DELETE', data })

export default request

