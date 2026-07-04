import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'

// Vant 样式
import 'vant/lib/index.css'

// 全局样式
import './styles/global.css'

// 不再手动清除认证状态，由路由守卫按需鉴权。
// 未勾选「记住我」时 token 保存在 sessionStorage 中，关闭标签页自动失效。

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
