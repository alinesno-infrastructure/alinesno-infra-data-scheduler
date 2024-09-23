<template>
  <div class="flow-row">
    <div class="flow-box">
      <div class="flow-item" :class="{ 'flow-item-active': isActive }" @click="!readable && open('flowApproverSetting', node)">
        <div class="flow-node-box" :class="{ 'has-error': node.error }">
          <div class="node-name" :class="nameClass(node, node.type == 1 ? 'node-sp' : 'node-transact')">
            <EditName v-model="node.name" />
            <img :src="approverIcon" style="margin-left: 10px;" />
          </div>

          <div class="node-main">

            <span v-if="node.content">
              {{ node.type == 1 ? '审批人' : '办理人' }}:
              <a-tooltip placement="top">
                <template #title>
                  <span>{{ node.content }}</span>
                </template>
                {{ node.content }}
              </a-tooltip>
            </span>
            <span v-else class="hint-title">设置此节点</span>
          </div>

          <!-- 错误提示 -->
          <a-icon v-if="node.error" type="exclamation-circle" theme="filled" class="node-error" />
          <div v-if="!readable && !node.deletable" class="close-icon">
            <a-icon type="close-circle" @click.stop="node.deletable = true" />
          </div>

          <!-- 删除提示 -->
          <DeleteConfirm :node="node" />
        </div>
      </div>
      <!-- 如果子节点是意见分支,则只能添加一个意见分支 -->
      <FlowAddNode v-model:node="node" :nodeType="node.type" :readable="readable" />
    </div>
    <FlowApproverSetting ref="flowApproverSetting" @close="isActive = false" />
  </div>
</template>

<script setup>
import { flowMixin } from '../../flowMixin';
import EditName from '../../EditName.vue';
import FlowAddNode from '../Add/index.vue';

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

const node = ref(props.node)
const readable = ref(props.readable)

const deleteable =ref(false)

function nameClass() {
  return (node, defaultStyle) => {
    if (node.status == -1) {
      return defaultStyle;
    }
    return { 'node-status-not': node.status == 0, 'node-status-current': node.status == 1, 'node-status-complete': node.status == 2 };
  };
}

</script>
