<template>
  <div>
      <div style="
    padding: 10px;
    font-weight: bold;
    margin-left: 10px;
    color: #333;
    font-size: 17px;
    margin-bottom: 18px;">
    <i class="fa-solid fa-truck"></i>
    {{ currentProcess?.name }} 运行实例列表
      </div>

     <el-row :gutter="20">
        <!--实例数据-->
        <el-col :span="24" :xs="24">
           <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
              <el-form-item label="实例名称" prop="dbName">
                 <el-input v-model="queryParams.dbName" placeholder="请输入实例名称" clearable style="width: 240px" @keyup.enter="handleQuery" />
              </el-form-item>
              <el-form-item>
                 <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
              </el-form-item>
           </el-form>

           <el-table v-loading="loading" :data="ProcessInstanceList" @selection-change="handleSelectionChange">
              <el-table-column type="index" width="40" align="center" />

              <!-- 业务字段-->
              <el-table-column label="任务名称" align="left" key="projectName" prop="projectName" v-if="columns[0].visible" :show-overflow-tooltip="true">
                 <template #default="scope">
                     <div>
                        {{ scope.row.name}}#{{ scope.row.runTimes }}
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

              <el-table-column label="用时" align="center" width="120" key="documentType" prop="documentType" v-if="columns[1].visible" :show-overflow-tooltip="true" >
                 <template #default="scope">
                        <span>{{ getTimeDifference(scope.row.executeTime , scope.row.finishTime).seconds }} 秒</span>
                 </template>
              </el-table-column>

              <el-table-column label="运行时间" align="center" width="280" key="hasStatus" prop="hasStatus" v-if="columns[1].visible" :show-overflow-tooltip="true" >
                 <template #default="scope">
                     <div>
                        <div>开始:  {{ parseTime(scope.row.executeTime) }}</div>
                        <div>结束: {{ parseTime(scope.row.finishTime) }}</div>
                     </div>
                 </template>
              </el-table-column>

           </el-table>
           <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
        </el-col>
     </el-row>


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
import { nextTick } from "vue";

// import TypeList from './channelList.vue'

// const props = defineProps({
//    processDefinitionId: {
//     type: String,
//     required: true,
//     default: ''
//   }
// });

const router = useRouter();
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
const currentProcess = ref(null);

// 是否打开配置文档
const openDocumentTypeDialog = ref(false);

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
     dbName: undefined,
     processId: undefined , 
     dbDesc: undefined
  },
  rules: {
     dbName: [{ required: true, message: "名称不能为空", trigger: "blur" }] , 
     jdbcUrl: [{ required: true, message: "连接不能为空", trigger: "blur" }],
     dbType: [{ required: true, message: "类型不能为空", trigger: "blur" }] , 
     dbUsername: [{ required: true , message: "用户名不能为空", trigger: "blur"}],
     dbPasswd: [{ required: true, message: "密码不能为空", trigger: "blur" }] , 
     dbDesc: [{ required: true, message: "备注不能为空", trigger: "blur" }] 
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询实例列表 */
function getList(process) {
  loading.value = true;
  currentProcess.value = process;

  queryParams.value.processDefinitionId = process.id;

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
function handleProcessInstanceSpace(id){
  let path =  '/project/space/'
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
function handleConfigType(id , documentType){
  openDocumentTypeDialog.value = true ; 
}

/** 修改状态 */
const handleChangStatusField = async(field , value , id) => {
   // 判断tags值 这样就不会进页面时调用了
     const res = await changStatusField({
        field: field,
        value: value?1:0,
        id: id
     }).catch(() => { })
     if (res && res.code == 200) {
        // 刷新表格
        getList()
     }
}

/** 提交配置文档类型 */
function submitDocumentTypeForm(){
  // TODO 待保存实例文档类型
}


/** 计算两个时间间隔 */
function getTimeDifference(startTime, endTime) {
   // 解析ISO 8601格式的时间字符串为Date对象
   const start = new Date(startTime);
   const end = new Date(endTime);

   // 计算时间差（以毫秒为单位）
   const diffInMilliseconds = end - start;

   // 可选：转换成更易读的格式，比如秒或分钟
   const diffInSeconds = diffInMilliseconds / 1000;
   const diffInMinutes = diffInSeconds / 60;

   // 返回所需的时间差单位，这里我们返回毫秒和秒两种形式
   return {
      milliseconds: diffInMilliseconds,
      seconds: diffInSeconds
   };
}

// getList();

defineExpose({
   getList
})

</script>
