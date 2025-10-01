<template>
  <div class="app-container">
    <el-tabs :tab-position="tabPosition" class="demo-tabs">
      <el-tab-pane label="系统运行配置">
        <NodeManage />
      </el-tab-pane> 
      <el-tab-pane label="环境管理">
        <EnvironmentManage />
      </el-tab-pane>
      <!-- 消息通知-->
       <el-tab-pane label="消息通知">
        <NotificationManage />
      </el-tab-pane>
      <el-tab-pane label="账号凭据管理">
        <CredentialManage />
      </el-tab-pane>
      <el-tab-pane label="密钥管理">
        <SecretsManager/>
      </el-tab-pane>
    </el-tabs>
 </div>
</template>

<script setup>
const tabPosition = ref('right')

// import RunningMonitor from './components/runningMonitor.vue'
import EnvironmentManage from './components/environmentManage.vue'
import CredentialManage from './components/credentialManage.vue'
import SecretsManager from './components/secretsManager.vue'
import CommandManage from './components/commandManage.vue'
import NodeManage from './components/nodeManage.vue'
import NotificationManage from './components/notificationManage.vue'

</script>
