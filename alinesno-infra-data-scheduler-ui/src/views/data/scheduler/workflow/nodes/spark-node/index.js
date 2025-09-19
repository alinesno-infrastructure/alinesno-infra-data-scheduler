import SparkNodeVue from './index.vue'
import { AppNode, AppNodeModel } from '@/views/data/scheduler/workflow/common/appNode'

// 自定义节点的 view
class SparkView extends AppNode {
    constructor(props) {
        super(props, SparkNodeVue);
    }
}


export default {
  type: 'spark',
  model: AppNodeModel,
  view: SparkView
}