<template>
  <div style="display: flex;flex-direction: column;align-items: center;">
    <el-button :type="currentExpression?'primary':'warning'" text @click="open">
      <i class="fa-solid fa-clock"></i>&nbsp;
      <span v-if="currentExpression && currentExpression.length">
          已设置：{{ shortExpr }}
      </span>
      <span v-else>未设置</span>
    </el-button>
    <span v-if="currentExpression && currentExpression.length" style="font-size: 13px;color: #a5a5a5;cursor: pointer;" >
      {{ zhDescription }}
    </span>

    <el-dialog
      v-model="openCron"
      title="Cron表达式生成器"
      width="800"
      append-to-body
      :before-close="handleClose"
    >
    <Vue3CronPlusPicker @hide="closeDialog" @fill="crontabFill" :expression="expression"/>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
// import { vue3CronPlus } from 'vue3-cron-plus'
// import 'vue3-cron-plus/dist/index.css'

import 'vue3-cron-plus-picker/style.css'
import {Vue3CronPlusPicker} from 'vue3-cron-plus-picker'

const props = defineProps({ 
  expression: { 
    type: String, 
    default: '' 
  } , 
  rowId: { 
    type: String, 
    default: '' 
  } 
})
const emit = defineEmits(['update:expression', 'fill', 'hide'])

const cronValue = ref('')
const showCron = ref(false)
const expression = ref('* * * * * * *')

const openCron = ref(false)
const currentExpression = ref(props.expression)

// 监听props变化
watch(() => props.expression, v => { 
  currentExpression.value = v 
})

// function fillValue(val) {
//   console.log('fillValue:', val)
//   cronValue.value = val
// }

function open() {
  openCron.value = true
}

// 处理对话框关闭
function handleClose() {
  openCron.value = false
  emit('hide')
}

// 关闭对话框
function closeDialog() {
  openCron.value = false
  // emit('hide')
}

// 确认选择的cron表达式
function crontabFill(expr) {
  currentExpression.value = expr || ''
  cronValue.value = expr || ''  // 更新cronValue
  emit('update:expression', currentExpression.value)
  emit('fill', props.rowId , currentExpression.value)
  openCron.value = false
}

// 监听cron值变化 - 修复了this关键字的问题
const changeCron = (value) => {
  console.log('cronValue:', value)
  if (typeof value === "string") {
    cronValue.value = value;  // 使用响应式变量而非this
    crontabFill(value);       // 填充并关闭对话框
  }
}

// 表达式截断显示
const shortExpr = computed(() => {
  const s = currentExpression.value || ''
  return s.length > 24 ? s.slice(0, 21) + '...' : s
})

const zhDescription = computed(() => {
  const expr = currentExpression.value || ''
  if (!expr) return ''
  try {
    return cronToChinese(expr)
  } catch (e) {
    return '自定义：' + expr
  }
})

// 将前端常见 cron 转为中文描述
function cronToChinese(raw) {
  if (!raw || !raw.trim()) return ''
  let s = raw.trim()
  const parts = s.split(/\s+/)
  if (parts.length < 5 || parts.length > 7) {
    return '自定义：' + raw
  }

  // 5 字段（min hour day month dow） -> 前补秒位 0
  let arr
  if (parts.length === 5) {
    arr = ['0', ...parts]
  } else if (parts.length === 7) {
    // 7 字段，去掉年部分以便统一（如果你想保留年，可以改逻辑）
    arr = parts.slice(0, 6)
  } else {
    arr = parts.slice(0, 6)
  }

  const [sec, min, hour, dom, mon, dow] = arr.map(x => x.toUpperCase())

  // 辅助解析
  const isEveryN = (tok) => {
    // 支持 */n 或 0/n 两种形式
    const m = tok.match(/^\*\/(\d+)$/) || tok.match(/^0\/(\d+)$/)
    return m ? parseInt(m[1], 10) : null
  }
  const isNumber = (t) => /^[0-9]+$/.test(t)
  const formatTime = (hh, mm, ss) => {
    // 删除前导零以更自然显示
    const fh = String(parseInt(hh, 10)).padStart(1, '0')
    const fm = String(parseInt(mm, 10)).padStart(2, '0')
    const fs = String(parseInt(ss, 10)).padStart(2, '0')
    return `${fh}:${fm}:${fs}`
  }

  // 星期映射：Quartz 允许 SUN-? 或 1-7（视实现），这里简单映射常见英文名
  const dowToChinese = (d) => {
    if (!d) return ''
    const map = {
      'SUN': '星期日', 'MON': '星期一', 'TUE': '星期二', 'WED': '星期三',
      'THU': '星期四', 'FRI': '星期五', 'SAT': '星期六',
      '0': '星期日', '1': '星期一', '2': '星期二', '3': '星期三', '4': '星期四', '5': '星期五', '6': '星期六', '7': '星期日'
    }
    // 支持逗号列表
    if (d.includes(',')) {
      return d.split(',').map(x => (map[x] || x)).join('、')
    }
    return map[d] || d
  }

  // 常见模式判定

  // 1) 每秒：秒为 * 且 分/时 为 *
  if ((sec === '*' || sec === '*/1') && (min === '*' || min === '*/1') && hour === '*') {
    return '每秒执行'
  }

  // 2) 每分钟：秒 == 0 且 分为 *
  if ((sec === '0' || sec === '0/1') && (min === '*' || min === '*/1') && hour === '*') {
    return '每分钟执行'
  }

  // 3) 每 N 秒
  const nSec = isEveryN(sec)
  if (nSec && min === '*' && hour === '*') {
    return `每 ${nSec} 秒执行一次`
  }

  // 4) 每 N 分钟（常见表达 */n 或 0/n）
  const nMin = isEveryN(min)
  if (nMin && hour === '*') {
    return `每 ${nMin} 分钟执行一次`
  }

  // 5) 每小时整点：秒=0 分=0 小时=*
  if (sec === '0' && min === '0' && hour === '*') {
    return '每小时整点执行'
  }

  // 6) 每天某时：dom=* or ? 且 dow = * or ?
  if ((dom === '*' || dom === '?' ) && (dow === '*' || dow === '?') && isNumber(hour) && (isNumber(min) || min === '0') ) {
    // 精确到秒/分/时
    const hh = hour
    const mm = isNumber(min) ? min : '0'
    const ss = isNumber(sec) ? sec : '0'
    return `每天 ${formatTime(hh, mm, ss)} 执行`
  }

  // 7) 每周：当 dom='?' 且 dow 指定
  if ((dom === '?' || dom === '*') && dow !== '?' && dow !== '*') {
    // 支持列表
    const dayDesc = dowToChinese(dow)
    const hh = isNumber(hour) ? hour : '0'
    const mm = isNumber(min) ? min : '0'
    const ss = isNumber(sec) ? sec : '0'
    return `每周 ${dayDesc} ${formatTime(hh, mm, ss)} 执行`
  }

  // 8) 每月：dom 为数字或列表，dow='?'
  if (dow === '?' && dom !== '?' && dom !== '*') {
    const hh = isNumber(hour) ? hour : '0'
    const mm = isNumber(min) ? min : '0'
    const ss = isNumber(sec) ? sec : '0'
    // 支持逗号列表的日
    const days = dom.includes(',') ? dom : `${dom} 日`
    return `每月 ${days} ${formatTime(hh, mm, ss)} 执行`
  }

  // 9) 每年：mon 具体且 dom 具体
  if (mon !== '*' && mon !== '?' && dom !== '*' && dom !== '?') {
    const hh = isNumber(hour) ? hour : '0'
    const mm = isNumber(min) ? min : '0'
    const ss = isNumber(sec) ? sec : '0'
    return `每年 ${mon} 月 ${dom} 日 ${formatTime(hh, mm, ss)} 执行`
  }

  // 兜底返回自定义文本
  return '自定义：' + raw
}
</script>
