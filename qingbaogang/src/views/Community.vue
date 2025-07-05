<template>
  <div class="community-container">
    <!-- 系统公告区域 - 添加到社区页面顶部 -->
    <div class="announcement-section" v-if="notices.length > 0">
      <div class="announcement-container">
        <div class="announcement-icon">
          <el-icon class="bell-icon"><Bell /></el-icon>
        </div>
        <div class="announcement-content">
          <el-carousel 
            height="36px" 
            direction="vertical" 
            :autoplay="true"
            :interval="4000"
            indicator-position="none"
            arrow="never">
            <el-carousel-item v-for="notice in notices" :key="notice.id" @click="viewNoticeDetail(notice)">
              <div class="announcement-item">
                {{ notice.title }}
              </div>
            </el-carousel-item>
          </el-carousel>
        </div>
        <div class="announcement-action">
          <el-button type="primary" size="small" @click="showAllNotices">更多公告</el-button>
        </div>
      </div>
    </div>
    
    <!-- 三栏布局容器 -->
    <div class="three-column-layout">
      <!-- 左侧栏 -->
      <div class="left-sidebar">
        <!-- AI心情小助手 -->
        <div class="sidebar-box mood-assistant">
          <div class="sidebar-title">
            <h3><i class="el-icon-magic-stick"></i> AI心情小助手</h3>
          </div>
          <div class="sidebar-content" v-if="randomTextResource">
            <div class="mood-quote">
              <p>{{ randomTextResource.content }}</p>
              <div class="mood-author" v-if="randomTextResource.author">—— {{ randomTextResource.author }}</div>
            </div>
            <div class="mood-refresh">
              <el-button type="primary" size="small" @click="refreshRandomText">
                <i class="el-icon-refresh-right"></i> 换一条
              </el-button>
            </div>
          </div>
          <div class="sidebar-content" v-else>
            <el-skeleton :rows="3" animated />
          </div>
        </div>

        <!-- 热门标签云 -->
        <div class="sidebar-box hot-tags">
          <div class="sidebar-title">
            <h3><i class="el-icon-collection-tag"></i> 热门标签</h3>
          </div>
          <div class="sidebar-content">
            <div class="tag-cloud" v-if="hotTags.length > 0">
              <el-tag
                v-for="tag in hotTags"
                :key="tag.name"
                :size="getTagSize(tag.count)"
                @click="searchByTag(tag.name)"
                class="hot-tag-item"
              >
                #{{ tag.name }} ({{ tag.count }})
              </el-tag>
            </div>
            <div v-else>
              <el-skeleton :rows="5" animated />
            </div>
          </div>
        </div>
      </div>

      <!-- 中间主内容 -->
      <div class="community-content">
        <div class="community-header">
          <div class="search-box">
            <input type="text" v-model="searchQuery" placeholder="搜索帖子/作者/标签..." @keyup.enter="searchPosts" />
            <button class="search-btn" @click="searchPosts">搜索</button>
            <button class="advanced-search-btn" @click="showAdvancedSearch = !showAdvancedSearch">
              {{ showAdvancedSearch ? '收起' : '高级搜索' }}
            </button>
          </div>
          <button class="publish-btn" @click="showPostForm = true">
            <el-icon><Plus /></el-icon>
            发布帖子
          </button>
        </div>

    <!-- 分类和筛选 -->
    <div class="filter-container">
      <div class="categories">
        <span 
          v-for="category in categories" 
          :key="category.value" 
          :class="['category-item', { active: activeCategory === category.value }]"
          @click="setCategory(category.value)"
        >
          {{ category.label }}
        </span>
      </div>
      <div class="sort-options">
        <span>排序:</span>
        <select v-model="sortBy" @change="fetchPosts" title="排序字段">
          <option value="create_time">最新发布</option>
          <option value="like_count">点赞数</option>
          <option value="comment_count">评论数</option>
        </select>
        <select v-model="sortOrder" @change="fetchPosts" title="排序方式">
          <option value="DESC">降序</option>
          <option value="ASC">升序</option>
        </select>
      </div>
    </div>

    <!-- 帖子列表 -->
    <div class="post-list">
      <div v-for="post in posts" :key="post.id" class="post-card">
        <div class="post-header">
          <h3 class="post-title" @click="goToPostDetail(post.id)">{{ post.title }}</h3>
          <div class="post-meta">
            <span class="author">{{ post.username || post.userName || '匿名' }}</span>
            <span class="date">{{ formatDate(post.createTime) }}</span>
          </div>
        </div>
        <div class="post-content" @click="goToPostDetail(post.id)">
          {{ post.content ? (post.content.length > 120 ? post.content.substring(0, 120) + '...' : post.content) : '无内容' }}
        </div>
        <!-- 帖子图片展示 -->
        <div v-if="post.images" class="post-images">
          <div class="images-grid" :class="'grid-' + Math.min(getImagesArray(post.images).length, 4)">
            <div 
              v-for="(image, index) in getImagesArray(post.images).slice(0, 4)" 
              :key="index" 
              class="image-item"
              @click.stop="showFullImage(image)"
            >
              <img 
                :src="getImageUrl(image, index)" 
                :alt="`${post.title}图片${index + 1}`" 
                class="post-image" 
                @error="handleImageError($event, post, index)" 
              />
              <!-- 如果有更多图片，显示+N -->
              <div v-if="index === 3 && getImagesArray(post.images).length > 4" class="more-images-overlay">
                <span>+{{ getImagesArray(post.images).length - 4 }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="post-tags">
          <template v-if="post.tags && typeof post.tags === 'string'">
            <span v-for="tag in post.tags.split(',')" :key="tag" class="tag">
              #{{ tag.trim() }}
            </span>
          </template>
          <template v-else-if="post.tags && Array.isArray(post.tags)">
            <span v-for="tag in post.tags" :key="tag" class="tag">
              #{{ typeof tag === 'string' ? tag.trim() : tag }}
            </span>
          </template>
        </div>
        <div class="post-actions">
          <div class="action-item" @click="toggleLike(post)" :class="{ 'active': post.liked }">
            <i :class="['icon', post.liked ? 'icon-liked' : 'icon-like']">{{ post.liked ? '❤️' : '🤍' }}</i>
            <span>{{ post.likeCount || 0 }}</span>
          </div>
          <div class="action-item" @click="goToPostDetail(post.id)">
            <i class="icon icon-comment">💬</i>
            <span>{{ post.commentCount || 0 }}</span>
          </div>
          <div class="action-item" @click="toggleFavorite(post)" :class="{ 'active': post.favorited }">
            <i :class="['icon', post.favorited ? 'icon-favorited' : 'icon-favorite']">{{ post.favorited ? '⭐' : '☆' }}</i>
          </div>
          <div v-if="isMyPost(post)" class="action-item delete-item" @click="deleteMyPost(post)">
            <i class="icon icon-delete">🗑️</i>
            <span>删除</span>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="posts.length === 0" class="empty-state">
        <p>暂无帖子</p>
        <button @click="showPostForm = true" class="empty-btn">发布第一篇帖子</button>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination">
      <button :disabled="currentPage === 1" @click="prevPage" class="page-btn">上一页</button>
      <span class="page-indicator">第 {{ currentPage }} 页</span>
      <button :disabled="!hasMorePages" @click="nextPage" class="page-btn">下一页</button>
    </div>
  </div>
      
  <!-- 右侧栏 -->
  <div class="right-sidebar">
    <!-- 本周热门帖子 -->
    <div class="sidebar-box hot-posts">
      <div class="sidebar-title">
        <h3><i class="el-icon-trophy"></i> 本周热门</h3>
      </div>
      <div class="sidebar-content">
                  <div class="hot-post-list" v-if="hotPosts.length > 0">
              <div v-for="post in hotPosts" :key="post.id" class="hot-post-item" @click="goToPostDetail(post.id)">
                <div class="hot-post-title">{{ post.title }}</div>
                <div class="hot-post-stats">
                  <span class="hot-post-likes"><i class="el-icon-star-on" style="color:#ff6b81;"></i> {{ post.likeCount || 0 }}</span>
                  <span class="hot-post-comments"><i class="el-icon-chat-dot-round" style="color:#3498db;"></i> {{ post.commentCount || 0 }}</span>
                </div>
              </div>
            </div>
        <div v-else>
          <el-skeleton :rows="5" animated />
        </div>
      </div>
    </div>
  </div>
</div>

    <!-- 发布帖子弹窗 -->
    <el-dialog
      v-model="showPostForm"
      title="发布帖子"
      width="650px"
      :close-on-click-modal="false"
      custom-class="post-dialog"
      :before-close="() => !isSubmitting && (showPostForm = false)"
    >
      <div class="post-form-container">
        <div class="form-header">
          <div class="form-avatar">
            <el-avatar :size="40" :src="userAvatar"></el-avatar>
          </div>
          <div class="form-user-info">
            <div class="username">{{ userStore.name }}</div>
            <div class="post-visibility">公开发布</div>
          </div>
        </div>
        
        <div class="form-body">
          <!-- 标题输入 -->
          <div class="form-group">
            <el-input
              v-model="newPost.title"
              placeholder="请输入标题（5-50字）"
              maxlength="50"
              show-word-limit
              class="title-input"
            ></el-input>
          </div>
          
          <!-- 分类选择 -->
          <div class="form-group category-select">
            <el-select
              v-model="newPost.category"
              placeholder="请选择分类"
              class="category-dropdown"
            >
              <el-option
                v-for="item in categories.filter(c => c.value !== 'all')"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              ></el-option>
            </el-select>
          </div>
          
          <!-- 内容编辑器 -->
          <div class="form-group">
            <el-input
              v-model="newPost.content"
              type="textarea"
              :rows="6"
              placeholder="分享你的故事、经验或问题..."
              maxlength="2000"
              show-word-limit
              class="content-textarea"
            ></el-input>
          </div>
          
          <!-- 标签输入 -->
          <div class="form-group">
            <div class="tags-input-wrapper">
              <el-input
                v-model="newPost.tags"
                placeholder="添加标签，用逗号分隔（最多5个）"
                maxlength="50"
                class="tags-input"
              >
                <template #prefix>
                  <el-icon class="tag-icon"><CollectionTag /></el-icon>
                </template>
              </el-input>
              
              <!-- 标签预览 -->
              <div v-if="newPost.tags" class="tags-preview">
                <el-tag
                  v-for="tag in newPost.tags.split(',').map(t => t.trim()).filter(t => t)"
                  :key="tag"
                  size="small"
                  class="preview-tag"
                >
                  #{{ tag }}
                </el-tag>
              </div>
            </div>
          </div>
          
          <!-- 图片上传区域 -->
          <div class="form-group">
            <div class="image-upload-section">
              <div class="upload-header">
                <span class="upload-title">
                  <el-icon><Picture /></el-icon> 添加图片
                </span>
                <span class="upload-count">{{ uploadedImages.length }}/{{ maxImageCount }}</span>
              </div>
              
              <!-- 图片预览区域 -->
              <div class="image-preview-grid" v-if="uploadedImages.length > 0">
                <div v-for="(image, index) in uploadedImages" :key="index" class="preview-item">
                  <img :src="getImageUrl(image, index)" class="preview-image" />
                  <div class="preview-actions">
                    <el-button
                      type="danger"
                      circle
                      size="small"
                      icon="Delete"
                      @click="uploadedImages.splice(index, 1)"
                      class="delete-image-btn"
                    ></el-button>
                  </div>
                </div>
              </div>
              
              <!-- 上传按钮 -->
              <div class="upload-actions" v-if="uploadedImages.length < maxImageCount">
                <input
                  type="file"
                  accept="image/*"
                  multiple
                  ref="fileInput"
                  @change="handleFileUpload"
                  style="display: none"
                />
                <el-button
                  type="primary"
                  @click="$refs.fileInput.click()"
                  :disabled="isSubmitting || uploadedImages.length >= maxImageCount"
                  class="upload-button"
                >
                  <el-icon><Upload /></el-icon>
                  <span>上传图片</span>
                </el-button>
                <div class="upload-tips">
                  支持 JPG、PNG、GIF 格式，单张图片不超过10MB
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showPostForm = false" :disabled="isSubmitting">取消</el-button>
          <el-button
            type="primary"
            @click="createNewPost"
            :loading="isSubmitting"
            :disabled="!newPost.title || !newPost.content || !newPost.category"
          >
            发布
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 高级搜索面板 -->
    <div v-if="showAdvancedSearch" class="advanced-search-panel">
      <div class="advanced-search-form">
        <div class="advanced-search-header">
          <h3>高级搜索</h3>
          <el-button type="text" @click="showAdvancedSearch = false" class="close-search-btn">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        
        <div class="form-group">
          <label>关键词</label>
          <div class="input-with-icon">
            <el-icon><Search /></el-icon>
            <input 
              type="text" 
              v-model="advancedSearch.keyword" 
              placeholder="搜索标题、内容和标签" 
              class="styled-input"
            />
          </div>
        </div>
        
        <div class="form-group">
          <label>分类</label>
          <div class="select-wrapper">
            <select 
              v-model="advancedSearch.category" 
              title="筛选分类" 
              class="styled-select"
            >
              <option value="">全部分类</option>
              <option v-for="category in categories.slice(1)" :key="category.value" :value="category.value">
                {{ category.label }}
              </option>
            </select>
            <div class="select-arrow">
              <el-icon><ArrowDown /></el-icon>
            </div>
          </div>
        </div>
        
        <div class="form-group sort-group">
          <label>排序方式</label>
          <div class="sort-selects">
            <div class="select-wrapper half-width">
              <select 
                v-model="advancedSearch.sortBy" 
                title="高级搜索排序字段" 
                class="styled-select"
              >
                <option value="create_time">发布时间</option>
                <option value="like_count">点赞数</option>
                <option value="comment_count">评论数</option>
              </select>
              <div class="select-arrow">
                <el-icon><Sort /></el-icon>
              </div>
            </div>
            
            <div class="select-wrapper half-width">
              <select 
                v-model="advancedSearch.sortOrder" 
                title="高级搜索排序方式" 
                class="styled-select"
              >
                <option value="DESC">降序</option>
                <option value="ASC">升序</option>
              </select>
              <div class="select-arrow">
                <el-icon><Sort /></el-icon>
              </div>
            </div>
          </div>
        </div>
        
        <div class="search-actions">
          <el-button @click="showAdvancedSearch = false" class="cancel-search-btn">取消</el-button>
          <el-button type="primary" @click="applyAdvancedSearch" class="apply-search-btn">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </div>
      </div>
    </div>

    <!-- 图片查看弹窗 -->
    <div v-if="showImageViewer" class="image-viewer-overlay">
      <div class="image-viewer-content">
        <img 
          :src="currentViewingImage" 
          alt="图片查看" 
        />
        <button @click="showImageViewer = false" class="close-btn">关闭</button>
      </div>
    </div>

    <!-- 系统公告详情弹窗 -->
    <el-dialog v-model="showNoticeDetail" title="公告详情" width="650px" destroy-on-close center>
      <div class="announcement-detail" v-if="currentNotice">
        <div class="announcement-detail-header">
          <h2>{{ currentNotice.title }}</h2>
                      <div class="announcement-detail-meta">
              <el-tag size="small" effect="plain" type="info">系统公告</el-tag>
              <span>发布时间: {{ formatDateTime(currentNotice.createTime) }}</span>
              <span v-if="currentNotice.updateTime && currentNotice.updateTime !== currentNotice.createTime">
                更新时间: {{ formatDateTime(currentNotice.updateTime) }}
              </span>
            </div>
        </div>
        <div class="announcement-detail-divider"></div>
        <div class="announcement-detail-content">
          <div v-if="currentNotice.content" style="white-space: pre-line;">{{ currentNotice.content }}</div>
          <el-empty v-else description="暂无详细内容"></el-empty>
        </div>
      </div>
    </el-dialog>

    <!-- 所有系统公告弹窗 -->
    <el-dialog v-model="showAllNoticesDialog" title="系统公告" width="800px" @open="fetchNotices" destroy-on-close>
      <div v-loading="noticesLoading" element-loading-text="加载中...">
        <el-empty v-if="allNotices.length === 0" description="暂无公告"></el-empty>
        
        <div class="announcement-list" v-else>
          <div 
            v-for="notice in allNotices" 
            :key="notice.id" 
            class="announcement-card"
            @click="viewNoticeDetail(notice)"
          >
            <div class="announcement-card-header">
              <el-icon><Document /></el-icon>
              <h3>{{ notice.title }}</h3>
            </div>
            <div class="announcement-card-content">
              <p>{{ truncateContent(notice.content, 80) }}</p>
            </div>
            <div class="announcement-card-footer">
              <span class="announcement-card-time">
                <el-icon><Clock /></el-icon>
                {{ formatDateTime(notice.createTime) }}
              </span>
              <el-button type="primary" size="small" text>查看详情</el-button>
            </div>
          </div>
        </div>
        
        <div class="pagination-container" v-if="allNotices.length > 0 && noticeTotal > noticePageSize">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="noticeTotal"
            :current-page="noticePage"
            :page-size="noticePageSize"
            @current-change="noticePageChange"
          ></el-pagination>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { request } from '@/utils/request'
import { 
  getPostList, 
  createPost, 
  likePost, 
  unlikePost, 
  isPostLiked, 
  getPostLikeCount,
  favoritePost,
  unfavoritePost,
  isPostFavorited,
  searchPosts as apiSearchPosts,
  getPostsByCategory,
  advancedSearchPosts,
  // 导入公告相关API
  getRecentNotices,
  getSystemNotices,
  getNoticeDetail,
  testNoticeApi,
  getServerTime,
  // 导入新增功能API
  getHotTags,
  getHotPostsOfWeek,
  getTextResources
} from '@/api/communityApi'
// 导入Element Plus图标
import { Bell, Document, Clock, Picture, Upload, CollectionTag, Plus, Close, Search, ArrowDown, Sort } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

// 响应式状态
const posts = ref([])
const currentPage = ref(1)
const pageSize = ref(5)
const hasMorePages = ref(true)
const searchQuery = ref('')
const showPostForm = ref(false)
const activeCategory = ref('all')
const sortBy = ref('create_time')
const sortOrder = ref('DESC')
const isLoading = ref(false)
const apiError = ref(null)
const searchMode = ref('basic') // 'basic', 'keyword', 'category', 'advanced'
const showAdvancedSearch = ref(false)
const advancedSearch = reactive({
  keyword: '',
  category: '',
  sortBy: 'create_time',
  sortOrder: 'DESC'
})
// 图片上传相关
const uploadedImages = ref([])
const maxImageCount = 6 // 最大上传图片数量
const isSubmitting = ref(false) // 提交状态
// 图片查看
const currentViewingImage = ref('')
const showImageViewer = ref(false)
const mockPosts = ref([
  {
    id: 'mock1',
    title: '欢迎来到心理健康社区',
    content: '这是一个模拟帖子，当API未正常工作时会显示。在这里，我们可以分享心理健康相关的话题和经验。',
    createTime: new Date().toISOString(),
    updateTime: new Date().toISOString(),
    userId: 1,
    username: '系统管理员',
    avatar: '/src/assets/default-avatar.png',
    tags: '心理健康,社区',
    category: '公告',
    likeCount: 15,
    commentCount: 5
  },
  {
    id: 'mock2',
    title: '如何缓解学习压力',
    content: '分享一些缓解学习压力的小技巧...',
    createTime: new Date(Date.now() - 86400000).toISOString(),
    updateTime: new Date(Date.now() - 86400000).toISOString(),
    userId: 2,
    username: '心理咨询师',
    avatar: '/src/assets/default-avatar.png',
    tags: '压力管理,学习方法',
    category: '学习',
    likeCount: 8,
    commentCount: 3
  }
])
const useMockData = ref(false)

// 用户信息
const userAvatar = computed(() => userStore.avatar || '/src/assets/default-avatar.png')

// 发帖表单
const newPost = reactive({
  title: '',
  content: '',
  tags: '',
  category: ''
})

// 分类
const categories = [
  { label: '全部', value: 'all' },
  { label: '心理健康', value: '心理健康' },
  { label: '学习方法', value: '学习方法' },
  { label: '情感', value: '情感' },
  { label: '压力管理', value: '压力管理' },
  { label: '社交', value: '社交' },
  { label: '职业发展', value: '职业发展' },
  { label: '健康', value: '健康' },
  { label: '生活', value: '生活' },
  { label: '其他', value: '其他' }
]

// 公告相关状态
const notices = ref([])
const showNoticeDetail = ref(false)
const currentNotice = ref(null)
const showAllNoticesDialog = ref(false)
const allNotices = ref([])
const noticeTotal = ref(0)
const noticePage = ref(1)
const noticePageSize = ref(10)
const noticesLoading = ref(false)

// 新增功能相关状态
const hotTags = ref([])
const hotPosts = ref([])
const textResources = ref([])
const randomTextResource = ref(null)
const tagsLoading = ref(false)
const hotPostsLoading = ref(false)
const textResourcesLoading = ref(false)

// 获取帖子列表
const fetchPosts = async () => {
  console.log('开始获取帖子列表，参数:', {
    current: currentPage.value,
    size: pageSize.value,
    category: activeCategory.value,
    keyword: searchQuery.value,
    sortBy: sortBy.value,
    sortOrder: sortOrder.value,
    searchMode: searchMode.value
  })
  
  isLoading.value = true
  apiError.value = null
  
  try {
    // 构建基础参数
    const params = {
      current: currentPage.value,
      size: pageSize.value
    }
    
    let response
    
    // 根据搜索模式选择不同的API
    switch (searchMode.value) {
      case 'keyword':
        // 关键词搜索
        console.log('执行关键词搜索，关键词:', searchQuery.value);
        params.keyword = searchQuery.value
        console.log('准备调用搜索API，参数:', params);
        try {
          response = await apiSearchPosts(params)
          console.log('搜索API响应:', response);
        } catch (error) {
          console.error('搜索API调用失败:', error);
        }
        break
        
      case 'category':
        // 分类筛选
        response = await getPostsByCategory(activeCategory.value, params)
        break
        
      case 'advanced':
        // 高级搜索
        params.keyword = searchQuery.value
        params.category = activeCategory.value !== 'all' ? activeCategory.value : undefined
        params.sortBy = sortBy.value
        params.sortOrder = sortOrder.value
        response = await advancedSearchPosts(params)
        break
        
      default:
        // 基础搜索
        if (activeCategory.value && activeCategory.value !== 'all') {
          params.category = activeCategory.value
        }
        if (searchQuery.value) {
          params.keyword = searchQuery.value
        }
        params.sortBy = sortBy.value
        params.sortOrder = sortOrder.value
        response = await getPostList(params)
    }
    
    if (response && response.data) {
      console.log('获取帖子列表成功:', response.data)
      
      let postList = []
      
      // 处理返回的分页对象
      if (response.data.records) {
        postList = response.data.records.map(post => {
          // 确保tags格式一致
          if (post.tags && Array.isArray(post.tags)) {
            post.tags = post.tags.join(',');
          }
          return post;
        });
        hasMorePages.value = currentPage.value * pageSize.value < response.data.total
      } else if (Array.isArray(response.data)) {
        postList = response.data.map(post => {
          // 确保tags格式一致
          if (post.tags && Array.isArray(post.tags)) {
            post.tags = post.tags.join(',');
          }
          return post;
        });
        hasMorePages.value = response.data.length >= pageSize.value
      } else {
        console.warn('未知的响应数据格式:', response.data)
        useMockData.value = true
      }
      
      // 如果获取到了帖子列表，给每个帖子获取点赞和收藏状态
      if (postList.length > 0 && !useMockData.value) {
        const userId = userStore.userId
        
        // 打印第一个帖子的数据结构
        if (postList[0]) {
          console.log('帖子列表中的单个帖子数据结构:', postList[0])
        }
        
        // 使用 Promise.all 并行获取所有帖子的点赞和收藏状态
        const postsWithStatus = await Promise.all(
          postList.map(async (post) => {
            try {
              let liked = false
              let likeCount = post.likeCount || 0
              let favorited = false
              
              // 尝试从本地存储获取点赞和收藏状态
              const likeKey = `post_like_${post.id}_${userId}`
              const favoriteKey = `post_favorite_${post.id}_${userId}`
              const likeData = localStorage.getItem(likeKey)
              const favoriteData = localStorage.getItem(favoriteKey)
              
              // 如果本地存储有数据，先使用本地数据
              if (likeData) {
                try {
                  const parsedLike = JSON.parse(likeData)
                  if (parsedLike && parsedLike.timestamp) {
                    // 如果缓存时间不超过1小时，使用本地数据
                    if (Date.now() - parsedLike.timestamp < 3600000) {
                      liked = parsedLike.liked
                      console.log(`从本地缓存获取帖子${post.id}的点赞状态:`, liked)
                    }
                  }
                } catch (e) {
                  console.error('解析本地点赞数据失败:', e)
                }
              }
              
              if (favoriteData) {
                try {
                  const parsedFavorite = JSON.parse(favoriteData)
                  if (parsedFavorite && parsedFavorite.timestamp) {
                    // 如果缓存时间不超过1小时，使用本地数据
                    if (Date.now() - parsedFavorite.timestamp < 3600000) {
                      favorited = parsedFavorite.favorited
                      console.log(`从本地缓存获取帖子${post.id}的收藏状态:`, favorited)
                    }
                  }
                } catch (e) {
                  console.error('解析本地收藏数据失败:', e)
                }
              }
              
              // 无论是否有本地缓存，都尝试从服务器获取最新状态
              const results = await Promise.allSettled([
                isPostLiked(post.id, userId),
                getPostLikeCount(post.id),
                isPostFavorited(post.id)
              ])
              
              // 处理每个结果
              const likedResult = results[0].status === 'fulfilled' ? results[0].value : null
              const likeCountResult = results[1].status === 'fulfilled' ? results[1].value : null
              const favoritedResult = results[2].status === 'fulfilled' ? results[2].value : null
              
              // 如果服务器请求成功，使用服务器数据并更新本地缓存
              if (likedResult) {
                liked = likedResult.data || false
                localStorage.setItem(likeKey, JSON.stringify({
                  liked,
                  timestamp: Date.now()
                }))
              }
              
              if (likeCountResult) {
                likeCount = likeCountResult.data || 0
              }
              
              if (favoritedResult) {
                favorited = favoritedResult.data || false
                localStorage.setItem(favoriteKey, JSON.stringify({
                  favorited,
                  timestamp: Date.now()
                }))
              }
              
              // 更新帖子数据
              return {
                ...post,
                liked,
                likeCount,
                favorited
              }
            } catch (err) {
              console.warn(`获取帖子 ${post.id} 的状态失败:`, err)
              // 如果获取失败，使用默认值
              return {
                ...post,
                liked: false,
                likeCount: post.likeCount || 0,
                favorited: false
              }
            }
          })
        )
        
        posts.value = postsWithStatus
      } else {
        posts.value = postList
      }
    } else {
      console.warn('响应数据为空')
      useMockData.value = true
    }
  } catch (error) {
    console.error('获取帖子列表失败:', error)
    apiError.value = error
    useMockData.value = true
  } finally {
    isLoading.value = false
    
    // 如果需要使用模拟数据
    if (useMockData.value) {
      console.log('使用模拟数据')
      posts.value = mockPosts.value
      hasMorePages.value = false
    }
  }
}

// 创建新帖子
const createNewPost = async () => {
  if (!newPost.title || !newPost.content || !newPost.category) {
    alert('标题、内容和分类不能为空')
    return
  }
  isSubmitting.value = true
  try {
    console.log('开始创建新帖子...')
    console.log('原始图片列表:', uploadedImages.value)
    
    // 处理图片URL，确保格式一致，并过滤掉无效的URL
    const formattedImageUrls = uploadedImages.value
      .filter(url => {
        if (!url || url.trim() === '') {
          console.warn(`过滤掉空URL: ${url}`)
          return false;
        }
        
        // 过滤只有路径没有文件名的URL
        if (url.trim() === '/static/upload/' || url.trim().endsWith('/')) {
          console.warn(`过滤掉无效的图片URL(只有路径): ${url}`)
          return false
        }
        
        // 提取文件名
        const parts = url.split('/')
        const filename = parts[parts.length - 1]
        
        // 检查文件名是否有效
        if (!filename || filename.trim() === '') {
          console.warn(`上传的图片URL没有有效的文件名，将被过滤: ${url}`)
          return false
        }
        
        return true
      })
      .map(url => {
        // 统一URL格式为 /static/upload/filename
        const parts = url.split('/')
        const filename = parts[parts.length - 1]
        
        if (!filename || filename.trim() === '') {
          console.warn(`跳过无效的图片URL(无文件名): ${url}`)
          return null; // 将被过滤
        }
        
        // 提取文件名并返回标准格式
        console.log(`标准化图片URL: ${url} -> /static/upload/${filename}`)
        return `/static/upload/${filename}`
      })
      .filter(url => url !== null); // 过滤掉处理中发现的无效URL
    
    console.log('格式化后的图片URL:', formattedImageUrls)
    
    // 检查是否有有效的图片URL
    if (formattedImageUrls.some(url => url === '/static/upload/' || url.endsWith('/'))) {
      console.error('发现无效的图片URL:', formattedImageUrls)
      alert('发现无效的图片URL，请重新上传图片')
      return;
    }
    
    // 构造帖子数据
    const postData = {
      title: newPost.title,
      content: newPost.content,
      tags: newPost.tags,
      userId: userStore.userId || '1',
      username: userStore.username || '用户' + (userStore.userId || '1'),
      category: newPost.category,
      images: formattedImageUrls.length > 0 ? formattedImageUrls.join(',') : '' // 使用格式化后的图片URL，如果没有图片则设为空字符串
    }
    console.log('发帖数据:', postData)
    
    const response = await createPost(postData)
    console.log('发帖成功，服务器返回:', response)
    
    // 重置表单
    newPost.title = ''
    newPost.content = ''
    newPost.tags = ''
    newPost.category = ''
    uploadedImages.value = []
    showPostForm.value = false
    alert('发帖成功！')
    
    // 刷新帖子列表
    currentPage.value = 1
    setTimeout(() => {
      fetchPosts()
    }, 1000)
  } catch (error) {
    console.error('发帖失败:', error)
    alert('发帖失败，请稍后重试: ' + (error.message || '未知错误'))
  } finally {
    isSubmitting.value = false
  }
}

// 点赞/取消点赞
const toggleLike = async (post) => {
  // 如果未登录，提示用户
  if (!userStore.isLoggedIn) {
    alert('请先登录后再点赞')
    router.push('/login')
    return
  }

  const userId = userStore.userId
  if (!userId) {
    console.error('未找到用户ID')
    return
  }
  
  try {
    // 确保postId是有效的
    if (!post.id) {
      console.error('无效的帖子ID', post)
      throw new Error('无效的帖子数据')
    }
    
    // 添加调试日志
    console.log(`处理点赞操作：帖子ID=${post.id}(${typeof post.id})，帖子标题="${post.title}"，用户ID=${userId}, 当前点赞状态=${post.liked}`)
    
    // 先在UI上立即反馈，再发送请求
    const originalStatus = post.liked
    const originalCount = post.likeCount || 0
    
    // 更新UI状态
    post.liked = !originalStatus
    post.likeCount = originalStatus ? (originalCount - 1) : (originalCount + 1)
    
    // 发送API请求
    let response
    if (originalStatus) {
      // 取消点赞
      console.log(`准备发送取消点赞请求，帖子ID=${post.id}，用户ID=${userId}`)
      response = await unlikePost(post.id, userId)
      console.log('取消点赞响应:', response)
      
      if (response && response.code === 200) {
        console.log('取消点赞成功')
      } else if (response && response.msg && (response.msg.includes('未点赞') || response.msg.includes('没有点赞'))) {
        console.warn('该帖子未被点赞，无需取消')
        // 不需要恢复UI状态
      } else {
        throw new Error(response?.msg || '取消点赞失败')
      }
    } else {
      // 点赞
      console.log(`准备发送点赞请求，帖子ID=${post.id}，用户ID=${userId}`)
      response = await likePost(post.id, userId)
      console.log('点赞响应:', response)
      
      if (response && response.code === 200) {
        console.log('点赞成功')
      } else if (response && response.msg && response.msg.includes('已经点赞')) {
        console.warn('已经点赞过该帖子')
        // 不需要恢复UI状态，因为帖子实际上已经是点赞状态
      } else {
        console.error('点赞请求异常响应:', response)
        throw new Error(response?.msg || '点赞失败')
      }
    }
    
    // 延时 1 秒后再次检查点赞状态，确认数据库状态
    setTimeout(async () => {
      try {
        const checkResponse = await isPostLiked(post.id, userId)
        console.log(`点赞操作后状态检查：帖子ID=${post.id}，数据库中的点赞状态=${checkResponse.data}`)
        
        // 如果状态不一致，需要向用户提示
        if (checkResponse.data !== post.liked) {
          console.warn('点赞状态与数据库不一致，可能需要刷新页面')
        }
      } catch (err) {
        console.error('检查点赞状态失败:', err)
      }
    }, 1000)
    
  } catch (error) {
    // 如果API请求失败，恢复UI状态
    console.error('点赞操作失败:', error)
    post.liked = !post.liked
    post.likeCount = post.liked ? (post.likeCount + 1) : (post.likeCount - 1)
    
    // 显示错误信息
    alert(error.message || '操作失败，请稍后重试')
  }
}

// 收藏/取消收藏
const toggleFavorite = async (post) => {
  // 检查用户是否登录
  if (!userStore.isLoggedIn) {
    alert('请先登录后再收藏')
    router.push('/login')
    return
  }
  
  const userId = userStore.userId
  if (!userId) {
    console.error('未找到用户ID')
    return
  }
  
  try {
    // 确保帖子ID有效
    if (!post.id) {
      throw new Error('无效的帖子数据')
    }
    
    console.log(`处理收藏操作：帖子ID=${post.id}, 当前收藏状态=${post.favorited}`)
    
    // 先更新UI状态
    const originalStatus = post.favorited
    post.favorited = !originalStatus
    
    // 更新本地存储
    const favoriteKey = `post_favorite_${post.id}_${userId}`
    localStorage.setItem(favoriteKey, JSON.stringify({
      favorited: post.favorited,
      timestamp: Date.now()
    }))
    
    // 发送API请求
    if (originalStatus) {
      // 取消收藏
      await unfavoritePost(post.id)
    } else {
      // 收藏
      await favoritePost(post.id)
    }
    
    console.log(originalStatus ? '取消收藏成功' : '收藏成功')
  } catch (error) {
    console.error('收藏操作失败:', error)
    
    // 恢复UI状态
    post.favorited = !post.favorited
    
    // 恢复本地存储
    const favoriteKey = `post_favorite_${post.id}_${userId}`
    localStorage.setItem(favoriteKey, JSON.stringify({
      favorited: post.favorited,
      timestamp: Date.now()
    }))
    
    // 显示错误提示
    alert('收藏操作失败，请稍后重试')
  }
}

// 翻页
const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    fetchPosts()
  }
}

const nextPage = () => {
  if (hasMorePages.value) {
    currentPage.value++
    fetchPosts()
  }
}

// 搜索帖子
const searchPosts = () => {
  console.log('搜索按钮被点击，当前搜索关键词:', searchQuery.value);
  
  if (searchQuery.value) {
    console.log('设置搜索模式为 keyword');
    searchMode.value = 'keyword'
  } else {
    console.log('设置搜索模式为 basic');
    searchMode.value = 'basic'
  }
  
  console.log('重置页码为 1');
  currentPage.value = 1
  
  console.log('调用 fetchPosts 函数获取帖子');
  fetchPosts()
}

// 设置分类
const setCategory = (category) => {
  activeCategory.value = category
  if (category !== 'all') {
    searchMode.value = 'category'
  } else {
    searchMode.value = 'basic'
  }
  currentPage.value = 1
  fetchPosts()
}

// 执行高级搜索
const doAdvancedSearch = () => {
  searchMode.value = 'advanced'
  currentPage.value = 1
  fetchPosts()
}

// 应用高级搜索
const applyAdvancedSearch = () => {
  // 将高级搜索的值应用到主搜索
  searchQuery.value = advancedSearch.keyword
  
  if (advancedSearch.category) {
    activeCategory.value = advancedSearch.category
  } else {
    activeCategory.value = 'all'
  }
  
  sortBy.value = advancedSearch.sortBy
  sortOrder.value = advancedSearch.sortOrder
  
  // 设置搜索模式为高级搜索
  searchMode.value = 'advanced'
  
  // 重置页码
  currentPage.value = 1
  
  // 关闭高级搜索面板
  showAdvancedSearch.value = false
  
  // 执行搜索
  fetchPosts()
}

// 格式化日期，可选择是否包含时间
const formatDate = (dateTime, includeTime = true) => {
  if (!dateTime) return '';
  
  try {
    let date;
    
    // 处理各种可能的时间格式
    if (typeof dateTime === 'number') {
      date = new Date(dateTime);
    } 
    else if (!isNaN(Number(dateTime)) && dateTime.toString().length >= 10) {
      const timestamp = dateTime.toString().length === 10 
        ? Number(dateTime) * 1000  // 秒转毫秒
        : Number(dateTime);        // 已经是毫秒
      date = new Date(timestamp);
    }
    else {
      date = new Date(dateTime);
      if (isNaN(date.getTime())) {
        return String(dateTime);
      }
    }
    
    // 确保日期有效
    if (isNaN(date.getTime())) {
      return String(dateTime);
    }
    
    // 格式化为 YYYY-MM-DD 或 YYYY-MM-DD HH:MM
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    
    if (includeTime) {
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}`;
    } else {
      return `${year}-${month}-${day}`;
    }
  } catch (error) {
    console.error('格式化日期出错:', error);
    return String(dateTime);
  }
}

// 跳转到帖子详情
const goToPostDetail = (postId) => {
  router.push(`/post/${postId}`)
}

// 跳转到个人主页
const goToProfile = () => {
  router.push('/user-profile')
}

// 获取热门标签
const fetchHotTags = async () => {
  tagsLoading.value = true
  try {
    const response = await getHotTags()
    if (response && response.data) {
      hotTags.value = response.data
      console.log('获取热门标签成功:', hotTags.value)
    }
  } catch (error) {
    console.error('获取热门标签失败:', error)
  } finally {
    tagsLoading.value = false
  }
}

// 获取本周热门帖子
const fetchHotPosts = async () => {
  hotPostsLoading.value = true
  try {
    const response = await getHotPostsOfWeek()
    if (response && response.data) {
      hotPosts.value = response.data
      console.log('获取本周热门帖子成功:', hotPosts.value)
    }
  } catch (error) {
    console.error('获取本周热门帖子失败:', error)
  } finally {
    hotPostsLoading.value = false
  }
}

// 获取文本资源
const fetchTextResources = async () => {
  textResourcesLoading.value = true
  try {
    const response = await getTextResources()
    if (response && response.data) {
      textResources.value = response.data
      console.log('获取文本资源成功:', textResources.value)
      if (textResources.value.length > 0) {
        refreshRandomText()
      }
    }
  } catch (error) {
    console.error('获取文本资源失败:', error)
  } finally {
    textResourcesLoading.value = false
  }
}

// 刷新随机文本
const refreshRandomText = () => {
  if (textResources.value && textResources.value.length > 0) {
    const randomIndex = Math.floor(Math.random() * textResources.value.length)
    randomTextResource.value = textResources.value[randomIndex]
  }
}

// 根据标签数量确定标签大小
const getTagSize = (count) => {
  if (hotTags.value.length === 0) return 'default'
  
  const maxCount = Math.max(...hotTags.value.map(tag => tag.count))
  const minCount = Math.min(...hotTags.value.map(tag => tag.count))
  
  if (maxCount === minCount) return 'default'
  
  const range = maxCount - minCount
  const step = range / 3
  
  if (count >= minCount + step * 2) return 'large'
  if (count >= minCount + step) return 'default'
  return 'small'
}

// 根据标签搜索
const searchByTag = (tagName) => {
  searchQuery.value = tagName
  searchMode.value = 'keyword'
  activeCategory.value = 'all'
  currentPage.value = 1
  fetchPosts()
}

// 组件挂载
onMounted(() => {
  console.log('Community 组件已挂载');
  fetchPosts();
  fetchRecentNotices(); // 获取最近公告
  fetchHotTags(); // 获取热门标签
  fetchHotPosts(); // 获取本周热门帖子
  fetchTextResources(); // 获取文本资源
  
  // 测试服务器时间，帮助调试时间显示问题
  testServerTime().then(result => {
    if (result) {
      console.log('服务器与客户端时间差（分钟）:', result.timeDiffMinutes);
    }
  });
  
  // 直接测试公告API
  testNoticeApi().then(result => {
    console.log('测试公告API结果:', result);
  }).catch(error => {
    console.error('测试公告API失败:', error);
  });
})

// 处理图片上传
const handleFileUpload = async (event) => {
  const files = event.target.files
  if (!files || files.length === 0) {
    console.log('没有选择图片文件')
    return
  }
  
  // 检查上传文件数量
  if (uploadedImages.value.length + files.length > maxImageCount) {
    alert(`最多可上传${maxImageCount}张图片`)
    return
  }
  
  for (let file of files) {
    if (!file.type.startsWith('image/')) {
      console.warn(`不是图片文件: ${file.name}, 类型: ${file.type}`)
      continue
    }
    
    const formData = new FormData()
    formData.append('file', file)
    try {
      console.log('开始上传图片...')
      const res = await request.post('/api/upload/image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      
      console.log('上传图片返回数据:', res)
      
      let imageUrl = ''
      // 兼容后端返回的data为字符串URL
      if (res.data && typeof res.data === 'string') {
        imageUrl = res.data
      } else if (res.data && res.data.url) {
        imageUrl = res.data.url
      }
      
      console.log('获取的图片URL:', imageUrl)
      
      if (!imageUrl || imageUrl.trim() === '') {
        console.error('上传图片失败: 服务器未返回有效的图片URL')
        alert('服务器未返回有效的图片URL')
        continue
      }
      
      // 检查URL是否只有路径没有文件名
      if (imageUrl.trim() === '/static/upload/' || imageUrl.trim().endsWith('/')) {
        console.error('上传图片失败: URL只有路径没有文件名', imageUrl)
        alert('服务器返回的图片URL格式错误，缺少文件名')
        continue
      }
      
      // 确保URL格式为 /static/upload/filename.ext
      if (!imageUrl.startsWith('/static/upload/')) {
        // 如果返回的URL格式不是 /static/upload/ 开头，则提取文件名并构建标准格式
        const parts = imageUrl.split('/')
        const filename = parts[parts.length - 1]
        
        if (!filename || filename.trim() === '') {
          console.error('上传图片失败: 无法从URL提取文件名', imageUrl)
          alert('图片URL格式错误，无法提取文件名')
          continue
        }
        
        imageUrl = `/static/upload/${filename}`
      } else {
        // 即使URL已经是/static/upload/开头，也要检查是否有文件名
        const parts = imageUrl.split('/')
        const filename = parts[parts.length - 1]
        
        if (!filename || filename.trim() === '') {
          console.error('上传图片失败: URL格式正确但缺少文件名', imageUrl)
          alert('图片URL缺少文件名')
          continue
        }
      }
      
      // 添加到上传列表
      uploadedImages.value.push(imageUrl)
      console.log(`添加图片URL到上传列表: ${imageUrl}`)
    } catch (error) {
      console.error('图片上传失败:', error)
      if (error.message && error.message.includes('Maximum upload size exceeded')) {
        alert('图片上传失败：文件大小超出限制（最大10MB）')
      } else {
        alert('图片上传失败，请重试：' + (error.message || '未知错误'))
      }
    }
  }
  console.log('上传后图片URL列表:', uploadedImages.value)
}

// 根据图片URL获取适当的显示URL
const getImageUrl = (imageUrl, index) => {
  if (!imageUrl) return '/src/assets/default-avatar.png';
  
  // 提取文件名
  const parts = imageUrl.split('/')
  const filename = parts[parts.length - 1]
  
  // 如果没有有效的文件名，返回默认图片
  if (!filename || filename.trim() === '') {
    console.warn(`图片URL没有有效的文件名: ${imageUrl}`)
    return '/src/assets/default-avatar.png';
  }
  
  // 首先尝试直接图片API
  // 检查filename是否包含查询参数（例如:1）
  const cleanFilename = filename.split(':')[0]; // 移除任何:1这样的后缀
  
  // 使用后端基础URL直接访问新的API
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/mental';
  return `${baseUrl}/api/static/direct-image/${cleanFilename}`
}

// 显示图片查看弹窗
const showFullImage = (imageUrl) => {
  const directUrl = getImageUrl(imageUrl)
  currentViewingImage.value = directUrl
  showImageViewer.value = true
}

// 辅助函数：将字符串转换为数组并确保URL完整
const getImagesArray = (images) => {
  if (!images) return []
  
  console.log('处理图片数据:', images, '类型:', typeof images)
  
  if (typeof images === 'string') {
    // 过滤掉空字符串和只有路径没有文件名的URL
    const imagesArray = images.split(',').filter(img => {
      // 过滤空字符串
      if (img.trim() === '') return false
      
      // 过滤只有路径没有文件名的情况（例如 "/static/upload/"）
      if (img.trim() === '/static/upload/' || img.trim().endsWith('/')) {
        console.warn(`过滤掉无效的图片URL(只有路径): ${img}`)
        return false
      }
      
      return true
    })
    
    console.log('解析并过滤后的图片数组:', imagesArray)
    
    // 处理每个URL，确保路径正确，过滤掉不完整的URL
    return imagesArray
      .map((url, index) => {
        console.log(`处理图片[${index}]的URL: ${url}`)
        
        // 如果已经是完整URL，直接返回
        if (url.startsWith('http')) {
          return url
        }
        
        // 提取文件名，处理可能的查询参数
        const parts = url.split('/')
        let filename = parts[parts.length - 1]
        
        // 清理文件名中的查询参数部分
        if (filename.includes(':')) {
          filename = filename.split(':')[0];
        }
        
        console.log(`提取的文件名: ${filename}`)
        
        // 检查文件名是否为空
        if (!filename || filename === '') {
          console.warn(`图片URL没有有效的文件名: ${url}`)
          return null // 返回null以便后续过滤
        }
        
        // 标准化URL格式为 /static/upload/filename
        return `/static/upload/${filename}`
      })
      .filter(url => url !== null) // 过滤掉无效的URL
  } else if (Array.isArray(images)) {
    console.log('已是数组格式的图片:', images)
    // 过滤掉不完整的URL并标准化格式
    return images
      .filter(url => {
        // 过滤非字符串
        if (typeof url !== 'string') return false
        
        // 过滤只有路径没有文件名的情况
        if (url.trim() === '/static/upload/' || url.trim().endsWith('/')) {
          console.warn(`过滤掉无效的图片URL(只有路径): ${url}`)
          return false
        }
        
        // 检查是否有有效的文件名
        const parts = url.split('/')
        const filename = parts[parts.length - 1]
        return filename && filename !== ''
      })
      .map(url => {
        // 提取文件名，统一使用/static/upload/格式
        const parts = url.split('/')
        let filename = parts[parts.length - 1]
        
        // 清理文件名中的查询参数部分
        if (filename.includes(':')) {
          filename = filename.split(':')[0];
        }
        
        return `/static/upload/${filename}`
      });
  }
  
  console.warn('无法处理的图片格式:', images)
  return []
}

// 图片错误处理
const handleImageError = (event, post, index) => {
  console.error(`图片加载失败: 帖子ID=${post.id}, 图片索引=${index}`)
  
  // 尝试其他URL格式
  const imagesArray = getImagesArray(post.images)
  const url = imagesArray[index]
  
  if (url) {
    const parts = url.split('/')
    let filename = parts[parts.length - 1]
    
    // 清理文件名中的查询参数部分
    if (filename.includes(':')) {
      filename = filename.split(':')[0];
    }
    
    // 检查文件名是否为空
    if (!filename || filename === '') {
      console.error('图片URL没有有效的文件名，无法处理')
      event.target.src = '/src/assets/default-avatar.png'
      event.target.alt = '图片加载失败'
      return
    }
    
    // 使用我们新创建的API直接获取图片
    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/mental';
    const directImageUrl = `${baseUrl}/api/static/direct-image/${filename}`
    console.log(`使用直接图片API获取: ${directImageUrl}`)
    event.target.src = directImageUrl
    
    // 如果直接API也失败，尝试其他URL格式
    event.target.onerror = () => {
      console.log(`直接图片API失败，尝试其他格式`)
      
      // 标准化当前URL为后端可识别的格式
      let standardUrl = `/static/upload/${filename}`
      
      // 尝试其他URL格式 - 按优先级排序
      const alternativeUrls = [
        // 新增对接口的尝试
        `${baseUrl}/api/resource/image/${filename}`,
        // 添加对image子目录的支持
        `${baseUrl}/static/upload/image/${filename}`,
        `${baseUrl}/api/static/direct-image/image/${filename}`,
        // 最有可能成功的格式
        `${baseUrl}/static/upload/${filename}`,
        `${baseUrl.replace('/mental', '')}/static/upload/${filename}`,
        `/mental/static/upload/${filename}`,
        // 其他可能的格式
        `${baseUrl}/upload/${filename}`,
        `${baseUrl.replace('/mental', '')}/upload/${filename}`,
        `/mental/upload/${filename}`,
        `/upload/${filename}`,
        // 检查src/main/resources/static/upload下是否有图片
        `${baseUrl}/src/main/resources/static/upload/image/${filename}`,
        `${baseUrl}/src/main/resources/static/upload/${filename}`,
        // 最后尝试直接访问文件名
        `/${filename}`
      ]
      
      console.log('图片加载失败URL:', url)
      console.log('标准化URL为:', standardUrl)
      console.log('将尝试以下替代URL:', alternativeUrls.join(', '))
      
      // 使用立即执行的递归函数尝试不同的URL
      const tryNextUrl = (urlList, index = 0) => {
        if (index >= urlList.length) {
          // 所有URL都尝试失败，使用默认图片
          console.log('所有URL尝试均失败，使用默认图片')
          event.target.src = '/src/assets/default-avatar.png'
          event.target.alt = '图片加载失败'
          event.target.onerror = null // 清除事件处理器
          return;
        }
        
        console.log(`尝试URL[${index}]: ${urlList[index]}`)
        event.target.src = urlList[index]
        
        // 设置错误处理函数以尝试下一个URL
        event.target.onerror = () => {
          console.log(`URL[${index}]加载失败，尝试下一个`)
          tryNextUrl(urlList, index + 1)
        }
      }
      
      // 开始尝试所有可能的URL
      tryNextUrl(alternativeUrls)
    }
  } else {
    console.error('没有有效的图片URL')
    event.target.src = '/src/assets/default-avatar.png'
    event.target.alt = '图片加载失败'
  }
}

// 判断是否为自己的帖子
const isMyPost = (post) => {
  return post.userId && userStore.userId && String(post.userId) === String(userStore.userId)
}

// 删除自己的帖子
const deleteMyPost = async (post) => {
  if (!confirm('确定要删除这条帖子吗？')) return
  try {
    const res = await request.delete(`/api/post/user/${post.id}`, { params: { userId: userStore.userId } })
    if (res.code === 200) {
      alert('删除成功')
      // 刷新帖子列表
      fetchPosts()
    } else {
      alert(res.msg || '删除失败')
    }
  } catch (e) {
    alert('删除失败，请稍后重试')
  }
}

// 格式化日期时间，处理后端返回的时间格式
const formatDateTime = (dateTime) => {
  if (!dateTime) return '';
  
  try {
    // 解析日期对象
    const date = new Date(dateTime);
    
    // 检查日期是否有效
    if (isNaN(date.getTime())) {
      console.warn('无效的日期时间:', dateTime);
      return String(dateTime);
    }
    
    // 格式化为年-月-日 时:分
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    
    return `${year}-${month}-${day} ${hours}:${minutes}`;
  } catch (error) {
    console.error('格式化日期时间出错:', error);
    return String(dateTime);
  }
}

// 截断内容
const truncateContent = (content, maxLength) => {
  if (!content) return '';
  return content.length > maxLength ? content.slice(0, maxLength) + '...' : content;
}

// 处理日期时间显示相关的工具函数可以在这里添加

// 获取最近公告
const fetchRecentNotices = async () => {
  console.log('正在获取最近系统公告');
  try {
    const response = await getRecentNotices(5);
    console.log('获取最近系统公告响应:', response);
    if (response && response.data) {
      notices.value = Array.isArray(response.data) ? response.data : [response.data];
      console.log('加载的最近公告数量:', notices.value.length);
      
      // 记录公告时间格式
      if (notices.value.length > 0) {
        const firstNotice = notices.value[0];
        console.log('公告时间示例:', firstNotice.createTime, '格式化后:', formatDateTime(firstNotice.createTime));
      }
    } else {
      console.warn('获取最近系统公告返回空数据');
      notices.value = [];
    }
  } catch (error) {
    console.error('获取最近公告失败:', error);
    // 使用模拟数据
    notices.value = [
      {
        id: 'mock1',
        title: '欢迎使用心理健康系统',
        content: '这是一条模拟的系统公告，当API未正常工作时会显示。',
        createTime: new Date().toISOString(),
        updateTime: new Date().toISOString(),
        status: 1
      }
    ];
  }
}

// 获取所有公告
const fetchNotices = async () => {
  noticesLoading.value = true;
  const params = {
    page: noticePage.value,
    size: noticePageSize.value
  };
  
  console.log('正在获取系统公告列表，参数:', params);
  
  try {
    const response = await getSystemNotices(params);
    console.log('获取系统公告列表响应:', response);
    if (response && response.data) {
      allNotices.value = response.data.records || response.data;
      noticeTotal.value = response.data.total || response.data.length;
      console.log('加载的公告数量:', allNotices.value.length);
      
      // 调试日期格式
      if (allNotices.value.length > 0) {
        const firstNotice = allNotices.value[0];
        console.log('公告列表中的时间格式:', firstNotice.createTime);
        console.log('格式化后:', formatDateTime(firstNotice.createTime));
      }
    } else {
      console.warn('获取系统公告列表返回空数据');
      allNotices.value = [];
      noticeTotal.value = 0;
    }
  } catch (error) {
    console.error('获取公告列表失败:', error);
    // 使用模拟数据
    allNotices.value = [
      {
        id: 'mock1',
        title: '欢迎使用心理健康系统',
        content: '这是一条模拟的系统公告，当API未正常工作时会显示。',
        createTime: new Date().toISOString(),
        updateTime: new Date().toISOString(),
        status: 1
      }
    ];
    noticeTotal.value = allNotices.value.length;
  } finally {
    noticesLoading.value = false;
  }
}

// 查看公告详情
const viewNoticeDetail = (notice) => {
  // 如果已经有完整的公告信息，直接显示
  if (notice.content) {
    // 为避免时间格式问题，确保格式化时间
    const processedNotice = {
      ...notice,
      createTime: notice.createTime,
      updateTime: notice.updateTime
    };
    
    // 记录原始时间格式用于调试
    console.log('查看公告详情，原始时间格式:', {
      createTime: notice.createTime,
      updateTime: notice.updateTime
    });
    
    currentNotice.value = processedNotice;
    showNoticeDetail.value = true;
    return;
  }
  
  // 否则获取详细信息
  console.log('获取公告详情，ID:', notice.id);
  getNoticeDetail(notice.id).then(response => {
    console.log('公告详情API响应:', response);
    if (response && response.data) {
      // 确保时间格式正确
      const detailData = response.data;
      
      // 记录原始时间格式用于调试
      console.log('公告详情中的时间格式:', {
        createTime: detailData.createTime,
        updateTime: detailData.updateTime
      });
      
      currentNotice.value = detailData;
      showNoticeDetail.value = true;
    }
  }).catch(error => {
    console.error('获取公告详情失败:', error);
    // 使用当前公告信息
    currentNotice.value = notice;
    showNoticeDetail.value = true;
  });
}

// 查看所有公告
const showAllNotices = () => {
  noticePage.value = 1; // 重置页码
  showAllNoticesDialog.value = true;
  // 对话框打开时会通过 @open 事件触发 fetchNotices 方法
}

// 公告分页切换
const noticePageChange = (page) => {
  noticePage.value = page;
  fetchNotices();
}

// 测试服务器时间
const testServerTime = async () => {
  try {
    const response = await getServerTime();
    if (response && response.data) {
      console.log('服务器时间测试结果:', response.data);
      const serverTime = new Date(response.data.serverTime);
      const clientTime = new Date();
      
      console.log('服务器时间:', serverTime.toLocaleString());
      console.log('客户端时间:', clientTime.toLocaleString());
      console.log('时差（分钟）:', Math.round((clientTime - serverTime) / (60 * 1000)));
      
      return {
        serverTime,
        clientTime,
        timeDiffMinutes: Math.round((clientTime - serverTime) / (60 * 1000))
      };
    }
  } catch (error) {
    console.error('测试服务器时间失败:', error);
  }
  return null;
}
</script>

<style scoped>
.community-container {
  max-width: 1500px;
  margin: 0 auto;
  padding: 24px 15px;
  background-color: #f9fafc;
  min-height: calc(100vh - 60px);
  width: 98%;
}

/* 三栏布局 */
.three-column-layout {
  display: flex;
  gap: 20px;
  margin-top: 20px;
  max-width: 100%;
  margin-left: auto;
  margin-right: auto;
  width: 100%;
}

.left-sidebar, .right-sidebar {
  width: 250px;
  flex-shrink: 0;
}

.community-content {
  flex-grow: 1;
  min-width: 0; /* 防止内容溢出 */
  max-width: calc(100% - 500px - 40px); /* 确保中间内容不会过宽，但比之前更宽 */
}

/* 侧边栏盒子样式 */
.sidebar-box {
  background-color: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.06);
  padding: 16px;
  margin-bottom: 20px;
  border: 1px solid #f0f0f0;
  transition: transform 0.2s, box-shadow 0.2s;
}

.sidebar-box:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.sidebar-title {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eaeaea;
  position: relative;
}

.sidebar-title:after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 60px;
  height: 3px;
  background: linear-gradient(90deg, #3498db, #6dd5fa);
  border-radius: 3px;
}

.sidebar-title h3 {
  font-size: 17px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
  display: flex;
  align-items: center;
}

.sidebar-title h3 i {
  margin-right: 8px;
}

/* AI心情小助手样式 */
.mood-assistant {
  background-color: #f8f9ff;
  position: relative;
  overflow: hidden;
}

.mood-assistant:before {
  content: '';
  position: absolute;
  top: -20px;
  right: -20px;
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, rgba(162, 155, 254, 0.1), rgba(108, 92, 231, 0.05));
  border-radius: 50%;
  z-index: 0;
}

.mood-quote {
  font-size: 15px;
  line-height: 1.7;
  color: #444;
  margin-bottom: 12px;
  padding: 15px;
  background: #fff;
  border-radius: 8px;
  border-left: 4px solid #a29bfe;
  box-shadow: 0 2px 8px rgba(162, 155, 254, 0.1);
  position: relative;
  z-index: 1;
}

.mood-author {
  text-align: right;
  font-style: italic;
  font-size: 13px;
  color: #666;
  margin-top: 10px;
  padding-right: 5px;
}

.mood-refresh {
  text-align: right;
  margin-top: 8px;
}

/* 热门标签云样式 */
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 5px;
}

.hot-tag-item {
  cursor: pointer;
  transition: all 0.25s;
  margin: 2px 0;
  position: relative;
  overflow: hidden;
}

.hot-tag-item:hover {
  transform: scale(1.05);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.hot-tag-item:after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, #3498db, transparent);
  transform: translateX(-100%);
  transition: transform 0.3s ease;
}

.hot-tag-item:hover:after {
  transform: translateX(0);
}

/* 本周热门帖子样式 */
.hot-post-item {
  padding: 12px;
  border-bottom: 1px solid #eaeaea;
  cursor: pointer;
  transition: all 0.3s;
  border-radius: 6px;
  margin-bottom: 4px;
  position: relative;
}

.hot-post-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.hot-post-item:hover {
  background-color: #f0f7ff;
  padding-left: 16px;
}

.hot-post-item:before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  height: 0;
  width: 3px;
  background: #3498db;
  transform: translateY(-50%);
  transition: height 0.3s;
}

.hot-post-item:hover:before {
  height: 70%;
}

.hot-post-title {
  font-size: 14px;
  margin-bottom: 10px;
  line-height: 1.5;
  font-weight: 500;
  color: #2c3e50;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.hot-post-stats {
  display: flex;
  font-size: 12px;
  color: #666;
  align-items: center;
}

.hot-post-likes, .hot-post-comments {
  margin-right: 15px;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .three-column-layout {
    gap: 15px;
  }
  
  .left-sidebar, .right-sidebar {
    width: 230px;
  }
  
  .community-content {
    max-width: calc(100% - 460px - 30px);
  }
}

@media (max-width: 1024px) {
  .three-column-layout {
    gap: 15px;
  }
  
  .left-sidebar, .right-sidebar {
    width: 220px;
  }
  
  .community-content {
    max-width: calc(100% - 440px - 30px);
  }
}

@media (min-width: 1400px) {
  .three-column-layout {
    max-width: 100%;
  }
  
  .community-content {
    max-width: calc(100% - 500px - 40px);
    padding: 0 10px;
  }
}

@media (max-width: 768px) {
  .three-column-layout {
    flex-direction: column;
    gap: 20px;
  }
  
  .left-sidebar, .right-sidebar {
    width: 100%;
  }
  
  .community-content {
    max-width: 100%;
    order: 1;
  }
  
  .left-sidebar {
    order: 2;
  }
  
  .right-sidebar {
    order: 3;
  }
  
  .sidebar-box {
    margin-bottom: 20px;
  }
}

.community-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 10px;
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: 600px;
}

.search-box input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.search-btn {
  padding: 8px 16px;
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.advanced-search-btn {
  padding: 8px 16px;
  background-color: transparent;
  color: #1890ff;
  border: 1px solid #1890ff;
  border-radius: 4px;
  cursor: pointer;
}

.community-header .publish-btn {
  background: linear-gradient(135deg, #3498db, #2980b9);
  color: white;
  border: none;
  border-radius: 8px;
  padding: 10px 16px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.1);
}

.community-header .publish-btn:hover {
  background: linear-gradient(135deg, #2980b9, #1a5276);
  transform: translateY(-2px);
  box-shadow: 0 5px 12px rgba(0, 0, 0, 0.15);
}

.filter-container {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 12px;
}

.categories {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.category-item {
  display: inline-block;
  padding: 5px 14px;
  margin-right: 6px;
  background: #f4f8fb;
  border-radius: 999px;
  cursor: pointer;
  font-size: 13px;
  color: #217dbb;
  border: 1.5px solid #e0e6ed;
  box-shadow: 0 2px 8px rgba(52,152,219,0.06);
  transition: background 0.2s, color 0.2s, box-shadow 0.2s, transform 0.15s;
  position: relative;
  margin-bottom: 6px;
}
.category-item.active {
  background: linear-gradient(90deg, #3498db 60%, #6dd5fa 100%);
  color: #fff;
  border: 1.5px solid #3498db;
  box-shadow: 0 4px 16px rgba(52,152,219,0.10);
  font-weight: bold;
  transform: scale(1.06);
  z-index: 1;
}
.category-item:hover:not(.active) {
  background: #eaf2fb;
  color: #3498db;
  box-shadow: 0 2px 12px rgba(52,152,219,0.10);
  transform: scale(1.04);
}

.sort-options {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  margin-left: 8px;
  flex-wrap: wrap;
}

.sort-options span {
  color: #217dbb;
  font-weight: 500;
  margin-right: 2px;
}

.sort-options select {
  padding: 5px 18px 5px 8px;
  border: 1.5px solid #e0e6ed;
  border-radius: 999px;
  background: #f4f8fb;
  color: #217dbb;
  font-size: 15px;
  outline: none;
  transition: border 0.2s, box-shadow 0.2s;
  cursor: pointer;
  min-width: 110px;
  width: auto;
}

.sort-options select:focus,
.sort-options select:hover {
  border: 1.5px solid #3498db;
  box-shadow: 0 2px 8px rgba(52,152,219,0.08);
  background: #eaf2fb;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 2px;
}

.post-card {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 18px 20px;
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  transition: transform 0.2s, box-shadow 0.2s;
  margin-bottom: 5px;
}

.post-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0,0,0,0.08);
}

.post-title {
  margin: 0 0 10px 0;
  cursor: pointer;
  color: #2c3e50;
  font-size: 18px;
  font-weight: 600;
  transition: color 0.2s;
}

.post-title:hover {
  color: #3498db;
}

.post-meta {
  display: flex;
  gap: 15px;
  color: #666;
  font-size: 14px;
  margin-bottom: 12px;
  align-items: center;
}

.post-meta .author {
  font-weight: 500;
  color: #3498db;
}

.post-content {
  color: #444;
  margin-bottom: 16px;
  line-height: 1.6;
  cursor: pointer;
  font-size: 15px;
  padding: 2px 0;
}

.post-images {
  margin-bottom: 16px;
  border-radius: 8px;
  overflow: hidden;
}

.images-grid {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.images-grid.grid-1 .image-item {
  width: 200px;
  height: 200px;
}

.images-grid.grid-2 .image-item {
  width: calc(50% - 5px);
  height: 150px;
}

.images-grid.grid-3 .image-item, 
.images-grid.grid-4 .image-item {
  width: calc(50% - 5px);
  height: 120px;
}

.image-item {
  position: relative;
  border: 1px solid #eaeaea;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  transition: transform 0.3s;
}

.image-item:hover {
  transform: scale(1.02);
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.image-item:hover img {
  transform: scale(1.05);
}

.more-images-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  color: white;
  font-size: 13px;
}

.post-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.tag {
  color: #3498db;
  font-size: 14px;
  background-color: #f0f7ff;
  padding: 3px 10px;
  border-radius: 12px;
  transition: all 0.2s;
}

.tag:hover {
  background-color: #e0f0ff;
  transform: translateY(-1px);
}

.post-actions {
  display: flex;
  gap: 24px;
  border-top: 1px solid #f0f0f0;
  padding-top: 15px;
  margin-top: 5px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #666;
  font-size: 15px;
  transition: all 0.2s;
  padding: 5px 8px;
  border-radius: 20px;
}

.action-item:hover {
  background-color: #f5f8fa;
}

.action-item.active {
  color: #3498db;
}

.action-item:hover {
  color: #3498db;
}

.icon-liked {
  color: #ff6b81;
}

.icon-favorited {
  color: #ffc107;
}

.delete-item {
  margin-left: auto;
  color: #e74c3c;
}

.delete-item:hover {
  color: #c0392b;
  background-color: #ffeeee;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 24px;
  margin-top: 36px;
  margin-bottom: 10px;
}

.page-btn {
  padding: 8px 16px;
  border: 1px solid #e0e6ed;
  background-color: #fff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 15px;
  transition: all 0.2s;
  color: #3498db;
}

.page-btn:hover:not(:disabled) {
  background-color: #f0f7ff;
  border-color: #3498db;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  color: #999;
}

.page-indicator {
  color: #555;
  font-size: 15px;
  font-weight: 500;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background-color: #fff;
  border-radius: 8px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow: auto;
  box-shadow: 0 4px 16px rgba(0,0,0,0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #888;
}

.post-form {
  padding: 16px;
}

.form-control {
  width: 100%;
  margin-bottom: 16px;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

textarea.form-control {
  resize: vertical;
  min-height: 120px;
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.cancel-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background-color: #fff;
  border-radius: 4px;
  cursor: pointer;
}

.submit-btn {
  padding: 8px 16px;
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #888;
}

.empty-btn {
  margin-top: 16px;
  padding: 8px 16px;
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

/* 高级搜索面板样式 */
.advanced-search-panel {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1001;
  backdrop-filter: blur(5px);
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.advanced-search-form {
  background-color: #fff;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow: auto;
  padding: 0;
  box-shadow: 0 10px 30px rgba(0,0,0,0.15);
  animation: slideDown 0.3s ease;
  display: flex;
  flex-direction: column;
}

@keyframes slideDown {
  from {
    transform: translateY(-50px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.advanced-search-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
  background: linear-gradient(to right, #3498db, #6dd5fa);
  border-radius: 12px 12px 0 0;
}

.advanced-search-header h3 {
  color: #fff;
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.close-search-btn {
  color: #fff;
  padding: 8px;
  margin: 0;
}

.form-group {
  margin: 0;
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
}

.form-group label {
  display: block;
  margin-bottom: 10px;
  color: #606266;
  font-weight: 500;
  font-size: 15px;
}

.input-with-icon {
  position: relative;
}

.input-with-icon .el-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #909399;
  font-size: 16px;
}

.styled-input {
  width: 100%;
  padding: 12px 12px 12px 35px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  font-size: 15px;
  box-sizing: border-box;
  transition: all 0.3s;
}

.styled-input:focus {
  border-color: #3498db;
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.2);
  outline: none;
}

.select-wrapper {
  position: relative;
}

.styled-select {
  width: 100%;
  padding: 12px 35px 12px 15px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  appearance: none;
  background-color: white;
  font-size: 15px;
  color: #606266;
  cursor: pointer;
  transition: all 0.3s;
}

.styled-select:focus {
  border-color: #3498db;
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.2);
  outline: none;
}

.select-arrow {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
  color: #909399;
}

.sort-group {
  border-bottom: none;
}

.sort-selects {
  display: flex;
  gap: 10px;
}

.half-width {
  flex: 1;
}

.search-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  background-color: #f9fafb;
  border-top: 1px solid #ebeef5;
  border-radius: 0 0 12px 12px;
}

.apply-search-btn {
  padding: 10px 24px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.cancel-search-btn {
  border: 1px solid #dcdfe6;
}

/* 图片上传区域样式 */
.image-upload-container {
  margin-bottom: 16px;
}

.upload-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.upload-header h4 {
  margin: 0;
  color: #333;
}

.image-count {
  color: #888;
  font-size: 13px;
}

.image-preview-container {
  display: flex;
  gap: 10px;
  margin-bottom: 8px;
}

.image-preview {
  position: relative;
  width: 100px;
  height: 100px;
  border: 1px solid #ddd;
  border-radius: 4px;
  overflow: hidden;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-actions {
  position: absolute;
  top: 0;
  right: 0;
  display: flex;
  gap: 5px;
  padding: 5px;
}

.delete-btn {
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  color: #888;
}

.upload-btn-wrapper {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
}

.upload-btn {
  display: flex;
  align-items: center;
  gap: 5px;
}

.upload-icon {
  color: #888;
  font-size: 16px;
}

.upload-tips {
  color: #888;
  font-size: 13px;
}

/* 图片查看弹窗样式 */
.image-viewer-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1002;
}

.image-viewer-content {
  background-color: #fff;
  border-radius: 8px;
  width: 90%;
  max-width: 800px;
  max-height: 90vh;
  overflow: auto;
  padding: 16px;
}

.image-viewer-content img {
  width: 100%;
  height: auto;
  max-height: 100%;
  object-fit: contain;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #888;
  float: right;
}

.delete-item {
  color: #ff4d4f;
}
.delete-item:hover {
  color: #d9363e;
}

/* 公告区域样式 */
.announcement-section {
  background: linear-gradient(135deg, #3498db, #1a5276);
  border-radius: 8px;
  margin: 10px auto;
  margin-bottom: 20px;
  overflow: hidden;
  box-shadow: 0 3px 15px rgba(0, 0, 0, 0.1);
  position: relative;
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.announcement-section:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
}

.announcement-container {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  color: white;
}

.announcement-icon {
  margin-right: 15px;
  position: relative;
}

.bell-icon {
  font-size: 22px;
  animation: ring 4s ease-in-out infinite;
  display: flex;
  align-items: center;
  justify-content: center;
}

@keyframes ring {
  0%, 100% {
    transform: rotate(0deg);
  }
  5%, 15% {
    transform: rotate(15deg);
  }
  10%, 20% {
    transform: rotate(-15deg);
  }
  25% {
    transform: rotate(0deg);
  }
}

.announcement-content {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.announcement-item {
  font-size: 16px;
  line-height: 36px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s;
}

.announcement-item:hover {
  transform: translateX(5px);
}

.announcement-action {
  margin-left: 20px;
  white-space: nowrap;
}

/* 弹出框样式优化 */
.announcement-detail {
  padding: 10px;
}

.announcement-detail-header h2 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 22px;
  font-weight: 600;
}

.announcement-detail-meta {
  display: flex;
  gap: 15px;
  color: #666;
  font-size: 14px;
  margin-bottom: 20px;
}

.announcement-detail-divider {
  height: 1px;
  background: linear-gradient(90deg, rgba(52, 152, 219, 0.8), rgba(52, 152, 219, 0.2));
  margin: 15px 0 20px 0;
}

.announcement-detail-content {
  line-height: 1.8;
  color: #333;
  font-size: 15px;
}

.announcement-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.announcement-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 15px;
  cursor: pointer;
  transition: transform 0.3s;
}

.announcement-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 40px rgba(52, 152, 219, 0.2);
}

.announcement-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.announcement-card-content {
  font-size: 15px;
  color: #333;
}

.announcement-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.announcement-card-time {
  font-size: 14px;
  color: #999;
}

.community-content {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 20px;
}

.community-main-layout {
  display: flex;
  gap: 40px;
  margin-top: 32px;
  justify-content: center;
  align-items: flex-start;
  max-width: 1400px;
  width: 100%;
  margin-left: auto;
  margin-right: auto;
  padding: 0 20px;
}
.community-sidebar {
  width: 180px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.community-content {
  flex: 0 1 1000px;
  min-width: 0;
  max-width: 1000px;
}
.left-sidebar {
  /* 左侧栏特定样式 */
}
.right-sidebar {
  /* 右侧栏特定样式 */
}
.sidebar-block {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
  padding: 20px 16px 18px 16px;
  margin-bottom: 0;
}
.sidebar-block h3 {
  font-size: 18px;
  margin-bottom: 18px;
  color: #217dbb;
  font-weight: bold;
}
.sidebar-block ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
.sidebar-block ul li {
  margin-bottom: 10px;
  color: #555;
  font-size: 15px;
}
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.tag-cloud .tag {
  background: #eaf2fb;
  color: #3498db;
  border-radius: 999px;
  padding: 5px 14px;
  font-size: 14px;
}
.ad-placeholder {
  background: #f4f8fb;
  color: #aaa;
  text-align: center;
  padding: 18px 0;
  border-radius: 8px;
  font-size: 15px;
}
.community-header {
  margin-bottom: 18px;
}
.search-box {
  display: flex;
  gap: 10px;
  align-items: center;
}
.search-box input {
  flex: 1;
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid #e0e6ed;
  font-size: 15px;
}
.search-btn, .advanced-search-btn, .publish-btn {
  padding: 8px 18px;
  border-radius: 6px;
  font-size: 15px;
  border: none;
  background: #217dbb;
  color: #fff;
  cursor: pointer;
  transition: background 0.2s;
}
.advanced-search-btn {
  background: #fff;
  color: #217dbb;
  border: 1.5px solid #217dbb;
}
.advanced-search-btn:hover, .search-btn:hover, .publish-btn:hover {
  background: #3498db;
  color: #fff;
}
.filter-container {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-start;
  align-items: center;
  margin-bottom: 18px;
  gap: 10px;
}
.categories {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.category-item {
  display: inline-block;
  padding: 6px 16px;
  margin-right: 6px;
  background: #f4f8fb;
  border-radius: 999px;
  cursor: pointer;
  font-size: 14px;
  color: #217dbb;
  border: 1.5px solid #e0e6ed;
  box-shadow: 0 2px 8px rgba(52,152,219,0.06);
  transition: background 0.2s, color 0.2s, box-shadow 0.2s, transform 0.15s;
  position: relative;
  margin-bottom: 6px;
}
.category-item.active {
  background: linear-gradient(90deg, #3498db 60%, #6dd5fa 100%);
  color: #fff;
  border: 1.5px solid #3498db;
  box-shadow: 0 4px 16px rgba(52,152,219,0.10);
  font-weight: bold;
  transform: scale(1.06);
  z-index: 1;
}
.category-item:hover:not(.active) {
  background: #eaf2fb;
  color: #3498db;
  box-shadow: 0 2px 12px rgba(52,152,219,0.10);
  transform: scale(1.04);
}
.sort-options {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  margin-left: 8px;
  flex-wrap: wrap;
}
.sort-options span {
  color: #217dbb;
  font-weight: 500;
  margin-right: 2px;
}
.sort-options select {
  padding: 6px 20px 6px 10px;
  border: 1.5px solid #e0e6ed;
  border-radius: 999px;
  background: #f4f8fb;
  color: #217dbb;
  font-size: 15px;
  outline: none;
  transition: border 0.2s, box-shadow 0.2s;
  cursor: pointer;
  min-width: 110px;
  width: auto;
}
.sort-options select:focus,
.sort-options select:hover {
  border: 1.5px solid #3498db;
  box-shadow: 0 2px 8px rgba(52,152,219,0.08);
  background: #eaf2fb;
}
.post-list {
  display: flex;
  flex-direction: column;
  gap: 22px;
}
.post-card {
  border: 1px solid #eee;
  border-radius: 14px;
  padding: 20px 18px;
  background-color: #fff;
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
}
.post-title {
  margin: 0 0 10px 0;
  cursor: pointer;
  color: #217dbb;
  font-size: 20px;
  font-weight: bold;
}
.post-meta {
  display: flex;
  gap: 10px;
  color: #888;
  font-size: 14px;
  margin-bottom: 10px;
}
.post-content {
  color: #333;
  margin-bottom: 14px;
  line-height: 1.6;
  cursor: pointer;
  font-size: 16px;
}
.post-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.tag {
  color: #1890ff;
  font-size: 14px;
}
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 24px;
  margin-top: 32px;
}
.page-btn {
  padding: 8px 18px;
  border: 1px solid #ddd;
  background-color: #fff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 15px;
}
.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.page-indicator {
  color: #666;
  font-size: 15px;
}
@media (max-width: 1200px) {
  .community-main-layout {
    gap: 20px;
    padding: 0 15px;
  }
  .community-sidebar {
    width: 160px;
  }
  .community-content {
    flex: 0 1 800px;
  }
}
@media (max-width: 900px) {
  .community-main-layout {
    flex-direction: column;
    gap: 20px;
    padding: 0 10px;
  }
  .community-sidebar {
    flex-direction: row;
    width: 100%;
    gap: 10px;
  }
  .left-sidebar, .right-sidebar {
    order: 1;
    width: 50%;
    min-width: 0;
  }
  .community-content {
    order: 2;
    max-width: 100%;
    min-width: 0;
    flex: 1 1 auto;
  }
}
@media (max-width: 600px) {
  .community-main-layout {
    flex-direction: column;
    gap: 0;
  }
  .community-sidebar {
    flex-direction: column;
    width: 100%;
    gap: 8px;
  }
  .left-sidebar, .right-sidebar {
    width: 100%;
  }
  .community-content {
    max-width: 100%;
    min-width: 0;
    margin: 0;
  }
}

/* 发布帖子弹窗样式 */
.post-dialog {
  border-radius: 12px;
  overflow: hidden;
}

.post-dialog :deep(.el-dialog__header) {
  padding: 16px 20px;
  margin: 0;
  border-bottom: 1px solid #ebeef5;
  background: linear-gradient(to right, #3498db, #2c3e50);
}

.post-dialog :deep(.el-dialog__title) {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}

.post-dialog :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: #fff;
}

.post-dialog :deep(.el-dialog__body) {
  padding: 20px;
}

.post-dialog :deep(.el-dialog__footer) {
  padding: 12px 20px;
  border-top: 1px solid #ebeef5;
  background-color: #f9fafb;
}

.post-form-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.form-user-info {
  display: flex;
  flex-direction: column;
}

.form-user-info .username {
  font-weight: 600;
  font-size: 16px;
  color: #333;
}

.form-user-info .post-visibility {
  font-size: 13px;
  color: #888;
}

.form-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  width: 100%;
}

.title-input :deep(.el-input__inner) {
  font-size: 18px;
  font-weight: 600;
  padding: 12px 15px;
  border-radius: 8px;
}

.category-select {
  max-width: 200px;
}

.category-dropdown {
  width: 100%;
}

.content-textarea :deep(.el-textarea__inner) {
  padding: 12px 15px;
  font-size: 15px;
  line-height: 1.6;
  border-radius: 8px;
  resize: none;
}

.tags-input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tags-input :deep(.el-input__inner) {
  padding-left: 35px;
  border-radius: 8px;
}

.tag-icon {
  color: #909399;
  font-size: 16px;
}

.tags-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 4px 0;
}

.preview-tag {
  background-color: #ecf5ff;
  border-color: #d9ecff;
  color: #409eff;
}

.image-upload-section {
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  padding: 16px;
  background-color: #f9fafb;
}

.upload-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.upload-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #606266;
}

.upload-count {
  color: #909399;
  font-size: 13px;
}

.image-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.preview-item {
  position: relative;
  width: 100%;
  height: 100px;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-actions {
  position: absolute;
  top: 5px;
  right: 5px;
  opacity: 0;
  transition: opacity 0.2s;
}

.preview-item:hover .preview-actions {
  opacity: 1;
}

.delete-image-btn {
  padding: 4px;
  background-color: rgba(255, 255, 255, 0.8);
}

.upload-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.upload-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: 6px;
}

.upload-tips {
  font-size: 12px;
  color: #909399;
  text-align: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style> 