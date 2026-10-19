import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import MainLayout from '@/layout/MainLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '用户登录认证', public: true }
  },
  {
    path: '/merchant/apply',
    name: 'MerchantApply',
    component: () => import('@/views/merchant/MerchantApplyView.vue'),
    meta: { title: '商户企业入驻向导', public: true }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/RoleDashboardView.vue'),
        meta: { title: '角色工作台' }
      },
      {
        path: 'merchant/sub-accounts',
        name: 'SubAccounts',
        component: () => import('@/views/merchant/SubAccountsView.vue'),
        meta: { title: '子账户与权限设置' }
      },
      {
        path: 'admin/merchant-audit',
        name: 'MerchantAudit',
        component: () => import('@/views/admin/MerchantAuditView.vue'),
        meta: { title: '商户资质审核台' }
      },
      {
        path: 'admin/customer-audit',
        name: 'CustomerAudit',
        component: () => import('@/views/admin/CustomerAuditView.vue'),
        meta: { title: '客户实名认证台' }
      },
      {
        path: 'admin/audit-logs',
        name: 'AuditLogs',
        component: () => import('@/views/admin/AuditLogsView.vue'),
        meta: { title: '敏感操作审计日志' }
      },
      {
        path: 'admin/alarm',
        name: 'AlarmCenter',
        component: () => import('@/views/admin/AlarmCenterView.vue'),
        meta: { title: '实时告警处置台' }
      },
      {
        path: 'admin/tickets',
        name: 'TicketCenter',
        component: () => import('@/views/admin/TicketCenterView.vue'),
        meta: { title: '客户工单协同台' }
      },
      {
        path: 'admin/params',
        name: 'SysParam',
        component: () => import('@/views/admin/SysParamView.vue'),
        meta: { title: '系统参数配置中枢' }
      },
      {
        path: 'admin/accounts',
        name: 'AccountManage',
        component: () => import('@/views/admin/AccountManageView.vue'),
        meta: { title: '空管与平台账号管理' }
      },

      {
        path: 'asset/uav',
        name: 'UavFleet',
        component: () => import('@/views/asset/UavFleetView.vue'),
        meta: { title: '机队资产台账' }
      },
      {
        path: 'asset/site',
        name: 'SiteMap',
        component: () => import('@/views/asset/SiteMapView.vue'),
        meta: { title: '场站一张图' }
      },
      {
        path: 'asset/crew',
        name: 'CrewShift',
        component: () => import('@/views/asset/CrewShiftView.vue'),
        meta: { title: '机组排班矩阵' }
      },
      {
        path: 'asset/efficiency',
        name: 'AssetEfficiency',
        component: () => import('@/views/asset/AssetEfficiencyView.vue'),
        meta: { title: '资产效能分析' }
      },
      {
        path: 'airspace/situation',
        name: 'AirspaceSituation',
        component: () => import('@/views/airspace/AirspaceSituationView.vue'),
        meta: { title: '空域三区态势图' }
      },
      {
        path: 'airspace/apply-wizard',
        name: 'FlightApplyWizard',
        component: () => import('@/views/airspace/FlightApplyWizardView.vue'),
        meta: { title: '飞行计划申报向导' }
      },
      {
        path: 'airspace/approval',
        name: 'FlightApproval',
        component: () => import('@/views/airspace/FlightApprovalView.vue'),
        meta: { title: '飞行申报审批台' }
      },
      {
        path: 'order/board',
        name: 'OrderBoard',
        component: () => import('@/views/order/OrderBoardView.vue'),
        meta: { title: '订单综合看板' }
      },
      {
        path: 'order/detail/:id',
        name: 'OrderDetail',
        component: () => import('@/views/order/OrderDetailView.vue'),
        meta: { title: '订单全景详情' }
      },
      {
        path: 'order/dispatch',
        name: 'OrderDispatch',
        component: () => import('@/views/order/OrderDispatchView.vue'),
        meta: { title: '智能调度工作台' }
      },
      {
        path: 'order/bills',
        name: 'OrderBills',
        component: () => import('@/views/order/OrderBillsView.vue'),
        meta: { title: '结算与账单中心' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局路由守卫
router.beforeEach((to, from, next) => {
  document.title = (to.meta.title ? `${to.meta.title} - ` : '') + '翔云·低空智慧物流运营平台'
  const token = localStorage.getItem('soarcloud_token')
  if (to.meta.public) {
    next()
  } else {
    if (!token) {
      next('/login')
    } else {
      next()
    }
  }
})

export default router
