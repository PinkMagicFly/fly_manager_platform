<template>
  <div class="m-login">
    <div class="m-header">
      <div class="m-logo">翔云</div>
      <h2>低空移动作业端</h2>
      <p>无人机机长 · 现场调度 · 维保签到</p>
    </div>

    <!-- 快速切换 -->
    <div class="quick-bar">
      <span>快捷切换：</span>
      <button @click="fill('pilot_zhang')">机长</button>
      <button @click="fill('maintain_li')">运维</button>
      <button @click="fill('customer_demo')">客户</button>
    </div>

    <div class="form-box">
      <div class="input-item">
        <label>登录账号 / 手机号</label>
        <input v-model="username" placeholder="请输入用户名或手机号" />
      </div>

      <div class="input-item">
        <label>登录密码</label>
        <input v-model="password" type="password" placeholder="请输入密码 (默认 123456)" />
      </div>

      <button class="login-btn" :disabled="loading" @click="handleLogin">
        {{ loading ? '正在验证身份...' : '立即登录' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const username = ref('pilot_zhang')
const password = ref('123456')
const loading = ref(false)

const fill = (u: string) => {
  username.value = u
  password.value = '123456'
}

const handleLogin = async () => {
  if (!username.value || !password.value) {
    alert('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const res: any = await request.post('/api/v1/auth/login', {
      username: username.value,
      password: password.value,
      loginType: 'password'
    })
    localStorage.setItem('soarcloud_mobile_token', res.token)
    localStorage.setItem('soarcloud_mobile_user', JSON.stringify(res))
    router.push('/profile')
  } catch (e) {
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.m-login {
  padding: 40px 24px;
}
.m-header {
  text-align: center;
  margin-bottom: 30px;
}
.m-logo {
  width: 60px;
  height: 60px;
  line-height: 60px;
  margin: 0 auto 16px;
  background: linear-gradient(135deg, #165dff 0%, #00b42a 100%);
  color: #fff;
  font-size: 20px;
  font-weight: bold;
  border-radius: 16px;
}
.m-header h2 {
  font-size: 22px;
  color: #0f172a;
  margin-bottom: 6px;
}
.m-header p {
  font-size: 13px;
  color: #64748b;
}
.quick-bar {
  background: #f1f5f9;
  padding: 8px 12px;
  border-radius: 8px;
  margin-bottom: 24px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.quick-bar button {
  background: #fff;
  border: 1px solid #cbd5e1;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}
.input-item {
  margin-bottom: 18px;
}
.input-item label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #334155;
  margin-bottom: 8px;
}
.input-item input {
  width: 100%;
  height: 44px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0 14px;
  font-size: 15px;
  outline: none;
}
.input-item input:focus {
  border-color: #165dff;
}
.login-btn {
  width: 100%;
  height: 46px;
  background-color: #165dff;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  margin-top: 10px;
  cursor: pointer;
}
.login-btn:disabled {
  background-color: #94a3b8;
}
</style>
