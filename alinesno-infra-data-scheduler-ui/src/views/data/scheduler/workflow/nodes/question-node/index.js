import QuestionNodeVue from './index.vue'
import { AppNode, AppNodeModel } from '@/views/data/scheduler/workflow/common/appNode'

// 自定义节点的 view
class QuestionNode extends AppNode {
    constructor(props) {
        super(props, QuestionNodeVue);
    }
}

export default {
  type: 'question',
  model: AppNodeModel,
  view: QuestionNode 
}