<template>
  <view class="container">
    <view class="shell">
      <view class="header panel">
        <view class="back-btn" @tap="goBack">←</view>
        <text class="title">器材借用</text>
      </view>

      <view class="tabs glass-card panel">
        <view :class="['tab', activeTab === 'list' ? 'active' : '']" @tap="switchTab('list')">
          <text class="tab-icon">📷</text>
          <text>器材列表</text>
        </view>
        <view :class="['tab', activeTab === 'mine' ? 'active' : '']" @tap="switchTab('mine')">
          <text class="tab-icon">🧾</text>
          <text>我的借用</text>
        </view>
      </view>

      <!-- 器材列表 -->
      <view v-if="activeTab === 'list'" class="content panel">
        <view class="glass-card list-card">
          <view class="list-header">
            <text class="list-title">可借用器材</text>
            <text class="list-sub">选择器材后在下方日历挑选日期</text>
          </view>

          <view v-if="equipments.length === 0" class="empty">
            <text class="empty-icon">📦</text>
            <text class="empty-text">暂无器材数据</text>
            <text class="empty-sub">请先执行 db/equipment_schema.sql 初始化</text>
          </view>

          <view v-else class="equipment-list">
            <view class="equipment-item" v-for="e in equipments" :key="e.id" :class="selectedEquipment?.id === e.id ? 'selected' : ''">
              <view class="equipment-left">
                <view class="code-row">
                  <text class="code">{{ e.equipmentName }}</text>
                  <view :class="['badge', e.status && String(e.status).toUpperCase() === 'MAINTENANCE' ? 'badge-rented' : (e.available ? 'badge-free' : 'badge-rented')]">
                    <text class="badge-dot"></text>
                    <text>{{ statusText(e) }}</text>
                  </view>
                </view>
                <text class="meta">🏷️ {{ e.equipmentType }}</text>
                <text class="meta">💳 {{ e.ratePerDay }} 币 / 天</text>
              </view>

              <button class="primary-btn" :disabled="e.status && String(e.status).toUpperCase() === 'MAINTENANCE'" @tap="selectEquipment(e)">
                <text class="btn-icon">🗓️</text>
                <text class="btn-text">选择日期</text>
              </button>
            </view>
          </view>
        </view>

        <!-- 借用日历（点击器材后出现） -->
        <view v-if="selectedEquipment" class="glass-card calendar-card">
          <view class="calendar-header">
            <view class="calendar-title">
              <text class="calendar-icon">🗓️</text>
              <text>借用日历</text>
            </view>
            <view class="month-nav">
              <view class="nav-btn" @tap="prevMonth">‹</view>
              <text class="month-text">{{ monthLabel }}</text>
              <view class="nav-btn" @tap="nextMonth">›</view>
            </view>
          </view>

          <view class="legend">
            <view class="legend-item"><view class="dot ok"></view><text>可借</text></view>
            <view class="legend-item"><view class="dot busy"></view><text>不可借</text></view>
            <view class="legend-item"><view class="dot sel"></view><text>已选</text></view>
          </view>

          <view class="week">
            <text class="wk">一</text><text class="wk">二</text><text class="wk">三</text><text class="wk">四</text><text class="wk">五</text><text class="wk">六</text><text class="wk">日</text>
          </view>

          <view class="grid">
            <view v-for="(d, idx) in calendarCells" :key="idx" class="cell" :class="cellClass(d)" @tap="tapDay(d)">
              <text class="day-text">{{ d.day > 0 ? d.day : '' }}</text>
            </view>
          </view>

          <view class="range-info">
            <view class="range-line">
              <text class="range-label">器材</text>
              <text class="range-value">{{ selectedEquipment.equipmentName }}</text>
            </view>
            <view class="range-line">
              <text class="range-label">数量</text>
              <picker mode="selector" :range="quantityOptions" :value="quantityIndex" @change="onQtyChange">
                <view class="range-value">{{ borrowQuantity }} 件</view>
              </picker>
            </view>
            <view class="range-line">
              <text class="range-label">日期</text>
              <text class="range-value">{{ startDate || '—' }} ～ {{ endDate || '—' }}</text>
            </view>
            <view class="range-line">
              <text class="range-label">预扣</text>
              <text class="range-value">{{ costText }}</text>
            </view>
          </view>

          <view class="actions">
            <button class="ghost-btn" @tap="resetRange">
              <text class="btn-text">重置</text>
            </button>
            <button class="primary-btn" :disabled="!canSubmit" @tap="submitLoan">
              <text class="btn-icon">📨</text>
              <text class="btn-text">提交借用申请</text>
            </button>
          </view>

          <view class="hint">* 灰色日期表示该日库存已满（申请中/已审核/已借出），提交后将预扣虚拟币</view>
        </view>
      </view>

      <!-- 我的借用 -->
      <view v-else class="content panel">
        <view class="glass-card lease-card">
          <view class="lease-header">
            <text class="lease-title">当前借用</text>
          </view>

          <view v-if="!myLoan" class="empty">
            <text class="empty-icon">🧾</text>
            <text class="empty-text">暂无借用</text>
            <text class="empty-sub">去“器材列表”选择器材并提交借用申请</text>
            <button class="primary-btn" @tap="switchTab('list')">
              <text class="btn-icon">📷</text>
              <text class="btn-text">去借用</text>
            </button>
          </view>

          <view v-else class="lease-body">
            <view class="lease-top">
              <text class="lease-code">{{ myEquipment?.equipmentName || '—' }}</text>
              <view :class="['badge', loanBadgeClass(myLoan)]">
                <text class="badge-dot"></text>
                <text>{{ loanStatusText(myLoan) }}</text>
              </view>
            </view>

            <view class="info-list">
              <view class="info-item">
                <text class="info-label">类型</text>
                <text class="info-value">{{ myEquipment?.equipmentType || '—' }}</text>
              </view>
              <view class="info-item">
                <text class="info-label">日期</text>
                <text class="info-value">{{ myLoan.startDate }} ～ {{ myLoan.endDate }}</text>
              </view>
              <view class="info-item">
                <text class="info-label">数量</text>
                <text class="info-value">{{ myLoan.quantity || 1 }} 件</text>
              </view>
              <view class="info-item">
                <text class="info-label">预扣</text>
                <text class="info-value">{{ myLoan.heldCost || 0 }} 币</text>
              </view>
              <view v-if="myLoan.borrowedAt" class="info-item">
                <text class="info-label">借出</text>
                <text class="info-value">{{ fmtTime(myLoan.borrowedAt) }}</text>
              </view>
              <view v-if="myLoan.returnedAt" class="info-item">
                <text class="info-label">归还</text>
                <text class="info-value">{{ fmtTime(myLoan.returnedAt) }}</text>
              </view>
            </view>

            <view class="actions" v-if="canCancelMyLoan">
              <button class="ghost-btn" @tap="cancelMyLoan">
                <text class="btn-text">取消申请</text>
              </button>
            </view>

            <view class="hint">
              * 申请提交后等待管理员线下发放器材
            </view>
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
const equipments = ref([]);

const selectedEquipment = ref(null);

const startDate = ref('');
const endDate = ref('');

const monthCursor = ref(new Date());
// dateStr -> 已占用数量（按天汇总）
const occupiedMap = ref(new Map());

const myLoan = ref(null);
const myEquipment = ref(null);

const borrowQuantity = ref(1);
const quantityIndex = ref(0);
const quantityOptions = computed(() => {
  const total = Math.max(1, Number(selectedEquipment.value?.quantity || 1));
  return Array.from({ length: total }, (_, i) => String(i + 1));
});

const onQtyChange = (e) => {
  const idx = Number(e?.detail?.value || 0);
  quantityIndex.value = idx;
  borrowQuantity.value = Math.max(1, idx + 1);
};

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

const switchTab = async (tab) => {
  activeTab.value = tab;
  if (tab === 'mine') {
    await refreshMine();
  }
};

const monthLabel = computed(() => {
  const d = monthCursor.value;
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  return `${y}-${m}`;
});

const costText = computed(() => {
  if (!selectedEquipment.value || !startDate.value || !endDate.value) return '—';
  const days = daysInclusive(startDate.value, endDate.value);
  const qty = Math.max(1, Number(borrowQuantity.value || 1));
  const cost = Math.max(0, Number(selectedEquipment.value.ratePerDay || 0)) * days * qty;
  return `${cost} 币（${qty} 件，${days} 天）`;
});

const canSubmit = computed(() => {
  if (!selectedEquipment.value) return false;
  if (!startDate.value || !endDate.value) return false;
  if (daysInclusive(startDate.value, endDate.value) <= 0) return false;
  // 校验：选中范围内每天占用+申请数量 <= 总库存
  const total = Math.max(1, Number(selectedEquipment.value.quantity || 1));
  const qty = Math.max(1, Number(borrowQuantity.value || 1));
  const map = occupiedMap.value;
  const days = listDays(startDate.value, endDate.value);
  return days.every(d => (Number(map.get(d) || 0) + qty) <= total);
});

const statusText = (e) => {
  if (e.status && String(e.status).toUpperCase() === 'MAINTENANCE') return '维护中';
  return e.available ? '可借' : '不可借';
};

const fmtTime = (v) => {
  if (!v) return '';
  const d = new Date(v);
  if (Number.isNaN(d.getTime())) return String(v).replace('T', ' ').slice(0, 16);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hh = String(d.getHours()).padStart(2, '0');
  const mm = String(d.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${day} ${hh}:${mm}`;
};

const loanStatusText = (l) => {
  const st = String(l.status || '').toUpperCase();
  if (st === 'PENDING') return '申请中';
  if (st === 'APPROVED') return '已审核';
  if (st === 'BORROWED') return '已借出';
  if (st === 'RETURNED') return '已归还';
  if (st === 'CANCELLED') return '已取消';
  if (st === 'REJECTED') return '已拒绝';
  if (st === 'EXPIRED') return '已过期';
  return st || '—';
};

const loanBadgeClass = (l) => {
  const st = String(l.status || '').toUpperCase();
  if (st === 'BORROWED') return 'badge-free';
  if (st === 'APPROVED') return 'badge-free';
  if (st === 'PENDING') return 'badge-rented';
  if (st === 'RETURNED') return 'badge-expired';
  if (st === 'CANCELLED' || st === 'REJECTED' || st === 'EXPIRED') return 'badge-expired';
  return 'badge-rented';
};

const fetchEquipments = async () => {
  const sessionId = getSessionId();
  const qs = startDate.value && endDate.value
    ? `?startDate=${encodeURIComponent(startDate.value)}&endDate=${encodeURIComponent(endDate.value)}`
    : '';

  await new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/equipments${qs}`,
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
          equipments.value = Array.isArray(res.data.data) ? res.data.data : [];
        } else {
          equipments.value = [];
        }
        resolve();
      },
      fail: () => {
        equipments.value = [];
        resolve();
      }
    });
  });
};

const refreshMine = async () => {
  const sessionId = getSessionId();
  await new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/equipments/me/loan`,
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
          myLoan.value = data.loan || null;
          myEquipment.value = data.equipment || null;
        } else {
          myLoan.value = null;
          myEquipment.value = null;
        }
        resolve();
      },
      fail: () => {
        myLoan.value = null;
        myEquipment.value = null;
        resolve();
      }
    });
  });
};

const selectEquipment = async (e) => {
  selectedEquipment.value = e;
  resetRange();
  borrowQuantity.value = 1;
  quantityIndex.value = 0;
  await fetchOccupiedForMonth();
};

const fetchOccupiedForMonth = async () => {
  if (!selectedEquipment.value) return;

  const start = monthStartStr(monthCursor.value);
  const end = monthEndStr(monthCursor.value);

  const sessionId = getSessionId();
  occupiedMap.value = new Map();

  await new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/equipments/${selectedEquipment.value.id}/occupied?startDate=${start}&endDate=${end}`,
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
          const list = Array.isArray(res.data.data) ? res.data.data : [];
          const map = new Map();
          list.forEach((x) => {
            const s = x.startDate;
            const e = x.endDate;
            const q = Math.max(1, Number(x.quantity || 1));
            listDays(s, e).forEach(d => map.set(d, Number(map.get(d) || 0) + q));
          });
          occupiedMap.value = map;
        }
        resolve();
      },
      fail: () => resolve()
    });
  });
};

const prevMonth = async () => {
  const d = new Date(monthCursor.value);
  d.setMonth(d.getMonth() - 1);
  monthCursor.value = d;
  await fetchOccupiedForMonth();
};

const nextMonth = async () => {
  const d = new Date(monthCursor.value);
  d.setMonth(d.getMonth() + 1);
  monthCursor.value = d;
  await fetchOccupiedForMonth();
};

const calendarCells = computed(() => buildCalendarCells(monthCursor.value));

const cellClass = (cell) => {
  if (!cell || cell.day <= 0) return 'empty-cell';
  const dateStr = cell.dateStr;
  const total = Math.max(1, Number(selectedEquipment.value?.quantity || 1));
  const busy = Number(occupiedMap.value.get(dateStr) || 0) >= total;
  const inSel = isInRange(dateStr, startDate.value, endDate.value);

  if (busy) return 'busy';
  if (inSel) return 'selected';
  return 'ok';
};

const tapDay = (cell) => {
  if (!cell || cell.day <= 0) return;
  const dateStr = cell.dateStr;
  const total = Math.max(1, Number(selectedEquipment.value?.quantity || 1));
  if (Number(occupiedMap.value.get(dateStr) || 0) >= total) return;

  if (!startDate.value || (startDate.value && endDate.value)) {
    startDate.value = dateStr;
    endDate.value = '';
    return;
  }

  // 已有 start，选择 end
  if (dateStr < startDate.value) {
    endDate.value = startDate.value;
    startDate.value = dateStr;
  } else {
    endDate.value = dateStr;
  }
};

const resetRange = () => {
  startDate.value = '';
  endDate.value = '';
};

const submitLoan = async () => {
  if (!canSubmit.value) return;

  const payload = {
    startDate: startDate.value,
    endDate: endDate.value,
    quantity: Math.max(1, Number(borrowQuantity.value || 1))
  };

  uni.showModal({
    title: '确认借用',
    content: `器材：${selectedEquipment.value.equipmentName}\n数量：${payload.quantity} 件\n日期：${startDate.value} ~ ${endDate.value}\n预扣：${costText.value}`,
    confirmText: '提交',
    cancelText: '取消',
    success: async (res) => {
      if (!res.confirm) return;

      const sessionId = getSessionId();
      uni.showLoading({ title: '提交中...' });

      await new Promise((resolve) => {
        uni.request({
          url: `${baseUrl}/api/equipments/${selectedEquipment.value.id}/loans`,
          method: 'POST',
          header: {
            'Content-Type': 'application/json',
            'X-Session-Id': sessionId
          },
          withCredentials: true,
          data: payload,
          success: (r) => {
            uni.hideLoading();
            if (r.statusCode === 401) {
              handle401();
              resolve();
              return;
            }
            if (r.statusCode === 200 && r.data && r.data.success) {
              uni.showToast({ title: '申请成功', icon: 'success' });
              // 申请成功后刷新占用与“我的借用”
              fetchOccupiedForMonth();
              refreshMine();
              activeTab.value = 'mine';
            } else {
              uni.showToast({ title: (r.data && r.data.message) ? r.data.message : '申请失败', icon: 'none' });
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

const canCancelMyLoan = computed(() => {
  if (!myLoan.value) return false;
  const st = String(myLoan.value.status || '').toUpperCase();
  if (!(st === 'PENDING' || st === 'APPROVED')) return false;
  const today = toYMD(new Date());
  return String(myLoan.value.startDate || '') > today;
});

const cancelMyLoan = () => {
  if (!myLoan.value) return;
  const sessionId = getSessionId();
  uni.showModal({
    title: '取消申请',
    content: '未到预约日期可取消并自动退款，确认取消？',
    confirmText: '取消申请',
    success: (r) => {
      if (!r.confirm) return;
      uni.showLoading({ title: '取消中...' });
      uni.request({
        url: `${baseUrl}/api/equipments/loans/${myLoan.value.id}/cancel`,
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
            uni.showToast({ title: '已取消', icon: 'success' });
            await refreshMine();
            await fetchEquipments();
          } else {
            uni.showToast({ title: (res.data && res.data.message) || '取消失败', icon: 'none' });
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

onShow(async () => {
  const userInfo = uni.getStorageSync('userInfo');
  if (!userInfo) return;

  await fetchEquipments();
  await refreshMine();
});

// utils
function monthStartStr(date) {
  const d = new Date(date.getFullYear(), date.getMonth(), 1);
  return toYMD(d);
}

function monthEndStr(date) {
  const d = new Date(date.getFullYear(), date.getMonth() + 1, 0);
  return toYMD(d);
}

function toYMD(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function buildCalendarCells(date) {
  const y = date.getFullYear();
  const m = date.getMonth();
  const first = new Date(y, m, 1);
  const daysInMonth = new Date(y, m + 1, 0).getDate();
  // Monday=1..Sunday=7
  const jsDay = first.getDay(); // 0 Sunday
  const weekDay = jsDay === 0 ? 7 : jsDay;
  const leading = weekDay - 1;

  const cells = [];
  for (let i = 0; i < leading; i++) {
    cells.push({ day: 0, dateStr: '' });
  }
  for (let d = 1; d <= daysInMonth; d++) {
    const dt = new Date(y, m, d);
    cells.push({ day: d, dateStr: toYMD(dt) });
  }
  while (cells.length % 7 !== 0) {
    cells.push({ day: 0, dateStr: '' });
  }
  return cells;
}

function isInRange(dateStr, s, e) {
  if (!dateStr || !s) return false;
  if (!e) return dateStr === s;
  return dateStr >= s && dateStr <= e;
}

function daysInclusive(s, e) {
  if (!s || !e) return 0;
  const a = new Date(s + 'T00:00:00');
  const b = new Date(e + 'T00:00:00');
  const diff = Math.floor((b.getTime() - a.getTime()) / 86400000);
  return diff + 1;
}

function listDays(s, e) {
  if (!s || !e) return [];
  const start = new Date(s + 'T00:00:00');
  const end = new Date(e + 'T00:00:00');
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return [];
  if (end.getTime() < start.getTime()) return [];

  const out = [];
  const cur = new Date(start);
  while (cur.getTime() <= end.getTime()) {
    out.push(toYMD(cur));
    cur.setDate(cur.getDate() + 1);
  }
  return out;
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

.list-card,
.calendar-card,
.lease-card {
  width: 100%;
}

.list-card {
  padding: 34rpx 28rpx;
}

.list-header {
  margin-bottom: 18rpx;
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

.equipment-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.equipment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 20rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.6);
  border: 2rpx solid rgba($primary, 0.06);
  transition: transform 0.2s ease;
}

.equipment-item.selected {
  border-color: rgba($primary, 0.22);
  box-shadow: $shadow-sm;
}

.equipment-item:active {
  transform: scale(0.99);
}

.equipment-left {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.code-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
  flex-wrap: nowrap;
}

.code {
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

/* 列表项内的“选择日期”按钮做紧凑化，给左侧文字留空间 */
.equipment-item .primary-btn {
  flex: 0 0 228rpx;
  padding: 18rpx 0;
  font-size: 26rpx;
}

.equipment-item .btn-icon {
  font-size: 26rpx;
}

.equipment-item .btn-text {
  font-size: 24rpx;
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

.calendar-card {
  padding: 34rpx 28rpx;
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.calendar-title {
  display: flex;
  align-items: center;
  gap: 12rpx;
  font-size: 32rpx;
  font-weight: 900;
  color: $text-main;
}

.calendar-icon {
  font-size: 32rpx;
}

.month-nav {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.nav-btn {
  width: 56rpx;
  height: 56rpx;
  border-radius: $radius-full;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba($bg-color, 0.9);
  box-shadow: $shadow-sm;
  font-size: 34rpx;
  color: $text-main;
}

.month-text {
  font-size: 26rpx;
  font-weight: 900;
  color: $text-main;
}

.legend {
  display: flex;
  gap: 16rpx;
  margin-bottom: 14rpx;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 10rpx;
  font-size: 22rpx;
  color: $text-light;
  font-weight: 700;
}

.dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: $radius-full;
}

.dot.ok {
  background: rgba($primary, 0.16);
}

.dot.busy {
  background: rgba($text-light, 0.22);
}

.dot.sel {
  background: rgba($primary, 0.95);
}

.week {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 10rpx;
  margin-bottom: 10rpx;
}

.wk {
  text-align: center;
  font-size: 22rpx;
  color: $text-light;
  font-weight: 800;
}

.grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 10rpx;
}

.cell {
  height: 72rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba($bg-color, 0.85);
  transition: transform 0.15s ease;
}

.cell:active {
  transform: scale(0.98);
}

.empty-cell {
  background: transparent;
}

.cell.ok {
  background: rgba($primary, 0.10);
}

.cell.busy {
  background: rgba($text-light, 0.16);
  opacity: 0.8;
}

.cell.selected {
  background: $primary-gradient;
}

.day-text {
  font-size: 26rpx;
  font-weight: 900;
  color: $text-main;
}

.cell.selected .day-text {
  color: $white;
}

.cell.busy .day-text {
  color: $text-light;
}

.range-info {
  margin-top: 18rpx;
  padding: 18rpx 18rpx;
  border-radius: $radius-lg;
  background: rgba($bg-color, 0.8);
}

.range-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10rpx 0;
}

.range-label {
  font-size: 24rpx;
  color: $text-light;
  font-weight: 800;
}

.range-value {
  font-size: 24rpx;
  color: $text-main;
  font-weight: 900;
}

.actions {
  display: flex;
  gap: 14rpx;
  margin-top: 16rpx;
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
  width: 200rpx;
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

.btn-icon {
  font-size: 28rpx;
}

.btn-text {
  font-size: 26rpx;
}

.lease-card {
  padding: 34rpx 28rpx;
}

.lease-header {
  margin-bottom: 18rpx;
}

.lease-title {
  font-size: 32rpx;
  font-weight: 900;
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
}

.lease-line {
  font-size: 26rpx;
  color: $text-light;
  margin-top: 10rpx;
}

.info-list {
  margin-top: 10rpx;
  padding: 16rpx 18rpx;
  border-radius: $radius-lg;
  background: rgba($bg-color, 0.8);
}

.info-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
  padding: 10rpx 0;
}

.info-label {
  flex: 0 0 96rpx;
  font-size: 24rpx;
  color: $text-light;
  font-weight: 800;
}

.info-value {
  flex: 1;
  text-align: right;
  font-size: 24rpx;
  color: $text-main;
  font-weight: 800;
  word-break: break-all;
}

.hint {
  margin-top: 12rpx;
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
