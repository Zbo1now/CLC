<template>
  <div class="equipment-management">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="queryParams" class="search-form">
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 150px">
          <el-option label="可用" value="AVAILABLE" />
          <el-option label="已借出" value="BORROWED" />
          <el-option label="维护中" value="MAINTENANCE" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="queryParams.keyword" placeholder="器材名称/型号/类型" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button type="success" @click="handleAdd">新增器材</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="equipmentName" label="器材名称" width="150" />
      <el-table-column prop="model" label="型号" width="120" />
      <el-table-column prop="equipmentType" label="类型" width="120" />
      <el-table-column prop="ratePerDay" label="日租金（币）" width="130" />
      <el-table-column label="库存" width="150">
        <template #default="{ row }">
          <span :class="{ 'text-danger': row.availableQuantity === 0 }">
            {{ row.availableQuantity || 0 }} / {{ row.quantity || 0 }}
          </span>
        </template>
      </el-table-column>
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
                <el-dropdown-item command="BORROWED">设为已借</el-dropdown-item>
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
        <el-form-item label="器材名称" prop="equipmentName">
          <el-input v-model="formData.equipmentName" placeholder="如: 篮球" />
        </el-form-item>
        <el-form-item label="型号" prop="model">
          <el-input v-model="formData.model" placeholder="如: Spalding 74-604Y" />
        </el-form-item>
        <el-form-item label="器材类型" prop="equipmentType">
          <el-input v-model="formData.equipmentType" placeholder="如: 球类器材" />
        </el-form-item>
        <el-form-item label="日租金" prop="ratePerDay">
          <el-input-number v-model="formData.ratePerDay" :min="1" :step="5" />
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="formData.quantity" :min="1" :step="1" />
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
const dialogTitle = ref('新增器材')
const formRef = ref(null)
const formData = reactive({
  id: null,
  equipmentName: '',
  model: '',
  equipmentType: '',
  ratePerDay: 5,
  quantity: 1
})

const formRules = {
  equipmentName: [{ required: true, message: '请输入器材名称', trigger: 'blur' }],
  model: [{ required: true, message: '请输入型号', trigger: 'blur' }],
  equipmentType: [{ required: true, message: '请输入器材类型', trigger: 'blur' }],
  ratePerDay: [{ required: true, message: '请输入日租金', trigger: 'blur' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getResourceList('equipment', queryParams)
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
  dialogTitle.value = '新增器材'
  Object.assign(formData, {
    id: null,
    equipmentName: '',
    model: '',
    equipmentType: '',
    ratePerDay: 5,
    quantity: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑器材'
  Object.assign(formData, {
    id: row.id,
    equipmentName: row.equipmentName,
    model: row.model,
    equipmentType: row.equipmentType,
    ratePerDay: row.ratePerDay,
    quantity: row.quantity
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  try {
    if (formData.id) {
      await updateResource('equipment', formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await createResource('equipment', formData)
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
    await updateResourceStatus('equipment', row.id, status)
    ElMessage.success('状态更新成功')
    loadData()
    emit('refresh')
  } catch (error) {
    ElMessage.error('状态更新失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该器材吗？', '提示', {
      type: 'warning'
    })
    await deleteResource('equipment', row.id)
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
    'BORROWED': 'warning',
    'MAINTENANCE': 'info'
  }
  return map[status] || ''
}

const getStatusText = (status) => {
  const map = {
    'AVAILABLE': '可用',
    'BORROWED': '已借出',
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
.equipment-management {
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

  .text-danger {
    color: #f56c6c;
    font-weight: bold;
  }
}
</style>
