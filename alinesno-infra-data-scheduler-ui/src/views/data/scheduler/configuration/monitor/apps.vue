<template>
  <div>
    <el-row class="acp-dashboard-panel" :gutter="20">
      <el-col class="panel-col" :span="19">
        <div class="grid-content">
          <div class="panel-header">
            <div class="header-title"><i class="fa-solid fa-link"></i> 业务最新巡检</div>
          </div>
          <div class="panel-body" style="height: auto;position: relative;">
              <div class="direct-box">
                <ul>
                  <li class="box-item" v-for="item in advantages" :key="item">
                    <div class="dire-panel">
                      <div class="panel-title">
                        <i class="dire-panel-icon" :class="item.icon" :alt="item.name" />
                        {{ item.title }} 
                      </div>
                      <div class="panel-describe">
                        {{ item.description }}
                      </div>
                    </div>
                  </li>
                </ul>
              </div>
          </div>
        </div>
      </el-col>
      <el-col :span="5">
        <div class="grid-content">
          <div class="panel-header">
            <div class="header-title"><i class="fa-solid fa-link"></i> 应用场景</div>
          </div>
          <div class="panel-body" style="height: auto;position: relative;">
            <div class="acp-app-list">
              <ul>
                <li class="app-items" style="width:100%" v-for="item in selectedAlarmScenarios" :key="item">
                  <div class="app-info">
                    <div class="app-item-title">
                      <i :class="item.icon" :alt="item.name" />
                        {{ item.name }}
                    </div>
                    <div class="app-item desc">{{ item.description }}</div>
                  </div>
                </li>
              </ul>
            </div>
          </div>

        </div>
      </el-col>
    </el-row>

  </div>
</template>

<script setup>

const advantages = [
  { icon: 'fa-solid fa-columns', title: '模块化设计', description: '系统采用模块化架构，便于管理和扩展，如产品管理。' },
  { icon: 'fa-solid fa-table-list', title: '详尽的产品管理', description: '提供全面的产品管理功能，包括产品列表、产品访问记录。' },
  { icon: 'fa-solid fa-comments-dollar', title: '完善的咨询服务', description: '涵盖产品评价、建议反馈以及咨询管理等功能。' },
  { icon: 'fa-solid fa-file-audit', title: '严格的安全审计', description: '具备操作日志和访问日志管理功能，能够追踪系统内的重要操作。' },
  { icon: 'fa-solid fa-file-pen', title: '高效的内容管理', description: '支持内容发布、公告管理以及友链设置等功能。' },
  { icon: 'fa-solid fa-user-gear', title: '直观的用户界面', description: '提供清晰的导航结构，便于用户快速定位所需功能。' }
];


const selectedAlarmScenarios = [
  { icon: 'fa-solid fa-box-open', name: '产品上线与管理', description: '在产品管理模块中，管理员可以添加新产品、编辑现有产品信息。' },
  { icon: 'fa-solid fa-comments-dollar', name: '客户咨询与反馈', description: '咨询服务模块允许用户提交产品评价和建议反馈。' },
  { icon: 'fa-solid fa-user-secret', name: '安全事件审计', description: '安全审计模块用于记录用户的操作日志和访问日志。' },
];


</script>