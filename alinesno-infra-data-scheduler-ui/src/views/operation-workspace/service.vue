<template>
  <div>
    <el-row class="acp-dashboard-panel" :gutter="20">
      <el-col class="panel-col" :span="19">
        <div class="grid-content">
          <div class="panel-header">
            <div class="header-title"><i class="fa-solid fa-file-waveform"></i> 最近运行编排任务</div>
          </div>
          <div class="panel-body acp-height-auto" style="padding: 0;padding-bottom: 10px;">
            <div class="acp-app-list">
              <ul>
                <li class="app-items" v-for="(item , index) in apps" :key="index" 
                  style="width:calc(25% - 10px);display: flex;border-radius: 6px;" 
                  @click="handleClick(item)">
                  <div class="app-icon">
                    <i :class="item.icon" style="font-size: 20px" />
                  </div>
                  <div class="app-info">
                    <div class="app-item-title">{{ item.name }}</div>
                    <div class="app-item desc">
                      <el-text line-clamp="1">
                        {{ item.description == null ? '此流程定义服务未添加描述信息': item.description }}
                      </el-text>
                    </div>
                  </div>
                </li>
              </ul>
              <div v-if="apps.length == 0">
                <el-empty image-size="120" description="任务列表为空，暂时还未配置任务" />
              </div>
            </div>
          </div>
        </div>
      </el-col>

      <el-col :span="5">
        <div class="grid-content">
          <div class="panel-header">
            <div class="header-title"><i class="fa-solid fa-user-nurse"></i> 任务状态列表</div>
          </div>
          <div class="panel-body acp-height-auto">
            <ul class="panel-item-text">
              <li style="width:50%;padding:4px;border-bottom: 0px;" v-for="item in opertionAssets" :key="item.id">
                <div class="item-health-box">
                  <div class="item-health-title">{{ item.title }}</div>
                  <div class="item-health-count">{{ item.count }}</div>
                </div>
              </li>
            </ul>
          </div>
          <div class="panel-footer">
            <div class="footer-link">
            </div>
          </div>
        </div>
      </el-col>

    </el-row>
  </div>
</template>

<script setup>

import { 
  recentlyProcess , 
  simpleStatistics 
} from '@/api/data/scheduler/dashboard'

const router = useRouter()

const opertionAssets = ref([
  { id: '6', title: '排队任务', enTitle: 'queueCount', count: 0},
  { id: '1', title: '总任务', enTitle: 'taskCount', count: 0},
  { id: '2', title: '运行中', enTitle: 'runningTaskCount', count: 0},
  { id: '3', title: '异常任务', enTitle: 'errorTaskCount', count: 0},
  { id: '4', title: '完成任务', enTitle: 'completeTaskCount', count: 0}
]);

const apps = ref([])

/** 查询出最近的业务列表 */
function handleRecentlyProcess(){
  recentlyProcess().then(res => {
    console.log(res)
    apps.value = res.data ;
  })
}

/** 查询出简单的统计信息 */
function handleSimpleStatistics(){
  simpleStatistics().then(res => {
    const updatedAssets = opertionAssets.value.map(item => ({
        ...item, // 复制原有的id, title 和 enTitle
        count: res.data[item.enTitle] || item.count // 更新count值或保持不变
      }));

      // 更新opertionAssets为新的格式
      opertionAssets.value = updatedAssets; 
  })
}

const handleClick = (item) => {
  console.log(item)
  // 跳转到 /data/scheduler/processInstance/index?processId=1870251951331667970
  router.push({
    path: '/data/scheduler/processInstance/index',
    query: {
      processId: item.id , 
      fromWhere: 'dashboard'
    }
  })
}

handleRecentlyProcess()
handleSimpleStatistics() 

</script>

<style lang="scss" scoped>
.item-health-title {
  margin-bottom: 5px !important;
}

.item-health-count {
  margin-bottom: 5px;
}

.acp-dashboard .acp-app-list ul {

  .app-status {
    float: right;
    margin-right: 10px;
    font-size: 13px;
    line-height: 13px;
    color: #545b64;
  }

  li.app-items .app-item.desc {
    font-size: 12px;
    color: #545b64;
    margin-top: 5px;
  }

  li.app-items {
    list-style: none;
    float: left;
    border-bottom: 1px solid #fafafa;
    width: 50%;
    cursor: pointer;
    padding: 10px 0px;
    width: calc(33% - 10px);
    background: #f5f5f5;
    border-radius: 3px;
    padding: 10px;
    margin-right: 5px;
    margin-top: 10px;
    margin-left: 5px;

    &:hover { 
      background: #f9f9f9;
      .app-item-title{
        font-weight: bold;
      }
    }
  }
}
</style>