<template>
  <div>
    <el-row class="acp-dashboard-panel" :gutter="20">
      <el-col class="panel-col" :span="19">
        <div class="grid-content">
          <div class="panel-header">
            <div class="header-title"><i class="fa-solid fa-file-waveform"></i> 快速常用项目列表</div>
          </div>
          <div class="panel-body acp-height-auto" style="padding: 0;padding-bottom: 10px;">
            <div class="acp-app-list">
              <ul>
                <li class="app-items" v-for="(item , index) in apps" :key="index" style="width:calc(25% - 10px)">
                  <div class="app-icon">
                    <!-- <img :src="item.icon" :alt="item.name" /> -->
                    <img :src="'http://data.linesno.com/icons/dashboard/'+(index+1)+'.svg'" :alt="item.name" />
                  </div>
                  <div class="app-info">
                    <div class="app-item-title">{{ item.name }}</div>
                    <div class="app-item desc">{{ item.description == null ? '此流程定义服务未添加描述信息': item.description }}</div>
                  </div>
                  <!-- <div class="app-status">
                    <el-icon>
                      <CircleCheckFilled />
                    </el-icon> 正常
                  </div> -->
                </li>
              </ul>
            </div>
          </div>
          <!-- <div class="panel-footer">
            <div class="footer-link">
            </div>
          </div> -->
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

import { recentlyProcess } from '@/api/data/scheduler/dashboard'

const opertionAssets = ref([
        { id: '6', title: '总项目数', count: 45 },
        { id: '1', title: '总任务', count: 45 },
        { id: '2', title: '运行中', count: 145 },
        { id: '3', title: '异常任务', count: 65 },
        { id: '4', title: '完成任务', count: 85 },
        { id: '5', title: '中断任务', count: 45 },
      ])

const apps = ref([])

/** 查询出最近的业务列表 */
function handleRecentlyProcess(){
  recentlyProcess().then(res => {
    console.log(res)
    apps.value = res.data ;
  })
}

handleRecentlyProcess()

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
    padding: 10px 0px;
    width: calc(33% - 10px);
    background: #fafafa;
    border-radius: 3px;
    padding: 10px;
    margin-right: 5px;
    margin-top: 10px;
    margin-left: 5px;
  }
}
</style>