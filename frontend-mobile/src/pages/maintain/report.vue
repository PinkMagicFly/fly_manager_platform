<template>
  <view class="m-report-container">
    <view class="card-box">
      <view class="form-title">现场设备故障紧急填报</view>

      <view class="form-item">
        <text class="label">选择目标无人机</text>
        <picker :range="uavOptions" range-key="label" :value="selectedIndex" @change="onUavChange">
          <view class="picker-val">
            {{ uavOptions[selectedIndex]?.label || '点击选择无人机' }}
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">故障现象详细描述</text>
        <textarea
          v-model="faultDesc"
          class="desc-textarea"
          placeholder="请描述异常表现，如：旋翼3号电机高温报警、指南针校准偏差等"
        />
      </view>

      <button class="submit-btn" :loading="submitting" @click="handleSubmit">
        立即提交故障工单
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '../../utils/request'

const uavOptions = ref<any[]>([])
const selectedIndex = ref(0)
const faultDesc = ref('')
const submitting = ref(false)

onLoad(async () => {
  try {
    const list: any = await request.get('/api/v1/uav/list')
    if (list && list.length > 0) {
      uavOptions.value = list.map((u: any) => ({
        id: u.uavId,
        label: `${u.uavSn} - ${u.model}`
      }))
    }
  } catch (err) {}
})

const onUavChange = (e: any) => {
  selectedIndex.value = e.detail.value
}

const handleSubmit = async () => {
  if (!faultDesc.value.trim()) {
    uni.showToast({ title: '请输入故障描述', icon: 'none' })
    return
  }
  const chosen = uavOptions.value[selectedIndex.value]
  if (!chosen) {
    uni.showToast({ title: '请选择无人机', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    await request.post('/api/v1/uav/fault', {
      uavId: chosen.id,
      faultDesc: faultDesc.value
    })
    uni.showToast({ title: '故障已提报受理', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 600)
  } catch (err: any) {
    uni.showToast({ title: err.message || '提交异常', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.m-report-container {
  padding: 16px;
  background-color: #f8fafc;
  min-height: 100vh;
}
.card-box {
  background-color: #fff;
  border-radius: 12px;
  padding: 20px;
}
.form-title {
  font-size: 16px;
  font-weight: bold;
  color: #0f172a;
  margin-bottom: 20px;
}
.form-item {
  margin-bottom: 16px;
}
.label {
  font-size: 13px;
  color: #475569;
  margin-bottom: 8px;
  display: block;
}
.picker-val {
  padding: 12px;
  background-color: #f1f5f9;
  border-radius: 8px;
  font-size: 13px;
  color: #0f172a;
}
.desc-textarea {
  width: 100%;
  height: 120px;
  padding: 10px;
  background-color: #f1f5f9;
  border-radius: 8px;
  box-sizing: border-box;
  font-size: 13px;
}
.submit-btn {
  margin-top: 24px;
  background-color: #ef4444;
  color: #fff;
  font-size: 15px;
  font-weight: bold;
  border-radius: 8px;
}
</style>
