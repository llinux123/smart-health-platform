<template>
  <div id="app">
    <router-view />
    <AppTabBar v-if="showTabBar" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppTabBar from '@/components/AppTabBar.vue'

const route = useRoute()
const showTabBar = computed(() => !route.meta.hideTab)
</script>

<style>
#app {
  min-height: 100vh;
}
</style>
