<script setup lang="ts">
import type { MenuProps } from 'ant-design-vue'
import { computed, h } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import logo from '@/assets/logo.jpg'
import { menus, type AppMenuItem } from '@/config/menus'

const route = useRoute()

const mapMenus = (items: AppMenuItem[]): MenuProps['items'] =>
  items.map((item) => ({
    key: item.path ?? item.key,
    label: item.path
      ? h(RouterLink, { to: item.path }, { default: () => item.title })
      : item.title,
    children: item.children?.length ? mapMenus(item.children) : undefined,
  }))

const menuItems = computed(() => mapMenus(menus))

const selectedKeys = computed(() => [route.path])
</script>

<template>
  <a-layout-header class="global-header">
    <div class="header-brand">
      <img :src="logo" alt="AIcoder logo" class="header-logo" />
      <span class="site-title">AIcoder</span>
    </div>
    <a-menu
      class="header-menu"
      theme="dark"
      mode="horizontal"
      :items="menuItems"
      :selected-keys="selectedKeys"
      overflowed-indicator="更多"
    />
    <div class="header-right">
      <a-button type="primary">登录</a-button>
    </div>
  </a-layout-header>
</template>

<style scoped>
.global-header {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 64px;
  padding: 0 24px;
  line-height: 64px;
}

.header-brand {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 10px;
}

.header-logo {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.site-title {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
}

.header-menu {
  flex: 1;
  min-width: 0;
  line-height: 64px;
  border-bottom: none;
}

.header-menu :deep(a) {
  color: inherit;
}

.header-right {
  flex-shrink: 0;
  margin-left: auto;
}

@media (max-width: 768px) {
  .global-header {
    padding: 0 12px;
    gap: 8px;
  }

  .site-title {
    display: none;
  }
}
</style>
