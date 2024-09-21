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
                <li class="app-items" v-for="item in apps" :key="item" style="width:calc(25% - 10px)">
                  <div class="app-icon">
                    <img :src="item.icon" :alt="item.name" />
                  </div>
                  <div class="app-info">
                    <div class="app-item-title">{{ item.name }}</div>
                    <div class="app-item desc">{{ item.desc }}</div>
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

<script>

export default {
  data() {
    return {
      opertionAssets: [
        { id: '6', title: '总项目数', count: 45 },
        { id: '1', title: '总任务', count: 45 },
        { id: '2', title: '运行中', count: 145 },
        { id: '3', title: '异常任务', count: 65 },
        { id: '4', title: '完成任务', count: 85 },
        { id: '5', title: '中断任务', count: 45 },
      ],
      apps: 
      [
        {
          "icon": "http://data.linesno.com/icons/dashboard/8.svg",
          "name": "全链路日志集成服务",
          "desc": "以实现全面的日志监控和分析。"
        },
        {
          "icon": "http://data.linesno.com/icons/dashboard/7.svg",
          "name": "日志离线分析服务",
          "desc": "通过离线分析技术，对日志数据进行深入分析"
        },
        {
          "icon": "http://data.linesno.com/icons/dashboard/6.svg",
          "name": "账户日志离线分析服务",
          "desc": "针对账户日志数据进行离线分析和处理"
        },
        {
          "icon": "http://data.linesno.com/icons/dashboard/5.svg",
          "name": "GPT对话日志分析服务",
          "desc": "对GPT对话日志进行分析和解析"
        },
        {
          "icon": "http://data.linesno.com/icons/dashboard/4.svg",
          "name": "应用规范安全分析服务",
          "desc": "通过对应用程序规范和安全性进行分析"
        },
        {
          "icon": "http://data.linesno.com/icons/dashboard/3.svg",
          "name": "用户画像分析服务",
          "desc": "通过分析用户的行为、兴趣和偏好等数据"
        },
        {
          "icon": "http://data.linesno.com/icons/dashboard/2.svg",
          "name": "推荐算法分析服务",
          "desc": "基于推荐算法对用户行为和偏好进行分析"
        },
        {
          "icon": "http://data.linesno.com/icons/dashboard/1.svg",
          "name": "安全日志分析服务",
          "desc": "检测和识别潜在的安全威胁和攻击行为"
        },
        {
          "icon": "http://data.linesno.com/icons/dashboard/7.svg",
          "name": "审计安全报告分析服务",
          "desc": "审计安全报告评估和改进其安全控制和合规性"
        }
      ]

    }
  }
}


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