<template>
   <div class="app-container">

      <el-row :gutter="20">
         <!--应用数据-->
         <el-col :span="24" :xs="24">
            <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
               <el-form-item label="密钥名称" prop="dbName">
                  <el-input v-model="queryParams.dbName" placeholder="请输入密钥名称" clearable style="width: 240px"
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

            <el-table v-loading="loading" :data="SecretsList" @selection-change="handleSelectionChange">
               <el-table-column type="selection" width="50" align="center" />

               <!-- 业务字段-->
               <el-table-column label="密钥标识" align="left" key="secName" prop="secName"
                  v-if="columns[0].visible">
                  <template #default="scope">
                     <div>
                        {{ scope.row.secName }} 
                        <span style="font-size: 13px;color: #a5a5a5;cursor: pointer;" v-copyText="'${{secrets.' + scope.row.secName + '}}'">
                           <i class="fa-solid fa-copy"></i>
                        </span>
                     </div>
                  </template>
               </el-table-column>

               <el-table-column label="描述" align="left" key="secDesc" prop="secDesc" v-if="columns[1].visible"
                  :show-overflow-tooltip="true" />

               <!-- 
               <el-table-column label="范围" align="center" width="100" key="secScope" prop="secScope"
                  v-if="columns[1].visible" :show-overflow-tooltip="true">
                  <template #default="scope">
                     <span v-if="scope.row.secScope === 'public'">全局</span>
                     <span v-if="scope.row.secScope === 'project'">项目</span>
                  </template>
               </el-table-column> 
               -->

               <el-table-column label="更新时间" align="center" prop="updateTime" v-if="columns[6].visible" width="160">
                  <template #default="scope">
                     <span>{{ parseTime(scope.row.updateTime) }}</span>
                  </template>
               </el-table-column>

               <!-- 操作字段  -->
               <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
                  <template #default="scope">
                     <el-tooltip content="修改" placement="top" v-if="scope.row.SecretsId !== 1">
                        <el-button link type="primary" @click="handleUpdate(scope.row)" v-hasPermi="['system:Secrets:edit']">
                           <i class="fa-solid fa-pen-nib"></i>
                        </el-button>
                     </el-tooltip> 
                     <el-tooltip content="删除" placement="top" v-if="scope.row.SecretsId !== 1">
                        <el-button type="danger" link @click="handleDelete(scope.row)" v-hasPermi="['system:Secrets:remove']">
                           <i class="fa-solid fa-trash"></i>
                        </el-button>
                     </el-tooltip>
                  </template>

               </el-table-column>
            </el-table>
            <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
               v-model:limit="queryParams.pageSize" @pagination="getList" />
         </el-col>
      </el-row>

      <!-- 添加或修改应用配置对话框 -->
      <el-dialog :title="title" v-model="open" width="900px" append-to-body>
         <el-form :model="form" :rules="rules" ref="databaseRef" label-width="100px" size="large">
            <el-row>
               <el-col :span="24">
                  <el-form-item label="密钥标识" prop="secName">
                     <el-input v-model="form.secName" placeholder="请输入密钥标识" maxlength="50" />
                  </el-form-item>
               </el-col>
            </el-row>
            <el-row>
               <el-col :span="24">
                  <el-form-item label="描述" prop="secDesc">
                     <el-input v-model="form.secDesc" placeholder="请输入描述信息"></el-input>
                  </el-form-item>
               </el-col>
            </el-row>
            <!-- 
            <el-col :span="24">
               <el-form-item label="范围" prop="secScope">
                  <el-radio-group v-model="form.secScope">
                     <el-radio v-for="dict in secScopeOptions" :key="dict.value" :value="dict.value"
                        :label="dict.value">{{ dict.label }}</el-radio>
                  </el-radio-group>
               </el-form-item>
            </el-col> 
            -->
            <el-row v-if="form.id == null">
               <el-col :span="24">
                  <el-form-item label="密钥值" prop="secValue">
                     <el-input v-model="form.secValue" type="textarea" rows="5" resize="none" placeholder="请输入密钥值" maxlength="255" />
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

<script setup name="Secrets">

import {
   listSecrets,
   delSecrets,
   getSecrets,
   updateSecrets,
   addSecrets,
   changStatusField
} from "@/api/data/scheduler/secrets";

// import TypeList from './channelList.vue'

const router = useRouter();
const { proxy } = getCurrentInstance();

// 定义变量
const SecretsList = ref([]);
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

const secScopeOptions = ref([
   { label: "全部", value: "public" },
   { label: "项目", value: "project" },
]);

// 列显隐信息
const columns = ref([
   { key: 0, label: `应用名称`, visible: true },
   { key: 1, label: `应用描述`, visible: true },
   { key: 2, label: `授权地址`, visible: true },
   { key: 3, label: `类型`, visible: true },
   { key: 4, label: `是否公开`, visible: true },
   { key: 5, label: `状态`, visible: true },
   { key: 6, label: `添加时间`, visible: true }
]);

const data = reactive({
   form: {
      secScope: 'public'
   },
   queryParams: {
      pageNum: 1,
      pageSize: 10,
      dbName: undefined,
      dbDesc: undefined
   },
   rules: {
      // 针对描述字段的验证
      secDesc: [{ required: true, message: "描述不能为空", trigger: "blur" }],
      // 针对范围字段的验证
      secScope: [{ required: true, message: "请选择范围", trigger: "change" }],
      // 针对密钥标识字段的验证
      secName: [
         { required: true, message: "密钥标识不能为空", trigger: "blur" },
         { max: 50, message: "长度不能超过50个字符", trigger: "blur" }
      ],
      // 针对密钥值字段的验证
      secValue: [
         { required: true, message: "密钥值不能为空", trigger: "blur" },
         { max: 255, message: "长度不能超过255个字符", trigger: "blur" }
      ]
   }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询应用列表 */
function getList() {
   loading.value = true;
   listSecrets(proxy.addDateRange(queryParams.value, dateRange.value)).then(res => {
      loading.value = false;
      SecretsList.value = res.rows;
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
   const SecretsIds = row.id || ids.value;
   proxy.$modal.confirm('是否确认删除应用编号为"' + SecretsIds + '"的数据项？').then(function () {
      return delSecrets(SecretsIds);
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
      SecretsName: undefined,
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
   title.value = "添加密钥";
};

/** 修改按钮操作 */
function handleUpdate(row) {
   reset();
   const SecretsId = row.id || ids.value;
   getSecrets(SecretsId).then(response => {
      form.value = response.data;
      open.value = true;
      title.value = "修改应用";
   });
};

/** 查看项目告警空间 */
function handleSecretsSpace(id) {
   let path = '/project/space/'
   router.push({ path: path + id });
}

/** 提交按钮 */
function submitForm() {
   proxy.$refs["databaseRef"].validate(valid => {
      if (valid) {
         if (form.value.id!= undefined) {
            updateSecrets(form.value).then(response => {
               proxy.$modal.msgSuccess("修改成功");
               open.value = false;
               getList();
            });
         } else {
            addSecrets(form.value).then(response => {
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
   // TODO 待保存应用文档类型
}

getList();

</script>