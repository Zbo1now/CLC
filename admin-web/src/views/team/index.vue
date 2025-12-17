<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>团队管理</span>
          <el-button type="primary">新增团队</el-button>
        </div>
      </template>
      <el-empty description="团队管理模块开发中..." />
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
