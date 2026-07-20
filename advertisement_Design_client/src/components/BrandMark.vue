<template>
  <div class="brand">
    <div class="brand-mark">{{ initials }}</div>
    <div class="brand-copy">
      <strong>{{ title }}</strong>
      <span>{{ subtitle }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    subtitle?: string
    initials?: string
  }>(),
  {
    subtitle: '',
    initials: 'AD'
  }
)
</script>
