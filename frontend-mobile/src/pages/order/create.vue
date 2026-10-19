<template>
  <view class="m-order-create-container">
    <view class="nav-bar">
      <text class="back-btn" @click="goBack">‹ 返回</text>
      <text class="page-title">低空寄件在线下单</text>
      <view style="width: 40px"></view>
    </view>

    <view class="form-scroll">
      <view class="card-box">
        <view class="card-title">起止服务网格与详细地址</view>

        
        <!-- 寄件端结构化输入 -->
        <view class="addr-block">
          <view class="addr-header">
            <view class="addr-title-group">
              <text class="badge bg-green">起</text>
              <text class="addr-label">寄件投递点</text>
            </view>
            <text class="btn-address-book" @click="openAddressSelector('sender')">📍 从地址簿选择</text>
          </view>
          <view class="region-selector-row">
            <RegionCascader
              v-model="senderRegionText"
              placeholder="请选择寄件省/市/区/街道"
              @change="onSenderRegionChange"
            />
          </view>
          <input
            v-model="senderDetail"
            class="detail-input"
            placeholder="详细地址 (如: 科技南十二路28号康佳研发大厦A座)"
            @blur="updateSenderCombined"
          />
          <view class="geo-coord-tag">
            <text>选点坐标: {{ form.senderLng.toFixed(6) }}, {{ form.senderLat.toFixed(6) }}</text>
          </view>
        </view>

        <!-- 收件端结构化输入 -->
        <view class="addr-block" style="margin-top: 14px">
          <view class="addr-header">
            <view class="addr-title-group">
              <text class="badge bg-orange">止</text>
              <text class="addr-label">收件目的地</text>
            </view>
            <text class="btn-address-book" @click="openAddressSelector('receiver')">📍 从地址簿选择</text>
          </view>
          <view class="region-selector-row">
            <RegionCascader
              v-model="receiverRegionText"
              placeholder="请选择收件省/市/区/街道"
              @change="onReceiverRegionChange"
            />
          </view>
          <input
            v-model="receiverDetail"
            class="detail-input"
            placeholder="详细地址 (如: 皇岗路5001号深业上城南区B座顶楼)"
            @blur="updateReceiverCombined"
          />
          <view class="geo-coord-tag">
            <text>选点坐标: {{ form.receiverLng.toFixed(6) }}, {{ form.receiverLat.toFixed(6) }}</text>
          </view>
        </view>

        <view class="preset-links">
          <text class="link-item" @click="usePreset(1)">一键填入: 南山科技园 → 福田深业</text>
          <text class="link-item" @click="usePreset(2)">一键填入: 龙岗大运 → 盐田保税</text>
        </view>
      </view>


      <view class="card-box">
        <view class="card-title">货物类别与重量</view>
        <view class="type-grid">
          <view
            v-for="t in goodsTypes"
            :key="t.val"
            class="type-pill"
            :class="{ active: form.goodsType === t.val }"
            @click="form.goodsType = t.val; triggerFeeCalc()"
          >
            {{ t.label }}
          </view>
        </view>
        <view class="field-row" style="margin-top: 12px">
          <text class="field-label">货物总重 (kg)</text>
          <input v-model="form.goodsWeight" type="digit" class="num-input" @blur="triggerFeeCalc" />
        </view>
        <view class="field-row" style="margin-top: 10px">
          <text class="field-label">加急配送 (1.3倍费率)</text>
          <switch :checked="form.urgentFlag === 1" @change="onUrgentChange" color="#0284c7" />
        </view>
      </view>

      <view class="card-box fee-card">
        <view class="card-title">费用预估试算明细</view>
        <view class="fee-line"><text>直线航程:</text><text>{{ feeResult?.distanceKm || 0 }} km</text></view>
        <view class="fee-line"><text>基础运费:</text><text>¥{{ feeResult?.feeBase || '0.00' }}</text></view>
        <view class="fee-line"><text>空域通道费:</text><text>¥{{ feeResult?.feeAirspace || '0.00' }}</text></view>
        <view class="fee-line"><text>加急附加费:</text><text class="text-orange">¥{{ feeResult?.feeUrgent || '0.00' }}</text></view>
        <view class="fee-line"><text>满减优惠:</text><text class="text-green">-¥{{ feeResult?.feeDiscount || '0.00' }}</text></view>
        <view class="fee-total-row">
          <text class="total-lbl">应付预估总额</text>
          <text class="total-val">¥{{ feeResult?.feeTotal || '0.00' }}</text>
        </view>
      </view>
    </view>

    <view class="bottom-bar">
      <view class="price-box">
        <text class="lbl">合计:</text>
        <text class="val">¥{{ feeResult?.feeTotal || '0.00' }}</text>
      </view>
      <button class="pay-btn" :loading="submitting" @click="handleCreateAndPay">立即模拟支付并下单</button>
    </view>

    <!-- 常用地址快速选择弹窗 -->
    <view v-if="addressModalVisible" class="addr-modal-wrapper">
      <view class="addr-modal-mask" @click="addressModalVisible = false"></view>
      <view class="addr-modal-content">
        <view class="addr-modal-header">
          <text class="addr-modal-title">从地址簿选择{{ selectTarget === 'sender' ? '寄件' : '收件' }}地址</text>
          <text class="addr-modal-close" @click="addressModalVisible = false">✕</text>
        </view>
        <scroll-view scroll-y class="addr-modal-scroll">
          <view v-if="addressBookList.length === 0" class="addr-empty">
            <text class="empty-txt">地址簿暂无保存的地址</text>
            <text class="link-manage" @click="navToManageAddress">去新建常用地址 ›</text>
          </view>
          <view
            v-for="item in addressBookList"
            :key="item.addressId"
            class="addr-select-item"
            @click="applySelectedAddress(item)"
          >
            <view class="item-top">
              <text class="item-name">{{ item.contactName }}</text>
              <text class="item-phone">{{ item.contactPhone }}</text>
              <text v-if="item.isDefault === 1" class="tag-default">默认</text>
            </view>
            <view class="item-detail">{{ item.fullAddress }}</view>
          </view>
        </scroll-view>
        <view class="addr-modal-footer">
          <button class="btn-manage-addr" @click="navToManageAddress">管理常用地址簿</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import request from '../../utils/request'
import RegionCascader from '../../components/RegionCascader.vue'

const submitting = ref(false)
const feeResult = ref<any>(null)

const addressModalVisible = ref(false)
const selectTarget = ref<'sender' | 'receiver'>('sender')
const addressBookList = ref<any[]>([])

const senderRegionText = ref('广东省深圳市南山区粤海街道')
const senderDetail = ref('科技南十二路28号康佳研发大厦A座')
const receiverRegionText = ref('广东省深圳市福田区莲花街道')
const receiverDetail = ref('皇岗路5001号深业上城南区B座顶楼')

const goodsTypes = [
  { val: 1, label: '普通文件' },
  { val: 2, label: '生物医疗' },
  { val: 3, label: '数码生鲜' },
  { val: 4, label: '高科工业' }
]

const form = reactive({
  senderAddr: '广东省深圳市南山区粤海街道科技南十二路28号康佳研发大厦A座',
  senderLng: 113.943000,
  senderLat: 22.540000,
  receiverAddr: '广东省深圳市福田区莲花街道皇岗路5001号深业上城南区B座顶楼',
  receiverLng: 114.062000,
  receiverLat: 22.553000,
  goodsType: 3,
  goodsWeight: '8.5',
  urgentFlag: 0,
  payWay: 1
})

const onSenderRegionChange = (data: any) => {
  senderRegionText.value = data.fullRegion
  form.senderLng = data.lng
  form.senderLat = data.lat
  updateSenderCombined()
  triggerFeeCalc()
}

const onReceiverRegionChange = (data: any) => {
  receiverRegionText.value = data.fullRegion
  form.receiverLng = data.lng
  form.receiverLat = data.lat
  updateReceiverCombined()
  triggerFeeCalc()
}

const updateSenderCombined = () => {
  form.senderAddr = `${senderRegionText.value || ''}${senderDetail.value || ''}`
}

const updateReceiverCombined = () => {
  form.receiverAddr = `${receiverRegionText.value || ''}${receiverDetail.value || ''}`
}

const usePreset = (type: number) => {
  if (type === 1) {
    senderRegionText.value = '广东省深圳市南山区粤海街道'
    senderDetail.value = '科技南十二路28号康佳研发大厦A座'
    form.senderLng = 113.943000
    form.senderLat = 22.540000

    receiverRegionText.value = '广东省深圳市福田区莲花街道'
    receiverDetail.value = '皇岗路5001号深业上城南区B座顶楼'
    form.receiverLng = 114.062000
    form.receiverLat = 22.553000
  } else {
    senderRegionText.value = '广东省深圳市龙岗区大运街道'
    senderDetail.value = '龙飞大道333号大运智能换电站'
    form.senderLng = 114.218000
    form.senderLat = 22.695000

    receiverRegionText.value = '广东省深圳市盐田区盐田街道'
    receiverDetail.value = '深盐路综合保税区保税大厦'
    form.receiverLng = 114.265000
    form.receiverLat = 22.578000
  }
  updateSenderCombined()
  updateReceiverCombined()
  triggerFeeCalc()
}

const onUrgentChange = (e: any) => {
  form.urgentFlag = e.detail.value ? 1 : 0
  triggerFeeCalc()
}

const triggerFeeCalc = async () => {
  try {
    const res: any = await request.post('/api/v1/order/calculate-fee', {
      senderLng: form.senderLng,
      senderLat: form.senderLat,
      receiverLng: form.receiverLng,
      receiverLat: form.receiverLat,
      goodsWeight: Number(form.goodsWeight) || 1.0,
      goodsType: form.goodsType,
      urgentFlag: form.urgentFlag
    })
    feeResult.value = res
  } catch (err) {}
}

const handleCreateAndPay = async () => {
  if (!form.goodsWeight || Number(form.goodsWeight) <= 0) {
    return uni.showToast({ title: '请输入合法重量', icon: 'none' })
  }
  updateSenderCombined()
  updateReceiverCombined()

  submitting.value = true
  try {
    // 提交前吸附预检（失败提示但不阻断）
    try {
      await Promise.all([
        request.get(`/api/v1/site/snap?lng=${form.senderLng}&lat=${form.senderLat}`),
        request.get(`/api/v1/site/snap?lng=${form.receiverLng}&lat=${form.receiverLat}`)
      ])
    } catch (e: any) {
      uni.showToast({ title: '提示: 地址落点较远，将由调度中枢二次吸附核验', icon: 'none' })
    }

    const created: any = await request.post('/api/v1/order/create', {
      senderAddr: form.senderAddr,
      senderLng: form.senderLng,
      senderLat: form.senderLat,
      receiverAddr: form.receiverAddr,
      receiverLng: form.receiverLng,
      receiverLat: form.receiverLat,
      goodsType: form.goodsType,
      goodsWeight: Number(form.goodsWeight),
      urgentFlag: form.urgentFlag,
      payWay: form.payWay
    })

    // 自动模拟支付
    await request.post(`/api/v1/order/${created.orderId}/pay`, {})

    uni.showToast({ title: '支付成功！进入待接单队列', icon: 'success' })
    setTimeout(() => {
      uni.navigateTo({ url: '/pages/order/list' })
    }, 600)
  } catch (err: any) {
    uni.showToast({ title: err.message || '下单支付失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

const openAddressSelector = async (target: 'sender' | 'receiver') => {
  selectTarget.value = target
  try {
    const res: any = await request.get('/api/v1/customer/address/list')
    addressBookList.value = Array.isArray(res) ? res : []
  } catch (e) {
    addressBookList.value = []
  }
  addressModalVisible.value = true
}

const applySelectedAddress = (addr: any) => {
  if (selectTarget.value === 'sender') {
    senderRegionText.value = addr.fullRegionText || `${addr.province} ${addr.city} ${addr.district} ${addr.street}`
    senderDetail.value = addr.detail
    form.senderLng = Number(addr.lng)
    form.senderLat = Number(addr.lat)
    updateSenderCombined()
  } else {
    receiverRegionText.value = addr.fullRegionText || `${addr.province} ${addr.city} ${addr.district} ${addr.street}`
    receiverDetail.value = addr.detail
    form.receiverLng = Number(addr.lng)
    form.receiverLat = Number(addr.lat)
    updateReceiverCombined()
  }
  addressModalVisible.value = false
  triggerFeeCalc()
  uni.showToast({ title: `已选用${selectTarget.value === 'sender' ? '寄件' : '收件'}地址`, icon: 'success' })
}

const navToManageAddress = () => {
  addressModalVisible.value = false
  uni.navigateTo({ url: `/pages/address/list?mode=select&target=${selectTarget.value}` })
}

const goBack = () => { uni.navigateBack() }

onMounted(() => {
  updateSenderCombined()
  updateReceiverCombined()
  triggerFeeCalc()
})

onShow(() => {
  const selected = uni.getStorageSync('selected_customer_address')
  if (selected) {
    try {
      const data = JSON.parse(selected)
      selectTarget.value = data.target
      applySelectedAddress(data.address)
      uni.removeStorageSync('selected_customer_address')
    } catch (e) {}
  }
})
</script>


<style scoped>
.m-order-create-container { display: flex; flex-direction: column; height: 100vh; background: #f1f5f9; }
.nav-bar { display: flex; justify-content: space-between; align-items: center; padding: 36px 14px 10px 14px; background: #0284c7; color: #fff; }
.back-btn { font-size: 16px; font-weight: bold; }
.page-title { font-size: 16px; font-weight: bold; }
.form-scroll { flex: 1; padding: 12px; overflow-y: auto; }
.card-box { background: #fff; border-radius: 8px; padding: 12px; margin-bottom: 12px; }
.card-title { font-size: 14px; font-weight: 700; color: #0f172a; margin-bottom: 10px; }
.addr-block { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 10px 12px; }
.addr-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.addr-title-group { display: flex; align-items: center; gap: 6px; }
.btn-address-book { font-size: 12px; color: #0284c7; background: #e0f2fe; padding: 3px 8px; border-radius: 4px; font-weight: bold; cursor: pointer; }
.addr-label { font-size: 13px; font-weight: 700; color: #0f172a; }
.region-selector-row { margin-bottom: 8px; }
.detail-input { background: #ffffff; border: 1px solid #cbd5e1; border-radius: 6px; padding: 0 10px; height: 38px; line-height: 38px; font-size: 13px; color: #0f172a; width: 100%; box-sizing: border-box; }
.geo-coord-tag { margin-top: 6px; font-size: 11px; color: #64748b; font-family: monospace; }


.addr-row { display: flex; align-items: center; gap: 8px; }
.badge { font-size: 11px; color: #fff; padding: 2px 6px; border-radius: 4px; font-weight: bold; }
.bg-green { background: #10b981; }
.bg-orange { background: #f59e0b; }
.input-text { flex: 1; background: #f8fafc; padding: 6px 10px; border-radius: 4px; font-size: 13px; }
.preset-links { display: flex; gap: 12px; margin-top: 10px; }
.link-item { font-size: 11px; color: #0284c7; text-decoration: underline; }
.type-grid { display: flex; gap: 8px; }
.type-pill { flex: 1; text-align: center; padding: 6px 0; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 12px; color: #334155; }
.type-pill.active { background: #e0f2fe; border-color: #0284c7; color: #0284c7; font-weight: bold; }
.field-row { display: flex; justify-content: space-between; align-items: center; }
.field-label { font-size: 13px; color: #475569; }
.num-input { width: 90px; text-align: right; background: #f8fafc; border-radius: 4px; padding: 4px 8px; font-size: 14px; font-weight: bold; }
.fee-card { background: #f8fafc; border: 1px solid #e2e8f0; }
.fee-line { display: flex; justify-content: space-between; font-size: 12px; color: #64748b; margin-bottom: 5px; }
.fee-total-row { display: flex; justify-content: space-between; align-items: center; border-top: 1px dashed #cbd5e1; padding-top: 8px; margin-top: 8px; }
.total-lbl { font-size: 14px; font-weight: bold; color: #0f172a; }
.total-val { font-size: 18px; font-weight: bold; color: #ef4444; }
.bottom-bar { display: flex; justify-content: space-between; align-items: center; padding: 10px 14px; background: #fff; border-top: 1px solid #e2e8f0; }
.price-box { display: flex; align-items: baseline; gap: 4px; }
.price-box .lbl { font-size: 12px; color: #64748b; }
.price-box .val { font-size: 20px; font-weight: 800; color: #ef4444; }
.pay-btn { background: #0284c7; color: #fff; font-size: 14px; font-weight: bold; padding: 0 18px; border-radius: 20px; margin: 0; }

/* 常用地址选择模态框 */
.addr-modal-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}
.addr-modal-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 10000;
}
.addr-modal-content {
  position: relative;
  z-index: 10001;
  width: 100%;
  background: #ffffff;
  border-radius: 16px 16px 0 0;
  padding: 16px 18px 24px;
  box-sizing: border-box;
}
.addr-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.addr-modal-title { font-size: 16px; font-weight: bold; color: #0f172a; }
.addr-modal-close { font-size: 18px; color: #94a3b8; padding: 4px 8px; cursor: pointer; }
.addr-modal-scroll { max-height: 280px; }
.addr-empty { text-align: center; padding: 30px 0; color: #94a3b8; font-size: 13px; display: flex; flex-direction: column; gap: 8px; align-items: center; }
.link-manage { color: #0284c7; text-decoration: underline; font-weight: bold; }
.addr-select-item {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 10px;
  cursor: pointer;
}
.addr-select-item:active { background: #f0f9ff; border-color: #0284c7; }
.item-top { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.item-name { font-size: 14px; font-weight: bold; color: #0f172a; }
.item-phone { font-size: 12px; color: #64748b; }
.tag-default { background: #e0f2fe; color: #0284c7; font-size: 10px; font-weight: bold; padding: 1px 5px; border-radius: 4px; }
.item-detail { font-size: 12px; color: #334155; line-height: 1.4; }
.addr-modal-footer { margin-top: 12px; }
.btn-manage-addr { background: #f1f5f9; color: #475569; font-size: 13px; border-radius: 8px; }
</style>
