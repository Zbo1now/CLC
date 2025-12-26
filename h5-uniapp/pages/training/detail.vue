<template>
  <view class="container">
    <view class="shell">
      <view class="header panel">
        <view class="back-btn" @tap="goBack">←</view>
        <text class="title">活动详情</text>
      </view>
      <view class="content panel">
        <view v-if="loading" class="glass-card list-card animate-in">
          <view class="empty">
            <text class="empty-text">加载中…</text>
          </view>
        </view>
        <view v-else-if="!event" class="glass-card list-card animate-in">
          <view class="empty">
            <text class="empty-icon">🧾</text>
            <text class="empty-text">活动不存在</text>
            <text class="empty-sub">可能已下架或参数错误</text>
          </view>
        </view>
        <view v-else>
          <view class="glass-card list-card animate-in">
            <view class="list-header">
              <text class="list-title">{{ event.eventName }}</text>
              <view :class="['badge', eventBadgeClass]">
                <text class="badge-dot"></text>
                <text>{{ eventBadgeText }}</text>
              </view>
            </view>
            <view class="section-title">活动信息</view>
            <view class="row">
              <view class="row-left">
                <text class="row-icon">🕒</text>
                <text class="row-label">时间</text>
              </view>
              <view class="row-right">
                <text class="row-value wrap">{{ timeRangeText }}</text>
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
                <text class="row-icon">💰</text>
                <text class="row-label">奖励</text>
              </view>
              <view class="row-right">
                <text class="row-value">+{{ Number(event.rewardCoins || 0) }} 币</text>
              </view>
            </view>
            <view class="row">
              <view class="row-left">
                <text class="row-icon">👥</text>
                <text class="row-label">人数限制</text>
              </view>
              <view class="row-right">
                <text class="row-value">{{ event.currentParticipants || 0 }} / {{ event.maxParticipants || 0 }}</text>
              </view>
            </view>
            <view class="row">
              <view class="row-left">
                <text class="row-icon">🧾</text>
                <text class="row-label">我的申报</text>
              </view>
              <view class="row-right">
                <text class="row-value wrap">{{ myStatusText }}</text>
              </view>
            </view>
            <!-- 活动未结束且团队已报名，显示“已报名”按钮且禁用 -->
            <button v-if="event && (event.eventStatus === 'NOT_STARTED' || event.eventStatus === 'IN_PROGRESS') && event.myParticipationStatus" class="primary-btn" disabled>
              <text class="btn-text">已报名</text>
            </button>
            <!-- 其他情况正常显示报名按钮 -->
            <button v-else-if="canSignup" class="primary-btn" :disabled="signingUp" @tap="signup">
              <text class="btn-text">{{ signingUp ? '报名中...' : '报名参加' }}</text>
            </button>
            <view v-if="event.description" class="block">
              <view class="block-head">
                <text class="row-icon">📝</text>
                <text class="row-label">活动说明</text>
              </view>
              <view class="block-body">
                <text class="desc-text">{{ event.description }}</text>
              </view>
            </view>
          </view>
          <view v-if="canSubmit" class="glass-card form-card animate-in">
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
                <button class="ghost-btn" @tap="pickProof">
                  <text class="proof-btn-text">重新选择</text>
                </button>
                <button class="danger-btn" @tap="clearProof">
                  <!-- <text class="btn-icon">🗑️</text> -->
                  <text class="proof-btn-text">移除</text>
                </button>
              </view>
            </view>
            <button class="primary-btn" style="margin-top: 18rpx;" :disabled="submitting || !proofPreview" @tap="submit">
              <text class="btn-text">{{ submitting ? '提交中...' : '提交材料' }}</text>
            </button>
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
const signingUp = ref(false);
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

const fmtMDHM = (v) => {
  if (!v) return '';
  const d = new Date(v);
  if (Number.isNaN(d.getTime())) {
    // 兜底：尽量截到“MM-DD HH:mm”长度
    return String(v).slice(5, 16).replace('T', ' ');
  }
  const pad = (n) => String(n).padStart(2, '0');
  const mm = pad(d.getMonth() + 1);
  const dd = pad(d.getDate());
  const hh = pad(d.getHours());
  const mi = pad(d.getMinutes());
  return `${mm}-${dd} ${hh}:${mi}`;
};

const fmtHM = (v) => {
  if (!v) return '';
  const d = new Date(v);
  if (Number.isNaN(d.getTime())) return '';
  const pad = (n) => String(n).padStart(2, '0');
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

const timeRangeText = computed(() => {
  if (!event.value) return '';
  const s = event.value.startTime;
  const e = event.value.endTime;
  if (!s && !e) return '';

  const sd = new Date(s);
  const ed = new Date(e);
  const validS = s && !Number.isNaN(sd.getTime());
  const validE = e && !Number.isNaN(ed.getTime());

  if (validS && validE) {
    const sameDay = sd.getFullYear() === ed.getFullYear() && sd.getMonth() === ed.getMonth() && sd.getDate() === ed.getDate();
    if (sameDay) {
      return `${fmtMDHM(s)}～${fmtHM(e)}`;
    }
    return `${fmtMDHM(s)}～${fmtMDHM(e)}`;
  }

  return fmtMDHM(s || e);
});

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
  const st = String(event.value?.eventStatus || '').toUpperCase();
  const mine = String(event.value?.myParticipationStatus || '').toUpperCase();
  const proofed = !!event.value?.myProofSubmitted;
  if (!mine) {
    if (st === 'IN_PROGRESS' || st === 'ENDED') return '未报名';
    return '尚未报名';
  }
  if (mine && !proofed) {
    if (st === 'NOT_STARTED' || st === 'IN_PROGRESS') return '报名完成';
    if (st === 'ENDED') return '可提交证明材料';
  }
  if (mine && proofed) {
    // proof已提交，才显示审核状态
    const s = String(event.value?.myParticipationStatus || '').toUpperCase();
    if (s === 'APPROVED') return '材料已通过';
    if (s === 'REJECTED') return '材料被驳回';
    return '材料待审核';
  }
  return '状态异常';
});

const canSubmit = computed(() => {
  const st = String(event.value?.eventStatus || '').toUpperCase();
  const mine = String(event.value?.myParticipationStatus || '').toUpperCase();
  return mine && st === 'ENDED' && !event.value?.myProofSubmitted;
});

const canSignup = computed(() => {
  const st = String(event.value?.eventStatus || '').toUpperCase();
  const mine = String(event.value?.myParticipationStatus || '').toUpperCase();
  if (!event.value) return false;
  if (mine) return false;
  // 只有未开始和进行中才能报名，已结束不能报名
  return st === 'NOT_STARTED' || st === 'IN_PROGRESS';
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

const signup = async () => {
  if (!event.value || signingUp.value) return;

  signingUp.value = true;
  uni.vibrateShort?.();
  uni.showLoading({ title: '报名中...' });

  const sessionId = getSessionId();
  await new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/training-events/${eventId.value}/signups`,
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
          uni.showToast({ title: '报名成功', icon: 'success' });
          fetchDetail();
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

  signingUp.value = false;
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
  align-items: stretch;
}

.glass-card {
  width: 100%;
  max-width: 640rpx;
  background: rgba($white, 0.98);
  border-radius: $radius-lg;
  box-shadow: $shadow-sm;
  margin-left: auto;
  margin-right: auto;
  margin-bottom: 28rpx;
  box-sizing: border-box;
}

.animate-in {
  animation: riseIn 0.32s ease both;
}

@keyframes riseIn {
  from {
    opacity: 0;
    transform: translateY(14rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.list-card {
  padding: 44rpx 34rpx;
}

.form-card {
  padding: 44rpx 34rpx;
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
  line-height: 1.2;
  flex: 1;
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

.section-title {
  font-size: 32rpx;
  font-weight: 800;
  color: $text-main;
  margin-bottom: 26rpx;
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

.row-value.wrap {
  max-width: 380rpx;
  white-space: normal;
  overflow: visible;
  text-overflow: clip;
}

.row-arrow {
  font-size: 36rpx;
  color: $text-light;
}

.row-input {
  flex: 1;
  font-size: 28rpx;
  color: $text-main;
  text-align: right;
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
  width: 100%;
  font-size: 28rpx;
  color: $text-main;
  line-height: 1.5;
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
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  box-shadow: $shadow-lg;
  transform: translateY(0);
  transition: all 0.2s ease;
}

.primary-btn:active {
  transform: scale(0.99);
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
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
}

.danger-btn {
  flex: 1;
  border-radius: $radius-full;
  background: rgba($secondary, 0.18);
  color: $secondary;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
}

.btn-icon {
  font-size: 30rpx;
  line-height: 1;
}

.btn-text {
  font-size: 32rpx;
  font-weight: 800;
}

.proof-btn-text {
  font-size: 28rpx;
  font-weight: 800;
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
</style>
