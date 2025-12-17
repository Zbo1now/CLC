<template>
  <div class="team-detail-container">
    <!-- 返回按钮 -->
    <div class="back-header">
      <el-button :icon="ArrowLeft" @click="handleBack">返回列表</el-button>
    </div>

    <div v-loading="loading" class="detail-content">
      <!-- 基础信息卡片 -->
      <el-card class="basic-card">
        <template #header>
          <div class="card-title">
            <el-icon><InfoFilled /></el-icon>
            <span>基础信息</span>
          </div>
        </template>

        <div class="basic-info">
          <div class="info-item">
            <span class="label">团队名称</span>
            <span class="value">{{ detail.teamName || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">联系人</span>
            <span class="value">{{ detail.contactName || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">联系方式</span>
            <span class="value">{{ detail.contactPhone || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">当前余额</span>
            <span class="value balance">{{ formatInt(detail.balance) }} 币</span>
          </div>
          <div class="info-item">
            <span class="label">连续打卡</span>
            <span class="value">{{ detail.currentStreak || 0 }} 天</span>
          </div>
          <div class="info-item">
            <span class="label">注册时间</span>
            <span class="value">{{ formatDateTime(detail.createdAt) }}</span>
          </div>
        </div>

        <div class="actions">
          <el-button type="primary" :icon="Key" @click="handleResetPassword">
            重置密码
          </el-button>
          <el-button type="success" :icon="CreditCard" @click="handleAdjustBalance">
            调整虚拟币
          </el-button>
          <el-button type="warning" :icon="Download" @click="handleExportReport">
            导出报告
          </el-button>
        </div>
      </el-card>

      <!-- 统计数据 -->
      <div class="stats-grid">
        <div class="stat-box">
          <div class="stat-icon success">
            <el-icon><Trophy /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ detail.totalAchievements || 0 }}</div>
            <div class="stat-label">累计成果</div>
          </div>
        </div>

        <div class="stat-box">
          <div class="stat-icon primary">
            <el-icon><Calendar /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ detail.totalCheckins || 0 }}</div>
            <div class="stat-label">累计打卡</div>
          </div>
        </div>

        <div class="stat-box">
          <div class="stat-icon warning">
            <el-icon><Briefcase /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ detail.totalDutyTasks || 0 }}</div>
            <div class="stat-label">累计值班</div>
          </div>
        </div>

        <div class="stat-box">
          <div class="stat-icon danger">
            <el-icon><Reading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ detail.totalTrainings || 0 }}</div>
            <div class="stat-label">累计培训</div>
          </div>
        </div>
      </div>

      <!-- 能力雷达图 -->
      <el-card class="radar-card">
        <template #header>
          <div class="card-title">
            <el-icon><DataAnalysis /></el-icon>
            <span>能力画像</span>
          </div>
        </template>

        <div ref="radarRef" class="radar-chart"></div>

        <div class="ability-legends">
          <div class="legend-item">
            <span class="legend-label">创新力</span>
            <span class="legend-value">{{ detail.innovationScore || 0 }}</span>
            <span class="legend-desc">成果总奖励</span>
          </div>
          <div class="legend-item">
            <span class="legend-label">活跃度</span>
            <span class="legend-value">{{ detail.activityScore || 0 }}</span>
            <span class="legend-desc">打卡频率</span>
          </div>
          <div class="legend-item">
            <span class="legend-label">资源利用</span>
            <span class="legend-value">{{ detail.resourceScore || 0 }}</span>
            <span class="legend-desc">消费合理性</span>
          </div>
          <div class="legend-item">
            <span class="legend-label">参与度</span>
            <span class="legend-value">{{ detail.participationScore || 0 }}</span>
            <span class="legend-desc">值班+培训</span>
          </div>
        </div>
      </el-card>

      <!-- 行为时间线 -->
      <el-card class="timeline-card">
        <template #header>
          <div class="card-title">
            <el-icon><Clock /></el-icon>
            <span>近期行为时间线</span>
          </div>
        </template>

        <el-tabs v-model="activeTab" class="timeline-tabs">
          <!-- 虚拟币流水 -->
          <el-tab-pane label="虚拟币流水" name="transactions">
            <div class="timeline-list">
              <div
                v-for="(item, idx) in timeline.transactions"
                :key="idx"
                class="timeline-item"
              >
                <div class="timeline-dot" :class="item.amount > 0 ? 'success' : 'danger'"></div>
                <div class="timeline-content">
                  <div class="timeline-header">
                    <span class="timeline-type">{{ getTxnTypeLabel(item.txn_type) }}</span>
                    <span :class="['timeline-amount', item.amount > 0 ? 'positive' : 'negative']">
                      {{ item.amount > 0 ? '+' : '' }}{{ item.amount }}
                    </span>
                  </div>
                  <div class="timeline-desc">{{ item.description }}</div>
                  <div class="timeline-time">{{ formatDateTime(item.created_at) }}</div>
                </div>
              </div>
              <el-empty v-if="!timeline.transactions || timeline.transactions.length === 0" description="暂无流水记录" />
            </div>
          </el-tab-pane>

          <!-- 成果申报 -->
          <el-tab-pane label="成果申报" name="achievements">
            <div class="timeline-list">
              <div
                v-for="(item, idx) in timeline.achievements"
                :key="idx"
                class="timeline-item"
              >
                <div class="timeline-dot" :class="getStatusClass(item.status)"></div>
                <div class="timeline-content">
                  <div class="timeline-header">
                    <span class="timeline-type">{{ item.title }}</span>
                    <el-tag :type="getStatusType(item.status)" size="small">
                      {{ getStatusLabel(item.status) }}
                    </el-tag>
                  </div>
                  <div class="timeline-desc">{{ item.description }}</div>
                  <div class="timeline-footer">
                    <span>奖励：{{ item.rewardCoins || 0 }} 币</span>
                    <span class="timeline-time">{{ formatDateTime(item.createdAt) }}</span>
                  </div>
                </div>
              </div>
              <el-empty v-if="!timeline.achievements || timeline.achievements.length === 0" description="暂无成果记录" />
            </div>
          </el-tab-pane>

          <!-- 资源预约 -->
          <el-tab-pane label="资源预约" name="bookings">
            <div class="timeline-list">
              <div
                v-for="(item, idx) in allBookings"
                :key="idx"
                class="timeline-item"
              >
                <div class="timeline-dot primary"></div>
                <div class="timeline-content">
                  <div class="timeline-header">
                    <span class="timeline-type">{{ item.type === 'device' ? '设备预约' : '场地预约' }}</span>
                    <el-tag type="info" size="small">{{ item.name }}</el-tag>
                  </div>
                  <div class="timeline-desc">
                    {{ formatDateTime(item.startTime) }} - {{ formatDateTime(item.endTime) }}
                  </div>
                  <div class="timeline-time">预约时间：{{ formatDateTime(item.createdAt) }}</div>
                </div>
              </div>
              <el-empty v-if="allBookings.length === 0" description="暂无预约记录" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="passwordDialogVisible" title="重置团队密码" width="500px">
      <el-form :model="passwordForm" label-width="100px">
        <el-form-item label="团队名称">
          <el-input v-model="detail.teamName" disabled />
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码（至少6位）" />
        </el-form-item>
        <el-form-item label="确认密码" required>
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" />
        </el-form-item>
        <el-form-item label="操作原因" required>
          <el-input v-model="passwordForm.reason" type="textarea" :rows="3" placeholder="请输入重置原因（必填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmResetPassword" :loading="resetting">
          确认重置
        </el-button>
      </template>
    </el-dialog>

    <!-- 调整虚拟币对话框 -->
    <el-dialog v-model="adjustDialogVisible" title="手动调整虚拟币" width="500px">
      <el-form :model="adjustForm" label-width="100px">
        <el-form-item label="团队名称">
          <el-input v-model="detail.teamName" disabled />
        </el-form-item>
        <el-form-item label="当前余额">
          <el-input :model-value="formatInt(detail.balance)" disabled>
            <template #append>币</template>
          </el-input>
        </el-form-item>
        <el-form-item label="调整金额" required>
          <el-input-number
            v-model="adjustForm.amount"
            :min="-10000"
            :max="10000"
            controls-position="right"
            style="width: 100%"
          />
          <div class="form-hint">正数为增加，负数为扣减</div>
        </el-form-item>
        <el-form-item label="操作原因" required>
          <el-input v-model="adjustForm.reason" type="textarea" :rows="3" placeholder="请输入调整原因（必填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmAdjust" :loading="adjusting">
          确认调整
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  InfoFilled,
  Key,
  CreditCard,
  Download,
  Trophy,
  Calendar,
  Briefcase,
  Reading,
  DataAnalysis,
  Clock
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getTeamDetail, getTeamTimeline, resetTeamPassword, adjustTeamBalance } from '@/api/team'

const route = useRoute()
const router = useRouter()
const teamId = ref(parseInt(route.params.id))

const loading = ref(false)
const resetting = ref(false)
const adjusting = ref(false)

const detail = reactive({
  teamName: '',
  contactName: '',
  contactPhone: '',
  balance: 0,
  currentStreak: 0,
  createdAt: null,
  totalAchievements: 0,
  totalCheckins: 0,
  totalDutyTasks: 0,
  totalTrainings: 0,
  innovationScore: 0,
  activityScore: 0,
  resourceScore: 0,
  participationScore: 0
})

const timeline = reactive({
  transactions: [],
  achievements: [],
  deviceBookings: [],
  venueBookings: []
})

const activeTab = ref('transactions')
const radarRef = ref(null)
let radarChart = null

// 对话框
const passwordDialogVisible = ref(false)
const passwordForm = reactive({
  newPassword: '',
  confirmPassword: '',
  reason: ''
})

const adjustDialogVisible = ref(false)
const adjustForm = reactive({
  amount: 0,
  reason: ''
})

const allBookings = computed(() => {
  return [...(timeline.deviceBookings || []), ...(timeline.venueBookings || [])]
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
})

function formatInt(v) {
  if (v == null) return '0'
  const n = Number(v || 0)
  return new Intl.NumberFormat('zh-CN').format(isFinite(n) ? n : 0)
}

function formatDateTime(dt) {
  if (!dt) return '-'
  const d = new Date(dt)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const txnTypeMap = {
  CHECKIN: '打卡',
  ACHIEVEMENT_REWARD: '成果奖励',
  DUTY_REWARD: '值班奖励',
  TRAINING_REWARD: '培训奖励',
  WORKSTATION_RENT: '工位租赁',
  WORKSTATION_RENEW: '工位续租',
  DEVICE_USAGE: '设备使用',
  EQUIPMENT_HOLD: '器材借用',
  VENUE_HOLD: '场地预约',
  ADMIN_ADJUST: '管理员调整'
}

function getTxnTypeLabel(type) {
  return txnTypeMap[type] || type
}

function getStatusLabel(status) {
  const map = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已拒绝'
  }
  return map[status] || status
}

function getStatusType(status) {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return map[status] || 'info'
}

function getStatusClass(status) {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return map[status] || 'primary'
}

async function fetchDetail() {
  loading.value = true
  try {
    const resp = await getTeamDetail(teamId.value)
    const data = resp?.data?.data || {}

    Object.assign(detail, data)

    // 初始化雷达图
    setTimeout(initRadar, 100)
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '加载团队详情失败')
  } finally {
    loading.value = false
  }
}

async function fetchTimeline() {
  try {
    const resp = await getTeamTimeline(teamId.value)
    const data = resp?.data?.data || {}

    timeline.transactions = data.transactions || []
    timeline.achievements = data.achievements || []
    timeline.deviceBookings = data.deviceBookings || []
    timeline.venueBookings = data.venueBookings || []
  } catch (e) {
    console.error('加载时间线失败', e)
  }
}

function initRadar() {
  if (!radarRef.value) return

  radarChart = echarts.init(radarRef.value)

  const option = {
    radar: {
      indicator: [
        { name: '创新力', max: 100 },
        { name: '活跃度', max: 100 },
        { name: '资源利用', max: 100 },
        { name: '参与度', max: 100 }
      ],
      splitArea: {
        areaStyle: {
          color: ['rgba(114, 172, 209, 0.05)', 'rgba(114, 172, 209, 0.1)']
        }
      }
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: [
              Math.min(100, detail.innovationScore || 0),
              detail.activityScore || 0,
              detail.resourceScore || 0,
              detail.participationScore || 0
            ],
            name: '能力值',
            areaStyle: {
              color: 'rgba(64, 158, 255, 0.3)'
            },
            lineStyle: {
              width: 2,
              color: '#409eff'
            }
          }
        ]
      }
    ]
  }

  radarChart.setOption(option)

  window.addEventListener('resize', () => {
    radarChart?.resize()
  })
}

function handleBack() {
  router.push('/team')
}

function handleResetPassword() {
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordForm.reason = ''
  passwordDialogVisible.value = true
}

async function handleConfirmResetPassword() {
  if (!passwordForm.newPassword || passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码长度至少6位')
    return
  }

  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  if (!passwordForm.reason || passwordForm.reason.trim() === '') {
    ElMessage.warning('请输入操作原因')
    return
  }

  resetting.value = true
  try {
    await resetTeamPassword(teamId.value, {
      newPassword: passwordForm.newPassword,
      reason: passwordForm.reason
    })

    ElMessage.success('密码重置成功')
    passwordDialogVisible.value = false
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '重置失败')
  } finally {
    resetting.value = false
  }
}

function handleAdjustBalance() {
  adjustForm.amount = 0
  adjustForm.reason = ''
  adjustDialogVisible.value = true
}

async function handleConfirmAdjust() {
  if (!adjustForm.amount || adjustForm.amount === 0) {
    ElMessage.warning('请输入调整金额')
    return
  }

  if (!adjustForm.reason || adjustForm.reason.trim() === '') {
    ElMessage.warning('请输入操作原因')
    return
  }

  adjusting.value = true
  try {
    await adjustTeamBalance(teamId.value, {
      amount: adjustForm.amount,
      reason: adjustForm.reason
    })

    ElMessage.success('虚拟币调整成功')
    adjustDialogVisible.value = false
    fetchDetail() // 刷新详情
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '调整失败')
  } finally {
    adjusting.value = false
  }
}

function handleExportReport() {
  ElMessageBox.confirm('导出团队能力报告功能开发中...', '提示', {
    confirmButtonText: '确定',
    showCancelButton: false,
    type: 'info'
  })
}

onMounted(() => {
  fetchDetail()
  fetchTimeline()
})
</script>

<style scoped lang="scss">
.team-detail-container {
  padding: 16px;
  background: var(--el-bg-color-page);
  min-height: calc(100vh - 110px);
}

.back-header {
  margin-bottom: 16px;
}

.detail-content {
  display: grid;
  gap: 16px;
}

.basic-card {
  .card-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
    font-size: 16px;
  }

  .basic-info {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 16px;
    margin-bottom: 20px;

    .info-item {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .label {
        font-size: 13px;
        color: var(--el-text-color-secondary);
      }

      .value {
        font-size: 16px;
        font-weight: 600;
        color: var(--el-text-color-primary);

        &.balance {
          color: var(--el-color-primary);
          font-size: 20px;
        }
      }
    }
  }

  .actions {
    display: flex;
    gap: 12px;
    padding-top: 16px;
    border-top: 1px dashed var(--el-border-color-lighter);
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;

  .stat-box {
    display: flex;
    align-items: center;
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    padding: 20px;
    transition: all 0.3s;

    &:hover {
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
      transform: translateY(-2px);
    }

    .stat-icon {
      width: 48px;
      height: 48px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      margin-right: 16px;

      &.success {
        background: rgba(103, 194, 58, 0.1);
        color: #67c23a;
      }

      &.primary {
        background: rgba(64, 158, 255, 0.1);
        color: #409eff;
      }

      &.warning {
        background: rgba(230, 162, 60, 0.1);
        color: #e6a23c;
      }

      &.danger {
        background: rgba(245, 108, 108, 0.1);
        color: #f56c6c;
      }
    }

    .stat-info {
      .stat-value {
        font-size: 24px;
        font-weight: 700;
        color: var(--el-text-color-primary);
      }

      .stat-label {
        font-size: 13px;
        color: var(--el-text-color-secondary);
        margin-top: 4px;
      }
    }
  }
}

.radar-card {
  .radar-chart {
    width: 100%;
    height: 400px;
  }

  .ability-legends {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 16px;
    margin-top: 20px;
    padding-top: 20px;
    border-top: 1px dashed var(--el-border-color-lighter);

    .legend-item {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .legend-label {
        font-size: 13px;
        color: var(--el-text-color-secondary);
      }

      .legend-value {
        font-size: 20px;
        font-weight: 700;
        color: var(--el-color-primary);
      }

      .legend-desc {
        font-size: 12px;
        color: var(--el-text-color-placeholder);
      }
    }
  }
}

.timeline-card {
  .timeline-tabs {
    :deep(.el-tabs__header) {
      margin-bottom: 20px;
    }
  }

  .timeline-list {
    .timeline-item {
      display: flex;
      gap: 16px;
      padding-bottom: 20px;
      position: relative;

      &:not(:last-child)::after {
        content: '';
        position: absolute;
        left: 7px;
        top: 28px;
        bottom: -12px;
        width: 2px;
        background: var(--el-border-color-lighter);
      }

      .timeline-dot {
        width: 16px;
        height: 16px;
        border-radius: 50%;
        margin-top: 4px;
        flex-shrink: 0;

        &.success {
          background: #67c23a;
        }

        &.danger {
          background: #f56c6c;
        }

        &.primary {
          background: #409eff;
        }

        &.warning {
          background: #e6a23c;
        }
      }

      .timeline-content {
        flex: 1;

        .timeline-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 8px;

          .timeline-type {
            font-weight: 600;
            color: var(--el-text-color-primary);
          }

          .timeline-amount {
            font-weight: 700;

            &.positive {
              color: #67c23a;
            }

            &.negative {
              color: #f56c6c;
            }
          }
        }

        .timeline-desc {
          font-size: 13px;
          color: var(--el-text-color-secondary);
          margin-bottom: 8px;
        }

        .timeline-footer {
          display: flex;
          justify-content: space-between;
          align-items: center;
          font-size: 12px;
          color: var(--el-text-color-secondary);
        }

        .timeline-time {
          font-size: 12px;
          color: var(--el-text-color-placeholder);
        }
      }
    }
  }
}

.form-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
</style>
