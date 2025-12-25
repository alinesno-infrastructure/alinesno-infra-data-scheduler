<template>
  <div class="process-log-viewer">
    <el-scrollbar
      ref="scrollbarRef"
      class="log-container"
      :style="'height: calc(100vh - ' + props.diffHeight  + 'px)'"
      v-loading="loading"
    >
      <div ref="logOutput" class="log-output">
        <div
          v-for="(line, idx) in logLines"
          :key="idx"
          :class="['log-line', line.level ? 'level-' + line.level : 'level-UNKNOWN']"
        >
          <div class="line-header">
            <span class="time">[{{ line.time }}]</span>
            <span class="message">{{ line.message }}</span>
            <span class="meta" v-if="line.nodeName"> · {{ line.nodeName }}</span>
          </div>

          <!-- 全量展示 meta（如果存在） -->
          <div v-if="line.metaFormatted" class="meta-block">
            <pre class="meta-pre">{{ line.metaFormatted }}</pre>
          </div>
        </div>
      </div>
    </el-scrollbar>

    <div class="log-footer">
      <el-button v-if="hasMoreLog" text bg type="primary" loading>获取日志中</el-button>
      <el-button v-else text bg type="primary">日志获取结束</el-button>

      <el-button type="primary" @click="onRefresh" :loading="loading" style="margin-left: 8px;">
        刷新日志
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onUnmounted } from 'vue';
import { readLog } from '@/api/data/scheduler/processLogger'; // 根据项目实际路径调整

const logger = ref({ start: 0, processInstanceId: null });
const logOutput = ref(null);
const logLines = ref([]); // 每行: { time, level, message, nodeName, raw, metaFormatted }
const scrollbarRef = ref(null);
const hasMoreLog = ref(true);
const loading = ref(false);
const interval = ref(null);

const props = defineProps({
  diffHeight: {
    type: Number,
    default: 90,
    required: false,
  },
})

/**
 * 解析 key=value 列表（支持嵌套大括号），返回 map：
 * - value 如果是非嵌套的普通值，直接为字符串（带可能的引号）
 * - 如果 value 是 { ... }，parseKeyValueContent 会把包含外层大括号的子串作为字符串返回（包含大括号）
 *
 * 本函数也在后续用 normalizeParsedMeta 递归解析嵌套的大括号内容。
 */
function parseKeyValueContent(content) {
  const map = {};
  let i = 0;
  const n = content.length;

  while (i < n) {
    // skip spaces
    while (i < n && /\s/.test(content[i])) i++;
    // read key
    let keyStart = i;
    while (i < n && content[i] !== '=') i++;
    if (i >= n) break;
    const key = content.substring(keyStart, i).trim();
    i++; // skip '='
    // read value
    if (i >= n) {
      map[key] = '';
      break;
    }
    let val = '';
    if (content[i] === '{') {
      // read until matching }
      let depth = 0;
      let j = i;
      while (j < n) {
        if (content[j] === '{') depth++;
        else if (content[j] === '}') {
          depth--;
          if (depth === 0) {
            j++;
            break;
          }
        }
        j++;
      }
      val = content.substring(i, j); // includes braces
      i = j;
    } else {
      // read until next top-level comma, ignore commas inside quotes
      let j = i;
      let inQuotes = false;
      while (j < n) {
        const ch = content[j];
        if (ch === '"') {
          inQuotes = !inQuotes;
        } else if (!inQuotes && ch === ',') {
          break;
        }
        j++;
      }
      val = content.substring(i, j).trim();
      i = j;
    }
    // skip comma and continue
    if (i < n && content[i] === ',') i++;
    map[key.trim()] = val;
  }

  return map;
}

/**
 * 递归处理 parseKeyValueContent 的输出：
 * - 去除字符串两端引号（如果存在）
 * - 对于值以 '{' 开头且 '}' 结尾的，去掉外层大括号并递归解析为对象
 */
function normalizeParsedMeta(parsedMap) {
  const result = {};
  for (const k in parsedMap) {
    let v = parsedMap[k];
    if (typeof v !== 'string') {
      result[k] = v;
      continue;
    }
    const s = v.trim();
    // 值为大括号包裹的结构：嵌套对象
    if (s.startsWith('{') && s.endsWith('}')) {
      const inner = s.substring(1, s.length - 1);
      try {
        const innerParsed = parseKeyValueContent(inner);
        result[k] = normalizeParsedMeta(innerParsed);
      } catch (e) {
        // 解析失败则保留原始（去掉最外层大括号）
        result[k] = inner;
      }
    } else {
      // 普通值：去掉外层引号（若有）
      let plain = s;
      if ((plain.startsWith('"') && plain.endsWith('"')) || (plain.startsWith("'") && plain.endsWith("'"))) {
        plain = plain.substring(1, plain.length - 1);
      }
      result[k] = plain;
    }
  }
  return result;
}

/**
 * 将 normalize 后的 meta 对象格式化为多行字符串，便于直接显示在日志中
 */
function formatMetaObject(obj, indent = 0) {
  const pad = (n) => '  '.repeat(n);
  if (obj === null || obj === undefined) return '';
  if (typeof obj !== 'object') {
    return String(obj);
  }
  let lines = [];
  for (const key of Object.keys(obj)) {
    const val = obj[key];
    if (val !== null && typeof val === 'object' && !Array.isArray(val)) {
      lines.push(`${pad(indent)}${key}:`);
      lines.push(formatMetaObject(val, indent + 1));
    } else {
      lines.push(`${pad(indent)}${key}: ${val}`);
    }
  }
  return lines.join('\n');
}

/**
 * 解析单行日志
 */
function parseLine(rawLine) {
  if (!rawLine || !rawLine.trim()) return null;

  let line = rawLine.trim();

  // 提取时间前缀 [yyyy-...]
  let time = '';
  const m = line.match(/^\[([^\]]+)\]\s*(.*)$/s);
  if (m) {
    time = m[1];
    line = m[2];
  }

  const nodeIdx = line.indexOf('NodeLog(');
  if (nodeIdx >= 0) {
    const p = line.indexOf('(', nodeIdx);
    if (p >= 0) {
      let depth = 0;
      let q = p;
      for (; q < line.length; q++) {
        if (line[q] === '(') depth++;
        else if (line[q] === ')') {
          depth--;
          if (depth === 0) {
            q++;
            break;
          }
        }
      }
      const inside = line.substring(p + 1, q - 1); // 不含最外层括号
      const kv = parseKeyValueContent(inside);

      const levelRaw = kv['level'] || kv['levelName'] || '';
      const level = (levelRaw || '').toString().replace(/^"|"$/g, '').toUpperCase();

      let message = kv['message'] || kv['msg'] || '';
      if (typeof message === 'string') {
        message = message.replace(/^"|"$/g, '');
      }
      if (!message) message = line;

      let nodeName = kv['nodeName'] || '';
      if (typeof nodeName === 'string') nodeName = nodeName.replace(/^"|"$/g, '');

      // time fallback from timestamp
      if (!time && kv['timestamp']) {
        const tsRaw = kv['timestamp'].replace(/^"|"$/g, '');
        try {
          const d = new Date(tsRaw);
          if (!isNaN(d.getTime())) {
            const pad = (v, len = 2) => String(v).padStart(len, '0');
            time = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${String((d.getMilliseconds())).padStart(3,'0')}`;
          } else {
            time = tsRaw;
          }
        } catch (e) {
          time = tsRaw;
        }
      }

      // 解析并格式化 meta（全部直接显示）
      let metaFormatted = '';
      if (kv['meta']) {
        let metaRaw = kv['meta'].toString().trim();
        // 如果 meta 是带 {} 的整体，去掉最外层大括号并 parse
        let inner = metaRaw;
        if (inner.startsWith('{') && inner.endsWith('}')) {
          inner = inner.substring(1, inner.length - 1);
        }
        try {
          const parsed = parseKeyValueContent(inner);
          const norm = normalizeParsedMeta(parsed);
          metaFormatted = formatMetaObject(norm, 0);
        } catch (e) {
          // 若解析失败，则直接把原始字符串拆成多行（做简单处理）
          metaFormatted = metaRaw.replace(/, /g, ',\n');
        }
      }

      // 如果 message 中已经包含大段文本，也可能想把它换行显示，但保留原样
      return {
        time: time || '',
        level: level || 'UNKNOWN',
        message: message,
        nodeName: nodeName || '',
        raw: rawLine,
        metaFormatted: metaFormatted // 直接显示（可能为空字符串）
      };
    }
  }

  // 非 NodeLog 格式，尝试识别级别前缀
  const parts = line.split(/\s+/, 2);
  let level = '';
  let message = line;
  if (parts.length > 1 && /^(DEBUG|INFO|WARN|ERROR|TRACE)$/i.test(parts[0])) {
    level = parts[0].toUpperCase();
    message = line.substring(parts[0].length).trim();
  } else {
    level = 'UNKNOWN';
    message = line;
  }

  return { time: time || '', level, message, nodeName: '', raw: rawLine, metaFormatted: '' };
}

/** 将后端返回处理为统一行为（拆行并解析） */
function handlePayload(payload) {
  if (!payload) return;
  const piece = payload.log || '';
  const nextOffset = payload.nextOffset != null ? payload.nextOffset : logger.value.start;
  const more = !!payload.hasMoreLog;

  if (piece.length > 0) {
    const lines = piece.split('\n');
    for (const l of lines) {
      const parsed = parseLine(l);
      if (parsed) {
        logLines.value.push(parsed);
      }
    }

    // 滚动到底部
    nextTick(() => {
      const element = logOutput.value;
      if (element && scrollbarRef.value && typeof scrollbarRef.value.setScrollTop === 'function') {
        scrollbarRef.value.setScrollTop(element.scrollHeight);
      } else if (element) {
        element.scrollTop = element.scrollHeight;
      }
    });
  }

  hasMoreLog.value = more;
  logger.value.start = nextOffset;
}

/** 轮询：每秒拉取一次（可按需调整） */
function simulateLogOutput(processInstanceId , nodeId) {
  if (interval.value) clearInterval(interval.value);

  interval.value = setInterval(() => {
    readLog(processInstanceId, logger.value.start , nodeId).then(res => {
      const payload = res && res.data ? res.data : null;
      if (!payload) {
        loading.value = false;
        return;
      }

      handlePayload(payload);

      if (!hasMoreLog.value) {
        clearInterval(interval.value);
        interval.value = null;
      }

      loading.value = false;
    }).catch(err => {
      console.error('readLog error', err);
      loading.value = false;
      clearInterval(interval.value);
      interval.value = null;
    });
  }, 1000);
}

function clearLoggerInterval(){
  if (interval.value) {
    clearInterval(interval.value);
    interval.value = null;
  }
}

function connectLogger(processInstanceId , nodeId) {
  if (!processInstanceId) {
    console.warn('connectLogger: missing processInstanceId');
    return;
  }
  loading.value = true;
  logger.value.start = 0;
  logger.value.processInstanceId = processInstanceId;
  logger.value.nodeId = nodeId ;
  logLines.value = [];
  hasMoreLog.value = true;
  simulateLogOutput(processInstanceId , nodeId);
}

function fetchLog(processInstanceId) {
  const id = processInstanceId || logger.value.processInstanceId;
  if (!id) {
    console.warn('fetchLog: missing processInstanceId');
    return;
  }
  const nodeId = logger.value.nodeId;
  loading.value = true;
  readLog(id, logger.value.start , nodeId).then(res => {
    const payload = res && res.data ? res.data : null;
    if (!payload) {
      loading.value = false;
      return;
    }
    handlePayload(payload);
    loading.value = false;
  }).catch(err => {
    console.error('fetchLog error', err);
    loading.value = false;
  });
}

function onRefresh() {
  fetchLog();
}

function onClear() {
  logLines.value = [];
  logger.value.start = 0;
}

defineExpose({
  connectLogger,
  clearLoggerInterval,
  fetchLog,
  onClear
});

onUnmounted(() => clearLoggerInterval());
</script>

<style scoped lang="scss">
.process-log-viewer {
  .log-container {
    background-color: #272822;
    color: #f8f8f2;
    padding: 10px;
    border-radius: 10px;
    overflow: auto;
    height: calc(100vh - 90px);
    font-size: 13px;
    line-height: 15px;

    .log-output {
      white-space: pre-wrap;
      word-break: break-word;
    }
  }

  .line-header {
    display: flex;
    gap: 8px;
    align-items: baseline;
  }

  .message {
    padding: 3px ;
    margin-bottom: 0px;
    line-height: 1.0rem;
  }

  .log-line {
    display: block;
    gap: 8px;
    padding: 0px 0;
  }

  .log-line .time {
    color: #888;
    display: inline-block;
    flex: 0 0 auto;
    min-width: 200px;
    font-size: 13px;
  }
  .log-line .message {
    white-space: pre-wrap;
    word-break: break-word;
    display: inline-block;
    max-width: calc(100% - 220px);
  }
  .log-line .meta {
    color: #999;
    font-size: 12px;
    margin-left: 6px;
  }

  /* meta 完整块样式 */
  .meta-block {
    margin-top: 3px;
    margin-left: 200px; /* 对齐到 message 之后的位置 */
    margin-bottom: 3px;
  }

  .meta-pre {
    background: #1e1f1c;
    color: #dcdcdc;
    padding: 10px;
    border-radius: 4px;
    max-height: 60vh;
    overflow: auto;
    white-space: pre-wrap; /* 保持换行同时允许长行换行 */
    word-break: break-word;
    font-family: "Courier New", Courier, monospace;
    font-size: 12px;
    line-height: 1.4;
    margin: 0;
  }

  /* 级别颜色 */
  .level-ERROR { color: #ff6b6b; }   /* 红 */
  .level-WARN  { color: #ffd166; }  /* 黄 */
  .level-INFO  { color: #8bd3ff; }  /* 浅蓝 */
  .level-DEBUG { color: #9aedc5; }  /* 绿 */
  .level-TRACE { color: #bbbbbb; }  /* 灰 */
  .level-UNKNOWN { color: #f8f8f2; } /* 默认白色 */

  .log-footer {
    margin-top: 10px;
    display: flex;
    align-items: center;
  }
}
</style>