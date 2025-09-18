import FunctionNodeVue from './index.vue'
import { AppNode, AppNodeModel } from '@/views/data/scheduler/workflow/common/appNode'

// 自定义节点的 view
class FunctionNode extends AppNode {
    constructor(props) {
        super(props, FunctionNodeVue);
    }
}


export default {
  type: 'function',
  model: AppNodeModel,
  view: FunctionNode 
}