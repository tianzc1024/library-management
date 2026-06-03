<template>
  <div class="borrower-login-container">
    <el-card class="login-card">
      <div class="login-header">
        <h2>图书借阅系统</h2>
        <p>借阅者登录</p>
      </div>
      <el-form ref="loginForm" :model="form" :rules="rules" label-width="0">
        <el-form-item prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="请输入手机号"
            prefix-icon="el-icon-mobile-phone"
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
import { borrowerLogin } from '@/api/borrower'

export default {
  name: 'BorrowerLogin',
  data() {
    return {
      form: {
        phone: '',
        password: ''
      },
      rules: {
        phone: [
          { required: true, message: '请输入手机号', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' }
        ]
      },
      loading: false
    }
  },
  methods: {
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (!valid) return

        this.loading = true
        borrowerLogin(this.form.phone, this.form.password)
          .then(res => {
            const data = res.data
            if (data.code === 200) {
              const user = data.data
              localStorage.setItem('token', user.token)
              localStorage.setItem('phone', user.phone)
              localStorage.setItem('name', user.name)
              localStorage.setItem('userId', user.userId)
              localStorage.setItem('userType', user.userType)

              this.$message.success('登录成功')
              this.$router.push('/borrower/my-records')
            } else {
              this.$message.error(data.message || '登录失败')
            }
          })
          .catch(() => {
            this.$message.error('登录失败，请检查手机号和密码')
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
.borrower-login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  color: #303133;
  font-size: 26px;
  margin-bottom: 8px;
}

.login-header p {
  color: #909399;
  font-size: 14px;
}
</style>
