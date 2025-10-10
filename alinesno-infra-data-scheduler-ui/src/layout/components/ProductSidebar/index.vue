<template>
  <div class="siderbar">
    <el-menu default-active="1" class="el-menu-vertical" :collapse="isCollapse" @open="handleOpen" @close="handleClose">
      <el-menu-item index="1" @click="jumpTo" class="menu-item">
        <i class="fa-solid fa-desktop"></i>
        <span>
          仪盘表 
        </span>
      </el-menu-item>

      <el-tooltip effect="dark" :content="item.desc" v-for="item in menuItems" :key="item.id" placement="right">
          <el-menu-item :index="item.id" @click="openServiceList(item.link)" class="menu-item">
            <i :class="item.icon"></i>
            <span>
              {{ item.desc }}
            </span>
          </el-menu-item>
      </el-tooltip>

    </el-menu>

    <el-menu style="" class="el-menu-vertical acp-suggest" :collapse="isCollapse" @open="handleOpen" @close="handleClose">
      <!-- <el-tooltip effect="dark" :content="item.desc" v-for="item in footerMenuItems" :key="item.id" placement="right"> -->
          <el-menu-item 
            :index="item.id" 
            v-for="item in footerMenuItems" 
            :key="item.id" 
            @click="openServiceList(item.link)" 
            class="menu-item">
            <i :class="item.icon"></i>
            <span>
              {{ item.desc }}
            </span>
          </el-menu-item>
      <!-- </el-tooltip> -->
    </el-menu>

  </div>
</template>

<script setup>

const dialogVisible = ref(false)
const router = useRouter();

// 菜单列表
const menuItems = ref([
  // {id:'2' , icon:'fa-solid fa-feather' , link:'/data/scheduler/project/index' , desc:'项目'},
  {id:'3' , icon:'fa-solid fa-masks-theater' , link:'/data/scheduler/processDefinition/index' , desc:'流程'},
  {id:'5' , icon:'fa-brands fa-wordpress' , link:'/data/scheduler/processInstance/index' , desc:'实例'},
  {id:'15' , icon:'fa-solid fa-box-open' , link:'/data/scheduler/catelog/index' , desc:'分类'},
  {id:'8' , icon:'fa-solid fa-file-pdf' , link:'/data/scheduler/resources/index' , desc:'资源'},
  {id:'7' , icon:'fa-solid fa-database' , link:'/data/scheduler/datasource/index' , desc:'数据源'},
  {id:'4' , icon:'fa-brands fa-skype' , link:'/data/scheduler/analyse/index' , desc:'监控'},
  {id:'9' , icon:'fa-solid fa-code-pull-request' , link:'/data/scheduler/apiRecord/index' , desc:'记录'},
]);

// 底部菜单
const footerMenuItems = ref([
  {id: '13', icon: 'fa-solid fa-robot', link: '/data/scheduler/llmModel/index', desc: '大模型'},
  {id:'10' , icon:'fa-solid fa-cog' , link:'/data/scheduler/configuration/index' , desc:' 系统配置'},
])

// 打开服务市场
function openServiceList(_path) {
  router.push({ path: _path });
}

// 打开客户配置
function jumpToConstomTheme() {
  router.push({ path: "/dashboard/dashboardTheme" });
}

// 打开首页
function jumpTo() {
  router.push({ path: "/index" });
}

// 打开智能客服
function openSmartService() {
  router.push({ path: "/dashboard/smartService" });
}

</script>

<style lang="scss" scoped>
.el-menu-vertical:not(.el-menu--collapse) {
  width: 65px;
}

.acp-suggest {
  bottom: 20px;
  position: absolute;
}

.siderbar {
  float: left;
  height: 100%;
  width: 64px;
  border-right: 1px solid #e6e6e6;
  padding-top: 40px;
  overflow: hidden;
  background-color: #fff;
  position: fixed;
}

.menu-item , .acp-suggest {
  display: flex;
  justify-content: center;
  flex-direction: column;
  align-items: center;
  line-height: 1.4rem;
  font-size: 14px !important;
  margin-bottom: 10px;
  padding-top:10px;
  margin: 2px;
  border-radius: 10px;
  width: calc(100% - 5px);

  span{
    font-size: 12px;
    color: #888;
  }
}
</style>