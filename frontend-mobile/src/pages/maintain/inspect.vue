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
        <view class="checklist-group">
          <view class="check-item" @click="toggleCheck('item1')">
            <checkbox value="item1" :checked="checkedItems.includes('item1')" color="#0284c7" />
            <text class="check-text">1. 机身机臂结构外观无裂纹、无变形损伤</text>
          </view>
          <view class="check-item" @click="toggleCheck('item2')">
            <checkbox value="item2" :checked="checkedItems.includes('item2')" color="#0284c7" />
            <text class="check-text">2. 螺旋桨紧固螺丝锁死，桨叶无豁口裂痕</text>
          </view>
          <view class="check-item" @click="toggleCheck('item3')">
            <checkbox value="item3" :checked="checkedItems.includes('item3')" color="#0284c7" />
            <text class="check-text">3. 动力电池插接牢固，金属触点无氧化烧灼</text>
          </view>
          <view class="check-item" @click="toggleCheck('item4')">
            <checkbox value="item4" :checked="checkedItems.includes('item4')" color="#0284c7" />
            <text class="check-text">4. 图传遥控通信链路通畅，现场GPS锁星>15颗</text>
          </view>
        </view>
      </view>

      <view class="form-item">
        <text class="label">巡检自检结论与备注说明</text>
        <textarea
          v-model="inspectNote"
          class="textarea-val"
          placeholder="请输入巡检自检结论，如：航前4项自检全部合格，动力充沛放行"
          :maxlength="200"
          :cursor-spacing="20"
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

const toggleCheck = (item: string) => {
  const idx = checkedItems.value.indexOf(item)
  if (idx > -1) {
    checkedItems.value.splice(idx, 1)
  } else {
    checkedItems.value.push(item)
  }
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
      inspectNote: inspectNote.value || '航前4项自检全部合格，动力充沛放行'
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
  padding: 18px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}
.form-title {
  font-size: 16px;
  font-weight: bold;
  color: #0f172a;
  margin-bottom: 18px;
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
  border: 1px solid #e2e8f0;
}
.readonly-val {
  padding: 12px;
  background-color: #f1f5f9;
  border-radius: 8px;
  font-size: 13px;
  color: #64748b;
  border: 1px solid #e2e8f0;
}
.checklist-group {
  display: flex;
  flex-direction: column;
}
.check-item {
  display: flex;
  align-items: center;
  padding: 10px 4px;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
}
.check-text {
  font-size: 13px;
  color: #1e293b;
  margin-left: 8px;
  line-height: 1.4;
  flex: 1;
}
.textarea-val {
  width: 100%;
  height: 80px;
  padding: 10px 12px;
  background-color: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 13px;
  color: #0f172a;
  line-height: 1.5;
  box-sizing: border-box;
}
.textarea-val:focus {
  border-color: #0284c7;
  background-color: #ffffff;
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


