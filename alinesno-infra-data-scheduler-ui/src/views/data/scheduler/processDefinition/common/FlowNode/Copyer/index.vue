<template>
  <div class="flow-row">
    <div class="flow-box">
      <div class="flow-item" :class="{ 'flow-item-active': isActive }" @click="!readable && open('flowCopyerSetting', node)">
        <div class="flow-node-box" :class="{ 'has-error': node.error }">
          <div class="node-name" :class="nameClass(node, 'node-cc')">
            <EditName v-model="node.name" />
            <div class="search-input el-input" style="display: none;">
              <input type="text" autocomplete="off" id="1460664942574174208" class="el-input__inner" />
            </div>
            <img :src="ccIcon" alt="" style="margin-left: 10px;" />
          </div>
          <div class="node-main">
            <span v-if="node.content">
              抄送人:
              <a-tooltip placement="top">
                <template slot="title">
                  <span>{{ node.content }}</span>
                </template>
                {{ node.content }}
              </a-tooltip>
            </span>
            <!-- <span v-else class="hint-title">设置此节点</span> -->
          </div>
          <!-- 错误提示 -->
          <a-icon v-if="node.error" type="exclamation-circle" theme="filled" class="node-error" />
          <div v-if="!readable && !node.deletable" class="close-icon"><a-icon type="close-circle" @click.stop="node.deletable = true" /></div>
          <!-- 删除提示 -->
          <DeleteConfirm :node="node" />
        </div>
      </div>
      <FlowAddNode :node.sync="node" :nodeType="2" :readable="readable" />
    </div>
    <FlowCopyerSetting ref="flowCopyerSetting" @close="isActive = false" />
  </div>
</template>

<script setup>

import { flowMixin } from '../../flowMixin';
import FlowAddNode from '../Add/index.vue';

</script>
