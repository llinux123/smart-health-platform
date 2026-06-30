import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/storage'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginPage.vue'),
    meta: { requiresAuth: false, hideTab: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/login/RegisterPage.vue'),
    meta: { requiresAuth: false, hideTab: true }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/home/HomePage.vue'),
    meta: { requiresAuth: true }
  },
  // AI 问诊
  {
    path: '/consultation',
    name: 'ConsultationSessions',
    component: () => import('@/views/consultation/SessionListPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/consultation/upload',
    name: 'ConsultationUpload',
    component: () => import('@/views/consultation/UploadPage.vue'),
    meta: { requiresAuth: true, hideTab: true }
  },
  {
    path: '/consultation/analysis',
    name: 'ConsultationAnalysis',
    component: () => import('@/views/consultation/AnalysisPage.vue'),
    meta: { requiresAuth: true, hideTab: true }
  },
  {
    path: '/consultation/chat/:sessionSn',
    name: 'ConsultationChat',
    component: () => import('@/views/consultation/ChatPage.vue'),
    meta: { requiresAuth: true, hideTab: true },
    props: true
  },
  // 挂号预约
  {
    path: '/registration/schedules',
    name: 'ScheduleList',
    component: () => import('@/views/registration/ScheduleListPage.vue'),
    meta: { requiresAuth: true, hideTab: true }
  },
  {
    path: '/registration/doctor/:id',
    name: 'DoctorDetail',
    component: () => import('@/views/registration/DoctorDetailPage.vue'),
    meta: { requiresAuth: true, hideTab: true },
    props: true
  },
  {
    path: '/registration/seckill/:scheduleId',
    name: 'Seckill',
    component: () => import('@/views/registration/SeckillPage.vue'),
    meta: { requiresAuth: true, hideTab: true },
    props: true
  },
  {
    path: '/registration/orders',
    name: 'OrderList',
    component: () => import('@/views/registration/OrderListPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/registration/orders/:orderSn',
    name: 'OrderDetail',
    component: () => import('@/views/registration/OrderDetailPage.vue'),
    meta: { requiresAuth: true, hideTab: true },
    props: true
  },
  // 处方
  {
    path: '/prescriptions',
    name: 'PrescriptionList',
    component: () => import('@/views/prescription/PrescriptionListPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/prescriptions/:id',
    name: 'PrescriptionDetail',
    component: () => import('@/views/prescription/PrescriptionDetailPage.vue'),
    meta: { requiresAuth: true, hideTab: true },
    props: true
  },
  // 管理后台
  {
    path: '/admin/schedule',
    name: 'AdminSchedule',
    component: () => import('@/views/admin/ScheduleManagePage.vue'),
    meta: { requiresAuth: true, hideTab: true }
  },
  {
    path: '/admin/prescription/review',
    name: 'AdminPrescriptionReview',
    component: () => import('@/views/admin/PrescriptionReviewPage.vue'),
    meta: { requiresAuth: true, hideTab: true }
  },
  {
    path: '/admin/prescription/issue',
    name: 'AdminPrescriptionIssue',
    component: () => import('@/views/admin/IssuePrescriptionPage.vue'),
    meta: { requiresAuth: true, hideTab: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 导航守卫
const whiteList = ['/login', '/register']

router.beforeEach((to, from, next) => {
  const token = getToken()
  if (whiteList.includes(to.path) || !to.meta.requiresAuth) {
    // 已登录用户访问登录页，重定向到首页
    if (token && (to.path === '/login' || to.path === '/register')) {
      next('/home')
    } else {
      next()
    }
  } else {
    if (!token) {
      next({ path: '/login', query: { redirect: to.fullPath } })
    } else {
      next()
    }
  }
})

export default router
