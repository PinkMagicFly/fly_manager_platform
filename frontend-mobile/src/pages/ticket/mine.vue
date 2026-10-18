<template>
  <view class="ticket-page">
    <view class="tab-header">
      <view class="tab-btn" :class="{ active: currentTab === 'create' }" @click="currentTab = 'create'">
        📝 提报新诉求
      </view>
      <view class="tab-btn" :class="{ active: currentTab === 'list' }" @click="switchToList">
        📋 我的工单 ({{ ticketList.length }})
      </view>
    </view>

    <!-- 1. 新建工单表单 -->
    <view v-if="currentTab === 'create'" class="form-card">
      <view class="card-title">发起客户服务与异常诉求</view>

      <view class="form-item">
        <text class="label">诉求类别</text>
        <picker :range="typeOptions" range-key="label" :value="typeIndex" @change="onTypeChange">
          <view class="picker-box">
            {{ typeOptions[typeIndex]?.label || '点击选择工单类别' }}
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">诉求简述标题</text>
        <input v-model="title" class="input-box" placeholder="如：加急运单起飞延误咨询" />
      </view>

      <view class="form-item">
        <text class="label">诉求详细描述</text>
        <textarea v-model="content" class="textarea-box" placeholder="请详细描述问题或需协调事项，客服将尽快核实跟进" />
      </view>

      <button class="submit-btn" :loading="submitting" @click="handleSubmit">
        立即提交服务工单
      </button>
    </view>

    <!-- 2. 我的工单列表 -->
    <view v-else class="list-section">
      <view v-if="ticketList.length === 0" class="empty-tip">暂无提交的工单记录</view>
      <view v-for="ticket in ticketList" :key="ticket.ticketId" class="ticket-card">
        <view class="ticket-head">
          <view class="sn-box">
            <text class="sn">{{ ticket.ticketSn }}</text>
            <text class="type-tag" :class="`tag-type-${ticket.type}`">{{ ticket.typeLabel }}</text>
          </view>
          <text class="state-badge" :class="`state-${ticket.state}`">{{ ticket.stateLabel }}</text>
        </view>
        <view class="ticket-title">{{ ticket.title }}</view>
        <view class="ticket-content">{{ ticket.content }}</view>
        <view class="ticket-meta">
          <text class="meta-time">{{ ticket.createdAt }}</text>
          <text v-if="ticket.orderSn" class="meta-order">单号: {{ ticket.orderSn }}</text>
        </view>
        <view v-if="ticket.replies && ticket.replies.length > 0" class="replies-box">
          <view class="reply-title">💬 官方处理答复进度：</view>
          <view v-for="(reply, rIdx) in ticket.replies" :key="rIdx" class="reply-item">
            <view class="reply-by">{{ reply.by }} ({{ reply.role || '客服' }}) <text class="reply-time">{{ reply.at }}</text></view>
            <view class="reply-content">{{ reply.content }}</view>
          </view>
        </view>
        <view v-else-if="ticket.state === 0" class="pending-tip">
          客服专员正在受理中，请耐心等待官方答复...
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '../../utils/request'

const currentTab = ref<'create' | 'list'>('create')
const ticketList = ref<any[]>([])
const submitting = ref(false)

const typeOptions = [
  { value: 1, label: '业务咨询' },
  { value: 2, label: '配送时效投诉' },
  { value: 3, label: '设备异常报修' }
]
const typeIndex = ref(0)
const title = ref('')
const content = ref('')

const onTypeChange = (e: any) => {
  typeIndex.value = e.detail.value
}

const loadMyTickets = async () => {
  try {
    const list: any = await request.get('/api/v1/ticket/mine')
    ticketList.value = list || []
  } catch (err) {}
}

const switchToList = () => {
  currentTab.value = 'list'
  loadMyTickets()
}

const handleSubmit = async () => {
  if (!title.value.trim()) {
    uni.showToast({ title: '请输入工单标题', icon: 'none' })
    return
  }
  if (!content.value.trim()) {
    uni.showToast({ title: '请输入详细诉求内容', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    await request.post('/api/v1/ticket', {
      type: typeOptions[typeIndex.value].value,
      title: title.value.trim(),
      content: content.value.trim()
    })
    uni.showToast({ title: '工单提报成功', icon: 'success' })
    title.value = ''
    content.value = ''
    setTimeout(() => {
      switchToList()
    }, 500)
  } catch (err: any) {
    uni.showToast({ title: err.message || '提交失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

onLoad(() => {
  loadMyTickets()
})
</script>

<style scoped>
.ticket-page { padding: 16px; background-color: #f8fafc; min-height: 100vh; }
.tab-header { display: flex; background-color: #e2e8f0; border-radius: 8px; padding: 4px; margin-bottom: 16px; }
.tab-btn { flex: 1; text-align: center; padding: 10px 0; font-size: 14px; color: #64748b; border-radius: 6px; }
.tab-btn.active { background-color: #ffffff; color: #0284c7; font-weight: bold; }
.form-card { background-color: #ffffff; border-radius: 12px; padding: 20px; }
.card-title { font-size: 16px; font-weight: bold; color: #0f172a; margin-bottom: 16px; }
.form-item { margin-bottom: 16px; }
.label { font-size: 13px; color: #475569; margin-bottom: 6px; display: block; }
.picker-box, .input-box { background-color: #f1f5f9; padding: 12px; border-radius: 8px; font-size: 14px; color: #0f172a; }
.textarea-box { width: 100%; box-sizing: border-box; background-color: #f1f5f9; padding: 12px; border-radius: 8px; font-size: 14px; color: #0f172a; height: 100px; }
.submit-btn { background-color: #0284c7; color: #ffffff; font-size: 15px; font-weight: bold; border-radius: 8px; margin-top: 10px; }
.list-section { display: flex; flex-direction: column; gap: 12px; }
.empty-tip { text-align: center; color: #94a3b8; padding: 40px 0; font-size: 14px; }
.ticket-card { background-color: #ffffff; border-radius: 12px; padding: 16px; }
.ticket-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.sn-box { display: flex; align-items: center; gap: 8px; }
.sn { font-size: 13px; font-weight: bold; font-family: monospace; color: #334155; }
.type-tag { font-size: 11px; padding: 2px 6px; border-radius: 4px; }
.tag-type-1 { background-color: #e0f2fe; color: #0284c7; }
.tag-type-2 { background-color: #fee2e2; color: #ef4444; }
.tag-type-3 { background-color: #fef3c7; color: #d97706; }
.state-badge { font-size: 12px; font-weight: bold; padding: 2px 8px; border-radius: 6px; }
.state-0 { background-color: #fee2e2; color: #ef4444; }
.state-1 { background-color: #e0f2fe; color: #0284c7; }
.state-2 { background-color: #dcfce7; color: #16a34a; }
.ticket-title { font-size: 15px; font-weight: bold; color: #0f172a; margin-bottom: 6px; }
.ticket-content { font-size: 13px; color: #64748b; line-height: 1.5; margin-bottom: 8px; }
.ticket-meta { display: flex; justify-content: space-between; font-size: 11px; color: #94a3b8; }
.replies-box { margin-top: 12px; padding: 10px; background-color: #f8fafc; border-radius: 8px; border-left: 3px solid #0284c7; }
.reply-title { font-size: 12px; font-weight: bold; color: #0f172a; margin-bottom: 6px; }
.reply-item { margin-bottom: 8px; }
.reply-by { font-size: 11px; font-weight: bold; color: #0284c7; }
.reply-time { font-size: 10px; color: #94a3b8; font-weight: normal; margin-left: 6px; }
.reply-content { font-size: 12px; color: #334155; margin-top: 2px; }
.pending-tip { margin-top: 10px; font-size: 12px; color: #94a3b8; font-style: italic; }
</style>

