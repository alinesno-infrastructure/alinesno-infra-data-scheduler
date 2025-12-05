<template>
   <div class="app-container">

      <!-- 返回标识 -->
      <el-page-header v-if="fromWhere" @back="goBack" :content="headerContent" style="margin-bottom:10px;">
         <template #content>
            <span style="font-size:15px;">
               {{ headerContent }}
            </span>
         </template>
      </el-page-header>

      <el-row :gutter="20">

         <!--实例数据-->
         <el-col :span="10" :xs="24">
            <LeftProcessDefinitionPanel />
         </el-col>

         <!--实例数据-->
         <el-col :span="14" :xs="24">
            <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
               <el-form-item label="实例名称" prop="dbName">
                  <el-input v-model="queryParams.dbName" placeholder="请输入实例名称" clearable style="width: 240px"
                     @keyup.enter="handleQuery" />
               </el-form-item>
               <el-form-item>
                  <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
                  <el-button icon="Refresh" @click="resetQuery">重置</el-button>
                  <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete">删除</el-button>
               </el-form-item>
            </el-form>

            <el-table v-loading="loading" :data="ProcessInstanceList" @selection-change="handleSelectionChange">
               <el-table-column type="selection" width="50" align="center" />

               <el-table-column label="图标" align="center" key="projectName" prop="projectName" width="50" v-if="columns[0].visible"
                  :show-overflow-tooltip="true">
                  <template #default="scope">
                     <i class="fa-solid fa-sun" style="font-size:17px;margin-right:5px"></i>
                  </template>
               </el-table-column>

               <!-- 业务字段-->
               <el-table-column label="任务名称" align="left" key="projectName" prop="projectName" v-if="columns[0].visible"
                  :show-overflow-tooltip="true">
                  <template #default="scope">
                     <div>
                        {{ scope.row.name }}#{{ scope.row.runTimes }}
                     </div>
                     <div style="font-size: 13px;color: #a5a5a5;cursor: pointer;"
                        v-copyText="scope.row.promptId">
                        耗时: <span>{{ getTimeDifference(scope.row.executeTime , scope.row.finishTime).formatted }}</span>
                     </div>
                  </template>
               </el-table-column>

               <el-table-column label="流程任务" width="100" align="center" key="jobDesc" prop="jobDesc"
                  v-if="columns[1].visible">
                  <template #default="scope">
                     <div style="margin-top: 2px;margin-bottom: 2px;">
                        <el-button type="primary" text @click="handleProcessTaskInstance(scope.row)">
                           <i class="fa-solid fa-truck-fast" style="margin-right:5px"></i> 流程
                        </el-button>
                     </div>
                  </template>
               </el-table-column>

               <el-table-column label="日志" width="100" align="center" key="jobDesc" prop="jobDesc"
                  v-if="columns[1].visible">
                  <template #default="scope">
                     <div style="margin-top: 2px;margin-bottom: 2px;">
                        <el-button type="primary" text @click="handleProcessInstanceLog(scope.row)">
                           <i class="fa-solid fa-ferry" style="margin-right:5px"></i> 日志
                        </el-button>
                     </div>
                  </template>
               </el-table-column>

               <el-table-column label="运行状态" align="center" width="140" key="projectCode" prop="projectCode"
                  v-if="columns[2].visible" :show-overflow-tooltip="true">
                  <template #default="scope">
                     <el-button type="primary" v-if="scope.row.executionStatus == 'executing'" text loading>
                        <i class="fa-solid fa-file-signature"></i> 
                        {{ scope.row.executionStatusLabel }}
                     </el-button>
                     <el-button type="danger" v-if="scope.row.executionStatus == 'error'" text>
                        <i class="fa-solid fa-file-circle-xmark"></i> &nbsp; 
                        {{ scope.row.executionStatusLabel }}
                     </el-button>
                     <el-button type="success" v-if="scope.row.executionStatus == 'completed'" text>
                        <i class="fa-solid fa-file-shield"></i> &nbsp; 
                        {{ scope.row.executionStatusLabel }}
                     </el-button>
                  </template>
               </el-table-column>

               <el-table-column label="起止时间" align="center" width="200" key="documentType" prop="documentType"
                  v-if="columns[1].visible" :show-overflow-tooltip="true">
                  <template #default="scope">
                     <div>
                        <div>开始:  {{ parseTime(scope.row.executeTime) }}</div>
                        <div>结束: {{ parseTime(scope.row.finishTime) }}</div>
                     </div>
                  </template>
               </el-table-column> 

               <!-- 操作字段  -->
               <el-table-column label="操作" align="center" width="100" class-name="small-padding fixed-width">
                  <template #default="scope">
                     <el-tooltip content="修改" placement="top" v-if="scope.row.ProcessInstanceId !== 1">
                        <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                           v-hasPermi="['system:ProcessInstance:edit']"></el-button>
                     </el-tooltip>
                     <el-tooltip content="删除" placement="top" v-if="scope.row.ProcessInstanceId !== 1">
                        <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)"
                           v-hasPermi="['system:ProcessInstance:remove']"></el-button>
                     </el-tooltip>
                  </template>

               </el-table-column>
            </el-table>
            <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
               v-model:limit="queryParams.pageSize" @pagination="getList" />
         </el-col>
      </el-row>

      <!-- 试运行窗口 -->
      <div class="aip-flow-drawer">
         <el-drawer 
               v-model="showDebugRunDialog" 
               :modal="false" 
               size="50%" 
               style="max-width: 800px;position: absolute;" 
               :title="executeInstanceTitle"
               :with-header="true">
               <div style="margin-top: 0px;">
                  <DebuggerProcessFlowPanel ref="debuggerProcessFlowPanelRef" />
               </div>
         </el-drawer>
      </div>

      <!-- 任务实例日志 -->
      <el-drawer v-model="openProcessInstanceLogDialog" class="process-instance-log-drawer" :with-header="false"
         :size="'60%'" :title="openProcessInstanceLogTitle" :before-close="handleCloseProcessInstanceLogDialog"
         :direction="'rtl'">
         <ProcessInstanceLog ref="processInstanceLogRef" />
      </el-drawer>

   </div>
</template>

<script setup name="ProcessInstance">

import {
   listProcessInstance,
   delProcessInstance,
   getProcessInstance,
   updateProcessInstance,
   addProcessInstance,
   changStatusField
} from "@/api/data/scheduler/processInstance";

import LeftProcessDefinitionPanel from "@/views/data/scheduler/processDefinition/leftPanel.vue"
import DebuggerProcessFlowPanel from './instanceProcessFlowPanel.vue'
import ListInstance from '@/views/data/scheduler/taskInstance/listInstance.vue'
import ProcessInstanceLog from './processInstanceLog.vue'
import { onMounted, ref, watch } from "vue";
import { getParam } from "@/utils/ruoyi";

const router = useRouter();
const route = useRoute();
const { proxy } = getCurrentInstance();

// 定义变量
const ProcessInstanceList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const dateRange = ref([]);

const fromWhere = ref(route.query.fromWhere);
const processDefinitionId = computed(() => route.query.processDefinitionId);

const headerContent = ref('实例运行列表');

// 流程运行状态实例弹
const debuggerProcessFlowPanelRef = ref(null);
const showDebugRunDialog = ref(false);

// 是否打开配置文档
// const processTaskDefinitionRef = ref(null);
// const openTaskInstanceDialog = ref(false);
// const processDefinitionTitle = ref('')

const executeInstanceTitle = ref('');

const processInstanceLogRef = ref(null)
const openProcessInstanceLogDialog = ref(false);
const openProcessInstanceLogTitle = ref('');

// 列显隐信息
const columns = ref([
   { key: 0, label: `实例名称`, visible: true },
   { key: 1, label: `实例描述`, visible: true },
   { key: 2, label: `授权地址`, visible: true },
   { key: 3, label: `类型`, visible: true },
   { key: 4, label: `是否公开`, visible: true },
   { key: 5, label: `状态`, visible: true },
   { key: 6, label: `添加时间`, visible: true }
]);

const data = reactive({
   form: {},
   queryParams: {
      pageNum: 1,
      pageSize: 10,
      processId: null
   },
   rules: {
      dbName: [{ required: true, message: "名称不能为空", trigger: "blur" }],
      jdbcUrl: [{ required: true, message: "连接不能为空", trigger: "blur" }],
      dbType: [{ required: true, message: "类型不能为空", trigger: "blur" }],
      dbUsername: [{ required: true, message: "用户名不能为空", trigger: "blur" }],
      dbPasswd: [{ required: true, message: "密码不能为空", trigger: "blur" }],
      dbDesc: [{ required: true, message: "备注不能为空", trigger: "blur" }]
   }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询实例列表 */
function getList() {
   loading.value = true;
   listProcessInstance(proxy.addDateRange(queryParams.value, dateRange.value)).then(res => {
      loading.value = false;
      ProcessInstanceList.value = res.rows;
      total.value = res.total;
   });
};

/** 搜索按钮操作 */
function handleQuery() {
   queryParams.value.pageNum = 1;
   getList();
};

/** 重置按钮操作 */
function resetQuery() {
   dateRange.value = [];
   proxy.resetForm("queryRef");
   queryParams.value.deptId = undefined;
   proxy.$refs.deptTreeRef.setCurrentKey(null);
   handleQuery();
};
/** 删除按钮操作 */
function handleDelete(row) {
   const ProcessInstanceIds = row.id || ids.value;
   proxy.$modal.confirm('是否确认删除实例编号为"' + ProcessInstanceIds + '"的数据项？').then(function () {
      return delProcessInstance(ProcessInstanceIds);
   }).then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
   }).catch(() => { });
};

/** 选择条数  */
function handleSelectionChange(selection) {
   ids.value = selection.map(item => item.id);
   single.value = selection.length != 1;
   multiple.value = !selection.length;
};

/** 重置操作表单 */
function reset() {
   form.value = {
      id: undefined,
      deptId: undefined,
      ProcessInstanceName: undefined,
      nickName: undefined,
      password: undefined,
      phonenumber: undefined,
      status: "0",
      remark: undefined,
   };
   proxy.resetForm("databaseRef");
};
/** 取消按钮 */
function cancel() {
   open.value = false;
   reset();
};

/** 新增按钮操作 */
function handleAdd() {
   reset();
   open.value = true;
   title.value = "添加实例";
};

/** 修改按钮操作 */
function handleUpdate(row) {
   reset();
   const ProcessInstanceId = row.id || ids.value;
   getProcessInstance(ProcessInstanceId).then(response => {
      form.value = response.data;
      open.value = true;
      title.value = "修改实例";
   });
};

/** 查看项目告警空间 */
function handleProcessInstanceSpace(id) {
   let path = '/project/space/'
   router.push({ path: path + id });
}

/** 提交按钮 */
function submitForm() {
   proxy.$refs["databaseRef"].validate(valid => {
      if (valid) {
         if (form.value.ProcessInstanceId != undefined) {
            updateProcessInstance(form.value).then(response => {
               proxy.$modal.msgSuccess("修改成功");
               open.value = false;
               getList();
            });
         } else {
            addProcessInstance(form.value).then(response => {
               proxy.$modal.msgSuccess("新增成功");
               open.value = false;
               getList();
            });
         }
      }
   });
};

/** 配置文档类型 */
function handleConfigType(id, documentType) {
   openDocumentTypeDialog.value = true;
}

/** 打开任务实例界面 */
function handleProcessTaskInstance(row) {
   // openTaskInstanceDialog.value = true
   // processDefinitionTitle.value = row.name
   // nextTick(() => {
   //    processTaskDefinitionRef.value.getList(row.id);
   // })

   executeInstanceTitle.value = row.name + '#' + row.runTimes ;

   showDebugRunDialog.value = true;
   nextTick(() => {
      debuggerProcessFlowPanelRef.value.getList(row.processDefinitionId , row.id);
   })
}

/** 打开任务实例日志界面 */
function handleProcessInstanceLog(row) {
   openProcessInstanceLogDialog.value = true
   openProcessInstanceLogTitle.value = row.name
   nextTick(() => {
      processInstanceLogRef.value.connectLogger(row.id);
   })
}

/** 关闭任务实例日志界面 */
function handleCloseProcessInstanceLogDialog() {
   openProcessInstanceLogDialog.value = false;
   processInstanceLogRef.value.clearLoggerInterval();
}

/** 修改状态 */
const handleChangStatusField = async (field, value, id) => {
   // 判断tags值 这样就不会进页面时调用了
   const res = await changStatusField({
      field: field,
      value: value ? 1 : 0,
      id: id
   }).catch(() => { })
   if (res && res.code == 200) {
      // 刷新表格
      getList()
   }
}

/** 提交配置文档类型 */
// function submitDocumentTypeForm(){
//   // TODO 待保存实例文档类型
// }


/** 计算两个时间间隔 */
function getTimeDifference(startTime, endTime, options = {}) {
  const { showDays = true } = options;
  const start = new Date(startTime);
  const end = new Date(endTime);

  if (isNaN(start.getTime()) || isNaN(end.getTime())) {
    throw new Error('无效的时间格式，请使用可解析的时间字符串（如 ISO 8601）');
  }

  const rawMs = end - start;             // 可能为负
  const negative = rawMs < 0;
  let ms = Math.abs(rawMs);

  const totalSeconds = Math.floor(ms / 1000);
  const seconds = totalSeconds % 60;
  const totalMinutes = Math.floor(totalSeconds / 60);
  const minutes = totalMinutes % 60;
  let hours = Math.floor(totalMinutes / 60);
  let days = 0;

  if (showDays) {
    days = Math.floor(hours / 24);
    hours = hours % 24;
  }

  // 构建可读字符串，省略为 0 的单位
  const parts = [];
  if (showDays && days > 0) parts.push(days + '天');
  if (hours > 0) parts.push(hours + '时');
  if (minutes > 0) parts.push(minutes + '分');
  if (seconds > 0) parts.push(seconds + '秒');

  // 若所有单位都为 0，则显示 "0秒"
  if (parts.length === 0) parts.push('0秒');

  const formatted = (negative ? '-' : '') + parts.join('');

  return {
    milliseconds: rawMs,            // 带符号
    totalSeconds: rawMs / 1000,    // 带符号（可为小数）
    days: negative ? -days : days,
    hours: negative ? -hours : hours,
    minutes: negative ? -minutes : minutes,
    seconds: negative ? -seconds : seconds,
    formatted: formatted
  };
}

/**
 * 返回上一步
 */
function goBack() {
   if (fromWhere.value === 'process') {
      router.push({ path: '/data/scheduler/processDefinition/index' })
   } else if (fromWhere.value === 'dashboard') {
      router.push({ path: '/index' })
   } else {
      router.go(-1);
   }
}

const trialRun = () => {
    console.log('试运行操作');
    showDebugRunDialog.value = true;
};

watch(showDebugRunDialog, async (visible) => {
  // 等待子组件挂载并渲染
  await nextTick()
  const comp = debuggerProcessFlowPanelRef.value
  if (!comp) return
  if (visible) {
    // 打开抽屉时开始轮询
    comp.startPolling && comp.startPolling()
  } else {
    // 关闭抽屉时停止轮询
    comp.stopPolling && comp.stopPolling()
  }
})

// 修改监听方式，直接监听路由变化
watch(
  () => route.query.processDefinitionId,
  async (newVal, oldVal) => {
    if (newVal && newVal !== oldVal) { // 确保值变化且不为空
      queryParams.value.processDefinitionId = newVal;
      queryParams.value.pageNum = 1; // 重置页码
      getList();
    }
  },
  { immediate: true } // 初始加载时执行一次
);

/**
 * 页面加载时
 */
onMounted(() => {
  // 简化初始化逻辑，由watch的immediate选项处理
  getList();
});

</script>

<style>
.flow-control-panel .el-card__body {
    padding: 13px !important
}

.aip-flow-drawer .el-drawer.ltr,
.aip-flow-drawer .el-drawer.rtl {
    height: 93%;
    bottom: 10px;
    top: auto;
    right: 10px;
    border-radius: 8px;
}

.aip-flow-drawer .el-drawer__header {
    margin-bottom: 0px;
}
</style>