import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

import 'element-plus/theme-chalk/src/message.scss'
import 'element-plus/theme-chalk/src/notification.scss'
import 'element-plus/theme-chalk/src/message-box.scss'
import 'element-plus/theme-chalk/src/loading.scss'

import './assets/styles/main.css'

import { initTheme } from '@/composables/useTheme'

// 首屏前初始化主题,避免 FOUC
initTheme()

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
