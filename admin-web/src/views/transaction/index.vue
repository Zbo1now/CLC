<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>虚拟币流水</span>
          <el-button type="primary">导出 CSV</el-button>
        </div>
      </template>
      <el-empty description="虚拟币流水模块开发中..." />
    </el-card>
  </div>
</template>

<script setup>
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
