<template>
  <view class="container">
    <view class="shell">
      <view class="header panel">
        <view class="back-btn" @tap="goBack">←</view>
        <view class="header-mid">
          <text class="title">值班任务</text>
        </view>
      </view>

      <view class="tabs glass-card panel">
        <view :class="['tab', activeTab === 'available' ? 'active' : '']" @tap="activeTab = 'available'">
          <text class="tab-icon">🗓️</text>
          <text>可报名任务</text>
        </view>
        <view :class="['tab', activeTab === 'mine' ? 'active' : '']" @tap="activeTab = 'mine'">
          <text class="tab-icon">🧾</text>
          <text>我的报名</text>
        </view>
      </view>

      <view class="content panel">
        <view class="glass-card tips-card">
          <view class="tips-row">
            <text class="tips-icon">💡</text>
            <text class="tips-text">任务结束后会显示“待确认”，管理员确认后自动发币</text>
          </view>
        </view>

        <view class="glass-card list-card">
          <view class="list-header">
            <text class="list-title">{{ activeTab === 'mine' ? '我的报名' : '可报名任务' }}</text>
            <text class="more-link" @tap="refresh">刷新</text>
          </view>

          <view v-if="loading && shownTasks.length === 0" class="empty">
            <text class="empty-icon">⏳</text>
            <text class="empty-text">加载中…</text>
          </view>

          <view v-else-if="!loading && shownTasks.length === 0" class="empty">
            <text class="empty-icon">🧑‍💼</text>
            <text class="empty-text">{{ activeTab === 'mine' ? '暂无报名记录' : '暂无值班任务' }}</text>
            <text class="empty-sub">
              {{ activeTab === 'mine' ? '去“可报名任务”里选择一个任务报名吧' : '当前可先管理员发布后会出现在这里' }}
            </text>
          </view>

          <view v-else class="task-list">
            <view class="task-item" v-for="t in shownTasks" :key="t.id">
              <view class="task-top">
                <text class="task-name">{{ t.taskName }}</text>
                <view :class="['badge', taskStatusClass(t)]">
                  <text class="badge-dot"></text>
                  <text>{{ taskStatusText(t) }}</text>
                </view>
              </view>

              <view class="task-meta">
                <text class="meta-line">🕒 {{ fmt(t.startTime) }} ～ {{ fmt(t.endTime) }}</text>
                <text class="meta-line">👥 名额：{{ t.signupCount || 0 }}/{{ t.requiredPeople || 0 }}（剩余 {{ t.remaining || 0 }}）</text>
                <text class="meta-line">💎 奖励：+{{ t.rewardCoins || 0 }} 币/人</text>
              </view>

              <view v-if="t.taskDesc" class="desc">{{ t.taskDesc }}</view>

              <view class="actions">
                <!-- 在"我的报名"标签页显示取消按钮 -->
                <button
                  v-if="activeTab === 'mine' && canCancel(t)"
                  class="cancel-btn"
                  @tap="doCancel(t)"
                >
                  取消报名
                </button>
                <!-- 原报名按钮 -->
                <button
                  v-else
                  class="primary-btn"
                  :disabled="!canSignup(t)"
                  @tap="doSignup(t)"
                >
                  <text class="btn-icon">📝</text>
                  <text class="btn-text">{{ signupBtnText(t) }}</text>
                </button>
              </view>
            </view>

            <view v-if="loading" class="load-more">加载中…</view>
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

const tasks = ref([]);
const loading = ref(false);
const activeTab = ref('available');

const shownTasks = computed(() => {
  const list = Array.isArray(tasks.value) ? tasks.value : [];
  if (activeTab.value === 'mine') {
    return list.filter(t => String(t?.mySignupStatus || '').trim() !== '');
  }
  return list;
});

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

const fmt = (v) => {
  if (!v) return '—';
  const d = new Date(v);
  if (Number.isNaN(d.getTime())) return String(v).replace('T', ' ').slice(0, 16);

  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hh = String(d.getHours()).padStart(2, '0');
  const mm = String(d.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${day} ${hh}:${mm}`;
};

const taskStatusText = (t) => {
  const s = String(t.taskStatus || '').toUpperCase();
  if (s === 'NOT_STARTED') return '未开始';
  if (s === 'IN_PROGRESS') return '进行中';
  return '已结束';
};

const taskStatusClass = (t) => {
  const s = String(t.taskStatus || '').toUpperCase();
  if (s === 'NOT_STARTED') return 'warn';
  if (s === 'IN_PROGRESS') return 'ok';
  return 'muted';
};

const mySignupText = (t) => {
  const s = String(t.mySignupStatus || '').toUpperCase();
  if (s === 'PENDING') return '待审核';
  if (s === 'APPROVED') return '已通过';
  if (s === 'REJECTED') return '已驳回';
  if (s === 'SIGNED') return '已报名';
  if (s === 'PENDING_CONFIRM') return '待确认';
  if (s === 'COMPLETED') return '已确认';
  return '';
};

const canSignup = (t) => {
  if (!t) return false;
  const status = String(t.taskStatus || '').toUpperCase();
  const my = String(t.mySignupStatus || '').toUpperCase();
  const remaining = Number(t.remaining ?? 0);

  if (my) return false;
  if (status === 'ENDED') return false;
  if (remaining <= 0) return false;
  return true;
};

const signupBtnText = (t) => {
  const my = mySignupText(t);
  if (my) return my;
  if (String(t.taskStatus || '').toUpperCase() === 'ENDED') return '已结束';
  if (Number(t.remaining ?? 0) <= 0) return '名额已满';
  return '报名';
};

const fetchTasks = async () => {
  if (loading.value) return;
  const sessionId = getSessionId();
  loading.value = true;

  await new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/duty-tasks`,
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
          tasks.value = Array.isArray(data.tasks) ? data.tasks : [];
        }
        resolve();
      },
      fail: () => resolve()
    });
  });

  loading.value = false;
};

const refresh = async () => {
  await fetchTasks();
};

const canCancel = (t) => {
  if (!t) return false;
  // 只有待审核(PENDING)状态且任务未结束才能取消
  const my = String(t.mySignupStatus || '').toUpperCase();
  const status = String(t.taskStatus || '').toUpperCase();
  return my === 'PENDING' && status !== 'ENDED';
};

const doSignup = async (t) => {
  if (!canSignup(t)) return;

  const sessionId = getSessionId();
  uni.showLoading({ title: '提交中…' });

  await new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/duty-tasks/${t.id}/signups`,
      method: 'POST',
      header: { 'X-Session-Id': sessionId, 'Content-Type': 'application/json' },
      withCredentials: true,
      data: {},
      success: (res) => {
        uni.hideLoading();
        if (res.statusCode === 401) {
          handle401();
          resolve();
          return;
        }
        if (res.statusCode === 200 && res.data && res.data.success) {
          uni.showToast({ title: '报名成功', icon: 'success' });
          fetchTasks();
          resolve();
          return;
        }
        uni.showToast({ title: (res.data && res.data.message) ? res.data.message : '报名失败', icon: 'none' });
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

const doCancel = async (t) => {
  if (!canCancel(t)) return;

  // 二次确认
  uni.showModal({
    title: '确认取消',
    content: `确定要取消报名"${t.taskName}"吗？`,
    success: async (modalRes) => {
      if (!modalRes.confirm) return;

      const sessionId = getSessionId();
      uni.showLoading({ title: '取消中…' });

      await new Promise((resolve) => {
        uni.request({
          url: `${baseUrl}/api/duty-tasks/${t.id}/signups`,
          method: 'DELETE',
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
              uni.showToast({ title: '取消成功', icon: 'success' });
              fetchTasks();
              resolve();
              return;
            }
            uni.showToast({ title: (res.data && res.data.message) ? res.data.message : '取消失败', icon: 'none' });
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

onShow(async () => {
  const userInfo = uni.getStorageSync('userInfo');
  if (!userInfo) return;
  await fetchTasks();
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
  gap: 18rpx;
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

.header-mid {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
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

.sub {
  font-size: 24rpx;
  color: $text-light;
  font-weight: 600;
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
  font-size: 26rpx;
}

.content {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18rpx;
}

.tips-card {
  /* 让提示横幅更“高”一些 */
  /* 仅收紧 tabs 与提示横幅的距离（只影响这一张卡片） */
  margin-top: -14rpx;
  padding: 28rpx 24rpx;
  background: linear-gradient(135deg, rgba(255,255,255,0.90), rgba(255,255,255,0.70));
}

.tips-row {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
}

.tips-icon {
  font-size: 30rpx;
}

.tips-text {
  flex: 1;
  font-size: 26rpx;
  color: $text-main;
  font-weight: 650;
  opacity: 0.92;
  line-height: 1.5;
}

.list-card {
  padding: 44rpx 34rpx;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
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

.task-item {
  padding: 24rpx 0;
  border-bottom: 1px solid rgba(0,0,0,0.05);
  animation: slideUp 0.5s ease-out;
}

.task-item:last-child {
  border-bottom: none;
}

.task-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 12rpx;
}

.task-name {
  flex: 1;
  font-size: 30rpx;
  color: $text-main;
  font-weight: 900;
}

.badge {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  padding: 10rpx 18rpx;
  border-radius: $radius-full;
  font-size: 24rpx;
  font-weight: 900;
  background: rgba($primary, 0.14);
  color: $primary;
}

.badge .badge-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 9999rpx;
  background: currentColor;
}

.badge.ok {
  background: rgba($primary, 0.14);
  color: $primary;
}

.badge.warn {
  background: rgba($secondary, 0.14);
  color: $secondary;
}

.badge.muted {
  background: rgba($text-light, 0.12);
  color: $text-light;
}

.task-meta {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.meta-line {
  font-size: 26rpx;
  color: $text-main;
  opacity: 0.88;
  font-weight: 650;
}

.desc {
  margin-top: 12rpx;
  padding: 16rpx 18rpx;
  border-radius: 18rpx;
  background: rgba(248, 250, 252, 0.8);
  color: $text-light;
  font-size: 24rpx;
  font-weight: 650;
  line-height: 1.6;
}

.actions {
  margin-top: 18rpx;
  display: flex;
  justify-content: flex-end;
}

.primary-btn {
  height: 80rpx;
  min-width: 220rpx;
  padding: 0 24rpx;
  border-radius: $radius-full;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  background: $primary-gradient;
  color: $white;
  font-size: 28rpx;
  font-weight: 800;
  box-shadow: 0 10rpx 20rpx rgba(99, 102, 241, 0.25);
  transition: transform 0.2s;
}

.primary-btn:active {
  transform: scale(0.98);
}

.primary-btn[disabled] {
  opacity: 0.6;
}

.cancel-btn {
  height: 80rpx;
  min-width: 220rpx;
  padding: 0 24rpx;
  border-radius: $radius-full;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  background: linear-gradient(135deg, #f59e0b 0%, #ef4444 100%);
  color: $white;
  font-size: 28rpx;
  font-weight: 800;
  box-shadow: 0 10rpx 20rpx rgba(239, 68, 68, 0.25);
  transition: transform 0.2s;
}

.cancel-btn:active {
  transform: scale(0.98);
}

.empty {
  padding: 40rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14rpx;
}

.empty-icon {
  font-size: 70rpx;
}

.empty-text {
  font-size: 30rpx;
  font-weight: 900;
  color: $text-main;
}

.empty-sub {
  font-size: 24rpx;
  color: $text-light;
  text-align: center;
  line-height: 1.6;
}

.load-more {
  padding: 18rpx 0 0;
  text-align: center;
  color: $text-light;
  font-size: 24rpx;
}
</style>
