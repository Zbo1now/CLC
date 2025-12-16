<template>
  <view class="container">
    <view class="header-bar">
      <view class="back-btn" @tap="goBack">←</view>
    </view>
    <view class="glass-card">
      <view class="title-area">
        <view class="main-title">刷脸登录</view>
        <view class="sub-title">CampusCoin Face ID</view>
      </view>

      <view class="camera-box" @tap="chooseImage">
        <image v-if="imagePath" :src="imagePath" class="preview-img" mode="aspectFill"></image>
        <view v-else class="placeholder">
          <text class="icon">👤</text>
          <text class="text">点击进行人脸识别</text>
        </view>
      </view>

      <button class="btn-submit" @tap="loginFace" :disabled="!imagePath">
        <text>✨</text> 立即验证
      </button>
      
      <view class="footer-hint" @tap="goBack">返回账号登录</view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { baseUrl } from '../../common/config.js';

const imagePath = ref('');
const imageBase64 = ref('');

function goBack() {
    uni.navigateBack();
}

function chooseImage() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['camera'],
    success: (res) => {
      imagePath.value = res.tempFilePaths[0];
      // #ifdef H5
      pathToBase64(res.tempFilePaths[0]).then(base64 => {
          imageBase64.value = base64;
          // 为了更好的体验，可以自动提交
          // loginFace(); 
      });
      // #endif
      
      // #ifndef H5
      uni.getFileSystemManager().readFile({
        filePath: res.tempFilePaths[0],
        encoding: 'base64',
        success: (r) => {
          imageBase64.value = 'data:image/jpeg;base64,' + r.data;
        }
      });
      // #endif
    }
  });
}

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

async function loginFace() {
  if (!imageBase64.value) return;
  
  try {
    uni.showLoading({ title: '识别中...' });
    
    uni.request({
      url: `${baseUrl}/api/face/login`,
      method: 'POST',
      header: { 'Content-Type': 'application/json' },
      data: {
        image: imageBase64.value
      },
      success: (res) => {
        uni.hideLoading();
        if (res.statusCode === 200 && res.data.success) {
          uni.showToast({ title: '识别成功', icon: 'success' });
          
          // 1. 标记登录状态
          uni.setStorageSync('userInfo', {
            teamName: res.data.data.teamName || 'User',
            loginTime: new Date().getTime()
          });

          // 如果返回了余额，则保存
          if (res.data.data && res.data.data.balance !== undefined) {
            uni.setStorageSync('teamBalance', res.data.data.balance);
          }
          
          setTimeout(() => {
            uni.reLaunch({ url: '/pages/home/home' });
          }, 1000);
        } else {
          uni.showToast({ title: res.data.message || '识别失败', icon: 'none' });
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
  font-size: 40rpx;
  color: #333;
  padding: 10rpx 20rpx;
}

.camera-box {
  width: 400rpx;
  height: 400rpx;
  background: rgba(255, 255, 255, 0.5);
  border: 4rpx solid $primary;
  border-radius: 50%; /* Circular for face ID look */
  margin: 60rpx auto;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
  box-shadow: 0 0 40rpx rgba(99, 102, 241, 0.2);
  
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
    .text { font-size: 28rpx; font-weight: 600; }
  }
}
</style>
