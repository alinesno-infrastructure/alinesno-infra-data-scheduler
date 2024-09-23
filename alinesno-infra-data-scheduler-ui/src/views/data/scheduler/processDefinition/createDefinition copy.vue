<template>
   <div class="app-container">
    <el-page-header @back="goBack" content="配置任务编排"></el-page-header>


    <div class="form-container" >
      <el-form :model="form" :rules="rules" ref="ruleFormRef" label-width="180px">


         <el-form-item>
            <el-button @click="goBack">上一步</el-button>
            <el-button icon="SuccessFilled" type="primary" @click="submitForm('form')">提交任务 </el-button>
         </el-form-item>
      </el-form>
    </div>

  </div>
</template>

<script setup name="createProcessDefinition">

const router = useRouter();
const { proxy } = getCurrentInstance();

const data = reactive({
  form: {
  },
  rules: {
  }
});

const { form, rules } = toRefs(data);

/** 从本地存储加载表单数据 */
function loadFormDataFromStorage() {
  const formDataStr = localStorage.getItem('processDefinitionFormData');
  if (formDataStr) {
    try {
      const formData = JSON.parse(formDataStr);
      Object.assign(form.value, formData);

      readerFieldMate.value = form.value.readerSourceProps.readerFieldMate
      sinkFieldMate.value = form.value.sinkSourceProps.sinkFieldMate

      console.log('Loaded form data from localStorage:', form.value);
    } catch (error) {
      console.error('Error parsing form data from localStorage:', error);
    }
  } else {
    console.log('No form data found in localStorage.');
  }
}

loadFormDataFromStorage(); // 调用此方法以加载数据

</script>


<style scoped lang="scss">
  .form-container {
    max-width: 80%;
    margin-left: auto;
    margin-right: auto;
    margin-top: 20px;
  }

  .label-title {
    text-align: center;
    max-width: 960px;
    margin-left: auto;
    margin-right: auto;
    margin-top: 10px;

    .tip {
      padding-bottom: 10px;
      font-size: 26px;
      font-weight: bold;
    }

    .sub-tip {
      font-size: 13px;
      text-align: center;
      padding: 10px;
    }
  }

  .image{
    width:100%;
    height: 120px ;
  }

  .select-card {
    border: 1px solid rgb(0, 91, 212) ;
  }

</style>