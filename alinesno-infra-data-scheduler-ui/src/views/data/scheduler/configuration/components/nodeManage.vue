<template>
  <div class="app-container">
    <div class="label-title">
      <div class="tip">AI数据编排计算服务配置</div>
      <div class="sub-tip">配置数据治理工作流程的编排能力，优化离线计算的数据处理效率</div>
    </div>

    <div class="form-container">
      <el-form
        :model="form"
        size="large"
        :rules="rules"
        v-loading="loading"
        ref="form"
        label-width="180px"
        class="demo-form"
      >
        <el-form-item label="计算引擎类型" prop="computeEngine">

          <!-- 
          <el-select v-model="form.computeEngine" placeholder="请选择计算引擎">
            <el-option
              v-for="item in engineOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            ></el-option>
          </el-select> 
          -->

          <el-radio-group v-model="form.computeEngine" size="large" >
            <el-radio-button 
              v-for="item in engineOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"></el-radio-button>
            </el-radio-group>

        </el-form-item>

        <el-form-item label="默认计算资源配额" prop="resourceQuota">
          <el-input-number 
            v-model="form.resourceQuota" 
            :min="1" 
            :max="100" 
            :step="5"
            controls-position="right"
          >
            <template slot="append">CU</template>
          </el-input-number>
          <span class="form-tip">(计算单元，1CU=1核4GB内存)</span>
        </el-form-item>

        <el-form-item label="任务超时时间" prop="taskTimeout">
          <el-input-number 
            v-model="form.taskTimeout" 
            :min="10" 
            :max="1440" 
            :step="10"
            controls-position="right"
          >
            <template slot="append">分钟</template>
          </el-input-number>
        </el-form-item>

        <el-form-item label="失败重试次数" prop="retryTimes">
          <el-input-number 
            v-model="form.retryTimes" 
            :min="0" 
            :max="10" 
            controls-position="right"
          ></el-input-number>
        </el-form-item>

        <el-form-item label="数据保留策略" prop="dataRetention">
          <el-radio-group v-model="form.dataRetention">
            <el-radio label="7">7天</el-radio>
            <el-radio label="30">30天</el-radio>
            <el-radio label="90">90天</el-radio>
            <el-radio label="custom">自定义</el-radio>
          </el-radio-group>
          <el-input-number 
            v-if="form.dataRetention === 'custom'"
            v-model="form.customRetentionDays" 
            :min="1" 
            :max="365"
            controls-position="right"
            style="margin-left: 15px;"
          >
            <template slot="append">天</template>
          </el-input-number>
        </el-form-item>

        <el-form-item label="启用数据质量检查" prop="enableQualityCheck">
          <el-switch 
            v-model="form.enableQualityCheck"
            active-text="启用"
            inactive-text="禁用"
          ></el-switch>
        </el-form-item>

        <el-form-item label="数据血缘跟踪" prop="enableLineage">
          <el-switch 
            v-model="form.enableLineage"
            active-text="启用"
            inactive-text="禁用"
          ></el-switch>
        </el-form-item>

        <!-- 
        <el-form-item label="任务调度策略" prop="schedulingStrategy">
          <el-select v-model="form.schedulingStrategy" placeholder="请选择调度策略">
            <el-option
              v-for="item in strategyOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            ></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="默认存储格式" prop="storageFormat">
          <el-checkbox-group v-model="form.storageFormat">
            <el-checkbox label="parquet">Parquet</el-checkbox>
            <el-checkbox label="orc">ORC</el-checkbox>
            <el-checkbox label="csv">CSV</el-checkbox>
            <el-checkbox label="json">JSON</el-checkbox>
          </el-checkbox-group>
        </el-form-item> 
        -->

        <el-form-item label="告警通知方式" prop="notificationMethods">
          <el-checkbox-group v-model="form.notificationMethods">
            <el-checkbox label="email">邮件</el-checkbox>
            <el-checkbox label="sms">短信</el-checkbox>
            <el-checkbox label="webhook">Webhook</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm('form')">
            保存配置
          </el-button>
          <el-button @click="resetForm">
            恢复默认
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      engineOptions: [
        { value: 'spark', label: 'Apache Spark' },
        { value: 'flink', label: 'Apache Flink' },
        { value: 'hive', label: 'Apache Hive' },
        { value: 'presto', label: 'Presto' }
      ],
      strategyOptions: [
        { value: 'fifo', label: '先进先出(FIFO)' },
        { value: 'priority', label: '优先级调度' },
        { value: 'fair', label: '公平调度' },
        { value: 'deadline', label: '截止时间优先' }
      ],
      form: {
        computeEngine: 'spark',
        resourceQuota: 10,
        taskTimeout: 120,
        retryTimes: 3,
        dataRetention: '30',
        customRetentionDays: 30,
        enableQualityCheck: true,
        enableLineage: true,
        schedulingStrategy: 'fair',
        storageFormat: ['parquet', 'orc'],
        notificationMethods: ['email', 'webhook']
      },
      rules: {
        computeEngine: [
          { required: true, message: '请选择计算引擎', trigger: 'change' }
        ],
        resourceQuota: [
          { required: true, message: '请输入资源配额', trigger: 'blur' },
          { type: 'number', min: 1, max: 100, message: '配额范围1-100CU', trigger: 'blur' }
        ],
        taskTimeout: [
          { required: true, message: '请输入任务超时时间', trigger: 'blur' },
          { type: 'number', min: 10, max: 1440, message: '超时时间10-1440分钟', trigger: 'blur' }
        ]
      },
      loading: false
    }
  },
  methods: {
    submitForm(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          this.loading = true
          // 这里应该调用API保存配置
          console.log('提交配置:', this.form)
          setTimeout(() => {
            this.loading = false
            this.$message.success('配置保存成功')
          }, 1000)
        }
      })
    },
    resetForm() {
      this.$refs.form.resetFields()
      this.$message.info('已恢复默认配置')
    }
  }
}
</script>

<style scoped lang="scss">
.app-container {
  padding: 20px;
}

.form-container {
  max-width: 1024px;
  margin: 20px auto;
  padding: 20px;
  background: #fff;
  border-radius: 4px;
  // box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.label-title {
  text-align: center;
  margin-bottom: 30px;

  .tip {
    font-size: 24px;
    font-weight: 500;
    color: #333;
    margin-bottom: 10px;
  }

  .sub-tip {
    font-size: 14px;
    color: #666;
  }
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #999;
}

.el-form-item {
  margin-bottom: 22px;
}
</style>