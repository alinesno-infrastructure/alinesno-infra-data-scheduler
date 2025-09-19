<template>
   <div class="app-container">
      <el-row :gutter="20">
         <!--应用数据-->
         <el-col :span="24" :xs="24">
            <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
               <el-form-item label="描述" prop="readerName">
                  <el-input v-model="queryParams.readerName" placeholder="请输入描述" clearable style="width: 240px"
                     @keyup.enter="handleQuery" />
               </el-form-item>
               <el-form-item label="读取源名称" prop="readerName">
                  <el-input v-model="queryParams['condition[readerName|like]']" placeholder="请输入读取源名称" clearable
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

            <el-table v-loading="loading" :data="DatasourceList" @selection-change="handleSelectionChange">
               <el-table-column type="selection" width="50" align="center" />
               <el-table-column label="图标" align="left" width="50" key="status" v-if="columns[5].visible">
                  <template #default="scope">
                     <div>
                        <img style="width:35px; height:35px"
                           :src="getDatasourceIconPath(scope.row.readerType)" />
                     </div>
                  </template>
               </el-table-column>

               <!-- 业务字段-->
               <el-table-column label="读取源" align="left" prop="readerDesc" :show-overflow-tooltip="true">
                  <template #default="scope">
                     <div>
                        <el-popover placement="top-start" :title="scope.row.readerDesc" :width="500" trigger="hover"
                           :content="scope.row.readerUrl + '【' + scope.row.readerUsername + '】'">
                           <template #reference>
                              {{ scope.row.readerDesc  }}
                           </template>
                        </el-popover>
                     </div>
                     <div style="font-size: 13px;color: #a5a5a5;cursor: pointer;" v-copyText="scope.row.promptId">
                        {{ scope.row.readerName }}
                     </div>
                  </template>
               </el-table-column>

               <el-table-column label="源类型" align="center" width="150" prop="operationType"
                  :show-overflow-tooltip="true">
                  <template #default="scope">
                     <el-button v-if="scope.row.operationType == 'source'" type="primary" bg text>
                        <i class="fa-solid fa-truck"></i>&nbsp;读取
                     </el-button>
                     <el-button v-if="scope.row.operationType == 'sink'" type="danger" bg text>
                        <i class="fa-solid fa-feather"></i>&nbsp;写入
                     </el-button>
                  </template>
               </el-table-column>

               <!-- <el-table-column label="作者" align="center" width="150" prop="owner" :show-overflow-tooltip="true" /> -->
               <el-table-column label="类型" align="center" width="150" prop="readerType" :show-overflow-tooltip="true" />

               <el-table-column label="状态" align="center" width="120" prop="hasStatus">
                  <template #default="scope">
                     <div>
                        <el-progress v-if="scope.row.hasStatus === 0" :text-inside="false" :stroke-width="20"
                           status="success" :percentage="100"></el-progress>
                        <el-progress v-if="scope.row.hasStatus === 1" :text-inside="false" :stroke-width="20"
                           status="exception" :percentage="100"></el-progress>
                     </div>
                  </template>
               </el-table-column>

               <!-- 操作字段  -->
               <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
                  <template #default="scope">
                     <el-tooltip content="连接验证" placement="top" v-if="scope.row.id !== 1">
                        <el-button link type="primary" icon="Refresh" @click="handleCheckConnect(scope.row)"
                           v-hasPermi="['system:Datasource:edit']"></el-button>
                     </el-tooltip>
                     <el-tooltip content="修改" placement="top" v-if="scope.row.id !== 1">
                        <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                           v-hasPermi="['system:Datasource:edit']"></el-button>
                     </el-tooltip>
                     <el-tooltip content="删除" placement="top" v-if="scope.row.id !== 1">
                        <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)"
                           v-hasPermi="['system:Datasource:remove']"></el-button>
                     </el-tooltip>
                  </template>

               </el-table-column>
            </el-table>
            <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
               v-model:limit="queryParams.pageSize" @pagination="getList" />
         </el-col>
      </el-row>

      <!-- 添加或修改应用配置对话框 -->
      <el-dialog :title="title" v-model="open" width="980px" append-to-body>

         <el-form ref="databaseRef" :model="form" :rules="rules" label-width="100px">

            <el-form-item label="源类型" prop="readerType">
               <el-radio-group v-model="form.readerType">
                  <el-radio key="reader" label="reader" value="reader" size="large">读取(reader)</el-radio>
                  <el-radio key="sink" label="sink" value="sink" size="large">写入(sink)</el-radio>
               </el-radio-group>
            </el-form-item>

            <!-- 数据采集模板 -->
            <el-form-item label="数据采集源" prop="readerSource" v-if="form.readerType == 'reader'">
               <el-radio-group v-model="form.readerSource">
                  <el-radio v-for="item in readerSource" class="database-type" :key="item.id" :label="item.id"
                     :value="item.id" size="large">
                     <div style="float: left;">
                        <img style="width:30px; height:30px"
                           :src="'http://data.linesno.com/icons/database/' + item.readerType + '.png'" />
                     </div>
                     <div style="float: left;margin-left: 10px;line-height: 1.2rem;">
                        {{ item.readerName }}
                        <div style="font-size: 11px;">
                           {{ item.readerDesc }}
                        </div>
                     </div>
                  </el-radio>
               </el-radio-group>

            </el-form-item>

            <el-form-item label="数据写入源" prop="sinkSource" v-if="form.readerType == 'sink'">
               <el-radio-group v-model="form.sinkSource">
                  <el-radio v-for="item in sinkSource" class="database-type" :key="item.id" :label="item.id"
                     :value="item.id" size="large">
                     <div style="float: left;">
                        <img style="width:30px; height:30px"
                           :src="getDatasourceIconPath(item.readerType)" />
                     </div>
                     <div style="float: left;margin-left: 10px;line-height: 1.2rem;">
                        {{ item.readerName }}
                        <div style="font-size: 11px;">
                           {{ item.readerDesc }}
                        </div>
                     </div>
                  </el-radio>
               </el-radio-group>

            </el-form-item>


            <el-form-item label="描述" prop="readerDesc">
               <el-input v-model="form.readerDesc" placeholder="读取源描述" />
            </el-form-item>

            <el-form-item label="连接" prop="readerUrl">
               <el-input v-model="form.readerUrl"
                  placeholder="jdbc:mysql://localhost:3306/readerName?useUnicode=true&characterEncoding=utf8&characterSetResults=utf8&useSSL=false&serverTimezone=GMT" />
            </el-form-item>

            <el-form-item label="用户名" prop="readerUsername">
               <el-input v-model="form.readerUsername" auto-complete="new-password" placeholder="读取源用户名" />
            </el-form-item>

            <el-form-item label="密码" prop="readerPasswd">
               <el-input type="password" auto-complete="new-password" v-model="form.readerPasswd" placeholder="读取源密码" />
            </el-form-item>

         </el-form>
         <template #footer>
            <div class="dialog-footer">
               <!-- <el-button type="primary" @click="submitForm">确 定</el-button>
               <el-button @click="cancel">取 消</el-button> -->
               <el-button type="text" @click="validateDburl">请先点击验证读取源是否连通</el-button>
               <el-button type="primary" @click="submitForm" :disabled="!btnChangeEnable">确 定</el-button>
               <el-button @click="cancel">取 消</el-button>
            </div>
         </template>
      </el-dialog>

   </div>
</template>

<script setup name="Datasource">

import { getDatasourceIconPath } from '@/utils/llmIcons';
import {
   listDatasource,
   delDatasource,
   getDatasource,
   checkDbConfig,
   checkConnection,
   updateDatasource,
   addDatasource
} from "@/api/data/scheduler/datasources";

// import {
//   getAllSourceReader,
// } from "@/api/data/pipeline/jobBuilder";

import { ElLoading } from 'element-plus'

const router = useRouter();
const { proxy } = getCurrentInstance();

// 定义变量
const DatasourceList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const readerDescs = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const dateRange = ref([]);
const btnChangeEnable = ref(true)

const readerSource = ref([])  // 读取源
const sinkSource = ref([])  // 数据目的

const postOptions = ref([]);
const roleOptions = ref([]);

// 列显隐信息
const columns = ref([
   { key: 0, label: `应用名称`, visible: true },
   { key: 1, label: `应用描述`, visible: true },
   { key: 2, label: `表数据量`, visible: true },
   { key: 3, label: `类型`, visible: true },
   { key: 4, label: `应用地址`, visible: true },
   { key: 5, label: `状态`, visible: true },
   { key: 6, label: `更新时间`, visible: true }
]);

const data = reactive({
   form: {},
   queryParams: {
      pageNum: 1,
      pageSize: 10,
      readerName: undefined,
      readerDesc: undefined
   },
   rules: {
      readerName: [{ required: true, message: "名称不能为空", trigger: "blur" }],
      readerUrl: [{ required: true, message: "连接不能为空", trigger: "blur" }],
      readerType: [{ required: true, message: "类型不能为空", trigger: "blur" }],
      readerUsername: [{ required: true, message: "用户名不能为空", trigger: "blur" }],
      readerPasswd: [{ required: true, message: "密码不能为空", trigger: "blur" }],
      readerDesc: [{ required: true, message: "备注不能为空", trigger: "blur" }]
   }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询应用列表 */
function getList() {
   loading.value = true;
   listDatasource(proxy.addDateRange(queryParams.value, dateRange.value)).then(res => {
      loading.value = false;
      DatasourceList.value = res.rows;
      total.value = res.total;
   });
};

/** 获取到所有数据源 */
function handleGetAllSourceReader() {
   getAllSourceReader().then(response => {
      const data = response.data;

      readerSource.value = data.filter(item => item.sourceType === 'sink');
      sinkSource.value = data.filter(item => item.sourceType === 'source');

      readerSource.value = data;
      sinkSource.value = data;

      console.log('sinkSource = ' + sinkSource.value)
   });
}

/** 搜索按钮操作 */
function handleQuery() {
   queryParams.value.pageNum = 1;
   getList();
}

/** 连接验证是否正常 */
function handleCheckConnect(row) {
   const loading = ElLoading.service({
      lock: true,
      text: row.readerDesc + '连接验证中',
      background: 'rgba(0, 0, 0, 0.7)',
   })

   checkConnection(row.id).then(response => {
      loading.close()
      proxy.$modal.msgSuccess("读取源校验成功");
      getList();
   }).catch(error => {
      loading.close()
      getList();
   })

}

/** 重置按钮操作 */
function resetQuery() {
   dateRange.value = [];
   proxy.resetForm("queryRef");
   queryParams.value.deptId = undefined;
   proxy.$refs.deptTreeRef.setCurrentKey(null);
   handleQuery();
}

/** 删除按钮操作 */
function handleDelete(row) {
   const readerIds = row.id || ids.value;
   const readerDescTmp = row.id || readerDescs.value;
   proxy.$modal.confirm('是否确认删除应用编号为"' + readerDescTmp + '"的数据项？').then(function () {
      return delDatasource(readerIds);
   }).then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
   }).catch(() => { });
};

/** 选择条数  */
function handleSelectionChange(selection) {
   ids.value = selection.map(item => item.id);
   readerDescs.value = selection.map(item => item.readerDesc);
   single.value = selection.length != 1;
   multiple.value = !selection.length;
};

/** 重置操作表单 */
function reset() {
   form.value = {
      id: undefined,
      deptId: undefined,
      DatasourceName: undefined,
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
   // open.value = true;
   // title.value = "添加数据源";
   let path = '/data/scheduler/datasource/addSource';
   router.push({ path: path });
};

/** 修改按钮操作 */
function handleUpdate(row) {
   reset();
   const id = row.id || ids.value;

   let path = '/data/scheduler/datasource/addSource';
   router.push({ path: path, query: { id: id } });
};

/** 提交按钮 */
function validateDburl() {
   proxy.$refs["databaseRef"].validate(valid => {
      if (valid) {
         if (!form.readerType) {
            form.value.readerType = 'MySQL';
         }

         checkDbConfig(form.value).then(resp => {
            if (resp.data.accepted) {
               proxy.$modal.msgSuccess("读取源校验成功");
               btnChangeEnable.value = true;
            } else {
               proxy.$modal.msgSuccess(resp.msg);
            }
         })

      }
   });
};

/** 提交按钮 */
function submitForm() {
   proxy.$refs["databaseRef"].validate(valid => {
      if (valid) {
         if (form.value.id != undefined) {
            updateDatasource(form.value).then(response => {
               proxy.$modal.msgSuccess("修改成功");
               open.value = false;
               getList();
            });
         } else {
            addDatasource(form.value).then(response => {
               proxy.$modal.msgSuccess("新增成功");
               open.value = false;
               getList();
            });
         }
      }
   });
};

getList();
// handleGetAllSourceReader();

</script>
