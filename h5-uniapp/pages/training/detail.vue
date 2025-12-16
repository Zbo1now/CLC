<template>
  <view class="container">
    <view class="shell">
      <view class="header panel">
        <view class="back-btn" @tap="goBack">←</view>
        <text class="title">活动详情</text>
      </view>

      <view class="content panel">
        <view v-if="loading" class="glass-card list-card">
          <view class="empty">
            <text class="empty-text">加载中…</text>
          </view>
        </view>

        <view v-else-if="!event" class="glass-card list-card">
          <view class="empty">
            <text class="empty-icon">🧾</text>
            <text class="empty-text">活动不存在</text>
            <text class="empty-sub">可能已下架或参数错误</text>
          </view>
        </view>

        <view v-else>
          <view class="glass-card list-card">
            <view class="list-header">
              <text class="list-title">{{ event.eventName }}</text>
              <view :class="['badge', eventBadgeClass]">
                <text class="badge-dot"></text>
                <text>{{ eventBadgeText }}</text>
              </view>
            </view>

            <view class="row">
              <view class="row-left">
                <text class="row-icon">🧩</text>
                <text class="row-label">类型</text>
              </view>
              <view class="row-right">
                <text class="row-value">{{ event.eventType || '培训/会议' }}</text>
              </view>
            </view>

            <view class="row">
              <view class="row-left">
                <text class="row-icon">🕒</text>
                <text class="row-label">时间</text>
              </view>
              <view class="row-right">
                <text class="row-value">{{ fmt(event.startTime) }} ～ {{ fmt(event.endTime) }}</text>
              </view>
            </view>

            <view class="row">
              <view class="row-left">
                <text class="row-icon">📍</text>
                <text class="row-label">地点</text>
              </view>
              <view class="row-right">
                <text class="row-value">{{ locationText }}</text>
              </view>
            </view>

            <view class="row">
              <view class="row-left">
                <text class="row-icon">🎉</text>
                <text class="row-label">奖励</text>
              </view>
              <view class="row-right">
                <text class="row-value">+{{ Number(event.rewardCoins || 0) }} 币</text>
              </view>
            </view>

            <view v-if="event.requireCheckin" class="row">
              <view class="row-left">
                <text class="row-icon">✅</text>
                <text class="row-label">签到</text>
              </view>
              <view class="row-right">
                <text class="row-value">可能需要签到（以活动要求为准）</text>
              </view>
            </view>

            <view v-if="event.description" class="block">
              <view class="block-head">
                <text class="row-icon">💬</text>
                <text class="row-label">活动说明</text>
              </view>
              <view class="block-body">
                <text class="desc-text">{{ event.description }}</text>
              </view>
            </view>

            <text class="hint">* 我的申报状态：{{ myStatusText }}</text>
          </view>

          <view v-if="canSubmit" class="glass-card form-card">
            <view class="section-title">提交参与证明</view>

            <view class="row clickable" @tap="pickProof">
              <view class="row-left">
                <text class="row-icon">📎</text>
                <text class="row-label">证明材料</text>
              </view>
              <view class="row-right">
                <text :class="['row-value', proofPreview ? '' : 'muted']">{{ proofPreview ? '已选择' : '未上传' }}</text>
                <text class="row-arrow">›</text>
              </view>
            </view>

            <view v-if="proofPreview" class="proof-preview">
              <image class="proof-img" :src="proofPreview" mode="aspectFill" />
              <view class="proof-actions">
                <button class="ghost-btn" @tap="pickProof">重新选择</button>
                <button class="danger-btn" @tap="clearProof">移除</button>
              </view>
            </view>

            <view class="row">
              <view class="row-left">
                <text class="row-icon">📝</text>
                <text class="row-label">补充说明</text>
              </view>
              <view class="row-right">
                <input class="row-input" v-model="note" placeholder="可选：如参与方式、组员名单等" />
              </view>
            </view>

            <button class="primary-btn" :disabled="submitting" @tap="submit">
              <text class="btn-icon">🚀</text>
              <text class="btn-text">{{ submitting ? '提交中...' : '提交申报' }}</text>
            </button>

            <text class="hint">* 提交后状态为“待审核”，管理员审核通过后自动发放虚拟币。</text>
          </view>

          <view v-else class="glass-card list-card">
            <view class="empty">
              <text class="empty-icon">💡</text>
              <text class="empty-text">暂不可提交</text>
              <text class="empty-sub">{{ tipsText }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { baseUrl } from '../../common/config.js';

const loading = ref(false);
const submitting = ref(false);
const eventId = ref(0);
const event = ref(null);

const proofPreview = ref('');
const proofBase64 = ref('');
const note = ref('');

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

const locationText = computed(() => {
  if (!event.value) return '';
  const mode = String(event.value.locationMode || '').toUpperCase();
  const place = event.value.locationDetail || '';
  if (mode === 'ONLINE') return place ? `线上 · ${place}` : '线上';
  return place ? place : '线下';
});

const eventBadgeText = computed(() => {
  const st = String(event.value?.eventStatus || '').toUpperCase();
  if (st === 'NOT_STARTED') return '未开始';
  if (st === 'ENDED') return '已结束';
  return '进行中';
});

const eventBadgeClass = computed(() => {
  const st = String(event.value?.eventStatus || '').toUpperCase();
  if (st === 'NOT_STARTED') return 'badge-not-started';
  if (st === 'ENDED') return 'badge-ended';
  return 'badge-in-progress';
});

const myStatusText = computed(() => {
  const s = String(event.value?.myParticipationStatus || '').toUpperCase();
  if (s === 'APPROVED') return '🎉 已通过，奖励已发放（可在流水查看）';
  if (s === 'REJECTED') return '❗ 已驳回，可在“我的申报”查看原因';
  if (s === 'PENDING') return '⏳ 已提交，等待管理员审核';
  return '尚未申报';
});

const canSubmit = computed(() => {
  const st = String(event.value?.eventStatus || '').toUpperCase();
  const mine = String(event.value?.myParticipationStatus || '').toUpperCase();
  return st === 'ENDED' && !mine;
});

const tipsText = computed(() => {
  const st = String(event.value?.eventStatus || '').toUpperCase();
  const mine = String(event.value?.myParticipationStatus || '').toUpperCase();
  if (mine) return '你已提交过该活动的申报，可返回查看状态。';
  if (st === 'NOT_STARTED') return '活动未开始，结束后再来提交证明。';
  if (st === 'IN_PROGRESS') return '活动进行中，结束后再来提交证明。';
  return '活动已结束，提交证明入口暂不可用。';
});

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

const pickProof = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['camera', 'album'],
    success: (res) => {
      const fp = res.tempFilePaths[0];
      proofPreview.value = fp;

      // #ifdef H5
      pathToBase64(fp).then((b64) => {
        proofBase64.value = b64;
      }).catch(() => {
        uni.showToast({ title: '图片处理失败', icon: 'none' });
      });
      // #endif

      // #ifndef H5
      uni.getFileSystemManager().readFile({
        filePath: fp,
        encoding: 'base64',
        success: (r) => {
          proofBase64.value = r.data;
        },
        fail: () => {
          uni.showToast({ title: '图片读取失败', icon: 'none' });
        }
      });
      // #endif
    }
  });
};

const clearProof = () => {
  proofPreview.value = '';
  proofBase64.value = '';
};

const fetchDetail = async () => {
  if (!eventId.value) return;
  const sessionId = getSessionId();
  loading.value = true;
  await new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/training-events/${eventId.value}`,
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
          event.value = data.event || null;
        } else {
          event.value = null;
        }
        resolve();
      },
      fail: () => {
        event.value = null;
        resolve();
      }
    });
  });
  loading.value = false;
};

const submit = async () => {
  if (!event.value) return;
  if (!proofBase64.value) {
    uni.showToast({ title: '请上传证明材料', icon: 'none' });
    return;
  }
  if (submitting.value) return;

  submitting.value = true;
  uni.vibrateShort?.();
  uni.showLoading({ title: '提交中...' });

  const sessionId = getSessionId();
  await new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/training-events/${eventId.value}/participations`,
      method: 'POST',
      header: {
        'Content-Type': 'application/json',
        'X-Session-Id': sessionId
      },
      withCredentials: true,
      data: {
        proofBase64: proofBase64.value,
        note: note.value
      },
      success: (res) => {
        uni.hideLoading();
        if (res.statusCode === 401) {
          handle401();
          resolve();
          return;
        }
        if (res.statusCode === 200 && res.data && res.data.success) {
          uni.showToast({ title: '提交成功', icon: 'success' });
          clearProof();
          note.value = '';
          fetchDetail();
          resolve();
          return;
        }
        uni.showToast({ title: (res.data && res.data.message) ? res.data.message : '提交失败', icon: 'none' });
        resolve();
      },
      fail: () => {
        uni.hideLoading();
        uni.showToast({ title: '网络异常', icon: 'none' });
        resolve();
      }
    });
  });

  submitting.value = false;
};

const goBack = () => {
  uni.navigateBack();
};

onLoad((q) => {
  eventId.value = Number(q?.eventId || 0);
  fetchDetail();
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
}

.title {
  font-size: 40rpx;
  font-weight: 800;
  color: $text-main;
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

.form-card {
  width: 100%;
  padding: 44rpx 34rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 800;
  color: $text-main;
  margin-bottom: 26rpx;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22rpx;
  gap: 16rpx;
}

.list-title {
  font-size: 32rpx;
  font-weight: 900;
  color: $text-main;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  transition: transform 0.18s ease;
}

.row.clickable:active {
  transform: scale(0.99);
}

.row-left {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.row-icon {
  font-size: 34rpx;
  line-height: 1;
}

.row-label {
  font-size: 28rpx;
  font-weight: 900;
  color: $text-main;
}

.row-right {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex: 1;
  justify-content: flex-end;
}

.row-value {
  font-size: 28rpx;
  font-weight: 800;
  color: $text-main;
  max-width: 360rpx;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-input {
  flex: 1;
  font-size: 28rpx;
  color: $text-main;
  text-align: right;
}

.row-arrow {
  font-size: 36rpx;
  color: $text-light;
}

.muted {
  color: $text-light;
  font-weight: 700;
}

.block {
  background: rgba($bg-color, 0.9);
  border-radius: 18rpx;
  box-shadow: $shadow-sm;
  padding: 18rpx 22rpx;
  margin-bottom: 18rpx;
}

.block-head {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 8rpx 0 14rpx;
}

.block-body {
  background: rgba($white, 0.6);
  border-radius: 16rpx;
  padding: 16rpx;
}

.desc-text {
  font-size: 28rpx;
  color: $text-main;
  line-height: 1.6;
}

.proof-preview {
  margin-top: 6rpx;
  border-radius: 18rpx;
  overflow: hidden;
  background: rgba($white, 0.85);
}

.proof-img {
  width: 100%;
  height: 320rpx;
}

.proof-actions {
  display: flex;
  gap: 16rpx;
  padding: 18rpx;
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

.ghost-btn {
  flex: 1;
  border-radius: $radius-full;
  background: rgba($white, 0.9);
  color: $primary;
  font-weight: 800;
}

.danger-btn {
  flex: 1;
  border-radius: $radius-full;
  background: rgba($secondary, 0.18);
  color: $secondary;
  font-weight: 800;
}

.btn-icon {
  font-size: 32rpx;
}

.btn-text {
  font-size: 32rpx;
}

.hint {
  display: block;
  margin-top: 18rpx;
  font-size: 24rpx;
  color: $text-light;
  line-height: 1.4;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  padding: 28rpx 0;
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
</style>
