<template>
  <span class="node-name-title">
    <span v-if="!isInput" @click.stop="clickEvent">{{ editValue }}</span>
    <el-input v-else type="text" @blur="blurEvent" v-model="editValue" :style="{ width: width }" />
  </span>
</template>

<script setup>
// import nodeSessionStore from '@/utils/nodeUtils'

import nodeSessionStore from '@/utils/nodeUtils';

import { useNodeStore } from '@/store/modules/flowNode'; // 根据实际情况调整路径
const flowNodeStore = useNodeStore();

// 定义 props
const props = defineProps({
  value: {
    type: String,
    required: false,
  },
  nodeId: {
    type: String,
    required: false,
  },
  width: {
    type: String,
    required: false,
    default: '85%',
  },
});

// 使用 ref 来创建一个可以被修改的 value 变量
const editValue = ref(props.value);

// 定义响应式变量
const isInput = ref(false);

// 方法
const clickEvent = () => {
  isInput.value = true;
};

const blurEvent = () => {
  isInput.value = false;
  console.log('editValue:' + editValue.value);
  // nodeSessionStore.updateNodeName(props.nodeId,editValue.value);

  // nodeSessionStore.updateNodeName(props.nodeId,editValue.value);
  flowNodeStore.updateNodeName(props.nodeId,editValue.value);
};
</script>