<template>
  <view class="region-cascader-wrap">
    <view class="picker-trigger" @click="openPicker">
      <text v-if="selectedText" class="val-text">{{ selectedText }}</text>
      <text v-else class="placeholder-text">{{ placeholder || '请选择行政区划 (区/街道)' }}</text>
      <text class="arrow">›</text>
    </view>

    <!-- 弹窗选择层 -->
    <view v-if="visible" class="picker-modal">
      <view class="modal-mask" @click="closePicker"></view>
      <view class="modal-body">
        <view class="modal-header">
          <text class="cancel-btn" @click="closePicker">取消</text>
          <text class="modal-title">选择深圳市行政区与街道</text>
          <text class="confirm-btn" @click="confirmSelect">确定</text>
        </view>

        <view class="tabs-nav">
          <text class="tab-item" :class="{ active: currentTab === 0 }" @click="currentTab = 0">{{ selectedDistrict || '选择区县' }}</text>
          <text v-if="selectedDistrict" class="tab-item" :class="{ active: currentTab === 1 }" @click="currentTab = 1">{{ selectedStreet || '选择街道' }}</text>
        </view>

        <scroll-view scroll-y class="list-scroll">
          <view v-if="currentTab === 0" class="option-list">
            <view
              v-for="d in regionData.districts"
              :key="d.name"
              class="opt-item"
              :class="{ selected: selectedDistrict === d.name }"
              @click="onSelectDistrict(d)"
            >
              <text>{{ d.name }}</text>
              <text v-if="selectedDistrict === d.name" class="check-mark">✓</text>
            </view>
          </view>

          <view v-else class="option-list">
            <view
              v-for="s in currentDistrictStreets"
              :key="s.name"
              class="opt-item"
              :class="{ selected: selectedStreet === s.name }"
              @click="onSelectStreet(s)"
            >
              <text>{{ s.name }}</text>
              <text v-if="selectedStreet === s.name" class="check-mark">✓</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import regionJson from '../static/region-sz.json'

const props = defineProps<{
  placeholder?: string
  modelValue?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void
  (e: 'change', data: {
    province: string
    city: string
    district: string
    street: string
    fullRegion: string
    lng: number
    lat: number
  }): void
}>()

const regionData = regionJson
const visible = ref(false)
const currentTab = ref(0)

const selectedDistrict = ref('')
const selectedStreet = ref('')
const selectedStreetObj = ref<any>(null)

const selectedText = computed(() => {
  if (selectedDistrict.value && selectedStreet.value) {
    return `${regionData.province}${regionData.city}${selectedDistrict.value}${selectedStreet.value}`
  }
  return props.modelValue || ''
})

const currentDistrictStreets = computed(() => {
  const d = regionData.districts.find(item => item.name === selectedDistrict.value)
  return d ? d.streets : []
})

const openPicker = () => {
  visible.value = true
  if (!selectedDistrict.value && regionData.districts.length > 0) {
    currentTab.value = 0
  }
}

const closePicker = () => {
  visible.value = false
}

const onSelectDistrict = (d: any) => {
  selectedDistrict.value = d.name
  selectedStreet.value = ''
  selectedStreetObj.value = null
  currentTab.value = 1
}

const onSelectStreet = (s: any) => {
  selectedStreet.value = s.name
  selectedStreetObj.value = s
  confirmSelect()
}

const confirmSelect = () => {
  if (!selectedDistrict.value || !selectedStreet.value || !selectedStreetObj.value) {
    uni.showToast({ title: '请选择完整的街道', icon: 'none' })
    return
  }

  const fullRegion = `${regionData.province}${regionData.city}${selectedDistrict.value}${selectedStreet.value}`
  emit('update:modelValue', fullRegion)
  emit('change', {
    province: regionData.province,
    city: regionData.city,
    district: selectedDistrict.value,
    street: selectedStreet.value,
    fullRegion,
    lng: selectedStreetObj.value.lng,
    lat: selectedStreetObj.value.lat
  })
  closePicker()
}
</script>

<style scoped>
.region-cascader-wrap { width: 100%; }
.picker-trigger { display: flex; justify-content: space-between; align-items: center; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding: 6px 10px; font-size: 13px; }
.val-text { color: #0f172a; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.placeholder-text { color: #94a3b8; }
.arrow { color: #cbd5e1; font-size: 14px; margin-left: 4px; }
.picker-modal { position: fixed; top: 0; left: 0; right: 0; bottom: 0; z-index: 9999; display: flex; flex-direction: column; justify-content: flex-end; }
.modal-mask { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0, 0, 0, 0.5); }
.modal-body { position: relative; background: #fff; border-radius: 14px 14px 0 0; padding-bottom: 20px; max-height: 65vh; display: flex; flex-direction: column; z-index: 10000; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid #f1f5f9; }
.modal-title { font-size: 14px; font-weight: 700; color: #0f172a; }
.cancel-btn { font-size: 13px; color: #64748b; }
.confirm-btn { font-size: 13px; color: #0284c7; font-weight: 600; }
.tabs-nav { display: flex; gap: 14px; padding: 8px 16px; border-bottom: 1px solid #f1f5f9; }
.tab-item { font-size: 12px; color: #64748b; padding-bottom: 3px; }
.tab-item.active { color: #0284c7; font-weight: 700; border-bottom: 2px solid #0284c7; }
.list-scroll { max-height: 40vh; padding: 4px 16px; }
.opt-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid #f8fafc; font-size: 13px; color: #334155; }
.opt-item.selected { color: #0284c7; font-weight: 700; }
.check-mark { color: #0284c7; font-weight: bold; }
</style>

