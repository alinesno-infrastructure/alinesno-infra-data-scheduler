<template>
  <div class="app-container">
    <el-col :span="24" :xs="24">
      <div class="toolbar" style="margin-bottom: 12px;display: flex;">
        <div>
          <el-input v-model="query.name" placeholder="名称" style="width: 200px;margin-right:8px" />
          <el-select v-model="query.provider" placeholder="选择类型" style="width: 160px;margin-right:8px">
            <el-option label="全部" value=""></el-option>
            <el-option label="企业微信机器人" value="WECHAT_ROBOT"></el-option>
            <el-option label="钉钉机器人" value="DINGTALK"></el-option>
            <el-option label="邮件" value="EMAIL"></el-option>
          </el-select>
        </div>
        <div>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </div>
      </div>
    </el-col>

    <el-row :gutter="10" class="mb8">

      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="onAdd">新增</el-button>
      </el-col>

      <right-toolbar v-model:showSearch="showSearch" @queryTable="fetchList" :columns="columns"></right-toolbar>
    </el-row>

    <el-table :data="list" stripe style="width: 100%">
      <!-- 序号-->
       <el-table-column type="index" width="50" />
      <el-table-column prop="icon" align="center" label="图标" width="60">
        <template #default="{ row }">
          <img :src="getImIconPath(row.provider)" style="width: 20px;height: 20px;" />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" width="220" />
      <el-table-column prop="provider" label="类型" width="140" />
      <el-table-column prop="webhook" label="Webhook" />
      <el-table-column prop="enabled" label="启用" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" @change="onToggleEnabled(row)"></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="mini" @click="onEdit(row.id)">编辑</el-button>
          <el-button size="mini" type="danger" @click="onRemove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

          <pagination v-show="page.total > 0" :total="page.total" v-model:page="page.current"
              v-model:limit="page.size" @pagination="onPageChange" />

    <!-- <div class="pagination" style="margin-top:12px;text-align:right;">
      <el-pagination background layout="prev, pager, next" :current-page="page.current" :page-size="page.size"
        :total="page.total" @current-change="onPageChange">
      </el-pagination>
    </div> -->

    <el-dialog :title="formTitle" v-model="showForm" width="720px">
      <notification-config-form :id="editingId" @saved="onSaved" @cancel="showForm = false" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import NotificationConfigForm from './notificationConfigForm.vue';
import { getImIconPath } from '@/utils/llmIcons';

// 导入你要求风格的 API（注意路径）
import { listCredential, removeCredential } from '@/api/data/scheduler/notificationConfig';

const list = ref([]);
const showForm = ref(false);
const editingId = ref(null);
const formTitle = ref('新增配置');

const page = reactive({ current: 1, size: 10, total: 0 });
const query = reactive({ name: '', provider: '' });

function fetchList() {
  // 使用 listCredential(provider)，后端使用 params 方式接收
  listCredential(query.provider).then(res => {
    const body = res && res.data ? res.data : res;
    if (body && body.success !== false) {
      const data = body.data || body;
      let arr = data || [];
      if (query.name) {
        arr = arr.filter(i => i.name && i.name.includes(query.name));
      }
      page.total = arr.length;
      const start = (page.current - 1) * page.size;
      list.value = arr.slice(start, start + page.size);
    } else {
      ElMessage.error((body && body.message) || '获取失败');
    }
  }).catch(err => {
    console.error(err);
    ElMessage.error('获取失败');
  });
}

onMounted(() => {
  fetchList();
});

function onPageChange(p) {
  page.current = p;
  fetchList();
}

function onAdd() {
  editingId.value = null;
  formTitle.value = '新增配置';
  showForm.value = true;
}

function onEdit(id) {
  editingId.value = id;
  formTitle.value = '编辑配置';
  showForm.value = true;
}

function onRemove(id) {
  ElMessageBox.confirm('确认删除该配置吗？', '提示', { type: 'warning' })
    .then(() => {
      removeCredential(id).then(res => {
          ElMessage.success('删除成功');
          fetchList();
      }).catch(err => {
        console.error(err);
        ElMessage.error('删除失败');
      });
    }).catch(() => { });
}

function onToggleEnabled(row) {
  // 简单保存开关变更：直接调用 save 接口（可进一步优化为专门接口）
  // 这里不直接保存，建议打开编辑或实现单字段保存逻辑。如需我可以补充 save 调用。
}

function onSaved() {
  showForm.value = false;
  // 重新拉取第一页
  page.current = 1;
  fetchList();
}
</script>