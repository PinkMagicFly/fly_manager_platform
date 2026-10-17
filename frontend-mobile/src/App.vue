<script setup lang="ts">
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'

onLaunch(() => {
  console.log('App Launch: 翔云·低空智慧物流运营平台 移动作业端初始化')
})
onShow(() => {
  console.log('App Show: 移动作业端前台运行')
})
onHide(() => {
  console.log('App Hide: 移动作业端进入后台')
})
</script>

<style>
/* UniApp 全局公共样式 */
page {
  background-color: #f8fafc;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  color: #1e293b;
  -webkit-font-smoothing: antialiased;
}
view, text, image, input, button {
  box-sizing: border-box;
}
</style>

