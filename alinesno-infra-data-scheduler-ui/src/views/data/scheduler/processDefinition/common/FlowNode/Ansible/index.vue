<template>
  <div class="flow-row">
    <div class="flow-box">
      <div class="flow-item" :class="{ 'flow-item-active': isActive }">
        <div class="flow-node-box" :class="{ 'has-error': node.error }">
          <div class="node-name node-zl">
            <EditName :value="node.name" :nodeId="node.id" />
            <img :src="approverIcon" style="margin-left: 10px;" />
          </div>

          <div class="node-main" @click="open(node)">
            <span class="hint-title">点击设置节点</span>
          </div>

          <div class="close-icon" @click="removeNode(node)">
            <!-- <a-icon type="closeCircle"  /> -->
            <el-icon><Close /></el-icon>
          </div>

        </div>
      </div>
      <FlowAddNode v-model:node="node" :nodeType="node.type" :readable="readable" />
    </div>
    <FlowAnsibleSetting ref="flowAnsibleSetting" @close="isActive = false" />
  </div>
</template>

<script setup>

import { approverIcon } from '@/utils/flowMixin';
// import nodeSessionStore from '@/utils/nodeUtils'

import { useNodeStore } from '@/store/modules/flowNode'; // 根据实际情况调整路径
const flowNodeStore = useNodeStore();

import EditName from '../../EditName.vue';
import FlowAddNode from '../Add/index.vue';
import FlowAnsibleSetting from '../../FlowDrawer/Ansible/index.vue';

const props = defineProps({
  node: {
    type: Object,
    default: () => ({}),  // 对象类型的默认值应通过函数返回
  },
  readable: {
    type: Boolean,
    default: false,  // 基本类型可以直接指定默认值
  },
});

const flowAnsibleSetting = ref(null);

const node = ref(props.node)
const readable = ref(props.readable)
const isActive = ref(false)

/**
 * 打开侧边配置
 * @param {*} name
 * @param {*} node
 * @param {*} routeNode
 */
function open(selectNode) {
  isActive.value = true; // 高亮

  //  打开配置
  selectNode = flowNodeStore.getNodeById(selectNode.id) ; 
  flowAnsibleSetting.value.showDrawer(selectNode);
}

/**
 * 删除节点
 * @param {*} node
 */
function removeNode(node) {
  flowNodeStore.removeNode(node.id);
}

</script>