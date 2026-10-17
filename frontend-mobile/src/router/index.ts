import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/profile'
  },
  {
    path: '/login',
    name: 'MobileLogin',
    component: () => import('@/pages/login/index.vue')
  },
  {
    path: '/profile',
    name: 'MobileProfile',
    component: () => import('@/pages/profile/index.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('soarcloud_mobile_token')
  if (to.path === '/login') {
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
