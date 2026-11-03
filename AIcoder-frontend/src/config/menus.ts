export interface AppMenuItem {
  key: string
  title: string
  path?: string
  children?: AppMenuItem[]
}

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
]
