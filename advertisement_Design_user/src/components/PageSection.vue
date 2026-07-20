<template>
  <section class="surface pad">
    <div class="section-head">
      <div>
        <h3>{{ title }}</h3>
        <p v-if="subtitle" class="muted" style="margin: 4px 0 0">{{ subtitle }}</p>
      </div>
      <slot name="extra" />
    </div>
    <slot />
  </section>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
}>()
</script>
