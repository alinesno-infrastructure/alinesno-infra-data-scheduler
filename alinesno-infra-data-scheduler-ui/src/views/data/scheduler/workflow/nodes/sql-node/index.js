import SqlNodeVue from './index.vue'
import { AppNode, AppNodeModel } from '@/views/data/scheduler/workflow/common/appNode'

// 自定义节点的 view
class SqlNode extends AppNode {
    constructor(props) {
        super(props, SqlNodeVue);
    }
}

export default {
  type: 'sql',
  model: AppNodeModel,
  view: SqlNode 
}