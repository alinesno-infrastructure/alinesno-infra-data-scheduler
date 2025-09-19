<template>
  <div class="llm-selector-panel">
    <el-select
      class="llm-selector"
      clearable
      :size="props.size"
      v-model="data"
      placeholder="请选择数据源"
    >
      <template #prefix>
        <!-- 根据选中的值动态显示图标 -->
        <img
          v-if="selectedModelIcon"
          :src="selectedModelIcon"
          alt="模型图片"
          style="width: 20px; height: 20px; margin-right: 0px;"
        />
      </template>

      <el-option
        v-for="model in models"
        :key="model.id"
        :label="model.name"
        :value="model.id"
      >
        <template #default>
          <img
            :src="model.imageUrl"
            alt="模型图片"
            style="width: 20px; height: 20px; margin-right: 10px;"
          />
          {{ model.name }}
        </template>
      </el-option>
    </el-select>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, defineEmits } from 'vue';
import { getDatasourceIconPath } from '@/utils/llmIcons';
import { listAvailableDataSources } from "@/api/data/scheduler/datasources";

// 定义 props
const props = defineProps({
  properties: {
    type: Object,
    default: () => ({})
  },
  nodeModel: {
    type: Object
  },
  size: {
    type: String,
    default: 'default'
  },
  modelValue: {
    type: String,
    default: ''
  },
  modelType: {
    type: String,
    default: ''
  }
});

// 定义 emits
const emit = defineEmits(['update:modelValue']);

// 数据源列表
const models = ref([]);

// internal state: 作为 v-model 的真实数据源
const selectedDatabase = ref(props.modelValue || '');

// 当父组件改变 modelValue 时，保持同步
watch(
  () => props.modelValue,
  (val) => {
    selectedDatabase.value = val;
  }
);

// 计算选中的模型图标
const selectedModelIcon = computed(() => {
  const selected = models.value.find((model) => model.id === selectedDatabase.value);
  return selected ? selected.imageUrl : null;
});

// 将 el-select 的 v-model 绑定到 data（内部 selectedDatabase）
// set 时既更新内部状态，也 emit 给父组件
const data = computed({
  get: () => selectedDatabase.value,
  set: (value) => {
    selectedDatabase.value = value;
    emit('update:modelValue', value);
  }
});

// 加载数据源列表
onMounted(() => {
  listAvailableDataSources()
    .then(response => {
      // 假设 response.data 是数组
      const newModels = (response.data || []).map(item => ({
        id: item.id,
        name: item.readerName + '(' + (item.readerDesc || '') + ')',
        imageUrl: getDatasourceIconPath(item.readerType)
      }));
      models.value = newModels;

      // 如果父已经传入了初始值且在列表中存在，确保内部选中一致
      if (props.modelValue) {
        const initialSelectedModel = models.value.find(model => model.id === props.modelValue);
        if (initialSelectedModel) {
          selectedDatabase.value = initialSelectedModel.id;
        } else {
          // 如果列表中未找到传入的 modelValue，仍然保留该值（以便父组件控制）
          selectedDatabase.value = props.modelValue;
        }
      }
    })
    .catch(err => {
      // 可根据需要处理错误
      console.error('listAvailableDataSources error:', err);
    });
});
</script>

<style scoped lang="scss">
.llm-selector-panel {
  width: 100%;
  .llm-selector {
    width: 100%;
  }
}
</style>