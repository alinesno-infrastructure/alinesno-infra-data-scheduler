import FlinkNodeVue from './index.vue'
import { AppNode, AppNodeModel } from '@/views/data/scheduler/workflow/common/appNode'

// 自定义节点的 view
class FlinkNode extends AppNode {
    constructor(props) {
        super(props, FlinkNodeVue);
    }
}

export default {
  type: 'flink',
  model: AppNodeModel,
  view: FlinkNode 
}