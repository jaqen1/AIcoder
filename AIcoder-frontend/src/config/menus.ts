export interface AppMenuItem {
  key: string
  title: string
  path?: string
  children?: AppMenuItem[]
}

// 菜单配置项
export const menus: AppMenuItem[] = [
  {
    key: 'home',
    title: '首页',
    path: '/',
  },
  {
    key: 'about',
    title: '关于',
    path: '/about',
  },
  {
    key: 'admin',
    title: '管理员',
    path: '/admin/userManage',
  },
  {
    key: 'author',
    title: '作者',
    path: 'https://github.com/jaqen1',
  },
]

/**
 * 根据登录用户权限过滤菜单
 * 路径以 /admin 开头的菜单仅管理员可见
 */
export const filterMenus = (
  items: AppMenuItem[] = menus,
  loginUser?: API.LoginUserVO,
): AppMenuItem[] => {
  return items
    .filter((menu) => {
      const menuKey = menu.path ?? menu.key
      if (menuKey?.startsWith('/admin')) {
        if (!loginUser || loginUser.userRole !== 'admin') {
          return false
        }
      }
      return true
    })
    .map((menu) => ({
      ...menu,
      children: menu.children?.length ? filterMenus(menu.children, loginUser) : menu.children,
    }))
}
