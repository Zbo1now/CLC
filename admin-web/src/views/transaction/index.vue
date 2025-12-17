<template>
  <div class="transaction-container">
    <!-- 筛选工具栏 -->
    <div class="filter-panel">
      <el-form :model="filters" :inline="true" class="filter-form">
        <el-form-item label="团队名称">
          <el-input
            v-model="filters.teamName"
            placeholder="输入团队名称搜索"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
            style="width: 200px"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="变动类型">
          <el-select
            v-model="filters.txnTypes"
            multiple
            placeholder="选择类型"
            clearable
            collapse-tags
            style="width: 240px"
          >
            <el-option label="打卡" value="CHECKIN" />
            <el-option label="成果奖励" value="ACHIEVEMENT_REWARD" />
            <el-option label="值班奖励" value="DUTY_REWARD" />
            <el-option label="培训奖励" value="TRAINING_REWARD" />
            <el-option label="工位租赁" value="WORKSTATION_RENT" />
            <el-option label="工位续租" value="WORKSTATION_RENEW" />
            <el-option label="设备使用" value="DEVICE_USAGE" />
            <el-option label="器材借用" value="EQUIPMENT_HOLD" />
            <el-option label="场地预约" value="VENUE_HOLD" />
          </el-select>
        </el-form-item>

        <el-form-item label="金额方向">
          <el-select v-model="filters.direction" placeholder="选择方向" style="width: 120px">
            <el-option label="全部" value="all" />
            <el-option label="收入" value="inflow" />
            <el-option label="支出" value="outflow" />
          </el-select>
        </el-form-item>

        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch" :icon="Search">查询</el-button>
          <el-button @click="handleReset" :icon="RefreshLeft">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="action-bar">
        <el-button type="success" :icon="Download" @click="handleExport" :loading="exporting">
          导出 CSV
        </el-button>
        <div class="stats">
          <el-tag type="info">
            <el-icon><Document /></el-icon>
            共 {{ pagination.total }} 条记录
          </el-tag>
        </div>
      </div>
    </div>

    <!-- 流水表格 -->
    <div class="table-wrapper">
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
        :height="tableHeight"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        
        <el-table-column prop="teamName" label="团队名称" min-width="140">
          <template #default="{ row }">
            <div class="team-cell">
              <el-icon class="team-icon"><User /></el-icon>
              <span>{{ row.teamName || '未知团队' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="txnType" label="变动类型" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="getTxnTypeTag(row.txnType)" effect="plain" size="small">
              {{ getTxnTypeLabel(row.txnType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="amount" label="变动金额" width="130" align="right">
          <template #default="{ row }">
            <span :class="getAmountClass(row.amount)">
              {{ formatAmount(row.amount) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="balanceAfter" label="操作后余额" width="130" align="right">
          <template #default="{ row }">
            <span class="balance">{{ formatInt(row.balanceAfter) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />

        <el-table-column prop="refId" label="关联ID" width="100" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.refId"
              link
              type="primary"
              size="small"
              @click="handleRefClick(row)"
            >
              {{ row.refId }}
            </el-button>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="变动时间" width="160" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[20, 50, 100, 200]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        class="pagination"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Download, Document, User } from '@element-plus/icons-vue'
import http from '@/api/http'

const loading = ref(false)
const exporting = ref(false)
const tableData = ref([])

const filters = reactive({
  teamName: '',
  txnTypes: [],
  direction: 'all',
  dateRange: null
})

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

const tableHeight = computed(() => {
  return window.innerHeight - 320
})

// 变动类型标签颜色映射
const txnTypeMap = {
  CHECKIN: { label: '打卡', tag: 'success' },
  ACHIEVEMENT_REWARD: { label: '成果奖励', tag: 'success' },
  DUTY_REWARD: { label: '值班奖励', tag: 'success' },
  TRAINING_REWARD: { label: '培训奖励', tag: 'success' },
  WORKSTATION_RENT: { label: '工位租赁', tag: 'warning' },
  WORKSTATION_RENEW: { label: '工位续租', tag: 'warning' },
  DEVICE_USAGE: { label: '设备使用', tag: 'warning' },
  EQUIPMENT_HOLD: { label: '器材借用', tag: 'warning' },
  VENUE_HOLD: { label: '场地预约', tag: 'warning' }
}

function getTxnTypeLabel(type) {
  return txnTypeMap[type]?.label || type
}

function getTxnTypeTag(type) {
  return txnTypeMap[type]?.tag || 'info'
}

function getAmountClass(amount) {
  if (amount > 0) return 'amount-positive'
  if (amount < 0) return 'amount-negative'
  return ''
}

function formatAmount(amount) {
  if (amount > 0) return `+${amount}`
  return amount || 0
}

function formatInt(v) {
  if (v == null) return '-'
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
      page: pagination.page,
      pageSize: pagination.pageSize
    }

    if (filters.teamName) params.teamName = filters.teamName
    if (filters.txnTypes && filters.txnTypes.length > 0) params.txnTypes = filters.txnTypes
    if (filters.direction) params.direction = filters.direction
    if (filters.dateRange && filters.dateRange.length === 2) {
      params.startDate = filters.dateRange[0]
      params.endDate = filters.dateRange[1]
    }

    const resp = await http.get('/admin/transactions', { params })
    const data = resp?.data?.data || {}
    
    tableData.value = data.list || []
    pagination.total = data.total || 0
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '加载流水记录失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchList()
}

function handleReset() {
  filters.teamName = ''
  filters.txnTypes = []
  filters.direction = 'all'
  filters.dateRange = null
  handleSearch()
}

function handlePageChange() {
  fetchList()
}

function handleSizeChange() {
  pagination.page = 1
  fetchList()
}

async function handleExport() {
  try {
    await ElMessageBox.confirm('确认导出当前筛选条件下的所有流水记录？', '导出确认', {
      confirmButtonText: '导出',
      cancelButtonText: '取消',
      type: 'info'
    })

    exporting.value = true
    ElMessage.info('正在生成 CSV 文件...')

    // 调用接口获取全量数据（暂不分页）
    const params = { page: 1, pageSize: 10000 }
    if (filters.teamName) params.teamName = filters.teamName
    if (filters.txnTypes && filters.txnTypes.length > 0) params.txnTypes = filters.txnTypes
    if (filters.direction) params.direction = filters.direction
    if (filters.dateRange && filters.dateRange.length === 2) {
      params.startDate = filters.dateRange[0]
      params.endDate = filters.dateRange[1]
    }

    const resp = await http.get('/admin/transactions', { params })
    const data = resp?.data?.data || {}
    const list = data.list || []

    if (list.length === 0) {
      ElMessage.warning('没有可导出的数据')
      return
    }

    // 生成 CSV 内容
    const headers = ['ID', '团队名称', '变动类型', '变动金额', '操作后余额', '说明', '关联ID', '关联类型', '变动时间']
    const rows = list.map((row) => [
      row.id,
      row.teamName || '',
      getTxnTypeLabel(row.txnType),
      row.amount,
      row.balanceAfter || '',
      row.description || '',
      row.refId || '',
      row.refType || '',
      formatDateTime(row.createdAt)
    ])

    const csv = [headers, ...rows].map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(',')).join('\n')
    const bom = '\uFEFF'
    const blob = new Blob([bom + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `虚拟币流水_${new Date().toISOString().slice(0, 10)}.csv`
    link.click()
    URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('导出失败')
    }
  } finally {
    exporting.value = false
  }
}

function handleRefClick(row) {
  // 根据 refType 跳转到对应详情页
  if (!row.refType || !row.refId) {
    ElMessage.info('该记录无关联详情')
    return
  }

  // 可根据实际业务扩展跳转逻辑
  if (row.refType === 'achievement') {
    ElMessage.info(`跳转至成果详情：ID=${row.refId}（功能待实现）`)
    // router.push(`/audit/${row.refId}`)
  } else {
    ElMessage.info(`关联类型：${row.refType}，ID：${row.refId}`)
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped lang="scss">
.transaction-container {
  padding: 16px;
  background: var(--el-bg-color-page);
  min-height: calc(100vh - 110px);
}

.filter-panel {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;

  .filter-form {
    :deep(.el-form-item) {
      margin-bottom: 12px;
    }
  }

  .action-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px dashed var(--el-border-color-lighter);

    .stats {
      display: flex;
      gap: 12px;
    }
  }
}

.table-wrapper {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 16px;

  .team-cell {
    display: flex;
    align-items: center;
    gap: 6px;

    .team-icon {
      color: var(--el-color-primary);
    }
  }

  .amount-positive {
    color: var(--el-color-success);
    font-weight: 600;
  }

  .amount-negative {
    color: var(--el-color-danger);
    font-weight: 600;
  }

  .balance {
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  .text-muted {
    color: var(--el-text-color-placeholder);
  }

  .pagination {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
