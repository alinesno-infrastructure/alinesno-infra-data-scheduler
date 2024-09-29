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
                        <el-input v-model="form.name" placeholder="请输入节点名称" />
                    </el-form-item>
                    <!-- 
                    <el-form-item label="描述" prop="desc">
                        <el-input v-model="form.desc" resize="none" :rows="3" type="textarea" placeholder="请输入节点描述" />
                    </el-form-item> 
                    -->
                    <el-form-item label="环境名称" prop="env">
                        <el-radio-group v-model="form.env">
                            <el-radio :label="'shabox'">沙箱环境</el-radio>
                            <el-radio :label="'prod'">生产环境</el-radio>
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
                    <el-row>
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
                    <el-form-item label="自定义参数">
                        <el-button type="primary" bg text @click="paramsDialog = true">
                            <i class="fa-solid fa-screwdriver-wrench"></i>&nbsp;配置任务参数
                        </el-button>
                    </el-form-item>
                </el-form>

                <div class="flow-setting-footer">
                    <el-button type="primary" bg size="large" @click="submitForm('ruleForm')">确认保存</el-button>
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

import flowNodeStore from '@/store/modules/flowNode'

import { branchIcon2 } from '@/utils/flowMixin';
import ContextParam from "../../../params/contextParam.vue";

const { proxy } = getCurrentInstance();

const codeEditorRef = ref(null)
const node = ref({})
const visible = ref(false)
const headerStyle = ref({
    'background-image': 'linear-gradient(90deg, #ff9313, #faab4b), linear-gradient(#3b5998, #3b5998)',
    'border-radius': '0px 0px 0 0',
})

const paramsDialog = ref(false) 
const taskParamRef = ref(null)

const data = reactive({
    form: {
        name: '',
        desc: '',
        delivery: false,
        retryCount: 0,
        env: '',
        rawScript: '' ,
        resourceId: '',
        customParams: ''
    },
    rules: {
        name: [
            { required: true, message: '请输入节点名称', trigger: 'blur' },
            { min: 3, max: 50, message: '长度在 3 到 50 个字符', trigger: 'blur' }
        ],
        gitUrl: [
            { required: true, message: '请输入节点描述', trigger: 'blur' },
        ],
        gitUsername: [
            { required: true, message: '请选择超时告警', trigger: 'change' }
        ],
        gitPassword: [
            { required: true, message: '请选择失败重试次数', trigger: 'change' }
        ],
        gitBranch: [
            { required: true, message: '请选择环境名称', trigger: 'change' }
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
}

/**
 * 关闭侧边栏
 */
function onClose() {
    visible.value = false;
}

defineExpose({ showDrawer })

</script>
