<template>
  <div class="surface stat">
    <div class="stat-head">
      <div>
        <div class="stat-label">{{ label }}</div>
        <div class="stat-value">{{ value }}</div>
      </div>
      <component :is="icon" v-if="icon" style="font-size: 20px; color: var(--primary)" />
    </div>
    <div class="muted">{{ hint }}</div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  label: string
  value: string | number
  hint?: string
  icon?: Component
}>()
</script>
