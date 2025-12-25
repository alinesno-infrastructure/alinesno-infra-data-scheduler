<template>
   <div>

      <el-row :gutter="20">
         <!--类型数据-->
           <el-col :span="8" :xs="24">
              <div class="head-container">
                 <el-input
                    v-model="deptName"
                    placeholder="请输入类型名称"
                    clearable
                    prefix-icon="Search"
                    style="margin-bottom: 20px"
                 />
              </div>
              <div class="head-container">
                 <el-tree
                    :data="deptOptions"
                    :props="{ label: 'label', children: 'children' }"
                    :expand-on-click-node="false"
                    :filter-node-method="filterNode"
                    ref="deptTreeRef"
                    node-key="id"
                    highlight-current
                    default-expand-all
                    @node-click="handleNodeClick"
                 />
              </div>
           </el-col> 

         <!--任务数据-->
         <el-col :span="16" :xs="24">
            <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
               <el-form-item label="任务名称" prop="name">
                  <el-input v-model="queryParams.name" placeholder="请输入任务名称" clearable style="width: 240px"
                     @keyup.enter="handleQuery" />
               </el-form-item>
               <el-form-item>
                  <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
               </el-form-item>
            </el-form>


            <el-table v-loading="loading" :data="ProcessDefinitionList" @selection-change="handleSelectionChange">
               <el-table-column type="index" label="序号" width="50" align="center" />

               <el-table-column align="center" width="40" key="icon">
                  <template #default="scope">
                     <div style="font-size:20px;color:#015bd4">
                        <i :class="scope.row.icon"></i>
                     </div>
                  </template>
               </el-table-column>

               <!-- 业务字段-->
               <el-table-column label="任务名称" align="left" key="name" prop="name" v-if="columns[0].visible">
                  <template #default="scope">
                     <!--
                     <div @click="openProcessInstance(scope.row)" style="cursor: pointer;">
                     </div>
                     -->
                     <router-link :to="{ path: '/data/scheduler/processInstance/index', query: { processDefinitionId: scope.row.id , fromWhere: 'process' } }">
                        {{ scope.row.name }}({{ scope.row.runCount }}/{{ scope.row.successCount }})
                     </router-link>
                     <div style="font-size: 13px;color: #a5a5a5;cursor: pointer;" class="text-overflow"
                        v-copyText="scope.row.promptId">
                        {{ scope.row.description?scope.row.description:'当前任务没有描述'}}
                     </div>
                  </template>
               </el-table-column>

               <!--
               <el-table-column label="运行次数" align="center" key="jobDesc" prop="jobDesc" v-if="columns[1].visible">
                  <template #default="scope">
                     <div style="margin-top: 5px;">
                        <router-link :to="{ path: '/data/scheduler/processInstance/index', query: { processDefinitionId: scope.row.id , fromWhere: 'process' } }">
                           <el-button type="primary" text> 
                              <i class="fa-solid fa-truck-fast" style="margin-right:5px;"></i>
                              次数: {{ scope.row.runCount }}({{ scope.row.successCount }}) 条 
                           </el-button>
                        </router-link>
                     </div>
                  </template>
               </el-table-column>
               -->

            </el-table>
            <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
               v-model:limit="queryParams.pageSize" @pagination="getList" />
         </el-col>
      </el-row>

   </div>
</template>

<script setup name="ProcessDefinition">

import {
   listProcessDefinition,
   delProcessDefinition,
   getProcessDefinition,
   updateProcessDefinition,
   addProcessDefinition,
   runOneTime,
   resumeTrigger,
   catalogTreeSelect,
   pauseTrigger,
   changStatusField
} from "@/api/data/scheduler/processDefinition";

import CronButton from './components/CronButton.vue'
import ListInstance from '@/views/data/scheduler/processInstance/listInstance.vue'
import { nextTick } from "vue";

const router = useRouter();
const { proxy } = getCurrentInstance();

// 定义变量
const ProcessDefinitionList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const dateRange = ref([]);
const openCron = ref(false);
const expression = ref("");
const deptOptions = ref(undefined);

// 定时任务表达式
const cronExpression = ref('') // 初始为空，表示未设置

// 是否打开配置文档
const processDefinitionRef = ref(null);
const openInstanceDialog = ref(false);
const processDefinitionTitle = ref('')

// 列显隐信息
const columns = ref([
   { key: 0, label: `任务名称`, visible: true },
   { key: 1, label: `任务描述`, visible: true },
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
      name: undefined,
      dbDesc: undefined
   },
   rules: {
      name: [{ required: true, message: "名称不能为空", trigger: "blur" }],
      jdbcUrl: [{ required: true, message: "连接不能为空", trigger: "blur" }],
      dbType: [{ required: true, message: "类型不能为空", trigger: "blur" }],
      dbUsername: [{ required: true, message: "用户名不能为空", trigger: "blur" }],
      dbPasswd: [{ required: true, message: "密码不能为空", trigger: "blur" }],
      dbDesc: [{ required: true, message: "备注不能为空", trigger: "blur" }]
   }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询任务列表 */
function getList() {
   loading.value = true;
   listProcessDefinition(proxy.addDateRange(queryParams.value, dateRange.value)).then(res => {
      loading.value = false;
      ProcessDefinitionList.value = res.rows;
      total.value = res.total;
   });
};

/** 搜索按钮操作 */
function handleQuery() {
   queryParams.value.pageNum = 1;
   getList();
};

/** 查询类型下拉树结构 */
function getDeptTree() {
   catalogTreeSelect().then(response => {
      deptOptions.value = response.data;
   });
};

// 节点单击事件
function handleNodeClick(data) {
  queryParams.value.categoryId = data.id;
  console.log('data.id = ' + data.id)
  getList();
}

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
   const ProcessDefinitionIds = row.id || ids.value;
   proxy.$modal.confirm('是否确认删除任务编号为"' + ProcessDefinitionIds + '"的数据项？').then(function () {
      return delProcessDefinition(ProcessDefinitionIds);
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
      ProcessDefinitionName: undefined,
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
   //   open.value = true;
   //   title.value = "添加任务";
   let path = '/data/scheduler/processDefinition/addDefinition';
   router.push({ path: path });
};

/** 修改按钮操作 */
function handleUpdate(row) {
   reset();

   let path = '/data/scheduler/processDefinition/addDefinition';
   router.push({ path: path , query: { 'processDefinitionId' : row.id} });
   
   // getProcessDefinition(ProcessDefinitionId).then(response => {
   //    form.value = response.data;
   //    open.value = true;
   //    title.value = "修改任务";
   // });

};

/** 查看项目告警空间 */
// function handleProcessDefinitionSpace(id) {
//    let path = '/project/space/'
//    router.push({ path: path + id });
// }

/** 提交按钮 */
function submitForm() {
   proxy.$refs["databaseRef"].validate(valid => {
      if (valid) {
         if (form.value.ProcessDefinitionId != undefined) {
            updateProcessDefinition(form.value).then(response => {
               proxy.$modal.msgSuccess("修改成功");
               open.value = false;
               getList();
            });
         } else {
            addProcessDefinition(form.value).then(response => {
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
   let path = '/data/scheduler/processDefinition/createDefinition' ;
   router.push({ path: path , 
      query:{
         processDefinitionId:id,
         node:'node',
         from: 'list'
      } }).then(() => {
      // window.location.reload();
   }) ;
   
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

/* 立即执行一次 */
function handleRunOneTime(row) {
   console.log('row = ' + row);
   runOneTime(row.id).then(res => {
      console.info("res = " + res);
      proxy.$modal.msgSuccess("执行成功");
   });
}

/** 任务详细信息 */
function handleView(row) {
   getProcessDefinition(row.id).then(response => {
      // this.form = response.data;
      // this.openView = true;
      console.log(response)
   });
}

/** 暂停按钮操作 */
function handleResumeTrigger(row) {
   resumeTrigger(row.id).then(response => {
      proxy.$modal.msgSuccess("执行成功");
   });
}

/** 恢复按钮操作 */
function handlePauseTrigger(row) {
   pauseTrigger(row.id).then(response => {
      proxy.$modal.msgSuccess("执行成功");
   });
}

/** 打开任务实例界面 */
function openProcessInstance(row) {
   // openInstanceDialog.value = true
   // processDefinitionTitle.value = row.name
   // // processDefinitionId.value = row.id
   // nextTick(() => {
   //    processDefinitionRef.value.getList(row.id);
   // })
   console.log('row.id = ' + row.id)
}

/** cron表达式按钮操作 */
// function handleShowCron() {
//    expression.value = this.form.cronExpression;
//    openCron.value = true;
// }

/** 任务日志列表查询 */
function handleProcessDefinitionLog(row) {
   const jobId = row.id || 0;
   router.push('/monitor/job-log/index/' + jobId)
}

/** 提交配置文档类型 */
function submitDocumentTypeForm() {
   // TODO 待保存任务文档类型
}

/** 下拉事件 */
function handleCommand(command, row) {
   switch (command) {
      case "handleRunOneTime":
         handleRunOneTime(row);
         break;
      case "handlePauseTrigger":
         handlePauseTrigger(row);
         break;
      case "handleResumeTrigger":
         handleResumeTrigger(row);
         break;
      case "handleView":
         handleView(row);
         break;
      case "handleProcessDefinitionLog":
         handleProcessDefinitionLog(row);
         break;
      default:
         break;
   }
}

getDeptTree();
getList();

</script>