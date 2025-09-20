<template>
  <div>
  <el-scrollbar style="height:calc(100vh - 230px)">
    <div class="debugger-process-flow-panel">
      <div
        class="flow-panel-item"
        v-for="(item, index) in nodes"
        :key="item.id"
      >
        <div class="left">
          <div class="index">{{ index + 1 }}</div>
          <div class="icon">
            <i class="arrow">→</i>
          </div>
          <div class="meta">
            <div class="flow-panel-item-title">{{ item.title }}</div>
            <div class="node">节点: {{ item.node }}</div>
          </div>

          <div class="right">
            <div class="time">{{ item.time }}</div>

            <!-- 运行中显示 loading 图标 -->
            <el-icon
              v-if="item.status === '运行中'"
              class="status-loading-icon"
              aria-hidden="true"
            >
              <Loading />
            </el-icon>

            <el-tag :type="statusTagType(item.status)" size="mini">
              {{ item.status }}
            </el-tag>

            <div class="log" @click="openLog(item)">查看日志</div>
          </div>
        </div>

        <div class="center">
          <div class="debugger-message">
            <el-text line-clamp="2">
              {{ item.message }}
            </el-text>
          </div>

          <div v-if="item.progress !== null" class="progress">
            <div class="progress-bar">
              <div
                class="progress-fill"
                :style="{ width: item.progress + '%' }"
              ></div>
            </div>
            <div class="progress-text">{{ item.progress }}%</div>
          </div>
        </div>
      </div>
    </div>
  </el-scrollbar>

  <div style="margin-top:15px;">
    <el-button type="primary" @click="handleTryRun()"> 
      <i class="fa-solid fa-paper-plane"></i> &nbsp; 开始执行 
    </el-button>
    <el-button type="danger"> 
      <i class="fa-solid fa-circle-stop"></i> &nbsp; 停止 
    </el-button>
  </div>

  <!-- 日志弹窗组件 -->
  <LogViewer v-model:visible="logDialogVisible" :title="currentTitle" :log="currentLog" />
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  tryRun
} from '@/api/data/scheduler/flow'

import LogViewer from './LogViewer.vue' // 请根据实际路径调整

const router = useRouter();
const currentProcessDefinitionId = ref(router.currentRoute.value.query.processDefinitionId);

const nodes = reactive([
  {
    id: 1,
    title: '开始处理批次 #20210809-01',
    node: '提取数据源',
    message: '成功提取 15,428 行数据，准备进入清洗阶段',
    status: '成功',
    time: '2021-08-09 10:05:05',
    progress: 100,
    log: '提取完成，耗时 12s，行数：15428\n详细：source=db1.table_a; filter=enabled=1'
  },
  {
    id: 2,
    title: '数据清洗',
    node: '清洗',
    message: '清洗中：发现空值 124 行，已做填充处理',
    status: '运行中',
    time: '2021-08-09 10:05:20',
    progress: 65,
    log: '清洗进度 65%。替换 null、标准化日期格式。\n示例：col_birth: 2020-01-01 -> 2020-01-01'
  },
  {
    id: 3,
    title: '字段映射与转换',
    node: '转换',
    message: '字段映射规则应用完成，部分记录触发类型转换警告',
    status: '成功',
    time: '2021-08-09 10:06:12',
    progress: 100,
    log: '转换完成，警告：年龄字段 3 条非数值，已置为默认值 0\nrowIds: 1101, 1145, 1189'
  },
  {
    id: 4,
    title: '数据校验',
    node: '验证',
    message: '校验失败：发现主键冲突 2 条，需要人工确认',
    status: '异常',
    time: '2021-08-09 10:06:50',
    progress: null,
    log: '校验错误：主键重复 id=1001, id=1002\n建议：人工合并或跳过重复记录'
  },
  {
    id: 5,
    title: '加载至临时表',
    node: '加载',
    message: '正在写入临时表，写入速率 1200 rows/s',
    status: '运行中',
    time: '2021-08-09 10:07:05',
    progress: 45,
    log: '加载中，当前写入 7,000 / 15,428\n目标表: tmp_etl_20210809'
  },
  {
    id: 6,
    title: '聚合统计',
    node: '聚合',
    message: '聚合任务排队，等待上一个节点完成后执行',
    status: '开始',
    time: '2021-08-09 10:07:20',
    progress: null,
    log: '聚合已加入队列，规则：按 region 分组计算 sum/avg'
  },
  {
    id: 7,
    title: '建立索引',
    node: '索引',
    message: '索引构建任务已触发，预计耗时 30s',
    status: '开始',
    time: '2021-08-09 10:07:35',
    progress: null,
    log: '索引任务已创建，字段: user_id_idx'
  },
  {
    id: 8,
    title: '质量回滚检查',
    node: '回滚检查',
    message: '检查通过，无需回滚',
    status: '成功',
    time: '2021-08-09 10:07:50',
    progress: 100,
    log: '数据完整性检查 OK，checksum 对比通过'
  },
  {
    id: 6,
    title: '聚合统计',
    node: '聚合',
    message: '聚合任务排队，等待上一个节点完成后执行',
    status: '开始',
    time: '2021-08-09 10:07:20',
    progress: null,
    log: '聚合已加入队列，规则：按 region 分组计算 sum/avg'
  },
  {
    id: 7,
    title: '建立索引',
    node: '索引',
    message: '索引构建任务已触发，预计耗时 30s',
    status: '开始',
    time: '2021-08-09 10:07:35',
    progress: null,
    log: '索引任务已创建，字段: user_id_idx'
  },
  {
    id: 8,
    title: '质量回滚检查',
    node: '回滚检查',
    message: '检查通过，无需回滚',
    status: '成功',
    time: '2021-08-09 10:07:50',
    progress: 100,
    log: '数据完整性检查 OK，checksum 对比通过'
  },
  {
    id: 9,
    title: '归档历史数据',
    node: '归档',
    message: '归档中：已处理 2 / 5 个分区',
    status: '运行中',
    time: '2021-08-09 10:08:05',
    progress: 40,
    log: '归档过程，中间文件 3 个，目标路径: s3://company/archive/2021-08-09/'
  },
  {
    id: 10,
    title: '完成通知',
    node: '通知',
    message: '等待所有节点完成后发送通知',
    status: '开始',
    time: '2021-08-09 10:08:20',
    progress: null,
    log: '通知服务已就绪，通知目标：ops@example.com'
  }
])

const logDialogVisible = ref(false)
const currentLog = ref('')
const currentTitle = ref('')

function openLog(item) {
  currentTitle.value = `${item.node} - ${item.title}`
  currentLog.value = item.log || '无日志内容'
  logDialogVisible.value = true
}

function statusTagType(status) {
  switch (status) {
    case '开始':
      return '' // default
    case '运行中':
      return 'warning'
    case '异常':
      return 'danger'
    case '成功':
      return 'success'
    default:
      return ''
  }
}

const handleTryRun = () => {
  tryRun(currentProcessDefinitionId.value).then(response => {
    ElMessage.success('开始执行成功');
  })
}

</script>

<style lang="scss" scoped>
.debugger-process-flow-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-radius: 6px;

  .flow-panel-item {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    background: #fafafa;
    padding: 12px;
    border-radius: 6px;
    gap: 2px;
    flex-direction: column;

    .left {
      display: flex;
      align-items: center;
      gap: 12px;
      min-width: 320px;
      width: 100%;
    }

    .index {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: #1d75b0;
      color:#fff ;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: bold;
    }

    .icon {
      .arrow {
        font-style: normal;
        color: #999;
      }
    }

    .meta {
      display: flex;
      gap: 10px;
      align-items: center;

      .flow-panel-item-title {
        font-weight: 600;
      }

      .node {
        font-size: 12px;
        color: #666;
      }
    }

    .center {
      flex: 1;
      padding: 8px 12px 0 12px;
      width: 100%;

      .debugger-message {
        color: #333;
        margin-bottom: 0;
      }

      .progress {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-top: 6px;

        .progress-bar {
          width: 200px;
          height: 8px;
          background: #f3f3f3;
          border-radius: 6px;
          overflow: hidden;

          .progress-fill {
            height: 100%;
            background: linear-gradient(90deg, #4caf50, #7bd389);
          }
        }

        .progress-text {
          font-size: 12px;
          color: #666;
        }
      }
    }

    .right {
      display: flex;
      flex-direction: row;
      align-items: center;
      min-width: 160px;
      gap: 10px;
      margin-left: auto;

      .time {
        font-size: 12px;
        color: #999;
      }

      .status-loading-icon {
        color: #ff9800;
        display: inline-flex;
        align-items: center;
        margin-right: 4px;

        svg {
          width: 18px;
          height: 18px;
          animation: spin 1s linear infinite;
          fill: currentColor;
        }
      }

      .log {
        font-size: 12px;
        color: #1e88e5;
        cursor: pointer;
      }
    }
  }
}

/* global keyframes (kept outside nesting) */
@keyframes spin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}
</style>