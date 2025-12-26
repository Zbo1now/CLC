<template>
  <view class="container">
    <view class="shell">
      <view class="header panel">
        <view class="back-btn" @tap="goBack">←</view>
        <text class="title">培训活动</text>
      </view>

      <view class="tabs glass-card panel">
        <view :class="['tab', activeTab === 'events' ? 'active' : '']" @tap="switchTab('events')">
          <text class="tab-icon">🎯</text>
          <text>活动列表</text>
        </view>
        <view :class="['tab', activeTab === 'mine' ? 'active' : '']" @tap="switchTab('mine')">
          <text class="tab-icon">🗂️</text>
          <text>我的申报</text>
        </view>
      </view>

      <view class="content panel">
        <view v-if="loading" class="glass-card empty-card">
          <text class="empty-text">加载中…</text>
        </view>

        <view v-else class="glass-card list-card">
          <view class="list-header">
            <text class="list-title">{{ activeTab === 'mine' ? '我的申报记录' : '已发布活动' }}</text>
            <text class="more-link" @tap="refresh">刷新</text>
          </view>

          <view v-if="shownList.length === 0" class="empty">
            <text class="empty-icon">📭</text>
            <text class="empty-text">{{ activeTab === 'mine' ? '暂无申报记录' : '暂无活动' }}</text>
            <text class="empty-sub">{{ activeTab === 'mine' ? '去“活动列表”查看并在活动结束后提交证明' : '等待管理员发布活动后会出现在这里' }}</text>
            <button v-if="activeTab === 'mine'" class="primary-btn" @tap="switchTab('events')">
              <text class="btn-icon">🎯</text>
              <text class="btn-text">去活动列表</text>
            </button>
          </view>

          <view v-else class="items">
            <view class="item" v-for="it in shownList" :key="itKey(it)" @tap="openDetail(it)">
              <view class="item-top">
                <view class="item-left">
                  <text class="item-icon">{{ itemIcon(it) }}</text>
                  <view class="item-main">
                    <text class="item-title">{{ itemTitle(it) }}</text>
                    <view class="item-meta">
                      <text v-if="rangeText(it)" class="meta-line">🕒 {{ rangeText(it) }}</text>
                      <text v-if="locationText(it)" class="meta-line">📍 {{ locationText(it) }}</text>
                      <text class="meta-line">💰 +{{ rewardText(it) }} 币</text>
                    </view>
                  </view>
                </view>

                <view :class="['badge', badgeClass(it)]">
                  <text class="badge-dot"></text>
                  <text>{{ badgeText(it) }}</text>
                </view>
              </view>

              <view class="item-bottom">
                <text :class="bottomClass(it)">{{ bottomText(it) }}</text>
              </view>
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

const activeTab = ref('events');
const loading = ref(false);
const events = ref([]);
const myParticipations = ref([]);

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

const fmt = (ts) => {
  if (!ts) return '';
  const d = new Date(ts);
  if (Number.isNaN(d.getTime())) return String(ts).slice(0, 16).replace('T', ' ');
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

const fmtMDHM = (v) => {
  if (!v) return '';
  const d = new Date(v);
  if (Number.isNaN(d.getTime())) {
    return String(v).slice(0, 16).replace('T', ' ');
  }
  const now = new Date();
  const sameYear = d.getFullYear() === now.getFullYear();
  const pad = (n) => String(n).padStart(2, '0');
  const mm = pad(d.getMonth() + 1);
  const dd = pad(d.getDate());
  const hh = pad(d.getHours());
  const mi = pad(d.getMinutes());
  return sameYear ? `${mm}-${dd} ${hh}:${mi}` : `${d.getFullYear()}-${mm}-${dd} ${hh}:${mi}`;
};

const fmtHM = (v) => {
  if (!v) return '';
  const d = new Date(v);
  if (Number.isNaN(d.getTime())) return '';
  const pad = (n) => String(n).padStart(2, '0');
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

const fetchEvents = async () => {
  const sessionId = getSessionId();
  await new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/training-events`,
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
          events.value = Array.isArray(data.events) ? data.events : [];
        }
        resolve();
      },
      fail: () => resolve()
    });
  });
};

const fetchMine = async () => {
  const sessionId = getSessionId();
  await new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/training-events/me/participations`,
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
          myParticipations.value = Array.isArray(res.data.data) ? res.data.data : [];
        }
        resolve();
      },
      fail: () => resolve()
    });
  });
};

const refresh = async () => {
  if (loading.value) return;
  loading.value = true;
  await Promise.all([fetchEvents(), fetchMine()]);
  loading.value = false;
};

onShow(async () => {
  await refresh();
});

const switchTab = (t) => {
  activeTab.value = t;
};

const shownList = computed(() => {
  if (activeTab.value === 'mine') return myParticipations.value;
  // 活动列表排序：未开始、进行中在前，已结束在后
  return events.value.slice().sort((a, b) => {
    const statusOrder = (st) => {
      const s = String(st || '').toUpperCase();
      if (s === 'NOT_STARTED') return 0;
      if (s === 'IN_PROGRESS') return 1;
      if (s === 'ENDED') return 2;
      return 99;
    };
    const ao = statusOrder(a.eventStatus);
    const bo = statusOrder(b.eventStatus);
    if (ao !== bo) return ao - bo;
    // 同状态下按开始时间降序
    const at = new Date(a.startTime).getTime() || 0;
    const bt = new Date(b.startTime).getTime() || 0;
    return bt - at;
  });
});

const itKey = (it) => {
  return String(activeTab.value === 'mine' ? it.id : it.id);
};

const locationText = (it) => {
  const mode = String(it.locationMode || '').toUpperCase();
  const place = it.locationDetail || '';
  if (mode === 'ONLINE') return place ? `线上 · ${place}` : '线上';
  return place ? place : '线下';
};

const rewardText = (it) => {
  const v = activeTab.value === 'mine' ? (it.eventRewardCoins ?? it.rewardCoins ?? 0) : (it.rewardCoins ?? 0);
  return Number(v || 0);
};

const itemIcon = (it) => {
  if (activeTab.value === 'mine') return '🗂️';
  const st = String(it.eventStatus || '').toUpperCase();
  if (st === 'NOT_STARTED') return '⏳';
  if (st === 'ENDED') return '🏁';
  return '🎯';
};

const itemTitle = (it) => {
  return it?.eventName || '培训活动';
};

const typeText = (it) => {
  return it?.eventType || '培训/会议';
};

const rangeText = (it) => {
  const s = it?.startTime;
  const e = it?.endTime;
  if (!s && !e) return '';

  const sd = new Date(s);
  const ed = new Date(e);
  const validS = s && !Number.isNaN(sd.getTime());
  const validE = e && !Number.isNaN(ed.getTime());

  if (validS && validE) {
    const sameDay = sd.getFullYear() === ed.getFullYear() && sd.getMonth() === ed.getMonth() && sd.getDate() === ed.getDate();
    if (sameDay) {
      return `${fmtMDHM(s)} - ${fmtHM(e)}`;
    }
    return `${fmtMDHM(s)} - ${fmtMDHM(e)}`;
  }

  return fmtMDHM(s || e);
};

const badgeText = (it) => {
  if (activeTab.value === 'mine') {
    // 我的申报：用申报状态
    const s = String(it.status || '').toUpperCase();
    if (s === 'APPROVED') return '已通过';
    if (s === 'REJECTED') return '已驳回';
    return '待审核';
  }
  const st = String(it.eventStatus || '').toUpperCase();
  if (st === 'NOT_STARTED') return '未开始';
  if (st === 'ENDED') return '已结束';
  return '进行中';
};

const badgeClass = (it) => {
  if (activeTab.value === 'mine') {
    const s = String(it.status || '').toUpperCase();
    if (s === 'APPROVED') return 'badge-approved';
    if (s === 'REJECTED') return 'badge-rejected';
    return 'badge-pending';
  }
  const st = String(it.eventStatus || '').toUpperCase();
  if (st === 'ENDED') return 'badge-ended';
  if (st === 'NOT_STARTED') return 'badge-not-started';
  return 'badge-in-progress';
};

const canSubmit = (it) => {
  if (activeTab.value === 'mine') return false;
  const st = String(it.eventStatus || '').toUpperCase();
  if (st !== 'ENDED') return false;
  return !it?.myProofSubmitted;
};

const bottomText = (it) => {
  if (activeTab.value === 'mine') {
    const s = String(it.status || '').toUpperCase();
    if (s === 'APPROVED') return `🎉 +${Number(it.rewardCoins || 0)} 币`;
    if (s === 'REJECTED') return it.rejectReason ? `❗ ${it.rejectReason}` : '❗ 已驳回';
    return '⏳ 等待管理员审核';
  }

  const st = String(it.eventStatus || '').toUpperCase();
  const mine = String(it.myParticipationStatus || '').toUpperCase();
  // 活动未结束且团队已报名，显示“已报名”
  if ((st === 'NOT_STARTED' || st === 'IN_PROGRESS') && mine) return '已报名';
  if (!mine && (st === 'IN_PROGRESS' || st === 'ENDED')) return '未报名';
  if (mine === 'APPROVED') return '🎉 已通过（已发币）';
  if (mine === 'REJECTED') return '❗ 已驳回';
  if (mine === 'PENDING') return '⏳ 已提交，待审核';
  if (canSubmit(it)) return '可提交证明';
  return '查看详情';
};

const bottomClass = (it) => {
  const t = bottomText(it);
  if (t.includes('🎉')) return 'reward';
  if (t.includes('❗')) return 'reject';
  return 'pending';
};

const openDetail = (it) => {
  const eventId = activeTab.value === 'mine' ? it.eventId : it.id;
  uni.navigateTo({ url: `/pages/training/detail?eventId=${eventId}` });
};

const goBack = () => {
  uni.navigateBack();
};

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
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  padding: 16rpx 0;
  font-size: 26rpx;
  color: $text-light;
  background: rgba($bg-color, 0.8);
  transition: all 0.25s ease;
  font-weight: 700;
}

.tab-icon {
  font-size: 30rpx;
}

.tab.active {
  color: $primary;
  background: rgba($white, 0.95);
  box-shadow: $shadow-sm;
}

.content {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18rpx;
}

.list-card {
  width: 100%;
  padding: 44rpx 34rpx;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6rpx 6rpx 18rpx;
}

.list-title {
  font-size: 32rpx;
  font-weight: 900;
  color: $text-main;
}

.more-link {
  font-size: 26rpx;
  color: $primary;
  font-weight: 800;
}

.empty-card {
  padding: 56rpx 18rpx;
  text-align: center;
}


.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  padding: 28rpx 0;
}

.empty-icon {
  font-size: 46rpx;
  display: block;
  margin-bottom: 10rpx;
}

.empty-text {
  font-size: 28rpx;
  font-weight: 800;
  color: $text-main;
}

.empty-sub {
  margin-top: 8rpx;
  display: block;
  font-size: 24rpx;
  color: $text-light;
}


.items {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.item {
  border-radius: 18rpx;
  padding: 22rpx;
  background: rgba($white, 0.85);
  box-shadow: $shadow-sm;
}

.item-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.item-left {
  display: flex;
  gap: 16rpx;
  align-items: flex-start;
  flex: 1;
}

.item-icon {
  font-size: 44rpx;
}

.item-main {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  flex: 1;
}

.item-title {
  font-size: 30rpx;
  color: $text-main;
  font-weight: 900;
  line-height: 1.2;
}

.item-meta {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.meta-line {
  font-size: 24rpx;
  color: $text-light;
  line-height: 1.35;
}

.badge {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 12rpx 16rpx;
  border-radius: $radius-full;
  font-size: 24rpx;
  font-weight: 900;
  white-space: nowrap;
}

.badge-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: currentColor;
}

.badge-pending {
  color: $text-light;
  background: rgba($text-light, 0.12);
}

.badge-approved {
  color: $primary;
  background: rgba($primary, 0.14);
}

.badge-rejected {
  color: $secondary;
  background: rgba($secondary, 0.14);
}

.badge-not-started {
  color: $text-light;
  background: rgba($text-light, 0.12);
}

.badge-in-progress {
  color: $primary;
  background: rgba($primary, 0.14);
}

.badge-ended {
  color: $secondary;
  background: rgba($secondary, 0.14);
}

.item-bottom {
  margin-top: 14rpx;
}

.reward {
  font-size: 26rpx;
  color: $primary;
  font-weight: 900;
}

.reject {
  font-size: 26rpx;
  color: $secondary;
  font-weight: 900;
}

.pending {
  font-size: 26rpx;
  color: $text-light;
  font-weight: 800;
}

.primary-btn {
  width: 100%;
  margin-top: 10rpx;
  border-radius: $radius-full;
  background: $primary-gradient;
  color: $white;
  font-size: 32rpx;
  font-weight: 800;
  padding: 22rpx 0;
  display: flex;
  justify-content: center;
  gap: 10rpx;
  box-shadow: $shadow-lg;
  transform: translateY(0);
  transition: all 0.2s ease;
}

.primary-btn[disabled] {
  opacity: 0.6;
}

.btn-icon {
  font-size: 32rpx;
}

</style>
