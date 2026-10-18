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
          <div class="stat-num text-primary">
            {{ pendingMerchantCount }} <span class="stat-unit">家</span>
          </div>
          <div class="stat-footer">
            {{ pendingMerchantCount > 0 ? '需运营管理人员核验' : '当前所有企业均已处理' }}
          </div>
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
            <el-button v-if="isAdmin" type="danger" plain :loading="resettingData" @click="handleResetDatabase">
              <el-icon><RefreshRight /></el-icon> 一键重置种子数据
            </el-button>

            <!-- 调度员专属快捷入口 -->
            <template v-if="isDispatcher">
              <el-button type="primary" plain @click="$router.push('/order/board')">
                <el-icon><List /></el-icon> 订单综合看板
              </el-button>
              <el-button type="success" plain @click="$router.push('/order/dispatch')">
                <el-icon><Compass /></el-icon> 智能调度工作台
              </el-button>
              <el-button type="info" plain @click="$router.push('/airspace/situation')">
                <el-icon><Guide /></el-icon> 空域管制态势
              </el-button>
            </template>

            <!-- 商户主账号专属快捷入口 (调度员不展示子账号设置) -->
            <el-button v-if="isMerchant && !isDispatcher" type="primary" plain @click="$router.push('/merchant/sub-accounts')">
              <el-icon><User /></el-icon> 企业子账号设置
            </el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never" header="今日待办与动态清单">
          <div v-loading="loadingTodos">
            <el-timeline v-if="dynamicTodos.length > 0">
              <el-timeline-item
                v-for="(item, idx) in dynamicTodos"
                :key="idx"
                :timestamp="item.timestamp"
                :type="item.type"
              >
                <div class="todo-item-content">
                  <span class="todo-text">{{ item.title }}</span>
                  <el-button
                    v-if="item.actionPath"
                    type="primary"
                    link
                    size="small"
                    @click="$router.push(item.actionPath)"
                  >
                    处理
                  </el-button>
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无待办事项，系统运转良好" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { UserFilled, Stamp, CreditCard, Document, User, RefreshRight, List, Compass, Guide } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()
const resettingData = ref(false)

const handleResetDatabase = () => {
  ElMessageBox.confirm(
    '警告：一键重置将恢复全平台系统基准种子数据，所有测试产生的脏数据将被清除，预置账号（admin, merchant_sf, dispatcher_sf, atc_officer, pilot_sf, maintain_li, customer_demo 密码统一 123456）将全部恢复就绪。确认继续执行重置吗？',
    '系统数据重置高危确认',
    {
      confirmButtonText: '确认重置初始化',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    resettingData.value = true
    try {
      const res: any = await request.post('/api/v1/admin/database/reset-init-data')
      ElMessageBox.alert(res || '平台初始化种子数据已成功重置！', '重置成功', {
        type: 'success',
        confirmButtonText: '知道了'
      })
      fetchDashboardData()
    } catch (err: any) {
      ElMessage.error(err.message || '重置失败')
    } finally {
      resettingData.value = false
    }
  })
}

const isAdmin = computed(() => userStore.userInfo?.roles.includes('ROLE_ADMIN'))
const isDispatcher = computed(() => userStore.userInfo?.roles.includes('ROLE_DISPATCHER'))
const isMerchant = computed(() => userStore.userInfo?.roles.includes('ROLE_MERCHANT') || userStore.userInfo?.roles.includes('ROLE_DISPATCHER') || userStore.userInfo?.accountKind === 2)

const userRoleName = computed(() => {
  if (isAdmin.value) return '超级管理员'
  if (isDispatcher.value) return '商户调度员 (顺丰速运)'
  if (isMerchant.value) return '物流商户主账号'
  return '平台操作员'
})


interface TodoItem {
  title: string
  timestamp: string
  type: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  actionPath?: string
}

const pendingMerchantCount = ref(0)
const loadingTodos = ref(false)
const dynamicTodos = ref<TodoItem[]>([])

const fetchDashboardData = async () => {
  loadingTodos.value = true
  const list: TodoItem[] = []

  try {
    if (isAdmin.value) {
      // 1. 查询待审核商户
      try {
        const merchants: any = await request.get('/api/v1/merchant/list?auditState=0')
        const pendingMerchants = merchants || []
        pendingMerchantCount.value = pendingMerchants.length

        pendingMerchants.forEach((m: any) => {
          list.push({
            title: `待审核商户：【${m.corpName}】提交了资质申报（信用代码：${m.licenseNo}）`,
            timestamp: m.createdAt ? m.createdAt.substring(11, 19) : '待办',
            type: 'warning',
            actionPath: '/admin/merchant-audit'
          })
        })
      } catch (e) {
      }

      // 2. 查询最新审计动态
      try {
        const logs: any = await request.get('/api/v1/admin/audit-logs?limit=4')
        if (Array.isArray(logs)) {
          logs.forEach((log: any) => {
            list.push({
              title: `[${log.moduleName || '系统'}] ${log.loginName || '用户'} 执行了「${log.actionName || '操作'}」`,
              timestamp: log.createdAt ? log.createdAt.substring(11, 19) : '刚刚',
              type: log.respStatus === 200 ? 'success' : 'danger',
              actionPath: '/admin/audit-logs'
            })
          })
        }
      } catch (e) {
      }
    } else if (isDispatcher.value) {
      // 调度员视角待办 (统计待接单与待调度，绝不调用商户主账号的子账号管理接口)
      try {
        const stats: any = await request.get('/api/v1/order/board-stats')
        if (stats) {
          if (stats.pendingDispatchCount > 0) {
            list.push({
              title: `当前有 ${stats.pendingDispatchCount} 笔运单处于【待调度】状态，等待指派飞行方案与申报`,
              timestamp: '待办',
              type: 'warning',
              actionPath: '/order/dispatch'
            })
          }
          if (stats.pendingAcceptCount > 0) {
            list.push({
              title: `当前有 ${stats.pendingAcceptCount} 笔运单处于【待接单】状态，请及时审核承运`,
              timestamp: '待办',
              type: 'primary',
              actionPath: '/order/board'
            })
          }
          if (stats.flyingCount > 0) {
            list.push({
              title: `当前有 ${stats.flyingCount} 架次低空无人机正处于在途配送执行中`,
              timestamp: '执行中',
              type: 'success',
              actionPath: '/order/board'
            })
          }
        }
      } catch (e) {
      }
    } else if (isMerchant.value) {
      // 商户主账号视角待办
      try {
        const archive: any = await request.get('/api/v1/merchant/my-archive')
        if (archive) {
          if (archive.auditState === 1) {
            list.push({
              title: `企业【${archive.corpName}】资质已审批通过，系统已激活`,
              timestamp: '正常运行',
              type: 'success'
            })
          } else if (archive.auditState === 0) {
            list.push({
              title: `企业入驻资质正在审核中，请耐心等待平台审批`,
              timestamp: '待办',
              type: 'warning'
            })
          }
        }
        const subs: any = await request.get('/api/v1/merchant/sub-accounts')
        if (Array.isArray(subs)) {
          list.push({
            title: `企业已配置 ${subs.length} 个协同子账号席位`,
            timestamp: '今日',
            type: 'primary',
            actionPath: '/merchant/sub-accounts'
          })
        }
      } catch (e) {
      }
    }


    // 补充底座基线动态
    if (list.length === 0) {
      list.push({
        title: '系统权限与安全底座正常运行，所有待办已清空',
        timestamp: '刚刚',
        type: 'primary'
      })
    }
  } catch (e) {
  } finally {
    dynamicTodos.value = list
    loadingTodos.value = false
  }
}

onMounted(() => {
  fetchDashboardData()
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
  padding: 4px;
}
.stat-title {
  font-size: 13px;
  color: #64748b;
}
.stat-num {
  font-size: 28px;
  font-weight: 700;
  margin: 8px 0;
}
.stat-unit {
  font-size: 13px;
  font-weight: 400;
  color: #64748b;
}
.stat-footer {
  font-size: 12px;
  color: #94a3b8;
}
.text-primary { color: #165dff; }
.text-success { color: #00b42a; }
.text-warning { color: #ff7d00; }
.text-danger { color: #f53f3f; }

.quick-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.todo-item-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.todo-text {
  font-size: 13px;
  color: #334155;
}
</style>
