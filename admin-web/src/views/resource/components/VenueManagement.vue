<template>
  <div class="venue-management">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="queryParams" class="search-form">
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 150px">
          <el-option label="可用" value="AVAILABLE" />
          <el-option label="已预订" value="BOOKED" />
          <el-option label="维护中" value="MAINTENANCE" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="queryParams.keyword" placeholder="场地名称/类型" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button type="success" @click="handleAdd">新增场地</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="venueName" label="场地名称" width="180" />
      <el-table-column prop="venueType" label="类型" width="150" />
      <el-table-column prop="capacity" label="容纳人数" width="120" />
      <el-table-column prop="ratePerHour" label="时租金（币）" width="130" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="handleEdit(row)" style="margin-right: 8px">编辑</el-button>
          <el-dropdown @command="(cmd) => handleStatusChange(row, cmd)" style="margin-right: 8px">
            <el-button type="warning" size="small">
              状态 <el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="AVAILABLE">设为可用</el-dropdown-item>
                <el-dropdown-item command="BOOKED">设为已订</el-dropdown-item>
                <el-dropdown-item command="MAINTENANCE">设为维护</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="queryParams.page"
      v-model:page-size="queryParams.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="loadData"
      @current-change="loadData"
      class="pagination"
    />

    <!-- 表单对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px">
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="场地名称" prop="venueName">
          <el-input v-model="formData.venueName" placeholder="如: 多功能会议室A" />
        </el-form-item>
        <el-form-item label="场地类型" prop="venueType">
          <el-input v-model="formData.venueType" placeholder="如: 会议室" />
        </el-form-item>
        <el-form-item label="容纳人数" prop="capacity">
          <el-input-number v-model="formData.capacity" :min="1" :step="5" />
        </el-form-item>
        <el-form-item label="时租金" prop="ratePerHour">
          <el-input-number v-model="formData.ratePerHour" :min="1" :step="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { 
  getResourceList, 
  createResource, 
  updateResource, 
  deleteResource,
  updateResourceStatus 
} from '@/api/resource'

const emit = defineEmits(['refresh'])

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const queryParams = reactive({
  status: '',
  keyword: '',
  page: 1,
  pageSize: 10
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增场地')
const formRef = ref(null)
const formData = reactive({
  id: null,
  venueName: '',
  venueType: '',
  capacity: 10,
  ratePerHour: 20
})

const formRules = {
  venueName: [{ required: true, message: '请输入场地名称', trigger: 'blur' }],
  venueType: [{ required: true, message: '请输入场地类型', trigger: 'blur' }],
  capacity: [{ required: true, message: '请输入容纳人数', trigger: 'blur' }],
  ratePerHour: [{ required: true, message: '请输入时租金', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getResourceList('venue', queryParams)
    tableData.value = res.data.items
    total.value = res.data.total
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.page = 1
  loadData()
}

const handleReset = () => {
  queryParams.status = ''
  queryParams.keyword = ''
  queryParams.page = 1
  loadData()
}

const handleAdd = () => {
  dialogTitle.value = '新增场地'
  Object.assign(formData, {
    id: null,
    venueName: '',
    venueType: '',
    capacity: 10,
    ratePerHour: 20
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑场地'
  Object.assign(formData, {
    id: row.id,
    venueName: row.venueName,
    venueType: row.venueType,
    capacity: row.capacity,
    ratePerHour: row.ratePerHour
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  try {
    if (formData.id) {
      await updateResource('venue', formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await createResource('venue', formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
    emit('refresh')
  } catch (error) {
    ElMessage.error(formData.id ? '更新失败' : '创建失败')
  }
}

const handleStatusChange = async (row, status) => {
  try {
    await updateResourceStatus('venue', row.id, status)
    ElMessage.success('状态更新成功')
    loadData()
    emit('refresh')
  } catch (error) {
    ElMessage.error('状态更新失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该场地吗？', '提示', {
      type: 'warning'
    })
    await deleteResource('venue', row.id)
    ElMessage.success('删除成功')
    loadData()
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const getStatusType = (status) => {
  const map = {
    'AVAILABLE': 'success',
    'BOOKED': 'warning',
    'MAINTENANCE': 'info'
  }
  return map[status] || ''
}

const getStatusText = (status) => {
  const map = {
    'AVAILABLE': '可用',
    'BOOKED': '已预订',
    'MAINTENANCE': '维护中'
  }
  return map[status] || status
}

onMounted(() => {
  loadData()
})

defineExpose({ loadData })
</script>

<style scoped lang="scss">
.venue-management {
  .search-form {
    background: var(--el-fill-color-light);
    padding: 20px;
    border-radius: 8px;
    margin-bottom: 20px;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
