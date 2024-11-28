<template>
  <div class="siderbar">
    <el-menu default-active="1" class="el-menu-vertical" :collapse="isCollapse" @open="handleOpen" @close="handleClose">
      <el-menu-item index="1" @click="jumpTo">
        <i class="fa-solid fa-desktop"></i>
      </el-menu-item>

      <el-tooltip effect="dark" :content="item.desc" v-for="item in menuItems" :key="item.id" placement="right">
          <el-menu-item :index="item.id" @click="openServiceList(item.link)">
            <i :class="item.icon"></i>
          </el-menu-item>
      </el-tooltip>

    </el-menu>

  </div>
</template>

<script setup>

const dialogVisible = ref(false)
const router = useRouter();

// 菜单列表
const menuItems = ref([
  {id:'2' , icon:'fa-solid fa-feather' , link:'/data/scheduler/project/index' , desc:'项目管理'},
  {id:'3' , icon:'fa-solid fa-masks-theater' , link:'/data/scheduler/processDefinition/index' , desc:'流程管理'},
  {id:'5' , icon:'fa-brands fa-wordpress' , link:'/data/scheduler/processInstance/index' , desc:'流程实例'},
  {id:'15' , icon:'fa-solid fa-box-open' , link:'/data/scheduler/catelog/index' , desc:'流程分类'},
  {id:'8' , icon:'fa-solid fa-file-pdf' , link:'/data/scheduler/resources/index' , desc:'资源中心'},
  {id:'7' , icon:'fa-solid fa-database' , link:'/data/scheduler/datasource/index' , desc:'数据源管理'},
  {id:'4' , icon:'fa-brands fa-skype' , link:'/data/scheduler/analyse/index' , desc:'监控服务'},
  {id:'10' , icon:'fa-solid fa-computer' , link:'/data/scheduler/configuration/index' , desc:'配置管理'},
  {id:'9' , icon:'fa-solid fa-code-pull-request' , link:'/data/scheduler/apiRecord/index' , desc:'操作记录'},
]);

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
</style>