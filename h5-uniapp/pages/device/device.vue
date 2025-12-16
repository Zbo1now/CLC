<template>
  <view class="container">
    <view class="shell">
      <view class="header panel">
        <view class="back-btn" @tap="goBack">←</view>
        <text class="title">设备租用</text>
      </view>

      <view class="tabs glass-card panel">
        <view :class="['tab', activeTab === 'list' ? 'active' : '']" @tap="activeTab = 'list'">
          <text class="tab-icon">🛠️</text>
          <text>设备列表</text>
        </view>
        <view :class="['tab', activeTab === 'mine' ? 'active' : '']" @tap="activeTab = 'mine'">
          <text class="tab-icon">🧾</text>
          <text>我的预约</text>
        </view>
      </view>

    <!-- 设备列表 -->
    <view v-if="activeTab === 'list'" class="content panel">
      <view class="glass-card filter-card">
        <view class="filter-title">
          <text class="filter-icon">⏱️</text>
          <text>预约时间段</text>
        </view>

        <view class="filter-row">
          <picker mode="date" :value="dateStr" @change="onDateChange">
            <view class="pill">
              <text class="pill-icon">📅</text>
              <text class="pill-text">{{ dateText }}</text>
            </view>
          </picker>
          <picker mode="time" :value="startTime" @change="onStartTimeChange">
            <view class="pill">
              <text class="pill-icon">🕑</text>
              <text class="pill-text">{{ startTime }}</text>
            </view>
          </picker>
          <picker mode="time" :value="endTime" @change="onEndTimeChange">
            <view class="pill">
              <text class="pill-icon">🕒</text>
              <text class="pill-text">{{ endTime }}</text>
            </view>
          </picker>
        </view>

        <view class="filter-actions">
          <view class="hint">* 选择时间后将自动刷新可预约状态</view>
        </view>
      </view>

      <view class="glass-card list-card">
        <view class="list-header">
          <text class="list-title">全部设备</text>
        </view>

        <view v-if="devices.length === 0" class="empty">
          <text class="empty-icon">🧰</text>
          <text class="empty-text">暂无设备数据</text>
          <text class="empty-sub">请先执行 db/device_schema.sql 初始化</text>
        </view>

        <view v-else class="device-list">
          <view class="device-item" v-for="d in devices" :key="d.id">
            <view class="device-left">
              <view class="code-row">
                <text class="code">{{ d.deviceName }}</text>

                <view :class="['badge', statusClass(d)]">
                  <text class="badge-dot"></text>
                  <text>{{ statusText(d) }}</text>
                </view>
              </view>

              <text class="meta">🏷️ {{ d.deviceType }}</text>
              <text class="meta">💳 {{ d.ratePerHour }} 币 / 小时</text>
              <text class="meta">⏱️ 预约：{{ dateText }} {{ startTime }} - {{ endTime }}</text>
            </view>

            <button class="primary-btn" :disabled="!d.available" @tap="confirmBooking(d)">
              <text class="btn-icon">🗓️</text>
              <text class="btn-text">{{ d.available ? '预约' : '不可预约' }}</text>
            </button>
          </view>
        </view>
      </view>
    </view>

    <!-- 我的预约 -->
    <view v-else class="content panel">
      <view class="glass-card lease-card">
        <view class="lease-header">
          <text class="lease-title">当前预约</text>
        </view>

        <view v-if="!myBooking" class="empty">
          <text class="empty-icon">🧾</text>
          <text class="empty-text">暂无预约</text>
          <text class="empty-sub">去“设备列表”选择一个可用设备吧</text>
          <button class="primary-btn" @tap="activeTab = 'list'">
            <text class="btn-icon">🛠️</text>
            <text class="btn-text">去预约</text>
          </button>
        </view>

        <view v-else class="lease-body">
          <view class="lease-top">
            <text class="lease-code">{{ myDevice?.deviceName || '—' }}</text>
            <view :class="['badge', bookingStatusClass(myBooking)]">
              <text class="badge-dot"></text>
              <text>{{ bookingStatusText(myBooking) }}</text>
            </view>
          </view>

          <text class="lease-line">🏷️ 类型：{{ myDevice?.deviceType || '—' }}</text>
          <text class="lease-line">💳 费率：{{ myDevice?.ratePerHour || 0 }} 币 / 小时</text>
          <text class="lease-line">🗓️ 预约：{{ fmt(myBooking.startAt) }} ～ {{ fmt(myBooking.endAt) }}</text>

          <text v-if="myBooking.actualStartAt" class="lease-line">▶️ 开始：{{ fmt(myBooking.actualStartAt) }}</text>
          <text v-if="myBooking.actualEndAt" class="lease-line">⏹️ 结束：{{ fmt(myBooking.actualEndAt) }}</text>

          <view class="actions">
            <button v-if="String(myBooking.status).toUpperCase() === 'RESERVED'" class="primary-btn" @tap="startUse">
              <text class="btn-icon">▶️</text>
              <text class="btn-text">开始使用</text>
            </button>
            <button v-else-if="String(myBooking.status).toUpperCase() === 'IN_USE'" class="primary-btn" @tap="finishUse">
              <text class="btn-icon">⏹️</text>
              <text class="btn-text">结束结算</text>
            </button>
          </view>

          <text class="hint">* 使用结束后按实际使用时长扣币</text>
        </view>
      </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue';
import { onShow, onHide } from '@dcloudio/uni-app';
import { baseUrl } from '../../common/config.js';

const activeTab = ref('list');
const devices = ref([]);

const myBooking = ref(null);
const myDevice = ref(null);

const dateStr = ref(todayDateStr());
const startTime = ref(defaultStartTime());
const endTime = ref(defaultEndTime());

let timer = null;

const dateText = computed(() => dateStr.value);

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

const goBack = () => {
  uni.navigateBack();
};

const buildTimestamp = (d, t) => `${d} ${t}`;

const onDateChange = async (e) => {
  const v = e?.detail?.value;
  if (!v) return;
  dateStr.value = v;
  await refreshDevices();
};

const onStartTimeChange = async (e) => {
  const v = e?.detail?.value;
  if (!v) return;
  startTime.value = v;
  if (!isEndAfterStart(startTime.value, endTime.value)) {
    endTime.value = plusOneHour(startTime.value);
  }
  await refreshDevices();
};

const onEndTimeChange = async (e) => {
  const v = e?.detail?.value;
  if (!v) return;
  endTime.value = v;
  await refreshDevices();
};

const refreshDevices = () => {
  const sessionId = getSessionId();
  const startAt = buildTimestamp(dateStr.value, startTime.value);
  const endAt = buildTimestamp(dateStr.value, endTime.value);

  if (!isEndAfterStart(startTime.value, endTime.value)) {
    uni.showToast({ title: '结束时间需晚于开始时间', icon: 'none' });
    return Promise.resolve();
  }

  return new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/devices?startAt=${encodeURIComponent(startAt)}&endAt=${encodeURIComponent(endAt)}`,
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
          devices.value = Array.isArray(res.data.data) ? res.data.data : [];
        } else {
          devices.value = [];
        }
        resolve();
      },
      fail: () => {
        devices.value = [];
        resolve();
      }
    });
  });
};

const refreshMine = () => {
  const sessionId = getSessionId();
  return new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/devices/me/booking`,
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
          myBooking.value = data.booking || null;
          myDevice.value = data.device || null;
        } else {
          myBooking.value = null;
          myDevice.value = null;
        }
        resolve();
      },
      fail: () => {
        myBooking.value = null;
        myDevice.value = null;
        resolve();
      }
    });
  });
};

const refreshAll = async () => {
  await refreshDevices();
  await refreshMine();
};

const confirmBooking = (d) => {
  if (!d.available) return;

  const startAt = buildTimestamp(dateStr.value, startTime.value);
  const endAt = buildTimestamp(dateStr.value, endTime.value);

  uni.showModal({
    title: '确认预约',
    content: `设备：${d.deviceName}\n时间：${startAt} ~ ${endAt}\n费率：${d.ratePerHour} 币/小时`,
    confirmText: '确认预约',
    success: (r) => {
      if (r.confirm) createBooking(d.id);
    }
  });
};

const createBooking = (deviceId) => {
  const sessionId = getSessionId();
  const startAt = buildTimestamp(dateStr.value, startTime.value);
  const endAt = buildTimestamp(dateStr.value, endTime.value);

  uni.showLoading({ title: '预约中...' });

  uni.request({
    url: `${baseUrl}/api/devices/${deviceId}/bookings`,
    method: 'POST',
    header: { 'Content-Type': 'application/json', 'X-Session-Id': sessionId },
    withCredentials: true,
    data: { startAt, endAt },
    success: async (res) => {
      uni.hideLoading();
      if (res.statusCode === 401) {
        handle401();
        return;
      }
      if (res.statusCode === 200 && res.data && res.data.success) {
        uni.showToast({ title: '预约成功', icon: 'success' });
        await refreshAll();
        activeTab.value = 'mine';
      } else {
        uni.showToast({ title: (res.data && res.data.message) || '预约失败', icon: 'none' });
        await refreshDevices();
      }
    },
    fail: () => {
      uni.hideLoading();
      uni.showToast({ title: '网络异常', icon: 'none' });
    }
  });
};

const startUse = () => {
  if (!myBooking.value) return;
  const sessionId = getSessionId();

  uni.showModal({
    title: '开始使用',
    content: '确认开始使用该设备？',
    confirmText: '开始',
    success: (r) => {
      if (!r.confirm) return;

      uni.showLoading({ title: '开始中...' });
      uni.request({
        url: `${baseUrl}/api/devices/bookings/${myBooking.value.id}/start`,
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
            uni.showToast({ title: '已开始', icon: 'success' });
            await refreshAll();
          } else {
            uni.showToast({ title: (res.data && res.data.message) || '开始失败', icon: 'none' });
            await refreshMine();
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

const finishUse = () => {
  if (!myBooking.value) return;
  const sessionId = getSessionId();

  uni.showModal({
    title: '结束结算',
    content: '结束后将按实际使用时长扣币。确认结束？',
    confirmText: '结束',
    success: (r) => {
      if (!r.confirm) return;

      uni.showLoading({ title: '结算中...' });
      uni.request({
        url: `${baseUrl}/api/devices/bookings/${myBooking.value.id}/finish`,
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
            const data = res.data.data || {};
            const minutes = data.minutes ?? 0;
            const cost = data.cost ?? 0;
            uni.showModal({
              title: '结算完成',
              content: `实际使用：${minutes} 分钟\n扣费：-${cost} 币`,
              showCancel: false
            });
            await refreshAll();
          } else {
            uni.showToast({ title: (res.data && res.data.message) || '结算失败', icon: 'none' });
            await refreshMine();
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

const statusText = (d) => {
  const runtime = String(d.runtimeStatus || '').toUpperCase();
  if (runtime === 'MAINTENANCE') return '维护中';
  if (runtime === 'IN_USE') return '使用中';
  return d.available ? '空闲' : '不可用';
};

const statusClass = (d) => {
  const runtime = String(d.runtimeStatus || '').toUpperCase();
  if (runtime === 'MAINTENANCE') return 'badge-maintenance';
  if (runtime === 'IN_USE') return 'badge-using';
  return d.available ? 'badge-free' : 'badge-busy';
};

const bookingStatusText = (b) => {
  const s = String(b.status || '').toUpperCase();
  if (s === 'IN_USE') return '使用中';
  if (s === 'RESERVED') return '已预约';
  if (s === 'FINISHED') return '已结束';
  return s || '—';
};

const bookingStatusClass = (b) => {
  const s = String(b.status || '').toUpperCase();
  if (s === 'IN_USE') return 'badge-using';
  if (s === 'RESERVED') return 'badge-free';
  if (s === 'FINISHED') return 'badge-expired';
  return 'badge-free';
};

const fmt = (v) => {
  if (!v) return '—';
  // 兼容：number(ms) / ISO string / "yyyy-MM-dd HH:mm:ss"
  if (typeof v === 'number') {
    const d = new Date(v);
    if (!Number.isNaN(d.getTime())) {
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
    }
  }
  const s = String(v);
  const t = s.replace('T', ' ');
  // 常见格式：2025-12-16 14:00:00 或 2025-12-16 14:00
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}/.test(t)) {
    return t.slice(0, 16);
  }
  // 兜底：尝试 Date 解析
  const d = new Date(s);
  if (!Number.isNaN(d.getTime())) {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  }
  return t.slice(0, 16);
};

// 使用 picker 组件选择日期/时间，兼容各端

onShow(async () => {
  const userInfo = uni.getStorageSync('userInfo');
  if (!userInfo) {
    uni.showToast({ title: '请先登录', icon: 'none' });
    setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 1000);
    return;
  }

  await refreshAll();

  if (timer) clearInterval(timer);
  timer = setInterval(async () => {
    if (activeTab.value === 'list') {
      await refreshDevices();
      return;
    }
    await refreshMine();
  }, 5000);
});

onHide(() => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
});

function todayDateStr() {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function defaultStartTime() {
  const d = new Date();
  const h = d.getHours();
  const next = (h + 1) % 24;
  return `${String(next).padStart(2, '0')}:00`;
}

function defaultEndTime() {
  return plusOneHour(defaultStartTime());
}

function plusOneHour(t) {
  const m = /^([0-1]\d|2[0-3]):([0-5]\d)$/.exec(String(t || ''));
  if (!m) return '01:00';
  const hh = parseInt(m[1], 10);
  const mm = m[2];
  const next = (hh + 1) % 24;
  return `${String(next).padStart(2, '0')}:${mm}`;
}

function normalizeTime(input) {
  const s = String(input || '').trim();
  if (!s) return null;
  const m = /^([0-1]?\d|2[0-3])[:：]([0-5]\d)$/.exec(s);
  if (!m) return null;
  return `${String(parseInt(m[1], 10)).padStart(2, '0')}:${m[2]}`;
}

function isEndAfterStart(start, end) {
  const s = normalizeTime(start);
  const e = normalizeTime(end);
  if (!s || !e) return false;
  const [sh, sm] = s.split(':').map(Number);
  const [eh, em] = e.split(':').map(Number);
  return eh * 60 + em > sh * 60 + sm;
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
  background: rgba($white, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  color: $text-main;
  box-shadow: $shadow-sm;
  transition: transform 0.2s;
}

.back-btn:active {
  transform: scale(0.98);
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
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  padding: 16rpx 0;
  border-radius: $radius-lg;
  color: $text-light;
  background: rgba($bg-color, 0.8);
  font-weight: 700;
  transition: all 0.25s ease;
  font-size: 26rpx;
}

.tab.active {
  color: $primary;
  background: rgba($white, 0.95);
  box-shadow: $shadow-sm;
}

.tab-icon {
  font-size: 30rpx;
}

.content {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24rpx;
}

.filter-card {
  width: 100%;
  padding: 32rpx 28rpx;
}

.filter-title {
  display: flex;
  align-items: center;
  gap: 12rpx;
  font-size: 30rpx;
  font-weight: 800;
  color: $text-main;
  margin-bottom: 18rpx;
}

.filter-icon {
  font-size: 32rpx;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.pill {
  padding: 18rpx 22rpx;
  border-radius: 9999rpx;
  background: rgba(248, 250, 252, 1);
  border: 2rpx solid rgba(99, 102, 241, 0.12);
  display: flex;
  align-items: center;
  gap: 12rpx;
  transition: transform 0.2s;
}

.pill:active {
  transform: scale(0.98);
}

.pill-icon {
  font-size: 30rpx;
}

.pill-text {
  font-size: 28rpx;
  font-weight: 700;
  color: $text-main;
}

.filter-actions {
  margin-top: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
}

.hint {
  font-size: 24rpx;
  color: $text-light;
}

.list-card {
  width: 100%;
  padding: 34rpx 28rpx;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22rpx;
}

.list-title {
  font-size: 32rpx;
  font-weight: 800;
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
  font-weight: 700;
  color: $text-main;
  display: block;
}

.empty-sub {
  font-size: 24rpx;
  color: $text-light;
  display: block;
  margin-top: 10rpx;
}

.device-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.device-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  padding: 24rpx 18rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.7);
  border: 2rpx solid rgba(99, 102, 241, 0.08);
  transition: transform 0.15s ease;
}

.device-item:active {
  transform: scale(0.995);
}

.device-left {
  flex: 1;
  min-width: 0;
}

.code-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin-bottom: 10rpx;
}

.code {
  font-size: 30rpx;
  font-weight: 900;
  color: $text-main;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta {
  display: block;
  font-size: 24rpx;
  color: $text-light;
  margin-top: 6rpx;
}

.badge {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 10rpx 14rpx;
  border-radius: 9999rpx;
  font-size: 22rpx;
  font-weight: 800;
}

.badge-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 9999rpx;
  background: currentColor;
}

.badge-free {
  background: rgba(34, 197, 94, 0.12);
  color: rgb(22, 163, 74);
}

.badge-using {
  background: rgba(245, 158, 11, 0.14);
  color: rgb(217, 119, 6);
}

.badge-busy {
  background: rgba(239, 68, 68, 0.12);
  color: rgb(220, 38, 38);
}

.badge-maintenance {
  background: rgba(100, 116, 139, 0.14);
  color: rgb(71, 85, 105);
}

.badge-expired {
  background: rgba(148, 163, 184, 0.16);
  color: rgb(100, 116, 139);
}

.primary-btn {
  background: $primary-gradient;
  color: $white;
  border-radius: 9999rpx;
  padding: 0 22rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  font-weight: 800;
  box-shadow: 0 10rpx 20rpx rgba(99, 102, 241, 0.25);
}

.primary-btn[disabled] {
  opacity: 0.55;
}

.ghost-btn {
  background: rgba(255, 255, 255, 0.6);
  color: $text-main;
  border-radius: 9999rpx;
  padding: 0 22rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  font-weight: 800;
  border: 2rpx solid rgba(99, 102, 241, 0.12);
}

.btn-icon {
  font-size: 28rpx;
}

.btn-text {
  font-size: 26rpx;
}

.lease-card {
  width: 100%;
  padding: 34rpx 28rpx;
}

.lease-header {
  margin-bottom: 18rpx;
}

.lease-title {
  font-size: 32rpx;
  font-weight: 800;
  color: $text-main;
}

.lease-body {
  padding-top: 8rpx;
}

.lease-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 18rpx;
}

.lease-code {
  font-size: 34rpx;
  font-weight: 900;
  color: $text-main;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lease-line {
  display: block;
  font-size: 26rpx;
  color: $text-light;
  margin-top: 10rpx;
}

.actions {
  margin-top: 22rpx;
  display: flex;
  gap: 14rpx;
}

.actions button {
  flex: 1;
}

</style>
