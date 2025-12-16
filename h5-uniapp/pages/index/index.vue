<template>
  <view class="container">
    <view class="glass-card">
      <view class="title-area">
        <view class="main-title">众创空间服务系统</view>
        <view class="sub-title">CampusCoin Service Platform</view>
      </view>

      <view class="tabs">
        <view :class="['tab-item', activeTab === 'login' ? 'active' : '']" @tap="switchTab('login')">登录</view>
        <view :class="['tab-item', activeTab === 'register' ? 'active' : '']" @tap="switchTab('register')">注册</view>
      </view>

      <view v-if="activeTab === 'login'" class="form-area">
        <view class="form-group">
          <view class="input-wrapper">
            <text class="input-icon">👥</text>
            <input class="input" v-model="loginForm.teamName" placeholder="请输入团队名称" />
          </view>
        </view>
        <view class="form-group">
          <view class="input-wrapper">
            <text class="input-icon">🔒</text>
            <input class="input" v-model="loginForm.password" placeholder="请输入密码" password confirm-type="go" @confirm="submitLogin" />
          </view>
        </view>
        <button class="btn-submit" @tap="submitLogin">
          <text>🚀</text> 立即登录
        </button>
        <view class="footer-hint">未注册？点击上方“注册”加入我们</view>
      </view>

      <view v-else class="form-area">
        <view class="form-group">
          <view class="input-wrapper">
            <text class="input-icon">👥</text>
            <input class="input" v-model="registerForm.teamName" placeholder="设置团队名称" />
          </view>
        </view>
        <view class="form-group">
          <view class="input-wrapper">
            <text class="input-icon">🔒</text>
            <input class="input" v-model="registerForm.password" placeholder="设置登录密码 (6位以上)" password />
          </view>
        </view>
        <view class="form-group">
          <view class="input-wrapper">
            <text class="input-icon">👤</text>
            <input class="input" v-model="registerForm.contactName" placeholder="负责人姓名" />
          </view>
        </view>
        <view class="form-group">
          <view class="input-wrapper">
            <text class="input-icon">📱</text>
            <input class="input" v-model="registerForm.contactPhone" placeholder="联系电话" />
          </view>
        </view>
        <button class="btn-submit" @tap="submitRegister">
          <text>✨</text> 注册并领取 500 币
        </button>
        <view class="footer-hint">注册即代表同意服务条款</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { baseUrl } from '../../common/config.js';

const activeTab = ref('login');
const loginForm = ref({ teamName: '', password: '' });
const registerForm = ref({ teamName: '', password: '', contactName: '', contactPhone: '' });

function switchTab(tab) {
  activeTab.value = tab;
}

function validatePassword(pwd) {
  return pwd && pwd.length >= 6;
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

async function submitLogin() {
  const { teamName, password } = loginForm.value;
  if (!teamName || !password) {
    uni.showToast({ title: '请填写完整信息', icon: 'none' });
    return;
  }
  try {
    uni.showLoading({ title: '登录中...' });
    const res = await apiPost('/api/auth/login', { teamName, password });
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
      uni.showToast({ title: data.message || '登录失败', icon: 'none' });
    }
  } catch (e) {
    uni.hideLoading();
    uni.showToast({ title: '网络异常', icon: 'none' });
  }
}

async function submitRegister() {
  const { teamName, password, contactName, contactPhone } = registerForm.value;
  if (!teamName || !contactName || !contactPhone) {
    uni.showToast({ title: '请填写完整信息', icon: 'none' });
    return;
  }
  if (!validatePassword(password)) {
    uni.showToast({ title: '密码至少6位', icon: 'none' });
    return;
  }
  try {
    uni.showLoading({ title: '注册中...' });
    const res = await apiPost('/api/auth/register', { teamName, password, contactName, contactPhone });
    uni.hideLoading();
    const data = res.data || {};
    if (res.statusCode === 201 && data.success) {
      uni.showToast({ title: '注册成功', icon: 'success' });
      activeTab.value = 'login';
      registerForm.value = { teamName: teamName, password: '', contactName, contactPhone };
    } else {
      uni.showToast({ title: data.message || '注册失败', icon: 'none' });
    }
  } catch (e) {
    uni.hideLoading();
    uni.showToast({ title: '网络异常', icon: 'none' });
  }
}
</script>

<style lang="scss" scoped>
@import '../../uni.scss';
</style>
