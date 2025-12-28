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
            <span class="label">账号状态</span>
            <span class="value">
              <el-tag :type="detail.enabled === false ? 'danger' : 'success'" size="small">
                {{ detail.enabled === false ? '已禁用' : '正常' }}
              </el-tag>
            </span>
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
          <el-button
            :type="detail.enabled === false ? 'success' : 'danger'"
            @click="handleToggleEnabled"
          >
            {{ detail.enabled === false ? '启用账号' : '禁用账号' }}
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

      <!-- 团队成员 -->
      <el-card class="members-card">
        <template #header>
          <div class="card-title">
            <el-icon><User /></el-icon>
            <span>团队成员</span>
          </div>
        </template>

        <div class="members-toolbar">
          <el-button type="primary" @click="openAddMemberDialog">添加成员</el-button>
        </div>

        <el-table :data="members" v-loading="membersLoading" stripe style="width: 100%">
          <el-table-column prop="id" label="ID" width="80" align="center" />
          <el-table-column prop="memberName" label="姓名" min-width="160" />
          <el-table-column prop="role" label="角色" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="row.role === 'LEADER' ? 'warning' : 'info'" size="small">
                {{ row.role === 'LEADER' ? '负责人' : '成员' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="phone" label="手机号" width="160" />
          <el-table-column prop="status" label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'INACTIVE' ? 'danger' : 'success'" size="small">
                {{ row.status === 'INACTIVE' ? '停用' : '正常' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170" align="center">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
        </el-table>
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

    <!-- 添加成员对话框 -->
    <el-dialog v-model="memberDialogVisible" title="添加团队成员" width="520px">
      <el-form :model="memberForm" label-width="90px">
        <el-form-item label="姓名" required>
          <el-input v-model="memberForm.memberName" placeholder="请输入成员姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="memberForm.phone" placeholder="选填" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="memberForm.role" style="width: 100%">
            <el-option label="成员" value="MEMBER" />
            <el-option label="负责人" value="LEADER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="memberDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addingMember" @click="handleConfirmAddMember">确认添加</el-button>
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
  Clock,
  User
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getTeamDetail, getTeamTimeline, resetTeamPassword, adjustTeamBalance, setTeamEnabled, getTeamMembers, addTeamMember } from '@/api/team'

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
  enabled: true,
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

// 团队成员
const members = ref([])
const membersLoading = ref(false)
const memberDialogVisible = ref(false)
const addingMember = ref(false)
const memberForm = reactive({
  memberName: '',
  phone: '',
  role: 'MEMBER'
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
    const data = resp?.data || {}

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
    const data = resp?.data || {}

    timeline.transactions = data.transactions || []
    timeline.achievements = data.achievements || []
    timeline.deviceBookings = data.deviceBookings || []
    timeline.venueBookings = data.venueBookings || []
  } catch (e) {
    console.error('加载时间线失败', e)
  }
}

async function fetchMembers() {
  membersLoading.value = true
  try {
    const resp = await getTeamMembers(teamId.value)
    members.value = resp?.data?.list || []
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '加载成员列表失败')
  } finally {
    membersLoading.value = false
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

async function handleToggleEnabled() {
  const currentEnabled = detail.enabled !== false
  const nextEnabled = !currentEnabled

  try {
    let reason = ''
    if (!nextEnabled) {
      const r = await ElMessageBox.prompt('请输入禁用原因（可选）', '禁用账号', {
        confirmButtonText: '确认禁用',
        cancelButtonText: '取消',
        inputPlaceholder: '例如：违规操作/账号异常'
      })
      reason = r?.value || ''
    } else {
      await ElMessageBox.confirm('确认启用该账号吗？', '启用账号', {
        confirmButtonText: '确认启用',
        cancelButtonText: '取消',
        type: 'warning'
      })
    }

    await setTeamEnabled(teamId.value, { enabled: nextEnabled, reason })
    detail.enabled = nextEnabled
    ElMessage.success(nextEnabled ? '已启用' : '已禁用')
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

function openAddMemberDialog() {
  memberForm.memberName = ''
  memberForm.phone = ''
  memberForm.role = 'MEMBER'
  memberDialogVisible.value = true
}

async function handleConfirmAddMember() {
  const memberName = String(memberForm.memberName || '').trim()
  if (!memberName) {
    ElMessage.warning('请输入成员姓名')
    return
  }

  addingMember.value = true
  try {
    await addTeamMember(teamId.value, {
      memberName,
      phone: memberForm.phone,
      role: memberForm.role
    })
    ElMessage.success('新增成功')
    memberDialogVisible.value = false
    fetchMembers()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '新增失败')
  } finally {
    addingMember.value = false
  }
}

onMounted(() => {
  fetchDetail()
  fetchTimeline()
  fetchMembers()
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
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 12px;
    max-width: 760px;
    margin: 0 auto;
    padding-top: 16px;
    border-top: 1px dashed var(--el-border-color-lighter);

    :deep(.el-button) {
      width: 100%;
    }
  }
}

@media (max-width: 900px) {
  .basic-card {
    .actions {
      grid-template-columns: repeat(2, minmax(0, 1fr));
      max-width: 520px;
    }
  }
}

@media (max-width: 520px) {
  .basic-card {
    .actions {
      grid-template-columns: 1fr;
      max-width: 320px;
    }
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

.members-card {
  .card-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
    font-size: 16px;
  }

  .members-toolbar {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 12px;
  }
}
</style>
