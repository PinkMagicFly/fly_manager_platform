<template>
  <el-container class="layout-container">
    <!-- 移动端顶部跳转原生App提示条 -->
    <div v-if="isMobile" class="mobile-top-banner" @click="goToMobile">
      <span>📱 检测到手机浏览器访问，点击进入专属【移动作业端】 ›</span>
    </div>

    <!-- 侧边导航栏 (移动端自动折叠为图标，保留主区域宽度) -->
    <el-aside :width="isMobile ? '64px' : '240px'" class="aside-menu">
      <div class="logo-box">
        <el-icon class="logo-icon"><Position /></el-icon>
        <div v-if="!isMobile" class="logo-title">
          <span class="main-text">翔云·低空运营</span>
          <span class="sub-text">智慧物流中枢</span>
        </div>
      </div>
      <el-menu
        :default-active="activePath"
        router
        class="menu-vertical"
        background-color="#0f172a"
        text-color="#94a3b8"
        active-text-color="#ffffff"
        :collapse="isMobile"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title><span>角色工作台</span></template>
        </el-menu-item>

        <!-- 1. 超级管理员专属：资质审核与系统审计 -->
        <el-sub-menu index="admin" v-if="isAdmin">
          <template #title>
            <el-icon><Stamp /></el-icon>
            <span>资质与实名审核</span>
          </template>
          <el-menu-item index="/admin/merchant-audit">商户资质审核台</el-menu-item>
          <el-menu-item index="/admin/customer-audit">客户实名认证台</el-menu-item>
          <el-menu-item index="/admin/audit-logs">操作审计日志</el-menu-item>
        </el-sub-menu>

        <!-- 2. 商户/管理员专属：企业子账号组织管理 -->
        <el-sub-menu index="merchant" v-if="isMerchant || isAdmin">
          <template #title>
            <el-icon><OfficeBuilding /></el-icon>
            <span>企业组织管理</span>
          </template>
          <el-menu-item index="/merchant/sub-accounts">子账户与权限设置</el-menu-item>
        </el-sub-menu>

        <!-- 3. 机队资产中心 (商户/运维/管理员可见) -->
        <el-sub-menu index="asset" v-if="isMerchant || isAdmin || isAtc">
          <template #title>
            <el-icon><Van /></el-icon>
            <span>机队资产中心</span>
          </template>
          <el-menu-item index="/asset/uav" v-if="isMerchant || isAdmin">机队资产台账</el-menu-item>
          <el-menu-item index="/asset/site">场站一张图</el-menu-item>
          <el-menu-item index="/asset/crew" v-if="isMerchant || isAdmin">机组排班矩阵</el-menu-item>
          <el-menu-item index="/asset/efficiency" v-if="isMerchant || isAdmin">资产效能分析</el-menu-item>
        </el-sub-menu>

        <!-- 4. 空管/超管专属：空域合规监管工作台 (无申报向导) -->
        <el-sub-menu index="airspace-atc" v-if="isAtc || isAdmin">
          <template #title>
            <el-icon><Compass /></el-icon>
            <span>空域合规监管</span>
          </template>
          <el-menu-item index="/airspace/situation">空域三区态势图</el-menu-item>
          <el-menu-item index="/airspace/approval">飞行计划审批台</el-menu-item>
        </el-sub-menu>

        <!-- 5. 商户专属：飞行申报与航线规划中枢 -->
        <el-sub-menu index="airspace-merchant" v-if="isMerchant && !isAtc && !isAdmin">
          <template #title>
            <el-icon><Guide /></el-icon>
            <span>空域与航线申报</span>
          </template>
          <el-menu-item index="/airspace/situation">空域管制态势图</el-menu-item>
          <el-menu-item index="/airspace/apply-wizard">飞行计划申报向导</el-menu-item>
          <el-menu-item index="/airspace/approval">飞行申报进度台账</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <!-- 右侧主体内容 -->
    <el-container class="right-container">
      <!-- 顶部 Header -->
      <el-header height="60px" class="header-bar">
        <div class="header-left">
          <span class="current-title">{{ currentRouteTitle }}</span>
        </div>
        <div class="header-right">
          <el-tag :type="roleTagType" effect="dark" class="role-tag">
            {{ roleText }}
          </el-tag>
          <span class="user-name">{{ userStore.userInfo?.loginName }}</span>
          <span v-if="!isMobile" class="user-mobile">({{ userStore.userInfo?.mobile }})</span>
          <el-button type="danger" link @click="handleLogout" class="logout-btn">
            <el-icon><SwitchButton /></el-icon> <span v-if="!isMobile">退出</span>
          </el-button>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox, ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isMobile = ref(false)
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

const goToMobile = () => {
  window.location.href = '/mobile/'
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

const activePath = computed(() => route.path)
const currentRouteTitle = computed(() => (route.meta.title as string) || '工作台')

const isAdmin = computed(() => userStore.userInfo?.roles.includes('ROLE_ADMIN'))
const isMerchant = computed(() => userStore.userInfo?.roles.includes('ROLE_MERCHANT') || userStore.userInfo?.accountKind === 2)
const isAtc = computed(() => userStore.userInfo?.roles.includes('ROLE_ATC') || userStore.userInfo?.accountKind === 4)
const isPilot = computed(() => userStore.userInfo?.roles.includes('ROLE_PILOT') || userStore.userInfo?.accountKind === 5)

const roleText = computed(() => {
  if (userStore.userInfo?.roles.includes('ROLE_ADMIN')) return isMobile.value ? '超管' : '平台超级管理员'
  if (userStore.userInfo?.roles.includes('ROLE_MERCHANT')) return isMobile.value ? '商户' : '低空物流商户'
  if (userStore.userInfo?.roles.includes('ROLE_ATC')) return isMobile.value ? '空管' : '空管监察员'
  if (userStore.userInfo?.roles.includes('ROLE_PILOT')) return isMobile.value ? '机长' : '商用飞手机长'
  return '系统用户'
})

const roleTagType = computed(() => {
  if (isAdmin.value) return 'danger'
  if (isMerchant.value) return 'primary'
  return 'info'
})

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出当前工作会话吗？', '安全提示', {
    confirmButtonText: '确定退出',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await userStore.logout()
    ElMessage.success('已安全退出')
    router.push('/login')
  })
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  display: flex;
}
.mobile-top-banner {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 28px;
  background: linear-gradient(90deg, #0284c7, #0ea5e9);
  color: #ffffff;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  cursor: pointer;
}
.right-container {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.aside-menu {
  background-color: #0f172a;
  border-right: 1px solid #1e293b;
  display: flex;
  flex-direction: column;
  transition: width 0.25s ease;
}
.logo-box {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  background-color: #0b1120;
  border-bottom: 1px solid #1e293b;
  gap: 12px;
}
.logo-icon {
  font-size: 26px;
  color: #38bdf8;
}
.logo-title {
  display: flex;
  flex-direction: column;
}
.main-text {
  font-size: 15px;
  font-weight: bold;
  color: #ffffff;
  letter-spacing: 0.5px;
}
.sub-text {
  font-size: 11px;
  color: #64748b;
}
.menu-vertical {
  border-right: none;
  flex: 1;
}
.header-bar {
  background-color: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}
.current-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.role-tag {
  font-size: 12px;
}
.user-name {
  font-weight: 600;
  font-size: 13px;
  color: #0f172a;
}
.user-mobile {
  font-size: 12px;
  color: #64748b;
}
.logout-btn {
  margin-left: 4px;
}
.main-content {
  background-color: #f1f5f9;
  padding: 14px;
  overflow-y: auto;
  overflow-x: auto;
}
@media (max-width: 768px) {
  .header-bar {
    padding: 0 8px;
  }
  .main-content {
    padding: 8px;
  }
}
</style>
