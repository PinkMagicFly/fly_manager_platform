<template>
  <div class="dashboard-wrap">
    <!-- 顶部欢迎卡片 -->
    <el-card shadow="never" class="welcome-card">
      <div class="welcome-box">
        <div class="user-avatar"><el-icon><UserFilled /></el-icon></div>
        <div class="welcome-info">
          <h3>欢迎进入工作台，{{ userStore.userInfo?.loginName }}</h3>
          <p class="role-desc">
            身份类别：<el-tag size="small" type="primary">{{ userRoleName }}</el-tag>
            <span v-if="userStore.userInfo?.corpName" class="corp-tag">企业：{{ userStore.userInfo?.corpName }}</span>
          </p>
        </div>
      </div>
    </el-card>

    <!-- 关键指标统计看板 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">待审批商户资质</div>
          <div class="stat-num text-primary">2 <span class="stat-unit">家</span></div>
          <div class="stat-footer">今日新增 +1</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">在网执飞无人机</div>
          <div class="stat-num text-success">4 <span class="stat-unit">架</span></div>
          <div class="stat-footer">健康度均值 96分</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">今日低空货运单</div>
          <div class="stat-num text-warning">28 <span class="stat-unit">单</span></div>
          <div class="stat-footer">按期交付率 100%</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">空域管制与告警</div>
          <div class="stat-num text-danger">0 <span class="stat-unit">起</span></div>
          <div class="stat-footer">当前空域合规运行</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作与待办事项 -->
    <el-row :gutter="16" class="content-row">
      <el-col :span="14">
        <el-card shadow="never" header="快捷协同通道">
          <div class="quick-grid">
            <el-button v-if="isAdmin" type="primary" plain @click="$router.push('/admin/merchant-audit')">
              <el-icon><Stamp /></el-icon> 商户入驻审核
            </el-button>
            <el-button v-if="isAdmin" type="success" plain @click="$router.push('/admin/customer-audit')">
              <el-icon><CreditCard /></el-icon> 客户实名授信
            </el-button>
            <el-button v-if="isAdmin" type="info" plain @click="$router.push('/admin/audit-logs')">
              <el-icon><Document /></el-icon> 全局审计日志
            </el-button>
            <el-button v-if="isMerchant" type="primary" plain @click="$router.push('/merchant/sub-accounts')">
              <el-icon><User /></el-icon> 企业子账号设置
            </el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never" header="今日待办清单">
          <el-timeline>
            <el-timeline-item timestamp="刚刚" type="primary">
              系统完成 RBAC 权限中心与加盐散列鉴权初始化
            </el-timeline-item>
            <el-timeline-item timestamp="10分钟前" type="warning">
              翼联创新低空货运提交资质申报，等待审核
            </el-timeline-item>
            <el-timeline-item timestamp="30分钟前" type="success">
              顺丰丰舟-90 (UAV-FY90-001) 起降场站机位预约成功
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { UserFilled, Stamp, CreditCard, Document, User } from '@element-plus/icons-vue'

const userStore = useUserStore()

const isAdmin = computed(() => userStore.userInfo?.roles.includes('ROLE_ADMIN'))
const isMerchant = computed(() => userStore.userInfo?.roles.includes('ROLE_MERCHANT') || userStore.userInfo?.accountKind === 2)

const userRoleName = computed(() => {
  if (isAdmin.value) return '超级管理员'
  if (isMerchant.value) return '物流商户主账号'
  return '平台操作员'
})
</script>

<style scoped>
.dashboard-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.welcome-card {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e2e8f0;
}
.welcome-box {
  display: flex;
  align-items: center;
  gap: 16px;
}
.user-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background-color: #165dff;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
}
.welcome-info h3 {
  margin: 0 0 6px 0;
  color: #0f172a;
  font-size: 18px;
}
.role-desc {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 8px;
}
.corp-tag {
  color: #165dff;
  font-weight: 500;
}
.stat-row {
  margin-top: 4px;
}
.stat-card {
  border-radius: 8px;
}
.stat-title {
  font-size: 13px;
  color: #64748b;
}
.stat-num {
  font-size: 28px;
  font-weight: 700;
  margin: 10px 0;
}
.stat-unit {
  font-size: 13px;
  font-weight: normal;
}
.text-primary { color: #165dff; }
.text-success { color: #10b981; }
.text-warning { color: #f59e0b; }
.text-danger { color: #ef4444; }
.stat-footer {
  font-size: 12px;
  color: #94a3b8;
}
.quick-grid {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  padding: 10px 0;
}
</style>
