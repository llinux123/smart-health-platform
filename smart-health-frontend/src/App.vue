<template>
  <div id="app">
    <router-view :key="route.fullPath" />
    <AppTabBar v-if="showTabBar" />
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppTabBar from '@/components/AppTabBar.vue'

const route = useRoute()
const showTabBar = computed(() => !route.meta.hideTab)

// 页面切换时滚动到顶部
watch(() => route.path, () => {
  window.scrollTo({ top: 0, behavior: 'instant' })
})
</script>

<style>
#app {
  min-height: 100vh;
  background: var(--color-bg);
}
</style>
