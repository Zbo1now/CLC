<template>
  <div class="system-config-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          <i class="el-icon-setting"></i>
          系统配置
        </h2>
        <p class="page-description">动态调整系统参数，无需重启服务即可生效</p>
      </div>
      <div class="header-right">
        <el-button 
          type="primary" 
          :icon="Refresh" 
          @click="handleRefreshCache"
          :loading="refreshing">
          刷新缓存
        </el-button>
      </div>
    </div>

    <!-- 分类标签页 -->
    <el-tabs v-model="activeCategory" class="config-tabs" @tab-click="handleTabClick">
      <el-tab-pane 
        v-for="category in categories" 
        :key="category.value" 
        :label="category.label" 
        :name="category.value">
        <template #label>
          <span class="tab-label">
            <i :class="category.icon"></i>
            {{ category.label }}
          </span>
        </template>

        <!-- 配置卡片列表 -->
        <div v-loading="loading" class="config-grid">
          <div 
            v-for="config in filteredConfigs" 
            :key="config.id" 
            class="config-card"
            @click="handleEditConfig(config)">
            <div class="card-header">
              <div class="card-icon" :style="{ background: category.color }">
                <i :class="getConfigIcon(config.configKey)"></i>
              </div>
              <div class="card-info">
                <h3 class="config-name">{{ config.displayName }}</h3>
                <p class="config-key">{{ config.configKey }}</p>
              </div>
            </div>
            <div class="card-body">
              <div class="config-value">
                <span class="value-number">{{ config.configValue }}</span>
                <span class="value-unit">{{ config.unit }}</span>
              </div>
              <p v-if="config.description" class="config-desc">{{ config.description }}</p>
              <div v-if="config.minValue !== null || config.maxValue !== null" class="config-range">
                <span>范围：</span>
                <span class="range-value">
                  {{ config.minValue !== null ? config.minValue : '无限制' }} - 
                  {{ config.maxValue !== null ? config.maxValue : '无限制' }}
                </span>
              </div>
            </div>
            <div class="card-footer">
              <span class="update-time">
                <i class="el-icon-time"></i>
                {{ formatTime(config.updatedAt) }}
              </span>
              <span v-if="config.updatedBy" class="updated-by">
                <i class="el-icon-user"></i>
                {{ config.updatedBy }}
              </span>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="!loading && filteredConfigs.length === 0" class="empty-state">
            <i class="el-icon-document"></i>
            <p>暂无配置项</p>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 编辑配置对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="currentConfig?.displayName"
      width="500px"
      :before-close="handleCloseDialog">
      <div v-if="currentConfig" class="dialog-content">
        <div class="config-detail">
          <div class="detail-item">
            <label>配置键：</label>
            <span class="detail-value">{{ currentConfig.configKey }}</span>
          </div>
          <div v-if="currentConfig.description" class="detail-item">
            <label>说明：</label>
            <span class="detail-value">{{ currentConfig.description }}</span>
          </div>
          <div v-if="currentConfig.minValue !== null || currentConfig.maxValue !== null" class="detail-item">
            <label>取值范围：</label>
            <span class="detail-value">
              {{ currentConfig.minValue !== null ? currentConfig.minValue : '无限制' }} ~ 
              {{ currentConfig.maxValue !== null ? currentConfig.maxValue : '无限制' }}
            </span>
          </div>
        </div>

        <el-form ref="formRef" :model="editForm" :rules="formRules" label-width="80px">
          <el-form-item label="配置值" prop="configValue">
            <el-input-number 
              v-if="currentConfig.valueType === 'NUMBER'"
              v-model="editForm.configValue"
              :min="currentConfig.minValue"
              :max="currentConfig.maxValue"
              :step="1"
              style="width: 100%">
            </el-input-number>
            <el-input 
              v-else
              v-model="editForm.configValue"
              placeholder="请输入配置值">
            </el-input>
            <span v-if="currentConfig.unit" class="input-unit">{{ currentConfig.unit }}</span>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveConfig" :loading="saving">
            保存
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getSystemConfigs, updateConfig, refreshCache } from '@/api/systemConfig'

// 配置分类
const categories = [
  { 
    value: 'REWARD', 
    label: '激励配置', 
    icon: 'el-icon-coin',
    color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
  },
  { 
    value: 'ACTIVITY', 
    label: '活动配置', 
    icon: 'el-icon-present',
    color: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
  },
  { 
    value: 'RESOURCE', 
    label: '资源配置', 
    icon: 'el-icon-box',
    color: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)'
  },
  { 
    value: 'SYSTEM', 
    label: '系统配置', 
    icon: 'el-icon-setting',
    color: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)'
  }
]

// 状态
const activeCategory = ref('REWARD')
const loading = ref(false)
const refreshing = ref(false)
const configs = ref({})
const dialogVisible = ref(false)
const currentConfig = ref(null)
const saving = ref(false)

// 编辑表单
const editForm = ref({
  configValue: ''
})

const formRef = ref(null)

const formRules = {
  configValue: [
    { required: true, message: '请输入配置值', trigger: 'blur' }
  ]
}

// 计算属性
const filteredConfigs = computed(() => {
  return configs.value[activeCategory.value] || []
})

// 获取配置图标
const getConfigIcon = (configKey) => {
  const iconMap = {
    'reward.checkin': 'el-icon-success',
    'reward.achievement.patent': 'el-icon-trophy',
    'reward.achievement.paper': 'el-icon-document',
    'reward.duty.default': 'el-icon-briefcase',
    'resource.workstation.default_rate': 'el-icon-office-building',
    'resource.device.default_rate': 'el-icon-monitor'
  }
  return iconMap[configKey] || 'el-icon-coin'
}

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return '未知'
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  
  return date.toLocaleDateString()
}

// 加载配置
const loadConfigs = async () => {
  loading.value = true
  try {
    const res = await getSystemConfigs()
    if (res.code === 0) {
      configs.value = res.data || {}
    } else {
      ElMessage.error(res.message || '加载配置失败')
    }
  } catch (error) {
    console.error('加载配置失败:', error)
    ElMessage.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

// 标签切换
const handleTabClick = () => {
  // 标签切换时可以做一些操作
}

// 编辑配置
const handleEditConfig = (config) => {
  currentConfig.value = { ...config }
  editForm.value.configValue = config.valueType === 'NUMBER' 
    ? parseInt(config.configValue) 
    : config.configValue
  dialogVisible.value = true
}

// 保存配置
const handleSaveConfig = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    saving.value = true
    try {
      const res = await updateConfig(
        currentConfig.value.id, 
        editForm.value.configValue.toString()
      )
      if (res.code === 0) {
        ElMessage.success('保存成功')
        dialogVisible.value = false
        await loadConfigs() // 重新加载配置
      } else {
        ElMessage.error(res.message || '保存失败')
      }
    } catch (error) {
      console.error('保存配置失败:', error)
      ElMessage.error('保存失败')
    } finally {
      saving.value = false
    }
  })
}

// 关闭对话框
const handleCloseDialog = () => {
  dialogVisible.value = false
  currentConfig.value = null
  editForm.value.configValue = ''
}

// 刷新缓存
const handleRefreshCache = async () => {
  try {
    await ElMessageBox.confirm(
      '刷新缓存后，所有配置将立即生效。是否继续？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    refreshing.value = true
    const res = await refreshCache()
    if (res.code === 0) {
      ElMessage.success('缓存刷新成功')
      await loadConfigs()
    } else {
      ElMessage.error(res.message || '缓存刷新失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('刷新缓存失败:', error)
      ElMessage.error('缓存刷新失败')
    }
  } finally {
    refreshing.value = false
  }
}

// 初始化
onMounted(() => {
  loadConfigs()
})
</script>

<style scoped lang="scss">
.system-config-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 120px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);

  .header-left {
    .page-title {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
      color: #303133;
      display: flex;
      align-items: center;
      gap: 10px;

      i {
        font-size: 28px;
        color: #409eff;
      }
    }

    .page-description {
      margin: 8px 0 0 0;
      font-size: 14px;
      color: #909399;
    }
  }
}

.config-tabs {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);

  :deep(.el-tabs__header) {
    margin-bottom: 24px;
  }

  :deep(.el-tabs__item) {
    font-size: 15px;
    font-weight: 500;
  }

  .tab-label {
    display: flex;
    align-items: center;
    gap: 8px;

    i {
      font-size: 16px;
    }
  }
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  min-height: 300px;
}

.config-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  border: 2px solid #f0f2f5;
  transition: all 0.3s ease;
  cursor: pointer;

  &:hover {
    border-color: #409eff;
    box-shadow: 0 4px 20px rgba(64, 158, 255, 0.15);
    transform: translateY(-4px);
  }

  .card-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;

    .card-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-size: 24px;
      flex-shrink: 0;
    }

    .card-info {
      flex: 1;
      min-width: 0;

      .config-name {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
        color: #303133;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .config-key {
        margin: 4px 0 0 0;
        font-size: 12px;
        color: #909399;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }

  .card-body {
    .config-value {
      display: flex;
      align-items: baseline;
      gap: 8px;
      margin-bottom: 12px;

      .value-number {
        font-size: 32px;
        font-weight: 700;
        color: #409eff;
        line-height: 1;
      }

      .value-unit {
        font-size: 14px;
        color: #909399;
      }
    }

    .config-desc {
      margin: 0 0 8px 0;
      font-size: 13px;
      color: #606266;
      line-height: 1.6;
    }

    .config-range {
      font-size: 13px;
      color: #909399;
      padding: 8px 12px;
      background: #f5f7fa;
      border-radius: 6px;

      .range-value {
        color: #606266;
        font-weight: 500;
      }
    }
  }

  .card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid #f0f2f5;
    font-size: 12px;
    color: #909399;

    span {
      display: flex;
      align-items: center;
      gap: 4px;

      i {
        font-size: 14px;
      }
    }
  }
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #909399;

  i {
    font-size: 64px;
    margin-bottom: 16px;
    opacity: 0.5;
  }

  p {
    margin: 0;
    font-size: 14px;
  }
}

.dialog-content {
  .config-detail {
    margin-bottom: 24px;
    padding: 16px;
    background: #f5f7fa;
    border-radius: 8px;

    .detail-item {
      display: flex;
      margin-bottom: 8px;
      font-size: 14px;

      &:last-child {
        margin-bottom: 0;
      }

      label {
        min-width: 80px;
        color: #909399;
        font-weight: 500;
      }

      .detail-value {
        flex: 1;
        color: #606266;
      }
    }
  }

  .input-unit {
    margin-left: 8px;
    color: #909399;
    font-size: 14px;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
