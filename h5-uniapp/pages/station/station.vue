<template>
  <view class="container">
    <view class="shell">
      <view class="header panel">
        <view class="back-btn" @tap="goBack">←</view>
        <text class="title">工位租赁</text>
      </view>

      <view class="tabs glass-card panel">
        <view :class="['tab', activeTab === 'list' ? 'active' : '']" @tap="activeTab = 'list'">
          <text class="tab-icon">🏢</text>
          <text>工位列表</text>
        </view>
        <view :class="['tab', activeTab === 'mine' ? 'active' : '']" @tap="activeTab = 'mine'">
          <text class="tab-icon">🧾</text>
          <text>我的租约</text>
        </view>
      </view>

      <!-- 工位列表 -->
      <view v-if="activeTab === 'list'" class="content panel">
        <view class="glass-card list-card">
        <view class="list-header">
          <text class="list-title">全部工位</text>
        </view>

        <view v-if="stations.length === 0" class="empty">
          <text class="empty-icon">🪑</text>
          <text class="empty-text">暂无工位数据</text>
          <text class="empty-sub">请先执行 db/station_schema.sql 初始化</text>
        </view>

        <view v-else class="station-list">
          <view class="station-item" v-for="s in stations" :key="s.id">
            <view class="station-left">
              <view class="code-row">
                <text class="code">{{ s.stationCode }}</text>
                <view :class="['badge', s.occupied ? 'badge-rented' : 'badge-free']">
                  <text class="badge-dot"></text>
                  <text>{{ s.occupied ? '已租' : '空闲' }}</text>
                </view>
              </view>
              <text class="location">📍 {{ s.location }}</text>
              <text class="rent">💳 {{ s.monthlyRent }} 币 / 月</text>
            </view>

            <button class="rent-btn" :disabled="s.occupied" @tap="confirmRent(s)">
              <text v-if="!s.occupied" class="btn-icon">🔑</text>
              <text class="btn-text">{{ s.occupied ? '已租用' : '租用' }}</text>
            </button>
          </view>
        </view>
        </view>
      </view>

      <!-- 我的租约 -->
      <view v-else class="content panel">
        <view class="glass-card lease-card">
        <view class="lease-header">
          <text class="lease-title">当前租约</text>
        </view>

        <view v-if="!lease" class="empty">
          <text class="empty-icon">🧾</text>
          <text class="empty-text">暂无租约</text>
          <text class="empty-sub">去“工位列表”选择一个空闲工位吧</text>
          <button class="primary-btn" @tap="activeTab = 'list'">
            <text class="btn-icon">🏢</text>
            <text class="btn-text">去租用</text>
          </button>
        </view>

        <view v-else class="lease-body">
          <view class="lease-top">
            <text class="lease-code">{{ workstation?.stationCode || '—' }}</text>
            <view :class="['badge', isExpired ? 'badge-expired' : 'badge-free']">
              <text class="badge-dot"></text>
              <text>{{ isExpired ? '已到期' : '生效中' }}</text>
            </view>
          </view>

          <text class="lease-line">📍 位置：{{ workstation?.location || '—' }}</text>
          <text class="lease-line">💳 月租：{{ lease.monthlyRent }} 币 / 月</text>
          <text class="lease-line">🗓️ 租期：{{ monthText(lease.startMonth) }} ～ {{ monthText(lease.endMonth) }}</text>

          <view class="actions">
            <button class="primary-btn" @tap="renewLease">
              <text class="btn-icon">🔁</text>
              <text class="btn-text">续租 1 个月</text>
            </button>
            <button class="ghost-btn" :disabled="!isExpired" @tap="releaseLease">
              <text class="btn-text">释放</text>
            </button>
          </view>

          <text class="hint">
            * 按自然月计费，续租会自动扣除虚拟币。
          </text>
        </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { baseUrl } from '../../common/config.js';

const activeTab = ref('list');
const stations = ref([]);
const lease = ref(null);
const workstation = ref(null);

const getSessionId = () => {
  let sessionId = '';
  const cookieStr = uni.getStorageSync('cookie');
  if (cookieStr && cookieStr.includes('JSESSIONID=')) {
    sessionId = cookieStr.split('JSESSIONID=')[1].split(';')[0];
  }
  return sessionId;
};

const goBack = () => {
  uni.navigateBack();
};

const currentMonthStart = () => {
  const now = new Date();
  const d = new Date(now.getFullYear(), now.getMonth(), 1);
  return d;
};

const isExpired = computed(() => {
  if (!lease.value || !lease.value.endMonth) return false;
  const end = new Date(String(lease.value.endMonth).slice(0, 10));
  return end.getTime() < currentMonthStart().getTime();
});

const monthText = (v) => {
  if (!v) return '—';
  const s = String(v);
  // 期望为 YYYY-MM-01 或 ISO，展示到月份
  return s.slice(0, 7);
};

const handle401 = () => {
  uni.showToast({ title: '登录已过期', icon: 'none' });
  setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 1200);
};

const fetchStations = () => {
  const sessionId = getSessionId();
  return new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/stations`,
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
          stations.value = Array.isArray(res.data.data) ? res.data.data : [];
        } else {
          stations.value = [];
        }
        resolve();
      },
      fail: () => {
        stations.value = [];
        resolve();
      }
    });
  });
};

const fetchMyLease = () => {
  const sessionId = getSessionId();
  return new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/stations/me/lease`,
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
          lease.value = data.lease || null;
          workstation.value = data.workstation || null;
        } else {
          lease.value = null;
          workstation.value = null;
        }
        resolve();
      },
      fail: () => {
        lease.value = null;
        workstation.value = null;
        resolve();
      }
    });
  });
};

const refreshAll = async () => {
  uni.vibrateShort?.();
  await fetchStations();
  await fetchMyLease();
};


const confirmRent = (s) => {
  uni.showModal({
    title: '确认租用',
    content: `租用工位 ${s.stationCode}\n月租：${s.monthlyRent} 币`,
    confirmText: '确认租用',
    success: (r) => {
      if (r.confirm) rentStation(s.id);
    }
  });
};

const rentStation = (stationId) => {
  const sessionId = getSessionId();
  uni.showLoading({ title: '租用中...' });

  uni.request({
    url: `${baseUrl}/api/stations/${stationId}/lease`,
    method: 'POST',
    header: { 'Content-Type': 'application/json', 'X-Session-Id': sessionId },
    withCredentials: true,
    data: { months: 1 },
    success: async (res) => {
      uni.hideLoading();
      if (res.statusCode === 401) {
        handle401();
        return;
      }
      if (res.statusCode === 200 && res.data && res.data.success) {
        uni.showToast({ title: '租赁成功', icon: 'success' });
        await refreshAll();
        activeTab.value = 'mine';
      } else {
        uni.showToast({ title: (res.data && res.data.message) || '租赁失败', icon: 'none' });
      }
    },
    fail: () => {
      uni.hideLoading();
      uni.showToast({ title: '网络异常', icon: 'none' });
    }
  });
};

const renewLease = () => {
  const sessionId = getSessionId();
  uni.showLoading({ title: '续租中...' });

  uni.request({
    url: `${baseUrl}/api/stations/me/lease/renew`,
    method: 'POST',
    header: { 'Content-Type': 'application/json', 'X-Session-Id': sessionId },
    withCredentials: true,
    data: { months: 1 },
    success: async (res) => {
      uni.hideLoading();
      if (res.statusCode === 401) {
        handle401();
        return;
      }
      if (res.statusCode === 200 && res.data && res.data.success) {
        uni.showToast({ title: '续租成功', icon: 'success' });
        await refreshAll();
      } else {
        uni.showToast({ title: (res.data && res.data.message) || '续租失败', icon: 'none' });
      }
    },
    fail: () => {
      uni.hideLoading();
      uni.showToast({ title: '网络异常', icon: 'none' });
    }
  });
};

const releaseLease = () => {
  if (!isExpired.value) return;
  const sessionId = getSessionId();

  uni.showModal({
    title: '确认释放',
    content: '租约已到期，释放后该工位可被其他团队租用。',
    confirmText: '释放',
    success: (r) => {
      if (!r.confirm) return;

      uni.showLoading({ title: '释放中...' });
      uni.request({
        url: `${baseUrl}/api/stations/me/lease/release`,
        method: 'POST',
        header: { 'X-Session-Id': sessionId },
        withCredentials: true,
        success: async (res) => {
          uni.hideLoading();
          if (res.statusCode === 401) {
            handle401();
            return;
          }
          if (res.statusCode === 200 && res.data && res.data.success) {
            uni.showToast({ title: '已释放', icon: 'success' });
            await refreshAll();
          } else {
            uni.showToast({ title: (res.data && res.data.message) || '释放失败', icon: 'none' });
          }
        },
        fail: () => {
          uni.hideLoading();
          uni.showToast({ title: '网络异常', icon: 'none' });
        }
      });
    }
  });
};

onShow(async () => {
  const userInfo = uni.getStorageSync('userInfo');
  if (!userInfo) {
    uni.showToast({ title: '请先登录', icon: 'none' });
    setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 1000);
    return;
  }

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
  color: $text-main;
  transition: transform 0.15s ease;
}

.back-btn:active {
  transform: scale(0.96);
}

.title {
  font-size: 40rpx;
  font-weight: 800;
  color: $text-main;
}

.tabs {
  padding: 14rpx;
  display: flex;
  gap: 14rpx;
  border-radius: $radius-lg;
}

.tab {
  flex: 1;
  border-radius: $radius-lg;
  padding: 18rpx 0;
  font-weight: 700;
  color: $text-light;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  background: rgba($bg-color, 0.8);
  transition: all 0.25s ease;
}

.tab.active {
  color: $primary;
  box-shadow: $shadow-sm;
  background: rgba($white, 0.95);
}

.tab-icon {
  font-size: 34rpx;
}

.content {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.list-card, .lease-card {
  width: 100%;
  padding: 32rpx 28rpx;
}

.list-header, .lease-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 22rpx;
}

.list-title, .lease-title {
  font-size: 32rpx;
  font-weight: 800;
  color: $text-main;
}


.empty {
  padding: 40rpx 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
  color: $text-light;
}

.empty-icon {
  font-size: 64rpx;
}

.empty-text {
  font-size: 30rpx;
  font-weight: 800;
  color: $text-main;
}

.empty-sub {
  font-size: 24rpx;
}

.station-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.station-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 22rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.6);
  border: 2rpx solid rgba($primary, 0.06);
  transition: transform 0.2s ease;
}

.station-item:active {
  transform: scale(0.99);
}

.station-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.code-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.code {
  font-size: 38rpx;
  font-weight: 900;
  color: $text-main;
}

.location, .rent {
  font-size: 24rpx;
  color: $text-light;
}

.badge {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  padding: 8rpx 14rpx;
  border-radius: $radius-full;
  font-size: 22rpx;
  font-weight: 800;
}

.badge-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: $radius-full;
  background: currentColor;
  opacity: 0.9;
}

.badge-free {
  color: $primary;
  background: rgba($primary, 0.12);
}

.badge-rented {
  color: $text-light;
  background: rgba($text-light, 0.14);
}

.badge-expired {
  color: $secondary;
  background: rgba($secondary, 0.14);
}

.rent-btn {
  min-width: 160rpx;
  background: $primary-gradient;
  color: $white;
  border-radius: $radius-full;
  padding: 20rpx 0;
  font-size: 28rpx;
  font-weight: 900;
  box-shadow: 0 14rpx 30rpx rgba($primary, 0.24);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  transition: transform 0.18s ease, opacity 0.18s ease, filter 0.18s ease;
}

.rent-btn:active {
  transform: scale(0.98);
}

.rent-btn:disabled {
  filter: grayscale(0.35);
  opacity: 0.55;
  box-shadow: none;
}

.lease-body {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.lease-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
}

.lease-code {
  font-size: 42rpx;
  font-weight: 900;
  color: $text-main;
}

.lease-line {
  font-size: 26rpx;
  color: $text-light;
}

.actions {
  display: flex;
  gap: 14rpx;
  margin-top: 14rpx;
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

.ghost-btn {
  width: 200rpx;
  background: rgba(255, 255, 255, 0.65);
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

.ghost-btn:disabled {
  opacity: 0.45;
}

.hint {
  margin-top: 10rpx;
  font-size: 22rpx;
  color: $text-light;
}
</style>
