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
      meta: { requiresAuth: true, adminOnly: true }
    },
    {
      path: '/book',
      name: 'BookList',
      component: () => import('../views/BookList.vue'),
      meta: { requiresAuth: true, adminOnly: true }
    },
    {
      path: '/user',
      name: 'UserList',
      component: () => import('../views/UserList.vue'),
      meta: { requiresAuth: true, adminOnly: true }
    },
    {
      path: '/borrow-list',
      name: 'BorrowList',
      component: () => import('../views/BorrowList.vue'),
      meta: { requiresAuth: true, adminOnly: true }
    },
    {
      path: '/borrow',
      name: 'BorrowForm',
      component: () => import('../views/BorrowForm.vue'),
      meta: { requiresAuth: true, adminOnly: true }
    },
    {
      path: '/borrower/login',
      name: 'BorrowerLogin',
      component: () => import('../views/borrower/BorrowerLogin.vue')
    },
    {
      path: '/borrower/my-records',
      name: 'MyBorrowRecords',
      component: () => import('../views/borrower/MyBorrowRecords.vue'),
      meta: { requiresAuth: true, borrowerOnly: true }
    }
  ]
})

// 全局路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userType = localStorage.getItem('userType') || ''

  if (to.meta.requiresAuth) {
    if (!token) {
      if (to.meta.borrowerOnly) {
        next('/borrower/login')
      } else {
        next('/login')
      }
      return
    }
    // 检查用户类型限制
    if (to.meta.adminOnly && userType === 'BORROWER') {
      next('/borrower/my-records')
      return
    }
    if (to.meta.borrowerOnly && userType !== 'BORROWER') {
      next('/')
      return
    }
    next()
  } else if (to.path === '/login' && token) {
    if (userType === 'BORROWER') {
      next('/borrower/my-records')
    } else {
      next('/')
    }
  } else if (to.path === '/borrower/login' && token && userType === 'BORROWER') {
    next('/borrower/my-records')
  } else {
    next()
  }
})

export default router
