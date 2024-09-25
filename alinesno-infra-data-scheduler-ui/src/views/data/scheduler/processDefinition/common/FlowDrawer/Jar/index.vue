<template>
    <a-drawer :headerStyle="headerStyle" :bodyStyle="bodyStyle" :closable="true" :visible="visible"
        :after-visible-change="afterVisibleChange" width="40%" placement="right" @close="onClose">
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
                    <el-form-item label="脚本">
                        <CodeEditor ref="codeEditorRef" :lang="python" />
                    </el-form-item>
                    <el-form-item label="资源" prop="resourceId">
                        <el-input v-model="form.resourceId" placeholder="请选择资源" />
                    </el-form-item>
                    <el-form-item label="自定义参数" prop="customParams">
                        <el-input v-model="form.customParams" placeholder="请输入自定义参数" />
                    </el-form-item>
                </el-form>

                <div class="flow-setting-footer">
                    <el-button type="primary" bg size="large" @click="submitForm('ruleForm')">确认提交</el-button>
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
import CodeEditor from '../../CodeEditor.vue';

const { proxy } = getCurrentInstance();

const codeEditorRef = ref(null)
const node = ref({})
const visible = ref(false)
const headerStyle = ref({
    'background-image': 'linear-gradient(90deg, #ff9313, #faab4b), linear-gradient(#3b5998, #3b5998)',
    'border-radius': '0px 0px 0 0',
})

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
