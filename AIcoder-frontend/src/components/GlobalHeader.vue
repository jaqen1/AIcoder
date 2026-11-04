<script setup lang="ts">
import { type MenuProps, message } from 'ant-design-vue'
import { computed, h } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import logo from '@/assets/logo.jpg'
import { filterMenus, menus, type AppMenuItem } from '@/config/menus'
import { LogoutOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'
import router from '@/router'

const route = useRoute()
const loginUserStore = useLoginUserStore()

const mapMenus = (items: AppMenuItem[]): MenuProps['items'] =>
  items.map((item) => ({
    key: item.path ?? item.key,
    label: item.path ? h(RouterLink, { to: item.path }, { default: () => item.title }) : item.title,
    children: item.children?.length ? mapMenus(item.children) : undefined,
  }))

const menuItems = computed(() => mapMenus(filterMenus(menus, loginUserStore.loginUser)))

const selectedKeys = computed(() => [route.path])

// 用户注销
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}

</script>

<template>
  <a-layout-header class="global-header">
    <div class="header-brand">
      <img :src="logo" alt="AIcoder logo" class="header-logo" />
      <span class="site-title">AIcoder</span>
    </div>
    <a-menu
      class="header-menu"
      theme="light"
      mode="horizontal"
      :items="menuItems"
      :selected-keys="selectedKeys"
      overflowed-indicator="更多"
    />
    <div class="user-login-status">
      <div v-if="loginUserStore.loginUser.id">
        <a-dropdown>
          <a-space>
            <a-avatar :src="loginUserStore.loginUser.userAvatar" />
            {{ loginUserStore.loginUser.userName ?? '无名' }}
          </a-space>
          <template #overlay>
            <a-menu>
              <a-menu-item @click="doLogout">
                <LogoutOutlined />
                退出登录
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>

      <div v-else>
        <a-button type="primary" href="/user/login">登录</a-button>
      </div>
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
  color: rgba(0, 0, 0, 0.85);
  background: #fff;
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
  color: rgba(0, 0, 0, 0.88);
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
