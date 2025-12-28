<template>
  <view class="container">
    <view class="shell">
      <view class="header panel">
        <view class="title-wrap">
          <text class="title">团队中心</text>
          <text class="subtitle">Team Center</text>
        </view>
      </view>

      <view class="content panel">
        <!-- 余额大盘（从首页迁移） -->
        <view class="glass-card balance-card">
          <view class="card-header">
            <text class="card-label">当前虚拟币余额</text>
            <text class="card-icon">BAL</text>
          </view>
          <view class="balance-value">{{ balance }} <text class="unit">币</text></view>
          <view class="card-actions">
            <view class="action-link" @tap="handleRecharge">获取</view>
            <view class="divider">|</view>
            <view class="action-link" @tap="viewDetails">查看明细 →</view>
          </view>
        </view>

        <!-- 团队信息 -->
        <view class="glass-card info-card">
          <view class="section-title">
            <text>团队信息</text>
          </view>

          <view class="row">
            <view class="row-left">
              <text class="row-label">团队名称</text>
            </view>
            <view class="row-right">
              <text class="row-value">{{ teamName || '—' }}</text>
            </view>
          </view>

          <view class="row">
            <view class="row-left">
              <text class="row-label">登录时间</text>
            </view>
            <view class="row-right">
              <text class="row-value">{{ loginTimeText }}</text>
            </view>
          </view>

          <view class="hint">* 团队详细资料后续接入后端</view>
        </view>

        <!-- 团队成员（占位） -->
        <view class="glass-card members-card">
          <view class="section-title-bar">
            <view class="section-title">
              <text>团队成员</text>
            </view>
            <view class="mini-btn" @tap="openAddMember">添加成员</view>
          </view>

          <view v-if="members.length === 0" class="empty-hint">暂无成员</view>

          <view class="member" v-for="m in members" :key="m.id">
            <view class="avatar">
              <text class="avatar-text">{{ toInitials(m.memberName) }}</text>
            </view>
            <view class="member-info">
              <text class="member-name">{{ m.memberName }}</text>
              <text class="member-meta">{{ roleLabel(m.role) }}<text v-if="m.phone"> · {{ m.phone }}</text></text>
            </view>
            <view class="pill">{{ statusLabel(m.status) }}</view>
          </view>
        </view>

        <!-- 账号与安全 -->
        <view class="glass-card actions-card">
          <view class="section-title">
            <text>账号与安全</text>
          </view>

          <view class="actions">
            <button class="primary-btn" @tap="logout">
              <text class="btn-text">退出登录</text>
            </button>
          </view>

          <view class="hint">* 退出后需要重新刷脸/账号登录</view>
        </view>
      </view>
    </view>

    <!-- 添加成员弹窗 -->
    <view v-if="addDialogVisible" class="dialog-mask" @tap="closeAddMember">
      <view class="dialog glass-card" @tap.stop>
        <view class="dialog-title">添加团队成员</view>

        <view class="dialog-form">
          <view class="dialog-row">
            <text class="dialog-label">姓名</text>
            <input class="dialog-input" v-model="addForm.memberName" placeholder="请输入成员姓名" />
          </view>
          <view class="dialog-row">
            <text class="dialog-label">手机号</text>
            <input class="dialog-input" v-model="addForm.phone" placeholder="选填" />
          </view>
        </view>

        <view class="dialog-actions">
          <button class="ghost-btn" @tap="closeAddMember">取消</button>
          <button class="primary-btn" @tap="submitAddMember" :disabled="addingMember">确认添加</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { baseUrl } from '../../common/config.js';

const balance = ref(0);

const teamName = ref('');
const loginTime = ref(null);

const members = ref([]);
const addingMember = ref(false);
const addDialogVisible = ref(false);

const addForm = ref({
  memberName: '',
  phone: ''
});

const getSessionId = () => {
  let sessionId = '';
  const cookieStr = uni.getStorageSync('cookie');
  if (cookieStr && cookieStr.includes('JSESSIONID=')) {
    sessionId = cookieStr.split('JSESSIONID=')[1].split(';')[0];
  }
  return sessionId;
};

const loginTimeText = computed(() => {
  if (!loginTime.value) return '—';
  const d = new Date(loginTime.value);
  if (Number.isNaN(d.getTime())) return '—';
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hh = String(d.getHours()).padStart(2, '0');
  const mm = String(d.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${day} ${hh}:${mm}`;
});

const handle401 = () => {
  uni.showToast({ title: '登录已过期', icon: 'none' });
  setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 1200);
};

function toInitials(name) {
  const s = String(name || '').trim();
  if (!s) return '-';
  return s.slice(0, 1).toUpperCase();
}

function roleLabel(role) {
  const r = String(role || '').toUpperCase();
  if (r === 'LEADER') return '负责人';
  return '成员';
}

function statusLabel(status) {
  const s = String(status || '').toUpperCase();
  if (s === 'INACTIVE') return '停用';
  return '正常';
}

const fetchSummary = () => {
  const sessionId = getSessionId();
  return new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/teams/me/summary`,
      method: 'GET',
      header: { 'X-Session-Id': sessionId },
      withCredentials: true,
      success: (res) => {
        if (res.statusCode === 401) {
          handle401();
          resolve();
          return;
        }
        if (res.statusCode === 200 && res.data && res.data.success) {
          const data = res.data.data || {};
          balance.value = data.balance ?? 0;
          uni.setStorageSync('teamBalance', balance.value);
        }
        resolve();
      },
      fail: () => resolve()
    });
  });
};

const fetchMembers = () => {
  const sessionId = getSessionId();
  return new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/teams/me/members`,
      method: 'GET',
      header: { 'X-Session-Id': sessionId },
      withCredentials: true,
      success: (res) => {
        if (res.statusCode === 401) {
          handle401();
          resolve();
          return;
        }
        if (res.statusCode === 200 && res.data && res.data.success) {
          members.value = (res.data.data && res.data.data.list) ? res.data.data.list : [];
        }
        resolve();
      },
      fail: () => resolve()
    });
  });
};

const refreshAll = async () => {
  await fetchSummary();
  await fetchMembers();
};

const logout = () => {
  uni.showModal({
    title: '退出登录',
    content: '确认退出当前账号吗？',
    confirmText: '退出',
    cancelText: '取消',
    success: (res) => {
      if (!res.confirm) return;
      uni.removeStorageSync('userInfo');
      uni.removeStorageSync('cookie');
      uni.removeStorageSync('teamBalance');
      uni.reLaunch({ url: '/pages/index/index' });
    }
  });
};

function handleRecharge() {
  uni.showToast({ title: '获取渠道即将上线', icon: 'none' });
}

function viewDetails() {
  uni.navigateTo({ url: '/pages/transactions/transactions' });
}

onMounted(() => {
  const userInfo = uni.getStorageSync('userInfo');
  if (!userInfo) {
    uni.showToast({ title: '请先登录', icon: 'none' });
    setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 1000);
    return;
  }
  teamName.value = userInfo.teamName || '';
  loginTime.value = userInfo.loginTime || null;

  const storedBalance = uni.getStorageSync('teamBalance');
  if (storedBalance !== '' && storedBalance !== null && storedBalance !== undefined) {
    balance.value = storedBalance;
  }
});

onShow(async () => {
  const userInfo = uni.getStorageSync('userInfo');
  if (!userInfo) return;
  await refreshAll();
});

function openAddMember() {
  addForm.value = { memberName: '', phone: '' };
  addDialogVisible.value = true;
}

function closeAddMember() {
  addDialogVisible.value = false;
}

async function submitAddMember() {
  const memberName = String(addForm.value.memberName || '').trim();
  const phone = String(addForm.value.phone || '').trim();
  const role = 'MEMBER';

  if (!memberName) {
    uni.showToast({ title: '请输入成员姓名', icon: 'none' });
    return;
  }

  const sessionId = getSessionId();
  addingMember.value = true;
  uni.request({
    url: `${baseUrl}/api/teams/me/members`,
    method: 'POST',
    header: { 'Content-Type': 'application/json', 'X-Session-Id': sessionId },
    withCredentials: true,
    data: { memberName, role, phone },
    success: async (res) => {
      if (res.statusCode === 401) {
        handle401();
        return;
      }
      if (res.statusCode === 200 && res.data && res.data.success) {
        uni.showToast({ title: '添加成功', icon: 'success' });
        closeAddMember();
        await fetchMembers();
        return;
      }
      uni.showToast({ title: (res.data && res.data.message) || '添加失败', icon: 'none' });
    },
    fail: () => {
      uni.showToast({ title: '网络异常', icon: 'none' });
    },
    complete: () => {
      addingMember.value = false;
    }
  });
}
</script>

<style lang="scss" scoped>
@import '../../uni.scss';

.container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0;
  justify-content: flex-start;
  box-sizing: border-box;
  width: 100%;
}

.shell {
  width: 100%;
  box-sizing: border-box;
  padding: 30rpx;
  padding-top: 80rpx;
  padding-bottom: 140rpx;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: flex-start;
  gap: 24rpx;
}

.panel {
  width: 100%;
  max-width: 640rpx;
  margin-left: auto;
  margin-right: auto;
  box-sizing: border-box;
}

.header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.title-wrap {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.title {
  font-size: 40rpx;
  font-weight: 900;
  color: $text-main;
}

.subtitle {
  font-size: 24rpx;
  color: $text-light;
}



.content {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24rpx;
}

.balance-card,
.info-card,
.members-card,
.actions-card {
  width: 100%;
}

.balance-card {
  padding: 44rpx 34rpx;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.card-label {
  font-size: 28rpx;
  color: $text-light;
  font-weight: 700;
}

.card-icon {
  font-size: 32rpx;
}

.balance-value {
  font-size: 68rpx;
  font-weight: 900;
  color: $primary;
  margin-bottom: 26rpx;
}

.unit {
  font-size: 28rpx;
  margin-left: 8rpx;
  color: $text-main;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 20rpx;
  font-size: 28rpx;
  color: $primary;
  font-weight: 700;
}

.divider {
  color: $text-light;
  opacity: 0.5;
}

.section-title {
  font-size: 32rpx;
  font-weight: 900;
  color: $text-main;
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 18rpx;
}

.section-title-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22rpx 22rpx;
  border-radius: 18rpx;
  background: rgba($bg-color, 0.9);
  box-shadow: $shadow-sm;
  margin-bottom: 18rpx;
}

.row-left {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.row-icon {
  font-size: 30rpx;
}

.row-label {
  font-size: 28rpx;
  font-weight: 800;
  color: $text-main;
}

.row-right {
  display: flex;
  align-items: center;
}

.row-value {
  font-size: 26rpx;
  color: $text-light;
  font-weight: 700;
}

.members-card,
.info-card,
.actions-card {
  padding: 34rpx 28rpx;
}

.member {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 18rpx 18rpx;
  border-radius: 18rpx;
  background: rgba($bg-color, 0.9);
  box-shadow: $shadow-sm;
  margin-bottom: 14rpx;
}

.avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: $radius-full;
  background: rgba($primary, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-text {
  font-size: 30rpx;
  font-weight: 900;
  color: $primary;
}

.member-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.member-name {
  font-size: 28rpx;
  font-weight: 900;
  color: $text-main;
}

.member-meta {
  font-size: 22rpx;
  color: $text-light;
}

.pill {
  padding: 12rpx 18rpx;
  border-radius: $radius-full;
  background: rgba($text-light, 0.12);
  color: $text-light;
  font-size: 22rpx;
  font-weight: 800;
}

.mini-btn {
  padding: 10rpx 16rpx;
  border-radius: $radius-full;
  background: rgba($primary, 0.12);
  color: $primary;
  font-size: 24rpx;
  font-weight: 800;
}

.empty-hint {
  padding: 18rpx 10rpx;
  font-size: 24rpx;
  color: $text-light;
}

.dialog-mask {
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30rpx;
  z-index: 999;
}

.dialog {
  width: 100%;
  max-width: 640rpx;
  padding: 30rpx 26rpx;
}

.dialog-title {
  font-size: 32rpx;
  font-weight: 900;
  color: $text-main;
  margin-bottom: 20rpx;
}

.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.dialog-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 18rpx 18rpx;
  border-radius: 18rpx;
  background: rgba($bg-color, 0.9);
  box-shadow: $shadow-sm;
}

.dialog-label {
  font-size: 26rpx;
  font-weight: 800;
  color: $text-main;
  width: 110rpx;
  flex-shrink: 0;
}

.dialog-input {
  flex: 1;
  font-size: 26rpx;
  color: $text-main;
}

.picker-value {
  flex: 1;
  font-size: 26rpx;
  color: $text-main;
  text-align: right;
}

.dialog-actions {
  display: flex;
  gap: 14rpx;
  margin-top: 22rpx;
}

.actions {
  display: flex;
  gap: 14rpx;
  margin-top: 6rpx;
}

.primary-btn {
  flex: 1;
  background: $primary-gradient;
  color: $white;
  border-radius: $radius-full;
  padding: 22rpx 0;
  font-size: 28rpx;
  font-weight: 900;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  box-shadow: 0 14rpx 30rpx rgba($primary, 0.24);
  transition: transform 0.18s ease;
}

.primary-btn:active {
  transform: scale(0.98);
}

.btn-text {
  font-size: 26rpx;
}

.ghost-btn {
  width: 220rpx;
  background: rgba($white, 0.65);
  color: $text-main;
  border-radius: $radius-full;
  padding: 22rpx 0;
  font-size: 28rpx;
  font-weight: 900;
  border: 2rpx solid rgba($primary, 0.14);
  transition: transform 0.18s ease, opacity 0.18s ease;
}

.ghost-btn:active {
  transform: scale(0.98);
}

.hint {
  margin-top: 12rpx;
  font-size: 22rpx;
  color: $text-light;
}
</style>
