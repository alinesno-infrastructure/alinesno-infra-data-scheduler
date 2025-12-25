<template>
  <div class="side-menu-wrapper">
    <!-- collapse 属性：控制菜单收缩展开 -->
    <el-menu
        class="side-menu"
        :default-active="activeIndex"
        :router="true"
        :collapse="isCollapse"
    >
      <el-sub-menu index="myFile" class="my-file">
        <template #title>
          <el-icon :size="20">
            <Files></Files>
          </el-icon>
          <span>我的文件</span>
        </template>
        <el-menu-item
            index="0"
            :route="{ name: 'file', query: { fileType: 0, filePath: '/' } }"
        >
          <el-icon :size="20">
            <Menu></Menu>
          </el-icon>
          <span>全部</span>
        </el-menu-item>
        <el-menu-item
            index="1"
            :route="{ name: 'file', query: { fileType: 1 } }"
        >
          <el-icon :size="20">
            <Picture></Picture>
          </el-icon>
          <span>脚本</span>
        </el-menu-item>
        <el-menu-item
            index="2"
            :route="{ name: 'file', query: { fileType: 2 } }"
        >
          <el-icon :size="20">
            <Document></Document>
          </el-icon>
          <span>UDF</span>
        </el-menu-item>
        <el-menu-item
            index="3"
            :route="{ name: 'file', query: { fileType: 3 } }"
        >
          <el-icon :size="20">
            <VideoCamera></VideoCamera>
          </el-icon>
          <span>工具</span>
        </el-menu-item>
        <el-menu-item
            index="4"
            :route="{ name: 'file', query: { fileType: 4 } }"
        >
          <el-icon :size="20">
            <Headset></Headset>
          </el-icon>
          <span>配置</span>
        </el-menu-item>
        <el-menu-item
            index="5"
            :route="{ name: 'file', query: { fileType: 5 } }"
        >
          <el-icon :size="20">
            <TakeawayBox></TakeawayBox>
          </el-icon>
          <span>其他</span>
        </el-menu-item>
      </el-sub-menu>
    </el-menu>
  </div>
</template>

<script setup>
import {useRoute} from "vue-router";

const route = useRoute()
const isCollapse = ref(false) //  控制菜单收缩展开
// 当前激活菜单的 index
const activeIndex = computed(() => route.query.fileType ? String(route.query.fileType) : String(0)); //  获取当前路由参数中包含的文件类型

</script>

<style lang="scss" scoped>

.side-menu-wrapper {
  position: relative;
  padding-right: 11px;

  .side-menu {
    overflow: auto;
  }

  .side-menu:not(.el-menu--collapse) {
    width: 210px;
    height: calc(100vh - 161px);
  }
}

:deep(.el-menu-item){
 padding-left: 10px !important;
}
</style>
