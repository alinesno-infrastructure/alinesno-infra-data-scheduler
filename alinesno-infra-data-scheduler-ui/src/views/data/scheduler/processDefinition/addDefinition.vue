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

        <!-- 异常策略 异常策略（2:忽略 1:结束） el-radio-group -->
         <el-form-item label="异常策略" prop="exceptionStrategy">
          <el-radio-group v-model="form.errorStrategy">
            <el-radio :label="item.code" v-for="item in ExceptionStrategyList" :key="item.code">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 任务描述 -->
         <el-form-item label="任务描述" prop="taskDesc">
          <el-input v-model="form.taskDesc" placeholder="请输入任务描述"></el-input>
        </el-form-item>

        <el-form-item style="width: 100%;" label="类型" prop="categoryId">
          <el-tree-select
              v-model="form.categoryId"
              :data="deptOptions"
              :props="{ value: 'id', label: 'label', children: 'children' }"
              value-key="id"
              placeholder="请选择归属类型"
              check-strictly
              @change="onCategoryChange"
          />
        </el-form-item>

        <el-form-item label="环境名称" prop="envId">
          <el-select v-model="form.envId" placeholder="选择执行环境" style="width:100%">
            <el-option v-for="item in envData" :key="item.id" :label="'(' + item.systemEnv + ')' + item.name"
              :value="item.id" :systemEnv="item.systemEnv" />
          </el-select>
        </el-form-item>

        <br />

        <el-form-item>
          <el-button icon="UploadFilled" type="primary" @click="saveDefinition">
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
import { 
  getProcessDefinitionByDto , 
  catalogTreeSelect , 
  saveProcessDefinition
} from '@/api/data/scheduler/processDefinition'
import { nextTick } from "vue";
import { ExceptionStrategyList } from "@/utils/workflow"

const { proxy } = getCurrentInstance();
const route = useRoute()
const router = useRouter();

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
    taskDesc: [
      { required: true, message: "请输入任务描述", trigger: "blur" }
    ],
    categoryId: [
      { required: true, message: "请选择任务分类", trigger: "blur" }
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
  console.log('form = ' + form.value)

  // 先做一下数据校验
  proxy.$refs["databaseRef"].validate(valid => {
    if (valid) {
      saveProcessDefinition(form.value).then(res => {
        console.log('res = > ' + res);
        proxy.$modal.msgSuccess("保存成功");
        goBack();
      })
    }
  })

}

function findNodeById(nodes, id) {
  if (!nodes) return null;
  for (const n of nodes) {
    if (n.id === id) return n;
    if (n.children) {
      const r = findNodeById(n.children, id);
      if (r) return r;
    }
  }
  return null;
}

function onCategoryChange(val) {
  const node = findNodeById(deptOptions.value, val);
  if (node && node.children && node.children.length) {
    proxy.$message.warning('只能选择叶子节点，请选择具体类型');
    // 撤销选择（根据需求可设为 null 或上一次有效值）
    form.value.categoryId = null;
  }
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

// // 调用此方法以加载数据
// getAllEnvironment().then(res => {
//   envData.value = res.data

//   nextTick(() => {

//     console.log('processDefinitionId = ' + processDefinitionId)
//     if (processDefinitionId) {
//       getProcessDefinitionByDto(processDefinitionId).then(res => {
//         form.value = res.data;
//         currentLoginStyle.value = form.value.dataCollectionTemplate;
//       })
//     } else {
//       // loadFormDataFromStorage();
//       // 清空本地缓存 
//       localStorage.setItem('processDefinitionFormData', JSON.stringify(form.value));
//     }
//   })
// })

// 调用此方法以加载数据
getAllEnvironment().then(res => {
  envData.value = res.data || [];

  // 如果没有可用环境，则添加一个默认环境项并默认选中
  if (!envData.value || envData.value.length === 0) {
    const defaultEnv = {
      id: 0,                    // 根据后端实际 id 类型可改为 '0' 或 -1
      name: '默认环境',
      systemEnv: 'default'
    };
    envData.value.push(defaultEnv);

    // 将表单 envId 设为默认环境（如果需要自动选中）
    form.value.envId = defaultEnv.id;
  }

  nextTick(() => {
    console.log('processDefinitionId = ' + processDefinitionId)
    if (processDefinitionId) {
      getProcessDefinitionByDto(processDefinitionId).then(res => {
        form.value = res.data;
        currentLoginStyle.value = form.value.dataCollectionTemplate;
      })
    } else {
      // 如果不希望自动选中默认环境，可以注释掉下一行
      localStorage.setItem('processDefinitionFormData', JSON.stringify(form.value));
    }
  })
})


getDeptTree();
</script>


<style scoped lang="scss">
.form-container {
  max-width: 900px;
  margin-left: auto;
  margin-right: auto;
  margin-top: 20px;
}

.label-title {
  text-align: center;
  max-width: 960px;
  margin-left: auto;
  margin-right: auto;
  margin-top: 10vh;

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