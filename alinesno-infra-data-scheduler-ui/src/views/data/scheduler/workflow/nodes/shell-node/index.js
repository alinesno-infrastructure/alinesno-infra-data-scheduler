import ShellNodeVue from './index.vue'
import { AppNode, AppNodeModel } from '@/views/data/scheduler/workflow/common/appNode'

// 自定义节点的 view
class ShellNode extends AppNode {
    constructor(props) {
        super(props, ShellNodeVue);
    }
}


export default {
  type: 'shell',
  model: AppNodeModel,
  view: ShellNode
}