<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>资源管理</span>
          <el-button type="primary">新增资源</el-button>
        </div>
      </template>
      <el-empty description="资源管理模块开发中..." />
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
