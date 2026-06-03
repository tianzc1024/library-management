<template>
  <div id="app">
    <!-- 登录页面不显示侧边栏 -->
    <template v-if="$route.path === '/login' || $route.path === '/borrower/login'">
      <router-view />
    </template>
    <!-- C端借阅者页面：简洁顶栏，无侧边栏 -->
    <template v-else-if="userType === 'BORROWER'">
      <el-container style="min-height: 100vh; flex-direction: column">
        <el-header style="background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; display: flex; align-items: center; justify-content: space-between; height: 60px">
          <h3 style="margin: 0; cursor: pointer" @click="$router.push('/borrower/my-records')">
            图书借阅系统
          </h3>
          <div class="user-info">
            <span class="borrower-name">{{ borrowerName }}</span>
            <span class="borrower-phone">{{ phone }}</span>
            <el-button type="text" style="color: #fff; margin-left: 12px" @click="borrowerLogout">
              退出
            </el-button>
          </div>
        </el-header>
        <el-main style="background: #f0f2f5; flex: 1">
          <router-view />
        </el-main>
      </el-container>
    </template>
    <template v-else>
      <el-container style="min-height: 100vh">
        <el-aside width="200px" style="background-color: #304156">
          <div class="logo">图书管理系统</div>
          <el-menu
            :default-active="$route.path"
            background-color="#304156"
            text-color="#bfcbd9"
            active-text-color="#409EFF"
            router
          >
            <el-menu-item index="/">
              <i class="el-icon-s-home"></i>
              <span>首页仪表盘</span>
            </el-menu-item>
            <el-menu-item index="/book">
              <i class="el-icon-reading"></i>
              <span>图书管理</span>
            </el-menu-item>
            <el-menu-item index="/user">
              <i class="el-icon-user"></i>
              <span>人员管理</span>
            </el-menu-item>
            <el-menu-item index="/borrow-list">
              <i class="el-icon-document"></i>
              <span>借阅记录</span>
            </el-menu-item>
            <el-menu-item index="/borrow">
              <i class="el-icon-s-order"></i>
              <span>借阅/归还</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        <el-container>
          <el-header style="background: #fff; border-bottom: 1px solid #dcdfe6; display: flex; align-items: center; justify-content: space-between">
            <h3 style="margin: 0; color: #303133">图书管理系统</h3>
            <div class="user-info">
              <span class="user-name">{{ realName }}</span>
              <el-dropdown @command="handleCommand">
                <span class="el-dropdown-link">
                  {{ username }}<i class="el-icon-arrow-down el-icon--right"></i>
                </span>
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item disabled>
                    角色：{{ role === 'ADMIN' ? '管理员' : '操作员' }}
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
            </div>
          </el-header>
          <el-main style="background: #f0f2f5">
            <router-view />
          </el-main>
        </el-container>
      </el-container>
    </template>
  </div>
</template>

<script>
export default {
  name: 'App',
  data() {
    return {
      username: localStorage.getItem('username') || '',
      realName: localStorage.getItem('realName') || '',
      role: localStorage.getItem('role') || '',
      userType: localStorage.getItem('userType') || '',
      phone: localStorage.getItem('phone') || '',
      borrowerName: localStorage.getItem('name') || ''
    }
  },
  methods: {
    handleCommand(command) {
      if (command === 'logout') {
        this.$confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          localStorage.removeItem('token')
          localStorage.removeItem('username')
          localStorage.removeItem('realName')
          localStorage.removeItem('role')
          sessionStorage.removeItem('token')
          this.$router.push('/login')
          this.$message.success('已退出登录')
        }).catch(() => {})
      }
    },
    borrowerLogout() {
      this.$confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        localStorage.removeItem('token')
        localStorage.removeItem('phone')
        localStorage.removeItem('name')
        localStorage.removeItem('userId')
        localStorage.removeItem('userType')
        this.$router.push('/borrower/login')
        this.$message.success('已退出登录')
      }).catch(() => {})
    }
  }
}
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', sans-serif; }
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  background-color: #263445;
}
.el-menu { border-right: none; }
.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-name {
  color: #606266;
  font-size: 14px;
}
.el-dropdown-link {
  cursor: pointer;
  color: #409EFF;
  font-size: 14px;
}
.borrower-name {
  font-size: 14px;
  margin-right: 8px;
}
.borrower-phone {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}
</style>
