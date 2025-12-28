<template>
  <view class="container">
    <view class="glass-card">
      <view class="title-area">
        <view class="main-title">创联空间</view>
        <view class="sub-title">InnoLink Space</view>
      </view>

      <view class="tabs">
        <view :class="['tab-item', activeTab === 'login' ? 'active' : '']" @tap="switchTab('login')">登录</view>
        <view :class="['tab-item', activeTab === 'register' ? 'active' : '']" @tap="switchTab('register')">注册</view>
      </view>

      <view v-if="activeTab === 'login'" class="form-area">
        <view class="form-group">
          <view class="input-wrapper">
            <text class="input-icon">ID</text>
            <input class="input" v-model="loginForm.teamName" placeholder="请输入团队名称" />
          </view>
        </view>
        <view class="form-group">
          <view class="input-wrapper">
            <text class="input-icon">PW</text>
            <input class="input" v-model="loginForm.password" placeholder="请输入密码" password confirm-type="go" @confirm="submitLogin" />
          </view>
        </view>
        <view class="form-group">
          <view class="input-wrapper captcha-row">
            <text class="input-icon">VC</text>
            <input class="input" v-model="loginForm.captchaCode" placeholder="请输入验证码" maxlength="10" />
            <image class="captcha-img" :src="loginCaptchaImage" mode="aspectFit" @tap="refreshLoginCaptcha"></image>
            <view class="captcha-refresh" @tap="refreshLoginCaptcha">刷新</view>
          </view>
        </view>
        <button class="btn-submit" @tap="submitLogin">
          立即登录
        </button>
        <view class="footer-hint">未注册？点击上方“注册”加入我们</view>
      </view>

      <view v-else class="form-area">
        <view class="form-group">
          <view class="input-wrapper">
            <text class="input-icon">ID</text>
            <input class="input" v-model="registerForm.teamName" placeholder="设置团队名称" />
          </view>
        </view>
        <view class="form-group">
          <view class="input-wrapper">
            <text class="input-icon">PW</text>
            <input class="input" v-model="registerForm.password" placeholder="设置登录密码 (6位以上)" password />
          </view>
        </view>
        <view class="form-group">
          <view class="input-wrapper">
            <text class="input-icon">NM</text>
            <input class="input" v-model="registerForm.contactName" placeholder="负责人姓名" />
          </view>
        </view>
        <view class="form-group">
          <view class="input-wrapper">
            <text class="input-icon">PH</text>
            <input class="input" v-model="registerForm.contactPhone" placeholder="联系电话" />
          </view>
        </view>
        <view class="form-group">
          <view class="input-wrapper captcha-row">
            <text class="input-icon">VC</text>
            <input class="input" v-model="registerForm.captchaCode" placeholder="请输入验证码" maxlength="10" />
            <image class="captcha-img" :src="registerCaptchaImage" mode="aspectFit" @tap="refreshRegisterCaptcha"></image>
            <view class="captcha-refresh" @tap="refreshRegisterCaptcha">刷新</view>
          </view>
        </view>
        <button class="btn-submit" @tap="submitRegister">
          注册
        </button>
        <!-- 注册即代表同意服务条款 -->
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { baseUrl } from '../../common/config.js';

const activeTab = ref('login');
const loginForm = ref({ teamName: '', password: '', captchaId: '', captchaCode: '' });
const registerForm = ref({ teamName: '', password: '', contactName: '', contactPhone: '', captchaId: '', captchaCode: '' });

const loginCaptchaImage = ref('');
const registerCaptchaImage = ref('');

function switchTab(tab) {
  activeTab.value = tab;
  if (tab === 'login') {
    refreshLoginCaptcha();
  } else {
    refreshRegisterCaptcha();
  }
}

function validatePassword(pwd) {
  return pwd && pwd.length >= 6;
}

function fetchCaptcha() {
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${baseUrl}/api/auth/captcha`,
      method: 'GET',
      success: (res) => resolve(res),
      fail: (err) => reject(err)
    });
  });
}

async function refreshLoginCaptcha() {
  try {
    const res = await fetchCaptcha();
    const data = res?.data?.data || {};
    loginForm.value.captchaId = data.captchaId || '';
    loginForm.value.captchaCode = '';
    loginCaptchaImage.value = data.imageBase64 ? `data:image/png;base64,${data.imageBase64}` : '';
  } catch (e) {
    loginCaptchaImage.value = '';
  }
}

async function refreshRegisterCaptcha() {
  try {
    const res = await fetchCaptcha();
    const data = res?.data?.data || {};
    registerForm.value.captchaId = data.captchaId || '';
    registerForm.value.captchaCode = '';
    registerCaptchaImage.value = data.imageBase64 ? `data:image/png;base64,${data.imageBase64}` : '';
  } catch (e) {
    registerCaptchaImage.value = '';
  }
}

async function apiPost(path, payload) {
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${baseUrl}${path}`,
      method: 'POST',
      header: { 'Content-Type': 'application/json' },
      data: payload,
      success: (res) => resolve(res),
      fail: (err) => reject(err)
    });
  });
}

function showError(message) {
  const msg = String(message || '').trim() || '操作失败，请稍后重试';
  uni.showModal({
    title: '提示',
    content: msg,
    showCancel: false,
    confirmText: '知道了'
  });
}

async function submitLogin() {
  const { teamName, password, captchaId, captchaCode } = loginForm.value;
  if (!teamName || !password || !captchaId || !captchaCode) {
    showError('请填写完整信息');
    return;
  }
  try {
    uni.showLoading({ title: '登录中...' });
    const res = await apiPost('/api/auth/login', { teamName, password, captchaId, captchaCode });
    uni.hideLoading();
    const data = res.data || {};
    if (res.statusCode === 200 && data.success) {
      uni.showToast({ title: '登录成功', icon: 'success' });
      
      // 1. 标记登录状态：保存用户信息到本地
      // 浏览器会自动管理 Cookie，我们只需要一个前端标记
      uni.setStorageSync('userInfo', {
        teamName: teamName,
        loginTime: new Date().getTime()
      });

      // 手动保存 Session ID 到 Cookie，解决跨域/H5无法读取 Set-Cookie 的问题
      if (data.data && data.data.sessionId) {
        const cookieStr = 'JSESSIONID=' + data.data.sessionId;
        uni.setStorageSync('cookie', cookieStr);
      }

      // 保存余额信息
      if (data.data && data.data.balance !== undefined) {
        uni.setStorageSync('teamBalance', data.data.balance);
      }
      // 跳转到首页
      setTimeout(() => {
        uni.reLaunch({ url: '/pages/home/home' });
      }, 1000);
    } else {
      showError(data.message || '登录失败');
      refreshLoginCaptcha();
    }
  } catch (e) {
    uni.hideLoading();
    showError('网络异常');
    refreshLoginCaptcha();
  }
}

async function submitRegister() {
  const { teamName, password, contactName, contactPhone, captchaId, captchaCode } = registerForm.value;
  if (!teamName || !contactName || !contactPhone || !captchaId || !captchaCode) {
    showError('请填写完整信息');
    return;
  }
  if (!validatePassword(password)) {
    showError('密码至少6位');
    return;
  }
  try {
    uni.showLoading({ title: '注册中...' });
    const res = await apiPost('/api/auth/register', { teamName, password, contactName, contactPhone, captchaId, captchaCode });
    uni.hideLoading();
    const data = res.data || {};
    if (res.statusCode === 201 && data.success) {
      uni.showToast({ title: '注册成功', icon: 'success' });
      activeTab.value = 'login';
      registerForm.value = { teamName: teamName, password: '', contactName, contactPhone, captchaId: '', captchaCode: '' };
      refreshLoginCaptcha();
    } else {
      showError(data.message || '注册失败');
      refreshRegisterCaptcha();
    }
  } catch (e) {
    uni.hideLoading();
    showError('网络异常');
    refreshRegisterCaptcha();
  }
}

onMounted(() => {
  refreshLoginCaptcha();
});
</script>

<style lang="scss" scoped>
@import '../../uni.scss';

.container {
  padding: 52rpx 30rpx;
}

/* 登录/注册页单独做更紧凑的卡片宽度，避免横屏时“铺满”的观感 */
.glass-card {
  max-width: 620rpx;
  padding: 56rpx 38rpx;
}

.title-area {
  margin-bottom: 46rpx;
}

.tabs {
  max-width: 520rpx;
  margin-left: auto;
  margin-right: auto;
  margin-bottom: 42rpx;
}

.tab-item {
  font-size: 28rpx;
}

.form-group {
  margin-bottom: 26rpx;
}

.input-wrapper {
  gap: 18rpx;
  padding: 22rpx 26rpx;
  border-radius: 18rpx;
  background: rgba($bg-color, 0.9);
  border: 2rpx solid rgba(30, 41, 59, 0.04);
}

.input-wrapper:focus-within {
  background: rgba($white, 0.95);
  border-color: rgba($primary, 0.55);
  box-shadow: 0 0 0 6rpx rgba(99, 102, 241, 0.10);
}

.input-icon {
  width: 64rpx;
  margin-right: 0;
  font-size: 26rpx;
  font-weight: 800;
  color: rgba($text-light, 0.9);
  letter-spacing: 1rpx;
}

.btn-submit {
  margin-top: 34rpx;
  padding: 30rpx 0;
  font-weight: 900;
  box-shadow: 0 14rpx 30rpx rgba(99, 102, 241, 0.26);
}

.footer-hint {
  margin-top: 26rpx;
}

.captcha-row {
  align-items: center;
}

.captcha-img {
  width: 168rpx;
  height: 64rpx;
  margin-left: 8rpx;
  border-radius: 12rpx;
  background: rgba(255, 255, 255, 0.86);
  border: 2rpx solid rgba(30, 41, 59, 0.06);
}

.captcha-refresh {
  margin-left: 10rpx;
  padding: 10rpx 16rpx;
  border-radius: $radius-full;
  font-size: 24rpx;
  color: $primary;
  font-weight: 800;
  background: rgba($primary, 0.10);
}

/* 横屏/大屏适配：保持卡片不被拉得过宽 */
@media (orientation: landscape) {
  .glass-card {
    max-width: 560rpx;
    padding: 48rpx 34rpx;
  }
}
</style>
