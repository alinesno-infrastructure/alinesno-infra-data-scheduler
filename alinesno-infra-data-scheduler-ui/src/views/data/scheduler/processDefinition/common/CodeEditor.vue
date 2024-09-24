<template>
    <div class="cm-container">
            <code-mirror 
              basic 
              :lang="lang" 
              v-model="codeVal" 
              style="height: 400px;" 
              :theme="theme"
              :extensions="extensions" />
          </div>
</template>

<script setup>

import CodeMirror from 'vue-codemirror6'
import { oneDark } from '@codemirror/theme-one-dark'
import { python } from '@codemirror/lang-python';

import { json } from '@codemirror/lang-json';
import { sql } from '@codemirror/lang-sql';
import { yaml } from '@codemirror/lang-yaml';

const router = useRouter();
const { proxy } = getCurrentInstance();

const props = defineProps({
  lang: {
    type: String,
    default: 'python' , 
  },
});


// 初始化
let codeVal = ref('');
const lang = props.lang == 'python'? python(): 
  props.lang == 'json'?json():
  props.lang == 'yaml'?yaml():
  props.lang == 'sql'?sql():
  python() ;

// 扩展
const extensions = [oneDark];

// 主题样式设置
const theme = {
//   "&": {
//     fontSize: "9.5pt",
//     color: "white",
//     backgroundColor: "#034"
//   },
//   ".cm-content": {
//     caretColor: "#0e9"
//   },
//   "&.cm-focused .cm-cursor": {
//     borderLeftColor: "#0e9"
//   },
//   "&.cm-focused .cm-selectionBackground, ::selection": {
//     backgroundColor: "#074"
//   },
//   ".cm-gutters": {
//     backgroundColor: "#045",
//     color: "#ddd",
//     border: "none"
//   }
}

/**
 * 获取到codeValue
 */
function getRawScript(){
  return codeVal.value ;
}

defineExpose({
  getRawScript
})

</script>

<style >
/* required! */
.cm-editor {
  height: 100%;
}

.cm-container{
  width:100%;
}
</style>