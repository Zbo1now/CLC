<template>
  <view class="container">
    <view class="shell">
      <view class="header panel">
        <view class="back-btn" @tap="goBack">←</view>
        <text class="title">流水明细</text>
      </view>

      <view class="content panel">
        <view class="glass-card list-card">
          <view class="list-header">
            <text class="list-title">全部流水</text>
            <text class="more-hint">{{ hasMore ? '上拉加载更多' : '已到底' }}</text>
          </view>

          <view v-if="loading && txns.length === 0" class="empty">
            <text class="empty-icon">⏳</text>
            <text class="empty-text">加载中…</text>
          </view>

          <view v-else-if="!loading && txns.length === 0" class="empty">
            <text class="empty-icon">🧾</text>
            <text class="empty-text">暂无流水</text>
            <text class="empty-sub">完成打卡、成果提交、工位/设备消费后会在这里显示</text>
          </view>

          <view v-else class="transaction-list">
            <view class="list-item" v-for="item in txns" :key="item.id">
              <view class="item-left">
                <text class="item-icon">{{ item.amount > 0 ? '📥' : '📤' }}</text>
                <view class="item-info">
                  <text class="item-desc">{{ item.description || '—' }}</text>
                  <text class="item-date">{{ formatTime(item.createdAt) }}</text>
                </view>
              </view>
              <text :class="['item-amount', item.amount > 0 ? 'income' : 'expense']">
                {{ item.amount > 0 ? '+' : '' }}{{ item.amount }}
              </text>
            </view>

            <view v-if="loading && txns.length > 0" class="load-more">加载中…</view>
            <view v-else-if="!hasMore && txns.length > 0" class="load-more muted">已到底</view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onShow, onReachBottom } from '@dcloudio/uni-app';
import { baseUrl } from '../../common/config.js';

const txns = ref([]);
const loading = ref(false);
const hasMore = ref(true);
const offset = ref(0);
const limit = 20;

const getSessionId = () => {
  let sessionId = '';
  const cookieStr = uni.getStorageSync('cookie');
  if (cookieStr && cookieStr.includes('JSESSIONID=')) {
    sessionId = cookieStr.split('JSESSIONID=')[1].split(';')[0];
  }
  return sessionId;
};

const handle401 = () => {
  uni.showToast({ title: '登录已过期', icon: 'none' });
  setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 1200);
};

const formatTime = (createdAt) => {
  if (!createdAt) return '';
  const d = new Date(createdAt);
  if (Number.isNaN(d.getTime())) return String(createdAt).replace('T', ' ').slice(0, 16);

  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hh = String(d.getHours()).padStart(2, '0');
  const mm = String(d.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${day} ${hh}:${mm}`;
};

const goBack = () => {
  uni.navigateBack();
};

const resetAndFetch = async () => {
  txns.value = [];
  offset.value = 0;
  hasMore.value = true;
  await fetchMore();
};

const fetchMore = async () => {
  if (loading.value) return;
  if (!hasMore.value) return;

  const sessionId = getSessionId();
  loading.value = true;

  await new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/transactions/me?offset=${offset.value}&limit=${limit}`,
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
          const list = Array.isArray(data.transactions) ? data.transactions : [];
          txns.value = txns.value.concat(list);
          offset.value = data.nextOffset ?? (offset.value + list.length);
          hasMore.value = !!data.hasMore;
        }
        resolve();
      },
      fail: () => resolve()
    });
  });

  loading.value = false;
};

onShow(async () => {
  const userInfo = uni.getStorageSync('userInfo');
  if (!userInfo) return;
  await resetAndFetch();
});

onReachBottom(async () => {
  await fetchMore();
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
  justify-content: flex-start;
  gap: 20rpx;
}

.back-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: $radius-full;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba($white, 0.7);
  box-shadow: $shadow-sm;
  font-size: 36rpx;
}

.title {
  font-size: 40rpx;
  font-weight: 900;
  color: $text-main;
}

.content {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24rpx;
}

.list-card {
  width: 100%;
  padding: 30rpx;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.list-title {
  font-size: 30rpx;
  font-weight: 900;
  color: $text-main;
}

.more-hint {
  font-size: 24rpx;
  color: $text-light;
}

.transaction-list {
  width: 100%;
}

.list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1px solid rgba(0,0,0,0.05);
}

.list-item:last-child {
  border-bottom: none;
}

.item-left {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.item-icon {
  font-size: 32rpx;
}

.item-info {
  display: flex;
  flex-direction: column;
}

.item-desc {
  font-size: 28rpx;
  color: $text-main;
  font-weight: 700;
}

.item-date {
  font-size: 22rpx;
  color: $text-light;
}

.item-amount {
  font-size: 30rpx;
  font-weight: 900;
}

.item-amount.income {
  color: #22c55e;
}

.item-amount.expense {
  color: $text-main;
}

.empty {
  padding: 60rpx 20rpx;
  text-align: center;
}

.empty-icon {
  font-size: 64rpx;
  display: block;
  margin-bottom: 14rpx;
}

.empty-text {
  font-size: 30rpx;
  font-weight: 900;
  color: $text-main;
  display: block;
  margin-bottom: 8rpx;
}

.empty-sub {
  font-size: 24rpx;
  color: $text-light;
}

.load-more {
  padding: 18rpx 0 0;
  text-align: center;
  font-size: 24rpx;
  color: $text-main;
  font-weight: 700;
}

.load-more.muted {
  color: $text-light;
}
</style>
