<template>
  <el-dialog
    :model-value="visible"
    :title="title || '节点日志'"
    width="60%"
    append-to-body
    @close="handleClose"
    :close-on-click-modal="false"
  >

      <ProcessInstanceLog 
        ref="processInstanceLogRef" 
        :diffHeight="300" />

  </el-dialog>
</template>

<script setup>
import { computed, watch } from 'vue'

import ProcessInstanceLog from './processInstanceLog.vue'

const processInstanceLogRef = ref(null)

const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '' },
  log: { type: String, default: '' }
})

const emit = defineEmits(['update:visible'])

function handleClose() {
  emit('update:visible', false)
}

const settingProcessIdAndNodeId = (processId , nodeId) => {
  console.log('processId = ' + processId)
  console.log('nodeId = ' + nodeId)
  nextTick(() => {
      processInstanceLogRef.value.connectLogger(processId , nodeId);
  })
}

watch(() => props.visible , (visible) => {
  if (visible) {
  }
}) 

defineExpose({
  settingProcessIdAndNodeId
})

</script>

<style scoped>
.log-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.log-meta {
  font-size: 13px;
  color: #606266;
}

.meta-label {
  color: #909399;
  margin-right: 6px;
}

.log-body {
  max-height: 360px;
  overflow: auto;
  background: #1e1e1e;
  color: #e6e6e6;
  padding: 12px;
  border-radius: 4px;
}

.log-pre {
  white-space: pre-wrap;
  word-break: break-word;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, "Roboto Mono", "Courier New", monospace;
  font-size: 13px;
  margin: 0;
}

.log-empty {
  color: #c0c4cc;
  font-size: 13px;
}
</style>