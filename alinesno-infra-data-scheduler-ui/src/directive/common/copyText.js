import { ElMessage } from "element-plus"; 

/**
 * v-copyText 复制文本内容
 * Copyright (c) 2022 ruoyi
 */
export default {
  beforeMount(el, { value, arg }) {
    if (arg === "callback") {
      el.$copyCallback = value;
    } else {
      el.$copyValue = value;
      const handler = () => {
        const success = copyTextToClipboard(el.$copyValue);
        if (success) {
          ElMessage.success('复制成功');
        } else {
          ElMessage.error('复制失败');
        }
        if (el.$copyCallback) {
          // 将 success 也传回去，便于回调处理
          el.$copyCallback(el.$copyValue, success);
        }
      };
      el.addEventListener("click", handler);
      el.$destroyCopy = () => el.removeEventListener("click", handler);
    }
  },

  updated(el, { value, arg }) {
    // 支持动态更新 value 或 callback
    if (arg === "callback") {
      el.$copyCallback = value;
    } else {
      el.$copyValue = value;
    }
  },

  beforeUnmount(el) {
    if (el.$destroyCopy) {
      el.$destroyCopy();
    }
    delete el.$copyValue;
    delete el.$copyCallback;
    delete el.$destroyCopy;
  }
}

function copyTextToClipboard(input, { target = document.body } = {}) {
  const element = document.createElement('textarea');
  const previouslyFocusedElement = document.activeElement;

  element.value = input;

  // Prevent keyboard from showing on mobile
  element.setAttribute('readonly', '');

  element.style.contain = 'strict';
  element.style.position = 'absolute';
  element.style.left = '-9999px';
  element.style.fontSize = '12pt'; // Prevent zooming on iOS

  const selection = document.getSelection();
  const originalRange = selection.rangeCount > 0 && selection.getRangeAt(0);

  target.append(element);
  element.select();

  // Explicit selection workaround for iOS
  element.selectionStart = 0;
  element.selectionEnd = input.length;

  let isSuccess = false;
  try {
    isSuccess = document.execCommand('copy');
  } catch { }

  element.remove();

  if (originalRange) {
    selection.removeAllRanges();
    selection.addRange(originalRange);
  }

  // Get the focus back on the previously focused element, if any
  if (previouslyFocusedElement) {
    previouslyFocusedElement.focus();
  }

  return isSuccess;
}