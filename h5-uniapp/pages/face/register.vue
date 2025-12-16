<template>
  <view class="container">
    <view class="header-bar">
      <view class="back-btn" @tap="goBack">←</view>
    </view>
    <view class="glass-card">
      <view class="title-area">
        <view class="main-title">人脸信息录入</view>
        <view class="sub-title">请保持光线充足，正对摄像头</view>
      </view>

      <view class="camera-box" @tap="chooseImage">
        <image v-if="imagePath" :src="imagePath" class="preview-img" mode="aspectFill"></image>
        <view v-else class="placeholder">
          <text class="icon">📷</text>
          <text class="text">点击拍摄/上传照片</text>
        </view>
      </view>

      <button class="btn-submit" @tap="uploadFace" :disabled="!imagePath">
        <text>🚀</text> 确认上传
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { baseUrl } from '../../common/config.js';

const imagePath = ref('');
const imageBase64 = ref('');

function goBack() {
  uni.navigateBack();
}

onMounted(() => {
  const userInfo = uni.getStorageSync('userInfo');
  if (!userInfo) {
    uni.showToast({ title: '请先登录', icon: 'none' });
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/index/index' });
    }, 1000);
  }
});

function chooseImage() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['camera', 'album'],
    success: (res) => {
      imagePath.value = res.tempFilePaths[0];
      
      // #ifdef H5
      pathToBase64(res.tempFilePaths[0]).then(base64 => {
          imageBase64.value = base64;
      }).catch(err => {
          console.error('Base64 conversion failed', err);
          uni.showToast({ title: '图片处理失败', icon: 'none' });
      });
      // #endif

      // #ifndef H5
      uni.getFileSystemManager().readFile({
        filePath: res.tempFilePaths[0],
        encoding: 'base64',
        success: (r) => {
          imageBase64.value = 'data:image/jpeg;base64,' + r.data;
        },
        fail: (err) => {
          console.error('Read file failed', err);
          uni.showToast({ title: '图片读取失败', icon: 'none' });
        }
      });
      // #endif
    }
  });
}

// H5 环境下 Base64 转换助手
function pathToBase64(path) {
    return new Promise((resolve, reject) => {
        // 在 H5 中，tempFilePath 是一个 blob URL
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

async function uploadFace() {
  if (!imageBase64.value) {
    uni.showToast({ title: '图片处理中，请稍候...', icon: 'none' });
    return;
  }
  
  const userInfo = uni.getStorageSync('userInfo');
  const teamName = userInfo ? userInfo.teamName : '';
  
  let sessionId = '';
  const cookieStr = uni.getStorageSync('cookie');
  if (cookieStr && cookieStr.includes('JSESSIONID=')) {
    sessionId = cookieStr.split('JSESSIONID=')[1].split(';')[0];
  }

  try {
    uni.showLoading({ title: '正在录入...' });
    
    uni.request({
      url: `${baseUrl}/api/face/register`,
      method: 'POST',
      header: { 
        'Content-Type': 'application/json',
        'X-Session-Id': sessionId
      },
      withCredentials: true,
      data: {
        image: imageBase64.value,
        teamName: teamName
      },
      success: (res) => {
        uni.hideLoading();
        // 后端统一返回 ApiResponse: { success, message, data }
        if (res.statusCode === 200 && res.data && res.data.success) {
          uni.showToast({ title: '录入成功', icon: 'success' });
          setTimeout(() => uni.navigateBack(), 1500);
        } else {
          uni.showToast({ title: (res.data && res.data.message) || '录入失败', icon: 'none' });
        }
      },
      fail: () => {
        uni.hideLoading();
        uni.showToast({ title: '网络异常', icon: 'none' });
      }
    });
  } catch (e) {
    uni.hideLoading();
  }
}
</script>

<style lang="scss" scoped>
@import '../../uni.scss';

.header-bar {
  width: 100%;
  padding: 20rpx 0;
  display: flex;
  align-items: center;
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

.camera-box {
  width: 100%;
  height: 500rpx;
  background: rgba(255, 255, 255, 0.5);
  border: 4rpx dashed $primary;
  border-radius: 32rpx;
  margin: 40rpx 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
  
  .preview-img {
    width: 100%;
    height: 100%;
  }
  
  .placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    color: $primary;
    
    .icon { font-size: 80rpx; margin-bottom: 20rpx; }
    .text { font-size: 32rpx; font-weight: 600; }
  }
}
</style>
