<script setup lang="ts">
import { ref } from 'vue'
import track from '@track/sdk'

const endpoint = ref((typeof window !== 'undefined' && window.location.origin) || '')
const appId = ref('vue3-example-app-id')
const appName = ref('Vue3 Example')
const userId = ref('user-123')
const sessionTTL = ref<number | string>(1440)

const inited = ref(false)
const started = ref(false)

const statusText = ref('等待初始化 SDK...')
const statusType = ref<'info' | 'success' | 'error'>('info')
const logs = ref<string[]>([])

function log(message: string, type: 'info' | 'success' | 'error' = 'info') {
  const time = new Date().toLocaleTimeString()
  logs.value.push(`[${time}] ${message}`)
  if (logs.value.length > 500) logs.value.shift()
  if (type !== 'info') console[type === 'error' ? 'error' : 'log'](message)
}

function setStatus(message: string, type: 'info' | 'success' | 'error' = 'info') {
  statusText.value = message
  statusType.value = type
}

async function onInit() {
  try {
    if (!endpoint.value || !appId.value || !userId.value) {
      setStatus('❌ 请填写所有必填字段', 'error')
      return
    }
    setStatus('⏳ 正在初始化...', 'info')
    log('正在初始化 SDK...', 'info')

    const cfg: any = {
      appId: appId.value,
      userId: userId.value,
      userProps: { plan: 'premium', version: '1.0.0', source: 'vue3-example' },
    }
    if (appName.value) cfg.appName = appName.value

    await track.init(
      cfg,
      {
        endpoint: endpoint.value,
        autoTrack: true,
        performance: true,
        errorTrack: true,
        sessionTTL: Number(sessionTTL.value) || 1440,
        clickTrack: { enabled: true },
      },
    )
    inited.value = true
    setStatus('✅ SDK 初始化成功，点击"启动追踪"开始使用', 'success')
    log('SDK 初始化成功', 'success')
  } catch (e: any) {
    setStatus(`❌ 初始化失败: ${e?.message || e}`, 'error')
    log(`初始化失败: ${e?.message || e}`, 'error')
  }
}

function onStart() {
  try {
    track.start()
    started.value = true
    setStatus('✅ SDK 已启动，正在采集数据...', 'success')
    log('SDK 已启动，开始自动采集', 'success')
  } catch (e: any) {
    setStatus(`❌ 启动失败: ${e?.message || e}`, 'error')
    log(`启动失败: ${e?.message || e}`, 'error')
  }
}

async function onStop() {
  try {
    await track.stop()
    started.value = false
    inited.value = false
    setStatus('⏸️ SDK 已停止', 'info')
    log('SDK 已停止', 'info')
  } catch (e: any) {
    setStatus(`❌ 停止失败: ${e?.message || e}`, 'error')
    log(`停止失败: ${e?.message || e}`, 'error')
  }
}

function onRandomUser() {
  userId.value = 'user-' + Math.random().toString(36).slice(2, 8)
  log('已生成随机用户 ID', 'success')
}

function onQuickFillLocal() {
  endpoint.value = 'http://localhost:8080'
  appId.value = 'example-app-id'
  userId.value = 'user-dev-' + Math.floor(Math.random() * 10000)
  log('已填充本地开发配置', 'success')
}

function onTrackEvent() {
  track.track('button_click', { buttonId: 'test-btn', category: 'action' })
  log('上报自定义事件: button_click', 'success')
}

function onBatchEvents() {
  const now = Date.now()
  ;[
    { id: 'batch_event_1', props: { idx: 1, ts: now } },
    { id: 'batch_event_2', props: { idx: 2, ts: now + 1 } },
    { id: 'batch_event_3', props: { idx: 3, ts: now + 2 } },
  ].forEach((it) => track.track(it.id, it.props))
  log('已触发 3 个自定义事件用于批量上报', 'success')
}

function onTestError() {
  try {
    throw new Error('这是一个测试错误，用于验证错误监控功能')
  } catch (e) {
    log('错误已捕获并上报', 'error')
  }
}

function onTestPromiseError() {
  Promise.reject(new Error('这是一个未处理的 Promise 错误')).catch(() => {
    log('Promise 错误已捕获并上报', 'error')
  })
}

function onTestPv() {
  log('手动触发 PV（通过路由变化触发）', 'info')
  window.history.pushState({}, '', '/test-page')
  setTimeout(() => window.history.pushState({}, '', '/'), 800)
}
</script>

<template>
  <div class="container">
    <h1>🚀 Track SDK - Vue 3 示例</h1>

    <div class="info-box">
      <strong>📝 说明：</strong>
      <ul>
        <li>确保已构建 SDK（在根包运行 <code>pnpm build</code>）</li>
        <li>请填写服务端地址（Endpoint），例如：<code>http://localhost:8080</code></li>
      </ul>
    </div>

    <div class="section">
      <h2>⚙️ SDK 配置</h2>
      <div class="input-group highlight">
        <label>🌐 服务端地址 (Endpoint) <span class="required">*必填</span></label>
        <input v-model="endpoint" placeholder="http://localhost:8080" />
        <small>示例：<code>http://localhost:8080</code>（本地）或 <code>https://track.yourdomain.com</code>（生产）</small>
      </div>
      <div class="grid-2">
        <div class="input-group">
          <label>应用 ID (App ID)</label>
          <input v-model="appId" />
        </div>
        <div class="input-group">
          <label>项目名 (App Name，可选)</label>
          <input v-model="appName" placeholder="不填则使用 App ID" />
        </div>
      </div>
      <div class="grid-2">
        <div class="input-group">
          <label>用户 ID (User ID)</label>
          <input v-model="userId" />
        </div>
      </div>
      <div class="input-group">
        <label>Session 有效期 (分钟)</label>
        <input v-model.number="sessionTTL" type="number" />
      </div>
      <div class="button-group">
        <button class="success" @click="onInit" :disabled="inited">初始化 SDK</button>
        <button @click="onStart" :disabled="!inited || started">启动追踪</button>
        <button class="danger" @click="onStop" :disabled="!started">停止追踪</button>
        <button @click="onQuickFillLocal">一键填充本地</button>
        <button @click="onRandomUser">随机用户</button>
      </div>
      <div :class="['status', statusType]">{{ statusText }}</div>
    </div>

    <div class="section">
      <h2>📊 自动采集与测试</h2>
      <div class="button-group">
        <button :disabled="!started" @click="log('点击了测试按钮（会被自动采集）','info')">测试点击采集</button>
        <button :disabled="!started" @click="onTestPv">手动触发 PV</button>
        <button class="danger" :disabled="!started" @click="onTestError">触发测试错误</button>
        <button class="danger" :disabled="!started" @click="onTestPromiseError">触发 Promise 错误</button>
      </div>
    </div>

    <div class="section">
      <h2>🎯 自定义事件上报</h2>
      <div class="button-group">
        <button :disabled="!started" @click="onTrackEvent">上报自定义事件</button>
        <button :disabled="!started" @click="onBatchEvents">批量上报 3 个事件</button>
      </div>
    </div>

    <div class="section">
      <h2>📋 操作日志</h2>
      <div class="log">
        <div v-for="(line, idx) in logs" :key="idx" class="log-entry">{{ line }}</div>
      </div>
      <button style="margin-top:10px" @click="logs = []">清空日志</button>
    </div>
  </div>
  
</template>

<style scoped>
* { box-sizing: border-box; }
.container { max-width: 1000px; margin: 0 auto; padding: 20px; }
h1 { margin-top: 0; }
h2 { color: #666; border-bottom: 2px solid #eee; padding-bottom: 10px; margin-top: 30px; }
.section { margin: 20px 0; }
.button-group { display: flex; flex-wrap: wrap; gap: 10px; }
button { padding: 10px 20px; margin: 5px 0; font-size: 14px; cursor: pointer; border: none; border-radius: 4px; background: #007bff; color: #fff; }
button:hover { background: #0056b3; }
button.danger { background: #dc3545; }
button.danger:hover { background: #c82333; }
button.success { background: #28a745; }
button.success:hover { background: #218838; }
.input-group { margin: 15px 0; }
.input-group.highlight { background: #fff3cd; padding: 15px; border-radius: 4px; border-left: 4px solid #ffc107; }
label { display: block; margin-bottom: 5px; color: #666; font-weight: 500; }
input { width: 100%; padding: 8px 12px; border: 1px solid #ddd; border-radius: 4px; font-size: 14px; }
input:focus { outline: none; border-color: #007bff; }
small { color: #856404; display: block; margin-top: 5px; }
.required { color: #dc3545; }
.status { padding: 10px; margin: 10px 0; border-radius: 4px; font-weight: 500; }
.status.success { background: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
.status.error { background: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
.status.info { background: #d1ecf1; color: #0c5460; border: 1px solid #bee5eb; }
.log { margin-top: 20px; padding: 15px; background: #f8f9fa; border-radius: 4px; font-family: 'Courier New', monospace; font-size: 12px; white-space: pre-wrap; max-height: 300px; overflow-y: auto; border: 1px solid #dee2e6; }
.log-entry { margin: 2px 0; }
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
@media (max-width: 640px) { .grid-2 { grid-template-columns: 1fr; } }
</style>
