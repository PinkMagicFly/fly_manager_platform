<template>
  <view class="m-profile">
    <view class="user-card">
      <view class="avatar">{{ user?.loginName?.charAt(0)?.toUpperCase() || 'U' }}</view>
      <view class="u-info">
        <text class="u-name">{{ user?.loginName }}</text>
        <text class="role-badge">{{ roleName }}</text>
      </view>
    </view>

    <view class="list-card">
      <view class="list-item" @click="navToWork" style="cursor: pointer;">
        <text class="lbl" style="color: #0284c7; font-weight: bold;">👉 进入外场机长作业工作台</text>
        <text class="val text-primary">点击进入 ›</text>
      </view>
      <view class="list-item" @click="navToAddress" style="cursor: pointer;">
        <text class="lbl" style="color: #0284c7; font-weight: bold;">📍 常用寄收地址簿</text>
        <text class="val text-primary">管理常用地址 ›</text>
      </view>
      <view class="list-item" @click="navToTicket" style="cursor: pointer;">
        <text class="lbl" style="color: #0284c7; font-weight: bold;">🎧 客户客服与诉求工单</text>
        <text class="val text-primary">发起咨询/投诉 ›</text>
      </view>

      <view class="list-item">
        <text class="lbl">绑定手机</text>
        <text class="val">{{ user?.mobile || '138****0000' }}</text>
      </view>
      <view class="list-item">
        <text class="lbl">实名资质状态</text>
        <text class="val text-success">✓ 已实名认证</text>
      </view>
      <view class="list-item">
        <text class="lbl">终端自适应模式</text>
        <text class="val text-primary">UniApp 轻量 (Client-Kind: app)</text>
      </view>
      <view class="list-item">
        <text class="lbl">弱网离线暂存队列</text>
        <text class="val">0 条待同步</text>
      </view>
    </view>

    <button class="logout-btn" @click="handleLogout">退出当前作业会话</button>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '../../utils/request'

const user = ref<any>(null)

const roleName = computed(() => {
  if (user.value?.roles?.includes('ROLE_PILOT')) return '商用飞手机长'
  if (user.value?.roles?.includes('ROLE_MAINTAINER')) return '外场维保专员'
  if (user.value?.roles?.includes('ROLE_MERCHANT')) return '物流商户代表'
  if (user.value?.roles?.includes('ROLE_ADMIN')) return '平台管理员'
  return '货运客户'
})

onLoad(async () => {
  const cached = uni.getStorageSync('soarcloud_mobile_user')
  if (cached) {
    try {
      user.value = JSON.parse(cached)
    } catch (e) {}
  }
  try {
    const fresh: any = await request.get('/api/v1/auth/current-user')
    user.value = fresh
    uni.setStorageSync('soarcloud_mobile_user', JSON.stringify(fresh))
  } catch (e) {}
})

const navToWork = () => {
  uni.navigateTo({
    url: '/pages/work/index'
  })
}

const navToAddress = () => {
  uni.navigateTo({
    url: '/pages/address/list'
  })
}

const navToTicket = () => {
  uni.navigateTo({
    url: '/pages/ticket/mine'
  })
}

const handleLogout = async () => {
  try {
    await request.post('/api/v1/auth/logout')
  } catch (e) {}
  uni.removeStorageSync('soarcloud_mobile_token')
  uni.removeStorageSync('soarcloud_mobile_user')
  uni.showToast({
    title: '已退出登录',
    icon: 'none'
  })
  setTimeout(() => {
    uni.reLaunch({
      url: '/pages/login/index'
    })
  }, 400)
}
</script>

<style scoped>
.m-profile {
  padding: 24px 16px;
}
.user-card {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 16px;
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
  color: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
}
.avatar {
  width: 54px;
  height: 54px;
  line-height: 54px;
  text-align: center;
  background-color: #165dff;
  border-radius: 50%;
  font-size: 22px;
  font-weight: bold;
  color: #fff;
}
.u-info {
  display: flex;
  flex-direction: column;
}
.u-name {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
  margin-bottom: 4px;
}
.role-badge {
  font-size: 12px;
  background: rgba(255, 255, 255, 0.15);
  color: #93c5fd;
  padding: 2px 8px;
  border-radius: 4px;
  align-self: flex-start;
}
.list-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 24px;
}
.list-item {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f1f5f9;
}
.lbl {
  font-size: 14px;
  color: #64748b;
}
.val {
  font-size: 14px;
  color: #0f172a;
  font-weight: 500;
}
.text-success {
  color: #10b981;
}
.text-primary {
  color: #165dff;
}
.logout-btn {
  width: 100%;
  height: 44px;
  line-height: 44px;
  background-color: #fee2e2;
  color: #ef4444;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
}
</style>

