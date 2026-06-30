<template>
  <div class="empty-state">
    <van-empty :description="description" :image="image">
      <slot>
        <van-button v-if="actionText" type="primary" size="small" @click="$emit('action')">
          {{ actionText }}
        </van-button>
      </slot>
    </van-empty>
  </div>
</template>

<script setup>
defineProps({
  description: { type: String, default: '暂无数据' },
  image: { type: String, default: 'default' },
  actionText: { type: String, default: '' }
})

defineEmits(['action'])
</script>

<style scoped>
.empty-state {
  padding: 40px 0;
}
</style>
