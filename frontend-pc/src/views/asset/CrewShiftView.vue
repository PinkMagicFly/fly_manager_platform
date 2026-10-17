<template>
  <div class="crew-shift-container">
    <!-- 机组资质台账 -->
    <el-card class="box-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon><UserFilled /></el-icon>
            <span class="title">机组人员执业资质台账与临期预警</span>
          </div>
          <el-button type="primary" size="small" @click="openStaffDialog">
            <el-icon><Plus /></el-icon> 登记机组人员
          </el-button>
        </div>
      </template>

      <el-table :data="staffList" stripe border size="small" style="width: 100%">
        <el-table-column prop="crewId" label="档案ID" width="75" align="center" />
        <el-table-column prop="realName" label="机组姓名" width="110" align="center">
          <template #default="{ row }"><b>{{ row.realName }}</b></template>
        </el-table-column>
        <el-table-column prop="crewTypeDesc" label="岗位类别" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.crewType === 1 ? 'primary' : 'warning'">{{ row.crewTypeDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="licenseNo" label="执照编号" min-width="160" align="center">
          <template #default="{ row }">
            <span class="license-code">{{ row.licenseNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="有效期至" width="120" align="center">
          <template #default="{ row }">{{ row.licenseExpire }}</template>
        </el-table-column>
        <el-table-column label="资质预警" width="140" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isExpired" type="danger" effect="dark" size="small">
              <el-icon><CircleCloseFilled /></el-icon> 已过期
            </el-tag>
            <el-tag v-else-if="row.isExpiringSoon" type="warning" effect="dark" size="small">
              <el-icon><WarningFilled /></el-icon> 临期 (剩 {{ row.expireDaysLeft }} 天)
            </el-tag>
            <el-tag v-else type="success" size="small">正常有效</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="flyHours" label="安全执飞" width="105" align="right">
          <template #default="{ row }"><b>{{ row.flyHours }}</b> 小时</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 周排班矩阵 -->
    <el-card class="box-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon><Calendar /></el-icon>
            <span class="title">机组周执勤排班矩阵 (时空防冲守护)</span>
          </div>
          <el-button type="success" size="small" @click="openShiftDialog">
            <el-icon><Plus /></el-icon> 录入执勤排班
          </el-button>
        </div>
      </template>

      <el-table :data="weekMatrix" border stripe style="width: 100%">
        <el-table-column prop="realName" label="执勤人员" width="120" fixed align="center">
          <template #default="{ row }">
            <div style="font-weight: bold">{{ row.realName }}</div>
            <div style="font-size: 11px; color: #64748b">{{ row.crewTypeDesc }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="totalWeekHours" label="周累计工时" width="105" fixed align="center">
          <template #default="{ row }">
            <el-tag type="info"><b>{{ row.totalWeekHours }}h</b></el-tag>
          </template>
        </el-table-column>

        <el-table-column
          v-for="(day, index) in weekDays"
          :key="index"
          :label="day.label + ' (' + day.date + ')'"
          min-width="135"
          align="center"
        >
          <template #default="{ row }">
            <div class="shift-cell">
              <div
                v-for="shift in getShiftsForDay(row.shifts, day.date)"
                :key="shift.shiftId"
                class="shift-badge"
                :class="'shift-state-' + shift.shiftState"
              >
                <div class="shift-time">{{ shift.startTime }} - {{ shift.endTime }}</div>
                <div class="shift-act">
                  <span class="badge-status">{{ shift.shiftStateDesc }}</span>
                  <el-button
                    v-if="shift.shiftState !== 4"
                    type="danger"
                    link
                    size="small"
                    style="font-size: 11px"
                    @click="handleCancelShift(shift.shiftId)"
                  >
                    调休
                  </el-button>
                </div>
              </div>
              <div v-if="getShiftsForDay(row.shifts, day.date).length === 0" class="no-shift">—</div>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>


    <!-- 弹窗 1：录入排班 -->
    <el-dialog v-model="shiftDialogVisible" title="录入机组执勤排班 (防重守卫)" width="450px">
      <el-form label-position="top">
        <el-form-item label="执勤机组人员" required>
          <el-select v-model="shiftForm.crewId" placeholder="选择人员" style="width: 100%">
            <el-option
              v-for="s in staffList"
              :key="s.crewId"
              :label="s.realName + ' (' + s.crewTypeDesc + (s.isExpired ? ' - 执照已过期' : '') + ')'"
              :value="s.crewId"
              :disabled="s.isExpired"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排班日期" required>
          <el-date-picker v-model="shiftForm.shiftDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="执勤起始时刻" required>
          <el-time-picker v-model="shiftForm.startTime" format="HH:mm:ss" value-format="HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="执勤截止时刻" required>
          <el-time-picker v-model="shiftForm.endTime" format="HH:mm:ss" value-format="HH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shiftDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="shiftLoading" @click="handleSaveShift">确认排班</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 2：登记机组档案 -->
    <el-dialog v-model="staffDialogVisible" title="登记机组人员档案" width="420px">
      <el-form label-position="top">
        <el-form-item label="真实姓名" required>
          <el-input v-model="staffForm.realName" placeholder="如：李机长" />
        </el-form-item>
        <el-form-item label="岗位类别" required>
          <el-radio-group v-model="staffForm.crewType">
            <el-radio :label="1">商用机长(飞手)</el-radio>
            <el-radio :label="2">外场运维专员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="执照编号" required>
          <el-input v-model="staffForm.licenseNo" placeholder="如：CAAC-UAV-2024009" />
        </el-form-item>
        <el-form-item label="执照有效期截止" required>
          <el-date-picker v-model="staffForm.licenseExpire" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="staffDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="staffLoading" @click="handleSaveStaff">保存档案</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getCrewStaffList,
  createCrewStaff,
  getWeekSchedule,
  scheduleShift,
  cancelShift
} from '@/api/asset'

const staffList = ref<any[]>([])
const weekMatrix = ref<any[]>([])
const shiftLoading = ref(false)
const staffLoading = ref(false)

const shiftDialogVisible = ref(false)
const shiftForm = reactive({
  crewId: undefined as number | undefined,
  shiftDate: '',
  startTime: '09:00:00',
  endTime: '18:00:00'
})

const staffDialogVisible = ref(false)
const staffForm = reactive({
  accountId: 4,
  realName: '',
  crewType: 1,
  licenseNo: '',
  licenseExpire: ''
})

// 计算本周 7 天日期与星期
const weekDays = computed(() => {
  const days = []
  const today = new Date()
  const dayOfWeek = today.getDay() || 7
  const monday = new Date(today)
  monday.setDate(today.getDate() - dayOfWeek + 1)

  const labels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  for (let i = 0; i < 7; i++) {
    const d = new Date(monday)
    d.setDate(monday.getDate() + i)
    const dateStr = d.toISOString().slice(0, 10)
    days.push({ label: labels[i], date: dateStr })
  }
  return days
})

const fetchStaff = async () => {
  try {
    const data = await getCrewStaffList()
    staffList.value = data || []
  } catch (err) {}
}

const fetchWeekSchedule = async () => {
  try {
    const monday = weekDays.value[0]?.date
    const data = await getWeekSchedule(monday)
    weekMatrix.value = data || []
  } catch (err) {}
}

const getShiftsForDay = (shifts: any[], dateStr: string) => {
  if (!shifts) return []
  return shifts.filter((s) => s.shiftDate === dateStr)
}

const openShiftDialog = () => {
  shiftForm.crewId = staffList.value.length > 0 ? staffList.value[0].crewId : undefined
  shiftForm.shiftDate = weekDays.value[0]?.date || ''
  shiftForm.startTime = '09:00:00'
  shiftForm.endTime = '18:00:00'
  shiftDialogVisible.value = true
}

const handleSaveShift = async () => {
  if (!shiftForm.crewId || !shiftForm.shiftDate || !shiftForm.startTime || !shiftForm.endTime) {
    ElMessage.warning('请完整填写排班参数')
    return
  }
  shiftLoading.value = true
  try {
    await scheduleShift({
      crewId: shiftForm.crewId,
      shiftDate: shiftForm.shiftDate,
      startTime: shiftForm.startTime,
      endTime: shiftForm.endTime
    })
    ElMessage.success('机组执勤排班录入成功')
    shiftDialogVisible.value = false
    fetchWeekSchedule()
  } finally {
    shiftLoading.value = false
  }
}

const handleCancelShift = async (shiftId: number) => {
  try {
    await cancelShift(shiftId)
    ElMessage.success('排班已变更为调休请假')
    fetchWeekSchedule()
  } catch (err) {}
}

const openStaffDialog = () => {
  staffForm.realName = ''
  staffForm.crewType = 1
  staffForm.licenseNo = ''
  staffForm.licenseExpire = ''
  staffDialogVisible.value = true
}

const handleSaveStaff = async () => {
  if (!staffForm.realName || !staffForm.licenseNo || !staffForm.licenseExpire) {
    ElMessage.warning('请完整填写机组档案信息')
    return
  }
  staffLoading.value = true
  try {
    await createCrewStaff(staffForm)
    ElMessage.success('机组人员登记成功')
    staffDialogVisible.value = false
    fetchStaff()
    fetchWeekSchedule()
  } finally {
    staffLoading.value = false
  }
}

onMounted(() => {
  fetchStaff()
  fetchWeekSchedule()
})
</script>

<style scoped>
.crew-shift-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.box-card {
  border-radius: 8px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.card-header .title {
  font-size: 15px;
  font-weight: bold;
  color: #0f172a;
}
.license-code {
  font-family: monospace;
  font-size: 12px;
  color: #0284c7;
}
.shift-cell {
  min-height: 52px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  justify-content: center;
}
.shift-badge {
  background-color: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 4px;
  padding: 4px 6px;
  font-size: 11px;
}
.shift-state-4 {
  background-color: #f1f5f9;
  border-color: #cbd5e1;
  opacity: 0.6;
}
.shift-time {
  font-weight: bold;
  color: #0369a1;
}
.shift-act {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 2px;
}
.badge-status {
  font-size: 10px;
  color: #64748b;
}
.no-shift {
  color: #cbd5e1;
  font-size: 14px;
}
</style>

