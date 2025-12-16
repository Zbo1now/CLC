<template>
  <view class="container">
    <view class="shell">
      <view class="header panel">
        <view class="back-btn" @tap="goBack">←</view>
        <text class="title">场地短租</text>
      </view>

      <view class="tabs glass-card panel">
        <view :class="['tab', activeTab === 'list' ? 'active' : '']" @tap="switchTab('list')">
          <text class="tab-icon">🏛️</text>
          <text>场地列表</text>
        </view>
        <view :class="['tab', activeTab === 'mine' ? 'active' : '']" @tap="switchTab('mine')">
          <text class="tab-icon">🧾</text>
          <text>我的预约</text>
        </view>
      </view>

      <!-- 场地列表 -->
      <view v-if="activeTab === 'list'" class="content panel">
        <view class="glass-card filter-card">
          <view class="filter-header">
            <text class="filter-title">选择时间段</text>
            <text class="filter-sub">仅支持整点预约</text>
          </view>

          <view class="filter-grid">
            <view class="picker-item">
              <text class="picker-label">日期</text>
              <picker mode="date" :value="date" @change="onDateChange">
                <view class="picker-value">{{ date || '请选择' }}</view>
              </picker>
            </view>
            <view class="picker-item">
              <text class="picker-label">开始</text>
              <picker mode="selector" :range="hourOptions" :value="startHourIndex" @change="onStartChange">
                <view class="picker-value">{{ startTime || '请选择' }}</view>
              </picker>
            </view>
            <view class="picker-item">
              <text class="picker-label">结束</text>
              <picker mode="selector" :range="endHourOptions" :value="endHourIndex" @change="onEndChange">
                <view class="picker-value">{{ endTime || '请选择' }}</view>
              </picker>
            </view>
          </view>

          <view class="filter-foot">
            <text class="filter-hint">{{ rangeHint }}</text>
            <button class="ghost-btn" @tap="resetRange">
              <text class="btn-text">重置</text>
            </button>
          </view>
        </view>

        <view class="glass-card list-card">
          <view class="list-header">
            <text class="list-title">可租场地</text>
            <text class="list-sub">选择场地后提交预约</text>
          </view>

          <view v-if="venues.length === 0" class="empty">
            <text class="empty-icon">🏛️</text>
            <text class="empty-text">暂无场地数据</text>
            <text class="empty-sub">请先执行 db/venue_schema.sql 初始化</text>
          </view>

          <view v-else class="venue-list">
            <view class="venue-item" v-for="v in venues" :key="v.id" :class="selectedVenue?.id === v.id ? 'selected' : ''">
              <view class="venue-left">
                <view class="name-row">
                  <text class="name">{{ v.venueName }}</text>
                  <view :class="['badge', stateBadgeClass(v)]">
                    <text class="badge-dot"></text>
                    <text>{{ stateText(v) }}</text>
                  </view>
                </view>
                <text class="meta">🏷️ {{ v.venueType }}</text>
                <text class="meta">💳 {{ v.ratePerHour }} 币 / 小时</text>
                <text class="meta small">⏱️ {{ availabilityText(v) }}</text>
              </view>

              <button class="primary-btn" :disabled="!canChooseVenue(v)" @tap="selectVenue(v)">
                <text class="btn-icon">🗓️</text>
                <text class="btn-text">预约</text>
              </button>
            </view>
          </view>
        </view>

        <!-- 占用情况 + 提交预约 -->
        <view v-if="selectedVenue" class="glass-card booking-card">
          <view class="booking-header">
            <view class="booking-title">
              <text class="booking-icon">🧷</text>
              <text>预约确认</text>
            </view>
            <view class="booking-right">
              <view class="mini-pill">{{ selectedVenue.venueName }}</view>
            </view>
          </view>

          <view class="booking-info">
            <view class="line">
              <text class="k">时间段</text>
              <text class="v">{{ date || '—' }} {{ startTime || '—' }} – {{ endTime || '—' }}</text>
            </view>
            <view class="line">
              <text class="k">预扣</text>
              <text class="v">{{ costText }}</text>
            </view>
          </view>

          <view class="occupied">
            <text class="occ-title">当天占用</text>
            <view v-if="occupied.length === 0" class="occ-empty">暂无占用</view>
            <view v-else class="occ-list">
              <view class="occ-item" v-for="(o, idx) in occupied" :key="idx">
                <text class="occ-time">{{ fmtDT(o.startTime) }} – {{ fmtDT(o.endTime) }}</text>
                <text class="occ-state">{{ o.status === 'IN_USE' ? '使用中' : '已预约' }}</text>
              </view>
            </view>
          </view>

          <view class="actions">
            <button class="primary-btn" :disabled="!canSubmit" @tap="submitBooking">
              <text class="btn-icon">📨</text>
              <text class="btn-text">提交预约</text>
            </button>
          </view>

          <view class="hint">* 预约开始后 10 分钟内未确认将自动取消，预扣币不退回</view>
        </view>
      </view>

      <!-- 我的预约 -->
      <view v-else class="content panel">
        <view class="glass-card mine-card">
          <view class="mine-header">
            <text class="mine-title">当前预约</text>
          </view>

          <view v-if="!myBooking" class="empty">
            <text class="empty-icon">🧾</text>
            <text class="empty-text">暂无预约</text>
            <text class="empty-sub">去“场地列表”选择时间段并提交预约</text>
            <button class="primary-btn" @tap="switchTab('list')">
              <text class="btn-icon">🏛️</text>
              <text class="btn-text">去预约</text>
            </button>
          </view>

          <view v-else class="mine-body">
            <view class="top">
              <text class="code">{{ myVenue?.venueName || '—' }}</text>
              <view :class="['badge', mineBadgeClass]">
                <text class="badge-dot"></text>
                <text>{{ mineStatusText }}</text>
              </view>
            </view>

            <text class="line">🏷️ 类型：{{ myVenue?.venueType || '—' }}</text>
            <text class="line">🗓️ 时间：{{ fmtDT(myBooking.startTime) }} – {{ fmtDT(myBooking.endTime) }}</text>
            <text class="line">💳 预扣：{{ myBooking.heldCost || 0 }} 币</text>

            <view v-if="showConfirm" class="confirm-box">
              <view class="confirm-hint">* 请在开始后 10 分钟内确认使用</view>
              <button class="primary-btn" @tap="confirmUse">
                <text class="btn-icon">✅</text>
                <text class="btn-text">确认使用</text>
              </button>
            </view>

            <view class="hint">* 未在 10 分钟内确认将自动取消，预扣币不退回</view>
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

const date = ref('');
const startTime = ref('');
const endTime = ref('');

// 仅支持整点：用小时下拉替代 time picker，避免出现分钟选项
const hourOptions = Array.from({ length: 24 }, (_, i) => `${String(i).padStart(2, '0')}:00`);

const startHourIndex = computed(() => {
  const idx = hourOptions.indexOf(startTime.value);
  return idx >= 0 ? idx : 0;
});

const endHourOptions = computed(() => {
  const sIdx = hourOptions.indexOf(startTime.value);
  if (sIdx < 0) return hourOptions;
  return hourOptions.slice(sIdx + 1);
});

const endHourIndex = computed(() => {
  const idx = endHourOptions.value.indexOf(endTime.value);
  return idx >= 0 ? idx : 0;
});

const venues = ref([]);
const selectedVenue = ref(null);
const occupied = ref([]);

const myBooking = ref(null);
const myVenue = ref(null);

const timer = ref(null);

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

const goBack = () => uni.navigateBack();

const switchTab = async (t) => {
  activeTab.value = t;
  if (t === 'mine') await refreshMine();
};

const rangeOk = computed(() => {
  if (!date.value || !startTime.value || !endTime.value) return false;
  if (!isOnTheHour(startTime.value) || !isOnTheHour(endTime.value)) return false;
  const a = toDT(date.value, startTime.value);
  const b = toDT(date.value, endTime.value);
  return a && b && b > a;
});

const hours = computed(() => {
  if (!rangeOk.value) return 0;
  const a = toDT(date.value, startTime.value);
  const b = toDT(date.value, endTime.value);
  return Math.round(((b - a) / 3600000) * 100) / 100;
});

const rangeHint = computed(() => {
  if (!date.value || !startTime.value || !endTime.value) return '请选择日期与开始/结束时间';
  if (!isOnTheHour(startTime.value) || !isOnTheHour(endTime.value)) return '仅支持整点（分钟为 00）';
  if (!rangeOk.value) return '结束时间需晚于开始时间';
  return `已选择 ${hours.value} 小时`; 
});

const costText = computed(() => {
  if (!selectedVenue.value || !rangeOk.value) return '—';
  const rate = Number(selectedVenue.value.ratePerHour || 0);
  const h = hours.value;
  if (!Number.isFinite(rate) || !Number.isFinite(h) || h <= 0) return '—';
  if (!Number.isInteger(h)) return '—';
  const cost = rate * h;
  return `${cost} 币（${h} 小时 × ${rate}）`;
});

const canSubmit = computed(() => {
  if (!selectedVenue.value) return false;
  if (!rangeOk.value) return false;
  if (!Number.isInteger(hours.value) || hours.value <= 0) return false;
  // 必须列表端判断可预约
  if (selectedVenue.value.available === false) return false;
  // 选中场地当天占用里也不能冲突
  const a = toDT(date.value, startTime.value);
  const b = toDT(date.value, endTime.value);
  return occupied.value.every(o => {
    const os = parseDT(o.startTime);
    const oe = parseDT(o.endTime);
    if (!os || !oe) return true;
    return oe <= a || os >= b;
  });
});

const fetchVenues = async () => {
  const sessionId = getSessionId();
  const qs = rangeOk.value
    ? `?startTime=${encodeURIComponent(`${date.value} ${startTime.value}`)}&endTime=${encodeURIComponent(`${date.value} ${endTime.value}`)}`
    : '';

  await new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/venues${qs}`,
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
          venues.value = Array.isArray(res.data.data) ? res.data.data : [];
        } else {
          venues.value = [];
        }
        // 同步 selectedVenue
        if (selectedVenue.value) {
          const hit = venues.value.find(x => x.id === selectedVenue.value.id);
          if (hit) selectedVenue.value = hit;
        }
        resolve();
      },
      fail: () => {
        venues.value = [];
        resolve();
      }
    });
  });
};

const fetchOccupiedDay = async () => {
  if (!selectedVenue.value || !date.value) {
    occupied.value = [];
    return;
  }
  const sessionId = getSessionId();
  const st = `${date.value} 00:00`;
  const et = `${date.value} 23:59`;

  await new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/venues/${selectedVenue.value.id}/occupied?startTime=${encodeURIComponent(st)}&endTime=${encodeURIComponent(et)}`,
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
          occupied.value = Array.isArray(res.data.data) ? res.data.data : [];
        } else {
          occupied.value = [];
        }
        resolve();
      },
      fail: () => {
        occupied.value = [];
        resolve();
      }
    });
  });
};

const refreshMine = async () => {
  const sessionId = getSessionId();
  await new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/venues/me/booking`,
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
          myVenue.value = data.venue || null;
        } else {
          myBooking.value = null;
          myVenue.value = null;
        }
        resolve();
      },
      fail: () => {
        myBooking.value = null;
        myVenue.value = null;
        resolve();
      }
    });
  });
};

const selectVenue = async (v) => {
  selectedVenue.value = v;
  await fetchOccupiedDay();
};

const canChooseVenue = (v) => {
  if (String(v.currentState || '').toUpperCase() === 'MAINTENANCE') return false;
  if (!rangeOk.value) return true;
  return v.available !== false;
};

const availabilityText = (v) => {
  if (!rangeOk.value) return '请先选择时间段';
  if (String(v.currentState || '').toUpperCase() === 'MAINTENANCE') return '维护中不可预约';
  return v.available ? '该时间段可预约' : '该时间段不可预约';
};

const stateText = (v) => {
  const st = String(v.currentState || '').toUpperCase();
  if (st === 'IN_USE') return '使用中';
  if (st === 'BOOKED') return '已预约';
  if (st === 'MAINTENANCE') return '维护中';
  return '空闲';
};

const stateBadgeClass = (v) => {
  const st = String(v.currentState || '').toUpperCase();
  if (st === 'IN_USE') return 'badge-inuse';
  if (st === 'BOOKED') return 'badge-booked';
  if (st === 'MAINTENANCE') return 'badge-maint';
  return 'badge-free';
};

const mineStatusText = computed(() => {
  if (!myBooking.value) return '—';
  const st = String(myBooking.value.status || '').toUpperCase();
  if (st === 'BOOKED') return '已预约';
  if (st === 'IN_USE') return '使用中';
  if (st === 'AUTO_CANCELLED') return '自动取消';
  if (st === 'CANCELLED') return '已取消';
  if (st === 'COMPLETED') return '已结束';
  return st;
});

const mineBadgeClass = computed(() => {
  if (!myBooking.value) return 'badge-booked';
  const st = String(myBooking.value.status || '').toUpperCase();
  if (st === 'IN_USE') return 'badge-inuse';
  if (st === 'BOOKED') return 'badge-booked';
  if (st === 'COMPLETED') return 'badge-free';
  return 'badge-maint';
});

const showConfirm = computed(() => {
  if (!myBooking.value) return false;
  const st = String(myBooking.value.status || '').toUpperCase();
  if (st !== 'BOOKED') return false;
  const now = Date.now();
  const start = parseDT(myBooking.value.startTime)?.getTime();
  if (!start) return false;
  return now >= start && now <= start + 10 * 60 * 1000;
});

const confirmUse = async () => {
  if (!myBooking.value) return;
  const sessionId = getSessionId();
  uni.showLoading({ title: '确认中...' });
  await new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/venues/bookings/${myBooking.value.id}/confirm`,
      method: 'POST',
      header: { 'X-Session-Id': sessionId },
      withCredentials: true,
      success: (res) => {
        uni.hideLoading();
        if (res.statusCode === 401) {
          handle401();
          resolve();
          return;
        }
        if (res.statusCode === 200 && res.data && res.data.success) {
          uni.showToast({ title: '已确认使用', icon: 'success' });
          refreshMine();
          fetchVenues();
        } else {
          uni.showToast({ title: (res.data && res.data.message) ? res.data.message : '确认失败', icon: 'none' });
        }
        resolve();
      },
      fail: () => {
        uni.hideLoading();
        uni.showToast({ title: '网络异常', icon: 'none' });
        resolve();
      }
    });
  });
};

const submitBooking = async () => {
  if (!canSubmit.value) return;
  if (!Number.isInteger(hours.value)) {
    uni.showToast({ title: '仅支持整小时预约', icon: 'none' });
    return;
  }

  uni.showModal({
    title: '确认预约',
    content: `场地：${selectedVenue.value.venueName}\n时间：${date.value} ${startTime.value}–${endTime.value}\n预扣：${costText.value}`,
    confirmText: '提交',
    cancelText: '取消',
    success: async (res) => {
      if (!res.confirm) return;

      const sessionId = getSessionId();
      uni.showLoading({ title: '提交中...' });

      await new Promise((resolve) => {
        uni.request({
          url: `${baseUrl}/api/venues/${selectedVenue.value.id}/bookings`,
          method: 'POST',
          header: {
            'Content-Type': 'application/json',
            'X-Session-Id': sessionId
          },
          withCredentials: true,
          data: {
            date: date.value,
            startTime: startTime.value,
            endTime: endTime.value
          },
          success: (r) => {
            uni.hideLoading();
            if (r.statusCode === 401) {
              handle401();
              resolve();
              return;
            }
            if (r.statusCode === 200 && r.data && r.data.success) {
              uni.showToast({ title: '预约成功', icon: 'success' });
              refreshMine();
              fetchVenues();
              fetchOccupiedDay();
              activeTab.value = 'mine';
            } else {
              uni.showToast({ title: (r.data && r.data.message) ? r.data.message : '预约失败', icon: 'none' });
            }
            resolve();
          },
          fail: () => {
            uni.hideLoading();
            uni.showToast({ title: '网络异常', icon: 'none' });
            resolve();
          }
        });
      });
    }
  });
};

const resetRange = async () => {
  date.value = '';
  startTime.value = '';
  endTime.value = '';
  selectedVenue.value = null;
  occupied.value = [];
  await fetchVenues();
};

const onDateChange = async (e) => {
  date.value = e.detail.value;
  await fetchVenues();
  await fetchOccupiedDay();
};

const onStartChange = async (e) => {
  const idx = Number(e.detail.value);
  startTime.value = hourOptions[idx] || '';

  // 若结束时间不合法（<=开始），清空让用户重选
  if (endTime.value) {
    const a = hourOptions.indexOf(startTime.value);
    const b = hourOptions.indexOf(endTime.value);
    if (a >= 0 && b >= 0 && b <= a) endTime.value = '';
  }
  await fetchVenues();
};

const onEndChange = async (e) => {
  const idx = Number(e.detail.value);
  endTime.value = endHourOptions.value[idx] || '';
  await fetchVenues();
};

onShow(async () => {
  const userInfo = uni.getStorageSync('userInfo');
  if (!userInfo) return;

  await fetchVenues();
  await refreshMine();

  if (timer.value) clearInterval(timer.value);
  timer.value = setInterval(() => {
    if (activeTab.value === 'list') {
      fetchVenues();
      if (selectedVenue.value && date.value) fetchOccupiedDay();
    } else {
      refreshMine();
      fetchVenues();
    }
  }, 8000);
});

onHide(() => {
  if (timer.value) {
    clearInterval(timer.value);
    timer.value = null;
  }
});

// helpers
function isOnTheHour(t) {
  return typeof t === 'string' && t.endsWith(':00');
}

function toDT(d, t) {
  if (!d || !t) return null;
  const x = new Date(`${d}T${t}:00`);
  return Number.isNaN(x.getTime()) ? null : x.getTime();
}

function parseDT(v) {
  if (!v) return null;
  const d = new Date(v);
  if (!Number.isNaN(d.getTime())) return d;
  // 兼容后端序列化为 yyyy-MM-ddTHH:mm:ss
  const s = String(v).replace(' ', 'T');
  const d2 = new Date(s);
  return Number.isNaN(d2.getTime()) ? null : d2;
}

function fmtDT(v) {
  const d = parseDT(v);
  if (!d) return String(v || '').slice(0, 16).replace('T', ' ');
  const hh = String(d.getHours()).padStart(2, '0');
  const mm = String(d.getMinutes()).padStart(2, '0');
  return `${hh}:${mm}`;
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
  transition: all 0.25s ease;
  font-weight: 700;
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
  padding: 28rpx 24rpx;
}

.filter-header {
  margin-bottom: 16rpx;
}

.filter-title {
  display: block;
  font-size: 30rpx;
  font-weight: 900;
  color: $text-main;
}

.filter-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: $text-light;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
}

.picker-item {
  padding: 16rpx 14rpx;
  border-radius: 18rpx;
  background: rgba($bg-color, 0.75);
}

.picker-label {
  display: block;
  font-size: 22rpx;
  font-weight: 800;
  color: $text-light;
}

.picker-value {
  margin-top: 10rpx;
  font-size: 24rpx;
  font-weight: 900;
  color: $text-main;
  padding: 12rpx 14rpx;
  border-radius: 16rpx;
  background: rgba($white, 0.75);
}

.filter-foot {
  margin-top: 16rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14rpx;
}

.filter-hint {
  flex: 1;
  min-width: 0;
  font-size: 22rpx;
  color: $text-light;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.list-card,
.booking-card,
.mine-card {
  width: 100%;
}

.list-card {
  padding: 28rpx 24rpx;
}

.list-header {
  margin-bottom: 16rpx;
}

.list-title {
  font-size: 30rpx;
  font-weight: 900;
  color: $text-main;
  display: block;
}

.list-sub {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: $text-light;
}

.venue-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.venue-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 18rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.6);
  border: 2rpx solid rgba($primary, 0.06);
  transition: transform 0.2s ease;
}

.venue-item.selected {
  border-color: rgba($primary, 0.22);
  box-shadow: $shadow-sm;
}

.venue-item:active {
  transform: scale(0.99);
}

.venue-left {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.name {
  flex: 1;
  min-width: 0;
  font-size: 32rpx;
  font-weight: 900;
  color: $text-main;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta {
  font-size: 22rpx;
  color: $text-light;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta.small {
  opacity: 0.9;
}

.badge {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  padding: 8rpx 14rpx;
  border-radius: $radius-full;
  font-size: 22rpx;
  font-weight: 800;
  white-space: nowrap;
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

.badge-booked {
  color: $text-light;
  background: rgba($text-light, 0.14);
}

.badge-inuse {
  color: $secondary;
  background: rgba($secondary, 0.14);
}

.badge-maint {
  color: $text-light;
  background: rgba($text-light, 0.16);
}

.primary-btn {
  flex: 0 0 190rpx;
  background: $primary-gradient;
  color: $white;
  border-radius: $radius-full;
  padding: 18rpx 0;
  font-size: 26rpx;
  font-weight: 900;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  box-shadow: 0 14rpx 30rpx rgba($primary, 0.24);
  transition: transform 0.18s ease, opacity 0.18s ease, filter 0.18s ease;
}

.primary-btn:active {
  transform: scale(0.98);
}

.primary-btn:disabled {
  filter: grayscale(0.35);
  opacity: 0.55;
  box-shadow: none;
}

.ghost-btn {
  width: 160rpx;
  background: rgba($white, 0.65);
  color: $text-main;
  border-radius: $radius-full;
  padding: 18rpx 0;
  font-size: 26rpx;
  font-weight: 900;
  border: 2rpx solid rgba($primary, 0.14);
  transition: transform 0.18s ease, opacity 0.18s ease;
}

.ghost-btn:active {
  transform: scale(0.98);
}

.btn-icon {
  font-size: 26rpx;
}

.btn-text {
  font-size: 24rpx;
}

.booking-card {
  padding: 28rpx 24rpx;
}

.booking-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.booking-title {
  display: flex;
  align-items: center;
  gap: 12rpx;
  font-size: 30rpx;
  font-weight: 900;
  color: $text-main;
}

.booking-icon {
  font-size: 30rpx;
}

.mini-pill {
  padding: 10rpx 14rpx;
  border-radius: $radius-full;
  background: rgba($bg-color, 0.8);
  color: $text-main;
  font-size: 22rpx;
  font-weight: 900;
  max-width: 240rpx;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.booking-info {
  padding: 16rpx;
  border-radius: $radius-lg;
  background: rgba($bg-color, 0.75);
}

.line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  padding: 10rpx 0;
}

.k {
  font-size: 22rpx;
  font-weight: 800;
  color: $text-light;
}

.v {
  font-size: 22rpx;
  font-weight: 900;
  color: $text-main;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.occupied {
  margin-top: 16rpx;
}

.occ-title {
  font-size: 24rpx;
  font-weight: 900;
  color: $text-main;
}

.occ-empty {
  margin-top: 10rpx;
  font-size: 22rpx;
  color: $text-light;
}

.occ-list {
  margin-top: 10rpx;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.occ-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14rpx 14rpx;
  border-radius: 18rpx;
  background: rgba($white, 0.55);
}

.occ-time {
  font-size: 22rpx;
  font-weight: 900;
  color: $text-main;
}

.occ-state {
  font-size: 22rpx;
  color: $text-light;
  font-weight: 800;
}

.actions {
  margin-top: 16rpx;
  display: flex;
  gap: 14rpx;
}

.mine-card {
  padding: 28rpx 24rpx;
}

.mine-title {
  font-size: 30rpx;
  font-weight: 900;
  color: $text-main;
}

.mine-body {
  padding-top: 12rpx;
}

.top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.mine-body .code {
  font-size: 32rpx;
  font-weight: 900;
  color: $text-main;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mine-body .line {
  font-size: 24rpx;
  color: $text-light;
  margin-top: 10rpx;
}

.confirm-box {
  margin-top: 16rpx;
  padding: 16rpx;
  border-radius: $radius-lg;
  background: rgba($primary, 0.08);
}

.confirm-hint {
  font-size: 22rpx;
  color: $text-light;
  margin-bottom: 12rpx;
}

.hint {
  margin-top: 14rpx;
  font-size: 22rpx;
  color: $text-light;
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
</style>
