/// <reference types="vite/client" />

// 补充 Vite 环境变量声明（修复 vue-tsc 找不到 import.meta.env 的问题）
interface ViteEnv {
  readonly VITE_APP_TITLE: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv & ViteEnv
}

// CSS side-effect imports — vite/client.d.ts 的 *.css 声明不匹配路径字符串
declare module 'element-plus/dist/index.css' {}
declare module './assets/styles/global.css' {}
declare module 'element-plus/dist/locale/zh-cn.mjs' {}
