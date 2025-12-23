<template>
  <div class="audit-container">
    <!-- 筛选表单 -->
    <el-card class="filter-card" shadow="hover">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="审核状态">
          <el-select v-model="filterForm.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="成果类型">
          <el-select v-model="filterForm.category" placeholder="全部类型" clearable style="width: 140px">
            <el-option label="论文发表" value="论文发表" />
            <el-option label="专利申请" value="专利申请" />
            <el-option label="竞赛获奖" value="竞赛获奖" />
            <el-option label="科研项目" value="科研项目" />
            <el-option label="其他成果" value="其他成果" />
          </el-select>
        </el-form-item>
        <el-form-item label="团队名称">
          <el-input v-model="filterForm.teamName" placeholder="搜索团队" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <el-card class="stat-card" :body-style="{ padding: '20px' }">
        <div class="stat-item">
          <div class="stat-icon pending">
            <el-icon :size="32"><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.pending }}</div>
            <div class="stat-label">待审核</div>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card" :body-style="{ padding: '20px' }">
        <div class="stat-item">
          <div class="stat-icon approved">
            <el-icon :size="32"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.approved }}</div>
            <div class="stat-label">已通过</div>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card" :body-style="{ padding: '20px' }">
        <div class="stat-item">
          <div class="stat-icon rejected">
            <el-icon :size="32"><CircleClose /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.rejected }}</div>
            <div class="stat-label">已驳回</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 成果列表 -->
    <el-card class="table-card" shadow="hover">
      <el-table
        v-loading="loading"
        :data="achievementList"
        style="width: 100%"
        empty-text="暂无数据"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="teamName" label="团队名称" width="150" />
        <el-table-column label="成果类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getCategoryType(row.category)" size="small">
              {{ row.category }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="成果标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="奖励币值" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.rewardCoins" class="reward-coins">
              <el-icon><Coin /></el-icon>
              {{ row.rewardCoins }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              :icon="View"
              link
              @click="handleViewDetail(row)"
            >
              查看详情
            </el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              type="success"
              size="small"
              :icon="Select"
              link
              @click="handleApprove(row)"
            >
              通过
            </el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              type="danger"
              size="small"
              :icon="CloseBold"
              link
              @click="handleReject(row)"
            >
              驳回
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 成果详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="成果详情"
      width="800px"
      :close-on-click-modal="false"
    >
      <div v-if="currentAchievement" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="成果ID">{{ currentAchievement.id }}</el-descriptions-item>
          <el-descriptions-item label="团队名称">{{ currentAchievement.teamName }}</el-descriptions-item>
          <el-descriptions-item label="成果类型">
            <el-tag :type="getCategoryType(currentAchievement.category)" size="small">
              {{ currentAchievement.category }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="成果标题" :span="2">{{ currentAchievement.title }}</el-descriptions-item>
          <el-descriptions-item label="成果描述" :span="2">
            <div class="description-text">{{ currentAchievement.description || '-' }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="附件" :span="2">
            <el-link
              v-if="currentAchievement.id && currentAchievement.proofUrl"
              :href="`/api/admin/achievements/${currentAchievement.id}/attachment`"
              target="_blank"
              type="primary"
              :icon="Link"
            >
              查看附件
            </el-link>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentAchievement.status)" size="small">
              {{ getStatusText(currentAchievement.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="奖励币值">
            <span v-if="currentAchievement.rewardCoins" class="reward-coins">
              <el-icon><Coin /></el-icon>
              {{ currentAchievement.rewardCoins }}
            </span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间" :span="2">
            {{ formatTime(currentAchievement.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="currentAchievement.reviewedAt" label="审核时间" :span="2">
            {{ formatTime(currentAchievement.reviewedAt) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="currentAchievement.reviewedBy" label="审核人">
            {{ currentAchievement.reviewedBy }}
          </el-descriptions-item>
          <el-descriptions-item v-if="currentAchievement.rejectReason" label="驳回原因" :span="2">
            <el-alert type="error" :closable="false" :title="currentAchievement.rejectReason" />
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          v-if="currentAchievement && currentAchievement.status === 'PENDING'"
          type="success"
          :icon="Select"
          @click="handleApprove(currentAchievement)"
        >
          通过审核
        </el-button>
        <el-button
          v-if="currentAchievement && currentAchievement.status === 'PENDING'"
          type="danger"
          :icon="CloseBold"
          @click="handleReject(currentAchievement)"
        >
          驳回
        </el-button>
      </template>
    </el-dialog>

    <!-- 审核通过对话框 -->
    <el-dialog
      v-model="approveDialogVisible"
      title="审核通过"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="approveForm" :rules="approveRules" ref="approveFormRef" label-width="100px">
        <el-form-item label="成果标题">
          <div class="form-item-value">{{ currentAchievement?.title }}</div>
        </el-form-item>
        <el-form-item label="团队名称">
          <div class="form-item-value">{{ currentAchievement?.teamName }}</div>
        </el-form-item>
        <el-form-item label="奖励币值" prop="rewardCoins">
          <el-input-number
            v-model="approveForm.rewardCoins"
            :min="1"
            :max="10000"
            :step="10"
            placeholder="请输入奖励币值"
            style="width: 100%"
          />
          <div class="form-tip">* 审核通过后将自动给团队发放相应币值</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="submitting" @click="handleConfirmApprove">
          确认通过
        </el-button>
      </template>
    </el-dialog>

    <!-- 审核驳回对话框 -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="审核驳回"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="rejectForm" :rules="rejectRules" ref="rejectFormRef" label-width="100px">
        <el-form-item label="成果标题">
          <div class="form-item-value">{{ currentAchievement?.title }}</div>
        </el-form-item>
        <el-form-item label="团队名称">
          <div class="form-item-value">{{ currentAchievement?.teamName }}</div>
        </el-form-item>
        <el-form-item label="驳回原因" prop="rejectReason">
          <el-input
            v-model="rejectForm.rejectReason"
            type="textarea"
            :rows="4"
            placeholder="请输入驳回原因，将通知团队"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="handleConfirmReject">
          确认驳回
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search,
  Refresh,
  View,
  Select,
  CloseBold,
  Clock,
  CircleCheck,
  CircleClose,
  Coin,
  Link
} from '@element-plus/icons-vue'
import { getAchievementList, approveAchievement, rejectAchievement } from '@/api/achievement'
import dayjs from 'dayjs'

// 筛选表单
const filterForm = reactive({
  status: '',
  category: '',
  teamName: ''
})

// 统计数据
const stats = reactive({
  pending: 0,
  approved: 0,
  rejected: 0
})

// 列表数据
const loading = ref(false)
const achievementList = ref([])

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

// 对话框
const detailDialogVisible = ref(false)
const approveDialogVisible = ref(false)
const rejectDialogVisible = ref(false)
const currentAchievement = ref(null)

// 审核通过表单
const approveFormRef = ref(null)
const approveForm = reactive({
  rewardCoins: 100
})
const approveRules = {
  rewardCoins: [
    { required: true, message: '请输入奖励币值', trigger: 'blur' },
    { type: 'number', min: 1, max: 10000, message: '币值范围：1-10000', trigger: 'blur' }
  ]
}

// 审核驳回表单
const rejectFormRef = ref(null)
const rejectForm = reactive({
  rejectReason: ''
})
const rejectRules = {
  rejectReason: [
    { required: true, message: '请输入驳回原因', trigger: 'blur' },
    { min: 5, max: 500, message: '驳回原因长度：5-500字', trigger: 'blur' }
  ]
}

const submitting = ref(false)

// 加载成果列表
const loadAchievementList = async () => {
  loading.value = true
  try {
    const params = {
      status: filterForm.status || undefined,
      category: filterForm.category || undefined,
      teamName: filterForm.teamName || undefined,
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    const res = await getAchievementList(params)
    if (res.success) {
      achievementList.value = res.data.items
      pagination.total = res.data.total
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch (error) {
    console.error('加载成果列表失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 加载统计数据
const loadStats = async () => {
  try {
    // 加载三种状态的统计数据
    const [pendingRes, approvedRes, rejectedRes] = await Promise.all([
      getAchievementList({ status: 'PENDING', page: 1, pageSize: 1 }),
      getAchievementList({ status: 'APPROVED', page: 1, pageSize: 1 }),
      getAchievementList({ status: 'REJECTED', page: 1, pageSize: 1 })
    ])
    stats.pending = pendingRes.data?.total || 0
    stats.approved = approvedRes.data?.total || 0
    stats.rejected = rejectedRes.data?.total || 0
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadAchievementList()
}

// 重置
const handleReset = () => {
  filterForm.status = ''
  filterForm.category = ''
  filterForm.teamName = ''
  pagination.page = 1
  loadAchievementList()
}

// 分页变化
const handleSizeChange = () => {
  loadAchievementList()
}

const handlePageChange = () => {
  loadAchievementList()
}

// 查看详情
const handleViewDetail = (row) => {
  currentAchievement.value = row
  detailDialogVisible.value = true
}

// 审核通过
const handleApprove = (row) => {
  currentAchievement.value = row
  approveForm.rewardCoins = row.rewardCoins || 100
  detailDialogVisible.value = false
  approveDialogVisible.value = true
}

// 确认通过
const handleConfirmApprove = async () => {
  if (!approveFormRef.value) return
  await approveFormRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      const res = await approveAchievement(currentAchievement.value.id, approveForm.rewardCoins)
      if (res.success) {
        ElMessage.success('审核通过并发币成功')
        approveDialogVisible.value = false
        loadAchievementList()
        loadStats()
      } else {
        ElMessage.error(res.message || '审核失败')
      }
    } catch (error) {
      console.error('审核通过失败:', error)
      ElMessage.error('审核失败')
    } finally {
      submitting.value = false
    }
  })
}

// 审核驳回
const handleReject = (row) => {
  currentAchievement.value = row
  rejectForm.rejectReason = ''
  detailDialogVisible.value = false
  rejectDialogVisible.value = true
}

// 确认驳回
const handleConfirmReject = async () => {
  if (!rejectFormRef.value) return
  await rejectFormRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      const res = await rejectAchievement(currentAchievement.value.id, rejectForm.rejectReason)
      if (res.success) {
        ElMessage.success('驳回成功')
        rejectDialogVisible.value = false
        loadAchievementList()
        loadStats()
      } else {
        ElMessage.error(res.message || '驳回失败')
      }
    } catch (error) {
      console.error('驳回失败:', error)
      ElMessage.error('驳回失败')
    } finally {
      submitting.value = false
    }
  })
}

// 工具函数
// 子类型友好显示
const getSubTypeText = (category, subType) => {
  if (!subType || subType === 'NULL' || subType === 'null' || subType === null) return '-';
  if (category === 'PATENT' || category === '专利申请') {
    if (subType === 'ACCEPTED') return '受理';
    if (subType === 'GRANTED') return '授权';
  }
  if (category === 'COMPETITION' || category === '竞赛获奖') {
    if (subType === 'SCHOOL') return '校级';
    if (subType === 'PROVINCE') return '省级';
    if (subType === 'NATIONAL') return '国家级';
  }
  return '-';
}
const formatTime = (time) => {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

const getStatusType = (status) => {
  const typeMap = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status) => {
  const textMap = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已驳回'
  }
  return textMap[status] || status
}

const getCategoryType = (category) => {
  const typeMap = {
    '论文发表': 'success',
    '专利申请': 'primary',
    '竞赛获奖': 'warning',
    '科研项目': 'info',
    '其他成果': 'default'
  }
  return typeMap[category] || 'default'
}

onMounted(() => {
  loadAchievementList()
  loadStats()
})
</script>

<style scoped>
.audit-container {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

.filter-form {
  margin-bottom: 0;
}

/* 统计卡片 */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 12px;
  transition: all 0.3s;
  border: none;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-icon.pending {
  background: linear-gradient(135deg, #ffd740 0%, #ff9100 100%);
}

.stat-icon.approved {
  background: linear-gradient(135deg, #66bb6a 0%, #43a047 100%);
}

.stat-icon.rejected {
  background: linear-gradient(135deg, #ef5350 0%, #e53935 100%);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

/* 表格卡片 */
.table-card {
  border-radius: 12px;
}

.reward-coins {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #f39c12;
  font-weight: 600;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

/* 详情对话框 */
.detail-content {
  max-height: 600px;
  overflow-y: auto;
}

.description-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
}

.form-item-value {
  color: #303133;
  font-weight: 500;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stat-cards {
    grid-template-columns: 1fr;
  }
}
</style>
