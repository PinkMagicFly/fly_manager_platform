<template>
  <view class="m-inspect-container">
    <view class="card-box">
      <view class="form-title">无人机航前 / 日常安全巡检打卡</view>

      <view class="form-item">
        <text class="label">巡检目标无人机</text>
        <picker :range="uavOptions" range-key="label" :value="selectedIndex" @change="onUavChange">
          <view class="picker-val">
            {{ uavOptions[selectedIndex]?.label || '点击选择执飞无人机' }}
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">当前起降场站</text>
        <view class="readonly-val">南山科技园低空综合起降枢纽 (SITE-SZ-001)</view>
      </view>

      <view class="form-item">
        <text class="label">法定安全自检核查清单 (必检4项)</text>
        <checkbox-group @change="onCheckChange">
          <label class="check-item">
            <checkbox value="item1" :checked="checkedItems.includes('item1')" color="#0284c7" />
            <text class="check-text">1. 机身机臂结构外观无裂纹、无变形损伤</text>
          </label>
          <label class="check-item">
            <checkbox value="item2" :checked="checkedItems.includes('item2')" color="#0284c7" />
            <text class="check-text">2. 螺旋桨紧固螺丝锁死，桨叶无豁口裂痕</text>
          </label>
          <label class="check-item">
            <checkbox value="item3" :checked="checkedItems.includes('item3')" color="#0284c7" />
            <text class="check-text">3. 动力电池插接牢固，金属触点无氧化烧灼</text>
          </label>
          <label class="check-item">
            <checkbox value="item4" :checked="checkedItems.includes('item4')" color="#0284c7" />
            <text class="check-text">4. 图传遥控通信链路通畅，现场GPS锁星>15颗</text>
          </label>
        </checkbox-group>
      </view>

      <view class="form-item">
        <text class="label">巡检自检结论与备注</text>
        <input
          v-model="inspectNote"
          class="input-val"
          placeholder="如：航前4项自检全部合格，动力充沛放行"
        />
      </view>

      <button class="submit-btn" :loading="submitting" @click="handleInspectSubmit">
        完成巡检打卡并提交后台
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
const checkedItems = ref<string[]>(['item1', 'item2', 'item3', 'item4'])
const inspectNote = ref('航前4项安全自检全部合格，动力充沛放行')
const submitting = ref(false)

onLoad(async () => {
  try {
    const list: any = await request.get('/api/v1/uav/list')
    if (list && list.length > 0) {
      uavOptions.value = list.map((u: any) => ({
        id: u.uavId,
        label: `${u.uavSn} - ${u.model} (电量:${u.batteryPct}%)`
      }))
    }
  } catch (err) {}
})

const onUavChange = (e: any) => {
  selectedIndex.value = e.detail.value
}

const onCheckChange = (e: any) => {
  checkedItems.value = e.detail.value
}

const handleInspectSubmit = async () => {
  if (checkedItems.value.length < 4) {
    uni.showToast({ title: '4项安全清单必须全部勾选合格', icon: 'none' })
    return
  }
  const chosen = uavOptions.value[selectedIndex.value]
  if (!chosen) {
    uni.showToast({ title: '请选择无人机', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    await request.post('/api/v1/uav/inspect', {
      uavId: chosen.id,
      siteId: 1,
      itemsSummary: '机身外观、螺旋桨紧固、动力电池插接、图传GPS定位自检合格',
      inspectNote: inspectNote.value
    })
    uni.showToast({ title: '巡检打卡成功，已同步后台', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 700)
  } catch (err: any) {
    uni.showToast({ title: err.message || '巡检上报异常', icon: 'none' })
  } finally {
    submitting.value = false
  }
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
  margin-bottom: 18px;
}
.label {
  font-size: 13px;
  font-weight: bold;
  color: #334155;
  margin-bottom: 8px;
  display: block;
}
.picker-val {
  padding: 12px;
  background-color: #f1f5f9;
  border-radius: 8px;
  font-size: 13px;
  color: #0284c7;
  font-weight: 500;
}
.readonly-val {
  padding: 12px;
  background-color: #f1f5f9;
  border-radius: 8px;
  font-size: 13px;
  color: #64748b;
}
.input-val {
  width: 100%;
  padding: 10px 12px;
  background-color: #f1f5f9;
  border-radius: 8px;
  font-size: 13px;
  box-sizing: border-box;
}
.check-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f8fafc;
}
.check-text {
  font-size: 13px;
  color: #1e293b;
  margin-left: 8px;
  line-height: 1.4;
}
.submit-btn {
  margin-top: 24px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #fff;
  font-size: 15px;
  font-weight: bold;
  border-radius: 8px;
}
</style>

