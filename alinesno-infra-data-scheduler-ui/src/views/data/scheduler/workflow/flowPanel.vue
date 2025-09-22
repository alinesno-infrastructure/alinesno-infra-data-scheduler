<template>
  <div className="workflow-app" id="flow-container"></div>
  <Control 
    class="workflow-control" 
    @clickNode="clickNode"
    @saveFlow="saveFlow"
    v-if="lf" 
    :lf="lf">
  </Control>
</template>

<script setup>

import { ref, onMounted, onUnmounted , computed } from 'vue'

import LogicFlow from '@logicflow/core'
import '@logicflow/extension/lib/style/index.css'
import "@logicflow/core/dist/style/index.css";

import Dagre from './plugins/dagre.js'
import AppEdge from './common/edge'

import { useWorkflowStore } from '@/store/modules/workflowStore'; // 导入 Pinia Store
import Control from './common/FlowControl.vue'
const nodes = import.meta.glob('./nodes/**/index.js', { eager: true })

import { initDefaultShortcut } from './common/shortcut';

const emits = defineEmits(['saveFlow'])

// 初始化 Pinia Store
const workflowStore = useWorkflowStore();

const lf = ref();
const flowId = ref('');

const renderGraphData = (data) => {
  const container = document.querySelector('#flow-container');
  if (container) {
    console.log('container = ' + container)

    lf.value = new LogicFlow({
      plugins: [Dagre],
      textEdit: false,
      adjustEdge: false,
      adjustEdgeStartAndEnd: false,
      background: {
        backgroundColor: '#f5f6f7'
      },
      grid: {
        size: 10,
        type: 'dot',
        config: {
          color: '#DEE0E3',
          thickness: 1
        }
      },
      keyboard: {
        enabled: true
      },
      isSilentMode: false,
      container: container
    })

    // 设置主题
    lf.value.setTheme({
      bezier: {
        stroke: '#afafaf',
        strokeWidth: 1
      }
    })

    // 获取流程ID
    lf.value.on('graph:rendered', () => {
      flowId.value = lf.value.graphModel.flowId;
    });

    initDefaultShortcut(lf.value, lf.value.graphModel);
    lf.value.batchRegister([...Object.keys(nodes).map((key) => nodes[key].default), AppEdge])
    lf.value.setDefaultEdgeType('app-edge')

    console.log('lf.value:', lf.value)
    console.log('data = ' + data)

    lf.value.render(data ? data : {});

    lf.value.graphModel.eventCenter.on('delete_edge', function (id_list) {
      id_list.forEach(function (id) {
        lf.value.deleteEdge(id);
      });
    });

    // 关键：将 lf 实例存入 Pinia 状态
    workflowStore.setLF(lf.value);

    // setTimeout(() => {
      // lf.value?.fitView({
        // animation: true , 
        // padding: 50
      // })
      // lf.value.extension.dagre.layout();
    // }, 500)

  }
};

// TODO 待获取画布中心位置
const clickNode = (shapeItem) => {

  // 清除所有选中的元素
  lf.value.clearSelectElements();

  // // 获取虚拟矩形的中心位置
  // const { virtualRectCenterPositionX, virtualRectCenterPositionY } = lf.value.graphModel.getVirtualRectSize();
  // console.log('virtualRectCenterPositionX = ' + virtualRectCenterPositionX + ', virtualRectCenterPositionY = ' + virtualRectCenterPositionY);

  // // 添加一个新的节点
  // const newNode = lf.value.graphModel.addNode({
  //   type: shapeItem.type,
  //   properties: shapeItem.properties,
  //   x: lf.value.graphModel.width / 2 + virtualRectCenterPositionX/2 - 300 , 
  //   y: lf.value.graphModel.height / 2 + virtualRectCenterPositionY/2 - 300,
  // });

  // 获取画布当前可见中心（graph 坐标）
  const { centerX, centerY } = getViewportCenterInGraphCoords();

  // 如果你仍希望考虑 virtualRect 的偏移（你的场景里可能需要）
  const { virtualRectCenterPositionX = 0, virtualRectCenterPositionY = 0 } = lf.value.graphModel.getVirtualRectSize() || {};

  // 最终位置（根据需要可以去掉 virtualRect 偏移）
  const nodeX = centerX + virtualRectCenterPositionX;
  const nodeY = centerY + virtualRectCenterPositionY;

  const newNode = lf.value.graphModel.addNode({
    type: shapeItem.type,
    properties: shapeItem.properties,
    x: nodeX,
    y: nodeY,
  });

  // 设置新节点为选中和悬停状态
  newNode.isSelected = true;
  newNode.isHovered = true;

  newNode.helloworld = 'hello world';

  // 将新节点置于最上层
  lf.value.toFront(newNode.id);
};

// TODO: 获取当前可见画布（viewport）中心对应的 graph 坐标
const getViewportCenterInGraphCoords = () => {
  const graphModel = lf.value.graphModel;
  const container = lf.value.container;

  // viewport 宽高（优先 container）
  const width = (container && container.clientWidth) || graphModel.width || 0;
  const height = (container && container.clientHeight) || graphModel.height || 0;

  // transform 可能在 graphModel.transform 或者通过方法获取
  let transform = graphModel.transform ?? {};
  if ((!transform || Object.keys(transform).length === 0) && typeof graphModel.getTransform === 'function') {
    transform = graphModel.getTransform() || {};
  }

  // 常见命名有 x,y,k 或 scale
  const tx = transform.x ?? transform.tx ?? 0;
  const ty = transform.y ?? transform.ty ?? 0;
  const k = transform.k ?? transform.scale ?? 1;

  // 屏幕（container）中心相对于 container 的坐标是 (width/2, height/2)
  // graph 坐标 = (screenContainerCoord - translate) / scale
  const centerX = (width / 2 - tx) / k;
  const centerY = (height / 2 - ty) / k;

  return { centerX, centerY, tx, ty, k, width, height };
};

const getWorkflowGraphData = () => {
  return lf.value.getGraphData();
};

const setWorkflowGraphData = (data) => {
  // lf.value.render(data);

  if(!data){
    data = {
      nodes:[{
        type: 'start',
        properties: {
          icon: 'kubeflow' , 
          color: "#2962FF",
          stepName: '开始',
          showNode: true,
          height: 480,
          width: 330
        },
        x: 240,
        y: 340,
      }]
    }
  }

  // 对 data 里的 nodes 数组中的每个元素的 properties 对象添加 noRender 属性
  if (data.nodes && Array.isArray(data.nodes)) {
      data.nodes.forEach((node) => {
          if (node.properties) {
              node.properties.noRender = true;
          }
      });
  }

  renderGraphData(data);
};

// 保存流程
const saveFlow = (flowId) => { 
  emits('saveFlow', flowId)
};

onMounted(() => {
  nextTick(() => {
    // renderGraphData();
    setWorkflowGraphData(null);
  })
});

// 页面卸载时清除 Pinia 中的 lf 实例
onUnmounted(() => {
  workflowStore.clearLF();
});

defineExpose({
  onmousedown,
  clickNode,
  setWorkflowGraphData,
  getWorkflowGraphData
})

</script>

<style lang="scss">
.workflow-app {
  width: 100%;
  height: calc(100vh - 105px);
  position: relative;
  background-color: #fafafa;
  background-image: radial-gradient(#d6d9db 10%, transparent 0);
  background-size: 10px 10px;
}

.workflow-control {
  position: absolute;
  bottom: 14px;
  left: 24px;
  z-index: 2;
  display: flex;
  gap: 10px
}

.lf-drag-able {
  cursor: pointer;
}
</style>