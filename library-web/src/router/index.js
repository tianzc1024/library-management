import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

const router = new Router({
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/Login.vue')
    },
    {
      path: '/',
      name: 'Dashboard',
      component: () => import('../views/Dashboard.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/book',
      name: 'BookList',
      component: () => import('../views/BookList.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/user',
      name: 'UserList',
      component: () => import('../views/UserList.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/borrow-list',
      name: 'BorrowList',
      component: () => import('../views/BorrowList.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/borrow',
      name: 'BorrowForm',
      component: () => import('../views/BorrowForm.vue'),
      meta: { requiresAuth: true }
    }
  ]
})

// 全局路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth) {
    if (token) {
      next()
    } else {
      next('/login')
    }
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
