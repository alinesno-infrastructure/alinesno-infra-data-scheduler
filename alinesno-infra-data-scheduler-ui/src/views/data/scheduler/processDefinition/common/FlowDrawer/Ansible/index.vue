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
                    <el-form-item label="配置清单" prop="resourceId">
                        <el-tree-select
                            v-model="form.resourceId"
                            :data="resourceData"
                            multiple
                            placeholder="请选择配置清单"
                            :render-after-expand="false"
                            style="width: 500px"
                        />
                    </el-form-item>
                    <el-form-item label="执行脚本">
                        <CodeEditor ref="codeEditorRef" :lang="'yaml'" />
                    </el-form-item>
                    <el-row>
                        <el-col :span="24">
                            <el-form-item label="请求参数" prop="extraVars">
                                <el-input v-model="form.extraVars" resize="none" :rows="1" type="textarea" placeholder="请输入请求参数" />
                            </el-form-item>
                        </el-col>
                    </el-row>
                    <el-form-item label="自定义参数">
                        <el-button type="primary" bg text @click="paramsDialog = true">
                            <i class="fa-solid fa-screwdriver-wrench"></i>&nbsp;配置任务参数
                        </el-button>
                    </el-form-item>
                    <!--
                    <el-form-item label="自定义参数" prop="customParams">
                        <el-input v-model="form.customParams" placeholder="请输入自定义参数" />
                    </el-form-item>
                    -->
                </el-form>

                <div class="flow-setting-footer">
                    <el-button type="primary" text bg @click="handleValidateTask()"><i class="fa-solid fa-truck-fast"></i>&nbsp;验证任务</el-button>
                    <el-button type="primary" bg  @click="submitForm('ruleForm')">确认保存</el-button>
                    <el-button @click="onClose" size="large" text bg>取消</el-button>
                </div>
            </div>
        </div>
        <FlowDrawerFooter @close="onClose" @save="onSave" />

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

    </a-drawer>
</template>

<script setup>

import flowNodeStore from '@/store/modules/flowNode'

import { getAllResource } from '@/api/data/scheduler/resource'
import { branchIcon2 } from '@/utils/flowMixin';
import CodeEditor from '../../CodeEditor.vue';
import { ElLoading } from 'element-plus'
import { validateTask } from '@/api/data/scheduler/processDefinition'

const { proxy } = getCurrentInstance();

const codeEditorRef = ref(null)
const node = ref({})
const visible = ref(false)
const headerStyle = ref({
    'background-image': 'linear-gradient(90deg, #ff9313, #faab4b), linear-gradient(#3b5998, #3b5998)',
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
        resourceId: [],
        customParams: {}
    },
    rules: {
        resourceId: [
            { required: true, message: '请选择资源', trigger: 'blur' }
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
 * 打开侧边栏 
 * @param {*} node 
 * @param {*} routeNode 
 */
function showDrawer(_node) {
    console.log('node = ' + _node.name)

    visible.value = true;
    node.value = _node;
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

            form.value.rawScript = codeEditorRef.value.getRawScript() 

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
