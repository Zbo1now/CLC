<template>
  <div class="team-container">
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索团队名称或联系人"
        clearable
        @clear="handleSearch"
        @keyup.enter="handleSearch"
        style="width: 300px"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <div class="toolbar-right">
        <el-select v-model="orderBy" placeholder="排序字段" style="width: 140px" @change="handleSearch">
          <el-option label="注册时间" value="createdAt" />
          <el-option label="虚拟币余额" value="balance" />
          <el-option label="本周活跃度" value="weeklyCheckins" />
          <el-option label="成果数量" value="totalAchievements" />
        </el-select>

        <el-select v-model="orderDir" placeholder="排序方向" style="width: 100px" @change="handleSearch">
          <el-option label="降序" value="DESC" />
          <el-option label="升序" value="ASC" />
        </el-select>

        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon primary">
          <el-icon><UserFilled /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ pagination.total }}</div>
          <div class="stat-label">团队总数</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon success">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ totalCheckins }}</div>
          <div class="stat-label">本周打卡总数</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon warning">
          <el-icon><Trophy /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ totalAchievements }}</div>
          <div class="stat-label">累计成果数</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon danger">
          <el-icon><Histogram /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ totalBalance }}</div>
          <div class="stat-label">币流通总量</div>
        </div>
      </div>
    </div>

    <!-- 团队列表 -->
    <div class="table-wrapper">
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        :height="tableHeight"
        @row-click="handleRowClick"
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />

        <el-table-column prop="teamName" label="团队名称" min-width="150">
          <template #default="{ row }">
            <div class="team-name-cell">
              <el-icon class="team-icon"><User /></el-icon>
              <span class="team-name">{{ row.teamName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="contactName" label="联系人" width="120" />

        <el-table-column prop="contactPhone" label="联系方式" width="140" />

        <el-table-column prop="balance" label="虚拟币余额" width="130" align="right" sortable>
          <template #default="{ row }">
            <span class="balance-value">{{ formatInt(row.balance) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="weeklyCheckins" label="本周活跃度" width="120" align="center" sortable>
          <template #default="{ row }">
            <el-tag :type="getActivityTag(row.weeklyCheckins)" size="small">
              {{ row.weeklyCheckins || 0 }} 次
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="totalAchievements" label="成果数量" width="110" align="center" sortable>
          <template #default="{ row }">
            <span class="achievement-value">{{ row.totalAchievements || 0 }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="weeklyResourceUsage" label="本周资源使用" width="130" align="center">
          <template #default="{ row }">
            {{ row.weeklyResourceUsage || 0 }} 次
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="注册时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="handleViewDetail(row.id)">
              <el-icon><View /></el-icon>
              查看详情
            </el-button>
            <el-button link type="success" size="small" @click.stop="handleAdjustBalance(row)">
              <el-icon><CreditCard /></el-icon>
              调币
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        class="pagination"
      />
    </div>

    <!-- 调整虚拟币对话框 -->
    <el-dialog
      v-model="adjustDialogVisible"
      title="手动调整虚拟币"
      width="500px"
    >
      <el-form :model="adjustForm" label-width="100px">
        <el-form-item label="团队名称">
          <el-input v-model="adjustForm.teamName" disabled />
        </el-form-item>
        <el-form-item label="当前余额">
          <el-input v-model="adjustForm.currentBalance" disabled>
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
          <div class="form-hint">
            正数为增加，负数为扣减
          </div>
        </el-form-item>
        <el-form-item label="操作原因" required>
          <el-input
            v-model="adjustForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入调整原因（必填）"
          />
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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Search,
  RefreshLeft,
  UserFilled,
  TrendCharts,
  Trophy,
  Histogram,
  User,
  View,
  CreditCard
} from '@element-plus/icons-vue'
import { getTeamList, adjustTeamBalance } from '@/api/team'

const router = useRouter()
const loading = ref(false)
const adjusting = ref(false)
const tableData = ref([])

const keyword = ref('')
const orderBy = ref('createdAt')
const orderDir = ref('DESC')

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

// 统计数据
const totalCheckins = ref(0)
const totalAchievements = ref(0)
const totalBalance = ref(0)

// 调整虚拟币对话框
const adjustDialogVisible = ref(false)
const adjustForm = reactive({
  teamId: null,
  teamName: '',
  currentBalance: 0,
  amount: 0,
  reason: ''
})

const tableHeight = computed(() => {
  return window.innerHeight - 420
})

function getActivityTag(count) {
  if (count >= 5) return 'success'
  if (count >= 3) return 'warning'
  return 'info'
}

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

async function fetchList() {
  loading.value = true
  try {
    const params = {
      keyword: keyword.value,
      orderBy: orderBy.value,
      orderDir: orderDir.value,
      page: pagination.page,
      pageSize: pagination.pageSize
    }

    const resp = await getTeamList(params)
    const data = resp?.data || {}

    tableData.value = data.list || []
    pagination.total = data.total || 0

    // 计算统计数据
    totalCheckins.value = tableData.value.reduce((sum, item) => sum + (item.weeklyCheckins || 0), 0)
    totalAchievements.value = tableData.value.reduce((sum, item) => sum + (item.totalAchievements || 0), 0)
    totalBalance.value = tableData.value.reduce((sum, item) => sum + (item.balance || 0), 0)
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '加载团队列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchList()
}

function handleReset() {
  keyword.value = ''
  orderBy.value = 'createdAt'
  orderDir.value = 'DESC'
  handleSearch()
}

function handlePageChange() {
  fetchList()
}

function handleSizeChange() {
  pagination.page = 1
  fetchList()
}

function handleRowClick(row) {
  handleViewDetail(row.id)
}

function handleViewDetail(teamId) {
  router.push(`/team/${teamId}`)
}

function handleAdjustBalance(row) {
  adjustForm.teamId = row.id
  adjustForm.teamName = row.teamName
  adjustForm.currentBalance = row.balance || 0
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
    await adjustTeamBalance(adjustForm.teamId, {
      amount: adjustForm.amount,
      reason: adjustForm.reason
    })

    ElMessage.success('虚拟币调整成功')
    adjustDialogVisible.value = false
    fetchList() // 刷新列表
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '调整失败')
  } finally {
    adjusting.value = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped lang="scss">
.team-container {
  padding: 16px;
  background: var(--el-bg-color-page);
  min-height: calc(100vh - 110px);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;

  .toolbar-right {
    display: flex;
    gap: 12px;
    align-items: center;
  }
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  margin-bottom: 16px;

  .stat-card {
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
      width: 56px;
      height: 56px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28px;
      margin-right: 16px;

      &.primary {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
      }

      &.success {
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
        color: white;
      }

      &.warning {
        background: linear-gradient(135deg, #fad0c4 0%, #ffd1ff 100%);
        color: #e65100;
      }

      &.danger {
        background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
        color: #d32f2f;
      }
    }

    .stat-content {
      flex: 1;

      .stat-value {
        font-size: 28px;
        font-weight: 700;
        color: var(--el-text-color-primary);
        line-height: 1.2;
      }

      .stat-label {
        font-size: 13px;
        color: var(--el-text-color-secondary);
        margin-top: 4px;
      }
    }
  }
}

.table-wrapper {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 16px;

  :deep(.el-table) {
    .el-table__row {
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        background: var(--el-fill-color-light);
      }
    }
  }

  .team-name-cell {
    display: flex;
    align-items: center;
    gap: 8px;

    .team-icon {
      color: var(--el-color-primary);
      font-size: 16px;
    }

    .team-name {
      font-weight: 600;
      color: var(--el-text-color-primary);
    }
  }

  .balance-value {
    font-weight: 700;
    color: var(--el-color-primary);
  }

  .achievement-value {
    font-weight: 600;
    color: var(--el-color-warning);
  }

  .pagination {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}

.form-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
</style>
