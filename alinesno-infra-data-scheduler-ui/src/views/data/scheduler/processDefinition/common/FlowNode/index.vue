<template>
  <div class="flow-row-container">

    <!-- 节点 -->
    <FlowShellNode v-if="node && node.type == 1" :node="node" :readable="readable" />
    <FlowHttpNode v-if="node && node.type == 2" :node="node" :readable="readable" />
    <FlowPythonNode v-if="node && node.type == 3" :node="node" :readable="readable" />
    <FlowSqlNode v-if="node && node.type == 4" :node="node" :readable="readable" />
    <FlowMavenNode v-if="node && node.type == 5" :node="node" :readable="readable" />
    <FlowAnsibleNode v-if="node && node.type == 6" :node="node" :readable="readable" />
    <FlowGroovyNode v-if="node && node.type == 7" :node="node" :readable="readable" />
    <FlowCheckout v-if="node && node.type == 8" :node="node" :readable="readable" />
    <FlowNotice v-if="node && node.type == 9" :node="node" :readable="readable" />
    
    <!-- 子节点-->
    <FlowNode v-if="node && node.childNode && node.childNode.hasOwnProperty('name')" :node="node.childNode" :readable="readable" />

    <!-- 修改子节点渲染方式 -->
    <!-- <template v-if="node.childNode && !dragging">
      <FlowNode :node="node.childNode" :readable="readable" />
    </template> -->
    
  </div>
</template>

<script setup>

const dragging = ref(false);

import FlowShellNode from './Shell/index'  // 引入SHELL组件(1)
import FlowHttpNode from './Http/index'  // 引入HTTP组件(2)
import FlowPythonNode from './Python/index'   // 引入PYTHON组件(3)
import FlowSqlNode from './Sql/index'  // 引入SQL组件(4)
import FlowMavenNode from './Maven/index' // 引入MAVEN组件(5)
import FlowAnsibleNode from './Ansible/index' // 引入JAR组件(6)
import FlowGroovyNode from './Groovy/index' // 引入Groovy组件(7)
import FlowCheckout from './Checkout/index' // 引入代码下载组件(8)
import FlowNotice from './Notice/index'

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
