<template>
  <view class="container">
    <!-- 顶部导航栏 -->
    <view class="nav-header">
      <view class="nav-title">众创空间 · 智创未来</view>
      <view class="nav-actions">
        <!-- 暂时保留图标位置，后续可做功能 -->
        <text class="nav-icon">🔔</text>
      </view>
    </view>

    <!-- 系统通知 -->
    <view class="notification-bar" v-if="notification">
      <text class="notice-icon">⚠️</text>
      <text class="notice-text">{{ notification }}</text>
    </view>

    <!-- 资产卡片 -->
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

    <!-- 快捷操作入口 -->
    <view class="grid-menu">
      <view class="grid-item" @tap="handleAction('checkin')">
        <view class="icon-box color-1">👤</view>
        <text class="grid-label">打卡</text>
      </view>
      <view class="grid-item" @tap="handleAction('achievement')">
        <view class="icon-box color-2">📝</view>
        <text class="grid-label">成果提交</text>
      </view>
      <view class="grid-item" @tap="handleAction('station')">
        <view class="icon-box color-3">🏢</view>
        <text class="grid-label">工位租赁</text>
      </view>
      <view class="grid-item" @tap="handleAction('device')">
        <view class="icon-box color-4">📸</view>
        <text class="grid-label">设备租用</text>
      </view>
    </view>

    <!-- 近期流水 -->
    <view class="glass-card list-card">
      <view class="list-header">
        <text class="list-title">近期流水摘要</text>
        <text class="more-link" @tap="viewDetails">全部</text>
      </view>
      <view class="transaction-list">
        <view class="list-item" v-for="(item, index) in transactions" :key="index">
          <view class="item-left">
            <text class="item-icon">{{ item.amount > 0 ? '📥' : '📤' }}</text>
            <view class="item-info">
              <text class="item-desc">{{ item.desc }}</text>
              <text class="item-date">{{ item.date }}</text>
            </view>
          </view>
          <text :class="['item-amount', item.amount > 0 ? 'income' : 'expense']">
            {{ item.amount > 0 ? '+' : '' }}{{ item.amount }}
          </text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { baseUrl } from '../../common/config.js';

const balance = ref(0);
const notification = ref('您的 3D 打印预约即将开始！');
const transactions = ref([]);

const getSessionId = () => {
  let sessionId = '';
  const cookieStr = uni.getStorageSync('cookie');
  if (cookieStr && cookieStr.includes('JSESSIONID=')) {
    sessionId = cookieStr.split('JSESSIONID=')[1].split(';')[0];
  }
  return sessionId;
};

const formatTxnTime = (createdAt) => {
  if (!createdAt) return '';
  const d = new Date(createdAt);
  if (Number.isNaN(d.getTime())) return String(createdAt).slice(0, 10);

  const now = new Date();
  const sameDay = d.getFullYear() === now.getFullYear()
    && d.getMonth() === now.getMonth()
    && d.getDate() === now.getDate();

  if (sameDay) {
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  }
  return `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
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
          uni.showToast({ title: '登录已过期', icon: 'none' });
          setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 1200);
          resolve();
          return;
        }
        if (res.statusCode === 200 && res.data && res.data.success) {
          const data = res.data.data || {};
          balance.value = data.balance ?? 0;

          const list = Array.isArray(data.transactions) ? data.transactions : [];
          transactions.value = list.map(t => ({
            desc: t.description,
            amount: t.amount,
            date: formatTxnTime(t.createdAt)
          }));

          // 同步到本地缓存，供其他页面兜底展示
          uni.setStorageSync('teamBalance', balance.value);
        }
        resolve();
      },
      fail: () => {
        resolve();
      }
    });
  });
};

onMounted(() => {
  // 检查登录状态
  const userInfo = uni.getStorageSync('userInfo');
  if (!userInfo) {
    uni.showToast({ title: '请先登录', icon: 'none' });
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/index/index' });
    }, 1000);
    return;
  }

  // 兜底：先用本地缓存顶一下，避免白屏；真实数据在 onShow 刷新
  const storedBalance = uni.getStorageSync('teamBalance');
  if (storedBalance !== '' && storedBalance !== null && storedBalance !== undefined) {
    balance.value = storedBalance;
  }
});

onShow(async () => {
  const userInfo = uni.getStorageSync('userInfo');
  if (!userInfo) return;
  await fetchSummary();
});

function handleRecharge() {
  uni.showToast({ title: '获取渠道即将上线', icon: 'none' });
}

function viewDetails() {
  uni.showToast({ title: '查看明细', icon: 'none' });
}

function handleAction(type) {
  if (type === 'checkin') {
    uni.navigateTo({ url: '/pages/checkin/checkin' });
    return;
  }
  if (type === 'achievement') {
    uni.navigateTo({ url: '/pages/achievement/achievement' });
    return;
  }
  if (type === 'station') {
    uni.navigateTo({ url: '/pages/station/station' });
    return;
  }
  if (type === 'device') {
    uni.navigateTo({ url: '/pages/device/device' });
    return;
  }
  const actions = {
    device: '设备租用'
  };
  uni.showToast({ title: actions[type] + '即将上线', icon: 'none' });
}

function pathToBase64(path) {
    return new Promise((resolve, reject) => {
        fetch(path)
            .then(response => response.blob())
            .then(blob => {
                const reader = new FileReader();
                reader.onloadend = () => resolve(reader.result);
                reader.onerror = reject;
                reader.readAsDataURL(blob);
            });
    });
}

function submitCheckIn(base64) {
  const cookieStr = uni.getStorageSync('cookie');
  let sessionId = '';
  if (cookieStr && cookieStr.includes('JSESSIONID=')) {
    sessionId = cookieStr.split('JSESSIONID=')[1].split(';')[0];
  }
  uni.request({
    url: `${baseUrl}/api/face/checkin`,
    method: 'POST',
    header: { 
      'Content-Type': 'application/json',
      'X-Session-Id': sessionId
    },
    withCredentials: true,
    data: { image: base64 },
    success: (res) => {
      uni.hideLoading();
      if (res.statusCode === 200 && res.data.success) {
        uni.showToast({ title: '打卡成功 +10币', icon: 'success' });
        // 刷新余额（模拟）
        balance.value += 10;
        uni.setStorageSync('teamBalance', balance.value);
      } else {
        uni.showToast({ title: res.data.message || '打卡失败', icon: 'none' });
      }
    },
    fail: () => {
      uni.hideLoading();
      uni.showToast({ title: '网络异常', icon: 'none' });
    }
  });
}
</script>

<style lang="scss" scoped>
@import '../../uni.scss';

.container {
  padding: 30rpx;
  padding-top: 80rpx; /* 留出状态栏高度 */
  justify-content: flex-start; /* 覆盖默认的居中 */
  gap: 32rpx;
}

.nav-header {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;
  
  .nav-title {
    font-size: 36rpx;
    font-weight: 800;
    color: $text-main;
    text-shadow: 0 2px 4px rgba(0,0,0,0.1);
  }
  
  .nav-icon {
    font-size: 40rpx;
  }
}

.notification-bar {
  width: 100%;
  background: rgba(255, 165, 0, 0.15);
  border: 1px solid rgba(255, 165, 0, 0.3);
  border-radius: 16rpx;
  padding: 20rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  animation: slideIn 0.5s ease;
  
  .notice-icon { font-size: 32rpx; }
  .notice-text { 
    font-size: 26rpx; 
    color: #d97706; 
    font-weight: 600;
  }
}

.balance-card {
  background: linear-gradient(135deg, rgba(255,255,255,0.9), rgba(255,255,255,0.7));
  padding: 40rpx;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 20rpx;
    .card-label { font-size: 28rpx; color: $text-light; }
    .card-icon { font-size: 32rpx; }
  }
  
  .balance-value {
    font-size: 64rpx;
    font-weight: 800;
    color: $primary;
    margin-bottom: 30rpx;
    .unit { font-size: 28rpx; margin-left: 8rpx; color: $text-main; }
  }
  
  .card-actions {
    display: flex;
    align-items: center;
    gap: 20rpx;
    font-size: 28rpx;
    color: $primary;
    font-weight: 600;
    
    .divider { color: $text-light; opacity: 0.5; }
  }
}

.grid-menu {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20rpx;
  
  .grid-item {
    background: rgba(255,255,255,0.6);
    backdrop-filter: blur(10px);
    border-radius: 24rpx;
    padding: 24rpx 10rpx;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16rpx;
    transition: transform 0.2s;
    
    &:active { transform: scale(0.95); }
    
    .icon-box {
      width: 80rpx;
      height: 80rpx;
      border-radius: 24rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 40rpx;
      
      &.color-1 { background: rgba(99, 102, 241, 0.1); color: #6366f1; }
      &.color-2 { background: rgba(236, 72, 153, 0.1); color: #ec4899; }
      &.color-3 { background: rgba(34, 197, 94, 0.1); color: #22c55e; }
      &.color-4 { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
    }
    
    .grid-label {
      font-size: 24rpx;
      color: $text-main;
      font-weight: 600;
    }
  }
}

.list-card {
  padding: 30rpx;
  
  .list-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24rpx;
    
    .list-title { font-size: 30rpx; font-weight: 700; color: $text-main; }
    .more-link { font-size: 24rpx; color: $text-light; }
  }
  
  .list-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20rpx 0;
    border-bottom: 1px solid rgba(0,0,0,0.05);
    
    &:last-child { border-bottom: none; }
    
    .item-left {
      display: flex;
      align-items: center;
      gap: 20rpx;
      
      .item-icon { font-size: 32rpx; }
      .item-info {
        display: flex;
        flex-direction: column;
        .item-desc { font-size: 28rpx; color: $text-main; font-weight: 500; }
        .item-date { font-size: 22rpx; color: $text-light; }
      }
    }
    
    .item-amount {
      font-size: 30rpx;
      font-weight: 700;
      &.income { color: #22c55e; }
      &.expense { color: $text-main; }
    }
  }
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(-10rpx); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
