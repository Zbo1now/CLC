<template>
  <div class="login-container">
    <div class="login-content">
      <div class="login-header">
        <el-icon class="logo-icon"><Monitor /></el-icon>
        <h2 class="title">众创空间管理系统</h2>
      </div>
      <el-card class="login-card" shadow="hover">
        <div class="welcome-text">欢迎回来，请登录</div>
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              size="large"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              show-password
              size="large"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              class="login-button"
              size="large"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate((valid, fields) => {
    if (valid) {
      loading.value = true
      // 模拟登录请求
      setTimeout(() => {
        loading.value = false
        if (loginForm.username === 'admin' && loginForm.password === '123456') {
          ElMessage.success('登录成功')
          router.push('/')
        } else {
          // 为了演示方便，只要输入了都让进，实际开发请对接后端
          // ElMessage.error('用户名或密码错误')
           ElMessage.success('登录成功 (演示模式)')
           router.push('/')
        }
      }, 1000)
    }
  })
}
</script>

<style scoped lang="scss">
.login-container {
  height: 100vh;
  width: 100vw;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #2b32b2 0%, #1488cc 100%);
  background-size: cover;
  
  .login-content {
    width: 400px;
    
    .login-header {
      text-align: center;
      margin-bottom: 30px;
      color: #fff;
      display: flex;
      flex-direction: column;
      align-items: center;
      
      .logo-icon {
        font-size: 48px;
        margin-bottom: 10px;
      }
      
      .title {
        margin: 0;
        font-size: 28px;
        font-weight: 600;
        letter-spacing: 1px;
      }
    }
    
    .login-card {
      border-radius: 8px;
      border: none;
      background: rgba(255, 255, 255, 0.95);
      
      .welcome-text {
        text-align: center;
        margin-bottom: 25px;
        color: #606266;
        font-size: 16px;
      }
      
      .login-form {
        padding: 10px 20px;
        
        .login-button {
          width: 100%;
          margin-top: 10px;
          font-weight: bold;
          letter-spacing: 2px;
        }
      }
    }
  }
}
</style>
