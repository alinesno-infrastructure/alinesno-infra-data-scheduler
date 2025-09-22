<template>
  <div>
  <el-scrollbar style="height:calc(100vh - 200px)" ref="scrollbarRef" >
    <div class="debugger-process-flow-panel" ref="innerRef">
      <div
        class="flow-panel-item"
        v-for="(item, index) in nodes"
        :key="item.id"
      >
        <div class="left">
          <div class="index">{{ index + 1 }}</div>
            <!-- 运行中显示 loading 图标 -->
            <el-button v-if="item.executionStatus === 'executing'" type="warning" text bg size="large" circle loading>
            </el-button>
            <el-button v-if="item.executionStatus === 'completed'" type="success" text bg  size="large" circle>
              <i class="fa-solid fa-check"></i>
            </el-button>
          <div class="meta">
            <div class="flow-panel-item-title">
              <span class="flow-panel-item-title-icon">
                <!-- <i :class="item.node.icon"></i> -->
                <img :src="getTaskIconPath(item.node.icon)" />
              </span>
              {{ item.node.stepName }}
            </div>
            <!-- <div class="node">节点: {{ item.node }}</div> -->
          </div>

          <div class="right">
            <div class="time" >
              <span v-if="item.executeTime && item.finishTime">
                ({{ usageTime(item.executeTime, item.finishTime)?.text}})
              </span>
              {{ item.addTime }}
            </div>


            <div class="log" @click="openLog(item)">查看日志</div>
          </div>
        </div>

        <div class="center">
          <div class="debugger-message">
            <el-text line-clamp="2" style="line-height: 1.5rem;">
              {{ item.executeInfo ? item.executeInfo: '暂时未有运行日志'  }}
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
      <div v-if="nodes.length == 0" style="margin-top:13vh">
        <el-empty description="当前流程未运行,暂时无数据,请点击下面的按键执行任务" ></el-empty>
      </div>
    </div>
  </el-scrollbar>

  <div style="margin-top:15px;">
    <el-button type="primary" 
        :loading="!isCompleted"
        @click="handleTryRun()"> 
      <i class="fa-solid fa-paper-plane"></i> &nbsp; 开始执行 
    </el-button>
  </div>

  <!-- 日志弹窗组件 -->
  <LogViewer v-model:visible="logDialogVisible" :title="currentTitle" :log="currentLog" />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, onUnmounted, watch, getCurrentInstance, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  tryRun,
  getLastExecutedFlowNodes
} from '@/api/data/scheduler/flow'

import LogViewer from './LogViewer.vue' // 请根据实际路径调整

const innerRef = ref(null); // 滚动条的处理_starter
const scrollbarRef = ref(null);

const router = useRouter();
const currentProcessDefinitionId = ref(router.currentRoute.value.query.processDefinitionId);

const nodes = ref([])

const logDialogVisible = ref(false)
const currentLog = ref('')
const currentTitle = ref('')

function initChatBoxScroll() {

  nextTick(() => {
    const element = innerRef.value;  // 获取滚动元素
    const scrollHeight = element.scrollHeight;

    scrollbarRef.value.setScrollTop(scrollHeight);
  })

}

function openLog(item) {
  currentTitle.value = `${item.node} - ${item.title}`
  currentLog.value = item.log || '无日志内容'
  logDialogVisible.value = true
}

const usageTime = (executeTime, finishTime) => {
  // 支持两种调用方式：xxx({ executeTime, finishTime }) 或 xxx(executeTime, finishTime)
  if (typeof executeTime === 'object' && executeTime !== null) {
    finishTime = executeTime.finishTime;
    executeTime = executeTime.executeTime;
  }

  if (!executeTime || !finishTime) return null;

  const start = new Date(executeTime);
  const end = new Date(finishTime);
  if (isNaN(start.getTime()) || isNaN(end.getTime())) return null;

  // 计算差值（毫秒）
  let diffMs = end.getTime() - start.getTime();
  // 若结束时间早于开始时间，取绝对值（可根据需要改为返回 null）
  if (diffMs < 0) diffMs = Math.abs(diffMs);

  // 秒（四舍五入到整数秒）
  const seconds = Math.round(diffMs / 1000);

  // 分钟（保留最多两位小数，去掉多余的0）
  const rawMinutes = seconds / 60;
  const minutes = parseFloat(rawMinutes.toFixed(2)); // 例如 1.5 -> 1.5；2.00 -> 2

  const text = seconds < 60 ? `${seconds}秒` : `${minutes}分钟`;

  return { seconds, minutes, text };
};

// ---------- 轮询逻辑 ----------
let timer = null
const isCompleted = ref(true)
const pollingInterval = 2000
const isPolling = ref(false)

async function fetchNodes() {
  if (!currentProcessDefinitionId.value) {
    return
  }

    getLastExecutedFlowNodes(currentProcessDefinitionId.value).then(resp => {

      let dataList = []
      if (!resp || resp.status === 'no_run') {
        // nothing
        return
      }

      console.log('getLastExecutedFlowNodes = ' + JSON.stringify(resp))

      // 如果已经结束则停止轮询
      if (resp.status === 'completed') {
        console.log('任务已结束，停止轮询')
        // stopPolling();
      }

      if (Array.isArray(resp.data)) {
        dataList = resp.data 
      } else if (Array.isArray(resp.data)) {
        dataList = resp.data
      } else if (Array.isArray(resp.result)) {
        dataList = resp.result
      } else {
        // 如果数据在 ajax 本体的某个字段，请根据实际后端返回结构调整这里
        // 不存在数组则直接返回
        return
      }

      nodes.value = dataList ;

      initChatBoxScroll();
    })

  // try {
  //   const resp = await getLastExecutedFlowNodes(currentProcessDefinitionId.value)

  //   let dataList = []
  //   if (!resp || resp.status === 'no_run') {
  //     // nothing
  //     return
  //   }

  //   console.log('getLastExecutedFlowNodes = ' + JSON.stringify(resp))

  //   // 如果已经结束则停止轮询
  //   if (resp.status === 'completed') {
  //     console.log('任务已结束，停止轮询')
  //     // stopPolling();
  //   }

  //   if (Array.isArray(resp.data)) {
  //     dataList = resp.data 
  //   } else if (Array.isArray(resp.data)) {
  //     dataList = resp.data
  //   } else if (Array.isArray(resp.result)) {
  //     dataList = resp.result
  //   } else {
  //     // 如果数据在 ajax 本体的某个字段，请根据实际后端返回结构调整这里
  //     // 不存在数组则直接返回
  //     return
  //   }

  //   nodes.value = dataList ;
  // } catch (error) {
  //   // 可以选择在出错时停止轮询或仅打印错误
  //   console.error('fetchNodes error:', error)
  // }

}

function startPolling() {
  if (isPolling.value) return
  isPolling.value = true
  // 立即执行一次
  fetchNodes()
  timer = setInterval(() => {
    fetchNodes()
  }, pollingInterval)
}

function stopPolling() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  isPolling.value = false
}

const handleTryRun = () => {
  isCompleted.value = false ;

  tryRun(currentProcessDefinitionId.value).then(response => {

    isCompleted.value = true;
    ElMessage.success('任务执行结束');

  }).catch(error => {
    isCompleted.value = true;
  })
}


// 当组件卸载时清除定时器
onUnmounted(() => {
  stopPolling()
})

// 建议父组件通过 ref 显式控制（drawer 打开/关闭时），因此暴露 start/stop 方法
defineExpose({
  startPolling,
  stopPolling,
  fetchNodes,
  isPolling
})

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
        font-size: 14px;
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

.flow-panel-item-title-icon{
  img {
    width: 30px;
  }
}
</style>