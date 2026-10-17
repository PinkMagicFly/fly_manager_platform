<template>
  <view class="m-work-container">
    <view class="top-bar">
      <view class="brand-text">
        <text class="title">{{ currentRoleTitle }}工作台</text>
        <text class="env-tag">Client-Kind: app</text>
      </view>
      <view class="logout-btn" @click="handleLogout">退出</view>
    </view>

    <!-- 1. 角色身份主卡片 -->
    <view class="hero-card" :class="'theme-' + currentRoleKey">
      <view class="hero-top">
        <view class="avatar-circle">{{ user?.loginName?.charAt(0)?.toUpperCase() || 'U' }}</view>
        <view class="user-meta">
          <view class="name-row">
            <text class="user-name">{{ user?.loginName }}</text>
            <text class="role-badge">{{ currentRoleTitle }}</text>
          </view>
          <text class="role-desc">{{ currentRoleSubtitle }}</text>
        </view>
      </view>

      <view class="hero-stats">
        <view v-for="(stat, idx) in roleStats" :key="idx" class="stat-col">
          <text class="stat-val">{{ stat.value }}</text>
          <text class="stat-lbl">{{ stat.label }}</text>
        </view>
      </view>
    </view>

    <!-- 2. 【机长视角 (ROLE_PILOT)】 -->
    <view v-if="isPilot" class="role-view-block">
      <view class="section-title">机长外场执飞操作中枢</view>
      <view class="action-grid">
        <view class="action-card bg-emerald" @click="navTo('/pages/maintain/inspect')">
          <text class="icon">📋</text>
          <text class="act-name">航前安全巡检打卡</text>
          <text class="act-tip">自检合格放行并落库</text>
        </view>
        <view class="action-card bg-rose" @click="navTo('/pages/maintain/report')">
          <text class="icon">⚠️</text>
          <text class="act-name">在途突发故障上报</text>
          <text class="act-tip">紧急提报并置为故障</text>
        </view>
        <view class="action-card bg-sky" @click="navTo('/pages/order/list')">
          <text class="icon">📦</text>
          <text class="act-name">执飞运单管理</text>
          <text class="act-tip">起飞与送达签收确认</text>
        </view>
      </view>

      <view class="content-card">
        <view class="card-head">
          <text class="head-title">执飞无人机五态监控</text>
          <text class="head-link" @click="fetchUavs">刷新</text>
        </view>
        <view v-for="uav in uavList" :key="uav.uavId" class="uav-item">
          <view class="uav-header">
            <text class="sn">{{ uav.uavSn }}</text>
            <text class="state-pill" :class="'state-' + uav.state">{{ uav.stateDesc }}</text>
          </view>
          <view class="model">{{ uav.model }}</view>
          <view class="metrics">
            <text class="metric">电量: {{ uav.batteryPct }}%</text>
            <text class="metric">健康分: {{ uav.healthScore }}分</text>
            <text class="metric">飞行: {{ uav.flyHours }}h</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 3. 【运维视角 (ROLE_MAINTAINER)】 -->
    <view v-else-if="isMaintainer" class="role-view-block">
      <view class="section-title">场站维保与设备检修工作台</view>
      <view class="action-grid">
        <view class="action-card bg-amber" @click="navTo('/pages/maintain/inspect')">
          <text class="icon">🔍</text>
          <text class="act-name">场站例行巡检打卡</text>
          <text class="act-tip">起降场设施例行点检</text>
        </view>
        <view class="action-card bg-rose" @click="navTo('/pages/maintain/report')">
          <text class="icon">🛠️</text>
          <text class="act-name">设备抢修现场登记</text>
          <text class="act-tip">故障提报与维修登记</text>
        </view>
      </view>

      <view class="content-card">
        <view class="card-head">
          <text class="head-title">维保工单池 (支持现场验收放行)</text>
          <text class="head-link" @click="fetchMaintains">刷新</text>
        </view>
        <view v-if="maintainList.length === 0" class="empty-hint">暂无待处理工单</view>
        <view v-for="m in maintainList" :key="m.maintainId" class="maintain-item">
          <view class="m-top">
            <text class="m-type-tag" :class="m.maintainType === 1 ? 'type-inspect' : 'type-fault'">
              {{ m.maintainType === 1 ? '定期巡检' : '故障抢修' }} #{{ m.maintainId }}
            </text>
            <text class="m-state" :class="m.maintainState === 2 ? 'text-success' : 'text-danger'">
              {{ m.maintainState === 2 ? '已验收合格' : '待检修处理' }}
            </text>
          </view>
          <text class="m-desc">{{ m.faultDesc || '例行自检巡检' }}</text>
          <view v-if="m.maintainState !== 2" class="m-act-bar">
            <button class="accept-btn" size="mini" @click="handleAcceptMaintain(m.maintainId)">现场检修合格并放行</button>
          </view>
        </view>
      </view>
    </view>

    <!-- 4. 【空管视角 (ROLE_ATC)】 -->
    <view v-else-if="isAtc" class="role-view-block">
      <view class="section-title">空管合规在线监管工作台</view>
      <view class="action-grid">
        <view class="action-card bg-emerald" @click="navTo('/pages/airspace/approval')">
          <text class="icon">✈️</text>
          <text class="act-name">飞行计划在线审批</text>
          <text class="act-tip">快速核准放行与时段锁定</text>
        </view>
      </view>
      <view class="content-card">
        <view class="card-head"><text class="head-title">在册机队与空域运行态势</text></view>
        <view v-for="uav in uavList" :key="uav.uavId" class="uav-item">
          <view class="uav-header">
            <text class="sn">{{ uav.uavSn }}</text>
            <text class="state-pill" :class="'state-' + uav.state">{{ uav.stateDesc }}</text>
          </view>
          <view class="model">{{ uav.model }}</view>
          <view class="metrics">
            <text class="metric">电量: {{ uav.batteryPct }}%</text>
            <text class="metric">健康分: {{ uav.healthScore }}分</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 4. 【客户/商户通用视角】 -->
    <view v-else class="role-view-block">
      <view class="section-title">{{ isCustomer ? '企业低空寄件与信用授信' : '商户机队运营概览' }}</view>
      <view class="action-grid" v-if="isCustomer">
        <view class="action-card bg-sky" @click="navTo('/pages/order/create')">
          <text class="icon">🚀</text>
          <text class="act-name">在线寄件下单</text>
          <text class="act-tip">即时试算与模拟支付</text>
        </view>
        <view class="action-card bg-emerald" @click="navTo('/pages/order/list')">
          <text class="icon">📦</text>
          <text class="act-name">我的物流运单</text>
          <text class="act-tip">查看在途轨迹与签收</text>
        </view>
      </view>

      <view class="content-card">
        <view class="card-head"><text class="head-title">在册机队五态监控台</text></view>
        <view v-for="uav in uavList" :key="uav.uavId" class="uav-item">
          <view class="uav-header">
            <text class="sn">{{ uav.uavSn }}</text>
            <text class="state-pill" :class="'state-' + uav.state">{{ uav.stateDesc }}</text>
          </view>
          <view class="model">{{ uav.model }}</view>
          <view class="metrics">
            <text class="metric">电量: {{ uav.batteryPct }}%</text>
            <text class="metric">健康分: {{ uav.healthScore }}分</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import request from '../../utils/request'

const user = ref<any>(null)
const uavList = ref<any[]>([])
const maintainList = ref<any[]>([])

const isPilot = computed(() => user.value?.roles?.includes('ROLE_PILOT'))
const isMaintainer = computed(() => user.value?.roles?.includes('ROLE_MAINTAINER'))
const isAtc = computed(() => user.value?.roles?.includes('ROLE_ATC') || user.value?.accountKind === 4)
const isCustomer = computed(() => user.value?.roles?.includes('ROLE_CUSTOMER') || user.value?.accountKind === 1)

const currentRoleKey = computed(() => {
  if (isPilot.value) return 'pilot'
  if (isMaintainer.value) return 'maintainer'
  if (isAtc.value) return 'atc'
  if (isCustomer.value) return 'customer'
  return 'merchant'
})

const currentRoleTitle = computed(() => {
  if (isPilot.value) return '商用飞手机长'
  if (isMaintainer.value) return '外场维保专员'
  if (isAtc.value) return '空管监察员'
  if (isCustomer.value) return '企业货运客户'
  return '低空物流商户'
})

const currentRoleSubtitle = computed(() => {
  if (isPilot.value) return '执照 CAAC-20210988 (有效期至 2028-12-31)'
  if (isMaintainer.value) return '常驻基地：南山科技园低空综合起降枢纽'
  if (isAtc.value) return '所属机构：民航深圳空管综合监察支队'
  if (isCustomer.value) return '签约单位：腾讯科技（深圳）低空物资部'
  return '企业名称：深圳市丰翼低空物流科技有限公司'
})

const roleStats = computed(() => {
  if (isAtc.value) {
    return [
      { label: '空域管制区', value: '3处' },
      { label: '今日申报', value: '2单' },
      { label: '违规案件', value: '1起' }
    ]
  }
  if (isPilot.value) {
    return [
      { label: '安全执飞', value: '480h' },
      { label: '执照状态', value: '正常' },
      { label: '在飞任务', value: '1单' }
    ]
  }
  if (isMaintainer.value) {
    const pending = maintainList.value.filter((m) => m.maintainState !== 2).length
    return [
      { label: '待修工单', value: `${pending}单` },
      { label: '今日巡检', value: '4架' },
      { label: '换电储备', value: '12组' }
    ]
  }
  if (isCustomer.value) {
    return [
      { label: '信用额度', value: '¥50,000' },
      { label: '在途订单', value: '1单' },
      { label: '累计签收', value: '28单' }
    ]
  }
  return [
    { label: '在册机队', value: `${uavList.value.length}架` },
    { label: '稼动率', value: '75%' },
    { label: '起降枢纽', value: '4处' }
  ]
})

const updateNavigationBarTitle = () => {
  let title = '移动作业端'
  if (isPilot.value) title = '外场机长执飞中枢'
  else if (isMaintainer.value) title = '场站设备维保工作台'
  else if (isCustomer.value) title = '低空即时寄件中心'
  else title = '商户机队运营总览'
  uni.setNavigationBarTitle({ title })
}

const fetchUser = async () => {
  const cached = uni.getStorageSync('soarcloud_mobile_user')
  if (cached) {
    try {
      user.value = JSON.parse(cached)
      updateNavigationBarTitle()
    } catch (e) {}
  }
  try {
    const fresh: any = await request.get('/api/v1/auth/current-user')
    user.value = fresh
    uni.setStorageSync('soarcloud_mobile_user', JSON.stringify(fresh))
    updateNavigationBarTitle()
  } catch (err) {}
}

const fetchUavs = async () => {
  try {
    const res: any = await request.get('/api/v1/uav/list')
    uavList.value = res || []
  } catch (err) {}
}

const fetchMaintains = async () => {
  try {
    const res: any = await request.get('/api/v1/uav/maintain/list')
    maintainList.value = res || []
  } catch (err) {}
}

const handleAcceptMaintain = async (maintainId: number) => {
  try {
    await request.put(`/api/v1/uav/maintain/${maintainId}/accept`, {
      repairContent: '外场运维专员现场检修合格并放行'
    })
    uni.showToast({ title: '已验收合格放行', icon: 'success' })
    fetchMaintains()
    fetchUavs()
  } catch (err: any) {
    uni.showToast({ title: err.message || '操作失败', icon: 'none' })
  }
}

const navTo = (url: string) => {
  uni.navigateTo({
    url,
    fail: () => {
      uni.reLaunch({ url })
    }
  })
}

const handleLogout = async () => {
  try { await request.post('/api/v1/auth/logout') } catch (e) {}
  uni.removeStorageSync('soarcloud_mobile_token')
  uni.removeStorageSync('soarcloud_mobile_user')
  uni.showToast({ title: '已退出登录', icon: 'none' })
  setTimeout(() => {
    uni.reLaunch({ url: '/pages/login/index' })
  }, 400)
}

onShow(() => {
  fetchUser()
  fetchUavs()
  fetchMaintains()
})
</script>

<style scoped>
.m-work-container {
  padding: 14px;
  background-color: #f1f5f9;
  min-height: 100vh;
}
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.brand-text .title {
  font-size: 16px;
  font-weight: bold;
  color: #0f172a;
  display: block;
}
.env-tag {
  font-size: 10px;
  color: #64748b;
  display: block;
}
.logout-btn {
  font-size: 12px;
  color: #ef4444;
  padding: 4px 8px;
  border-radius: 4px;
  background-color: #fee2e2;
}

.hero-card {
  border-radius: 12px;
  padding: 16px;
  color: #ffffff;
  margin-bottom: 14px;
}
.theme-pilot { background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%); }
.theme-maintainer { background: linear-gradient(135deg, #d97706 0%, #b45309 100%); }
.theme-customer { background: linear-gradient(135deg, #059669 0%, #047857 100%); }
.theme-merchant { background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%); }

.hero-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.avatar-circle {
  width: 44px;
  height: 44px;
  border-radius: 22px;
  background-color: rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
}
.user-meta .name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.user-name {
  font-size: 16px;
  font-weight: bold;
}
.role-badge {
  font-size: 10px;
  background-color: rgba(255, 255, 255, 0.3);
  padding: 2px 6px;
  border-radius: 4px;
}
.role-desc {
  font-size: 11px;
  opacity: 0.85;
  display: block;
  margin-top: 2px;
}

.hero-stats {
  display: flex;
  justify-content: space-around;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  padding-top: 10px;
}
.stat-col {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.stat-val {
  font-size: 16px;
  font-weight: bold;
}
.stat-lbl {
  font-size: 10px;
  opacity: 0.8;
}

.section-title {
  font-size: 14px;
  font-weight: bold;
  color: #334155;
  margin-bottom: 8px;
}

.action-grid {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}
.action-card {
  flex: 1;
  padding: 14px 12px;
  border-radius: 10px;
  color: #fff;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
  transition: transform 0.15s ease, opacity 0.15s ease;
}
.action-card:active {
  transform: scale(0.98);
  opacity: 0.9;
}
.bg-sky { background: linear-gradient(135deg, #0284c7, #0369a1); }
.bg-emerald { background: linear-gradient(135deg, #10b981, #059669); }
.bg-amber { background: linear-gradient(135deg, #f59e0b, #d97706); }
.bg-rose { background: linear-gradient(135deg, #ef4444, #dc2626); }
.action-card .icon { font-size: 20px; margin-bottom: 4px; }
.act-name { font-size: 13px; font-weight: bold; }
.act-tip { font-size: 10px; opacity: 0.85; margin-top: 2px; }

.content-card {
  background-color: #fff;
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 12px;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.head-title { font-size: 13px; font-weight: bold; color: #0f172a; }
.head-link { font-size: 11px; color: #0284c7; }

.uav-item {
  border-bottom: 1px solid #f8fafc;
  padding: 10px 0;
}
.uav-item:last-child { border-bottom: none; }
.uav-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.sn { font-family: monospace; font-weight: bold; font-size: 13px; color: #0284c7; }
.state-pill {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 8px;
  color: white;
}
.state-0 { background-color: #10b981; }
.state-1 { background-color: #3b82f6; }
.state-2 { background-color: #f59e0b; }
.state-3 { background-color: #ef4444; }
.state-4 { background-color: #94a3b8; }
.model { font-size: 12px; color: #475569; margin: 2px 0; }
.metrics { display: flex; gap: 10px; font-size: 11px; color: #64748b; }

.maintain-item {
  border-bottom: 1px solid #f1f5f9;
  padding: 10px 0;
}
.maintain-item:last-child { border-bottom: none; }
.m-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.m-type-tag { font-size: 11px; font-weight: bold; }
.type-inspect { color: #0284c7; }
.type-fault { color: #ef4444; }
.m-state { font-size: 11px; }
.text-success { color: #10b981; }
.text-danger { color: #ef4444; }
.m-desc { font-size: 12px; color: #334155; }
.m-act-bar { margin-top: 6px; display: flex; justify-content: flex-end; }
.accept-btn {
  background-color: #10b981;
  color: #fff;
  border-radius: 4px;
  font-size: 11px;
}
.empty-hint {
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
  padding: 16px 0;
}
</style>
