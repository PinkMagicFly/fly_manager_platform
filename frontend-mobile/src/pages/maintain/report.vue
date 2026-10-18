<template>
  <view class="m-report-container">
    <view class="card-box">
      <view class="form-title">现场设备故障与突发异常紧急上报</view>

      <view v-if="orderId" class="order-badge">
        <text class="badge-lbl">当前关联执飞运单：</text>
        <text class="badge-val">ID #{{ orderId }}</text>
      </view>

      <view class="form-item">
        <text class="label">故障异常类别</text>
        <picker :range="faultTypeOptions" :value="faultTypeIndex" @change="onFaultTypeChange">
          <view class="picker-val">
            {{ faultTypeOptions[faultTypeIndex] }}
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">选择执飞/故障无人机</text>
        <picker :range="uavOptions" range-key="label" :value="selectedIndex" @change="onUavChange">
          <view class="picker-val">
            {{ uavOptions[selectedIndex]?.label || '点击选择无人机' }}
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">故障现象详细描述与现场情况</text>
        <textarea
          v-model="faultDesc"
          class="desc-textarea"
          placeholder="请描述异常表现，如：3号电机过热转速骤降、指南针失步、就近迫降避险等"
        />
      </view>

      <button class="submit-btn" :loading="submitting" @click="handleSubmit">
        立即生成严重告警并上报
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '../../utils/request'

const orderId = ref<number | undefined>(undefined)
const defaultUavId = ref<number | undefined>(undefined)

const faultTypeOptions = [
  '动力电机过热异常',
  '传感器数据失步/指南针校准偏差',
  '低空通信测控链路中断',
  '低电量紧急迫降避险',
  '恶劣微气象遭遇强风切变',
  '其他突发机械/航电故障'
]
const faultTypeIndex = ref(0)

const uavOptions = ref<any[]>([])
const selectedIndex = ref(0)
const faultDesc = ref('')
const submitting = ref(false)

onLoad(async (options: any) => {
  if (options?.orderId) {
    orderId.value = Number(options.orderId)
  }
  if (options?.uavId) {
    defaultUavId.value = Number(options.uavId)
  }

  try {
    const list: any = await request.get('/api/v1/uav/list')
    if (list && list.length > 0) {
      uavOptions.value = list.map((u: any) => ({
        id: u.uavId,
        label: `${u.uavSn} - ${u.model}`
      }))
      if (defaultUavId.value) {
        const foundIdx = uavOptions.value.findIndex(item => item.id === defaultUavId.value)
        if (foundIdx >= 0) {
          selectedIndex.value = foundIdx
        }
      }
    }
  } catch (err) {}
})

const onFaultTypeChange = (e: any) => {
  faultTypeIndex.value = e.detail.value
}

const onUavChange = (e: any) => {
  selectedIndex.value = e.detail.value
}

const handleSubmit = async () => {
  if (!faultDesc.value.trim()) {
    uni.showToast({ title: '请输入故障详细描述', icon: 'none' })
    return
  }
  const chosen = uavOptions.value[selectedIndex.value]

  submitting.value = true
  try {
    await request.post('/api/v1/alarm/report', {
      orderId: orderId.value,
      uavId: chosen ? chosen.id : defaultUavId.value,
      faultType: faultTypeOptions[faultTypeIndex.value],
      description: faultDesc.value.trim()
    })
    uni.showToast({ title: '已生成严重告警，平台将联系处置', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 800)
  } catch (err: any) {
    uni.showToast({ title: err.message || '上报异常', icon: 'none' })
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
  margin-bottom: 12px;
}
.order-badge {
  background-color: #fee2e2;
  padding: 8px 12px;
  border-radius: 6px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}
.badge-lbl {
  font-size: 12px;
  color: #b91c1c;
}
.badge-val {
  font-size: 13px;
  font-weight: bold;
  color: #ef4444;
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
  box-sizing: border-box;
  background-color: #f1f5f9;
  padding: 12px;
  border-radius: 8px;
  font-size: 13px;
  color: #0f172a;
  height: 90px;
}
.submit-btn {
  background-color: #ef4444;
  color: #fff;
  font-size: 15px;
  font-weight: bold;
  border-radius: 8px;
  margin-top: 10px;
}
</style>
