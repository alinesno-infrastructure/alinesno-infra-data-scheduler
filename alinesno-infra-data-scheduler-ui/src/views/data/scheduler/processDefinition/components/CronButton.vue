<template>
  <div style="display:flex;flex-direction:column;align-items:center;">
    <el-button :type="currentExpression?'primary':'warning'" text @click="open">
      <i class="fa-solid fa-clock"></i>&nbsp;
      <span v-if="currentExpression && currentExpression.length">已设置：{{ shortExpr }}</span>
      <span v-else>未设置</span>
    </el-button>

    <span v-if="currentExpression && currentExpression.length" style="font-size:13px;color:#a5a5a5;cursor:pointer;">
      {{ zhDescription }}
    </span>

    <el-dialog v-model="openCron" title="Cron 表达式生成器" width="720px" append-to-body :before-close="handleClose">
      <div style="display:flex;flex-direction:column;gap:12px;">
        <!-- 模式选择：预设 / 自定义 -->
        <el-radio-group v-model="mode" size="large">
          <el-radio-button label="preset">预设</el-radio-button>
          <el-radio-button label="custom">自定义</el-radio-button>
        </el-radio-group>

        <!-- 预设面板 -->
        <div v-if="mode === 'preset'">
          <div style="display:flex;gap:12px;align-items:center;">
            <el-select v-model="presetUnit" placeholder="选择周期" size="large" style="width:250px;">
              <el-option label="分钟（每 N 分钟）" value="minute"></el-option>
              <el-option label="小时（每 N 小时）" value="hour"></el-option>
              <el-option label="天（每 N 天）" value="day"></el-option>
            </el-select>

            <el-input-number v-model="presetInterval" :min="1" :max="999" size="large" style="width:120px;" />
            <div style="color:#888;font-size:13px;">间隔：每 {{ presetInterval }} {{ presetUnit === 'minute' ? '分钟' : presetUnit==='hour' ? '小时' : '天' }} 执行</div>
          </div>

          <div style="margin-top:8px;color:#666;font-size:13px;">
            说明：生成的表达式会采用 6 字段格式 (sec min hour dom month dow)，例如每 1 分钟：0 */1 * * * *
          </div>
        </div>

        <!-- 自定义面板 -->
        <div v-if="mode === 'custom'">
          <div style="display:flex;gap:8px;align-items:center;">
            <el-input v-model="customExpr" placeholder="输入 5 或 6 字段的 cron 表达式（支持 H 等扩展）" clearable size="large" style="flex:1;" />
            <el-button size="large" type="primary" @click="tryParseCustom">校验并预览</el-button>
          </div>
          <div style="margin-top:6px;color:#888;font-size:13px;">
            提示：支持 5 字段 (min hour dom month dow) 或 6 字段 (sec min hour dom month dow)。5 字段会在后端视情况前置秒位 0。
          </div>
          <div v-if="customPreview" style="margin-top:6px;font-size:13px;">
            预览解析：<strong style="color:#409EFF">{{ customPreview }}</strong> — {{ cronToChinese(customPreview) }}
          </div>
          <div v-if="customError" style="color:#f56c6c;margin-top:6px;">{{ customError }}</div>
        </div>

        <!-- 底部操作 -->
        <div style="display:flex;justify-content:flex-end;gap:8px;margin-top:12px;">
          <el-button @click="handleClose" size="large">取消</el-button>
          <el-button type="primary" @click="confirm" size="large">确定</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'

const props = defineProps({
  expression: { type: String, default: '' },
  rowId: { type: String, default: '' }
})
const emit = defineEmits(['update:expression', 'fill', 'hide'])

// 状态
const openCron = ref(false)
const currentExpression = ref(props.expression || '')
watch(() => props.expression, v => { currentExpression.value = v || '' })

// 模式：preset | custom
const mode = ref('preset')
const presetUnit = ref('minute') // minute/hour/day
const presetInterval = ref(1)

// 自定义表达式输入与预览
const customExpr = ref('')
const customPreview = ref('')
const customError = ref('')

// 打开/关闭
function open() { 
  // 初始化 custom 输入为当前表达式（便于编辑）
  customExpr.value = currentExpression.value || ''
  customPreview.value = ''
  customError.value = ''
  openCron.value = true
}
function handleClose() {
  openCron.value = false
  emit('hide')
}

// 生成 6-field cron 字符串（sec min hour dom month dow）
function buildPresetExpr(unit, n) {
  n = Math.max(1, parseInt(n || 1, 10))
  if (unit === 'minute') {
    // 每 N 分钟：0 */N * * * *
    return `0 */${n} * * * *`
  } else if (unit === 'hour') {
    // 每 N 小时：0 0 */N * * *
    return `0 0 */${n} * * *`
  } else { // day
    // 每 N 天（每天的 00:00:00 开始）：0 0 0 */N * *
    return `0 0 0 */${n} * *`
  }
}

// 简单校验 5/6 字段的表达式（前端轻量校验）
function isValidCronExpression(expr) {
  if (!expr || !expr.trim()) return false
  const parts = expr.trim().split(/\s+/)
  if (parts.length !== 5 && parts.length !== 6) return false
  // 简单检查每段不为空
  return parts.every(p => p.length > 0)
}

// 按钮：校验自定义并显示预览
function tryParseCustom() {
  customError.value = ''
  customPreview.value = ''
  if (!isValidCronExpression(customExpr.value)) {
    customError.value = '表达式应为 5 或 6 字段的 cron（以空格分隔）'
    return
  }
  // 如果是 5 字段，自动展示前置秒位 0 的 6 字段预览
  const parts = customExpr.value.trim().split(/\s+/)
  if (parts.length === 5) {
    customPreview.value = ['0', ...parts].join(' ')
  } else {
    customPreview.value = parts.join(' ')
  }
}

// 确认：根据当前 mode 填充 expression 并 emit
function confirm() {
  let expr = ''
  if (mode.value === 'preset') {
    expr = buildPresetExpr(presetUnit.value, presetInterval.value)
  } else {
    // custom
    if (!isValidCronExpression(customExpr.value)) {
      customError.value = '表达式应为 5 或 6 字段的 cron（以空格分隔）'
      return
    }
    const parts = customExpr.value.trim().split(/\s+/)
    expr = parts.length === 5 ? ['0', ...parts].join(' ') : parts.join(' ')
  }
  // 更新本地和上层
  currentExpression.value = expr
  emit('update:expression', currentExpression.value)
  emit('fill', props.rowId, currentExpression.value)
  openCron.value = false
}

// 供显示用：截断
const shortExpr = computed(() => {
  const s = currentExpression.value || ''
  return s.length > 24 ? s.slice(0, 21) + '...' : s
})

// 中文描述（轻量版，兼容 5 或 6 字段）
const zhDescription = computed(() => {
  const expr = currentExpression.value || ''
  if (!expr) return ''
  try {
    return cronToChinese(expr)
  } catch (e) {
    return '自定义：' + expr
  }
})

function cronToChinese(raw) {
  if (!raw || !raw.trim()) return ''
  const parts = raw.trim().split(/\s+/)
  let arr
  if (parts.length === 5) arr = ['0', ...parts]
  else if (parts.length === 6) arr = parts.slice(0, 6)
  else return '自定义：' + raw

  const [sec, min, hour, dom, mon, dow] = arr.map(x => x.toUpperCase())

  const isNum = t => /^[0-9]+$/.test(t)
  const isWildcard = t => t === '*' || t === '?'
  const everyN = t => {
    const m = t.match(/^\*\/(\d+)$/) || t.match(/^0\/(\d+)$/)
    return m ? parseInt(m[1], 10) : null
  }

  const formatTime = (hh, mm, ss) => {
    const fh = String(parseInt(hh || '0', 10))
    const fm = String(parseInt(mm || '0', 10)).padStart(2, '0')
    const fs = String(parseInt(ss || '0', 10)).padStart(2, '0')
    return `${fh}:${fm}:${fs}`
  }

  const dowMap = {
    'SUN': '星期日','MON': '星期一','TUE':'星期二','WED':'星期三',
    'THU':'星期四','FRI':'星期五','SAT':'星期六',
    '0':'星期日','1':'星期一','2':'星期二','3':'星期三','4':'星期四','5':'星期五','6':'星期六','7':'星期日'
  }
  function dowToChinese(d) {
    if (!d) return ''
    return d.split(',').map(tok => {
      tok = tok.trim()
      if (dowMap[tok]) return dowMap[tok]
      if (tok.includes('-')) {
        const parts = tok.split('-').map(p => (dowMap[p] || p))
        return parts.join('至')
      }
      return tok
    }).join('、')
  }

  // 优先识别：每秒 / 每分钟 / 每 N 秒 / 每 N 分钟
  if ((isWildcard(sec) || sec === '*/1') && (isWildcard(min) || min === '*/1') && isWildcard(hour)) return '每秒执行'
  if ((sec === '0' || sec === '0/1') && (isWildcard(min) || min === '*/1') && isWildcard(hour)) return '每分钟执行'
  const nSec = everyN(sec)
  if (nSec && isWildcard(min) && isWildcard(hour)) return `每 ${nSec} 秒执行一次`
  const nMin = everyN(min)
  if (nMin && isWildcard(hour)) return `每 ${nMin} 分钟执行一次`

  // 小时步进识别：例如 0 0 */5 * * * -> 每 5 小时执行一次
  const nHour = everyN(hour)
  if (nHour && (min === '0') && (sec === '0') && isWildcard(dom) && isWildcard(dow)) {
    return `每 ${nHour} 小时执行一次`
  }

  // 每小时整点（sec=0,min=0,hour=*）
  if ((sec === '0' || sec === '0/1') && (min === '0') && isWildcard(hour)) return '每小时整点执行'

  // 每天某时：要求 DOM 和 DOW 都为 wildcard/? 且 hour 为具体数字或 step?（以具体数字为主）
  if (isWildcard(dom) && isWildcard(dow) && isNum(hour)) {
    const hh = hour
    const mm = isNum(min) ? min : '0'
    const ss = isNum(sec) ? sec : '0'
    return `每天 ${formatTime(hh, mm, ss)} 执行`
  }

  // 每周：当 dom 是 wildcard/? 且 dow 指定
  if (isWildcard(dom) && !isWildcard(dow)) {
    const hh = isNum(hour) ? hour : '0'
    const mm = isNum(min) ? min : '0'
    const ss = isNum(sec) ? sec : '0'
    return `每周 ${dowToChinese(dow)} ${formatTime(hh, mm, ss)} 执行`
  }

  // 每月：当 dow 是 '?' 或 wildcard，且 dom 指定（数字或列表）
  if ((dow === '?' || isWildcard(dow)) && !isWildcard(dom) && dom !== '?') {
    const hh = isNum(hour) ? hour : '0'
    const mm = isNum(min) ? min : '0'
    const ss = isNum(sec) ? sec : '0'
    const days = dom.includes(',') ? dom : `${dom} 日`
    return `每月 ${days} ${formatTime(hh, mm, ss)} 执行`
  }

  // 每年：当 month 指定且 dom 指定
  if (!isWildcard(mon) && mon !== '?' && !isWildcard(dom) && dom !== '?') {
    const hh = isNum(hour) ? hour : '0'
    const mm = isNum(min) ? min : '0'
    const ss = isNum(sec) ? sec : '0'
    return `每年 ${mon} 月 ${dom} 日 ${formatTime(hh, mm, ss)} 执行`
  }

  // 兜底
  return '自定义：' + raw
}

</script>