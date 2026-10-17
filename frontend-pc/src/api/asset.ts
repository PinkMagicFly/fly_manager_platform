import request from '@/utils/request'

/**
 * 机队资产与场站调度 API 模块
 */

// 无人机资产相关
export function getUavList(params?: { merchantId?: number; model?: string; state?: number }): Promise<any> {
  return request({
    url: '/api/v1/uav/list',
    method: 'get',
    params
  })
}

export function getUavDetail(id: number): Promise<any> {
  return request({
    url: `/api/v1/uav/${id}`,
    method: 'get'
  })
}

export function createUav(data: any): Promise<any> {
  return request({
    url: '/api/v1/uav',
    method: 'post',
    data
  })
}

export function updateUav(id: number, data: any): Promise<any> {
  return request({
    url: `/api/v1/uav/${id}`,
    method: 'put',
    data
  })
}

export function changeUavState(id: number, data: { targetState: number; remark?: string }): Promise<any> {
  return request({
    url: `/api/v1/uav/${id}/state`,
    method: 'put',
    data
  })
}

export function reportFault(data: { uavId: number; faultDesc: string }): Promise<any> {
  return request({
    url: '/api/v1/uav/fault',
    method: 'post',
    data
  })
}

export function getMaintainList(params?: { uavId?: number }): Promise<any> {
  return request({
    url: '/api/v1/uav/maintain/list',
    method: 'get',
    params
  })
}

export function acceptMaintain(maintainId: number, data: { repairContent: string; healthScore?: number }): Promise<any> {
  return request({
    url: `/api/v1/uav/maintain/${maintainId}/accept`,
    method: 'put',
    data
  })
}

// 场站与机位预约相关
export function getSiteMap(): Promise<any> {
  return request({
    url: '/api/v1/site/map',
    method: 'get'
  })
}

export function getSiteList(): Promise<any> {
  return request({
    url: '/api/v1/site/list',
    method: 'get'
  })
}

export function getSiteDetail(id: number): Promise<any> {
  return request({
    url: `/api/v1/site/${id}`,
    method: 'get'
  })
}

export function checkBookingConflict(data: { siteId: number; uavId?: number; startTime: string; endTime: string }): Promise<any> {
  return request({
    url: '/api/v1/site/booking/check',
    method: 'post',
    data
  })
}

export function createSiteBooking(data: { siteId: number; uavId: number; applyId?: number; startTime: string; endTime: string }): Promise<any> {
  return request({
    url: '/api/v1/site/booking',
    method: 'post',
    data
  })
}

export function getSiteBookings(id: number, limit = 10): Promise<any> {
  return request({
    url: `/api/v1/site/${id}/bookings`,
    method: 'get',
    params: { limit }
  })
}

// 机组人员资质与排班
export function getCrewStaffList(): Promise<any> {
  return request({
    url: '/api/v1/crew/staff/list',
    method: 'get'
  })
}

export function createCrewStaff(data: any): Promise<any> {
  return request({
    url: '/api/v1/crew/staff',
    method: 'post',
    data
  })
}

export function getWeekSchedule(startDate?: string): Promise<any> {
  return request({
    url: '/api/v1/crew/shift/week',
    method: 'get',
    params: { startDate }
  })
}

export function scheduleShift(data: { crewId: number; shiftDate: string; startTime: string; endTime: string }): Promise<any> {
  return request({
    url: '/api/v1/crew/shift',
    method: 'post',
    data
  })
}

export function cancelShift(shiftId: number): Promise<any> {
  return request({
    url: `/api/v1/crew/shift/${shiftId}`,
    method: 'delete'
  })
}

// 资产效能分析
export function getEfficiencyOverview(merchantId?: number): Promise<any> {
  return request({
    url: '/api/v1/asset/efficiency/overview',
    method: 'get',
    params: { merchantId }
  })
}

