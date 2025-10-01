<template>
  <div>
    <el-form :model="form" :rules="rules" ref="formRef" size="large" label-width="120px">
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name"></el-input>
      </el-form-item>

      <el-form-item label="类型" prop="provider">
        <el-select v-model="form.provider" placeholder="选择类型" @change="onProviderChange">
          <el-option label="企业微信机器人" value="WECHAT_ROBOT"></el-option>
          <el-option label="钉钉机器人" value="DINGTALK"></el-option>
          <el-option label="邮件" value="EMAIL"></el-option>
        </el-select>
      </el-form-item>

      <el-form-item label="启用" prop="enabled">
        <el-switch v-model="form.enabled"></el-switch>
      </el-form-item>

      <!-- 机器人 公共字段 -->
      <template v-if="isRobot">
        <el-form-item label="Webhook" prop="webhook">
          <el-input v-model="form.webhook"></el-input>
        </el-form-item>
        <el-form-item label="Secret" prop="secret">
          <el-input v-model="form.secret" show-password></el-input>
        </el-form-item>
      </template>

      <!-- 邮件字段 -->
      <template v-if="isEmail">
        <el-form-item label="SMTP 主机" prop="smtpHost">
          <el-input v-model="form.smtpHost"></el-input>
        </el-form-item>
        <el-form-item label="SMTP 端口" prop="smtpPort">
          <el-input-number v-model="form.smtpPort" :min="1" :max="65535"></el-input-number>
        </el-form-item>
        <el-form-item label="SMTP 用户名" prop="smtpUsername">
          <el-input v-model="form.smtpUsername"></el-input>
        </el-form-item>
        <el-form-item label="SMTP 密码" prop="smtpPassword">
          <el-input v-model="form.smtpPassword" show-password></el-input>
        </el-form-item>
        <el-form-item label="From 地址" prop="fromAddress">
          <el-input v-model="form.fromAddress"></el-input>
        </el-form-item>
      </template>

      <el-form-item>
        <el-button type="primary" :loading="isLoading" @click="onSubmit">保存</el-button>
        <el-button @click="onCancel">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { toRaw } from 'vue';

// 导入你要求风格的 API（注意路径）
import { getCredential, saveCredential } from '@/api/data/scheduler/notificationConfig';

const props = defineProps({
  id: { type: [String, Number], default: null }
});
const emit = defineEmits(['saved', 'cancel']);

const formRef = ref(null);
const isLoading = ref(false);

const form = reactive({
  id: null,
  name: '',
  provider: '',
  webhook: '',
  secret: '',
  accessToken: '',
  smtpHost: '',
  smtpPort: 25,
  smtpUsername: '',
  smtpPassword: '',
  fromAddress: '',
  enabled: true
});

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  provider: [{ required: true, message: '请选择类型', trigger: 'change' }],
};

const isRobot = ref(false);
const isEmail = ref(false);

function onProviderChange(val) {
  isRobot.value = val === 'WECHAT_ROBOT' || val === 'DINGTALK';
  isEmail.value = val === 'EMAIL';
}

onMounted(() => {
  if (props.id) {
    load(props.id);
  } else {
    onProviderChange(form.provider);
  }
});

watch(() => props.id, (nv) => {
  if (nv) load(nv);
  else {
    Object.assign(form, {
      id: null,
      name: '',
      provider: '',
      webhook: '',
      secret: '',
      accessToken: '',
      smtpHost: '',
      smtpPort: 25,
      smtpUsername: '',
      smtpPassword: '',
      fromAddress: '',
      enabled: true
    });
    onProviderChange('');
  }
});

function load(id) {
  getCredential(id).then(res => {
    const body = res && res.data ? res.data : res;
      const d = body ;
      Object.assign(form, d || {});
      onProviderChange(form.provider);
  }).catch(err => {
    console.error(err);
    ElMessage.error('加载失败');
  });
}

function onSubmit() {
  formRef.value.validate((valid) => {
    if (!valid) return;
    isLoading.value = true;
    saveCredential(toRaw(form)).then(res => {
      isLoading.value = false;
      ElMessage.success('保存成功');
      emit('saved');
    }).catch(err => {
      isLoading.value = false;
      console.error(err);
      ElMessage.error('保存失败');
    });
  });
}

function onCancel() {
  emit('cancel');
}
</script>

<style scoped>
/* 可自定义样式 */
</style>