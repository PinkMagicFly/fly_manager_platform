<template>
  <view class="address-page">
    <view class="nav-bar">
      <text class="back-btn" @click="goBack">‹ 返回</text>
      <text class="page-title">{{ isSelectMode ? '选择常用地址' : '常用寄收地址簿' }}</text>
      <view style="width: 40px"></view>
    </view>

    <!-- 地址列表 -->
    <scroll-view scroll-y class="list-scroll">
      <view v-if="addressList.length === 0" class="empty-box">
        <text class="empty-icon">📍</text>
        <text class="empty-text">暂无常用地址，点击下方按钮添加</text>
      </view>

      <view
        v-for="addr in addressList"
        :key="addr.addressId"
        class="address-card"
        :class="{ 'card-selectable': isSelectMode }"
        @click="onSelectAddress(addr)"
      >
        <view class="card-top">
          <view class="name-phone">
            <text class="name">{{ addr.contactName }}</text>
            <text class="phone">{{ addr.contactPhone }}</text>
            <text v-if="addr.isDefault === 1" class="tag-default">默认</text>
          </view>
          <view v-if="isSelectMode" class="select-badge">点击选用</view>
        </view>

        <view class="detail-addr">{{ addr.fullAddress }}</view>
        <view class="coord-info">
          选点坐标: {{ Number(addr.lng).toFixed(6) }}, {{ Number(addr.lat).toFixed(6) }}
        </view>

        <view class="card-actions" @click.stop>
          <view class="action-left" @click="handleSetDefault(addr)">
            <radio :checked="addr.isDefault === 1" color="#0284c7" style="transform: scale(0.7);" />
            <text class="act-text">{{ addr.isDefault === 1 ? '默认地址' : '设为默认' }}</text>
          </view>
          <view class="action-right">
            <text class="btn-op text-edit" @click="openEditDialog(addr)">编辑</text>
            <text class="btn-op text-del" @click="handleDelete(addr)">删除</text>
          </view>
        </view>
      </view>
      <view style="height: 80px;"></view>
    </scroll-view>

    <!-- 底部新建按钮 -->
    <view class="footer-bar">
      <button class="btn-create-addr" @click="openCreateDialog">
        + 新建常用地址
      </button>
    </view>

    <!-- 新建/编辑地址弹窗 -->
    <view v-if="dialogVisible" class="modal-mask" @click="dialogVisible = false">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">{{ editId ? '编辑常用地址' : '新增常用地址' }}</text>
          <text class="modal-close" @click="dialogVisible = false">✕</text>
        </view>

        <view class="form-body">
          <view class="form-item">
            <text class="form-label">联系人姓名</text>
            <input v-model="form.contactName" class="form-input" placeholder="如：张经理 / 业务部" />
          </view>

          <view class="form-item">
            <text class="form-label">联系人电话</text>
            <input v-model="form.contactPhone" type="number" class="form-input" placeholder="11位手机号码" />
          </view>

          <view class="form-item">
            <text class="form-label">省/市/区/街道</text>
            <RegionCascader
              v-model="regionText"
              placeholder="点击选择所在行政区与街道"
              @change="onRegionChange"
            />
          </view>

          <view class="form-item">
            <text class="form-label">详细楼栋门牌地址</text>
            <input v-model="form.detail" class="form-input" placeholder="如：科技南十二路28号A座18层" />
          </view>

          <view class="form-item-switch">
            <text class="form-label">设为默认常用地址</text>
            <switch :checked="form.isDefault === 1" color="#0284c7" @change="onDefaultSwitchChange" />
          </view>
        </view>

        <view class="modal-footer">
          <button class="btn-cancel" @click="dialogVisible = false">取消</button>
          <button class="btn-confirm" :loading="saving" @click="submitForm">保存地址</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '../../utils/request'
import RegionCascader from '../../components/RegionCascader.vue'

const addressList = ref<any[]>([])
const isSelectMode = ref(false)
const selectTarget = ref<'sender' | 'receiver'>('sender')

const dialogVisible = ref(false)
const saving = ref(false)
const editId = ref<number | null>(null)
const regionText = ref('')

const form = reactive({
  contactName: '',
  contactPhone: '',
  province: '广东省',
  city: '深圳市',
  district: '南山区',
  street: '粤海街道',
  lng: 113.943000,
  lat: 22.540000,
  detail: '',
  isDefault: 0
})

const goBack = () => {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
  } else {
    uni.reLaunch({ url: '/pages/work/index' })
  }
}

const loadAddresses = async () => {
  try {
    const res: any = await request.get('/api/v1/customer/address/list')
    addressList.value = Array.isArray(res) ? res : []
  } catch (e) {
    addressList.value = []
  }
}

const onRegionChange = (info: any) => {
  if (info) {
    form.province = info.province || '广东省'
    form.city = info.city || '深圳市'
    form.district = info.district || '南山区'
    form.street = info.street || '粤海街道'
    if (info.lng && info.lat) {
      form.lng = info.lng
      form.lat = info.lat
    }
  }
}

const onDefaultSwitchChange = (e: any) => {
  form.isDefault = e.detail.value ? 1 : 0
}

const openCreateDialog = () => {
  editId.value = null
  form.contactName = ''
  form.contactPhone = ''
  form.province = '广东省'
  form.city = '深圳市'
  form.district = '南山区'
  form.street = '粤海街道'
  form.lng = 113.943000
  form.lat = 22.540000
  form.detail = ''
  form.isDefault = addressList.value.length === 0 ? 1 : 0
  regionText.value = '广东省 深圳市 南山区 粤海街道'
  dialogVisible.value = true
}

const openEditDialog = (addr: any) => {
  editId.value = addr.addressId
  form.contactName = addr.contactName
  form.contactPhone = addr.contactPhone
  form.province = addr.province
  form.city = addr.city
  form.district = addr.district
  form.street = addr.street
  form.lng = Number(addr.lng)
  form.lat = Number(addr.lat)
  form.detail = addr.detail
  form.isDefault = addr.isDefault
  regionText.value = `${addr.province} ${addr.city} ${addr.district} ${addr.street}`
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!form.contactName.trim()) {
    uni.showToast({ title: '请输入联系人姓名', icon: 'none' })
    return
  }
  if (!form.contactPhone.trim() || form.contactPhone.length < 7) {
    uni.showToast({ title: '请输入有效联系电话', icon: 'none' })
    return
  }
  if (!form.detail.trim()) {
    uni.showToast({ title: '请输入详细楼栋门牌', icon: 'none' })
    return
  }

  saving.value = true
  try {
    if (editId.value) {
      await request.put(`/api/v1/customer/address/${editId.value}`, form)
      uni.showToast({ title: '常用地址修改成功', icon: 'success' })
    } else {
      await request.post('/api/v1/customer/address', form)
      uni.showToast({ title: '常用地址添加成功', icon: 'success' })
    }
    dialogVisible.value = false
    await loadAddresses()
  } catch (err: any) {
    uni.showToast({ title: err.message || '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

const handleSetDefault = async (addr: any) => {
  if (addr.isDefault === 1) return
  try {
    await request.put(`/api/v1/customer/address/${addr.addressId}/default`)
    uni.showToast({ title: '已设为默认地址', icon: 'success' })
    await loadAddresses()
  } catch (err: any) {
    uni.showToast({ title: err.message || '设置失败', icon: 'none' })
  }
}

const handleDelete = (addr: any) => {
  uni.showModal({
    title: '确认删除',
    content: `确定要删除常用地址 [${addr.contactName} - ${addr.detail}] 吗？`,
    confirmText: '删除',
    confirmColor: '#ef4444',
    success: async (res) => {
      if (res.confirm) {
        try {
          await request.delete(`/api/v1/customer/address/${addr.addressId}`)
          uni.showToast({ title: '已删除地址', icon: 'success' })
          await loadAddresses()
        } catch (err: any) {
          uni.showToast({ title: err.message || '删除失败', icon: 'none' })
        }
      }
    }
  })
}

const onSelectAddress = (addr: any) => {
  if (!isSelectMode.value) return
  uni.setStorageSync('selected_customer_address', JSON.stringify({
    target: selectTarget.value,
    address: addr
  }))
  uni.navigateBack()
}

onLoad((options: any) => {
  if (options && options.mode === 'select') {
    isSelectMode.value = true
    selectTarget.value = options.target || 'sender'
  }
  loadAddresses()
})
</script>

<style scoped>
.address-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f8fafc;
}

.nav-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 44px 16px 12px;
  background: #0284c7;
  color: #fff;
}
.back-btn { font-size: 15px; cursor: pointer; }
.page-title { font-size: 16px; font-weight: bold; }

.list-scroll {
  flex: 1;
  padding: 12px 16px;
  box-sizing: border-box;
}

.empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  gap: 10px;
}
.empty-icon { font-size: 40px; }
.empty-text { font-size: 14px; color: #94a3b8; }

.address-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}
.card-selectable {
  border: 1px dashed #0284c7;
  cursor: pointer;
}
.card-selectable:active {
  background-color: #f0f9ff;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.name-phone {
  display: flex;
  align-items: center;
  gap: 8px;
}
.name { font-size: 15px; font-weight: bold; color: #0f172a; }
.phone { font-size: 13px; color: #64748b; }
.tag-default {
  background: #e0f2fe;
  color: #0284c7;
  font-size: 10px;
  font-weight: bold;
  padding: 1px 6px;
  border-radius: 4px;
}
.select-badge {
  font-size: 11px;
  color: #0284c7;
  background: #e0f2fe;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: bold;
}

.detail-addr {
  font-size: 13px;
  color: #334155;
  line-height: 1.5;
  margin-bottom: 6px;
}
.coord-info {
  font-size: 11px;
  color: #94a3b8;
  font-family: monospace;
  margin-bottom: 10px;
}

.card-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
}
.action-left {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}
.act-text { font-size: 12px; color: #64748b; }
.action-right {
  display: flex;
  gap: 16px;
}
.btn-op { font-size: 12px; cursor: pointer; }
.text-edit { color: #0284c7; }
.text-del { color: #ef4444; }

.footer-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 16px;
  background: #ffffff;
  border-top: 1px solid #e2e8f0;
}
.btn-create-addr {
  background: #0284c7;
  color: #ffffff;
  font-size: 15px;
  font-weight: bold;
  border-radius: 8px;
  border: none;
}

.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: flex-end;
  z-index: 999;
}
.modal-content {
  width: 100%;
  background: #ffffff;
  border-radius: 16px 16px 0 0;
  padding: 16px 20px 24px;
  box-sizing: border-box;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.modal-title { font-size: 16px; font-weight: bold; color: #0f172a; }
.modal-close { font-size: 18px; color: #94a3b8; padding: 4px 8px; cursor: pointer; }

.form-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.form-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-label { font-size: 12px; color: #475569; }
.form-input {
  background: #f1f5f9;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
  color: #0f172a;
}
.form-item-switch {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
}

.modal-footer {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}
.btn-cancel {
  flex: 1;
  background: #e2e8f0;
  color: #475569;
  font-size: 14px;
  border-radius: 8px;
}
.btn-confirm {
  flex: 2;
  background: #0284c7;
  color: #ffffff;
  font-size: 14px;
  font-weight: bold;
  border-radius: 8px;
}
</style>

