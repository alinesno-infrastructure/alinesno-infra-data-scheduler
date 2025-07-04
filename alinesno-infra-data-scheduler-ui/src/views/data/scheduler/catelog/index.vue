<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="分类名称" prop="name">
        <el-input
            v-model="queryParams.name"
            placeholder="请输入分类名称"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="hasStatus">
        <el-select v-model="queryParams.hasStatus" placeholder="分类状态" clearable>
          <el-option
              v-for="dict in sys_normal_disable"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="Plus"
            @click="handleAdd"
            v-hasPermi="['system:dept:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="info"
            plain
            icon="Sort"
            @click="toggleExpandAll"
        >展开/折叠</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
        v-if="refreshTable"
        v-loading="loading"
        :data="deptList"
        row-key="id"
        :default-expand-all="isExpandAll"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }">

      <el-table-column prop="name" label="分类名称">
        <template #default="scope">
          {{ scope.row.name }}
        </template>
      </el-table-column>
      <el-table-column prop="description" label="类型描述" ></el-table-column>
      <el-table-column align="center" prop="orderNum" label="排序" width="100"></el-table-column>
      <el-table-column align="center" prop="level" label="数据级别" width="100">
        <template #default="scope">
          L2级别
        </template>
      </el-table-column>
      <el-table-column align="center" prop="hasStatus" label="状态" width="100">
        <template #default="scope">
          <dict-tag :options="sys_normal_disable" :value="scope.row.hasStatus" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="200">
        <template #default="scope">
          <span>{{ parseTime(scope.row.addTime ) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
              type="text"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['system:dept:edit']"
          >修改</el-button>
          <el-button
              type="text"
              icon="Plus"
              @click="handleAdd(scope.row)"
              v-hasPermi="['system:dept:add']"
          >新增</el-button>
          <el-button
              v-if="scope.row.parentId != 0"
              type="text"
              icon="Delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['system:dept:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加或修改分类对话框 -->
    <el-dialog :title="title" v-model="open" width="800px" append-to-body>
      <el-form ref="deptRef" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="上级分类" prop="parentId">
              <el-tree-select
                  v-model="form.parentId"
                  :data="deptOptions"
                  :props="{ value: 'id', label: 'name', children: 'children' }"
                  value-key="id"
                  placeholder="选择上级分类"
                  check-strictly
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
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
          </el-col>
          <el-col :span="24">
            <el-form-item label="分类名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入分类名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="显示排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" controls-position="right" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
             <el-form-item label="安全级别" prop="confidentialityLevel">
              <el-radio-group v-model="form.confidentialityLevel">
                <el-radio v-for="level in dataSecurityLevels" :key="level.key" :label="level.key">
                  {{ level.value }} - {{ level.desc }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="类型描述" prop="description">
              <el-input v-model="form.description" placeholder="请输入分类描述" />
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

<script setup name="Catalog">
import { listCatalog, getCatalog, delCatalog, addCatalog, updateCatalog, listCatalogExcludeChild } from "@/api/data/scheduler/catalog";

const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict("sys_normal_disable");

const deptList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const title = ref("");
const deptOptions = ref([]);
const isExpandAll = ref(true);
const refreshTable = ref(true);

const data = reactive({
  form: {},
  queryParams: {
    name: undefined,
    hasStatus: undefined
  },
  rules: {
    parentId: [{ required: true, message: "上级分类不能为空", trigger: "blur" }],
    name: [{ required: true, message: "分类名称不能为空", trigger: "blur" }],
    description: [{ required: true, message: "分类描述不能为空", trigger: "blur" }],
    confidentialityLevel: [{ required: true, message: "安全级别不能为空", trigger: "blur" }],
    orderNum: [{ required: true, message: "显示排序不能为空", trigger: "blur" }],
    email: [{ type: "email", message: "请输入正确的邮箱地址", trigger: ["blur", "change"] }],
    phone: [{ pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: "请输入正确的手机号码", trigger: "blur" }]
  },
});

const { queryParams, form, rules } = toRefs(data);

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

// 定义数据安全级别常量
const dataSecurityLevels = ref([
  { key: 1, value: 'Public', desc: '公开数据，无敏感信息，对数据丢失或泄露的影响较小' },
  { key: 2, value: 'Internal', desc: '仅限公司内部使用，对数据丢失或泄露有一定影响' },
  { key: 3, value: 'Confidential', desc: '重要数据，对数据丢失或泄露有较大影响，需严格保护' },
  { key: 4, value: 'Highly Confidential', desc: '非常重要的数据，对数据丢失或泄露有严重影响，需最高级别保护' },
  { key: 5, value: 'Top Secret', desc: '极其重要的数据，对数据丢失或泄露有灾难性影响，需最严格的保护措施' }
]);


/** 查询分类列表 */
function getList() {
  loading.value = true;
  listCatalog(queryParams.value).then(response => {
    deptList.value = proxy.handleTree(response.data, "id");
    loading.value = false;
  });
}
/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
}
/** 表单重置 */
function reset() {
  form.value = {
    id: undefined,
    parentId: undefined,
    name: undefined,
    orderNum: 0,
    leader: undefined,
    phone: undefined,
    email: undefined,
    hasStatus: "0"
  };
  proxy.resetForm("deptRef");
}
/** 搜索按钮操作 */
function handleQuery() {
  getList();
}
/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}
/** 新增按钮操作 */
function handleAdd(row) {
  reset();
  listCatalog().then(response => {
    deptOptions.value = proxy.handleTree(response.data, "id");
  });
  if (row != undefined) {
    form.value.parentId = row.id;
  }
  open.value = true;
  title.value = "添加分类";
}
/** 展开/折叠操作 */
function toggleExpandAll() {
  refreshTable.value = false;
  isExpandAll.value = !isExpandAll.value;
  nextTick(() => {
    refreshTable.value = true;
  });
}
/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  listCatalogExcludeChild(row.id).then(response => {
    deptOptions.value = proxy.handleTree(response.data, "id");
  });
  getCatalog(row.id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改分类";
  });
}
/** 提交按钮 */
function submitForm() {
  proxy.$refs["deptRef"].validate(valid => {
    if (valid) {
      if (form.value.id != undefined) {
        updateCatalog(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addCatalog(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}
/** 删除按钮操作 */
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除名称为"' + row.name + '"的数据项?').then(function() {
    return delCatalog(row.id);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

getList();
</script>
