<template>
   <div class="app-container">

    <el-page-header @back="goBack" content="读取源配置"></el-page-header>
    <div class="label-title">
      <div class="tip">创建数据读取源</div>
      <div class="sub-tip">根据业务场景需求创建数据编排任务，便于数据业务场景开发和分析</div>
    </div>
    <div class="form-container" >
         <el-form ref="databaseRef" :model="form" :rules="rules" label-width="120px">

            <el-form-item label="描述" prop="readerDesc">
               <el-input v-model="form.readerDesc" placeholder="读取源描述"/>
            </el-form-item>

            <el-form-item label="源类型" prop="operationType">
               <el-radio-group v-model="form.operationType" @change="changeReaderType()">
                  <el-radio key="source" label="source" value="source"  size="large">读取(reader)</el-radio>
                  <!-- <el-radio key="sink" label="sink" value="sink"  size="large">写入(sink)</el-radio> -->
               </el-radio-group> 
            </el-form-item>

            <!-- 数据采集模板 -->
            <el-form-item label="数据采集源" prop="readerType">
               <el-radio-group v-model="form.readerType">
                  <el-radio v-for="item in form.operationType=='source'?readerType:sinkSource" 
                     class="database-type" 
                     :key="item.code"
                     :label="item.code"
                     :value="item.code" 
                     size="large">
                     <div style="float: left;">
                        <img style="width:30px; height:30px" :src="'http://data.linesno.com/icons/database/' + item.icon+ ''" /> 
                     </div>
                     <div style="float: left;margin-left: 10px;line-height: 1.2rem;">
                           {{ item.code}}
                           <div style="font-size: 11px;">
                           {{ item.readerDesc }} 
                        </div>
                     </div>
                  </el-radio>
               </el-radio-group> 

            </el-form-item>

               <!-- 数据源配置项 -->
               <div v-if="
                        form.readerType === 'mysql' || 
                        form.readerType === 'doris' || 
                        form.readerType === 'clickhouse' || 
                        form.readerType === 'postgresql' ||
                        form.readerType === 'sqlserver' ||
                        form.readerType === 'hive' ||
                        form.readerType === 'oracle'
                        ">

                  <el-form-item label="连接" prop="readerUrl">
                     <el-input v-model="form.readerUrl" placeholder="jdbc:mysql://localhost:3306/readerName?useUnicode=true&characterEncoding=utf8&characterSetResults=utf8&useSSL=false&serverTimezone=GMT"/>
                  </el-form-item>

                  <el-form-item label="用户名" prop="readerUsername">
                     <el-input v-model="form.readerUsername" style="width:50%"  auto-complete="new-password" placeholder="读取源用户名"/>
                  </el-form-item>

                  <el-form-item label="密码" prop="readerPasswd">
                     <el-input type="password" auto-complete="new-password" :show-password="true" style="width:50%"  v-model="form.readerPasswd" placeholder="读取源密码"/>
                  </el-form-item>
               </div>

               <!-- SSO配置项 -->
               <div v-if="
                        form.readerType === 'minio' || 
                        form.readerType === 'qiniu' || 
                        form.readerType === 's3file'
                        ">
                  <el-form-item label="地址" prop="readerUrl">
                     <el-input v-model="form.readerUrl" placeholder="SSO文件读取地址"/>
                  </el-form-item>
                  <el-form-item label="AccessKey" prop="accessKey">
                     <el-input v-model="form.accessKey" style="width:50%" :show-password="true" placeholder="请求密钥"/>
                  </el-form-item>
                  <el-form-item label="SecretKey" prop="secretKey">
                     <el-input v-model="form.secretKey" style="width:50%" :show-password="true" placeholder="请求Security"/>
                  </el-form-item>
               </div>

               <!-- Ftp/ElasticSearch/Redis 配置项 -->
               <div v-if="
                        form.readerType === 'elasticsearch'  ||
                        form.readerType === 'ftp'  ||
                        form.readerType === 'sftp'  ||
                        form.readerType === 'redis' 
                        ">
                  <el-form-item label="Host地址" prop="readerUrl">
                     <el-input v-model="form.readerUrl" placeholder="ElasticSearch地址"/>
                  </el-form-item>
                  <el-form-item label="用户名" prop="readerUsername" 
                     v-if="
                        form.readerType !== 'redis' 
                     ">
                     <el-input v-model="form.readerUsername" style="width:50%" placeholder="请求用户名"/>
                  </el-form-item>
                  <el-form-item label="密码" prop="readerUsername">
                     <el-input v-model="form.readerPasswd" style="width:50%" :show-password="true" placeholder="请求密码"/>
                  </el-form-item>
               </div>

               <!-- Kafka配置项/ActiveMQ配置项 -->
               <div v-if="
                        form.readerType === 'kafka'  ||
                        form.readerType === 'activemq' 
                        ">
                  <el-form-item label="Server地址" prop="readerUrl">
                     <el-input v-model="form.readerUrl" placeholder="Server地址"/>
                  </el-form-item>
               </div>

               <!-- cvs/excel配置项 -->
               <div v-if="
                        form.readerType === 'csv'  ||
                        form.readerType === 'localfile'  ||
                        form.readerType === 'excel' 
                        ">
                  <el-form-item label="文件地址" prop="readerUrl">
                     <el-input v-model="form.readerUrl" placeholder="文件地址"/>
                  </el-form-item>
                  <el-form-item label="用户名" prop="readerUsername">
                     <el-input v-model="form.readerUsername" style="width:50%" placeholder="地址认证用户名"/>
                  </el-form-item>
                  <el-form-item label="密码" prop="readerPasswd">
                     <el-input v-model="form.readerPasswd" style="width:50%" :show-password="true" placeholder="地址认证密码"/>
                  </el-form-item>
               </div>

            </el-form>

            <div class="dialog-footer" style="float:right;margin-top:20px">
               <!-- <el-button type="primary" @click="submitForm">确 定</el-button>
               <el-button @click="cancel">取 消</el-button> -->
               <el-button type="text" @click="validateDburl">请先点击验证读取源是否连通</el-button>
               <el-button type="primary" @click="submitForm" :disabled="!btnChangeEnable">确认保存</el-button>
               <!-- <el-button @click="cancel">取 消</el-button> -->
            </div>
      </div>
   </div>
</template>

<script setup name="Datasource">

import {
   listDatasource,
   delDatasource,
   getDatasource,
   checkDbConfig,
   checkConnectionByObj,
   updateDatasource,
   addDatasource ,
   allDataSource
} from "@/api/data/scheduler/datasources";

import { ElLoading } from 'element-plus'
import { nextTick } from "vue";

const router = useRouter();
const route = useRoute();
const { proxy } = getCurrentInstance();

// 定义变量
const open = ref(false);
const loading = ref(true);
const btnChangeEnable = ref(true)

const readerType = ref([])  // 读取源
const sinkSource = ref([])  // 数据目的

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
   form: {
      operationType: 'source'
   },
   rules: {
      readerName: [{ required: true, message: "名称不能为空", trigger: "blur" }] , 
      readerType: [{ required: true, message: "数据源类型不能为空", trigger: "blur" }] , 
      readerUrl: [{ required: true, message: "连接不能为空", trigger: "blur" }],
      operationType: [{ required: true, message: "类型不能为空", trigger: "blur" }] , 
      readerUsername: [{ required: true , message: "用户名不能为空", trigger: "blur"}],
      readerPasswd: [{ required: true, message: "密码不能为空", trigger: "blur" }] , 
      accessKey: [{ required: true, message: "密钥不能为空", trigger: "blur" }] , 
      secretKey: [{ required: true, message: "密钥不能为空", trigger: "blur" }] , 
      readerDesc: [{ required: true, message: "备注不能为空", trigger: "blur" }] 
   }
});

const { form, rules } = toRefs(data);

/** 修改状态信息 */
function changeReaderType(val){
   form.value.readerType = '' ;
}

/** 获取到所有数据源 */
function handleGetAllSourceReader() {
   allDataSource().then(response => {
    const data = response.data;

    readerType.value = data.filter(item => item.sourceType === 'sink');
    sinkSource.value = data.filter(item => item.sourceType === 'source');

   //  readerType.value = data ; 
   //  sinkSource.value = data ; 

    console.log('sinkSource = ' + sinkSource.value)

    nextTick(() => {
      handleSourceInfo();
    });
  });
}

/** 通过id获取到源信息 */
function handleSourceInfo(){

   let id =route.query.id;

   if(id){
      getDatasource(id).then(response => {
         form.value = response.data;
      });
   }
}

/** 返回 */
function goBack() {
   router.push({path:'/data/scheduler/datasource/index',query:{}});
}

/** 提交按钮 */
function validateDburl() {
   proxy.$refs["databaseRef"].validate(valid => {
      if (valid) {
         const loading = ElLoading.service({
            lock: true,
            text: form.value.readerDesc + '连接验证中',
            background: 'rgba(0, 0, 0, 0.7)',
         })
         checkConnectionByObj(form.value).then(resp=>{
            proxy.$modal.msgSuccess("读取源校验成功");
            loading.close()
         }).catch(error => {
            loading.close()
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

handleGetAllSourceReader();

</script>

<style scoped lang="scss">
  .form-container {
    max-width: 960px;
    margin-left: auto;
    margin-right: auto;
    margin-top: 20px;
  }

  .label-title {
    text-align: center;
    max-width: 960px;
    margin-left: auto;
    margin-right: auto;
    margin-top: 10px;

    .tip {
      padding-bottom: 10px;
      font-size: 26px;
      font-weight: bold;
    }

    .sub-tip {
      font-size: 13px;
      text-align: center;
      padding: 10px;
    }
  }

  .image{
    width:100%;
    height: 120px ;
  }

  .select-card {
    border: 1px solid rgb(0, 91, 212) ;
  }
</style>