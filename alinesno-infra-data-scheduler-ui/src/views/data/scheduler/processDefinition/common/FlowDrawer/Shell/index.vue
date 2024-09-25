<template>
    <a-drawer :headerStyle="headerStyle" :bodyStyle="bodyStyle" :closable="true" :visible="visible"
        :after-visible-change="afterVisibleChange" width="40%" placement="right" @close="onClose">
        <template #title>
            <img :src="branchIcon2" class="anticon" />
            <span class="flow-ant-drawer-title">
                <!-- <EditName :value="node.name" :nodeId="node.id" /> -->
                {{ node.name }}
            </span>
        </template>
        <div class="flow-setting-module">
            <div class="flow-setting-content">

                <el-form :model="form" :rules="rules" label-width="auto" style="max-width: 980px" ref="ruleForm">
                    <el-form-item label="节点名称" prop="name">
                        <el-input v-model="form.name" :value="node.name" disabled="disabled" placeholder="请输入节点名称" />
                    </el-form-item>
                    <el-form-item label="描述" prop="desc">
                        <el-input v-model="form.desc" resize="none" :rows="3" type="textarea" placeholder="请输入节点描述" />
                    </el-form-item>
                    <el-form-item label="超时告警" prop="delivery">
                        <el-switch v-model="form.delivery" />
                    </el-form-item>
                    <el-form-item label="失败重试次数" prop="retryCount">
                        <el-radio-group v-model="form.retryCount">
                            <el-radio :label="0">0次</el-radio>
                            <el-radio :label="1">1次</el-radio>
                            <el-radio :label="3">3次</el-radio>
                        </el-radio-group>
                    </el-form-item>
                    <el-form-item label="环境名称" prop="env">
                        <el-radio-group v-model="form.env">
                            <el-radio :label="'shabox'">沙箱环境</el-radio>
                            <el-radio :label="'prod'">生产环境</el-radio>
                        </el-radio-group>
                    </el-form-item>
                    <el-form-item label="脚本">
                        <CodeEditor ref="codeEditorRef" :lang="shell" />
                    </el-form-item>
                    <el-form-item label="资源" prop="resourceId">
                        <!-- <el-input v-model="form.resourceId" placeholder="请选择资源" /> -->
                        <el-tree-select
                            v-model="form.resourceId"
                            :data="resourceData"
                            multiple
                            placeholder="请选择资源"
                            :render-after-expand="false"
                            style="width: 500px"
                        />
                    </el-form-item>
                    <el-form-item label="自定义参数">
                        <el-button type="primary" bg text @click="paramsDialog = true">
                            <i class="fa-solid fa-screwdriver-wrench"></i>&nbsp;配置任务参数
                        </el-button>
                    </el-form-item>
                </el-form>

                <div class="flow-setting-footer">
                    <el-button type="primary" bg size="large" @click="submitForm('ruleForm')">确认提交</el-button>
                    <el-button @click="onClose" size="large" text bg>取消</el-button>
                </div>
            </div>
        </div>

        <el-dialog title="全局环境变量" v-model="paramsDialog" append-to-body destroy-on-close class="scrollbar">
            <ContextParam ref="taskParamRef" :context="form.context" />
            <template #footer>
            <div class="dialog-footer">
                <el-button @click="paramsDialog = false">取消</el-button>
                <el-button type="primary" @click="callTaskParamRef()">
                确认 
                </el-button>
            </div>
            </template>
        </el-dialog>

        <FlowDrawerFooter @close="onClose" @save="onSave" />
    </a-drawer>
</template>

<script setup>

import flowNodeStore from '@/store/modules/flowNode'

import { branchIcon2 } from '@/utils/flowMixin';
import ContextParam from "../../../params/contextParam.vue";
import CodeEditor from '../../CodeEditor.vue';

const { proxy } = getCurrentInstance();

const paramsDialog = ref(false) 
const taskParamRef = ref(null)

const codeEditorRef = ref(null)
const node = ref({})
const visible = ref(false)
const headerStyle = ref({
    'background-color': '#3b5998',
    'border-radius': '0px 0px 0 0',
})

const resourceData = ref([
  {
    value: '2',
    label: '项目文档', // Level one 2 换成 项目文档
    children: [
      {
        value: '2-1-1',
        label: 'requirements.docx' // 假设 Level three 2-1-1 换成 requirements.docx 文件
      },
      {
        value: '2-2-1',
        label: 'design.pdf' // 假设 Level three 2-2-1 换成 design.pdf 文件
      },
    ],
  },
])

const data = reactive({
    form: {
        name: '',
        desc: '',
        delivery: false,
        retryCount: 0,
        env: 'shabox',
        rawScript: '' ,
        resourceId: '',
        customParams: ''
    },
    rules: {
        desc: [
            { required: true, message: '请输入节点描述', trigger: 'blur' },
            { min: 10, max: 200, message: '长度在 10 到 200 个字符', trigger: 'blur' }
        ],
        delivery: [
            { required: true, message: '请选择超时告警', trigger: 'change' }
        ],
        retryCount: [
            { required: true, message: '请选择失败重试次数', trigger: 'change' }
        ],
        env: [
            { required: true, message: '请选择环境名称', trigger: 'change' }
        ],
        resourceId: [
            { required: true, message: '请选择资源', trigger: 'blur' }
        ],
        customParams: [
            { required: true, message: '请输入自定义参数', trigger: 'blur' }
        ]
    }
});

const { form, rules } = toRefs(data);

/**
 * 提交表单 
 * @param {*} formName 
 */
const submitForm = (formName) => {
    const formInstance = proxy.$refs[formName];
    formInstance.validate((valid) => {
        if (valid) {

            form.value.rawScript = codeEditorRef.value.getRawScript() 

            // 更新节点信息
            node.value.params = form.value;
            flowNodeStore().setNode(node.value);
            onClose();
        } else {
            console.log('验证失败!');
            return false;
        }
    });
};

/** 
 * 获取到环境变量值  
 */
function callTaskParamRef(){
 let contextParam = taskParamRef.value.getEnvVarsAsJson() ; 
 form.value.customParams = contextParam ;
 paramsDialog.value = false ;

 console.log(JSON.stringify(contextParam, null, 2));
}

/**
 * 打开侧边栏 
 * @param {*} node 
 * @param {*} routeNode 
 */
function showDrawer(_node) {
    console.log('node = ' + _node.name)

    visible.value = true;
    node.value = _node;
}

/**
 * 关闭侧边栏
 */
function onClose() {
    visible.value = false;
}

defineExpose({ showDrawer })

</script>
