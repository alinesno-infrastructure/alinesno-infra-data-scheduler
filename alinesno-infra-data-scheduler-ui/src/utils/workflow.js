// 原始类型映射（保留原有 key -> value）
const WorkflowType = {
  Base: 'base',
  Start: 'start',
  End: 'end',
  AiChat: 'ai_chat',
  SearchDataset: 'knowledge_search',
  Notice: 'notice',
  Condition: 'condition',
  Shell: 'shell',
  SqlNode: 'sql',
  HttpApi: 'http_api',
  FunctionLib: 'function_lib',
  FunctionLibCustom: 'function',
  FlinkNode: 'flink',
  Application: 'application',
  DocumentExtractNode: 'document_extract',
  ImageUnderstandNode: 'image_understand',
  FormNode: 'form',
  PythonNode: 'python',
  SparkNode: 'spark',
  ShellNode: 'shell' // 与 Shell 相同的值，兼容老数据
};

// 为便于前端展示，提供 list 形式：{ key, value, label, desc }
const WorkflowTypeList = [
  { key: 'Base', value: WorkflowType.Base, label: '基础节点', desc: '通用基础节点' },
  { key: 'Start', value: WorkflowType.Start, label: '开始节点', desc: '工作流起点' },
  { key: 'End', value: WorkflowType.End, label: '结束节点', desc: '工作流终点' },
  { key: 'AiChat', value: WorkflowType.AiChat, label: 'AI 聊天', desc: '调用聊天模型' },
  { key: 'SearchDataset', value: WorkflowType.SearchDataset, label: '知识检索', desc: '检索知识库' },
  { key: 'Notice', value: WorkflowType.Notice, label: '通知', desc: '发送通知/消息' },
  { key: 'Condition', value: WorkflowType.Condition, label: '条件', desc: '条件分支节点' },
  { key: 'Shell', value: WorkflowType.Shell, label: 'Shell', desc: '执行 Shell 命令' },
  { key: 'SqlNode', value: WorkflowType.SqlNode, label: 'SQL', desc: '执行 SQL' },
  { key: 'HttpApi', value: WorkflowType.HttpApi, label: 'HTTP API', desc: '调用 HTTP 接口' },
  { key: 'FunctionLib', value: WorkflowType.FunctionLib, label: '函数库', desc: '内置函数库调用' },
  { key: 'FunctionLibCustom', value: WorkflowType.FunctionLibCustom, label: '自定义函数', desc: '用户自定义函数' },
  { key: 'FlinkNode', value: WorkflowType.FlinkNode, label: 'Flink', desc: 'Flink 作业节点' },
  { key: 'Application', value: WorkflowType.Application, label: '应用', desc: '外部应用调用' },
  { key: 'DocumentExtractNode', value: WorkflowType.DocumentExtractNode, label: '文档抽取', desc: '从文档中抽取信息' },
  { key: 'ImageUnderstandNode', value: WorkflowType.ImageUnderstandNode, label: '图像理解', desc: '图像分析/识别' },
  { key: 'FormNode', value: WorkflowType.FormNode, label: '表单', desc: '表单交互' },
  { key: 'PythonNode', value: WorkflowType.PythonNode, label: 'Python', desc: '执行 Python 脚本' },
  { key: 'SparkNode', value: WorkflowType.SparkNode, label: 'Spark', desc: 'Spark 作业节点' },
  { key: 'ShellNode', value: WorkflowType.ShellNode, label: 'Shell (alias)', desc: 'Shell 别名，兼容旧配置' }
];

// 异常策略枚举（与你给出的映射一致）
const ExceptionStrategy = {
  IGNORE_FLOW: 0,   // 继续执行（忽略当前错误）
  STOP_FLOW: 1,     // 停止执行（出错则终止整个流程）
  PAUSE_TASK: 2,    // 暂停任务（保留状态，人工干预/恢复）
  Unknown: -1
};

// 同样提供 list 形式，便于返回给前端或 UI 展示
const ExceptionStrategyList = [
  { code: 0, key: 'IGNORE_FLOW', label: '继续执行', desc: '出现异常时忽略错误并继续后续节点的执行' },
  { code: 1, key: 'STOP_FLOW', label: '停止执行', desc: '出现异常时标记失败并停止整个流程' },
//  { code: 2, key: 'PAUSE_TASK', label: '暂停任务', desc: '出现异常时将流程置为暂停，等待人工干预或恢复' },
];

// 常用工具函数
function getWorkflowTypeByValue(value) {
  return WorkflowTypeList.find(item => item.value === value) || null;
}

function getWorkflowTypeByKey(key) {
  return WorkflowTypeList.find(item => item.key === key) || null;
}

function getExceptionStrategyByCode(code) {
  return ExceptionStrategyList.find(item => item.code === code) || { code, key: 'Unknown', label: '未知', desc: '' };
}

// 将常见的“返回数据项”格式化为 API 可直接返回的结构
function buildExceptionStrategyResponse() {
  return {
    list: ExceptionStrategyList,
    map: ExceptionStrategy // 兼容旧代码引用
  };
}

function buildWorkflowTypeResponse() {
  return {
    list: WorkflowTypeList,
    map: WorkflowType
  };
}

export {
  WorkflowType,
  WorkflowTypeList,
  ExceptionStrategy,
  ExceptionStrategyList,
  getWorkflowTypeByValue,
  getWorkflowTypeByKey,
  getExceptionStrategyByCode,
  buildExceptionStrategyResponse,
  buildWorkflowTypeResponse
};