<template>
  <div class="dashboard-container" v-loading="loading">
    <!-- 顶部关键指标横条 -->
    <div class="metrics-bar">
      <div class="metric-card" @click="period = 'today'">
        <div class="metric-icon">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="metric-content">
          <div class="metric-label">今日净流通</div>
          <div class="metric-value">{{ formatInt(todayNet) }}</div>
        </div>
      </div>
      
      <div class="metric-card">
        <div class="metric-icon success">
          <el-icon><UserFilled /></el-icon>
        </div>
        <div class="metric-content">
          <div class="metric-label">团队总数</div>
          <div class="metric-value">{{ teams.totalTeams }}</div>
        </div>
      </div>
      
      <div class="metric-card clickable" @click="go('/audit')">
        <div class="metric-icon danger">
          <el-icon><Warning /></el-icon>
        </div>
        <div class="metric-content">
          <div class="metric-label">待审核成果</div>
          <div class="metric-value">{{ todos.pendingAchievements }}</div>
        </div>
      </div>
      
      <div class="metric-card clickable" @click="go('/resource')">
        <div class="metric-icon warning">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="metric-content">
          <div class="metric-label">即将开始预约</div>
          <div class="metric-value">{{ todos.upcomingBookings }}</div>
        </div>
      </div>
    </div>

    <!-- 主内容区：三栏紧凑布局 -->
    <el-row :gutter="8" class="main-content">
      <!-- 左列：流通趋势 -->
      <el-col :xs="24" :sm="24" :md="10" :lg="10">
        <div class="compact-card">
          <div class="compact-header">
            <span class="compact-title">币流通趋势</span>
            <el-radio-group v-model="period" size="small">
              <el-radio-button label="today">日</el-radio-button>
              <el-radio-button label="week">周</el-radio-button>
              <el-radio-button label="month">月</el-radio-button>
            </el-radio-group>
          </div>
          <div class="compact-body">
            <div class="inline-metrics">
              <div class="inline-metric">
                <span class="label">流入</span>
                <span class="value success">{{ formatInt(coinPeriod.inflow.total) }}</span>
              </div>
              <div class="inline-metric">
                <span class="label">流出</span>
                <span class="value warning">{{ formatInt(coinPeriod.outflow.total) }}</span>
              </div>
              <div class="inline-metric">
                <span class="label">净额</span>
                <span class="value">{{ formatInt(netCoin) }}</span>
              </div>
            </div>
            <div ref="trendRef" class="chart-compact"></div>
          </div>
        </div>

        <div class="compact-card" style="margin-top: 8px">
          <div class="compact-header">
            <span class="compact-title">构成明细</span>
          </div>
          <div class="compact-body">
            <div class="detail-grid">
              <div class="detail-section">
                <div class="detail-title">流入来源</div>
                <div v-for="(v, k) in coinPeriod.inflow.bySource" :key="k" class="detail-item">
                  <span class="detail-label">{{ inflowLabel(k) }}</span>
                  <span class="detail-value">{{ formatInt(v) }}</span>
                </div>
              </div>
              <div class="detail-section">
                <div class="detail-title">流出去向</div>
                <div v-for="(v, k) in coinPeriod.outflow.bySource" :key="k" class="detail-item">
                  <span class="detail-label">{{ outflowLabel(k) }}</span>
                  <span class="detail-value">{{ formatInt(v) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 中列：热门资源与热力图 -->
      <el-col :xs="24" :sm="24" :md="6" :lg="6">
        <div class="compact-card">
          <div class="compact-header">
            <span class="compact-title">热门资源</span>
          </div>
          <div class="compact-body">
            <div ref="popularRef" class="chart-compact-small"></div>
          </div>
        </div>

        <div class="compact-card" style="margin-top: 8px">
          <div class="compact-header">
            <span class="compact-title">活跃热力图</span>
          </div>
          <div class="compact-body">
            <div ref="heatmapRef" class="chart-heatmap-compact"></div>
          </div>
        </div>
      </el-col>

      <!-- 右列：团队与活动 -->
      <el-col :xs="24" :sm="24" :md="8" :lg="8">
        <div class="compact-card">
          <div class="compact-header">
            <span class="compact-title">本周 Top5</span>
          </div>
          <div class="compact-body">
            <div class="top-section">
              <div class="top-label">收益榜</div>
              <div v-for="(it, idx) in teams.activeTop5.slice(0, 5)" :key="it.teamId || idx" class="compact-top-item">
                <span class="top-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
                <span class="top-name">{{ it.teamName || '未知' }}</span>
                <span class="top-value success">+{{ formatInt(it.amount || 0) }}</span>
              </div>
            </div>
            <div class="divider-line"></div>
            <div class="top-section">
              <div class="top-label">消耗榜</div>
              <div v-for="(it, idx) in teams.spendTop5.slice(0, 5)" :key="it.teamId || idx" class="compact-top-item">
                <span class="top-rank">{{ idx + 1 }}</span>
                <span class="top-name">{{ it.teamName || '未知' }}</span>
                <span class="top-value warning">-{{ formatInt(it.amount || 0) }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="compact-card" style="margin-top: 8px">
          <div class="compact-header">
            <span class="compact-title">行为分布</span>
          </div>
          <div class="compact-body">
            <div ref="behaviorRef" class="chart-compact-small"></div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useDark } from '@vueuse/core'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getDashboardOverview } from '@/api/dashboard'

const router = useRouter()
const isDark = useDark()

const loading = ref(false)
const period = ref('week')
const overview = ref(null)

const trendRef = ref(null)
const behaviorRef = ref(null)
const popularRef = ref(null)
const heatmapRef = ref(null)

let trendChart
let behaviorChart
let popularChart
let heatmapChart

const coinPeriod = computed(() => {
  const coin = overview.value?.coin || {}
  return (
    coin?.[period.value] || {
      inflow: { total: 0, bySource: {} },
      outflow: { total: 0, bySource: {} }
    }
  )
})

const coinTrend = computed(() => {
  const coin = overview.value?.coin || {}
  return coin?.trend?.[period.value] || []
})

const netCoin = computed(() => {
  const inflow = coinPeriod.value?.inflow?.total || 0
  const outflow = coinPeriod.value?.outflow?.total || 0
  return inflow - outflow
})

const todayNet = computed(() => {
  const coin = overview.value?.coin || {}
  const today = coin?.today || { inflow: { total: 0 }, outflow: { total: 0 } }
  return (today.inflow?.total || 0) - (today.outflow?.total || 0)
})

const teams = computed(() => {
  return (
    overview.value?.teams || {
      totalTeams: 0,
      activatedTeams: 0,
      activeTop5: [],
      spendTop5: []
    }
  )
})

const activity = computed(() => {
  return (
    overview.value?.activity || {
      behaviorDistribution: [],
      popularResources: [],
      heatmap: { days: [], hours: [], data: [] }
    }
  )
})

const todos = computed(() => {
  return (
    overview.value?.todos || {
      pendingAchievements: 0,
      upcomingBookings: 0
    }
  )
})

function formatInt(v) {
  const n = Number(v || 0)
  return new Intl.NumberFormat('zh-CN').format(isFinite(n) ? n : 0)
}

function inflowLabel(key) {
  const map = {
    checkin: '打卡',
    achievement: '成果',
    duty: '值班',
    training: '培训'
  }
  return map[key] || key
}

function outflowLabel(key) {
  const map = {
    workstation: '工位',
    device: '设备',
    equipment: '器材',
    venue: '场地'
  }
  return map[key] || key
}

function cssVar(name, fallback = '') {
  const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return v || fallback
}

function toRgba(color, alpha) {
  if (!color) return `rgba(0,0,0,${alpha})`

  const c = String(color).trim()
  if (c.startsWith('rgba(')) {
    const inner = c.slice(5, -1)
    const parts = inner.split(',').map((x) => x.trim())
    if (parts.length >= 3) {
      return `rgba(${parts[0]}, ${parts[1]}, ${parts[2]}, ${alpha})`
    }
    return c
  }
  if (c.startsWith('rgb(')) {
    return c.replace('rgb(', 'rgba(').replace(')', `, ${alpha})`)
  }
  if (c.startsWith('#')) {
    let hex = c.slice(1)
    if (hex.length === 3) {
      hex = hex
        .split('')
        .map((ch) => ch + ch)
        .join('')
    }
    if (hex.length === 6) {
      const r = parseInt(hex.slice(0, 2), 16)
      const g = parseInt(hex.slice(2, 4), 16)
      const b = parseInt(hex.slice(4, 6), 16)
      return `rgba(${r}, ${g}, ${b}, ${alpha})`
    }
  }
  return c
}

function disposeCharts() {
  trendChart?.dispose()
  behaviorChart?.dispose()
  popularChart?.dispose()
  heatmapChart?.dispose()
  trendChart = undefined
  behaviorChart = undefined
  popularChart = undefined
  heatmapChart = undefined
}

function initChart(domRef) {
  const dom = domRef?.value
  if (!dom) return undefined
  return echarts.init(dom, isDark.value ? 'dark' : undefined)
}

function renderTrend() {
  if (!trendChart) trendChart = initChart(trendRef)
  if (!trendChart) return

  const points = coinTrend.value || []
  const x = points.map((p) => p.date)
  const inflow = points.map((p) => Number(p.inflow || 0))
  const outflow = points.map((p) => Number(p.outflow || 0))

  const primary = cssVar('--el-color-primary', '#409EFF')
  const success = cssVar('--el-color-success', '#67C23A')
  const warning = cssVar('--el-color-warning', '#E6A23C')
  const text = cssVar('--el-text-color-primary', '#303133')
  const text2 = cssVar('--el-text-color-secondary', '#909399')
  const border = cssVar('--el-border-color-lighter', '#ebeef5')

  trendChart.setOption(
    {
      textStyle: { color: text },
      tooltip: { trigger: 'axis' },
      legend: { data: ['流入', '流出'], textStyle: { color: text2 } },
      grid: { left: 8, right: 16, top: 30, bottom: 8, containLabel: true },
      xAxis: {
        type: 'category',
        data: x,
        axisLine: { lineStyle: { color: border } },
        axisLabel: { color: text2 }
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        splitLine: { lineStyle: { color: border } },
        axisLabel: { color: text2 }
      },
      series: [
        {
          name: '流入',
          type: 'line',
          smooth: true,
          showSymbol: false,
          data: inflow,
          lineStyle: { color: success, width: 2 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: toRgba(success, 0.35) },
              { offset: 1, color: toRgba(success, 0.03) }
            ])
          },
          itemStyle: { color: success }
        },
        {
          name: '流出',
          type: 'line',
          smooth: true,
          showSymbol: false,
          data: outflow,
          lineStyle: { color: warning, width: 2 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: toRgba(warning, 0.28) },
              { offset: 1, color: toRgba(warning, 0.03) }
            ])
          },
          itemStyle: { color: warning }
        }
      ],
      color: [primary]
    },
    true
  )
}

function renderBehavior() {
  if (!behaviorChart) behaviorChart = initChart(behaviorRef)
  if (!behaviorChart) return

  const data = (activity.value?.behaviorDistribution || []).map((it) => ({
    name: it.name,
    value: Number(it.value || 0)
  }))

  const text = cssVar('--el-text-color-primary', '#303133')
  const text2 = cssVar('--el-text-color-secondary', '#909399')
  const border = cssVar('--el-border-color-lighter', '#ebeef5')

  const colors = [
    cssVar('--el-color-primary', '#409EFF'),
    cssVar('--el-color-success', '#67C23A'),
    cssVar('--el-color-warning', '#E6A23C'),
    cssVar('--el-color-danger', '#F56C6C')
  ]

  behaviorChart.setOption(
    {
      textStyle: { color: text },
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, textStyle: { color: text2 } },
      series: [
        {
          type: 'pie',
          radius: ['45%', '70%'],
          avoidLabelOverlap: true,
          itemStyle: { borderColor: border, borderWidth: 1 },
          label: { show: false },
          emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
          labelLine: { show: false },
          data
        }
      ],
      color: colors
    },
    true
  )
}

function renderPopular() {
  if (!popularChart) popularChart = initChart(popularRef)
  if (!popularChart) return

  const list = activity.value?.popularResources || []
  const names = list.map((it) => it.name)
  const values = list.map((it) => Number(it.value || 0))

  const primary = cssVar('--el-color-primary', '#409EFF')
  const text = cssVar('--el-text-color-primary', '#303133')
  const text2 = cssVar('--el-text-color-secondary', '#909399')
  const border = cssVar('--el-border-color-lighter', '#ebeef5')

  popularChart.setOption(
    {
      textStyle: { color: text },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 8, right: 16, top: 10, bottom: 8, containLabel: true },
      xAxis: {
        type: 'value',
        axisLabel: { color: text2 },
        splitLine: { lineStyle: { color: border } }
      },
      yAxis: {
        type: 'category',
        data: names,
        axisLabel: { color: text2 },
        axisLine: { lineStyle: { color: border } }
      },
      series: [
        {
          type: 'bar',
          data: values,
          barWidth: 14,
          itemStyle: { color: primary, borderRadius: [6, 6, 6, 6] }
        }
      ]
    },
    true
  )
}

function renderHeatmap() {
  if (!heatmapChart) heatmapChart = initChart(heatmapRef)
  if (!heatmapChart) return

  const hm = activity.value?.heatmap || { days: [], hours: [], data: [] }
  const days = hm.days || []
  const hours = hm.hours || []
  const data = hm.data || []
  const max = data.reduce((m, it) => Math.max(m, Number(it?.[2] || 0)), 0) || 10

  const primary = cssVar('--el-color-primary', '#409EFF')
  const text = cssVar('--el-text-color-primary', '#303133')
  const text2 = cssVar('--el-text-color-secondary', '#909399')
  const border = cssVar('--el-border-color-lighter', '#ebeef5')

  heatmapChart.setOption(
    {
      textStyle: { color: text },
      tooltip: {
        position: 'top',
        formatter: (p) => {
          const v = p?.value || []
          const h = v[0]
          const d = v[1]
          const c = v[2]
          return `${days[d] || ''} ${String(h).padStart(2, '0')}:00<br/>活跃：${c}`
        }
      },
      grid: { left: 40, right: 16, top: 10, bottom: 24 },
      xAxis: {
        type: 'category',
        data: hours,
        axisLabel: { color: text2 },
        axisLine: { lineStyle: { color: border } },
        splitLine: { show: true, lineStyle: { color: border } }
      },
      yAxis: {
        type: 'category',
        data: days,
        axisLabel: { color: text2 },
        axisLine: { lineStyle: { color: border } },
        splitLine: { show: true, lineStyle: { color: border } }
      },
      visualMap: {
        min: 0,
        max,
        calculable: false,
        orient: 'horizontal',
        left: 'center',
        bottom: 0,
        inRange: {
          color: ['transparent', primary]
        },
        textStyle: { color: text2 }
      },
      series: [
        {
          type: 'heatmap',
          data,
          label: { show: false },
          emphasis: {
            itemStyle: {
              shadowBlur: 6,
              shadowColor: cssVar('--el-color-info', '#909399')
            }
          }
        }
      ]
    },
    true
  )
}

function renderAll() {
  renderTrend()
  renderBehavior()
  renderPopular()
  renderHeatmap()
}

async function fetchOverview() {
  loading.value = true
  try {
    overview.value = await getDashboardOverview()
    await nextTick()
    disposeCharts()
    renderAll()
  } catch (e) {
    ElMessage.error(e?.message || '获取仪表盘数据失败')
  } finally {
    loading.value = false
  }
}

function handleResize() {
  trendChart?.resize()
  behaviorChart?.resize()
  popularChart?.resize()
  heatmapChart?.resize()
}

function go(path) {
  router.push(path)
}

watch(period, async () => {
  await nextTick()
  renderTrend()
})

watch(
  isDark,
  async () => {
    await nextTick()
    disposeCharts()
    renderAll()
  },
  { flush: 'post' }
)

onMounted(async () => {
  await fetchOverview()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  disposeCharts()
})
</script>

<style scoped lang="scss">
.dashboard-container {
  padding: 0;
  
  // 顶部指标横条
  .metrics-bar {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 8px;
    margin-bottom: 8px;
  }

  .metric-card {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 16px;
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 6px;
    transition: all 0.2s;

    &.clickable {
      cursor: pointer;
      &:hover {
        border-color: var(--el-color-primary);
        transform: translateY(-1px);
      }
    }

    .metric-icon {
      width: 40px;
      height: 40px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--el-color-primary-light-9);
      color: var(--el-color-primary);
      font-size: 20px;

      &.success {
        background: var(--el-color-success-light-9);
        color: var(--el-color-success);
      }
      &.danger {
        background: var(--el-color-danger-light-9);
        color: var(--el-color-danger);
      }
      &.warning {
        background: var(--el-color-warning-light-9);
        color: var(--el-color-warning);
      }
    }

    .metric-content {
      flex: 1;
      min-width: 0;
    }

    .metric-label {
      font-size: 12px;
      color: var(--el-text-color-secondary);
      margin-bottom: 2px;
    }

    .metric-value {
      font-size: 20px;
      font-weight: 700;
      color: var(--el-text-color-primary);
    }
  }

  .main-content {
    margin-top: 0 !important;
  }

  // 紧凑卡片
  .compact-card {
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 6px;
    overflow: hidden;
  }

  .compact-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 12px;
    background: var(--el-fill-color-lighter);
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  .compact-title {
    font-size: 13px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  .compact-body {
    padding: 10px;
  }

  // 行内指标
  .inline-metrics {
    display: flex;
    gap: 16px;
    margin-bottom: 8px;
    padding: 8px 0;
    border-bottom: 1px dashed var(--el-border-color-lighter);
  }

  .inline-metric {
    display: flex;
    align-items: baseline;
    gap: 6px;

    .label {
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }

    .value {
      font-size: 16px;
      font-weight: 700;
      color: var(--el-text-color-primary);

      &.success {
        color: var(--el-color-success);
      }
      &.warning {
        color: var(--el-color-warning);
      }
    }
  }

  // 图表
  .chart-compact {
    height: 220px;
  }

  .chart-compact-small {
    height: 180px;
  }

  .chart-heatmap-compact {
    height: 200px;
  }

  // 明细网格
  .detail-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  .detail-section {
    .detail-title {
      font-size: 12px;
      font-weight: 600;
      color: var(--el-text-color-primary);
      margin-bottom: 6px;
      padding-bottom: 4px;
      border-bottom: 1px solid var(--el-border-color-lighter);
    }
  }

  .detail-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 4px 0;
    font-size: 12px;

    .detail-label {
      color: var(--el-text-color-secondary);
    }

    .detail-value {
      font-weight: 600;
      color: var(--el-text-color-primary);
    }
  }

  // Top榜
  .top-section {
    .top-label {
      font-size: 12px;
      font-weight: 600;
      color: var(--el-text-color-primary);
      margin-bottom: 6px;
    }
  }

  .compact-top-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 5px 0;
    font-size: 12px;

    .top-rank {
      width: 18px;
      height: 18px;
      border-radius: 4px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      background: var(--el-fill-color);
      color: var(--el-text-color-regular);
      font-size: 11px;
      font-weight: 600;
      flex-shrink: 0;

      &.rank-1 {
        background: linear-gradient(135deg, #ffd700 0%, #ffed4e 100%);
        color: #8b6914;
      }
      &.rank-2 {
        background: linear-gradient(135deg, #c0c0c0 0%, #e8e8e8 100%);
        color: #666;
      }
      &.rank-3 {
        background: linear-gradient(135deg, #cd7f32 0%, #e8a87c 100%);
        color: #5c3d1f;
      }
    }

    .top-name {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      color: var(--el-text-color-primary);
    }

    .top-value {
      font-weight: 600;
      flex-shrink: 0;

      &.success {
        color: var(--el-color-success);
      }
      &.warning {
        color: var(--el-color-warning);
      }
    }
  }

  .divider-line {
    height: 1px;
    background: var(--el-border-color-lighter);
    margin: 10px 0;
  }

  @media (max-width: 992px) {
    .metrics-bar {
      grid-template-columns: repeat(2, 1fr);
    }

    .detail-grid {
      grid-template-columns: 1fr;
    }
  }

  @media (max-width: 768px) {
    .metrics-bar {
      grid-template-columns: 1fr;
    }
  }
}
</style>
