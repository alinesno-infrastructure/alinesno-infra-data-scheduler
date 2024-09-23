<template>
  <div>
    <el-table
      :data="envVarsList"
      style="width: 100%"
      @row-dblclick="handleEdit"
      @row-remove="handleRemove"
    >
      <el-table-column type="index" width="50" align="center" />
      <el-table-column prop="key" label="Key名称" width="200">
        <template #default="{ row, $index }">
          <el-input v-if="row.editing" v-model="row.key"  />
          <span v-else>{{ row.key }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="value" label="Value值">
        <template #default="{ row, $index }">
          <el-input v-if="row.editing" v-model="row.value"  />
          <span v-else>{{ row.value }}</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="200">
        <template #default="{ row, $index }">
          <el-button v-if="!row.editing"  type="primary" @click="handleEdit(row, $index)">编辑</el-button>
          <el-button v-else  type="success" @click="handleSave(row, $index)">保存</el-button>
          <el-button v-if="!row.editing"  type="danger" @click="handleRemove(row, $index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <br/>
    <el-button type="primary" @click="addEnvVar">添加环境变量</el-button>
    <!-- <el-button type="primary" @click="getEnvVarsAsJson">获取 JSON</el-button> -->
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus';

const props = defineProps({
  context: {
    type: Object ,
  }
});

// 定义环境变量列表
const envVarsList = ref([
  { key: 'VAR1', value: 'value1', editing: false },
  { key: 'VAR2', value: 'value2', editing: false },
]);

// 添加新的环境变量
function addEnvVar() {
  // 检查新添加的环境变量值是否已存在
  if (envVarsList.value.some(item => item.value === '')) {
    ElMessage.error('已有未命名的环境变量，请先命名后再添加新的环境变量');
    return;
  }
  envVarsList.value.push({ key: '', value: '', editing: true });
}

// 开始编辑
function handleEdit(row, index) {
  row.editing = true;
}

// 保存编辑
function handleSave(row, index) {
  // 检查是否有重复的值
  const existingValues = envVarsList.value.filter(item => item !== row).map(item => item.value);
  if (existingValues.includes(row.value)) {
    ElMessage.error('环境变量值已存在，请使用不同的值');
    return;
  }

  if (row.key.trim() === '' || row.value.trim() === '') {
    ElMessage.error('Key 和 Value 均不能为空');
    return;
  }
  row.editing = false;
}

// 删除环境变量
function handleRemove(row, index) {
  envVarsList.value.splice(index, 1);
}

// 获取环境变量 JSON
function getEnvVarsAsJson() {
  const envVarsJson = {};
  envVarsList.value.forEach(item => {
    envVarsJson[item.key] = item.value;
  });
  console.log(envVarsJson);
  // 或者你可以将 JSON 发送到后端或者显示给用户
  return envVarsJson
}

// 定义一个方法来根据props.context初始化envVarsList
function initEnvVarsListFromContext() {
  if (props.context && typeof props.context === 'object') {
    const list = Object.entries(props.context).map(([key, value]) => ({
      key,
      value,
      editing: false
    }));
    envVarsList.value = list;
  }
}

// 在组件挂载完成后调用此方法
onMounted(() => {
  initEnvVarsListFromContext();
});

// 主动暴露方法
defineExpose({ getEnvVarsAsJson })

</script>

<style scoped lang="scss">
/* 样式可以根据需要调整 */
</style>