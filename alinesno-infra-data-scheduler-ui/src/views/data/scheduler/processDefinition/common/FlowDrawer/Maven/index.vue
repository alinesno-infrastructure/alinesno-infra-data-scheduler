<template>
    <a-drawer :headerStyle="headerStyle" :bodyStyle="bodyStyle" :closable="true" :visible="visible"
        :after-visible-change="afterVisibleChange" width="50%" placement="right" @close="onClose">
        <template #title>
            <img :src="branchIcon2" class="anticon" />
            <span class="flow-ant-drawer-title">
                {{ node.name }}
            </span>
        </template>
        <div class="flow-setting-module">
            <div class="flow-setting-content">

                <el-form :model="form" :rules="rules" label-width="auto" style="max-width: 980px" ref="ruleForm">
                    <el-form-item label="节点名称" prop="name">
                        <el-input v-model="form.name" disabled="disabled" placeholder="请输入节点名称" />
                    </el-form-item>
                    <!--
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
                            <el-radio :label="'Sponsor'">沙箱环境</el-radio>
                            <el-radio :label="'Venue'">生产环境</el-radio>
                        </el-radio-group>
                    </el-form-item>
                    -->
                    <el-form-item label="命令" prop="name">
                        <el-input v-model="form.goals" placeholder="请输入maven命令，比如 clean install" />
                    </el-form-item>
                    <el-form-item label="构建文件" prop="name">
                        <el-input v-model="form.pomXml" placeholder="请输入pom路径，比如 pom.xml" />
                    </el-form-item>
                    <el-form-item label="Setting路径" prop="settings">
                        <el-input v-model="form.settings" placeholder="请输入settings.xml路径，默认取maven下的settings.xml" />
                    </el-form-item>
                    <!--
                    <el-form-item label="脚本">
                        <CodeEditor ref="codeEditorRef" :lang="python" />
                    </el-form-item>
                    -->
                    <el-form-item label="资源" prop="resourceId">
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
                    <el-button type="primary" text bg @click="handleValidateTask()"><i class="fa-solid fa-truck-fast"></i>&nbsp;验证任务</el-button>
                    <el-button type="primary" bg  @click="submitForm('ruleForm')">确认保存</el-button>
                    <el-button @click="onClose" size="large" text bg>取消</el-button>
                </div>
            </div>
        </div>

        <el-dialog title="任务环境变量" v-model="paramsDialog" append-to-body destroy-on-close class="scrollbar">
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

import nodeSessionStore from '@/utils/nodeUtils'

import { getAllResource } from '@/api/data/scheduler/resource'
import { branchIcon2 } from '@/utils/flowMixin';
import { ElLoading } from 'element-plus'
import { validateTask } from '@/api/data/scheduler/processDefinition'
import ContextParam from "../../../params/contextParam.vue";

const { proxy } = getCurrentInstance();

const paramsDialog = ref(false) 
const taskParamRef = ref(null)

// const codeEditorRef = ref(null)

const node = ref({})
const visible = ref(false)
const headerStyle = ref({
    'background-image': 'linear-gradient(90deg, #29cc80, #5ccc98), linear-gradient(#29cc80, #29cc80)',
    'border-radius': '0px 0px 0 0',
})

const resourceData = ref([])

const data = reactive({
    form: {
        name: '',
        desc: '',
        delivery: false,
        retryCount: 0,
        env: '',
        rawScript: '' ,
        customParams: {} 
    },
    rules: {
        name: [
            { required: true, message: '请输入节点名称', trigger: 'blur' },
            { min: 3, max: 50, message: '长度在 3 到 50 个字符', trigger: 'blur' }
        ],
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
        // resourceId: [
        //     { required: true, message: '请选择资源', trigger: 'blur' }
        // ],
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

            // form.value.rawScript = codeEditorRef.value.getRawScript() 

            // 更新节点信息
            node.value.params = form.value;
            nodeSessionStore.setNode(node.value);
            onClose();
        } else {
            console.log('验证失败!');
            return false;
        }
    });
};


/**
 * 打开侧边栏 
 * @param {*} node 
 * @param {*} routeNode 
 */
function showDrawer(_node) {
    console.log('node = ' + _node.name)

    visible.value = true;
    node.value = _node;

    if(_node.params){
        form.value = _node.params
    }

    form.value.name = _node.name;
    
    nextTick(() => {
        getAllResource().then(res => {
            resourceData.value = res.data
        })
    })
}

/** 验证脚本任务 */
function handleValidateTask() {

    const formInstance = proxy.$refs['ruleForm'];
    formInstance.validate((valid) => {
        if (valid) {
            
    const loading = ElLoading.service({
        lock: true,
        text: 'Loading',
        background: 'rgba(0, 0, 0, 0.7)',
    })

    const formDataStr = localStorage.getItem('processDefinitionFormData');
    const formData = JSON.parse(formDataStr);
    let data = {
        taskParams: form.value,
        taskType: node.value.type,
        context: formData
    }

    // 提交流程信息
    validateTask(data).then(response => {
        console.log(response);
        proxy.$modal.msgSuccess("任务执行成功,无异常.");
        loading.close();
    }).catch(error => {
        loading.close();
    })
            
        } else {
            console.log('验证失败!');
            return false;
        }
    });

}

/**
 * 关闭侧边栏
 */
function onClose() {
    visible.value = false;
}

defineExpose({ showDrawer })

</script>
