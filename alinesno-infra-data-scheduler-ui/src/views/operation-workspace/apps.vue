<template>
  <div>
    <el-row class="acp-dashboard-panel" :gutter="20">
      <el-col class="panel-col" :span="19">
        <div class="grid-content">
          <div class="panel-header">
            <div class="header-title"><i class="fa-solid fa-link"></i> 编排计算使用教程</div>
          </div>
          <div class="panel-body" style="height: auto;position: relative;">
            <div class="direct-box">
              <ul>
                <li class="box-item" v-for="item in directContent" :key="item">
                  <div class="dire-panel">
                    <div class="panel-title">
                      <!-- <img class="dire-panel-icon" :src="item.icon" :alt="item.name" /> -->
                      <i class="dire-panel-icon" :class="item.icon" :alt="item.name" />
                      {{ item.name }}
                    </div>
                    <div class="panel-describe">{{ item.desc }}</div>
                    <!-- <div class="panel-tip">访问链接</div> -->
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
            <div class="header-title"><i class="fa-solid fa-link"></i> 构建解决方案</div>
          </div>
          <div class="panel-body" style="height: auto;position: relative;">
            <div class="acp-app-list">
              <ul>
                <li class="app-items" style="width:100%" v-for="item in apps" :key="item">
                  <div class="app-info">
                    <div class="app-item-title">
                      <i :class="item.icon" :alt="item.name" /> {{ item.name }}
                    </div>
                    <div class="app-item desc" style="margin-top: 10px;line-height: 1.2rem;">
                      {{ item.desc }}
                    </div>
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

const directContent = ref([
  {
    "icon": "fa-solid fa-chart-line",
    "name": "快速数据集成迁移",
    "desc": "提供快速且无缝的数据集成迁移服务，帮助用户将数据从一个系统或平台迁移到另一个系统或平台。"
  },
  {
    "icon": "fa-solid fa-box-open",
    "name": "数据离线分析计算",
    "desc": "通过离线分析计算，对大量数据进行处理和分析，以获得有关数据集的洞察和结论。"
  },
  {
    "icon": "fa-solid fa-sort-amount-down-alt",
    "name": "数据分层关系规范",
    "desc": "定义和规范数据的分层关系，确保数据在不同层级之间的正确关联和一致性。"
  },
  {
    "icon": "fa-solid fa-lock",
    "name": "数据生命周期和仓库",
    "desc": "管理数据的完整生命周期，包括数据的创建、存储、使用、归档和销毁等各个阶段，并提供相应的数据仓库支持。"
  },
  {
    "icon": "fa-solid fa-network-wired",
    "name": "轻量级湖仓一体化",
    "desc": "将数据湖和数据仓库相结合，提供一体化的数据存储和管理解决方案，具有轻量级和高效的特点。"
  }
])

const apps = ref([
  {
    "icon": "fa-solid fa-lock",
    "name": "数据智能治理方案",
    "desc": "确保数据全生命周期的安全与合规，提升数据质量和使用效率，满足严格隐私法规要求。"
  },
  {
    "icon": "fa-solid fa-chart-line",
    "name": "DataOps集成一体",
    "desc": "融合DevOps理念与大数据技术，加速数据分析流程，支持快速迭代和企业级大规模数据处理需求。"
  },
  {
    "icon": "fa-solid fa-sort-amount-down-alt",
    "name": "数据离线分析",
    "desc": "支持深度数据挖掘和复杂查询，为用户提供全面的数据洞察，适用于大数据量的详尽分析任务。"
  }
])

</script>
