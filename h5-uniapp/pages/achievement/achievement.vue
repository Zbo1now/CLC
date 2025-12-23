/* 统一按钮文字字号 */
.btn-text {
  font-size: 32rpx;
  font-weight: 800;
}
<template>
  <view class="container">
    <view class="shell">
      <view class="header panel">
        <view class="back-btn" @tap="goBack">←</view>
        <text class="title">成果提交</text>
      </view>

      <view class="tabs glass-card panel">
        <view :class="['tab', activeTab === 'submit' ? 'active' : '']" @tap="switchTab('submit')">
          <text>提交成果</text>
        </view>
        <view :class="['tab', activeTab === 'mine' ? 'active' : '']" @tap="switchTab('mine')">
          <text>我的记录</text>
        </view>
      </view>

      <!-- 提交表单 -->
      <view v-if="activeTab === 'submit'" class="content panel">
        <view class="glass-card form-card">
          <view class="section-title">填写信息</view>

        <view class="row clickable" @tap="openCategoryPicker">
          <view class="row-left">
            <text class="row-label">成果类型</text>
          </view>
          <view class="row-right">
            <text :class="['row-value', categoryLabel === '请选择' ? 'muted' : '']">{{ categoryLabel }}</text>
            <text class="row-arrow">›</text>
          </view>
        </view>

        <view v-if="showSubType" class="row clickable" @tap="openSubTypePicker">
          <view class="row-left">
            <text class="row-label">细分类型</text>
          </view>
          <view class="row-right">
            <text :class="['row-value', subTypeLabel === '请选择' ? 'muted' : '']">{{ subTypeLabel }}</text>
            <text class="row-arrow">›</text>
          </view>
        </view>

        <view class="row">
          <view class="row-left">
            <text class="row-label">名称/标题</text>
          </view>
          <view class="row-right">
            <input class="row-input" v-model="title" placeholder="请输入成果名称或标题" />
          </view>
        </view>

        <view class="block">
          <view class="block-head">
            <text class="row-label">简要说明</text>
          </view>
          <view class="block-body">
            <textarea class="block-textarea" v-model="description" placeholder="简单描述成果内容、获奖情况或创新点" />
          </view>
        </view>

        <view class="row clickable" @tap="pickProof">
          <view class="row-left">
            <text class="row-label">证明材料</text>
          </view>
          <view class="row-right">
            <text :class="['row-value', 'proof-state', proofPreview ? '' : 'muted']">{{ proofPreview ? '已选择' : '未上传' }}</text>
            <text class="row-arrow">›</text>
          </view>
        </view>

        <view v-if="proofPreview" class="proof-preview">
          <template v-if="/\.(jpg|jpeg|png)$/i.test(proofPreview)">
            <image class="proof-img" :src="proofPreview" mode="aspectFill" />
          </template>
          <template v-else>
            <view class="file-preview">
              <text class="file-name">{{ proofFile?.name || proofPreview }}</text>
            </view>
          </template>
          <view class="proof-actions">
            <button class="ghost-btn" style="font-size:32rpx" @tap="pickProof">重新选择</button>
            <button class="danger-btn" style="font-size:32rpx" @tap="clearProof">移除</button>
          </view>
        </view>

        <button class="primary-btn" :disabled="submitting" @tap="submit">
          <text class="btn-text">{{ submitting ? '提交中...' : '提交审核' }}</text>
        </button>

        <text class="hint">* 提交后状态为“待审核”，管理员审核通过后自动发放虚拟币。</text>
        </view>
      </view>

      <!-- 我的记录 -->
      <view v-else class="content panel">
        <view class="glass-card list-card">
          <view class="list-header">
            <text class="list-title">审核进度与历史</text>
            <text class="more-link" @tap="refreshMine">刷新</text>
          </view>

          <view v-if="mine.length === 0" class="empty">
            <text class="empty-text">暂无提交记录</text>
            <text class="empty-sub">去“提交成果”里提交一条吧</text>
            <button class="primary-btn" @tap="switchTab('submit')">
              <text class="btn-text">去提交</text>
            </button>
          </view>

          <view v-else class="items">
            <view class="item" v-for="it in mine" :key="it.id">
              <view class="item-top">
                <view class="item-left">
                  <view class="item-main">
                    <text class="item-title">{{ it.title }}</text>
                    <text class="item-meta">{{ categoryText(it.category, it.subType) }} · {{ formatTime(it.createdAt) }}</text>
                  </view>
                </view>

                <view :class="['badge', badgeClass(it.status)]">
                  <text class="badge-dot"></text>
                  <text>{{ statusText(it.status) }}</text>
                </view>
              </view>

              <view class="item-bottom">
                <text v-if="it.status === 'APPROVED'" class="reward">+{{ it.rewardCoins }} 币</text>
                <text v-else-if="it.status === 'REJECTED'" class="reject">{{ it.rejectReason || '已驳回' }}</text>
                <text v-else class="pending">等待管理员审核</text>
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

const activeTab = ref('submit');
const submitting = ref(false);

const category = ref('PAPER');
const subType = ref('');
const title = ref('');
const description = ref('');

const proofPreview = ref('');
const proofBase64 = ref('');
const proofFile = ref(null); // 新增

const mine = ref([]);

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

const goBack = () => uni.navigateBack();

const categoryOptions = [
  { label: '学术论文', value: 'PAPER' },
  { label: '专利', value: 'PATENT' },
  { label: '竞赛获奖', value: 'COMPETITION' },
  { label: '其他创新成果', value: 'OTHER' }
];

const patentOptions = [
  { label: '受理', value: 'ACCEPTED' },
  { label: '授权', value: 'GRANTED' }
];

const competitionOptions = [
  { label: '校级', value: 'SCHOOL' },
  { label: '省级', value: 'PROVINCE' },
  { label: '国家级', value: 'NATIONAL' }
];

const showSubType = computed(() => category.value === 'PATENT' || category.value === 'COMPETITION');

const categoryLabel = computed(() => {
  const found = categoryOptions.find(x => x.value === category.value);
  return found ? found.label : '请选择';
});

const subTypeLabel = computed(() => {
  if (!showSubType.value) return '无需选择';
  const options = category.value === 'PATENT' ? patentOptions : competitionOptions;
  const found = options.find(x => x.value === subType.value);
  return found ? found.label : '请选择';
});

const switchTab = async (t) => {
  activeTab.value = t;
  if (t === 'mine') {
    await refreshMine();
  }
};

const openCategoryPicker = () => {
  uni.showActionSheet({
    itemList: categoryOptions.map(x => x.label),
    success: (res) => {
      const picked = categoryOptions[res.tapIndex];
      category.value = picked.value;
      subType.value = '';
    }
  });
};

const openSubTypePicker = () => {
  const options = category.value === 'PATENT' ? patentOptions : competitionOptions;
  uni.showActionSheet({
    itemList: options.map(x => x.label),
    success: (res) => {
      subType.value = options[res.tapIndex].value;
    }
  });
};

const pickProof = () => {
  uni.chooseFile({
    count: 1,
    type: 'all',
    extension: ['.jpg', '.jpeg', '.png', '.pdf', '.doc', '.docx', '.zip'],
    success: (res) => {
      const file = res.tempFiles[0];
      proofPreview.value = file.path || file.name;
      proofFile.value = file;
    },
    fail: () => {
      uni.showToast({ title: '文件选择失败', icon: 'none' });
    }
  });
};

const clearProof = () => {
  proofPreview.value = '';
  proofBase64.value = '';
};

const submit = () => {
  if (!category.value) {
    uni.showToast({ title: '请选择成果类型', icon: 'none' });
    return;
  }
  if (showSubType.value && !subType.value) {
    uni.showToast({ title: '请选择细分类型', icon: 'none' });
    return;
  }
  if (!title.value.trim()) {
    uni.showToast({ title: '请填写名称/标题', icon: 'none' });
    return;
  }
  if (!proofFile.value) {
    uni.showToast({ title: '请上传证明材料', icon: 'none' });
    return;
  }

  submitting.value = true;
  uni.vibrateShort?.();
  uni.showLoading({ title: '提交中...' });

  const sessionId = getSessionId();
  uni.uploadFile({
    url: `${baseUrl}/api/achievements`,
    filePath: proofFile.value.path || proofFile.value.name,
    name: 'proof',
    formData: {
      category: category.value,
      subType: subType.value || null,
      title: title.value,
      description: description.value
    },
    header: {
      'X-Session-Id': sessionId
    },
    success: async (res) => {
      uni.hideLoading();
      submitting.value = false;
      let data = res.data;
      if (typeof data === 'string') {
        try { data = JSON.parse(data); } catch {}
      }
      if (res.statusCode === 401) {
        handle401();
        return;
      }
      if (res.statusCode === 200 && data && data.success) {
        uni.showToast({ title: '提交成功', icon: 'success' });
        // reset
        title.value = '';
        description.value = '';
        clearProof();
        await switchTab('mine');
      } else {
        uni.showToast({ title: (data && data.message) || '提交失败', icon: 'none' });
      }
    },
    fail: () => {
      uni.hideLoading();
      submitting.value = false;
      uni.showToast({ title: '网络异常', icon: 'none' });
    }
  });
};

const refreshMine = () => {
  const sessionId = getSessionId();
  return new Promise((resolve) => {
    uni.request({
      url: `${baseUrl}/api/achievements/me`,
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
          mine.value = Array.isArray(res.data.data) ? res.data.data : [];
        } else {
          mine.value = [];
        }
        resolve();
      },
      fail: () => {
        mine.value = [];
        resolve();
      }
    });
  });
};

const formatTime = (v) => {
  if (!v) return '';
  const d = new Date(v);
  if (Number.isNaN(d.getTime())) return String(v).slice(0, 10);
  const now = new Date();
  const sameYear = d.getFullYear() === now.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return sameYear ? `${mm}-${dd}` : `${d.getFullYear()}-${mm}-${dd}`;
};

const statusText = (s) => {
  const v = (s || '').toUpperCase();
  if (v === 'APPROVED') return '已通过';
  if (v === 'REJECTED') return '已驳回';
  return '待审核';
};

const badgeClass = (s) => {
  const v = (s || '').toUpperCase();
  if (v === 'APPROVED') return 'badge-approved';
  if (v === 'REJECTED') return 'badge-rejected';
  return 'badge-pending';
};

const categoryIcon = (c) => {
  const v = (c || '').toUpperCase();
  if (v === 'PAPER') return '📄';
  if (v === 'PATENT') return '🧾';
  if (v === 'COMPETITION') return '🏆';
  return '💡';
};

const categoryText = (c, s) => {
  const v = (c || '').toUpperCase();
  const st = (s || '').toUpperCase();
  if (v === 'PAPER') return '学术论文';
  if (v === 'PATENT') return `专利${st ? (st === 'ACCEPTED' ? '（受理）' : '（授权）') : ''}`;
  if (v === 'COMPETITION') {
    const level = st === 'SCHOOL' ? '校级' : st === 'PROVINCE' ? '省级' : st === 'NATIONAL' ? '国家级' : '';
    return `竞赛获奖${level ? '（' + level + '）' : ''}`;
  }
  return '其他创新成果';
};

onShow(async () => {
  const userInfo = uni.getStorageSync('userInfo');
  if (!userInfo) return;
  if (activeTab.value === 'mine') {
    await refreshMine();
  }
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
  font-size: 30rpx;
}

.content {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.form-card {
  width: 100%;
  padding: 44rpx 34rpx;
}

.list-card {
  width: 100%;
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

.block-textarea {
  width: 100%;
  min-height: 160rpx;
  font-size: 28rpx;
  color: $text-main;
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

.file-preview {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 12rpx 16rpx;
  border-radius: 12rpx;
  background: rgba($bg-color, 0.9);
  margin-top: 10rpx;
}

.file-icon {
  font-size: 36rpx;
  color: $text-main;
}

.file-name {
  font-size: 28rpx;
  color: $text-main;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.hint {
  display: block;
  margin-top: 18rpx;
  font-size: 24rpx;
  color: $text-light;
  line-height: 1.4;
}

.list-card {
  padding: 44rpx 34rpx;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22rpx;
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
  font-size: 24rpx;
  color: $text-light;
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
</style>

/* proof-actions按钮专用文字样式 */
.proof-btn-text {
  font-size: 28rpx;
  font-weight: 800;
}

/* 小字体样式 */
.small-text {
  font-size: 22rpx !important;
}
