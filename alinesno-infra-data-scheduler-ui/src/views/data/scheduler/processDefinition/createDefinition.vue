<template>
  <div class="app-container">
    <el-page-header @back="goBack" content="配置任务编排"></el-page-header>

    <div class="form-container">
      <div class="designer-wrap">

        <div style="position: absolute;top: 20px;z-index: 1000;right: 20px;">
          <el-button type="primary" size="large" bg text @click="submitProcessDefinition()">
            <i class="fa-solid fa-paper-plane"></i>&nbsp;提交流程
          </el-button>
        </div>

        <div class="designer-content-box" :style="{ height: readable ? '100vh' : 'calc(100vh - 50px)' }">
          <div class="flow-design-wrap">
            <div id="flow-design" class="flow-design-container" :style="zoomStyle">
              <div id="flow-design-content" class="flow-design-content">
                <FlowStartNode :node="nodeData" />

                <FlowNode :node="item" :readable="readable" v-for="(item, index) in nodeDataArr" :key="index" />

                <FlowEndNode :node="nodeData" :readable="readable" />
              </div>
            </div>
            <FlowHelper />
            <FlowTips />
            <!-- <FlowZoom /> -->
          </div>
        </div>


      </div>
    </div>

  </div>
</template>

<script setup name="createProcessDefinition">

const router = useRouter();
const route = useRoute();
const { proxy } = getCurrentInstance();

import { ElLoading } from 'element-plus'

// import nodeSessionStore from '@/utils/nodeUtils'
// import nodeSessionStore from '@/utils/nodeUtils';

import { useNodeStore } from '@/store/modules/flowNode'; // 根据实际情况调整路径
const flowNodeStore = useNodeStore();

import { getTaskDefinition } from '@/api/data/scheduler/taskDefinition'
import { commitProcessDefinition , updateProcessDefinition } from '@/api/data/scheduler/processDefinition'

import FlowHelper from './common/FlowHelper';
import FlowTips from './common/FlowTips';
import FlowNode from './common/FlowNode/index';
import FlowStartNode from './common/FlowNode/Start';
import FlowEndNode from './common/FlowNode/End';

import emitter from '@/utils/emitter' 
import { nextTick } from 'vue';

const processDefinitionId = route.query.processDefinitionId
const updateNode = ref(route.query.node)
const nodeDataArr = ref(flowNodeStore.getNodes);


/** 返回 */
function goBack() {

  let queryObj = {} ; 
  
  if(processDefinitionId){
   queryObj = {'processDefinitionId': processDefinitionId } ;
  }

  router.push({ path: '/data/scheduler/processDefinition/addDefinition', query: queryObj });
}

/**
 * 提交流程流程定义
 */
function submitProcessDefinition() {
  // let nodes = nodeSessionStore.getAllNodes();

  let nodes = nodeDataArr.value // nodeSessionStore.getAllNodes();
  console.log('submitProcessDefinition:' + JSON.stringify(nodes))

  const loading = ElLoading.service({
    lock: true,
    text: 'Loading',
    background: 'rgba(0, 0, 0, 0.7)',
  })

  const formDataStr = localStorage.getItem('processDefinitionFormData');
  const formData = JSON.parse(formDataStr);

  let data = {
    taskFlow: nodes,
    context: formData,
    type: updateNode.value ,
    processId: processDefinitionId 
  }

  if(processDefinitionId){ // 更新流程

    updateProcessDefinition(data).then(response => {
      console.log(response);
      proxy.$modal.msgSuccess("流程更新成功");
      loading.close();
    }).catch(error => {
      loading.close();
    })
  }else{ // 新增流程
    commitProcessDefinition(data).then(response => {
      console.log(response);
      proxy.$modal.msgSuccess("流程提交成功");
      loading.close();

      // 返回管理界面
      router.push({ path: '/data/scheduler/processDefinition/index', query: {} });

    }).catch(error => {
      loading.close();
    })
  }

}

flowNodeStore.resetNodes()
if(processDefinitionId){

  getTaskDefinition(processDefinitionId).then(res => {
    // 使用 forEach 循环遍历 data 数组
    res.data.forEach(item => {
      if(item.type != 0){
        // nodeSessionStore.setNode(item);
        flowNodeStore.setNode(item);
      }
    });

    nodeDataArr.value = flowNodeStore.getNodes
    
  })
}else{
    nodeDataArr.value = flowNodeStore.getNodes
}

/** 初始化数据 */
// onMounted(() => {
//   console.log('onMounted');

  // flowNodeStore.resetNodes()

  // if(processDefinitionId){

  //   getTaskDefinition(processDefinitionId).then(res => {
  //     // 使用 forEach 循环遍历 data 数组
  //     res.data.forEach(item => {
  //       if(item.type != 0){
  //         // nodeSessionStore.setNode(item);
  //         flowNodeStore.setNode(item);
  //       }
  //     });

  //     nodeDataArr.value = flowNodeStore.getNodes
      
  //   })
  // }else{
  //     nodeDataArr.value = flowNodeStore.getNodes
  // }
// })

// nextTick(() => {

//   if(processDefinitionId){

//     getTaskDefinition(processDefinitionId).then(res => {
//       // 使用 forEach 循环遍历 data 数组
//       res.data.forEach(item => {
//         if(item.type != 0){
//           // nodeSessionStore.setNode(item);
//           flowNodeStore.setNode(item);
//         }
//       });

//       nodeDataArr.value = flowNodeStore.getNodes
      
//     })
//   }else{
//       nodeDataArr.value = flowNodeStore.getNodes
//   }
// })


</script>
