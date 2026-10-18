<template>
  <view class="m-login">
    <view class="m-header">
      <view class="m-logo">翔云</view>
      <text class="title">低空移动作业端</text>
      <text class="subtitle">无人机机长 · 现场调度 · 维保签到</text>
    </view>

    <!-- 快速切换 -->
    <view class="quick-bar">
      <text class="quick-label">快捷切换：</text>
      <button class="mini-btn" @click="fill('customer_demo')">客户</button>
      <button class="mini-btn" @click="fill('pilot_sf')">机长</button>
      <button class="mini-btn" @click="fill('maintain_li')">运维</button>
    </view>


    <view class="form-box">
      <view class="input-item">
        <text class="label">登录账号 / 手机号</text>
        <input class="input" v-model="username" placeholder="请输入用户名或手机号" />
      </view>

      <view class="input-item">
        <text class="label">登录密码</text>
        <input class="input" v-model="password" password placeholder="请输入密码 (默认 123456)" />
      </view>

      <button class="login-btn" :disabled="loading" @click="handleLogin">
        {{ loading ? '正在验证身份...' : '立即登录' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import request from '../../utils/request'

const username = ref('pilot_sf')
const password = ref('123456')

const loading = ref(false)

const fill = (u: string) => {
  username.value = u
  password.value = '123456'
}

const handleLogin = async () => {
  if (!username.value || !password.value) {
    uni.showToast({
      title: '请输入账号和密码',
      icon: 'none'
    })
    return
  }
  loading.value = true
  try {
    const res: any = await request.post('/api/v1/auth/login', {
      username: username.value,
      password: password.value,
      loginType: 'password'
    })
    uni.setStorageSync('soarcloud_mobile_token', res.token)
    uni.setStorageSync('soarcloud_mobile_user', JSON.stringify(res))
    uni.showToast({
      title: '登录成功',
      icon: 'success'
    })
    setTimeout(() => {
      uni.reLaunch({
        url: '/pages/work/index'
      })
    }, 400)
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
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 30px;
}
.m-logo {
  width: 60px;
  height: 60px;
  line-height: 60px;
  text-align: center;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #165dff 0%, #00b42a 100%);
  color: #fff;
  font-size: 20px;
  font-weight: bold;
  border-radius: 16px;
}
.title {
  font-size: 22px;
  font-weight: bold;
  color: #0f172a;
  margin-bottom: 6px;
}
.subtitle {
  font-size: 13px;
  color: #64748b;
}
.quick-bar {
  background: #f1f5f9;
  padding: 8px 12px;
  border-radius: 8px;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.quick-label {
  font-size: 12px;
  color: #64748b;
}
.mini-btn {
  background: #fff;
  border: 1px solid #cbd5e1;
  padding: 0 8px;
  height: 28px;
  line-height: 26px;
  border-radius: 4px;
  font-size: 12px;
  color: #334155;
  margin: 0;
}
.input-item {
  margin-bottom: 18px;
  display: flex;
  flex-direction: column;
}
.label {
  display: block;
  font-size: 13px;
  color: #475569;
  margin-bottom: 6px;
  font-weight: 500;
}
.input {
  width: 100%;
  height: 44px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 14px;
  background-color: #f8fafc;
}
.login-btn {
  width: 100%;
  height: 46px;
  line-height: 46px;
  margin-top: 10px;
  background: #165dff;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
}
.login-btn:disabled {
  background-color: #94a3b8;
}
</style>

