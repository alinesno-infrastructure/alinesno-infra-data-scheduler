import NoticeNodeVue from './index.vue'
import { AppNode, AppNodeModel } from '@/views/data/scheduler/workflow/common/appNode'

// 自定义节点的 view
class NoticeNode extends AppNode {
    constructor(props) {
        super(props, NoticeNodeVue);
    }
}

export default {
  type: 'notice',
  model: AppNodeModel,
  view: NoticeNode 
}