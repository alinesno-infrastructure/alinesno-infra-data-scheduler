/** 不再作用，使用pinia */
import { getStartNode } from '@/utils/nodeUtil';

const SESSION_KEY = 'sessionData';

// 初始化状态
function loadFromSessionStorage() {
  const storedData = sessionStorage.getItem(SESSION_KEY);
  if (storedData) {
    const data = JSON.parse(storedData);
    return {
      nodes: data.nodes || [getStartNode()],
      mapImg: data.mapImg || ''
    };
  } else {
    return {
      nodes: [getStartNode()],
      mapImg: ''
    };
  }
}

// 保存状态到 sessionStorage
function saveToSessionStorage(state) {
  const data = {
    nodes: state.nodes,
    mapImg: state.mapImg
  };
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(data));
}

// 初始化状态
const initialState = loadFromSessionStorage();

// 工具类方法
const nodeSessionStore = {
  // Getter for nodes
  getNodes: () => initialState.nodes,

  // Getter for mapImg
  getMapImg: () => initialState.mapImg,

  // Action to get node by ID
  getNodeById: (nodeId) => initialState.nodes.find(node => node.id === nodeId),

  // Action to set or replace a node
  setNode: (node) => {
    const index = initialState.nodes.findIndex(existingNode => existingNode.id === node.id);
    if (index !== -1) {
      initialState.nodes.splice(index, 1, node);
    } else {
      initialState.nodes.push(node);
    }
    saveToSessionStorage(initialState);
    console.info('Updated nodes: ', JSON.stringify(initialState.nodes));
  },

  // Action to add a node
  addNode: (data) => {
    const { addNode, currNode } = data;
    if (currNode && currNode.id) {
      const currentIndex = initialState.nodes.findIndex(node => node.id === currNode.id);
      if (currentIndex !== -1) {
        initialState.nodes.splice(currentIndex + 1, 0, addNode);
      } else {
        initialState.nodes.push(addNode);
      }
    } else {
      initialState.nodes.push(addNode);
    }
    saveToSessionStorage(initialState);
    console.info('Updated nodes: ', JSON.stringify(initialState.nodes));
  },

  // Action to remove a node
  removeNode: (id) => {
    const index = initialState.nodes.findIndex(node => node.id === id);
    if (index !== -1) {
      initialState.nodes.splice(index, 1);
      saveToSessionStorage(initialState);
      console.info(`Node with ID ${id} removed. Updated nodes: `, JSON.stringify(initialState.nodes));
    } else {
      console.warn(`Node with ID ${id} not found.`);
    }
  },

  // Action to update a node's name
  updateNodeName: (id, newName) => {
    const index = initialState.nodes.findIndex(node => node.id === id);
    if (index !== -1) {
      const updatedNode = { ...initialState.nodes[index], name: newName };
      initialState.nodes.splice(index, 1, updatedNode);
      saveToSessionStorage(initialState);
      console.info(`Node with ID ${id} updated. New name: ${newName}. Updated nodes: `, JSON.stringify(initialState.nodes));
    } else {
      console.warn(`Node with ID ${id} not found.`);
    }
  },

  // Action to get all nodes
  getAllNodes: () => initialState.nodes,

  // Action to reset nodes to initial state
  resetNodes: () => {
    initialState.nodes = [getStartNode()];
    saveToSessionStorage(initialState);
    console.info('Nodes reset to initial state:', JSON.stringify(initialState.nodes));
  },

  // Action to set the map image
  setMapImg: (mapImg) => {
    initialState.mapImg = mapImg;
    saveToSessionStorage(initialState);
  }
};

export default nodeSessionStore;

// import nodeSessionStore from '@/utils/sessionStore';

// // 获取所有节点
// const nodes = nodeSessionStore.getNodes();

// // 添加一个节点
// nodeSessionStore.addNode({ addNode: { id: 'newId', name: 'New Node' }, currNode: { id: 'existingId' } });

// // 删除一个节点
// nodeSessionStore.removeNode('someId');

// // 更新节点名称
// nodeSessionStore.updateNodeName('someId', 'Updated Name');

// // 重置节点
// nodeSessionStore.resetNodes();