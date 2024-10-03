<template>
   <div class="app-container">

      <el-row :gutter="20">
         <!--环境数据-->
         <el-col :span="24" :xs="24">
            <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
               <el-form-item label="环境名称" prop="dbName">
                  <el-input v-model="queryParams.dbName" placeholder="请输入环境名称" clearable style="width: 240px"
                     @keyup.enter="handleQuery" />
               </el-form-item>
               <el-form-item>
                  <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
                  <el-button icon="Refresh" @click="resetQuery">重置</el-button>
               </el-form-item>
            </el-form>

            <el-row :gutter="10" class="mb8">

               <el-col :span="1.5">
                  <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
               </el-col>
               <el-col :span="1.5">
                  <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate">修改</el-button>
               </el-col>
               <el-col :span="1.5">
                  <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete">删除</el-button>
               </el-col>

               <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
            </el-row>

            <el-table v-loading="loading" :data="EnvironmentList" @selection-change="handleSelectionChange">
               <el-table-column type="selection" width="50" align="center" />

               <!-- 业务字段-->
               <el-table-column label="环境名称" align="left" width="250" key="name" prop="name" v-if="columns[0].visible">
                  <template #default="scope">
                     {{ scope.row.name }}
                  </template>
               </el-table-column>
               <el-table-column label="环境描述" align="left" key="description" prop="description"
                  v-if="columns[1].visible" />
  
                <el-table-column label="默认" align="center" width="100" key="hasStatus" prop="hasStatus" v-if="columns[1].visible" :show-overflow-tooltip="true" >
                   <template #default="scope">
                      <el-switch
                         v-model="scope.row.defaultEnv"
                         :active-value="true"
                         :inactive-value="false"
                         @change="handleChangDefaultEnv(scope.row.id)"
                      />
                   </template>
                </el-table-column>
               <el-table-column label="环境代码" align="center" width="200" key="systemEvn" prop="systemEvn"
                  v-if="columns[2].visible" :show-overflow-tooltip="true">
                  <template #default="scope">
                     <el-button type="primary" bg text v-if="scope.row.systemEnv === 'windows'"> 
                        <i class="fa-brands fa-windows"></i> &nbsp;{{ scope.row.systemEnv}}
                     </el-button>
                     <el-button type="primary" bg text v-if="scope.row.systemEnv === 'linux'"> 
                        <i class="fa-brands fa-linux"></i> &nbsp;{{ scope.row.systemEnv}}
                     </el-button>
                     <el-button type="primary" bg text v-if="scope.row.systemEnv === 'macos'"> 
                        <i class="fa-solid fa-laptop"></i> &nbsp;{{ scope.row.systemEnv}}
                     </el-button>
                  </template>
               </el-table-column>

               <el-table-column label="范围" align="center" width="100" key="credentialScope" prop="credentialScope"
                  v-if="columns[1].visible" :show-overflow-tooltip="true">
                  <template #default="scope">
                     <span v-if="scope.row.credentialScope === 'public'">全局</span>
                     <span v-if="scope.row.credentialScope === 'project'">项目</span>
                  </template>
               </el-table-column>

               <el-table-column label="更新时间" align="center" prop="addTime" v-if="columns[6].visible" width="160">
                  <template #default="scope">
                     <span>{{ parseTime(scope.row.updateTime) }}</span>
                  </template>
               </el-table-column>

               <!-- 操作字段  -->
               <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
                  <template #default="scope">
                     <el-tooltip content="修改" placement="top" v-if="scope.row.EnvironmentId !== 1">
                        <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                           v-hasPermi="['system:Environment:edit']"></el-button>
                     </el-tooltip>
                     <el-tooltip content="删除" placement="top" v-if="scope.row.EnvironmentId !== 1">
                        <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)"
                           v-hasPermi="['system:Environment:remove']"></el-button>
                     </el-tooltip>
                  </template>

               </el-table-column>
            </el-table>
            <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
               v-model:limit="queryParams.pageSize" @pagination="getList" />
         </el-col>
      </el-row>

      <!-- 添加或修改环境配置对话框 -->
      <el-dialog :title="title" v-model="open" width="900px" append-to-body>
         <el-form :model="form" :rules="rules" ref="databaseRef" label-width="100px">
            <el-col :span="24">
               <el-form-item label="系统环境" prop="systemEnv">
                  <el-radio-group v-model="form.systemEnv">
                     <el-radio v-for="dict in systemEnvTypeOptions" :key="dict.value" :value="dict.value"
                        :label="dict.value">{{ dict.label }}</el-radio>
                  </el-radio-group>
               </el-form-item>
            </el-col>
            <el-row>
               <el-col :span="24">
                  <el-form-item label="环境名称" prop="name">
                     <el-input v-model="form.name" placeholder="请输入环境名称" maxlength="50" />
                  </el-form-item>
               </el-col>
            </el-row>
            <el-col :span="24">
               <el-form-item label="范围" prop="credentialScope">
                  <el-radio-group v-model="form.credentialScope">
                     <el-radio v-for="dict in envScopeOptions" :key="dict.value" :value="dict.value"
                        :label="dict.value">{{
         dict.label }}</el-radio>
                  </el-radio-group>
               </el-form-item>
            </el-col>
            <el-row>
               <el-col :span="24">
                  <el-form-item label="环境描述" prop="description">
                     <el-input v-model="form.description" placeholder="请输入环境介绍" maxlength="255" />
                  </el-form-item>
               </el-col>
            </el-row>
            <el-row>
               <el-col :span="24">
                  <el-form-item label="环境参数" prop="config">
                     <el-input v-model="form.config" type="textarea" resize="none" rows="10" placeholder="请输入环境介绍" />
                  </el-form-item>
               </el-col>
            </el-row>
         </el-form>
         <template #footer>
            <div class="dialog-footer">
               <el-button type="primary" @click="submitForm">确 定</el-button>
               <el-button @click="cancel">取 消</el-button>
            </div>
         </template>
      </el-dialog>

   </div>
</template>

<script setup name="Environment">

import {
   listEnvironment,
   delEnvironment,
   getEnvironment,
   updateEnvironment,
   addEnvironment,
   defaultEnv,
   changStatusField
} from "@/api/data/scheduler/environment";

// import TypeList from './channelList.vue'

const router = useRouter();
const { proxy } = getCurrentInstance();

// 定义变量
const EnvironmentList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const dateRange = ref([]);

// 是否打开配置文档
const openDocumentTypeDialog = ref(false);

// 列显隐信息
const columns = ref([
   { key: 0, label: `环境名称`, visible: true },
   { key: 1, label: `环境描述`, visible: true },
   { key: 2, label: `授权地址`, visible: true },
   { key: 3, label: `类型`, visible: true },
   { key: 4, label: `是否公开`, visible: true },
   { key: 5, label: `状态`, visible: true },
   { key: 6, label: `添加时间`, visible: true }
]);

// 系统环境类型
const systemEnvTypeOptions = ref([
   { label: "Windows", value: "windows" },
   { label: "Linux", value: "linux" },
   { label: "MacOS", value: "macos" }
])

const envScopeOptions = ref([
   { label: "全部", value: "public" },
   { label: "项目", value: "project" },
]);

const data = reactive({
   form: {},
   queryParams: {
      pageNum: 1,
      pageSize: 10,
      dbName: undefined,
      dbDesc: undefined
   },
   rules: {
    systemEnv: [{ required: true, message: "请选择系统环境", trigger: "change" }],
    name: [{ required: true, message: "请输入环境名称", trigger: "blur" }, { max: 50, message: "环境名称不能超过50个字符", trigger: "blur" }],
    credentialScope: [{ required: true, message: "请选择范围", trigger: "change" }],
    description: [{ required: true, message: "请输入环境描述", trigger: "blur" }, { max: 255, message: "环境描述不能超过255个字符", trigger: "blur" }],
    config: [{ required: true, message: "请输入环境参数", trigger: "blur" }] 
   }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询环境列表 */
function getList() {
   loading.value = true;
   listEnvironment(proxy.addDateRange(queryParams.value, dateRange.value)).then(res => {
      loading.value = false;
      EnvironmentList.value = res.rows;
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
   const EnvironmentIds = row.id || ids.value;
   proxy.$modal.confirm('是否确认删除环境编号为"' + EnvironmentIds + '"的数据项？').then(function () {
      return delEnvironment(EnvironmentIds);
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
      EnvironmentName: undefined,
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
   title.value = "添加环境";
};

/** 修改按钮操作 */
function handleUpdate(row) {
   reset();
   const EnvironmentId = row.id || ids.value;
   getEnvironment(EnvironmentId).then(response => {
      form.value = response.data;
      open.value = true;
      title.value = "修改环境";
   });
};

/** 修改默认环境 */
function handleChangDefaultEnv(id) {
   defaultEnv(id).then(response => {
      proxy.$modal.msgSuccess("修改成功");
      getList();
   });
}

/** 查看项目告警空间 */
function handleEnvironmentSpace(id) {
   let path = '/project/space/'
   router.push({ path: path + id });
}

/** 提交按钮 */
function submitForm() {
   proxy.$refs["databaseRef"].validate(valid => {
      if (valid) {
         if (form.value.id != undefined) {
            updateEnvironment(form.value).then(response => {
               proxy.$modal.msgSuccess("修改成功");
               open.value = false;
               getList();
            });
         } else {
            addEnvironment(form.value).then(response => {
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
function submitDocumentTypeForm() {
   // TODO 待保存环境文档类型
}

getList();

</script>