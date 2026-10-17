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
