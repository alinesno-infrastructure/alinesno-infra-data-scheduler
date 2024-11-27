<template>
  <div class="after-node-btn" @click="visible = true">
    <a-popover placement="right" v-model="visible">
      <template #content>
        <a-menu mode="vertical" class="flow-ant-menu-vertical">
          <a-menu-item key="8" @click="addType(8)">
            <img :src="parallelIcon" class="anticon" />
            <span>Checkout操作</span>
          </a-menu-item>
          <a-menu-item key="1" @click="addType(1)">
            <img :src="approverIcon2" class="anticon" />
            <span>SHELL脚本</span>
          </a-menu-item>
          <a-menu-item key="2" @click="addType(2)">
            <img :src="webhookIcon2" class="anticon" />
            <span>HTTP请求</span>
          </a-menu-item>
          <a-menu-item key="3" @click="addType(3)">
            <img :src="branchIcon2" class="anticon" />
            <span>PYTHON脚本</span>
          </a-menu-item>
          <a-menu-item key="4" @click="addType(4)">
            <img :src="ccIcon2" class="anticon" />
            <span>SQL脚本</span>
          </a-menu-item>
          <a-menu-item key="5" @click="addType(5)">
            <img :src="writeIcon2" class="anticon" />
            <span>MAVEN命令</span>
          </a-menu-item>
          <a-menu-item key="6" @click="addType(6)">
            <img :src="noticeIcon2" class="anticon" />
            <span>ANSIBLE包执行</span>
          </a-menu-item>
          <a-menu-item key="7" @click="addType(7)">
            <img :src="parallelIcon" class="anticon" />
            <span>Groovy操作</span>
          </a-menu-item>
          <a-menu-item key="9" @click="addType(9)">
            <img :src="branchIcon2" class="anticon" />
            <span>通知回调</span>
          </a-menu-item>
        </a-menu>
      </template>
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
// import nodeSessionStore from '@/utils/nodeUtils'

import { useNodeStore } from '@/store/modules/flowNode'; // 根据实际情况调整路径
const flowNodeStore = useNodeStore();

import emitter from '@/utils/emitter' 

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

  console.log('type = ' + type);

  let addNode = addApproverNode(type);
  const nodeType = props.nodeType;
  const currNode = props.node;
  const id = props.id;

  console.log('currNode= ' + JSON.stringify(currNode));

  // nodeSessionStore.addNode({addNode, currNode, nodeType, id});
  flowNodeStore.addNode({addNode, currNode});

}

/**
 * 获取节点类型信息 
 * @param {*} type 
 */
function getTypeInfo(type) {
    let name = '';
    let icon = '';

    switch (type) {
      case 1:
        name = 'SHELL脚本';
        icon = approverIcon2;
        break;
      case 2:
        name = 'HTTP请求';
        icon = webhookIcon2;
        break;
      case 3:
        name = 'PYTHON脚本';
        icon = branchIcon2;
        break;
      case 4:
        name = 'SQL脚本';
        icon = ccIcon2;
        break;
      case 5:
        name = 'MAVEN命令';
        icon = writeIcon2;
        break;
      case 6:
        name = 'ANSIBLE执行';
        icon = noticeIcon2;
        break;
      case 7:
        name = 'Groovy操作';
        icon = parallelIcon;
        break;
      case 8:
        name = 'Checkout操作';
        icon = parallelIcon;
        break;
      case 9:
        name = '通知回调';
        icon = branchIcon2;
        break;
      default:
        console.error(`未知类型: ${type}`);
        return null;
    }

    return { name:name, icon:icon };
}

/**
 * 添加审批节点 ||
 */
function addApproverNode(type) {
  return {
    id: uuidv4(),
    name: getTypeInfo(type).name , // type == 1 ? '审批人' : '办理人',
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
  };
}
</script>
