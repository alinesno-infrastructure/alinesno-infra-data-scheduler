<template>
  <div class="app-container">
      <el-page-header @back="goBack" content="配置任务编排"></el-page-header>

      <div class="form-container">
          <div class="designer-wrap">
            <!-- <FlowNav v-if="navable && !readable" :currentNav="3" @click="publish" @change="change" /> -->

            <div class="designer-content-box" :style="{ height: readable ? '100vh' : 'calc(100vh - 50px)' }">
              <div class="flow-design-wrap">
                <div id="flow-design" class="flow-design-container" :style="zoomStyle">
                  <div id="flow-design-content" class="flow-design-content">
                    <FlowStartNode :node="nodeData" />

                    <FlowNode :node="item" :readable="readable" v-for="(item , index) in nodeDataArr" :key="index" />

                    <FlowEndNode :node="nodeData" :readable="readable" />
                  </div>
                </div>
                <FlowHelper />
                <FlowTips />
                <FlowZoom />
              </div>
            </div>


          </div>
      </div>

  </div>
</template>

<script setup name="createProcessDefinition">

const router = useRouter();

import flowNodeStore from '@/store/modules/flowNode'

import { getStartNode } from '@/utils/nodeUtil';

import FlowZoom from './common/FlowZoom';
import FlowHelper from './common/FlowHelper';
import FlowTips from './common/FlowTips';
import FlowNode from './common/FlowNode/index';
import FlowStartNode from './common/FlowNode/Start';
import FlowEndNode from './common/FlowNode/End';

// 定义props
const props = defineProps({
  node: {
    type: Object,
    default: getStartNode,
  },
  navable: {
    type: Boolean,
    default: true,
  },
  readable: {
    type: Boolean,
    default: false,
  },
});

const nodeDataArr = ref(flowNodeStore().nodes);

/** 返回 */
function goBack() {
   router.push({path:'/data/scheduler/processDefinition/addDefinition',query:{}});
}

</script>
