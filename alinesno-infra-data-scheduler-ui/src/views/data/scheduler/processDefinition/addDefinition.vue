<template>
  <div class="app-container">
    <el-page-header @back="goBack" content="任务配置"></el-page-header>
    <div class="label-title">
      <div class="tip">创建数据编排任务</div>
      <div class="sub-tip">根据业务场景需求创建数据编排任务，便于数据业务场景开发和分析</div>
    </div>
    <div class="form-container">
      <el-form :model="form" :rules="rules" ref="databaseRef" label-width="180px" size="large">

        <el-form-item label="图标" prop="icon">
            <el-radio-group v-model="form.icon">
              <el-radio v-for="item in icons"
                :value="item.icon"
                :key="item.icon"
                :label="item.icon"
                >
                <i :class="item.icon"></i>
              </el-radio>
            </el-radio-group>
          </el-form-item>

        <!-- 任务名称 -->
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="form.taskName" placeholder="请输入任务名称"></el-input>
        </el-form-item>

        <el-form-item style="width: 100%;" label="类型" prop="categoryId">
          <el-tree-select
              v-model="form.categoryId"
              :data="deptOptions"
              :props="{ value: 'id', label: 'label', children: 'children' }"
              value-key="id"
              placeholder="请选择归属类型"
              check-strictly
          />
        </el-form-item>

        <!-- 数据采集模板
        <el-form-item label="数据采集模板" prop="dataCollectionTemplate">
          <el-row>
            <el-col :span="7" v-for="(o, index) in loginStyleArr" :key="index" :offset="index > 0 ? 1 : 0">
              <el-card :body-style="{ padding: '0px !important' }"
                :class="currentLoginStyle == o.id ? 'select-card' : ''" shadow="never">
                <img :src="o.icon" class="image">
                <div style="padding: 14px; line-height: 1.4rem; padding: 8px;">
                  <span>{{ o.desc }}</span>
                  <div class="bottom clearfix">
                    <el-button @click="selectStyle(o)" type="text" class="button">选择</el-button>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </el-form-item>
        -->

        <el-form-item label="环境名称" prop="envId">
          <el-select v-model="form.envId" placeholder="选择执行环境" style="width:100%">
            <el-option v-for="item in envData" :key="item.id" :label="'(' + item.systemEnv + ')' + item.name"
              :value="item.id" :systemEnv="item.systemEnv" />
          </el-select>
        </el-form-item>

        <el-form-item label="变量">
          <el-button type="primary" bg text @click="centerDialogVisible = true">
            <i class="fa-solid fa-screwdriver-wrench"></i>&nbsp;配置全局变量
          </el-button>
        </el-form-item>

        <!-- CRON表达式 -->
        <el-form-item label="CRON表达式" prop="cronExpression">
          <el-input disabled="true" v-model="form.cronExpression" placeholder="请输入CRON表达式">
            <template #append>
              <el-button :icon="Search" @click="handleShowCron">生成CRON表达式</el-button>
            </template>
          </el-input>
        </el-form-item>

        <!-- 是否告警 -->
        <el-form-item label="起止时间" prop="startTime">
          <el-col :span="11">
            <el-date-picker 
              v-model="form.startTime" 
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              type="datetime" 
              placeholder="开始日期" 
              :shortcuts="shortcuts"
              style="width: 100%" />
          </el-col>
          <el-col :span="2" class="text-center">
            <span class="text-gray-500">-</span>
          </el-col>
          <el-col :span="11">
            <el-date-picker 
              type="datetime" 
              v-model="form.endTime" 
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              :shortcuts="shortcuts" 
              placeholder="结束时间"
              style="width: 100%" />
          </el-col>
        </el-form-item>

        <!-- 参与人监控邮箱 -->
        <!-- <el-form-item label="参与人监控邮箱" prop="monitorEmail">
          <el-input v-model="form.monitorEmail" placeholder="请输入参与人监控邮箱"></el-input>
        </el-form-item> -->

        <br />

        <el-form-item>
          <el-button icon="Right" type="primary" @click="createDatasource">
            下一步
          </el-button>
          <el-button icon="UploadFilled" type="danger" @click="saveDefinition">
            保存 
          </el-button>
          <el-button @click="resetForm">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-dialog title="Cron表达式生成器" v-model="openCron" append-to-body destroy-on-close class="scrollbar">
      <crontab @hide="openCron = false" @fill="crontabFill" :expression="expression"></crontab>
    </el-dialog>

    <el-dialog title="全局环境变量" v-model="centerDialogVisible" append-to-body destroy-on-close class="scrollbar">
      <ContextParam ref="contextParamRef" :context="form.globalParams" />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="centerDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="callContextParamRef()">
            确认
          </el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup name="createJob">

import ContextParam from "./params/contextParam.vue";
import Crontab from '@/components/Crontab'
import { getAllEnvironment } from '@/api/data/scheduler/environment'
import { getProcessDefinitionByDto , catalogTreeSelect } from '@/api/data/scheduler/processDefinition'
import { nextTick } from "vue";

const { proxy } = getCurrentInstance();
const route = useRoute()
const router = useRouter();

// const loginStyleArr = ref([
//   { id: '1', icon: 'http://data.linesno.com/icons/flow/style-04.png', desc: '数据分析处理,从数据库中读取数据进行解析和加载' },
//   { id: '2', icon: 'http://data.linesno.com/icons/flow/style-05.png', desc: '运维自动化任务,运维自动化管理和处理结果' },
//   { id: '3', icon: 'http://data.linesno.com/icons/flow/style-06.png', desc: '数据采集,Agent智能体数据资产分析及更新' }
// ]);

const icons = ref([
  { id: 1, icon: 'fa-solid fa-charging-station'} ,
  { id: 1, icon: 'fa-solid fa-truck'} ,
  { id: 2, icon: 'fa-solid fa-paper-plane'} ,
  { id: 2, icon: 'fa-solid fa-ship'} ,
  { id: 3, icon: 'fa-solid fa-chart-column'},
  { id: 4, icon: 'fa-solid fa-server'}, 
  { id: 5, icon: 'fa-solid fa-box-open'}, 
  { id: 8, icon: 'fa-solid fa-file-invoice-dollar'}, 
  { id: 9, icon: 'fa-solid fa-user-tie'},
]);

const currentLoginStyle = ref('0')
const deptOptions = ref(undefined);
const processDefinitionId = route.query.processDefinitionId
const envData = ref([])
const contextParamRef = ref(null)

// 是否显示Cron表达式弹出层
const openCron = ref(false)

// 是否显示弹窗
const centerDialogVisible = ref(false)

// 传入的表达式
const expression = ref("")

// 快捷选择
const shortcuts = ref([
  {
    text: '今天',
    value: new Date(),
  },
  {
    text: '昨天',
    value: () => {
      const date = new Date()
      date.setDate(date.getDate() - 1)
      return date
    },
  },
  {
    text: '周前',
    value: () => {
      const date = new Date()
      date.setDate(date.getDate() - 7)
      return date
    },
  },
])

const data = reactive({
  form: {
    id: 0,
    taskName: "", // 任务名称
    envId: "", // 环境ID
    globalParams: {} , // 上下文内容
    dataCollectionTemplate: "", // 数据采集模板
    dataQuality: "", // 数据质量
    isAlertEnabled: false, // 是否告警
    monitorEmail: "",  // 参与人监控邮箱
    cronExpression: "", // cron表达式
    startTime: "", // 开始时间
    endTime: "", // 结束时间
  },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    jobName: undefined,
    dbDesc: undefined
  },
  rules: {
    taskName: [
      { required: true, message: "请输入任务名称", trigger: "blur" }
    ],
    envId: [
      { required: true, message: "请选择环境ID", trigger: "blur" }
    ],
    icon: [
      { required: true, message: "请输入数据采集模板", trigger: "blur" }
    ],
    dataQuality: [
      { required: true, message: "请输入数据质量要求", trigger: "blur" }
    ],
    monitorEmail: [
      { required: true, message: "请输入参与人监控邮箱", trigger: "blur" },
      { type: "email", message: "请输入正确的邮箱地址", trigger: ["blur", "change"] }
    ],
    cronExpression: [
      { required: true, message: "请输入CRON时间表态式", trigger: "blur" }
    ],
    startTime: [
      { required: true, message: "请输入任务开始时间", trigger: "blur" }
    ],
    endTime: [
      { required: true, message: "请输入任务结束时间", trigger: "blur" }
    ]
  }
});
const { queryParams, form, rules } = toRefs(data);

/** 查询任务列表 */
function selectStyle(item) {
  currentLoginStyle.value = item.id;
  form.value.dataCollectionTemplate = item.id;
  console.log('item = ' + item.id);
}

/** 返回 */
function goBack() {
  router.push({ path: '/data/scheduler/processDefinition/index', query: {} });
}

/** 创建数据源 */
function createDatasource() {

  proxy.$refs["databaseRef"].validate(valid => {
    if (valid) {
      console.log('task form data = {}', form.value.startTime)

      // 将form数据转换为JSON字符串并存储到localStorage
      localStorage.setItem('processDefinitionFormData', JSON.stringify(form.value));

      let path = '/data/scheduler/processDefinition/createDefinition';
      router.push({ path: path , query:{'processDefinitionId': processDefinitionId } }).then(() => {
        window.location.reload(); 
      });
    }
  });

}

/** 保存更新流程 */
function saveDefinition(){
  proxy.$modal.msgSuccess("功能模块开发中.");
}

/** cron表达式按钮操作 */
function handleShowCron() {
  console.log('cron expression = ' + form.value.cronExpression);
  expression.value = form.value.cronExpression;
  openCron.value = true;
}

/** 获取到环境变量值  */
function callContextParamRef() {
  let contextParam = contextParamRef.value.getEnvVarsAsJson();
  form.value.globalParams = contextParam;
  centerDialogVisible.value = false;

  console.log(JSON.stringify(contextParam, null, 2));
}

/** 确定后回传值 */
function crontabFill(value) {
  form.value.cronExpression = value;
}

/** 定义一个方法来从localStorage中获取formData */
function loadFormDataFromStorage() {
  const formDataStr = localStorage.getItem('processDefinitionFormData');
  if (formDataStr) {
    try {
      const formData = JSON.parse(formDataStr);
      Object.assign(form.value, formData);

      currentLoginStyle.value = form.value.dataCollectionTemplate;

      console.log('Loaded form data from localStorage:', form.value);
    } catch (error) {
      console.error('Error parsing form data from localStorage:', error);
    }
  } else {
    console.log('No form data found in localStorage.');
  }
}

/** 重置表单 */
function resetForm() {
  form.value = {
    taskName: "", // 任务名称
    globalParams: {}, // 上下文内容
    dataCollectionTemplate: "", // 数据采集模板
    dataQuality: "", // 数据质量
    isAlertEnabled: false, // 是否告警
    monitorEmail: "", // 参与人监控邮箱
    cronExpression: "", // cron表达式
    startTime: "", // 开始时间
    endTime: "", // 结束时间
  };

  // 清除localStorage中的数据
  localStorage.removeItem('processDefinitionFormData');
}

/** 查询类型下拉树结构 */
function getDeptTree() {
  catalogTreeSelect().then(response => {
    deptOptions.value = response.data;
  });
};

// 调用此方法以加载数据
getAllEnvironment().then(res => {
  envData.value = res.data

  nextTick(() => {

    console.log('processDefinitionId = ' + processDefinitionId)
    if (processDefinitionId) {
      getProcessDefinitionByDto(processDefinitionId).then(res => {
        form.value = res.data;
        currentLoginStyle.value = form.value.dataCollectionTemplate;
      })
    } else {
      // loadFormDataFromStorage();
      // 清空本地缓存 
      localStorage.setItem('processDefinitionFormData', JSON.stringify(form.value));
    }
  })
})


getDeptTree();
</script>


<style scoped lang="scss">
.form-container {
  max-width: 960px;
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

.image {
  width: 100%;
  height: 120px;
}

.select-card {
  border: 1px solid rgb(0, 91, 212);
}
</style>