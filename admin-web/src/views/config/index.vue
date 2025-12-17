<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>系统配置</span>
          <el-button type="primary">保存配置</el-button>
        </div>
      </template>
      <el-empty description="系统配置模块开发中..." />
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
