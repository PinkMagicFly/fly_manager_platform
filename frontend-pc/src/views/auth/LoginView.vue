<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="logo-icon-wrap"><el-icon class="brand-icon"><Position /></el-icon></div>
        <h2 class="title">翔云·低空智慧物流运营平台</h2>
        <p class="subtitle">低空智能立体交通与货运协同数字底座</p>
      </div>

      <div class="quick-roles">
        <span class="quick-label">演示账号：</span>
        <el-button size="small" type="primary" plain @click="fillQuick('admin')">管理员</el-button>
        <el-button size="small" type="success" plain @click="fillQuick('merchant_sf')">商户(顺丰)</el-button>
        <el-button size="small" type="warning" plain @click="fillQuick('atc_officer')">空管员</el-button>
        <el-button size="small" type="info" plain @click="fillQuick('pilot_zhang')">机长</el-button>
      </div>

      <el-tabs v-model="loginMode">
        <el-tab-pane label="密码登录" name="password" />
        <el-tab-pane label="验证码登录" name="sms" />
      </el-tabs>

      <el-form :model="loginForm" :rules="rules" ref="formRef" size="large">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" placeholder="登录用户名或手机号" :prefix-icon="User" clearable />
        </el-form-item>

        <el-form-item prop="password" v-if="loginMode === 'password'">
          <el-input v-model="loginForm.password" type="password" placeholder="密码 (默认 123456)" :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
        </el-form-item>

        <el-form-item prop="smsCode" v-else>
          <div style="display:flex;gap:10px;width:100%">
            <el-input v-model="loginForm.smsCode" placeholder="验证码 (默认 123456)" :prefix-icon="Key" clearable @keyup.enter="handleLogin" />
            <el-button :disabled="countdown > 0" @click="handleSendCode">{{ countdown > 0 ? `${countdown}s` : '获取验证码' }}</el-button>
          </div>
        </el-form-item>

        <el-button type="primary" :loading="loading" style="width:100%;margin-top:10px;" @click="handleLogin">立即登录进入系统</el-button>
      </el-form>

      <div class="login-footer">
        <router-link to="/merchant/apply" class="apply-link"><el-icon><Promotion /></el-icon> 物流企业入驻向导</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { User, Lock, Key, Position, Promotion } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()
const loginMode = ref<'password' | 'sms'>('password')
const loading = ref(false)
const formRef = ref<FormInstance>()
const countdown = ref(0)

const loginForm = reactive({ username: 'admin', password: '123456', smsCode: '123456' })
const rules = {
  username: [{ required: true, message: '请输入账号或手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  smsCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const fillQuick = (user: string) => {
  loginForm.username = user
  loginForm.password = '123456'
  loginForm.smsCode = '123456'
}

const handleSendCode = async () => {
  if (!loginForm.username) return ElMessage.warning('请先输入手机号')
  try {
    const res: any = await request.post(`/api/v1/auth/send-sms-code?mobile=${loginForm.username}`)
    ElMessage.success(`验证码发送成功: ${res}`)
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {}
}

const handleLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res: any = await request.post('/api/v1/auth/login', {
        username: loginForm.username,
        password: loginMode.value === 'password' ? loginForm.password : undefined,
        smsCode: loginMode.value === 'sms' ? loginForm.smsCode : undefined,
        loginType: loginMode.value
      })
      userStore.setLoginSession(res.token, res)
      ElMessage.success(`欢迎进入翔云平台，${res.loginName}`)
      router.push('/dashboard')
    } catch (e) {} finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  width: 100vw;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at 50% 50%, #1e293b 0%, #0b1120 100%);
}
.login-card {
  width: 430px;
  background: #ffffff;
  border-radius: 12px;
  padding: 32px 36px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}
.login-header {
  text-align: center;
  margin-bottom: 20px;
}
.logo-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  background: linear-gradient(135deg, #165dff 0%, #38bdf8 100%);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
}
.brand-icon {
  font-size: 26px;
  color: #ffffff;
}
.title {
  margin: 0 0 6px 0;
  font-size: 19px;
  color: #0f172a;
}
.subtitle {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}
.quick-roles {
  background-color: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  padding: 8px 10px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.quick-label {
  font-size: 12px;
  color: #64748b;
}
.login-footer {
  margin-top: 20px;
  text-align: center;
  border-top: 1px solid #f1f5f9;
  padding-top: 14px;
}
.apply-link {
  font-size: 13px;
  color: #165dff;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
</style>
