// utils/llmIcons.js (or you can put this in your component file)
const getLlmIconPath = (providerCode) => {
  const iconMap = {
    'aip': 'aip.png',
    'customModel': 'customModel.png',
    'deepseek': 'deepseek.png',
    'doubao': 'doubao.png',
    'gitee': 'gitee.png',
    'ollama': 'ollama.png',
    'qwen': 'qwen.png',
    'qwq': 'qwq.png',
    'jdcloud': 'jdcloud.png',
    'siliconflow': 'siliconflow.png'
  };
  
  const fileName = iconMap[providerCode] || 'customModel.png';
  return new URL(`/src/assets/icons/llm/${fileName}`, import.meta.url).href;
};

const getImIconPath = (providerCode) => {
  const iconMap = {
    'EMAIL': 'email.png',
    'DINGTALK': 'dingtalk.png',
    'WECHAT_ROBOT': 'wechat.png'
  };
  
  const fileName = iconMap[providerCode] || 'email.png';
  return new URL(`/src/assets/icons/im/${fileName}`, import.meta.url).href;
};

// 获取TaskIcon图标
const getTaskIconPath = (taskCode) => {
  const fileName = taskCode + '.png' ;
  return new URL(`/src/assets/task-icons/${fileName}`, import.meta.url).href;
}

// 获取数据源图标
const getDatasourceIconPath = (datasourceCode) => {
 
  const iconMap = {
    'clickhouse': 'clickhouse.png',
    'doris': 'doris.png',
    'elasticsearch': 'elasticsearch.png',
    'ftp': 'ftp.png',
    'hive': 'hive.png',
    'kafka': 'kafka.png',
    'minio': 'minio.png',
    'mysql': 'mysql.png',
    'postgresql': 'postgresql.png',
    'qiniu': 'qiniu.png',
    's3file': 's3file.jpeg'
  };

  const fileName = iconMap[datasourceCode] || 'doris.png';
  return new URL(`/src/assets/icons/datasources/${fileName}`, import.meta.url).href;
}

const getAipStyle = (type) => {
  const iconMap = {
    'dark': 'dark.png',
    'light': 'light.png',
  };
  
  const fileName = iconMap[type] || 'light.png';
  return new URL(`/src/assets/icons/aip-style/${fileName}`, import.meta.url).href;
};

const getAgentIconPath = (type) => {
  const iconMap = {
    'rag': 'rag.png',
    'flow': 'flow.png',
    'script': 'script.png',
    'deepsearch': 'deepsearch.png',
    'react': 'react.png',
  };
  
  const fileName = iconMap[type] || 'rag.png';
  return new URL(`/src/assets/icons/agent/${fileName}`, import.meta.url).href;
};

const getOutlineSvg = (type) => {
  const iconMap = {
    'iframe': 'iframe.svg',
    'script': 'script.svg',
    'offiaccount-copylink': 'offiaccount-copylink.png',
    'aip_store_search': 'aip_store_search.jpg',
    'example_banner': 'example_banner.png',
  };
  
  const fileName = iconMap[type] || 'iframe.svg';
  return new URL(`/src/assets/icons/outline/${fileName}`, import.meta.url).href;
};

export { 
  getLlmIconPath,
  getAipStyle, 
  getAgentIconPath,
  getImIconPath,
  getTaskIconPath,
  getDatasourceIconPath,
  getOutlineSvg
};