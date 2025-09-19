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
    ShellNode: 'shell'
};

export { WorkflowType };