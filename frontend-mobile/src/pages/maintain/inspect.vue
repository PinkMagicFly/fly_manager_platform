<template>
  <view class="m-inspect-container">
    <view class="card-box">
      <view class="form-title">无人机航前/日常巡检打卡</view>

      <view class="form-item">
        <text class="label">巡检场站</text>
        <view class="readonly-val">南山科技园低空综合起降枢纽 (SITE-SZ-001)</view>
      </view>

      <view class="form-item">
        <text class="label">自检核查清单</text>
        <checkbox-group @change="onCheckChange">
          <label class="check-item">
            <checkbox value="item1" checked color="#0284c7" />
            <text class="check-text">机身机臂结构外观无裂纹、无变形</text>
          </label>
          <label class="check-item">
            <checkbox value="item2" checked color="#0284c7" />
            <text class="check-text">螺旋桨紧固螺丝锁死，无缺角裂痕</text>
          </label>
          <label class="check-item">
            <checkbox value="item3" checked color="#0284c7" />
            <text class="check-text">动力电池插接牢固，接触弹片无烧灼</text>
          </label>
          <label class="check-item">
            <checkbox value="item4" checked color="#0284c7" />
            <text class="check-text">图传遥控通信链路正常，GPS锁星>15颗</text>
          </label>
        </checkbox-group>
      </view>

      <button class="submit-btn" :loading="submitting" @click="handleInspectSubmit">
        完成巡检打卡
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const checkedItems = ref<string[]>(['item1', 'item2', 'item3', 'item4'])
const submitting = ref(false)

const onCheckChange = (e: any) => {
  checkedItems.value = e.detail.value
}

const handleInspectSubmit = () => {
  if (checkedItems.value.length < 4) {
    uni.showToast({ title: '必须全部检查合格方可放行', icon: 'none' })
    return
  }
  submitting.value = true
  setTimeout(() => {
    submitting.value = false
    uni.showToast({ title: '巡检打卡成功，设备正常', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 600)
  }, 400)
}
</script>

<style scoped>
.m-inspect-container {
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
  margin-bottom: 20px;
}
.label {
  font-size: 13px;
  color: #475569;
  margin-bottom: 8px;
  display: block;
}
.readonly-val {
  padding: 12px;
  background-color: #f1f5f9;
  border-radius: 8px;
  font-size: 13px;
  color: #0f172a;
}
.check-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f8fafc;
}
.check-text {
  font-size: 13px;
  color: #334155;
  margin-left: 8px;
}
.submit-btn {
  margin-top: 20px;
  background-color: #10b981;
  color: #fff;
  font-size: 15px;
  font-weight: bold;
  border-radius: 8px;
}
</style>
