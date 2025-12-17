<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span>虚拟币总流通</span>
              <el-tag type="success">日</el-tag>
            </div>
          </template>
          <div class="card-content">
            <span class="number">¥ 12,345</span>
            <div class="sub-info">
              <span>收入: 8,000</span>
              <span style="margin-left: 10px">支出: 4,345</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span>活跃团队</span>
              <el-tag type="warning">周</el-tag>
            </div>
          </template>
          <div class="card-content">
            <span class="number">28</span>
            <div class="sub-info">
              <span>较上周 +12%</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span>待审核成果</span>
              <el-tag type="danger">急</el-tag>
            </div>
          </template>
          <div class="card-content">
            <span class="number">5</span>
            <div class="sub-info">
              <el-button link type="primary">立即处理</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span>资源占用率</span>
              <el-tag>实时</el-tag>
            </div>
          </template>
          <div class="card-content">
            <span class="number">85%</span>
            <el-progress :percentage="85" :show-text="false" status="warning" style="margin-top: 10px"/>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>团队活跃度趋势</span>
            </div>
          </template>
          <div ref="chartRef" style="height: 350px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>热门资源 Top 5</span>
            </div>
          </template>
          <el-table :data="topResources" style="width: 100%" :show-header="false">
            <el-table-column type="index" width="50" />
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="usage" label="使用次数" align="right">
              <template #default="scope">
                <el-tag size="small">{{ scope.row.usage }} 次</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'

const chartRef = ref(null)

const topResources = [
  { name: '3D 打印机 A', usage: 120 },
  { name: '会议室 101', usage: 98 },
  { name: '高性能工作站', usage: 85 },
  { name: '激光切割机', usage: 66 },
  { name: 'VR 设备套装', usage: 45 },
]

onMounted(() => {
  const chart = echarts.init(chartRef.value)
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '活跃度',
        type: 'line',
        smooth: true,
        areaStyle: {},
        data: [120, 132, 101, 134, 90, 230, 210],
        itemStyle: {
          color: '#409EFF'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,158,255,0.5)' },
            { offset: 1, color: 'rgba(64,158,255,0.01)' }
          ])
        }
      }
    ]
  }
  chart.setOption(option)
  
  window.addEventListener('resize', () => {
    chart.resize()
  })
})
</script>

<style scoped lang="scss">
.dashboard-container {
  .data-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .card-content {
      .number {
        font-size: 24px;
        font-weight: bold;
        display: block;
        margin-bottom: 10px;
      }
      .sub-info {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }
  }
}
</style>
