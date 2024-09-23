<template>
  <span class="node-name-title">
    <span v-if="!isInput" @click.stop="clickEvent()">{{ value }}</span>
    <a-input v-if="isInput" type="text" @blur="blurEvent()" @focus="$event.currentTarget.select()" v-focus v-model="value" :style="{ width: width }" />
  </span>
</template>

<script setup>
  import { flowMixin } from './flowMixin';
</script>
