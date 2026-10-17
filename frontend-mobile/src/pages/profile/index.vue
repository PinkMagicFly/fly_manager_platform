<template>
  <div class="m-profile">
    <div class="user-card">
      <div class="avatar">{{ user?.loginName?.charAt(0)?.toUpperCase() || 'U' }}</div>
      <div class="u-info">
        <h3>{{ user?.loginName }}</h3>
        <p class="role-badge">{{ roleName }}</p>
      </div>
    </div>

    <div class="list-card">
      <div class="list-item">
        <span class="lbl">绑定手机</span>
        <span class="val">{{ user?.mobile || '138****0000' }}</span>
      </div>
      <div class="list-item">
        <span class="lbl">实名资质状态</span>
        <span class="val text-success">✓ 已实名认证</span>
      </div>
      <div class="list-item">
        <span class="lbl">终端自适应模式</span>
        <span class="val text-primary">移动端弱网轻量 (Client-Kind: app)</span>
      </div>
      <div class="list-item">
        <span class="lbl">弱网离线队列</span>
        <span class="val">0 条待同步</span>
      </div>
    </div>

    <button class="logout-btn" @click="handleLogout">退出当前作业会话</button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const user = ref<any>(null)

const roleName = computed(() => {
  if (user.value?.roles?.includes('ROLE_PILOT')) return '商用飞手机长'
  if (user.value?.roles?.includes('ROLE_MAINTAINER')) return '外场维保专员'
  if (user.value?.roles?.includes('ROLE_MERCHANT')) return '物流商户代表'
  if (user.value?.roles?.includes('ROLE_ADMIN')) return '平台管理员'
  return '货运客户'
})

onMounted(async () => {
  const cached = localStorage.getItem('soarcloud_mobile_user')
  if (cached) {
    user.value = JSON.parse(cached)
  }
  try {
    const fresh: any = await request.get('/api/v1/auth/current-user')
    user.value = fresh
    localStorage.setItem('soarcloud_mobile_user', JSON.stringify(fresh))
  } catch (e) {}
})

const handleLogout = async () => {
  try {
    await request.post('/api/v1/auth/logout')
  } catch (e) {}
  localStorage.removeItem('soarcloud_mobile_token')
  localStorage.removeItem('soarcloud_mobile_user')
  router.push('/login')
}
</script>

<style scoped>
.m-profile {
  padding: 24px 16px;
}
.user-card {
  display: flex;
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
  background-color: #165dff;
  border-radius: 50%;
  font-size: 22px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
}
.u-info h3 {
  font-size: 18px;
  margin-bottom: 4px;
}
.role-badge {
  font-size: 12px;
  background: rgba(255, 255, 255, 0.15);
  padding: 2px 8px;
  border-radius: 4px;
  display: inline-block;
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
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #f1f5f9;
  font-size: 14px;
}
.list-item:last-child {
  border-bottom: none;
}
.lbl {
  color: #64748b;
}
.val {
  font-weight: 500;
  color: #0f172a;
}
.text-success { color: #10b981; }
.text-primary { color: #165dff; }
.logout-btn {
  width: 100%;
  height: 44px;
  background-color: #fee2e2;
  color: #ef4444;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}
</style>
