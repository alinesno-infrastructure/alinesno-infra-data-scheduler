<template>
  <el-scrollbar style="height:calc(100vh - 230px)">
  <div class="runtime-config">

    <!-- 基本运行 -->
    <section class="card">
      <h3>基本运行</h3>
      <div class="row">
        <label>启用任务</label>
        <input type="checkbox" v-model="cfg.enabled" />
      </div>

      <div class="row">
        <label>并行度（线程 / 并发）</label>
        <input type="number" v-model.number="cfg.parallelism" min="1" />
      </div>

      <div class="row">
        <label>最大批量大小</label>
        <input type="number" v-model.number="cfg.batchSize" min="1" />
      </div>

      <div class="row">
        <label>单次超时（秒）</label>
        <input type="number" v-model.number="cfg.timeoutSeconds" min="0" />
      </div>
    </section>

    <!-- 错误处理与重试 -->
    <section class="card">
      <h3>错误处理 & 重试</h3>

      <div class="row">
        <label>重试次数</label>
        <input type="number" v-model.number="cfg.retry.count" min="0" />
      </div>

      <div class="row">
        <label>重试策略</label>
        <select v-model="cfg.retry.strategy">
          <option value="fixed">固定间隔</option>
          <option value="exponential">指数退避</option>
          <option value="fibonacci">斐波那契退避</option>
          <option value="no_retry">不重试</option>
        </select>
      </div>

      <div class="row" v-if="cfg.retry.strategy !== 'no_retry'">
        <label>初始间隔（秒）</label>
        <input type="number" v-model.number="cfg.retry.initialInterval" min="0" />
      </div>

      <div class="row" v-if="cfg.retry.strategy === 'exponential' || cfg.retry.strategy === 'fibonacci'">
        <label>最大间隔（秒）</label>
        <input type="number" v-model.number="cfg.retry.maxInterval" min="0" />
      </div>

      <div class="row">
        <label>重试是否幂等（只能在任务支持时生效）</label>
        <input type="checkbox" v-model="cfg.retry.idempotent" />
      </div>

      <div class="row">
        <label>失败记录策略</label>
        <select v-model="cfg.failurePolicy">
          <option value="stop">停止任务</option>
          <option value="skip_record">跳过错误记录并继续</option>
          <option value="move_to_dlq">移到 Dead Letter Queue</option>
          <option value="ignore">忽略</option>
        </select>
      </div>

      <div class="row">
        <label>错误记录保留天数</label>
        <input type="number" v-model.number="cfg.dlq.retentionDays" min="0" />
      </div>
    </section>

    <!-- 检查点 & 增量 -->
    <section class="card">
      <h3>检查点 / 增量</h3>

      <div class="row">
        <label>启用检查点</label>
        <input type="checkbox" v-model="cfg.checkpoint.enabled" />
      </div>

      <div class="row" v-if="cfg.checkpoint.enabled">
        <label>检查点频率（秒）</label>
        <input type="number" v-model.number="cfg.checkpoint.intervalSeconds" min="1" />
      </div>

      <div class="row" v-if="cfg.checkpoint.enabled">
        <label>检查点存储位置</label>
        <input v-model="cfg.checkpoint.storage" placeholder="例如：s3://bucket/checkpoints 或 /var/checkpoints" />
      </div>

      <div class="row">
        <label>增量键（可选）</label>
        <input v-model="cfg.incremental.key" placeholder="例如 updated_at 或 id" />
      </div>
    </section>

    <!-- 日志 & 指标 -->
    <section class="card">
      <h3>日志 / 指标 / 告警</h3>

      <div class="row">
        <label>日志级别</label>
        <select v-model="cfg.logging.level">
          <option>ERROR</option>
          <option>WARN</option>
          <option>INFO</option>
          <option>DEBUG</option>
        </select>
      </div>

      <div class="row">
        <label>保留日志（天）</label>
        <input type="number" v-model.number="cfg.logging.retentionDays" min="0" />
      </div>

      <div class="row">
        <label>上报指标（enable）</label>
        <input type="checkbox" v-model="cfg.metrics.enabled" />
      </div>

      <div class="row" v-if="cfg.metrics.enabled">
        <label>指标上报间隔（秒）</label>
        <input type="number" v-model.number="cfg.metrics.intervalSeconds" min="1" />
      </div>

      <div class="row">
        <label>告警阈值（错误率 ≥ %）</label>
        <input type="number" v-model.number="cfg.alerts.errorRatePct" min="0" max="100" />
      </div>

      <div class="row">
        <label>告警接收</label>
        <select v-model="cfg.alerts.channel">
          <option value="email">Email</option>
          <option value="webhook">Webhook</option>
          <option value="slack">Slack</option>
          <option value="none">无</option>
        </select>
      </div>

    </section>

  </div>
  </el-scrollbar>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue';

const STORAGE_KEY = 'workflow_runtime_config_v1';

function defaultCfg() {
  return {
    enabled: true,
    parallelism: 4,
    batchSize: 100,
    timeoutSeconds: 300,

    retry: {
      count: 3,
      strategy: 'exponential', // fixed | exponential | fibonacci | no_retry
      initialInterval: 5,
      maxInterval: 300,
      idempotent: false,
    },

    failurePolicy: 'move_to_dlq', // stop | skip_record | move_to_dlq | ignore
    dlq: {
      path: 'dlq/default',
      retentionDays: 7,
    },

    checkpoint: {
      enabled: true,
      intervalSeconds: 300,
      storage: 's3://bucket/checkpoints',
    },

    incremental: {
      key: '',
    },

    logging: {
      level: 'INFO',
      retentionDays: 30,
    },

    metrics: {
      enabled: true,
      intervalSeconds: 60,
    },

    alerts: {
      errorRatePct: 10,
      channel: 'email', // email | webhook | slack | none
      emailTo: '',
      webhookUrl: '',
    },

    // 保持 JSON 导出友好
  };
}

const cfg = reactive(defaultCfg());
const lastSaved = ref('');
const message = ref('');
const fileInput = ref(null);

onMounted(() => {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (raw) {
    try {
      const parsed = JSON.parse(raw);
      Object.assign(cfg, parsed);
      lastSaved.value = new Date().toLocaleString();
    } catch (e) {
      // ignore parse errors
    }
  }
});

function saveConfig() {
  try {
    const copy = JSON.parse(JSON.stringify(cfg));
    localStorage.setItem(STORAGE_KEY, JSON.stringify(copy));
    lastSaved.value = new Date().toLocaleString();
    message.value = '已保存配置到本地。';
    clearMessageLater();
  } catch (e) {
    message.value = '保存失败：' + e.message;
    clearMessageLater();
  }
}

function exportConfig() {
  const blob = new Blob([JSON.stringify(cfg, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'runtime_config.json';
  a.click();
  URL.revokeObjectURL(url);
}

function triggerFile() {
  if (fileInput.value) fileInput.value.click();
}
function importConfig(e) {
  const f = e.target.files && e.target.files[0];
  if (!f) return;
  const reader = new FileReader();
  reader.onload = () => {
    try {
      const parsed = JSON.parse(reader.result);
      Object.keys(parsed).forEach(k => {
        cfg[k] = parsed[k];
      });
      lastSaved.value = new Date().toLocaleString();
      message.value = '导入成功';
    } catch (err) {
      message.value = '导入失败：无效 JSON';
    } finally {
      clearMessageLater();
    }
  };
  reader.readAsText(f);
  e.target.value = '';
}

function validateConfig() {
  const problems = [];
  if (cfg.parallelism < 1) problems.push('并行度必须 >= 1');
  if (cfg.batchSize < 1) problems.push('批量大小必须 >= 1');
  if (cfg.timeoutSeconds < 0) problems.push('超时必须 >= 0');
  if (cfg.retry.count < 0) problems.push('重试次数不能为负数');

  if (cfg.checkpoint.enabled && (!cfg.checkpoint.storage || cfg.checkpoint.intervalSeconds < 1)) {
    problems.push('启用检查点时需配置存储位置且间隔 >= 1 秒');
  }

  if (problems.length) {
    message.value = '校验未通过：' + problems.join('；');
  } else {
    message.value = '校验通过';
  }
  clearMessageLater();
}

function resetToDefault() {
  const d = defaultCfg();
  Object.keys(d).forEach(k => { cfg[k] = d[k]; });
  message.value = '已重置为默认配置';
  clearMessageLater();
}

function clearMessageLater(ms = 4000) {
  setTimeout(() => { message.value = ''; }, ms);
}

const jsonPreview = computed(() => JSON.stringify(cfg, null, 2));
</script>

<style scoped lang="scss">
.runtime-config {
  max-width: 900px;
  margin: 0 auto;
  font-family: system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial;
  color: #222;

  h3 {
    margin-bottom: 20px !important;
  }

  .card {
    border: 1px solid #e6e6e6;
    padding: 12px;
    margin-bottom: 12px;
    border-radius: 6px;
    background: #fff;
  }

  .row {
    display: flex;
    align-items: center;
    gap: 10px;
    margin: 8px 0;

    label {
      width: 180px;
      font-weight: normal;
      font-size: 14px;
    }

    input[type="text"],
    input[type="number"],
    input[type="password"],
    select,
    input {
      flex: 1;
      padding: 6px 8px;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 13px;
    }
  }

  .message {
    margin-top: 8px;
    color: #0a66c2;
  }

  .json-preview {
    pre {
      max-height: 280px;
      overflow: auto;
      background: #0f1724;
      color: #d6f8ff;
      padding: 12px;
      border-radius: 6px;
    }
  }
}
</style>