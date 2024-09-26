<template>
  <div class="flow-row-container">

    <!-- 节点 -->
    <FlowShellNode v-if="node && node.type == 1" :node="node" :readable="readable" />
    <FlowHttpNode v-if="node && node.type == 2" :node="node" :readable="readable" />
    <FlowPythonNode v-if="node && node.type == 3" :node="node" :readable="readable" />
    <FlowSqlNode v-if="node && node.type == 4" :node="node" :readable="readable" />
    <FlowMavenNode v-if="node && node.type == 5" :node="node" :readable="readable" />
    <FlowJarNode v-if="node && node.type == 6" :node="node" :readable="readable" />
    <FlowK8SNode v-if="node && node.type == 7" :node="node" :readable="readable" />
    <FlowCheckout v-if="node && node.type == 8" :node="node" :readable="readable" />
    
    <!-- 子节点-->
    <FlowNode v-if="node && node.childNode && node.childNode.hasOwnProperty('name')" :node="node.childNode" :readable="readable" />
    
  </div>
</template>

<script setup>

import FlowShellNode from './Shell/index'  // 引入SHELL组件(1)
import FlowHttpNode from './Http/index'  // 引入HTTP组件(2)
import FlowPythonNode from './Python/index'   // 引入PYTHON组件(3)
import FlowSqlNode from './Sql/index'  // 引入SQL组件(4)
import FlowMavenNode from './Maven/index' // 引入MAVEN组件(5)
import FlowJarNode from './Jar/index' // 引入JAR组件(6)
import FlowK8SNode from './K8S/index' // 引入K8S组件(7)
import FlowCheckout from './Checkout/index' // 引入代码下载组件(8)

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
