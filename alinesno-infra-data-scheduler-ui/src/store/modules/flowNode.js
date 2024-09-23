import { defineStore } from 'pinia';
import { getStartNode } from '@/utils/nodeUtil';

const flowNodeStore = defineStore(
  'flowNode',
  {
    state: () => ({
      // 节点数据数组
      nodes: [getStartNode()],
      // 缩略图
      mapImg: ''
    }),
    getters: {
      // 可以在这里定义任何需要的 getter
      currentNode: (state) => state.nodes,
      currentMapImage: (state) => state.mapImg
    },
    actions: {
      /**
       * 设置当前节点
       * @param {Object} state - 当前状态对象
       * @param {Object} node - 新的节点对象
       */
      setNode(state, node) {
        // 这里假设你想要替换整个节点数组中的某个特定节点
        // 如果你需要替换第一个节点（例如初始节点），你可以这样做：
        if (state.nodes.length > 0) {
          state.nodes[0] = node;
        }
      },
      /**
       * 添加节点
       * @param {Object} data - 包含新节点信息的对象
       */
      addNode(data) {
        console.log('data.nodeType = ', JSON.stringify(data));

        const { addNode, currNode } = data;

        // 检查 currNode 是否存在且有 id 属性
        if (currNode && currNode.id) {
          // 找到当前的 currNode
          const currentIndex = this.nodes.findIndex(node => node.id === currNode.id);

          if (currentIndex !== -1) {
            // 在当前 currNode 之后插入新节点
            this.nodes.splice(currentIndex + 1, 0, addNode);
          } else {
            // 如果找不到 currNode，则直接添加到数组末尾
            this.nodes.push(addNode);
          }
        } else {
          // 如果 currNode 不存在或为空对象，直接添加到数组末尾
          this.nodes.push(addNode);
        }

        console.info('Updated nodes: ', JSON.stringify(this.nodes));
      }
    }
  }
)

export default flowNodeStore