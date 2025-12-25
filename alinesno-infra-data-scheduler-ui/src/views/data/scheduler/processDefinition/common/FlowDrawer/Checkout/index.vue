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
                    <el-form-item label="环境名称" prop="env">
                        <el-radio-group v-model="form.env">
                            <el-radio :label="'shabox'">沙箱环境</el-radio>
                            <el-radio :label="'prod'">生产环境</el-radio>
                        </el-radio-group>
                    </el-form-item>
                    -->

                    <el-form-item label="类型" prop="sourceType">
                        <el-radio-group v-model="form.sourceType">
                            <el-radio :label="'public'">开源</el-radio>
                            <el-radio :label="'private'">私有</el-radio>
                        </el-radio-group>
                    </el-form-item>
                    <el-form-item label="仓库地址" prop="gitUrl">
                        <el-input v-model="form.gitUrl" placeholder="请配置仓库地址" />
                    </el-form-item>
                    <el-row>
                        <el-col :span="12">
                            <el-form-item label="分支" prop="gitBranch">
                                <el-input v-model="form.gitBranch" placeholder="请配置分支" />
                            </el-form-item>
                        </el-col>
                    </el-row>
                    <el-row v-if="form.sourceType == 'private'">
                        <el-col :span="12">
                            <el-form-item label="账号" prop="gitUsername">
                                <el-input v-model="form.gitUsername" placeholder="请配置账号" />
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                            <el-form-item label="密码" prop="gitPassword">
                                <el-input v-model="form.gitPassword" placeholder="请配置密码" />
                            </el-form-item>
                        </el-col>
                    </el-row>

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

// import nodeSessionStore from '@/utils/nodeUtils'

import { useNodeStore } from '@/store/modules/flowNode'; // 根据实际情况调整路径
const flowNodeStore = useNodeStore();

import { getAllResource } from '@/api/data/scheduler/resource'
import { branchIcon2 } from '@/utils/flowMixin';
import { ElLoading } from 'element-plus'
import { validateTask } from '@/api/data/scheduler/processDefinition'

const { proxy } = getCurrentInstance();

const node = ref({})
const visible = ref(false)
const headerStyle = ref({
    'background-image': 'linear-gradient(90deg, #ff9313, #faab4b), linear-gradient(#015bd4, #015bd4)',
    'border-radius': '0px 0px 0 0',
})

const paramsDialog = ref(false) 
const taskParamRef = ref(null)

const resourceData = ref([])

const data = reactive({
    form: {
        name: '',
        sourceType: 'private',
        gitUrl: '',
        gitBranch: 'master',
        gitUsername: '',
        gitPassword: '',
    },
    rules: {
        name: [
            { required: true, message: '请输入节点名称', trigger: 'blur' },
            { min: 3, max: 50, message: '长度在 3 到 50 个字符', trigger: 'blur' }
        ],
        gitUrl: [
            { required: true, message: '请配置仓库地址', trigger: 'blur' },
        ],
        gitUsername: [
            { required: true, message: '请配置账号名', trigger: 'change' }
        ],
        gitPassword: [
            { required: true, message: '请配置克隆密码', trigger: 'change' }
        ],
        gitBranch: [
            { required: true, message: '请配置分支', trigger: 'change' }
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
            // nodeSessionStore.setNode(node.value);
            flowNodeStore.setNode(node.value);
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
