<template>
  <div>
     <el-row :gutter="20">
        <!--类型数据-->
        <!-- <el-col :span="4" :xs="24">
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
        </el-col> -->

        <!--指令数据-->
        <el-col :span="24" :xs="24">
           <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
              <el-form-item label="指令名称" prop="originalFilename">
                 <el-input v-model="queryParams.originalFilename" placeholder="请输入指令名称" clearable style="width: 240px" @keyup.enter="handleQuery" />
              </el-form-item>
              <el-form-item label="指令名称" prop="originalFilename">
                 <el-input v-model="queryParams['condition[originalFilename|like]']" placeholder="请输入指令名称" clearable style="width: 240px" @keyup.enter="handleQuery" />
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
                 <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete">删除</el-button>
              </el-col>

              <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
           </el-row>

           <el-table v-loading="loading" :data="ResourceList" @selection-change="handleSelectionChange">
              <el-table-column type="selection" width="50" :align="'center'" />

              <el-table-column label="图标" align="center" width="60" key="format" prop="format" v-if="columns[5].visible">
                 <template #default="scope">
                    <!-- <span style="font-size:25px;color:#3b5998">
                       <i :class="scope.row.icon" />
                    </span> -->
                    <img :src="'http://data.linesno.com/icons/fileType/' + scope.row.icon" style="width:35px;" />
                 </template>
              </el-table-column>

              <!-- 业务字段-->
              <el-table-column label="文件名" align="left" key="originalFilename" prop="originalFilename" v-if="columns[0].visible">
                 <template #default="scope">
                    <div>
                       {{ scope.row.originalFilename }}
                    </div>
                    <div style="font-size: 13px;color: #a5a5a5;cursor: pointer;" v-copyText="scope.row.id">
                       标识: {{ scope.row.id }} <el-icon><CopyDocument /></el-icon>
                    </div>
                 </template>
              </el-table-column>
              <el-table-column label="文件大小" align="center" width="100" key="size" prop="size" v-if="columns[2].visible" :show-overflow-tooltip="true">
                 <template #default="scope">
                    {{ formatFileSize(scope.row.size) }}
                 </template>
              </el-table-column>
              <el-table-column label="文件存储" align="center" width="150" key="platform" prop="platform" v-if="columns[2].visible" :show-overflow-tooltip="true">
                 <template #default="scope">
                    <el-button v-if="scope.row.platform === 'minio'" type="primary" text bg @click="configResource(scope.row)">
                     <i class="fa-solid fa-hard-drive" style="margin-right:5px;"></i> Minio存储
                   </el-button>
                    <el-button v-if="scope.row.platform === 'qiniu-kodo'" type="danger" text bg @click="configResource(scope.row)">
                     <i class="fa-solid fa-box-archive" style="margin-right:5px"></i> 七牛云存储 
                   </el-button>
                 </template>
              </el-table-column>
              <el-table-column label="存储类型" align="center" width="200" key="contentType" prop="contentType" v-if="columns[3].visible" :show-overflow-tooltip="true" />

              <el-table-column label="上传时间" align="center" prop="createTime" v-if="columns[6].visible" width="160">
                 <template #default="scope">
                    <span>{{ parseTime(scope.row.createTime) }}</span>
                 </template>
              </el-table-column>

              <!-- 操作字段  -->
              <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
                 <template #default="scope">
                    <el-tooltip content="下载" placement="top" v-if="scope.row.ResourceId !== 1">
                       <el-button link type="primary" icon="Download" @click="handleOpenLink(scope.row)"  v-hasPermi="['system:Resource:edit']"></el-button>
                    </el-tooltip>
                    <el-tooltip content="打开" placement="top" v-if="scope.row.ResourceId !== 1">
                       <el-button link type="primary" icon="Link" @click="handleOpenLink(scope.row)"  v-hasPermi="['system:Resource:edit']"></el-button>
                    </el-tooltip>
                    <el-tooltip content="删除" placement="top" v-if="scope.row.ResourceId !== 1">
                       <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:Resource:remove']"></el-button>
                    </el-tooltip>
                 </template>

              </el-table-column>
           </el-table>
           <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
        </el-col>
     </el-row>

     <!-- 添加或修改指令配置对话框 -->
     <el-dialog :title="promptTitle" v-model="promptOpen" width="1024" destroy-on-close append-to-body>

        <ResourceEditor :currentPostId="currentPostId" :currentResourceContent="currentResourceContent" />

     </el-dialog>

     <!-- 添加或修改指令配置对话框 -->
     <el-dialog :title="title" v-model="open" width="900px" append-to-body>
        <el-form :model="form" :rules="rules" ref="databaseRef" label-width="100px">
           <el-row>
              <el-col :span="24">
                 <el-form-item style="width: 100%;" label="类型" prop="promptType">
                    <el-tree-select
                       v-model="form.promptType"
                       :data="deptOptions"
                       :props="{ value: 'id', label: 'label', children: 'children' }"
                       value-key="id"
                       placeholder="请选择归属类型"
                       check-strictly
                    />
                 </el-form-item>
              </el-col>
           </el-row>
           <el-row>
              <el-col :span="24">
                 <el-form-item label="名称" prop="originalFilename">
                    <el-input v-model="form.originalFilename" placeholder="请输入指令名称" maxlength="50" />
                 </el-form-item>
              </el-col>
           </el-row>
           <el-row>
              <el-col :span="24">
                 <el-form-item label="数据来源" prop="dataSourceApi">
                    <el-input v-model="form.dataSourceApi" placeholder="请输入dataSourceApi数据来源" maxlength="128" />
                 </el-form-item>
              </el-col>
           </el-row>

           <el-row>
              <el-col :span="24">
                 <el-form-item label="备注" prop="promptDesc">
                    <el-input v-model="form.promptDesc" placeholder="请输入指令备注"></el-input>
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

<script setup name="Resource">

import {
  listResource,
  delResource,
  getResource,
  updateResource,
  catalogTreeSelect,
  addResource
} from "@/api/data/scheduler/resource";

// import ResourceEditor from "./editor.vue"

const router = useRouter();
const { proxy } = getCurrentInstance();

// 定义变量
const ResourceList = ref([]);
const open = ref(false);

const promptTitle = ref("");
const currentPostId = ref("");
const currentResourceContent = ref([]);
const promptOpen = ref(false);

const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const dateRange = ref([]);
const deptOptions = ref(undefined);
const postOptions = ref([]);
const roleOptions = ref([]);

// 列显隐信息
const columns = ref([
  { key: 0, label: `指令名称`, visible: true },
  { key: 1, label: `指令描述`, visible: true },
  { key: 2, label: `表数据量`, visible: true },
  { key: 3, label: `类型`, visible: true },
  { key: 4, label: `指令地址`, visible: true },
  { key: 5, label: `状态`, visible: true },
  { key: 6, label: `更新时间`, visible: true }
]);

const data = reactive({
  form: {},
  queryParams: {
     pageNum: 1,
     pageSize: 10,
     originalFilename: undefined,
     promptDesc: undefined,
     catalogId: undefined
  },
  rules: {
     originalFilename: [{ required: true, message: "名称不能为空", trigger: "blur" }] ,
     dataSourceApi: [{ required: true, message: "连接不能为空", trigger: "blur" }],
     promptType: [{ required: true, message: "类型不能为空", trigger: "blur" }] ,
     promptDesc: [{ required: true, message: "备注不能为空", trigger: "blur" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

function formatFileSize(bytes) {
   if (bytes === 0) return '0 B';
   const k = 1024;
   const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
   const i = Math.floor(Math.log(bytes) / Math.log(k));
   return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i];
}

/** 查询指令列表 */
function getList() {
  loading.value = true;
  listResource(proxy.addDateRange(queryParams.value, dateRange.value)).then(res => {
     loading.value = false;
     ResourceList.value = res.rows;
     total.value = res.total;
  });
};

// 节点单击事件
function handleNodeClick(data) {
  queryParams.value.catalogId = data.id;
  console.log('data.id = ' + data.id)
  getList();
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
};

/** 重置按钮操作 */
function resetQuery() {
  dateRange.value = [];
  proxy.resetForm("queryRef");

  queryParams.value.catalogId = undefined;

  proxy.$refs.deptTreeRef.setCurrentKey(null);
  handleQuery();
};
/** 删除按钮操作 */
function handleDelete(row) {
  const ResourceIds = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除指令编号为"' + ResourceIds + '"的数据项？').then(function () {
     return delResource(ResourceIds);
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

/** 查询类型下拉树结构 */
function getDeptTree() {
 catalogTreeSelect().then(response => {
   deptOptions.value = response.data;
 });
};

/** 配置Resource */
function configResource(row){
  promptTitle.value = "配置角色Resource";
  promptOpen.value = true ;
  currentPostId.value = row.id;

  if(row.contentType){
     currentResourceContent.value = JSON.parse(row.contentType);
  }
}

/** 重置操作表单 */
function reset() {
  form.value = {
     id: undefined,
     deptId: undefined,
     ResourceName: undefined,
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
  promptOpen.value = false ;
  reset();
};

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加指令";
};

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const ResourceId = row.id || ids.value;
  getResource(ResourceId).then(response => {
     form.value = response.data;
     open.value = true;
     title.value = "修改指令";
  });
};

/** 提交按钮 */
function submitForm() {
  proxy.$refs["databaseRef"].validate(valid => {
     if (valid) {
        if (form.value.id != undefined) {
           updateResource(form.value).then(response => {
              proxy.$modal.msgSuccess("修改成功");
              open.value = false;
              getList();
           });
        } else {
           addResource(form.value).then(response => {
              proxy.$modal.msgSuccess("新增成功");
              open.value = false;
              getList();
           });
        }
     }
  });
};

getDeptTree();
getList();

</script>
