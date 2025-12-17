<template>
  <div class="app-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-button @click="handleBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <h2 class="page-title">{{ activityName }} - 参与记录</h2>
    </div>

    <!-- 参与记录列表 -->
    <el-card class="main-card">
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <div class="filter-left">
          <el-select
            v-model="filters.reviewStatus"
            placeholder="审核状态"
            clearable
            style="width: 150px"
            @change="handleFilterChange"
          >
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>

          <el-input
            v-model="filters.searchKeyword"
            placeholder="搜索团队名称"
            clearable
            style="width: 250px"
            @change="handleFilterChange"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <el-button
          type="success"
          :disabled="selectedIds.length === 0"
          @click="handleBatchApprove"
        >
          <el-icon><Select /></el-icon>
          批量审核通过 ({{ selectedIds.length }})
        </el-button>
      </div>

      <!-- 表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        empty-text="暂无数据"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" :selectable="isSelectable" />
        <el-table-column prop="teamName" label="团队名称" min-width="150" />
        <el-table-column prop="applyTime" label="报名时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.applyTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="reviewStatus" label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getReviewStatusType(row.reviewStatus)">
              {{ getReviewStatusText(row.reviewStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="参与状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewedBy" label="审核人" width="120">
          <template #default="{ row }">
            {{ row.reviewedBy || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="reviewedAt" label="审核时间" width="160">
          <template #default="{ row }">
            {{ row.reviewedAt ? formatDateTime(row.reviewedAt) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="rejectReason" label="驳回原因" min-width="150">
          <template #default="{ row }">
            <span v-if="row.rejectReason" class="reject-reason">
              {{ row.rejectReason }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="coinsRewarded" label="已发币" width="90">
          <template #default="{ row }">
            <span v-if="row.coinsRewarded > 0" class="coin-value">
              {{ row.coinsRewarded }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.reviewStatus === 'PENDING'"
              type="success"
              size="small"
              link
              @click="handleApprove(row)"
            >
              通过
            </el-button>
            <el-button
              v-if="row.reviewStatus === 'PENDING'"
              type="danger"
              size="small"
              link
              @click="handleReject(row)"
            >
              驳回
            </el-button>
            <el-button
              v-if="row.reviewStatus === 'APPROVED' && row.status !== 'COMPLETED'"
              type="primary"
              size="small"
              link
              @click="handleComplete(row)"
            >
              标记完成
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 驳回对话框 -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="审核驳回"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules">
        <el-form-item label="驳回原因" prop="rejectReason">
          <el-input
            v-model="rejectForm.rejectReason"
            type="textarea"
            :rows="4"
            placeholder="请输入驳回原因"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="handleRejectSubmit">
          确定驳回
        </el-button>
      </template>
    </el-dialog>

    <!-- 标记完成对话框 -->
    <el-dialog
      v-model="completeDialogVisible"
      title="标记已完成"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form ref="completeFormRef" :model="completeForm">
        <el-form-item label="完成备注">
          <el-input
            v-model="completeForm.completionNotes"
            type="textarea"
            :rows="4"
            placeholder="请输入完成备注（可选）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCompleteSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Search, Select } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import {
  getParticipationList,
  approveParticipation,
  rejectParticipation,
  batchApproveParticipation,
  completeParticipation
} from '@/api/activity'

const route = useRoute()
const router = useRouter()

// 活动信息
const activityId = ref(parseInt(route.query.activityId))
const activityName = ref(route.query.activityName || '活动')

// 筛选条件
const filters = reactive({
  reviewStatus: '',
  searchKeyword: ''
})

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 表格数据
const loading = ref(false)
const tableData = ref([])
const selectedIds = ref([])

// 驳回对话框
const rejectDialogVisible = ref(false)
const rejectFormRef = ref(null)
const rejectForm = reactive({
  id: null,
  rejectReason: ''
})
const rejectRules = {
  rejectReason: [
    { required: true, message: '请输入驳回原因', trigger: 'blur' }
  ]
}

// 完成对话框
const completeDialogVisible = ref(false)
const completeFormRef = ref(null)
const completeForm = reactive({
  id: null,
  completionNotes: ''
})

const submitting = ref(false)

// 格式化日期时间
const formatDateTime = (dateTime) => {
  return dayjs(dateTime).format('YYYY-MM-DD HH:mm')
}

// 获取审核状态类型
const getReviewStatusType = (status) => {
  const typeMap = {
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  }
  return typeMap[status] || ''
}

// 获取审核状态文本
const getReviewStatusText = (status) => {
  const textMap = {
    'PENDING': '待审核',
    'APPROVED': '已通过',
    'REJECTED': '已驳回'
  }
  return textMap[status] || status
}

// 获取参与状态类型
const getStatusType = (status) => {
  const typeMap = {
    'PENDING': 'info',
    'APPROVED': 'success',
    'REJECTED': 'danger',
    'COMPLETED': 'primary'
  }
  return typeMap[status] || ''
}

// 获取参与状态文本
const getStatusText = (status) => {
  const textMap = {
    'PENDING': '待审核',
    'APPROVED': '已通过',
    'REJECTED': '已驳回',
    'COMPLETED': '已完成'
  }
  return textMap[status] || status
}

// 是否可选择
const isSelectable = (row) => {
  return row.reviewStatus === 'PENDING'
}

// 加载参与记录列表
const loadParticipationList = async () => {
  loading.value = true
  try {
    const res = await getParticipationList({
      activityId: activityId.value,
      reviewStatus: filters.reviewStatus || null,
      searchKeyword: filters.searchKeyword || null,
      page: pagination.page,
      pageSize: pagination.pageSize
    })

    if (res.success) {
      tableData.value = res.data.items
      pagination.total = res.data.total
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch (error) {
    console.error('加载参与记录列表失败:', error)
    ElMessage.error('加载失败，请重试')
  } finally {
    loading.value = false
  }
}

// 筛选变化
const handleFilterChange = () => {
  pagination.page = 1
  loadParticipationList()
}

// 分页变化
const handlePageChange = () => {
  loadParticipationList()
}

const handleSizeChange = () => {
  pagination.page = 1
  loadParticipationList()
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

// 返回
const handleBack = () => {
  router.back()
}

// 审核通过
const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定审核通过团队"${row.teamName}"的报名申请吗？通过后将自动发放奖励虚拟币`,
      '审核通过确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'success'
      }
    )

    const res = await approveParticipation(row.id)
    if (res.success) {
      ElMessage.success('审核通过成功，已发放奖励币')
      loadParticipationList()
    } else {
      ElMessage.error(res.message || '审核通过失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('审核通过失败:', error)
      ElMessage.error('审核通过失败，请重试')
    }
  }
}

// 审核驳回
const handleReject = (row) => {
  rejectForm.id = row.id
  rejectForm.rejectReason = ''
  rejectDialogVisible.value = true
}

// 提交驳回
const handleRejectSubmit = async () => {
  const valid = await rejectFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await rejectParticipation(rejectForm.id, rejectForm.rejectReason)
    if (res.success) {
      ElMessage.success('审核驳回成功')
      rejectDialogVisible.value = false
      loadParticipationList()
    } else {
      ElMessage.error(res.message || '审核驳回失败')
    }
  } catch (error) {
    console.error('审核驳回失败:', error)
    ElMessage.error('审核驳回失败，请重试')
  } finally {
    submitting.value = false
  }
}

// 批量审核通过
const handleBatchApprove = async () => {
  try {
    await ElMessageBox.confirm(
      `确定批量审核通过选中的 ${selectedIds.value.length} 条记录吗？通过后将自动发放奖励虚拟币`,
      '批量审核通过确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'success'
      }
    )

    const res = await batchApproveParticipation(selectedIds.value)
    if (res.success) {
      const { successCount, failCount } = res.data
      ElMessage.success(`批量审核完成：成功 ${successCount} 条，失败 ${failCount} 条`)
      loadParticipationList()
    } else {
      ElMessage.error(res.message || '批量审核失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量审核失败:', error)
      ElMessage.error('批量审核失败，请重试')
    }
  }
}

// 标记完成
const handleComplete = (row) => {
  completeForm.id = row.id
  completeForm.completionNotes = ''
  completeDialogVisible.value = true
}

// 提交标记完成
const handleCompleteSubmit = async () => {
  submitting.value = true
  try {
    const res = await completeParticipation(completeForm.id, completeForm.completionNotes)
    if (res.success) {
      ElMessage.success('标记已完成成功')
      completeDialogVisible.value = false
      loadParticipationList()
    } else {
      ElMessage.error(res.message || '标记已完成失败')
    }
  } catch (error) {
    console.error('标记已完成失败:', error)
    ElMessage.error('标记已完成失败，请重试')
  } finally {
    submitting.value = false
  }
}

// 初始化
onMounted(() => {
  loadParticipationList()
})
</script>

<style scoped lang="scss">
.app-container {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.main-card {
  border-radius: 8px;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-left {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.reject-reason {
  color: var(--el-color-danger);
}

.coin-value {
  color: #f59e0b;
  font-weight: 600;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
