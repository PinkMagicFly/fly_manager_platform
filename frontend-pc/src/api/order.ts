import request from '@/utils/request'

/** 运费前置多维阶梯试算 */
export function calculateFee(data: any): Promise<any> {
  return request({
    url: '/api/v1/order/calculate-fee',
    method: 'post',
    data
  })
}

/** 在线下单创建 */
export function createOrder(data: any): Promise<any> {
  return request({
    url: '/api/v1/order/create',
    method: 'post',
    data
  })
}

/** 模拟在线支付 (10 -> 20) */
export function payOrder(id: number | string): Promise<any> {
  return request({
    url: `/api/v1/order/${id}/pay`,
    method: 'post'
  })
}

/** 商户接单 (20 -> 30) */
export function acceptOrder(id: number | string): Promise<any> {
  return request({
    url: `/api/v1/order/${id}/accept`,
    method: 'post'
  })
}

/** 商户批量接单 */
export function batchAcceptOrders(ids: (number | string)[]): Promise<any> {
  return request({
    url: '/api/v1/order/batch-accept',
    method: 'post',
    data: ids
  })
}

/** 获取智能调度推荐候选无人机与飞手 */
export function getRecommendDispatch(id: number | string): Promise<any> {
  return request({
    url: `/api/v1/order/${id}/recommend-dispatch`,
    method: 'get'
  })
}

/** 智能调度派单确认 (30 -> 40，自动联动申报单) */
export function dispatchOrder(data: any): Promise<any> {
  return request({
    url: '/api/v1/order/dispatch',
    method: 'post',
    data
  })
}

/** 机长确认起飞 (40 -> 50) */
export function takeoffOrder(id: number | string): Promise<any> {
  return request({
    url: `/api/v1/order/${id}/takeoff`,
    method: 'post'
  })
}

/** 送达签收并自动出账 (50 -> 60) */
export function deliverOrder(id: number | string): Promise<any> {
  return request({
    url: `/api/v1/order/${id}/deliver`,
    method: 'post'
  })
}

/** 取消运单 */

/** 商户拒单 (20 -> 70，原因必填) */
export function rejectOrder(id: number | string, reason: string): Promise<any> {
  return request({
    url: `/api/v1/order/${id}/reject`,
    method: 'post',
    params: { reason }
  })
}

export function cancelOrder(id: number | string, reason?: string): Promise<any> {
  return request({
    url: `/api/v1/order/${id}/cancel${reason ? `?reason=${encodeURIComponent(reason)}` : ''}`,
    method: 'post'
  })
}

/** 异常中断 */
export function abortOrder(id: number | string, reason?: string): Promise<any> {
  return request({
    url: `/api/v1/order/${id}/abort${reason ? `?reason=${encodeURIComponent(reason)}` : ''}`,
    method: 'post'
  })
}

/** 查询订单全景详情 (四合一视图) */
export function getOrderDetail(id: number | string): Promise<any> {
  return request({
    url: `/api/v1/order/${id}`,
    method: 'get'
  })
}

/** 查询订单飞行航线与实际航迹全景回放 */
export function getOrderTrace(id: number | string): Promise<any> {
  return request({
    url: `/api/v1/order/${id}/trace`,
    method: 'get'
  })
}

/** 分页条件检索运单列表 */
export function getOrderPage(params: any): Promise<any> {
  return request({
    url: '/api/v1/order/page',
    method: 'get',
    params
  })
}

/** 订单看板九态统计分布 */
export function getOrderBoardStats(merchantId?: number | string): Promise<any> {
  return request({
    url: '/api/v1/order/board-stats',
    method: 'get',
    params: { merchantId }
  })
}

/** 分页检索对账单列表 */
export function getBillPage(params: any): Promise<any> {
  return request({
    url: '/api/v1/order/bill/page',
    method: 'get',
    params
  })
}

/** 查询对账单详情 */
export function getBillDetail(id: number | string): Promise<any> {
  return request({
    url: `/api/v1/order/bill/${id}`,
    method: 'get'
  })
}

/** 商户确认对账 */
export function confirmBill(id: number | string): Promise<any> {
  return request({
    url: `/api/v1/order/bill/${id}/confirm`,
    method: 'post'
  })
}

/** 申请开具发票 */
export function applyInvoice(data: any): Promise<any> {
  return request({
    url: '/api/v1/order/bill/invoice',
    method: 'post',
    data
  })
}
