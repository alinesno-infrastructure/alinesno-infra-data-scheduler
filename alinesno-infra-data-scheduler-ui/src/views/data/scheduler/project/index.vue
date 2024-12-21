<template>
   <div class="app-container">

      <el-row :gutter="20">
         <!--应用数据-->
         <el-col :span="24" :xs="24">
            <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
               <el-form-item label="应用名称" prop="dbName">
                  <el-input v-model="queryParams.dbName" placeholder="请输入应用名称" clearable style="width: 240px"
                     @keyup.enter="handleQuery" />
               </el-form-item>
               <el-form-item label="应用名称" prop="dbName">
                  <el-input v-model="queryParams['condition[dbName|like]']" placeholder="请输入应用名称" clearable
                     style="width: 240px" @keyup.enter="handleQuery" />
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

            <el-table v-loading="loading" :data="ProjectList" @selection-change="handleSelectionChange">
               <el-table-column type="selection" width="50" align="center" />

               <el-table-column label="图标" align="center" width="60" key="icon">
                  <template #default="scope">
                     <div class="role-icon">
                        <img style="width:40px;height:40px;border-radius:50%;" :src="displayProjectIcon(scope.row)" />
                        <!-- <img v-else style="width:40px;height:40px;border-radius:50%;" 
                              :src="'http://data.linesno.com/icons/sepcialist/dataset_' + ((scope.$index + 1) % 10 + 5) + '.png'" /> -->
                     </div>
                  </template>
               </el-table-column>

               <!-- 业务字段-->
               <el-table-column label="项目名称" width="250" align="left" prop="projectName">
                  <template #default="scope">
                     <div>
                        {{ scope.row.projectName }}
                     </div>
                     <div style="font-size: 13px;color: #a5a5a5;cursor: pointer;" v-copyText="scope.row.projectCode">
                        调用码: {{ scope.row.projectCode }} <el-icon>
                           <CopyDocument />
                        </el-icon>
                     </div>
                  </template>
               </el-table-column>
               <el-table-column label="应用描述" align="left" key="projectDesc" prop="projectDesc"
                  v-if="columns[1].visible" />
               <el-table-column label="应用代码" align="center" width="200" key="projectCode" prop="projectCode"
                  v-if="columns[2].visible" :show-overflow-tooltip="true">
                  <template #default="scope">
                     <div style="font-size: 13px;color: #a5a5a5;cursor: pointer;" v-copyText="scope.row.projectCode">
                        {{ scope.row.projectCode }} <el-icon>
                           <CopyDocument />
                        </el-icon>
                     </div>
                  </template>
               </el-table-column>

               <el-table-column label="开启" align="center" width="100" key="hasStatus" prop="hasStatus"
                  v-if="columns[1].visible" :show-overflow-tooltip="true">
                  <template #default="scope">
                     <el-switch v-model="scope.row.hasStatus" :active-value="0" :inactive-value="1"
                        @change="handleChangStatusField('hasStatus', scope.row.hasStatus, scope.row.id)" />
                  </template>
               </el-table-column>

               <el-table-column label="添加时间" align="center" prop="addTime" v-if="columns[6].visible" width="160">
                  <template #default="scope">
                     <span>{{ parseTime(scope.row.addTime) }}</span>
                  </template>
               </el-table-column>

               <!-- 操作字段  -->
               <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
                  <template #default="scope">
                     <el-tooltip content="修改" placement="top" v-if="scope.row.ProjectId !== 1">
                        <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                           v-hasPermi="['system:Project:edit']"></el-button>
                     </el-tooltip>
                     <el-tooltip content="删除" placement="top" v-if="scope.row.ProjectId !== 1">
                        <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)"
                           v-hasPermi="['system:Project:remove']"></el-button>
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

         <el-form :model="form" :rules="rules" ref="databaseRef" label-width="100px">
            <el-row>
               <el-col :span="24" class="editor-after-div">
                  <el-form-item label="封面" prop="projectIcons">
                     <el-upload :file-list="imageUrl"
                        :action="upload.url + '?type=img&updateSupport=' + upload.updateSupport"
                        list-type="picture-card" :auto-upload="true" :on-success="handleAvatarSuccess"
                        :before-upload="beforeAvatarUpload" :headers="upload.headers" :disabled="upload.isUploading"
                        :on-progress="handleFileUploadProgress">
                        <el-icon class="avatar-uploader-icon">
                           <Plus />
                        </el-icon>
                     </el-upload>
                  </el-form-item>
               </el-col>
            </el-row>
            <el-row>
               <el-col :span="24">
                  <el-form-item label="应用名称" prop="projectName">
                     <el-input v-model="form.projectName" placeholder="请输入应用名称" maxlength="50" />
                  </el-form-item>
               </el-col>
            </el-row>
            <el-row>
               <el-col :span="24">
                  <el-form-item label="应用介绍" prop="projectDesc">
                     <el-input v-model="form.projectDesc" placeholder="请输入应用描述" maxlength="255" />
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


      <!-- 文档列表 -->
      <el-dialog :title="title" v-model="openDocumentTypeDialog" width="1024px" append-to-body>

         <TypeList />

         <template #footer>
            <div class="dialog-footer">
               <el-button type="primary" @click="submitDocumentTypeForm">确 定</el-button>
               <el-button @click="openDocumentTypeDialog = false">取 消</el-button>
            </div>
         </template>
      </el-dialog>

   </div>
</template>

<script setup name="Project">

import {getToken} from "@/utils/auth";

import {
   listProject,
   delProject,
   getProject,
   updateProject,
   addProject,
   changStatusField
} from "@/api/data/scheduler/project";

// import TypeList from './channelList.vue'

const router = useRouter();
const { proxy } = getCurrentInstance();

// 定义变量
const ProjectList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const dateRange = ref([]);
const imageUrl = ref([])

// 是否打开配置文档
const openDocumentTypeDialog = ref(false);

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
   form: {},
   queryParams: {
      pageNum: 1,
      pageSize: 10,
      projectName: undefined,
      projectDesc: undefined
   },
   rules: {
      projectName: [{ required: true, message: "名称不能为空", trigger: "blur" }],
      projectDesc: [{ required: true, message: "描述不能为空", trigger: "blur" }],
   }
});

const { queryParams, form, rules } = toRefs(data);

/*** 应用导入参数 */
const upload = reactive({
  // 是否显示弹出层（应用导入）
  open: false,
  // 弹出层标题（应用导入）
  title: "",
  // 是否禁用上传
  isUploading: false,
  // 是否更新已经存在的应用数据
  updateSupport: 0,
  // 设置上传的请求头部
  headers: {Authorization: "Bearer " + getToken()},
  // 上传的地址
  url: import.meta.env.VITE_APP_BASE_API + "/api/infra/data/scheduler/fileStore/importData", 
  // 显示地址
  display: import.meta.env.VITE_APP_BASE_API + "/api/infra/data/scheduler/fileStore/displayImage/" 
});

/** 查询应用列表 */
function getList() {
   loading.value = true;
   listProject(proxy.addDateRange(queryParams.value, dateRange.value)).then(res => {
      loading.value = false;
      ProjectList.value = res.rows;
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
   const ProjectIds = row.id || ids.value;
   proxy.$modal.confirm('是否确认删除应用编号为"' + ProjectIds + '"的数据项？').then(function () {
      return delProject(ProjectIds);
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
      projectIcons: undefined,
      id: undefined,
      deptId: undefined,
      ProjectName: undefined,
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

/** 显示图标 */
function displayProjectIcon(row){
   if(row.projectIcons){
      return import.meta.env.VITE_APP_BASE_API + "/api/infra/data/scheduler/fileStore/displayImage/" + row.projectIcons;
   }else {
      return 'http://data.linesno.com/icons/sepcialist/dataset_6.png' ; 
   }
}

/** 新增按钮操作 */
function handleAdd() {
   imageUrl.value = [];
   reset();
   open.value = true;
   title.value = "添加应用";
};

/** 修改按钮操作 */
function handleUpdate(row) {
   imageUrl.value = [];
   reset();
   const ProjectId = row.id || ids.value;
   getProject(ProjectId).then(response => {
      form.value = response.data;
      open.value = true;
      title.value = "修改应用";
   });
};

/** 查看项目告警空间 */
function handleProjectSpace(id) {
   let path = '/project/space/'
   router.push({ path: path + id });
}

/** 提交按钮 */
function submitForm() {
   proxy.$refs["databaseRef"].validate(valid => {
      if (valid) {
         if (form.value.id != undefined) {
            updateProject(form.value).then(response => {
               proxy.$modal.msgSuccess("修改成功");
               open.value = false;
               getList();
            });
         } else {
            addProject(form.value).then(response => {
               proxy.$modal.msgSuccess("新增成功");
               open.value = false;
               getList();
            });
         }
      }
   });
};


/** 图片上传成功 */
const handleAvatarSuccess = (response, uploadFile) => {
  // imageUrl.value = URL.createObjectURL(uploadFile.raw);
  imageUrl.value = response.data ? response.data.split(',').map(url =>{return { url:upload.display + url }}):[];
  form.value.projectIcons = response.data ;
  console.log('form.icon = ' + form.value.projectIcons);
};

/** 图片上传之前 */
const beforeAvatarUpload = (rawFile) => {
  if (rawFile.size / 1024 / 1024 > 2) {
    ElMessage.error('Avatar picture size can not exceed 2MB!');
    return false;
  }
  return true;
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