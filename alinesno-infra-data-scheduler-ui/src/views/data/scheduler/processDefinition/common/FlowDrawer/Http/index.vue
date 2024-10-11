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
                        <el-input v-model="form.name" :value="node.name" disabled="disabled" placeholder="请输入节点名称" />
                    </el-form-item>
                    <el-row>
                        <el-col :span="24">
                            <el-form-item label="地址" prop="url">
                                <el-input v-model="form.url" placeholder="请输入请求地址" />
                            </el-form-item>
                        </el-col>
                    </el-row>
                    <el-form-item label="请求方式" prop="method">
                        <el-radio-group v-model="form.method">
                            <el-radio :label="'get'">GET</el-radio>
                            <el-radio :label="'post'">POST</el-radio>
                            <el-radio :label="'put'">PUT</el-radio>
                            <el-radio :label="'delete'">DELETE</el-radio>
                        </el-radio-group>
                    </el-form-item>
                    <el-row>
                        <el-col :span="24">
                            <el-form-item label="请求参数" prop="requestBody">
                                <el-input v-model="form.requestBody" resize="none" :rows="1" type="textarea" placeholder="请输入请求参数" />
                            </el-form-item>
                        </el-col>
                    </el-row>
                    <el-form-item label="重试次数" prop="retryCount">
                        <el-radio-group v-model="form.retryCount">
                            <el-radio :label="1">1次</el-radio>
                            <el-radio :label="3">3次</el-radio>
                            <el-radio :label="5">5次</el-radio>
                        </el-radio-group>
                    </el-form-item>
                </el-form>

                <div class="flow-setting-footer">
                    <el-button type="primary" text bg @click="handleValidateTask()"><i class="fa-solid fa-truck-fast"></i>&nbsp;验证任务</el-button>
                    <el-button type="primary" bg  @click="submitForm('ruleForm')">确认保存</el-button>
                    <el-button @click="onClose" size="large" text bg>取消</el-button>
                </div>
            </div>
        </div>
        <FlowDrawerFooter @close="onClose" @save="onSave" />
    </a-drawer>
</template>

<script setup>

import flowNodeStore from '@/store/modules/flowNode'
import { branchIcon2 } from '@/utils/flowMixin';
import { ElLoading } from 'element-plus'
import { validateTask } from '@/api/data/scheduler/processDefinition'

const { proxy } = getCurrentInstance();

const codeEditorRef = ref(null)
const node = ref({})
const visible = ref(false)
const headerStyle = ref({
    'background-image': 'linear-gradient(90deg, #268bfb -16.37%, #33e1ae 137.34%), linear-gradient(#3b5998, #3b5998)',
    'border-radius': '0px 0px 0 0',
})

const data = reactive({
    form: {
        name: '',
        url: '',
        method: 'get',
        requestBody: '',
        resourceIds:[],
        retryCount: 1
    },
    rules: {
        url: [
            { required: true, message: '请输入请求地址', trigger: 'blur' }
        ],
        method: [
            { required: true, message: '请选择请求方式', trigger: 'change' }
        ],
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
    form.value = _node.params ;
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
