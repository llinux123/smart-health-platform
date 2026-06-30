<template>
  <van-tabbar v-model="active" route>
    <van-tabbar-item icon="home-o" to="/home">首页</van-tabbar-item>
    <van-tabbar-item icon="chat-o" to="/consultation">问诊</van-tabbar-item>
    <van-tabbar-item icon="orders-o" to="/registration/orders">订单</van-tabbar-item>
    <van-tabbar-item icon="contact-o" to="/prescriptions">我的</van-tabbar-item>
  </van-tabbar>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const active = ref(0)

const tabMap = {
  '/home': 0,
  '/consultation': 1,
  '/registration/orders': 2,
  '/prescriptions': 3
}

watch(
  () => route.path,
  (path) => {
    active.value = tabMap[path] ?? 0
  },
  { immediate: true }
)
</script>
