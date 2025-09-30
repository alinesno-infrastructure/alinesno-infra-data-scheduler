<template>
    <div class="node-components border-r-4" v-show="show">

        <div class="node-components-title">
            <span>
                <i class="fa-solid fa-masks-theater"></i> 任务节点
            </span>
            <!-- 
            <span style="cursor: pointer;">
                <i class="fa-solid fa-xmark"></i>
            </span> 
            -->
        </div>
        <el-scrollbar height="600px" style="padding:10px;">
            <el-row>
                <el-col :span="24">
                    <div class="node-components-content">
                        <el-input v-model="searchNodeComponent" :prefix-icon="Search" clearable style="width: 100%"
                            placeholder="搜索" />
                    </div>
                </el-col>

                <el-col :span="12" v-for="item in filteredWorkflowNodes" :key="item.name" >
                    <div class="node-components-content" @click="clickNode(item)">
                        <div class="node-components-icon">
                            <!-- <i :class="item.properties.icon" /> -->
                             <img :src="getTaskIconPath(item.properties.icon)" />
                        </div>
                        <div class="node-info">
                            <span>{{ item.label }}</span>
                            <desc class="description">{{ item.description }}</desc>
                        </div>
                    </div>
                </el-col>

                 <el-col :span="24" v-if="filteredWorkflowNodes.length === 0">
                    <div class="node-components-content no-results">
                        <el-empty description="没有搜索结果，请重新再搜索." image-size="100" />
                    </div>
                </el-col>

            </el-row>
        </el-scrollbar>
    </div>
</template>

<script setup>
import { ref } from 'vue';

const emit = defineEmits(['clickNode']);
const props = defineProps({
    show: {
        type: Boolean,
        default: false
    },
    id: {
        type: String,
        default: ''
    },
    // workflowRef: Object
});

const searchNodeComponent = ref('')

const workflowNodes = [
    {
        id: '1001',
        name: "AI_CHAT",
        label: "AI对话",
        description: "与 AI 大模型进行对话",
        type: "ai_chat",
        properties: {
            icon: 'chatgpt' , 
            color: "#2962FF",
            stepName: 'AI对话',
            showNode: true,
            height: 622,
            width: 330
        }
    },
    {
        id: '1002',
        name: "IMAGE_UNDERSTAND",
        label: "图片理解",
        description: "识别出图片中的对象、场景等信息回答用户问题",
        type: "image_understand",
        properties: {
            icon: 'mr',
            color: "#388E3C",
            stepName: '图片理解',
            showNode: true,
            height: 584,
            width: 330
        }
    },
    {
        id: '1003',
        name: "IMAGE_GENERATE",
        label: "执行SHELL",
        description: "通过Shell执行系统命令，支持标准输出与错误信息返回",
        type: "shell",
        properties: {
            icon: 'shell' , 
            color: "#F57C00",
            stepName: '执行SHELL',
            showNode: true,
            height: 514,
            width: 330
        }
    },
    {
        id: '1005',
        name: "FLINK",
        label: "执行FLINK",
        description: "提交并运行Apache Flink任务，返回执行状态与结果",
        type: "flink",
        properties: {
            icon: 'flink' , 
            color: "#616161",
            stepName: '执行FLINK',
            showNode: true,
            height: 570,
            width: 330
        }
    },
    {
        id: '1006',
        name: "CONDITION",
        label: "判断器",
        description: "根据不同条件执行不同的节点",
        type: "condition",
        properties: {
            icon: 'conditions' , 
            color: "#303F9F",
            stepName: '判断器',
            showNode: true,
            height: 352,
            width: 540,
            branch_condition_list: [
                {
                    "index": 0,
                    "height": 129,
                    "id": "1009"
                },
                {
                    "index": 2,
                    "height": 44,
                    "id": "161"
                }
            ]
        }
    },
    {
        id: '1007',
        name: "SQL",
        label: "运行SQL",
        description: "执行SQL查询，变量转字符串处理后输出结果集",
        type: "sql",
        properties: {
            icon: 'sql' , 
            color: "#FF9800",
            stepName: '运行SQL',
            showNode: true,
            height: 500,
            width: 330
        }
    },
    {
        id: '1009',
        name: "NOTICE",
        label: "IM通知",
        description: "通过 IM 渠道发送通知消息，支持文本内容推送",
        type: "notice",
        properties: {
            icon: 'dinky',
            color: "#F4511E",
            stepName: 'IM通知',
            showNode: true,
            height: 570,
            width: 330
        }
    },
    {
        id: '1010',
        name: "DOCUMENT_EXTRACT",
        label: "文档内容提取",
        description: "提取文档中的内容",
        type: "document_extract",
        properties: {
            icon: 'chunjun' , 
            color: "#8E24AA",
            stepName: '文档内容提取',
            showNode: true,
            height: 320,
            width: 330
        }
    },
    {
        id: '1011',
        name: "SPARK",
        label: "执行SPARK",
        description: "提交并执行 Apache Spark 作业，处理分布式数据计算任务",
        type: "spark",
        properties: {
            icon: 'spark',
            color: "#00ACC1",
            stepName: '执行SPARK',
            showNode: true,
            height: 563,
            width: 330
        }
    },
    {
        id: '1012',
        name: "PYTHON",
        label: "执行PYTHON",
        description: "执行 Python 程序，支持标准输出与错误信息返回",
        type: "python",
        properties: {
            icon: 'python' , 
            color: "#FFB300",
            stepName: '执行PYTHON',
            showNode: true,
            height: 390,
            width: 330
        }
    },
    {
        id: '1013',
        name: "function",
        label: "执行GROOVY",
        description: "使用Groovy脚本进行编辑开发",
        type: "function",
        properties: {
            icon: 'groovy',
            color: "#424242",
            stepName: '执行GROOVY',
            showNode: true,
            height: 657,
            width: 330
        }
    },
    {
        id: '1014',
        name: "HTTP_API",
        label: "调用HTTP",
        description: "开发来实现Http接口的调用。",
        type: "http_api",
        properties: {
            icon: 'http' , 
            color: "#424242",
            stepName: '调用HTTP',
            showNode: true,
            height: 540,
            width: 330
        }
    },
    {
        // 结束节点
        id: '1015',
        name: "END",
        label: "结束节点",
        description: "结束流程",
        type: "end",
        properties: {
            icon: 'sqoop' , 
            color: "#424242",
            stepName: '结束节点',
            showNode: true,
            height: 330,
            width: 330
        }
    }
];

// 将搜索拆分为关键字数组（去除空项），匹配任一字段包含所有关键字才保留
const filteredWorkflowNodes = computed(() => {
    const q = (searchNodeComponent.value || '').trim().toLowerCase();
    if (!q) {
        return workflowNodes;
    }
    const terms = q.split(/\s+/).filter(Boolean);
    return workflowNodes.filter(node => {
        const hay = `${node.label} ${node.name} ${node.description} ${node.type}`.toLowerCase();
        // 所有 terms 必须都出现在 hay 中
        return terms.every(t => hay.indexOf(t) !== -1);
    });
});

// 辅助：安全地构造正则（转义特殊字符）
function escapeRegExp(string = '') {
    return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

// 高亮 label（返回 HTML）
const highlightLabel = (item) => {
    const q = (searchNodeComponent.value || '').trim();
    if (!q) return escapeHtml(item.label);
    return highlightText(item.label, q);
}

// 高亮 description（返回 HTML）
const highlightDesc = (item) => {
    const q = (searchNodeComponent.value || '').trim();
    if (!q) return escapeHtml(item.description);
    return highlightText(item.description, q);
}

// 将 text 中匹配的关键字用 <mark> 包裹（支持多关键字，忽略大小写）
function highlightText(text = '', query = '') {
    const terms = query.split(/\s+/).filter(Boolean);
    if (terms.length === 0) return escapeHtml(text);

    // 依次替换每个关键字（注意逐一替换以避免影响后续关键字）
    let result = escapeHtml(text);
    terms.forEach(term => {
        const re = new RegExp(escapeRegExp(term), 'ig');
        result = result.replace(re, match => `<mark class="node-highlight">${match}</mark>`);
    });
    return result;
}

// 简单 HTML 转义，防止注入
function escapeHtml(str = '') {
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

const clickNode = (item) => {
    // const { id, workflowRef } = props;
    // 点击之后，show 设置为 false
    // props.show = false;
    console.log('item = ' + item);
    emit('clickNode', item);

    // props.workflowRef?.clickNode(item);

    // return item ;
};

defineExpose({ clickNode });

</script>

<style lang="scss" scoped>
.node-components {
    position: fixed;
    right: 120px;
    z-index: 100000;
    width: 600px;
    background: #ffffff;
    padding-bottom: 20px;
    border-radius: 5px;
    box-shadow: var(--el-box-shadow-light);
    bottom: 90px;
    left: 250px;

    .node-components-title {
        font-size: 14px;
        padding: 15px;
        background: #f5f7fa;
        margin-bottom: 5px;
        display: flex;
        justify-content: space-between;
        align-items: center;
    }

    .node-components-icon {
        width: 35px;
        height: 35px;
        background: #fff;
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        color: rgb(255, 255, 255);
        // padding: 2px;
        border-radius: 5px;
        
        img{
            width: 100%;
            // height: 100%;
        }
    }

    .node-components-content {
        display: flex;
        line-height: 17px;
        padding: 8px 10px;
        gap: 10px;
        cursor: pointer;
        margin-top:5px;
        margin-bottom: 5px;
        align-items: center;
        justify-content: flex-start;

        &:hover {
            background: #f5f5f5;
        }
    }

    .avatar {
        width: 30px;
    }

    .node-info {
        display: flex;
        width: calc(100% - 50px);
        flex-direction: column;
        gap: 5px;
        font-size: 13px;
    }

    .description {
        color: #8f959e;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
    }

    .no-results {
        justify-content: center;
    }
}
</style>