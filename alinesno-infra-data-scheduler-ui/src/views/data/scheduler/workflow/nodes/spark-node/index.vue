<template>
  <!-- 工作流节点容器，包含节点标题、设置和输出参数等内容 -->
  <FlowContainer :nodeModel="nodeModel" :properties="properties">

    <!-- 节点设置部分 -->
    <div class="node-settings">
      <!-- 节点设置标题 -->
      <div class="settings-title" style="display: flex;align-items: center;justify-content: space-between;">
        <span>
          节点设置 
        </span>
      </div>
      <!-- 节点设置表单区域 -->
      <div class="settings-form">
        <el-form :model="formData" label-width="auto" label-position="top">

          <!-- 运行类型选择 -->
          <el-form-item label="运行类型">
            <el-radio-group v-model="formData.runType">
              <el-radio label="spark-sql">Spark-SQL</el-radio>
              <el-radio label="pyspark">PySpark</el-radio>
            </el-radio-group>
          </el-form-item>

          <!-- Spark-SQL 编辑器（仅当选择 spark-sql） -->
          <el-form-item v-if="formData.runType === 'spark-sql'" label="运行Spark-SQL语句">
            <div class="function-CodemirrorEditor mb-8" style="height: 120px;width:100%">
              <ScriptEditorPanel ref="auditEditorRef" :lang="'sql'" />
              <div class="function-CodemirrorEditor__footer">
                <el-button text @click="openCodemirrorDialog" style="background-color: transparent !important;" class="magnify">
                  <i class="fa-solid fa-up-right-and-down-left-from-center"></i>
                </el-button>
              </div>
            </div>
          </el-form-item>

          <!-- PySpark 编辑器（仅当选择 pyspark） -->
          <el-form-item v-else label="运行PySpark脚本">
            <div class="function-CodemirrorEditor mb-8" style="height: 120px;width:100%">
              <ScriptEditorPanel ref="auditEditorRef" :lang="'python'" />
              <div class="function-CodemirrorEditor__footer">
                <el-button text @click="openCodemirrorDialog" style="background-color: transparent !important;" class="magnify">
                  <i class="fa-solid fa-up-right-and-down-left-from-center"></i>
                </el-button>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="是否异步">
            <el-switch v-model="formData.isAsync" size="small" />
          </el-form-item>

          <el-form-item label="返回内容">
            <el-switch v-model="formData.isPrint" size="small" />
          </el-form-item>
        </el-form>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      append-to-body
      width="1024"
      fullscreen
    >
      <template #header>
        <div class="dialog-footer mt-24" style="display: flex;align-items: center; justify-content: space-between; ">
          <div>
            {{ dialogHint }}
          </div>
          <div>
            <el-button type="primary" size="large" text bg @click="confirmAndSave"> 确定保存 </el-button>
          </div>
        </div>
      </template>
      <ScriptEditorFullPanel ref="auditFullEditorRef" :lang="currentLang" />
    </el-dialog>

  </FlowContainer>
</template>

<script setup>
import { set } from 'lodash'
import { ref, reactive, computed, nextTick, onMounted } from 'vue'

import FlowContainer from '@/views/data/scheduler/workflow/common/FlowContainer'
import ScriptEditorPanel from '@/views/data/scheduler/workflow/components/ScriptEditor';
import ScriptEditorFullPanel from '@/views/data/scheduler/workflow/components/ScriptEditorFull';

// import FlowCascader from '@/views/data/scheduler/workflow/common/FlowCascader'
// import DatasourceSelector from '@/views/data/scheduler/workflow/components/DatasourceSelector'

// 定义组件接收的属性
const props = defineProps({
  properties: {
    type: Object,
    default: () => ({})
  },
  isSelected: {
    type: Boolean,
    default: false
  },
  nodeModel: {
    type: Object
  }
});

const auditEditorRef = ref(null)
const auditFullEditorRef = ref(null)

// 绑定选择框的值
const dialogVisible = ref(false)
const chatDataCode = ref('')

// 初始化默认node_data结构
const defaultNodeData = {
  runType: 'spark-sql', // 'spark-sql' | 'pyspark'
  sqlContent: '',
  pysparkContent: '',
  isAsync: false,
  isPrint: false
}

// 表单数据对象（代理到 nodeModel.properties.node_data）
const form = reactive({ ...defaultNodeData })

const formData = computed({
  get: () => {
    if (props.nodeModel.properties.node_data) {
      return props.nodeModel.properties.node_data
    } else {
      // 深拷贝默认，防止引用问题
      set(props.nodeModel.properties, 'node_data', JSON.parse(JSON.stringify(form)))
    }
    return props.nodeModel.properties.node_data
  },
  set: (value) => {
    set(props.nodeModel.properties, 'node_data', value)
  }
})

// 根据 runType 设置编辑器语言
const currentLang = computed(() => {
  return formData.value.runType === 'pyspark' ? 'python' : 'sql'
})

const dialogTitle = computed(() => {
  return formData.value.runType === 'pyspark' ? '函数内容（PySpark）' : '函数内容（Spark-SQL）'
})

const dialogHint = computed(() => {
  return formData.value.runType === 'pyspark'
    ? '请输入运行的PySpark脚本，可输入多行'
    : '请输入运行的SPARK-SQL语句，可输入多行'
})

// 确认保存
function confirmAndSave() {
  chatDataCode.value = auditFullEditorRef.value.getRawScript()
  // 更新内联编辑器内容
  if (auditEditorRef.value && auditEditorRef.value.setRawScript) {
    auditEditorRef.value.setRawScript(chatDataCode.value)
  }

  // 根据 runType 保存到不同字段
  if (formData.value.runType === 'pyspark') {
    formData.value.pysparkContent = chatDataCode.value
  } else {
    formData.value.sqlContent = chatDataCode.value
  }

  dialogVisible.value = false
}

// 打开大编辑器弹窗
function openCodemirrorDialog() {

  // 取当前编辑器的内容（内联编辑器）
  let current = ''
  if (auditEditorRef.value && auditEditorRef.value.getRawScript) {
    current = auditEditorRef.value.getRawScript()
  } else {
    // 如果内联编辑器没有内容，则从 formData 中加载
    current = formData.value.runType === 'pyspark' ? (formData.value.pysparkContent || '') : (formData.value.sqlContent || '')
  }

  chatDataCode.value = current
  dialogVisible.value = true

  nextTick(() => {
    if (auditFullEditorRef.value && auditFullEditorRef.value.setRawScript) {
      auditFullEditorRef.value.setRawScript(chatDataCode.value)
    }
  })
}

onMounted(() => {
  // 初始化内联编辑器内容
  if (formData.value.runType === 'pyspark') {
    if (formData.value.pysparkContent && auditEditorRef.value && auditEditorRef.value.setRawScript) {
      auditEditorRef.value.setRawScript(formData.value.pysparkContent)
    }
  } else {
    if (formData.value.sqlContent && auditEditorRef.value && auditEditorRef.value.setRawScript) {
      auditEditorRef.value.setRawScript(formData.value.sqlContent)
    }
  }
})

</script>

<style lang="scss" scoped>

.function-CodemirrorEditor__footer {
  position: absolute;
  bottom: 0px;
  right: 0px;
}

</style>