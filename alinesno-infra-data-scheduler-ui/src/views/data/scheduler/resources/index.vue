<template>
    <div class="app-container" style="padding:10px;margin-top:0px">
      <el-container class="el-container">
          <AsideMenu></AsideMenu>
          <el-main class="el-main">
            <!-- <router-view></router-view> -->
             <FileList />
          </el-main>
      </el-container>
    </div>
  </template>
  
  <script setup>
  import AsideMenu from './AsideMenu'
  import FileList from './FileList'
  
  </script>
  
  <style scoped>
  
  .file-wrapper {
    width: 100% !important;
  }
  
  
  .el-main {
    padding: 0px 16px !important;
    overflow: hidden;
  }
  </style>
  