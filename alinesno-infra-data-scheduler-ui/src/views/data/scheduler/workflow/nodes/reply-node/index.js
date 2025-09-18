import ReplyNodeVue from './index.vue'
import { AppNode, AppNodeModel } from '@/views/data/scheduler/workflow/common/appNode'

// 自定义节点的 view
class ReplyNode extends AppNode {
    constructor(props) {
        super(props, ReplyNodeVue);
    }
}

export default {
  type: 'reply',
  model: AppNodeModel,
  view: ReplyNode 
}