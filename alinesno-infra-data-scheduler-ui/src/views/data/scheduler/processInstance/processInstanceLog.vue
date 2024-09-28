<template>
  <el-scrollbar ref="scrollbarRef" class="log-container" v-loading="loading">
      <pre ref="logOutput" class="log-output">{{ logText }}</pre>
  </el-scrollbar>
  <div class="log-footer">
    <el-button v-if="hasMoreLog" text bg type="primary" loading>获取日志中</el-button> 
    <el-button v-else text bg type="primary" >日志获取结束</el-button> 
  </div>
</template>

<script setup>

import {
  readLog 
} from '@/api/data/scheduler/processInstance'
import { onUnmounted } from 'vue';

const logger = ref({
  start: 0
});

const logOutput = ref(null);
const logText = ref('');

// 滚动条的处理_starter
const scrollbarRef = ref(null)
const hasMoreLog = ref(true)
const loading = ref(false)
const processInstanceId = ref(null)
const start = ref(0)
const interval = ref(null) 

const simulateLogOutput = (processInstanceId) => {
  interval.value = setInterval(() => {

    // 获取新的日志
    readLog({'processInstanceId': processInstanceId,'start': logger.value.start }).then(res => {
      let loggerT = res.data;
      loading.value = false;
      
      logText.value += loggerT.log ; 

      // 滚动到底部
      nextTick(() => {
        const element = logOutput.value;  // 获取滚动元素
        const scrollHeight = element.scrollHeight;

        scrollbarRef.value.setScrollTop(scrollHeight) ; 
      })

      hasMoreLog.value = loggerT.hasMoreLog;
      logger.value.start = loggerT.nextOffset ;
      
    })
   
    console.log('!hasMoreLog.value = ' + !hasMoreLog.value) ;

    // 没有日志则结束请求
    if(!hasMoreLog.value){
      clearInterval(interval.value);
    }
      
    
  }, 1000); // 每 100ms 输出一行，可以根据需要调整
};

/** 清除日志输出 */
function clearLoggerInterval(){
  clearInterval(interval.value);
}

/** 绑定日志输出 */
function connectLogger(id){
  loading.value = true ;
  logger.value.start = 0; 
  logText.value = ''; 
  simulateLogOutput(id);
}

// 在组件卸载时清除定时器
onUnmounted(() => {
  console.log('unmounted')
  if (interval.value) {
    clearInterval(interval.value);
  }
});

defineExpose({
  connectLogger , clearLoggerInterval
})

</script>

<style scoped lang="scss">
.log-container {
  font-family: "Courier New", Courier, monospace;
  background-color: #272822;
  color: #f8f8f2;
  padding: 10px;
  border-radius: 4px;
  overflow: auto;
  height: calc(100vh - 90px);
  font-size: 13px;
  line-height: 15px;

  .log-output {
    white-space: pre-wrap;
    word-break: break-all;
  }

}
.log-footer {
  margin-top:10px;
}
</style>