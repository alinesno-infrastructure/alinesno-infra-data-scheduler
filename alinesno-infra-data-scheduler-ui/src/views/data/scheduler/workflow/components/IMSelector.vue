<template>
  <div class="im-selector-panel">
    <el-select
      class="im-selector"
      clearable
      :size="props.size"
      v-model="data"
      placeholder="请选择通知"
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
        v-for="model in ims"
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

import { ref, computed, defineEmits, nextTick } from 'vue';
import { getImIconPath } from '@/utils/llmIcons';

import {
  listAllIM
} from "@/api/data/scheduler/notificationConfig";

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
});

// 定义大模型列表，添加 id 属性
const ims = ref([]);

// 定义选中的模型
const selectedModel = ref('');

// 计算选中模型的图标
const selectedModelIcon = computed(() => {
  const selected = ims.value.find((model) => model.id === selectedModel.value);
  return selected ? selected.imageUrl : null;
})

// 定义 emits
const emit = defineEmits(['update:modelValue']);

// 定义 computed 属性
const data = computed({
  set: (value) => {
    console.log('data = ' + JSON.stringify(value));
    selectedModel.value = value;
    emit('update:modelValue', value);
  },
  get: () => {
    return props.modelValue;
  }
})

nextTick(() => {
  listAllIM().then(response => {
    console.log('listAllIM = ' + JSON.stringify(response.data));
    // 转换接口返回的数据结构
    const newModels = response.data.map(item => ({
      id: item.id,
      name: item.name ,
      imageUrl: getImIconPath(item.provider) 
    }));
    ims.value = newModels;

    // 根据传入的 modelValue 更新 selectedModel
    const initialSelectedModel = ims.value.find(model => model.id === props.modelValue);
    if (initialSelectedModel) {
      selectedModel.value = initialSelectedModel.id;
    }
  });
})

</script>

<style scoped lang="scss">
/* 可以在这里添加自定义样式 */
.im-selector-panel {
  width: 100%;
  .im-selector {
    width: 100%;
  }
}
</style>