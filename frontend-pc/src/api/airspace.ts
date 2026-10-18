import request from '@/utils/request'

/**
 * 空域合规与电子围栏服务 API 模块
 *
 * @author SoarCloud Team
 */

// 1. 空域区块态势与网格管理
export function getAirspaceBlocks(): Promise<any> {
  return request({
    url: '/api/v1/airspace/blocks',
    method: 'get'
  })
}

export function validateAirspaceRoute(data: {
  routePoints: number[][]
  altPlanned: number
  startAt?: string
  endAt?: string
}): Promise<any> {
  return request({
    url: '/api/v1/airspace/validate',
    method: 'post',
    data
  })
}

export function planAirspaceRoute(data: {
  startLng: number
  startLat: number
  endLng: number
  endLat: number
  altPlanned?: number
}): Promise<any> {
  return request({
    url: '/api/v1/airspace/plan-route',
    method: 'post',
    data
  })
}


export function saveAirspaceBlock(data: any): Promise<any> {
  return request({
    url: '/api/v1/airspace/blocks/save',
    method: 'post',
    data
  })
}

export function deleteAirspaceBlock(id: number): Promise<any> {
  return request({
    url: `/api/v1/airspace/blocks/delete/${id}`,
    method: 'delete'
  })
}

// 2. 飞行计划申报与审批
export function getFlightApplyList(params?: {
  merchantId?: number
  approveState?: number
}): Promise<any> {
  return request({
    url: '/api/v1/flight-apply/list',
    method: 'get',
    params
  })
}

export function getFlightApplyDetail(id: number): Promise<any> {
  return request({
    url: `/api/v1/flight-apply/detail/${id}`,
    method: 'get'
  })
}

export function createFlightApply(data: any): Promise<any> {
  return request({
    url: '/api/v1/flight-apply/create',
    method: 'post',
    data
  })
}

export function approveFlightApply(data: {
  applyId: number
  approveState: number
  approveNote?: string
  suggestedAlt?: number
}): Promise<any> {
  return request({
    url: '/api/v1/flight-apply/approve',
    method: 'post',
    data
  })
}

export function cancelFlightApply(id: number): Promise<any> {
  return request({
    url: `/api/v1/flight-apply/cancel/${id}`,
    method: 'post'
  })
}

// 3. 电子围栏规则
export function getFenceRules(): Promise<any> {
  return request({
    url: '/api/v1/fence-rule/list',
    method: 'get'
  })
}

export function updateFenceRule(data: any): Promise<any> {
  return request({
    url: '/api/v1/fence-rule/update',
    method: 'put',
    data
  })
}

