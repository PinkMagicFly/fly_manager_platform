<template>
  <div class="apply-container">
    <div class="apply-box">
      <div class="apply-header">
        <h2>低空物流商户企业入驻向导</h2>
        <p>提交营业执照与适航运营资质，开启城市低空物流智能运营</p>
      </div>

      <el-steps :active="activeStep" finish-status="success" align-center class="step-bar">
        <el-step title="企业信息" description="社会信用与法人" />
        <el-step title="资质证照" description="适航许可与证件" />
        <el-step title="主账号设置" description="企业登录名与密码" />
        <el-step title="提交完成" description="等待管理员审批" />
      </el-steps>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="140px" class="apply-form">
        <!-- 步骤1: 企业基本信息 -->
        <div v-show="activeStep === 0">
          <el-form-item label="企业全称" prop="corpName">
            <el-input v-model="form.corpName" placeholder="如：深圳市丰翼低空物流科技有限公司" />
          </el-form-item>
          <el-form-item label="统一社会信用代码" prop="licenseNo">
            <el-input v-model="form.licenseNo" placeholder="18位营业执照信用代码" />
          </el-form-item>
          <el-form-item label="业务联系人" prop="contactName">
            <el-input v-model="form.contactName" placeholder="业务负责人真实姓名" />
          </el-form-item>
          <el-form-item label="联系人手机号" prop="contactPhone">
            <el-input v-model="form.contactPhone" placeholder="11位有效手机号码" />
          </el-form-item>
        </div>

        <!-- 步骤2: 资质与许可 -->
        <div v-show="activeStep === 1">
          <el-form-item label="适航运营许可编号" prop="airworthinessCert">
            <el-input v-model="form.airworthinessCert" placeholder="如：CAAC-CCAR92-2024-SZ01" />
          </el-form-item>
          <el-form-item label="资质证书附件路径">
            <el-input v-model="form.certFilePath" placeholder="/uploads/cert/2026/license_demo.pdf" />
            <span class="tip-text">支持预设虚拟附件路径进行资格查验与存证备案</span>
          </el-form-item>
        </div>

        <!-- 步骤3: 主账号设置 -->
        <div v-show="activeStep === 2">
          <el-form-item label="企业主账号登录名" prop="loginName">
            <el-input v-model="form.loginName" placeholder="用于后续登录翔云平台的账号" />
          </el-form-item>
          <el-form-item label="设置初始密码" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="不少于6位密码" />
          </el-form-item>
        </div>

        <!-- 步骤4: 提交完成 -->
        <div v-show="activeStep === 3" class="result-box">
          <el-result icon="success" title="企业入驻申请已成功提交" sub-title="您的资质材料已进入平台管理员审批队列，审批通过后将自动激活商户权限并短信通知。">
            <template #extra>
              <el-button type="primary" @click="$router.push('/login')">返回登录页</el-button>
            </template>
          </el-result>
        </div>

        <div class="action-bar" v-if="activeStep < 3">
          <el-button v-if="activeStep > 0" @click="activeStep--">上一步</el-button>
          <el-button type="primary" v-if="activeStep < 2" @click="nextStep">下一步</el-button>
          <el-button type="success" :loading="submitting" v-if="activeStep === 2" @click="handleSubmit">确认提交申请</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import request from '@/utils/request'

const activeStep = ref(0)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  corpName: '',
  licenseNo: '',
  contactName: '',
  contactPhone: '',
  airworthinessCert: '',
  certFilePath: '/uploads/cert/license_demo.pdf',
  loginName: '',
  password: ''
})

const rules = {
  corpName: [{ required: true, message: '请输入企业全称', trigger: 'blur' }],
  licenseNo: [{ required: true, message: '请输入统一社会信用代码', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不合法', trigger: 'blur' }
  ],
  loginName: [{ required: true, message: '请输入主账号登录名', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码不能少于6位', trigger: 'blur' }]
}

const nextStep = async () => {
  if (!formRef.value) return
  if (activeStep.value === 0) {
    try {
      await formRef.value.validateField(['corpName', 'licenseNo', 'contactName', 'contactPhone'])
      activeStep.value++
    } catch (e) {
      ElMessage.warning('请完整填写企业信息并输入合法的11位手机号')
    }
  } else if (activeStep.value === 1) {
    activeStep.value++
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    submitting.value = true
    await request.post('/api/v1/merchant/apply', form)
    ElMessage.success('提交入驻申请成功！')
    activeStep.value = 3
  } catch (e) {
    ElMessage.warning('请检查并完整填写主账号登录名与密码（不少于6位）')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.apply-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f1f5f9;
  padding: 40px 20px;
}
.apply-box {
  width: 760px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  padding: 40px;
}
.apply-header {
  text-align: center;
  margin-bottom: 30px;
}
.apply-header h2 {
  margin: 0 0 8px 0;
  color: #0f172a;
}
.apply-header p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}
.step-bar {
  margin-bottom: 36px;
}
.apply-form {
  padding: 0 20px;
}
.tip-text {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
  display: block;
}
.result-box {
  padding: 20px 0;
}
.action-bar {
  margin-top: 30px;
  text-align: right;
  border-top: 1px solid #f1f5f9;
  padding-top: 20px;
}
</style>
