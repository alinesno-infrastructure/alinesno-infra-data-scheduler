<template>
  <div class="flow-row-container">
    <FlowApproverNode v-if="node" :node="node" :readable="readable" />
    <FlowNode v-if="node && node.childNode && node.childNode.hasOwnProperty('name')" :node="node.childNode" :readable="readable" />
  </div>
</template>

<script setup>

import FlowApproverNode from './Approver/index.vue';

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

</script>
