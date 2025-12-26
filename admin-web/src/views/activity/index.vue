<template>
  <div class="app-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card-primary">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32"><Calendar /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.total }}</div>
              <div class="stat-label">全部活动</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card-warning">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32"><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.notStarted }}</div>
              <div class="stat-label">未开始</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card-success">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32"><VideoPlay /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.ongoing }}</div>
              <div class="stat-label">进行中</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card-info">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32"><Select /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.finished }}</div>
              <div class="stat-label">已结束</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 活动列表 -->
    <el-card class="main-card">
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <div class="filter-left">
          <el-select
            v-model="filters.activityType"
            placeholder="活动类型"
            clearable
            style="width: 150px"
            @change="handleFilterChange"
          >
            <el-option label="值班任务" value="DUTY" />
            <el-option label="培训/会议" value="TRAINING" />
          </el-select>

          <el-select
            v-model="filters.status"
            placeholder="活动状态"
            clearable
            style="width: 150px"
            @change="handleFilterChange"
          >
            <el-option label="未开始" value="NOT_STARTED" />
            <el-option label="进行中" value="ONGOING" />
            <el-option label="已结束" value="FINISHED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>

          <el-input
            v-model="filters.searchKeyword"
            placeholder="搜索活动名称或地点"
            clearable
            style="width: 250px"
            @change="handleFilterChange"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          发布活动
        </el-button>
      </div>

      <!-- 表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        empty-text="暂无数据"
      >
        <el-table-column prop="activityName" label="活动名称" min-width="180" />
        <el-table-column prop="activityType" label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="row.activityType === 'DUTY' ? 'primary' : 'success'">
              {{ row.activityType === 'DUTY' ? '值班任务' : '培训/会议' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="地点" min-width="130" />
        <el-table-column prop="startTime" label="开始时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="参与情况" width="140">
          <template #default="{ row }">
            <div class="participation-info">
              <span>{{ row.currentParticipants }} / {{ row.maxParticipants }}</span>
              <el-progress
                :percentage="Math.round((row.currentParticipants / row.maxParticipants) * 100)"
                :stroke-width="4"
                :show-text="false"
                style="margin-top: 4px"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="审核统计" width="140">
          <template #default="{ row }">
            <div class="review-stats">
              <el-tooltip content="待审核" placement="top">
                <el-tag size="small" type="warning">{{ row.pendingCount }}</el-tag>
              </el-tooltip>
              <el-tooltip content="已通过" placement="top">
                <el-tag size="small" type="success">{{ row.approvedCount }}</el-tag>
              </el-tooltip>
              <el-tooltip content="已完成" placement="top">
                <el-tag size="small" type="info">{{ row.completedCount }}</el-tag>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="rewardCoins" label="奖励币" width="90">
          <template #default="{ row }">
            <span class="coin-value">{{ row.rewardCoins }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              link
              @click="handleViewParticipations(row)"
            >
              参与记录
            </el-button>
            <el-button
              v-if="row.status === 'NOT_STARTED'"
              type="primary"
              size="small"
              link
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'NOT_STARTED' || row.status === 'ONGOING'"
              type="warning"
              size="small"
              link
              @click="handleCancel(row)"
            >
              下线
            </el-button>
            <el-button
              v-if="row.status === 'NOT_STARTED'"
              type="danger"
              size="small"
              link
              @click="handleDelete(row)"
            >
              删除
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

    <!-- 创建/编辑活动对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '发布活动' : '编辑活动'"
      width="700px"
      :close-on-click-modal="false"
    >
      <div class="centered-form-wrapper">
        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-width="120px"
        >
        <el-form-item label="活动名称" prop="activityName">
          <el-input v-model="formData.activityName" placeholder="请输入活动名称" />
        </el-form-item>

        <el-form-item label="活动类型" prop="activityType">
          <el-radio-group v-model="formData.activityType">
            <el-radio label="DUTY">值班任务</el-radio>
            <el-radio label="TRAINING">培训/会议</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="活动描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="4"
            placeholder="请输入活动描述"
          />
        </el-form-item>

        <el-form-item label="活动地点" prop="location">
          <el-input v-model="formData.location" placeholder="请输入活动地点" />
        </el-form-item>

        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="formData.startTime"
            type="datetime"
            placeholder="选择开始时间"
            style="width: 100%"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>

        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="formData.endTime"
            type="datetime"
            placeholder="选择结束时间"
            style="width: 100%"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>

        <el-form-item label="奖励币值" prop="rewardCoins">
          <el-input-number
            v-model="formData.rewardCoins"
            :min="0"
            :step="10"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="参与方式" prop="participationType">
          <el-radio-group v-model="formData.participationType">
            <el-radio label="MANUAL_REVIEW">人工审核</el-radio>
          </el-radio-group>
          <!-- <div class="form-tip">团队报名后需要管理员审核通过</div> -->
        </el-form-item>

        <el-form-item label="最大参与团队" prop="maxParticipants">
          <el-input-number
            v-model="formData.maxParticipants"
            :min="1"
            :step="1"
            style="width: 100%"
          />
        </el-form-item>

        </el-form>
      </div>

      <template #footer>
        <div class="centered-form-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ dialogMode === 'create' ? '创建' : '更新' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Calendar,
  Clock,
  VideoPlay,
  Select,
  Search,
  Plus
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import {
  getActivityList,
  createActivity,
  updateActivity,
  updateActivityStatus,
  deleteActivity
} from '@/api/activity'
import { useRouter } from 'vue-router'

const router = useRouter()

// 筛选条件
const filters = reactive({
  activityType: '',
  status: '',
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

// 统计数据
const stats = reactive({
  total: 0,
  notStarted: 0,
  ongoing: 0,
  finished: 0
})

// 对话框
const dialogVisible = ref(false)
const dialogMode = ref('create') // 'create' | 'edit'
const formRef = ref(null)
const submitting = ref(false)

// 表单数据
const formData = reactive({
  activityName: '',
  activityType: 'DUTY',
  description: '',
  location: '',
  startTime: '',
  endTime: '',
  rewardCoins: 0,
  participationType: 'MANUAL_REVIEW',
  maxParticipants: 10
})

// 表单验证规则
const formRules = {
  activityName: [
    { required: true, message: '请输入活动名称', trigger: 'blur' }
  ],
  activityType: [
    { required: true, message: '请选择活动类型', trigger: 'change' }
  ],
  location: [
    { required: true, message: '请输入活动地点', trigger: 'blur' }
  ],
  startTime: [
    { required: true, message: '请选择开始时间', trigger: 'change' }
  ],
  endTime: [
    { required: true, message: '请选择结束时间', trigger: 'change' }
  ],
  rewardCoins: [
    { required: true, message: '请输入奖励币值', trigger: 'blur' }
  ],
  maxParticipants: [
    { required: true, message: '请输入最大参与团队数', trigger: 'blur' }
  ]
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  return dayjs(dateTime).format('YYYY-MM-DD HH:mm')
}

// 获取状态类型
const getStatusType = (status) => {
  const typeMap = {
    'NOT_STARTED': 'warning',
    'ONGOING': 'success',
    'FINISHED': 'info',
    'CANCELLED': 'danger'
  }
  return typeMap[status] || ''
}

// 获取状态文本
const getStatusText = (status) => {
  const textMap = {
    'NOT_STARTED': '未开始',
    'ONGOING': '进行中',
    'FINISHED': '已结束',
    'CANCELLED': '已取消'
  }
  return textMap[status] || status
}

// 加载活动列表
const loadActivityList = async () => {
  loading.value = true
  try {
    const res = await getActivityList({
      activityType: filters.activityType || null,
      status: filters.status || null,
      searchKeyword: filters.searchKeyword || null,
      page: pagination.page,
      pageSize: pagination.pageSize
    })

    if (res.success) {
      // 按开始时间倒序排序
      tableData.value = res.data.items.slice().sort((a, b) => {
        const t1 = new Date(a.startTime).getTime();
        const t2 = new Date(b.startTime).getTime();
        return t2 - t1;
      });
      pagination.total = res.data.total;
      // 调试：打印location字段
      console.log('活动列表数据', tableData.value);
      // 计算统计数据
      updateStats();
    } else {
      ElMessage.error(res.message || '加载失败');
    }
  } catch (error) {
    console.error('加载活动列表失败:', error)
    ElMessage.error('加载失败，请重试')
  } finally {
    loading.value = false
  }
}

// 更新统计数据
const updateStats = async () => {
  try {
    // 查询全部活动
    const allRes = await getActivityList({
      page: 1,
      pageSize: 1
    })
    stats.total = allRes.data?.total || 0

    // 查询未开始活动
    const notStartedRes = await getActivityList({
      status: 'NOT_STARTED',
      page: 1,
      pageSize: 1
    })
    stats.notStarted = notStartedRes.data?.total || 0

    // 查询进行中活动
    const ongoingRes = await getActivityList({
      status: 'ONGOING',
      page: 1,
      pageSize: 1
    })
    stats.ongoing = ongoingRes.data?.total || 0

    // 查询已结束活动
    const finishedRes = await getActivityList({
      status: 'FINISHED',
      page: 1,
      pageSize: 1
    })
    stats.finished = finishedRes.data?.total || 0
  } catch (error) {
    console.error('更新统计数据失败:', error)
  }
}

// 筛选变化
const handleFilterChange = () => {
  pagination.page = 1
  loadActivityList()
}

// 分页变化
const handlePageChange = () => {
  loadActivityList()
}

const handleSizeChange = () => {
  pagination.page = 1
  loadActivityList()
}

// 创建活动
const handleCreate = () => {
  dialogMode.value = 'create'
  Object.assign(formData, {
    activityName: '',
    activityType: 'DUTY',
    description: '',
    location: '',
    startTime: '',
    endTime: '',
    rewardCoins: 0,
    participationType: 'MANUAL_REVIEW',
    maxParticipants: 10
  })
  dialogVisible.value = true
}

// 编辑活动
const handleEdit = (row) => {
  dialogMode.value = 'edit'
  Object.assign(formData, {
    id: row.id,
    activityName: row.activityName,
    activityType: row.activityType,
    description: row.description,
    location: row.location,
    startTime: row.startTime,
    endTime: row.endTime,
    rewardCoins: row.rewardCoins,
    participationType: row.participationType,
    maxParticipants: row.maxParticipants
  })
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    let res
    if (dialogMode.value === 'create') {
      res = await createActivity(formData)
    } else {
      res = await updateActivity(formData.id, formData)
    }

    if (res.success) {
      ElMessage.success(res.message || '操作成功')
      dialogVisible.value = false
      loadActivityList()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('操作失败，请重试')
  } finally {
    submitting.value = false
  }
}

// 下线活动
const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm(
      '确定要下线该活动吗？下线后用户将无法报名参与',
      '下线确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const res = await updateActivityStatus(row.id, 'CANCELLED')
    if (res.success) {
      ElMessage.success('下线成功')
      loadActivityList()
    } else {
      ElMessage.error(res.message || '下线失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('下线失败:', error)
      ElMessage.error('下线失败，请重试')
    }
  }
}

// 删除活动
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该活动吗？删除后将无法恢复，关联的参与记录也会被删除',
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const res = await deleteActivity(row.id)
    if (res.success) {
      ElMessage.success('删除成功')
      loadActivityList()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败，请重试')
    }
  }
}

// 查看参与记录
const handleViewParticipations = (row) => {
  router.push({
    path: '/activity/participation',
    query: { activityId: row.id, activityName: row.activityName }
  })
}

// 初始化
onMounted(() => {
  loadActivityList()
})
</script>

<style scoped lang="scss">
.app-container {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-4px);
  }

  .stat-content {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .stat-icon {
    flex-shrink: 0;
    width: 60px;
    height: 60px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
  }

  .stat-info {
    flex: 1;
  }

  .stat-value {
    font-size: 28px;
    font-weight: bold;
    line-height: 1;
    margin-bottom: 8px;
  }

  .stat-label {
    font-size: 14px;
    color: var(--el-text-color-secondary);
  }
}

.stat-card-primary .stat-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-card-warning .stat-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-card-success .stat-icon {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-card-info .stat-icon {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
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

.participation-info {
  font-size: 12px;
}

.review-stats {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
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

.form-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
/* 表单弹窗整体居中美化 */
.centered-form-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.centered-form-wrapper .el-form {
  width: 420px;
  margin: 0 auto;
}
.centered-form-footer {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 8px;
}
</style>
