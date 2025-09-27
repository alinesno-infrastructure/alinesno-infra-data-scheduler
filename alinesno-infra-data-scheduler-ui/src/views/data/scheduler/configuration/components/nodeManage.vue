<template>
  <div class="app-container">
    <div class="label-title">
      <div class="tip">AI分布式计算引擎配置</div>
      <div class="sub-tip">选择计算引擎并填写引擎地址、请求 Token 与请求超时时间</div>
    </div>

    <div class="form-container">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="160px" size="large" class="compute-form">
        <el-form-item label="计算引擎" prop="computeEngine">
          <el-radio-group v-model="form.computeEngine" class="engine-group">
            <el-radio v-for="item in engineOptions" :key="item.value" :label="item.value" class="engine-button">
              <i :class="item.icon" aria-hidden="true"></i>
              <span class="engine-label">{{ item.label }}</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="引擎地址" prop="engineAddress">
          <div style="display: flex;align-items: center;width: 100%;">
            <el-input v-model="form.engineAddress"
              placeholder="例如：http://spark-master:8080 或 thrift://trino-coordinator:8080" autocomplete="off" />
            <el-button type="primary" size="large" @click="onCheckAddress" :loading="checking" style="margin-left: 12px;">
              检测地址
            </el-button>
          </div>
          <div class="form-tip">填写可访问的引擎 REST/Thrift 接入地址，点击“检测地址”校验 
            <span v-if="healthStatus === 'ok'" style="color: #67C23A; margin-left: 8px;">地址可用（SUCCESS）</span>
            <span v-if="healthStatus === 'fail'" style="color: #F56C6C; margin-left: 8px;">地址不可用（FAILURE/错误）</span>
          </div>
        </el-form-item>

        <el-form-item label="引擎请求 Token" prop="adminUser">
          <el-input v-model="form.adminUser" placeholder="用于请求引擎的管理员用户（可选）" autocomplete="off" show-password />
          <div class="form-tip">若引擎启用了鉴权，请填入执行管理员 </div>
        </el-form-item>

        <el-form-item label="请求超时时间" prop="requestTimeout">
          <el-input-number v-model="form.requestTimeout" :min="1" :max="300" :step="1" controls-position="right">
            <template #append>秒</template>
          </el-input-number>
          <div class="form-tip">请求引擎的超时时间，单位秒</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="onSubmit">保存配置</el-button>
          <el-button @click="onReset">恢复默认</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getConfig, saveConfig, probeEngineOnServer } from '@/api/data/scheduler/compluteEngine' // 注意路径要与文件名一致

// 表单引用
const formRef = ref(null)
const loading = ref(false)

// 检测相关状态
const checking = ref(false)
const healthStatus = ref('') // '', 'ok', 'fail'

// 导出引擎选项以便 UI 引用
const engineOptions = [
  { value: 'spark', label: 'Apache Spark', icon: 'fa-solid fa-bolt' },
  { value: 'flink', label: 'Apache Flink', icon: 'fa-solid fa-water' },
  { value: 'trino', label: 'Trino', icon: 'fa-solid fa-database' }
]

// 默认值
const defaultForm = {
  computeEngine: 'spark',
  engineAddress: '',
  adminUser: '',
  requestTimeout: 30
}

const form = reactive({ ...defaultForm })

const rules = {
  computeEngine: [
    { required: true, message: '请选择计算引擎', trigger: 'change' }
  ],
  engineAddress: [
    { required: true, message: '请输入引擎地址', trigger: 'blur' },
    { min: 3, message: '地址太短', trigger: 'blur' }
  ],
  requestTimeout: [
    { required: true, message: '请输入请求超时时间', trigger: 'blur' },
    { type: 'number', min: 1, max: 300, message: '范围 1-300 秒', trigger: 'blur' }
  ]
}

// 页面加载时拉取后端配置（若存在）
async function loadConfig() {
  loading.value = true
  try {
    const res = await getConfig()
    // 假设后端返回的数据结构为 { data: {...} } 或直接返回对象，视你的 request 封装而定
    const data = res && res.data ? res.data : res
    if (data) {
      form.computeEngine = data.computeEngine || defaultForm.computeEngine
      form.engineAddress = data.engineAddress || defaultForm.engineAddress
      form.adminUser = data.adminUser || defaultForm.adminUser
      form.requestTimeout = data.requestTimeout || defaultForm.requestTimeout
    }
  } catch (err) {
    console.error(err)
    ElMessage.error('拉取配置失败，使用默认值')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadConfig()
})


async function onCheckAddress() {
  if (!form.engineAddress) {
    ElMessage.warning('请先填写引擎地址再进行检测')
    return
  }
  checking.value = true
  healthStatus.value = ''
  try {
    // 这里把 adminUser 写死为 fpgor（根据你给出的服务端示例），可改为可配置项
    const res = await probeEngineOnServer(form.engineAddress, form.adminUser)
    // res 可能是字符串或包含在 data 中，视 request 封装而定
    const success = res.data.success ; // && res.data ? res.data : res
    if (success) {
      healthStatus.value = 'ok'
      ElMessage.success('引擎健康检查通过 (SUCCESS)')
    } else {
      healthStatus.value = 'fail'
      ElMessage.error('引擎健康检查未通过')
    }
  } catch (err) {
    console.error('health check error:', err)
    healthStatus.value = 'fail'
    ElMessage.error('检测引擎地址时发生错误（可能无法访问或跨域）')
  } finally {
    checking.value = false
  }
}

async function onSubmit() {
  if (!formRef.value) return
  formRef.value.validate(async valid => {
    if (!valid) return

    // 可选：在保存之前强制检测一次（若需要），示例注释掉
    await onCheckAddress()
    if (healthStatus.value !== 'ok') {
      ElMessage.warning('建议先通过健康检查再保存配置')
      return
    }

    loading.value = true
    try {
      await saveConfig({
        computeEngine: form.computeEngine,
        engineAddress: form.engineAddress,
        adminUser: form.adminUser,
        requestTimeout: form.requestTimeout
      })
      ElMessage.success('配置保存成功')
    } catch (err) {
      console.error(err)
      ElMessage.error('配置保存失败')
    } finally {
      loading.value = false
    }
  })
}

function onReset() {
  Object.assign(form, defaultForm)
  if (formRef.value) formRef.value.clearValidate()
  ElMessage.info('已恢复默认配置')
}
</script>

<style scoped lang="scss">
.app-container {
  padding: 20px;
}

.form-container {
  max-width: 860px;
  margin: 18px auto;
  padding: 20px;
  background: #fff;
  border-radius: 6px;
}

.label-title {
  text-align: center;
  margin-bottom: 20px;

  .tip {
    font-size: 22px;
    font-weight: 600;
    color: #222;
    margin-bottom: 6px;
  }

  .sub-tip {
    font-size: 13px;
    color: #666;
  }
}

.engine-group {
  display: flex;
  // gap: 10px;
}

.engine-button {
  display: flex;
  align-items: center;
  // padding: 8px 14px;
}

.engine-button i {
  font-size: 18px;
  margin-right: 8px;
  color: #409EFF;
}

.engine-label {
  font-weight: 500;
}

.form-tip {
  // margin-top: 6px;
  font-size: 12px;
  color: #999;
  line-height: 35px;
}

.el-form-item {
  margin-bottom: 18px;
}
</style>