<template>
  <div class="flow-design-operate">
    <!-- <div class="flow-fullscreen">
      <span class="flow-fullscreen-icon"></span>
    </div>  -->
    <div class="flow-design-zoom">
      <div class="flow-design-zoom-less" @click="onZoomOut"></div>
      <span>{{ zoomValue }}%</span>
      <div class="flow-design-zoom-more" @click="onZoomIn"></div>
    </div>
  </div>
</template>

<script setup>

const INIT_ZOOM_VALUE = ref(100);
const MIN_ZOOM_VALUE = ref(50);
const MAX_ZOOM_VALUE = ref(200);

</script>
