
import { getStartNode, addNode, addBranch, delNode, delBranchNode, addCondition, updateNode } from '@/utils/nodeUtil';

const flowNodeStore = defineStore(
  'flowNode',
  {
    state: () => ({
      //  节点数据
      node: getStartNode(),
      //  缩略图
      mapImg: '',
      // 意见分支
      suggestBranchEnable: true,
      // 并行节点
      parallelBranchEnable: true,
    }),
    actions: {
      /**
       *  初始节点
       */
      setNode(state, node) {
        console.log('state = ' + state + ' , node = ' + node)
        if (node) {
          this.node = node;
        } else {
          this.node = getStartNode();
        }
      },
      /**
       *  获取getters
       */
      getters() {
        return {
          node: this.node,
          mapImg: this.mapImg,
          suggestBranchEnable: this.suggestBranchEnable,
          parallelBranchEnable: this.parallelBranchEnable,
        }; 
      },
      /**
       * 添加节点
       */
      addNode(state, data) {
        console.log('state = ' + state + ' , data = ' + data.nodeType);
        if (data.nodeType == 0) {
          //  开始
          if (this.node.hasOwnProperty('name')) {
            data.addNode.childNode = this.node;
            data.addNode.childNode.pid = data.addNode.id;
            data.addNode.pid = 0;
          }
          this.node = data.addNode;
        } else {
          if (data.id) {
            data.currNode.conditionNodes.forEach((conditionNode, i) => {
              if (conditionNode.id == data.id) {
                // 获取当前操作节点
                addNode(this.node, conditionNode, data.addNode);
              }
            });
          } else {
            // 获取当前操作节点
            addNode(this.node, data.currNode, data.addNode);
          }
        }
        // 更新地图
        // updateMap(state);
        //console.log('node', state.node);
        console.info('nodeJson ' + JSON.stringify(this.node));
      }
    }
  })

export default flowNodeStore

