import { defineStore } from 'pinia';
import { getStartNode } from '@/utils/nodeUtil';

export const useNodeStore = defineStore('node', {
  // 定义初始状态
  state: () => ({
    nodes: [getStartNode()],
    mapImg: ''
  }),
  // 定义 getters
  getters: {
    getNodes: (state) => state.nodes,
    getMapImg: (state) => state.mapImg,
    getAllNodes: (state) => state.nodes,
    getNodeById: (state) => (nodeId) => state.nodes.find(node => node.id === nodeId)
  },
  // 定义 actions
  actions: {
    setNode(node) {
      const index = this.nodes.findIndex(existingNode => existingNode.id === node.id);
      if (index !== -1) {
        this.nodes.splice(index, 1, node);
      } else {
        this.nodes.push(node);
      }
      console.info('Updated nodes: ', JSON.stringify(this.nodes));
    },
    addNode({ addNode, currNode }) {
      if (currNode && currNode.id) {
        const currentIndex = this.nodes.findIndex(node => node.id === currNode.id);
        if (currentIndex !== -1) {
          this.nodes.splice(currentIndex + 1, 0, addNode);
        } else {
          this.nodes.push(addNode);
        }
      } else {
        this.nodes.push(addNode);
      }
      console.info('Updated nodes: ', JSON.stringify(this.nodes));
    },
    removeNode(id) {
      const index = this.nodes.findIndex(node => node.id === id);
      if (index !== -1) {
        this.nodes.splice(index, 1);
        console.info(`Node with ID ${id} removed. Updated nodes: `, JSON.stringify(this.nodes));
      } else {
        console.warn(`Node with ID ${id} not found.`);
      }
    },
    updateNodeName(id, newName) {
      const index = this.nodes.findIndex(node => node.id === id);
      if (index !== -1) {
        const updatedNode = { ...this.nodes[index], name: newName };
        this.nodes.splice(index, 1, updatedNode);
        console.info(`Node with ID ${id} updated. New name: ${newName}. Updated nodes: `, JSON.stringify(this.nodes));
      } else {
        console.warn(`Node with ID ${id} not found.`);
      }
    },
    resetNodes() {
      this.nodes = [getStartNode()];
      console.info('Nodes reset to initial state:', JSON.stringify(this.nodes));
    },
    setMapImg(mapImg) {
      this.mapImg = mapImg;
    }
  }
});