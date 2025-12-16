<template>
  <view class="container">
    <view class="header">
      <view class="back-btn" @tap="goBack">←</view>
      <text class="title">每日打卡</text>
    </view>

    <view class="stats-card glass-card">
      <view class="stat-item">
        <text class="stat-val">{{ currentStreak }}</text>
        <text class="stat-label">连续打卡(天)</text>
      </view>
      <view class="stat-item">
        <text class="stat-val">{{ balance }}</text>
        <text class="stat-label">当前积分</text>
      </view>
    </view>

    <view class="calendar-card glass-card">
      <view class="calendar-header">
        <text>{{ year }}年{{ month }}月</text>
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
        {{ todayChecked ? '今日已打卡' : '立即打卡' }}
      </button>
      <text class="tip-text" v-if="!todayChecked">连续打卡3天以上，每日奖励翻倍！</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
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
      if (res.data.code === 200) {
        const data = res.data.data;
        currentStreak.value = data.currentStreak;
        balance.value = data.balance;
        hasFace.value = data.hasFace;
      } else if (res.statusCode === 401) {
        uni.showToast({ title: '登录已过期', icon: 'none' });
        setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 1500);
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
      if (res.data.code === 200) {
        generateCalendar(res.data.data);
      } else {
        generateCalendar([]);
      }
    },
    fail: () => {
      generateCalendar([]);
    }
  });
};

const generateCalendar = (history) => {
  // 确保 history 是数组
  const checkinList = Array.isArray(history) ? history : [];
  
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
    // History dates are strings like "2025-12-15"
    const dateStr = `${year.value}-${String(month.value).padStart(2, '0')}-${String(i).padStart(2, '0')}`;
    const checked = checkinList.some(h => h.checkinDate === dateStr);
    
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
      if (checkRes.data.code === 200) {
        uni.showToast({ title: checkRes.data.data.message, icon: 'none' });
        todayChecked.value = true;
        fetchStatus();
        fetchHistory();
      } else {
        uni.showToast({ title: checkRes.data.message, icon: 'none' });
      }
    },
    fail: () => {
      uni.hideLoading();
      uni.showToast({ title: '网络错误', icon: 'none' });
    }
  });
};

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
  fetchStatus();
  fetchHistory();
});
</script>

<style>
.container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f6f7fb 0%, #e2e6f0 100%);
  padding: 20px;
  box-sizing: border-box;
}

.header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding-top: 20px;
}

.back-btn {
  font-size: 24px;
  margin-right: 15px;
  color: #333;
}

.title {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.glass-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.1);
  margin-bottom: 20px;
  padding: 20px;
}

.stats-card {
  display: flex;
  justify-content: space-around;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-val {
  font-size: 24px;
  font-weight: bold;
  color: #4a90e2;
}

.stat-label {
  font-size: 12px;
  color: #666;
  margin-top: 5px;
}

.calendar-header {
  text-align: center;
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 15px;
  color: #333;
}

.week-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  margin-bottom: 10px;
  color: #999;
  font-size: 12px;
}

.days-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.day-cell {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 14px;
  color: #333;
  position: relative;
}

.day-cell.today {
  background: rgba(74, 144, 226, 0.1);
  color: #4a90e2;
  font-weight: bold;
}

.day-cell.checked {
  background: #4a90e2;
  color: white;
}

.check-mark {
  position: absolute;
  bottom: 2px;
  font-size: 10px;
}

.action-area {
  margin-top: 40px;
  text-align: center;
}

.checkin-btn {
  background: linear-gradient(135deg, #4a90e2 0%, #357abd 100%);
  color: white;
  border-radius: 30px;
  padding: 10px 0;
  font-size: 18px;
  box-shadow: 0 4px 15px rgba(74, 144, 226, 0.4);
  border: none;
}

.checkin-btn:disabled {
  background: #ccc;
  box-shadow: none;
}

.tip-text {
  display: block;
  margin-top: 15px;
  font-size: 12px;
  color: #666;
}
</style>
