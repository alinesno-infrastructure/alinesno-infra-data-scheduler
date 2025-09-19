<template>
  <div>

  <el-button type="warning" text @click="open">
    <i class="fa-solid fa-clock"></i>&nbsp;
    <span v-if="currentExpression && currentExpression.length">已设置：{{ shortExpr }}</span>
    <span v-else>未设置</span>
  </el-button>

  <el-dialog
    v-model="openCron"
    title="Cron表达式生成器"
    width="800"
    append-to-body
    :before-close="handleClose"
  >
    <!-- TODO: 添加全局环境变量功能 -->
    <Crontab 
      :expression="currentExpression" 
      @hide="onHide" 
      @fill="crontabFill" />
  </el-dialog>

  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import Crontab from '@/components/Crontab'

const props = defineProps({ expression: { type: String, default: '' } })
const emit = defineEmits(['update:expression', 'fill', 'hide'])

const openCron = ref(false)
const currentExpression = ref(props.expression)

watch(() => props.expression, v => { currentExpression.value = v })

function open() {
  console.log('open clicked, before:', openCron.value)
  openCron.value = true
  console.log('after:', openCron.value)
}

function crontabFill(expr) {
  currentExpression.value = expr || ''
  emit('update:expression', currentExpression.value)
  emit('fill', currentExpression.value)
  openCron.value = false
}

function onHide() {
  openCron.value = false
  emit('hide')
}

const shortExpr = computed(() => {
  const s = currentExpression.value || ''
  return s.length > 24 ? s.slice(0, 21) + '...' : s
})
</script>