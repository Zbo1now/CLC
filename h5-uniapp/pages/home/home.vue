<template>
  <view class="container">
    <!-- 顶部导航栏 -->
    <view class="nav-header">
      <view class="nav-title">众创空间 · 智创未来</view>
      <view class="nav-actions">
        <view class="nav-icon-btn">
          <text class="nav-icon">🔔</text>
        </view>
      </view>
    </view>

    <!-- 公告栏（替代首页余额大盘） -->
    <view class="glass-card bulletin-card">
      <view class="card-header">
        <text class="card-label">公告栏</text>
        <text class="card-icon">📣</text>
      </view>
      <view class="bulletin-list">
        <view class="bulletin-item">
          <text class="dot">•</text>
          <text class="bulletin-text">本周开放：3D 打印机与激光切割机预约</text>
        </view>
        <view class="bulletin-item">
          <text class="dot">•</text>
          <text class="bulletin-text">每日打卡可获得虚拟币奖励（以实际规则为准）</text>
        </view>
        <view class="bulletin-item">
          <text class="dot">•</text>
          <text class="bulletin-text">设备/工位消费将自动生成流水记录</text>
        </view>
      </view>
    </view>

    <!-- 获取虚拟币入口 -->
    <view class="glass-card module-card">
      <view class="module-header">
        <view class="module-left">
          <text class="module-icon">💎</text>
          <text class="module-title">获取虚拟币</text>
        </view>
        <text class="module-sub">Earn</text>
      </view>
      <view class="grid-menu">
        <view class="grid-item" @tap="handleAction('checkin')">
          <view class="icon-box color-1">👤</view>
          <text class="grid-label">打卡</text>
        </view>
        <view class="grid-item" @tap="handleAction('achievement')">
          <view class="icon-box color-2">📝</view>
          <text class="grid-label">成果提交</text>
        </view>
        <view class="grid-item" @tap="handleAction('duty')">
          <view class="icon-box color-3">🧑‍💼</view>
          <text class="grid-label">值班任务</text>
        </view>
      </view>
    </view>

    <!-- 消费虚拟币入口 -->
    <view class="glass-card module-card">
      <view class="module-header">
        <view class="module-left">
          <text class="module-icon">🧾</text>
          <text class="module-title">消费虚拟币</text>
        </view>
        <text class="module-sub">Spend</text>
      </view>
      <view class="grid-menu">
        <view class="grid-item" @tap="handleAction('station')">
          <view class="icon-box color-3">🏢</view>
          <text class="grid-label">工位租赁</text>
        </view>
        <view class="grid-item" @tap="handleAction('device')">
          <view class="icon-box color-4">🛠️</view>
          <text class="grid-label">设备租用</text>
        </view>
        <view class="grid-item" @tap="handleAction('equipment')">
          <view class="icon-box color-5">📷</view>
          <text class="grid-label">器材借用</text>
        </view>
        <view class="grid-item" @tap="handleAction('venue')">
          <view class="icon-box color-1">🏛️</view>
          <text class="grid-label">场地短租</text>
        </view>
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
  uni.navigateTo({ url: '/pages/transactions/transactions' });
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
  if (type === 'duty') {
    uni.navigateTo({ url: '/pages/duty/duty' });
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
  if (type === 'equipment') {
    uni.navigateTo({ url: '/pages/equipment/equipment' });
    return;
  }
  if (type === 'venue') {
    uni.navigateTo({ url: '/pages/venue/venue' });
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
  padding-bottom: 140rpx; /* 预留底部 tabBar 空间 */
  justify-content: flex-start; /* 覆盖默认的居中 */
  gap: 32rpx;
}

.nav-header {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18rpx 18rpx;
  border-radius: $radius-lg;
  background: rgba($white, 0.72);
  backdrop-filter: blur(14px);
  box-shadow: $shadow-sm;
  
  .nav-title {
    font-size: 36rpx;
    font-weight: 800;
    color: $text-main;
  }
  
  .nav-icon-btn {
    width: 72rpx;
    height: 72rpx;
    border-radius: $radius-full;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba($bg-color, 0.85);
    box-shadow: $shadow-sm;
    transition: transform 0.2s;
  }

  .nav-icon-btn:active {
    transform: scale(0.98);
  }

  .nav-icon {
    font-size: 38rpx;
  }
}

.bulletin-card {
  background: linear-gradient(135deg, rgba(255,255,255,0.9), rgba(255,255,255,0.7));
  /* 与下方模块卡片左右边界对齐 */
  margin: 0;
  padding: 34rpx 28rpx;
}

.module-card {
  padding: 34rpx 28rpx;
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

.bulletin-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  margin-bottom: 18rpx;
}

.bulletin-item {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
}

.dot {
  font-size: 28rpx;
  color: $primary;
  line-height: 1.2;
}

.bulletin-text {
  flex: 1;
  font-size: 26rpx;
  color: $text-main;
  font-weight: 600;
  opacity: 0.92;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 20rpx;
  font-size: 28rpx;
  color: $primary;
  font-weight: 700;
}

.grid-menu {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
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
      &.color-5 { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
    }
    
    .grid-label {
      font-size: 24rpx;
      color: $text-main;
      font-weight: 600;
    }
  }
}

.module-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}

.module-left {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.module-icon {
  font-size: 34rpx;
}

.module-title {
  font-size: 30rpx;
  font-weight: 900;
  color: $text-main;
}

.module-sub {
  font-size: 22rpx;
  color: $text-light;
  font-weight: 800;
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

</style>
