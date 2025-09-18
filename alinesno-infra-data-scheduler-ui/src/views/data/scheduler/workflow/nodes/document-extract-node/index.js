import DocumentExtractNodeVue from './index.vue'
import { AppNode, AppNodeModel } from '@/views/data/scheduler/workflow/common/appNode'

// 自定义节点的 view
class DocumentExtractNode extends AppNode {
    constructor(props) {
        super(props, DocumentExtractNodeVue);
    }
}


export default {
  type: 'document_extract',
  model: AppNodeModel,
  view: DocumentExtractNode 
}