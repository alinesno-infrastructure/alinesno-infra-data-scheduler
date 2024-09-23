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
          <span>图片</span>
        </el-menu-item>
        <el-menu-item
            index="2"
            :route="{ name: 'file', query: { fileType: 2 } }"
        >
          <el-icon :size="20">
            <Document></Document>
          </el-icon>
          <span>文档</span>
        </el-menu-item>
        <el-menu-item
            index="3"
            :route="{ name: 'file', query: { fileType: 3 } }"
        >
          <el-icon :size="20">
            <VideoCamera></VideoCamera>
          </el-icon>
          <span>视频</span>
        </el-menu-item>
        <el-menu-item
            index="4"
            :route="{ name: 'file', query: { fileType: 4 } }"
        >
          <el-icon :size="20">
            <Headset></Headset>
          </el-icon>
          <span>音乐</span>
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
import {Delete, Document, Files, Headset, Menu, Picture, Share, TakeawayBox, VideoCamera} from "@element-plus/icons-vue";
import {computed, onMounted, ref, watch} from "vue";
import {useRoute} from "vue-router";

const route = useRoute()
const isCollapse = ref(false) //  控制菜单收缩展开
const myFileMenuMap = ref({ // 菜单 index 和名称 Map
  0: '全部',
  1: '图片',
  2: '文档',
  3: '视频',
  4: '音乐',
  5: '其他',
  6: '回收站',
  8: '我的分享'
})
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
</style>
