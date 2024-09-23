<template>
  <div class="after-node-btn" @click="visible = true">
    <a-popover placement="right" v-model="visible">
      <template #content>
        <a-menu mode="vertical" class="flow-ant-menu-vertical">
          <a-menu-item key="1" @click="addType(1)">
            <img :src="approverIcon2" class="anticon" />
            <span>Shell脚本</span>
          </a-menu-item>
          <a-menu-item key="4" @click="addType(4)">
            <img :src="branchIcon2" class="anticon" />
            <span>HTTP请求</span>
          </a-menu-item>
          <a-menu-item v-if="nodeType == 1" key="7" @click="addType(7)">
            <img :src="branchIcon2" class="anticon" />
            <span>Python脚本</span>
          </a-menu-item>
          <a-menu-item key="2" @click="addType(2)">
            <img :src="ccIcon2" class="anticon" />
            <span>SQL脚本</span>
          </a-menu-item>
          <a-menu-item key="6" @click="addType(6)">
            <img :src="writeIcon2" class="anticon" />
            <span>Maven命令</span>
          </a-menu-item>
          <a-menu-item key="20" @click="addType(20)">
            <img :src="noticeIcon2" class="anticon" />
            <span>Jar包执行</span>
          </a-menu-item>
        </a-menu>
      </template>
      <!-- 当审批节点下添加意见分支,就不允许添加其他类型的节点了 -->
      <img :src="plusIcon" />
    </a-popover>
  </div>
</template>

<script setup>

import {
  plusIcon,
  approverIcon2,
  branchIcon2,
  parallelIcon,
  ccIcon2,
  writeIcon2,
  noticeIcon2,
  webhookIcon2
} from '@/utils/flowMixin';

import { v4 as uuidv4 } from 'uuid';

import flowNodeStore from '@/store/modules/flowNode'

const props = defineProps({
  node: {
    type: Object,
    default: () => ({
      addable: true,
    }),
  },
  nodeType: {
    type: Number,
    default: 1,
  },
  id: {
    type: String,
    default: '',
  },
  readable: {
    type: Boolean,
    default: false,
  },
});

const visible = ref(true)

/** 添加节点 */
function addType(type) {

  let addNode = addApproverNode(type);
  const nodeType = props.nodeType;
  const currNode = props.node;
  const id = props.id;

  flowNodeStore().addNode('flow/addNode' , {addNode, currNode, nodeType, id});
}

/**
 * 添加审批节点 ||
 */
function addApproverNode(type) {
  return {
    id: uuidv4(),
    name: type == 1 ? '审批人' : '办理人',
    type: type,
    // 流程节点状态(用于只读模式, 0:未进行 1:进行中  2:已完成)
    status: -1,
    // 流程基础配置属性
    attr: {
      // 审批类型
      approvalMethod: 1,
      // 审批方式
      approvalMode: 1,
      // 审批人与发起人为同一人时
      sameMode: 2,
      // 审批人为空时
      noHander: 4,
    },
    // 审批设置
    approverGroups: [
      {
        id: uuidv4(),
        // 审批人模式
        approverType: 1,
        // 层级模式
        levelMode: 1,
        // 审批人ID
        approverIds: [],
        // 审批人名称
        approverNames: [],
      },
    ],
    // 表单权限
    privileges: [],
    // 高级配置
    configure: {},
    // 子节点
    childNode: null,
    // 显示添加按钮
    addable: true,
    // 可删除提示
    deletable: false,
    // 是否有错误
    error: false,
    // 显示内容
    content: null,
  };
}
</script>
