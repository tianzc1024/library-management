<template>
  <div class="login-container">
    <el-card class="login-card">
      <div class="login-header">
        <h2>图书管理系统</h2>
        <p>用户登录</p>
      </div>
      <el-form ref="loginForm" :model="form" :rules="rules" label-width="0">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            prefix-icon="el-icon-user"
            @keyup.enter.native="handleLogin"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="el-icon-lock"
            show-password
            @keyup.enter.native="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="rememberMe">记住我</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { login } from '@/api/auth'

export default {
  name: 'Login',
  data() {
    return {
      form: {
        username: '',
        password: ''
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' }
        ]
      },
      loading: false,
      rememberMe: false
    }
  },
  methods: {
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (!valid) return

        this.loading = true
        login(this.form.username, this.form.password)
          .then(res => {
            const data = res.data
            if (data.code === 200) {
              const user = data.data
              localStorage.setItem('token', user.token)
              localStorage.setItem('username', user.username)
              localStorage.setItem('realName', user.realName)
              localStorage.setItem('role', user.role)

              if (!this.rememberMe) {
                // 不记住的情况下使用 sessionStorage 作为备份标记
                sessionStorage.setItem('token', user.token)
              }

              this.$message.success('登录成功')
              this.$router.push('/')
            } else {
              this.$message.error(data.message || '登录失败')
            }
          })
          .catch(() => {
            this.$message.error('登录失败，请检查用户名和密码')
          })
          .finally(() => {
            this.loading = false
          })
      })
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #304156 0%, #409EFF 100%);
}

.login-card {
  width: 400px;
  border-radius: 8px;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  color: #303133;
  font-size: 24px;
  margin-bottom: 8px;
}

.login-header p {
  color: #909399;
  font-size: 14px;
}
</style>
