<template>
  <view class="container">
    <view class="header">
      <view class="back-btn" @tap="goBack">←</view>
      <text class="title">每日打卡</text>
    </view>

    <view class="stats-card glass-card">
      <view class="stat-item">
        <text class="stat-val">{{ currentStreak }}</text>
        <text class="stat-label"><text class="stat-icon">🔥</text> 连续打卡(天)</text>
      </view>
      <view class="stat-item">
        <text class="stat-val">{{ balance }}</text>
        <text class="stat-label"><text class="stat-icon">💰</text> 当前积分</text>
      </view>
    </view>

    <view class="calendar-card glass-card">
      <view class="calendar-header">
        <text class="calendar-title"><text class="calendar-icon">🗓️</text>{{ year }}年{{ month }}月</text>
      </view>
      <view class="week-row">
        <text v-for="d in ['日','一','二','三','四','五','六']" :key="d">{{ d }}</text>
      </view>
      <view class="days-grid">
        <view 
          v-for="(day, idx) in days" 
          :key="idx" 
          :class="['day-cell', { 'today': day.isToday, 'checked': day.checked }]"
        >
          <text>{{ day.date || '' }}</text>
          <view v-if="day.checked" class="check-mark">✓</view>
        </view>
      </view>
    </view>

    <view class="action-area">
      <button class="checkin-btn" @tap="handleCheckin" :disabled="todayChecked">
        <text v-if="!todayChecked" class="btn-icon">📷</text>
        <text class="btn-text">{{ todayChecked ? '已打卡' : '立即打卡' }}</text>
      </button>
      <text class="tip-text" v-if="!todayChecked">连续打卡3天以上，每日奖励翻倍！</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { baseUrl } from '../../common/config.js';

const year = ref(new Date().getFullYear());
const month = ref(new Date().getMonth() + 1);
const days = ref([]);
const currentStreak = ref(0);
const balance = ref(0);
const todayChecked = ref(false);
const hasFace = ref(false);

const goBack = () => {
  uni.navigateBack();
};

const fetchStatus = () => {
  // 获取 SessionID (优先从 cookie 字符串解析，或者直接存一个 key)
  // 之前我们存的是 'JSESSIONID=xxx'，这里简单处理一下
  let sessionId = '';
  const cookieStr = uni.getStorageSync('cookie');
  if (cookieStr && cookieStr.includes('JSESSIONID=')) {
    sessionId = cookieStr.split('JSESSIONID=')[1].split(';')[0];
  }

  uni.request({
    url: `${baseUrl}/api/face/status`,
    method: 'GET',
    header: { 
      'X-Session-Id': sessionId // 手动传递 SessionID
    },
    withCredentials: true, 
    success: (res) => {
      // 后端统一返回 ApiResponse: { success, message, data }
      if (res.statusCode === 200 && res.data && res.data.success) {
        const data = res.data.data || {};
        currentStreak.value = data.currentStreak;
        balance.value = data.balance;
        hasFace.value = data.hasFace;
      } else if (res.statusCode === 401) {
        uni.showToast({ title: '登录已过期', icon: 'none' });
        setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 1500);
      } else {
        uni.showToast({ title: (res.data && res.data.message) || '获取状态失败', icon: 'none' });
      }
    }
  });
};

const fetchHistory = () => {
  let sessionId = '';
  const cookieStr = uni.getStorageSync('cookie');
  if (cookieStr && cookieStr.includes('JSESSIONID=')) {
    sessionId = cookieStr.split('JSESSIONID=')[1].split(';')[0];
  }

  uni.request({
    url: `${baseUrl}/api/face/history?year=${year.value}&month=${month.value}`,
    method: 'GET',
    header: { 
      'X-Session-Id': sessionId 
    },
    withCredentials: true,
    success: (res) => {
      if (res.statusCode === 200 && res.data && res.data.success) {
        generateCalendar(res.data.data || []);
      } else {
        generateCalendar([]);
      }
    },
    fail: () => {
      generateCalendar([]);
    }
  });
};

// 将后端返回的日期字段尽量归一成 YYYY-MM-DD
const normalizeToYmd = (value) => {
  if (!value) return '';
  if (typeof value === 'string') {
    // 兼容 "2025-12-16" / "2025-12-16T00:00:00.000+08:00" 等
    return value.slice(0, 10);
  }
  if (typeof value === 'number') {
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return '';
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }
  return '';
};

const generateCalendar = (history) => {
  // 确保 history 是数组
  const checkinList = Array.isArray(history) ? history : [];

  // 每次生成日历都重置一次今日状态，避免月切换/返回页面时状态残留
  todayChecked.value = false;
  
  const firstDay = new Date(year.value, month.value - 1, 1).getDay();
  const lastDate = new Date(year.value, month.value, 0).getDate();
  const today = new Date().getDate();
  const isCurrentMonth = new Date().getMonth() + 1 === month.value;

  const tempDays = [];
  // Empty slots for previous month
  for (let i = 0; i < firstDay; i++) {
    tempDays.push({});
  }

  // Days
  for (let i = 1; i <= lastDate; i++) {
    const isToday = isCurrentMonth && i === today;
    // Check if this day is in history
    // 兼容后端日期序列化为字符串/时间戳/ISO
    const dateStr = `${year.value}-${String(month.value).padStart(2, '0')}-${String(i).padStart(2, '0')}`;
    const checked = checkinList.some(h => normalizeToYmd(h.checkinDate) === dateStr);
    
    if (isToday && checked) {
      todayChecked.value = true;
    }

    tempDays.push({
      date: i,
      isToday: isToday,
      checked: checked
    });
  }
  days.value = tempDays;
};


const handleCheckin = () => {
  if (!hasFace.value) {
    uni.showModal({
      title: '首次打卡',
      content: '您需要先录入人脸信息才能进行打卡。',
      confirmText: '去录入',
      success: (res) => {
        if (res.confirm) {
          uni.navigateTo({ url: '/pages/face/register' });
        }
      }
    });
    return;
  }

  // Open Camera
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['camera'],
    success: (res) => {
      const tempFilePath = res.tempFilePaths[0];
      uploadAndCheckin(tempFilePath);
    }
  });
};

// H5 环境下 Base64 转换助手
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

const uploadAndCheckin = (filePath) => {
  uni.showLoading({ title: '人脸比对中...' });
  
  // #ifdef H5
  pathToBase64(filePath).then(base64 => {
      sendCheckinRequest(base64);
  }).catch(err => {
      uni.hideLoading();
      uni.showToast({ title: '图片处理失败', icon: 'none' });
  });
  // #endif

  // #ifndef H5
  uni.getFileSystemManager().readFile({
    filePath: filePath,
    encoding: 'base64',
    success: (res) => {
      sendCheckinRequest(res.data);
    },
    fail: () => {
      uni.hideLoading();
      uni.showToast({ title: '图片读取失败', icon: 'none' });
    }
  });
  // #endif
};

const sendCheckinRequest = (base64Image) => {
  let sessionId = '';
  const cookieStr = uni.getStorageSync('cookie');
  if (cookieStr && cookieStr.includes('JSESSIONID=')) {
    sessionId = cookieStr.split('JSESSIONID=')[1].split(';')[0];
  }

  uni.request({
    url: `${baseUrl}/api/face/checkin`,
    method: 'POST',
    data: { image: base64Image },
    header: { 
      'X-Session-Id': sessionId 
    },
    withCredentials: true,
    success: (checkRes) => {
      uni.hideLoading();
      if (checkRes.statusCode === 401) {
         uni.showToast({ title: '登录已过期', icon: 'none' });
         setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 1500);
         return;
      }
      if (checkRes.statusCode === 200 && checkRes.data && checkRes.data.success) {
        // 打卡成功时，后端把奖励提示放在 data.message 里
        const tip = (checkRes.data.data && checkRes.data.data.message) || checkRes.data.message || '打卡成功';
        uni.showToast({ title: tip, icon: 'none' });
        todayChecked.value = true;
        fetchStatus();
        fetchHistory();
      } else {
        const msg = (checkRes.data && checkRes.data.message) || '打卡失败';
        // 兜底：如果前端 hasFace 状态未及时刷新，后端会返回“请先进行人脸录入”
        if (msg.includes('人脸录入')) {
          hasFace.value = false;
          uni.showModal({
            title: '需要先录入人脸',
            content: msg,
            confirmText: '去录入',
            success: (r) => {
              if (r.confirm) {
                uni.navigateTo({ url: '/pages/face/register' });
              }
            }
          });
          return;
        }
        uni.showToast({ title: msg, icon: 'none' });
      }
    },
    fail: () => {
      uni.hideLoading();
      uni.showToast({ title: '网络错误', icon: 'none' });
    }
  });
};

// 进入页面/从录入页返回时都刷新一次状态，确保 hasFace 最新
onShow(() => {
  const userInfo = uni.getStorageSync('userInfo');
  if (!userInfo) return;
  fetchStatus();
  fetchHistory();
});

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

  // 先生成一个空的日历，防止网络请求慢或失败时页面空白
  generateCalendar([]);
});
</script>

<style lang="scss" scoped>
@import '../../uni.scss';

/*
  打卡页视觉风格：沿用首页/全局 uni.scss 的玻璃拟态 + 渐变主色
  目标：全屏自适应、信息层级清晰、交互更“轻”
*/

.container {
  min-height: 100vh;
  padding: 30rpx;
  padding-top: 80rpx;
  justify-content: flex-start; /* 覆盖全局 container 的居中 */
  gap: 24rpx;
}

.header {
  width: 100%;
  max-width: 680rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 8rpx;
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
  letter-spacing: 1rpx;
}

.stats-card {
  width: 100%;
  max-width: 680rpx;
  padding: 36rpx 28rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
}

.stat-val {
  font-size: 52rpx;
  font-weight: 900;
  color: $primary;
  line-height: 1;
}

.stat-label {
  font-size: 24rpx;
  color: $text-light;
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.stat-icon {
  font-size: 26rpx;
}

.calendar-card {
  width: 100%;
  max-width: 680rpx;
  padding: 32rpx 28rpx;
}

.calendar-header {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 22rpx;
}

.calendar-title {
  font-size: 34rpx;
  font-weight: 800;
  color: $text-main;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.calendar-icon {
  font-size: 30rpx;
}

.week-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  margin-bottom: 14rpx;
  color: $text-light;
  font-size: 24rpx;
  font-weight: 600;
}

.days-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 14rpx;
}

.day-cell {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius-full;
  font-size: 28rpx;
  color: $text-main;
  position: relative;
  background: rgba(255, 255, 255, 0.6);
  border: 2rpx solid rgba(99, 102, 241, 0.06);
  transition: transform 0.2s ease, background 0.2s ease;
}

.day-cell.today {
  background: rgba(99, 102, 241, 0.12);
  color: $primary;
  font-weight: 800;
}

.day-cell.checked {
  background: $primary-gradient;
  color: $white;
  border-color: rgba(255, 255, 255, 0.25);
}

.day-cell:active {
  transform: scale(0.96);
}

.check-mark {
  position: absolute;
  bottom: 6rpx;
  font-size: 20rpx;
  animation: pop 0.2s ease-out;
}

.action-area {
  width: 100%;
  max-width: 680rpx;
  margin-top: 18rpx;
  text-align: center;
}

.checkin-btn {
  width: 100%;
  background: $primary-gradient;
  color: $white;
  border-radius: $radius-full;
  padding: 26rpx 0;
  font-size: 34rpx;
  font-weight: 800;
  box-shadow: 0 16rpx 34rpx rgba(99, 102, 241, 0.28);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  transition: transform 0.18s ease, filter 0.18s ease, opacity 0.18s ease;
}

.checkin-btn:active {
  transform: scale(0.98);
}

/* 已打卡：保持“同色系”，但明显不可用 */
.checkin-btn:disabled {
  filter: grayscale(0.35);
  opacity: 0.55;
  box-shadow: none;
}

.btn-icon {
  font-size: 34rpx;
}

.btn-text {
  font-size: 34rpx;
}

.tip-text {
  display: block;
  margin-top: 18rpx;
  font-size: 24rpx;
  color: $text-light;
}

@keyframes pop {
  from { transform: scale(0.7); opacity: 0.2; }
  to { transform: scale(1); opacity: 1; }
}
</style>
