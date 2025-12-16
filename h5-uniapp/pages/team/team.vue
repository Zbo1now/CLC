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
            <text class="card-icon">💰</text>
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
            <text class="section-icon">🏷️</text>
            <text>团队信息</text>
          </view>

          <view class="row">
            <view class="row-left">
              <text class="row-icon">👥</text>
              <text class="row-label">团队名称</text>
            </view>
            <view class="row-right">
              <text class="row-value">{{ teamName || '—' }}</text>
            </view>
          </view>

          <view class="row">
            <view class="row-left">
              <text class="row-icon">🕒</text>
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
          <view class="section-title">
            <text class="section-icon">🧑‍🤝‍🧑</text>
            <text>团队成员</text>
          </view>

          <view class="member" v-for="m in members" :key="m.id">
            <view class="avatar">
              <text class="avatar-text">{{ m.initials }}</text>
            </view>
            <view class="member-info">
              <text class="member-name">{{ m.name }}</text>
              <text class="member-meta">{{ m.role }} · {{ m.status }}</text>
            </view>
            <view class="pill">待接入</view>
          </view>
        </view>

        <!-- 账号与安全 -->
        <view class="glass-card actions-card">
          <view class="section-title">
            <text class="section-icon">🛡️</text>
            <text>账号与安全</text>
          </view>

          <view class="actions">
            <button class="primary-btn" @tap="logout">
              <text class="btn-icon">🚪</text>
              <text class="btn-text">退出登录</text>
            </button>
          </view>

          <view class="hint">* 退出后需要重新刷脸/账号登录</view>
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

const members = ref([
  { id: 1, initials: 'A', name: '成员 A', role: '负责人', status: '在线' },
  { id: 2, initials: 'B', name: '成员 B', role: '成员', status: '离线' },
  { id: 3, initials: 'C', name: '成员 C', role: '成员', status: '离线' }
]);

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

const refreshAll = async () => {
  await fetchSummary();
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

.section-icon {
  font-size: 32rpx;
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

.btn-icon {
  font-size: 28rpx;
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
