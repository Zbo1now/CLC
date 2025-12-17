<template>
  <div class="resource-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card workstation-card" @click="activeTab = 'workstation'">
          <div class="stat-icon">
            <el-icon><OfficeBuilding /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-title">工位管理</div>
            <div class="stat-value">{{ stats.workstation?.total || 0 }}</div>
            <div class="stat-detail">
              <span>可用 {{ stats.workstation?.available || 0 }}</span>
              <span>已租 {{ stats.workstation?.rented || 0 }}</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card device-card" @click="activeTab = 'device'">
          <div class="stat-icon">
            <el-icon><Monitor /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-title">设备管理</div>
            <div class="stat-value">{{ stats.device?.total || 0 }}</div>
            <div class="stat-detail">
              <span>可用 {{ stats.device?.available || 0 }}</span>
              <span>使用中 {{ stats.device?.inUse || 0 }}</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card equipment-card" @click="activeTab = 'equipment'">
          <div class="stat-icon">
            <el-icon><Box /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-title">器材管理</div>
            <div class="stat-value">{{ stats.equipment?.total || 0 }}</div>
            <div class="stat-detail">
              <span>可用 {{ stats.equipment?.available || 0 }}</span>
              <span>已借 {{ stats.equipment?.borrowed || 0 }}</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card venue-card" @click="activeTab = 'venue'">
          <div class="stat-icon">
            <el-icon><House /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-title">场地管理</div>
            <div class="stat-value">{{ stats.venue?.total || 0 }}</div>
            <div class="stat-detail">
              <span>可用 {{ stats.venue?.available || 0 }}</span>
              <span>已订 {{ stats.venue?.booked || 0 }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- Tab切换内容 -->
    <el-card class="content-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="工位管理" name="workstation">
          <WorkstationManagement ref="workstationRef" @refresh="loadStats" />
        </el-tab-pane>
        <el-tab-pane label="设备管理" name="device">
          <DeviceManagement ref="deviceRef" @refresh="loadStats" />
        </el-tab-pane>
        <el-tab-pane label="器材管理" name="equipment">
          <EquipmentManagement ref="equipmentRef" @refresh="loadStats" />
        </el-tab-pane>
        <el-tab-pane label="场地管理" name="venue">
          <VenueManagement ref="venueRef" @refresh="loadStats" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { OfficeBuilding, Monitor, Box, House } from '@element-plus/icons-vue'
import { getResourceStats } from '@/api/resource'
import WorkstationManagement from './components/WorkstationManagement.vue'
import DeviceManagement from './components/DeviceManagement.vue'
import EquipmentManagement from './components/EquipmentManagement.vue'
import VenueManagement from './components/VenueManagement.vue'

const activeTab = ref('workstation')
const stats = ref({})

const loadStats = async () => {
  try {
    const res = await getResourceStats()
    stats.value = res.data
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const handleTabChange = (tabName) => {
  console.log('切换到:', tabName)
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped lang="scss">
.resource-container {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 24px;
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 20px;
  height: 120px;

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
  }

  &.device-card {
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  }

  &.equipment-card {
    background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  }

  &.venue-card {
    background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  }

  .stat-icon {
    font-size: 48px;
    opacity: 0.9;
  }

  .stat-content {
    flex: 1;

    .stat-title {
      font-size: 14px;
      opacity: 0.9;
      margin-bottom: 8px;
    }

    .stat-value {
      font-size: 32px;
      font-weight: bold;
      margin-bottom: 8px;
    }

    .stat-detail {
      font-size: 12px;
      opacity: 0.8;
      display: flex;
      gap: 12px;
    }
  }
}

.content-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

@media (max-width: 768px) {
  .stats-row {
    margin-bottom: 10px;
  }

  .stat-card {
    margin-bottom: 10px;
    height: auto;
    padding: 16px;
  }
}
</style>
