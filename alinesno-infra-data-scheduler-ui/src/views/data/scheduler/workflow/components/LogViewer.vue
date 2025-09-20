<template>
  <el-dialog
    :model-value="visible"
    :title="title || '节点日志'"
    width="60%"
    @close="handleClose"
    :close-on-click-modal="false"
  >
    <div class="log-container">
      <div class="log-meta">
        <span class="meta-label">节点：</span><span class="meta-value">{{ title }}</span>
      </div>

      <div class="log-body">
        <pre class="log-pre" v-if="log">{{ log }}</pre>
        <div v-else class="log-empty">暂无日志内容</div>
      </div>
    </div>

    <template #footer>
      <el-button type="primary" @click="copyLog" :disabled="!log">复制日志</el-button>
      <el-button type="danger" @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '' },
  log: { type: String, default: '' }
})

const emit = defineEmits(['update:visible'])

function handleClose() {
  emit('update:visible', false)
}

async function copyLog() {
  try {
    await navigator.clipboard.writeText(props.log || '')
    // 简单提示，生产中可换为消息组件
    // 这里为了避免依赖额外插件用 alert，建议替换为 ElMessage
    alert('日志已复制到剪贴板')
  } catch (err) {
    alert('复制失败')
  }
}
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