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
        <!-- 历史答复与双向沟通流水 -->
        <view v-if="ticket.replies && ticket.replies.length > 0" class="replies-box">
          <view class="reply-title">💬 沟通与处理进展：</view>
          <view
            v-for="(reply, rIdx) in ticket.replies"
            :key="rIdx"
            class="reply-item"
            :class="{ 'reply-customer': reply.role === '客户' }"
          >
            <view class="reply-header">
              <text class="role-badge" :class="reply.role === '客户' ? 'badge-customer' : (reply.role === '官方专员' ? 'badge-dispatch' : 'badge-staff')">
                {{ reply.role === '客户' ? '我 (客户)' : (reply.role === '官方专员' ? '工单分派' : '官方客服') }}
              </text>
              <text class="reply-user">{{ reply.by }}</text>
              <text class="reply-time">{{ reply.at }}</text>
            </view>
            <view class="reply-content">{{ reply.content }}</view>
          </view>
        </view>
        <view v-else-if="ticket.state === 0" class="pending-tip">
          客服专员正在受理中，请耐心等待官方答复...
        </view>

        <!-- 继续沟通 / 追问回答操作区（工单未关闭时可用） -->
        <view v-if="ticket.state !== 2" class="follow-up-section">
          <view v-if="!openReplyIdMap[ticket.ticketId]" class="btn-wrap">
            <button class="btn-open-reply" @click="toggleReply(ticket.ticketId)">
              💬 继续沟通 / 补充回答
            </button>
          </view>
          <view v-else class="reply-editor-box">
            <textarea
              v-model="replyTextMap[ticket.ticketId]"
              class="reply-textarea"
              placeholder="请在此输入您想继续回答或向客服补充说明的事项..."
              maxlength="500"
            />
            <view class="reply-actions">
              <button size="mini" class="btn-action-cancel" @click="toggleReply(ticket.ticketId)">取消</button>
              <button
                size="mini"
                type="primary"
                class="btn-action-send"
                :loading="sendingTicketId === ticket.ticketId"
                @click="handleSendReply(ticket.ticketId)"
              >
                发送回答
              </button>
            </view>
          </view>
        </view>
        <view v-else class="closed-tip">
          ✅ 本工单已妥善处理并办结关闭
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

const openReplyIdMap = ref<Record<number, boolean>>({})
const replyTextMap = ref<Record<number, string>>({})
const sendingTicketId = ref<number | null>(null)

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

const toggleReply = (ticketId: number) => {
  openReplyIdMap.value[ticketId] = !openReplyIdMap.value[ticketId]
  if (!replyTextMap.value[ticketId]) {
    replyTextMap.value[ticketId] = ''
  }
}

const handleSendReply = async (ticketId: number) => {
  const text = replyTextMap.value[ticketId]?.trim()
  if (!text) {
    uni.showToast({ title: '请输入回答或补充内容', icon: 'none' })
    return
  }

  sendingTicketId.value = ticketId
  try {
    await request.put(`/api/v1/ticket/${ticketId}/reply`, { content: text })
    uni.showToast({ title: '回答已发送给客服', icon: 'success' })
    replyTextMap.value[ticketId] = ''
    openReplyIdMap.value[ticketId] = false
    await loadMyTickets()
  } catch (err: any) {
    uni.showToast({ title: err.message || '发送失败', icon: 'none' })
  } finally {
    sendingTicketId.value = null
  }
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
.reply-title { font-size: 12px; font-weight: bold; color: #0f172a; margin-bottom: 8px; }
.reply-item { margin-bottom: 10px; padding: 8px 10px; background-color: #ffffff; border-radius: 6px; border: 1px solid #e2e8f0; }
.reply-customer { background-color: #f0fdf4; border-color: #bbf7d0; }
.reply-header { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.role-badge { font-size: 10px; font-weight: bold; padding: 1px 5px; border-radius: 4px; }
.badge-customer { background-color: #16a34a; color: #ffffff; }
.badge-staff { background-color: #0284c7; color: #ffffff; }
.badge-dispatch { background-color: #f59e0b; color: #ffffff; }
.reply-user { font-size: 11px; font-weight: bold; color: #334155; }
.reply-time { font-size: 10px; color: #94a3b8; margin-left: auto; }
.reply-content { font-size: 12px; color: #334155; line-height: 1.5; }
.pending-tip { margin-top: 10px; font-size: 12px; color: #94a3b8; font-style: italic; }

.follow-up-section { margin-top: 14px; padding-top: 10px; border-top: 1px dashed #e2e8f0; }
.btn-wrap { display: flex; justify-content: flex-end; }
.btn-open-reply { font-size: 12px; color: #0284c7; background-color: #e0f2fe; border-radius: 6px; padding: 4px 12px; line-height: 1.8; margin: 0; }
.reply-editor-box { background-color: #f8fafc; padding: 10px; border-radius: 8px; border: 1px solid #cbd5e1; }
.reply-textarea { width: 100%; box-sizing: border-box; background-color: #ffffff; padding: 8px; border-radius: 6px; font-size: 13px; color: #0f172a; height: 70px; border: 1px solid #e2e8f0; }
.reply-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
.btn-action-cancel { margin: 0; font-size: 12px; color: #64748b; background-color: #e2e8f0; }
.btn-action-send { margin: 0; font-size: 12px; font-weight: bold; background-color: #0284c7; color: #ffffff; }
.closed-tip { margin-top: 12px; font-size: 12px; color: #16a34a; text-align: center; background-color: #f0fdf4; padding: 6px; border-radius: 6px; }
</style>

