<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'

import { useLoginUserStore } from '@/stores/loginUser.ts'

const loginUserStore = useLoginUserStore()
loginUserStore.fetchLoginUser()
</script>

<template>
  <BasicLayout />
</template>

<style>
html,
body,
#app {
  height: 100%;
  margin: 0;
}
</style>
