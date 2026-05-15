// src/pages/NewsDetail.jsx
import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import NewsAI from '../components/NewsAI';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
console.log('=== NewsDetail 模块加载开始 ===');

// 调试 MUI 导入
try {
  const MuiTabs = await import('@mui/material/Tabs');
  console.log('MUI Tabs 导入成功:', MuiTabs);
} catch (error) {
  console.error('MUI Tabs 导入失败:', error);
}
import {
  Box,
  Container,
  Typography,
  Card,
  CardMedia,
  CardContent,
  Chip,
  Stack,
  Divider,
  Button,
  Skeleton,
  Alert,
  Paper,
  Grid,
  Snackbar
} from '@mui/material';
import {
  AccessTime,
  Visibility,
  Favorite,
  FavoriteBorder,
  Bookmark,
  BookmarkBorder,
  TrendingUp,
  OpenInNew,
  ArrowBack,
  Image as ImageIcon,
  Category as CategoryIcon,
  Tag,
  Article,      // 添加这个
  SmartToy      // 添加这个
} from '@mui/icons-material';

import { getNewsDetail } from '../api/news';
import {
  likeNews,
  collectNews,
  getBehaviorStatus,
  recordReadBehavior,
  recordClickBehavior
} from '../api/behavior';
import {
  formatCategory,
  formatDateTime,
  getFullImageUrl,
  processImageUrls,
  defaultNewsImage
} from '../utils/formatHelper';
import { useAuth } from '../hooks/useAuth';

// 相对时间显示辅助函数
const formatTimeAgo = (dateStr) => {
  if (!dateStr) return '未知时间';
  try {
    const date = new Date(dateStr.replace('T', ' '));
    if (isNaN(date.getTime())) return dateStr;

    const now = new Date();
    const diffInMs = now - date;
    const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
    const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60));
    const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));

    if (diffInMinutes < 1) return '刚刚';
    if (diffInMinutes < 60) return `${diffInMinutes}分钟前`;
    if (diffInHours < 24) return `${diffInHours}小时前`;
    if (diffInDays < 30) return `${diffInDays}天前`;
    return `${Math.floor(diffInDays / 30)}月前`;
  } catch {
    return '未知时间';
  }
};

const NewsDetail = () => {
  const { newsId } = useParams();
  const navigate = useNavigate();
  const { userId, isLoggedIn } = useAuth();

  // 核心状态
  const [news, setNews] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeImageIndex, setActiveImageIndex] = useState(0);
  
  const [activeTab, setActiveTab] = useState('news');
  // 用户行为相关状态
  const [liked, setLiked] = useState(false);
  const [collected, setCollected] = useState(false);
  const [behaviorLoading, setBehaviorLoading] = useState(false);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    type: 'success'
  });

  // 阅读时长统计
  const startTimeRef = useRef(null);

  // 关闭提示框
  const closeSnackbar = () => setSnackbar(prev => ({ ...prev, open: false }));

  // ========== 调试函数：检查用户ID一致性 ==========
  const checkUserIdConsistency = useCallback(() => {
    if (!import.meta.env.DEV) return;
    
    console.log('=== 用户ID一致性检查 ===');
    console.log('1. useAuth提供的userId:', userId);
    console.log('2. localStorage中的userId:', localStorage.getItem('userId'));
    
    const token = localStorage.getItem('token');
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        console.log('3. Token payload:', payload);
        console.log('   - sub:', payload.sub);
        console.log('   - userId:', payload.userId);
        console.log('   - id:', payload.id);
      } catch (e) {
        console.log('无法解析token:', e.message);
      }
    }
  }, [userId]);

  // ========== 点赞处理 ==========
  const handleLike = async () => {
    if (!isLoggedIn) {
      setSnackbar({
        open: true,
        message: '请先登录',
        type: 'warning'
      });
      setTimeout(() => navigate('/login'), 1000);
      return;
    }

    if (behaviorLoading) return;
    setBehaviorLoading(true);

    try {
      console.log('点赞操作 - 新闻ID:', newsId);
      const res = await likeNews(newsId);
      
      if ((res.code === 'SUCCESS' || res.code === 'success') && res.data) {
        setLiked(!liked);
        setNews(prev => ({
          ...prev,
          likeCount: res.data.likeCount || prev.likeCount
        }));
        setSnackbar({
          open: true,
          message: res.data.message || (liked ? '取消点赞成功' : '点赞成功'),
          type: 'success'
        });
      } else {
        setSnackbar({
          open: true,
          message: res.message || '点赞操作失败',
          type: 'error'
        });
      }
    } catch (err) {
      console.error('点赞接口调用失败:', err);
      
      if (err.response?.status === 401) {
        setSnackbar({
          open: true,
          message: '登录已过期，请重新登录',
          type: 'error'
        });
        setTimeout(() => navigate('/login'), 1000);
      } else if (err.message?.includes('foreign key constraint')) {
        setSnackbar({
          open: true,
          message: '用户信息异常，请重新登录',
          type: 'error'
        });
        localStorage.removeItem('userId');
        setTimeout(() => navigate('/login'), 1000);
      } else {
        setSnackbar({
          open: true,
          message: '网络错误，点赞操作失败',
          type: 'error'
        });
      }
    } finally {
      setBehaviorLoading(false);
    }
  };

  // ========== 收藏处理 ==========
  const handleCollect = async () => {
    if (!isLoggedIn) {
      setSnackbar({
        open: true,
        message: '请先登录',
        type: 'warning'
      });
      setTimeout(() => navigate('/login'), 1000);
      return;
    }

    if (behaviorLoading) return;
    setBehaviorLoading(true);

    try {
      console.log('收藏操作 - 新闻ID:', newsId);
      const res = await collectNews(newsId);
      
      if ((res.code === 'SUCCESS' || res.code === 'success') && res.data) {
        setCollected(!collected);
        setNews(prev => ({
          ...prev,
          collectCount: res.data.collectCount || prev.collectCount
        }));
        setSnackbar({
          open: true,
          message: res.data.message || (collected ? '取消收藏成功' : '收藏成功'),
          type: 'success'
        });
      } else {
        setSnackbar({
          open: true,
          message: res.message || '收藏操作失败',
          type: 'error'
        });
      }
    } catch (err) {
      console.error('收藏接口调用失败:', err);
      
      if (err.response?.status === 401) {
        setSnackbar({
          open: true,
          message: '登录已过期，请重新登录',
          type: 'error'
        });
        setTimeout(() => navigate('/login'), 1000);
      } else {
        setSnackbar({
          open: true,
          message: '网络错误，收藏操作失败',
          type: 'error'
        });
      }
    } finally {
      setBehaviorLoading(false);
    }
  };

  // ========== 调试用户ID ==========
  useEffect(() => {
    // 只在开发环境下运行调试
    if (import.meta.env.DEV) {
      checkUserIdConsistency();
    }
  }, [checkUserIdConsistency]);

  // ========== 加载新闻详情核心逻辑 ==========
  useEffect(() => {
    if (!newsId) {
      setError('未找到新闻ID');
      setLoading(false);
      return;
    }

    // 监听用户登录状态变化
    console.log('用户状态变化:', { userId, isLoggedIn });

    let isMounted = true;
    let behaviorStatusFetched = false;

    const fetchNewsDetail = async () => {
      try {
        setLoading(true);
        console.log('开始加载新闻详情，ID:', newsId);
        
        const res = await getNewsDetail(newsId);
        console.log('新闻详情接口返回:', res);

        if (!isMounted) return;

        if ((res.code === 'SUCCESS' || res.code === 'success') && res.data) {
          const newsData = res.data;

          // 处理图片数据
          newsData.processedImageUrls = processImageUrls(newsData.imageUrls);
          newsData.coverImageUrl = getFullImageUrl(newsData.coverImageUrl) || defaultNewsImage;
          newsData.thumbnailUrl = getFullImageUrl(newsData.thumbnailUrl) || defaultNewsImage;

          const hasValidImage = newsData.coverImageUrl !== defaultNewsImage || newsData.processedImageUrls.length > 0;
          newsData.hasImages = hasValidImage;
          newsData.imageCount = newsData.processedImageUrls.length || (hasValidImage ? 1 : 0);

          if (isMounted) {
            setNews(newsData);
            setError('');
          }

          // 如果是登录用户，获取行为状态
          if (isLoggedIn && userId) {
            try {
              console.log('登录用户，获取行为状态...');
              const behaviorRes = await getBehaviorStatus(newsId);
              if ((behaviorRes.code === 'SUCCESS' || behaviorRes.code === 'success') && behaviorRes.data) {
                if (isMounted) {
                  setLiked(behaviorRes.data.liked || false);
                  setCollected(behaviorRes.data.collected || false);
                }
                behaviorStatusFetched = true;
              }
            } catch (err) {
              console.log('获取行为状态失败:', err);
              // 对于401错误，说明token已失效，清除登录状态
              if (err.response?.status === 401) {
                console.log('Token已失效，清除登录状态');
                localStorage.removeItem('token');
                localStorage.removeItem('userId');
              }
            }
          }

          // 🔴 关键修改：只有登录用户才记录行为
          if (isLoggedIn && userId) {
            console.log('登录用户，记录行为...');
            try {
              await recordClickBehavior(newsId);
              await recordReadBehavior(newsId);
              console.log('行为记录成功，用户ID:', userId);
            } catch (err) {
              console.log('记录行为失败（可能是token失效）:', err);
              // 对于401错误，不跳转，只记录
              if (err.response?.status === 401) {
                console.log('Token已失效，请重新登录');
                // 显示提示但不跳转
                setSnackbar({
                  open: true,
                  message: '登录已过期，请重新登录',
                  type: 'warning'
                });
                // 清除本地存储
                localStorage.removeItem('token');
                localStorage.removeItem('userId');
              }
            }
          } else {
            console.log('未登录用户，跳过行为记录');
          }

          // 如果未登录或获取行为状态失败，设置默认状态
          if (!behaviorStatusFetched && isMounted) {
            setLiked(false);
            setCollected(false);
          }
          
          if (isMounted) {
            startTimeRef.current = new Date();
          }
        } else {
          if (isMounted) {
            setError(res.message || '获取新闻详情失败');
          }
        }
      } catch (err) {
        console.error('加载新闻详情异常:', err);
        if (isMounted) {
          setError('网络错误，无法加载新闻详情');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    fetchNewsDetail();

    return () => {
      isMounted = false;
      if (startTimeRef.current && userId && newsId) {
        const endTime = new Date();
        const durationSeconds = Math.floor((endTime - startTimeRef.current) / 1000);
        console.log(`用户阅读该新闻时长：${durationSeconds}秒，用户ID: ${userId}`);
      }
    };
  }, [newsId, isLoggedIn, userId]);

  // ========== 加载中状态 ==========
  if (loading) {
    return (
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Skeleton variant="rectangular" height={400} sx={{ mb: 3, borderRadius: 2 }} />
        <Skeleton variant="text" height={60} sx={{ mb: 2 }} />
        <Skeleton variant="text" height={40} width="60%" />
        <Box sx={{ mt: 4 }}>
          <Skeleton variant="rectangular" height={200} sx={{ mb: 2 }} />
          <Skeleton variant="rectangular" height={80} width="80%" />
        </Box>

        
      </Container>
    );
  }

  // ========== 错误状态 ==========
  if (error || !news) {
    return (
      <Container maxWidth="lg" sx={{ py: 8, textAlign: 'center' }}>
        <Alert severity="error" sx={{ mb: 3 }}>
          <Typography variant="h6">{error || '新闻不存在或加载失败'}</Typography>
        </Alert>
        <Button
          variant="contained"
          startIcon={<ArrowBack />}
          onClick={() => navigate(-1)}
          sx={{ mr: 2 }}
        >
          返回上一页
        </Button>
        <Button
          variant="outlined"
          onClick={() => navigate('/news')}
        >
          浏览更多新闻
        </Button>
      </Container>
    );
  }

  // ========== 处理图片数据 ==========
  const imageUrls = news.processedImageUrls || [];
  const hasImages = news.hasImages && imageUrls.length > 0;

  // ========== 主内容渲染 ==========
  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
      <Tabs value={activeTab} onChange={(e, newValue) => setActiveTab(newValue)}>
        <Tab label="新闻详情" value="news" icon={<Article />} />
        <Tab label="AI 助手" value="ai" icon={<SmartToy />} />
      </Tabs>
      </Box>
      {/* 返回按钮 */}
      <Box sx={{ mb: 3 }}>
        <Button
          variant="outlined"
          startIcon={<ArrowBack />}
          onClick={() => navigate(-1)}
          sx={{ borderRadius: 2 }}
        >
          返回
        </Button>
      </Box>

      {/* 新闻主卡片 */}
      {activeTab === 'news' && (
      <Paper elevation={3} sx={{ borderRadius: 3, overflow: 'hidden', mb: 4 }}>
        {/* 新闻图片区域 */}
        {hasImages && (
          <Box sx={{ position: 'relative' }}>
            <CardMedia
              component="img"
              height="500"
              image={imageUrls[activeImageIndex] || news.coverImageUrl || defaultNewsImage}
              alt={news.title}
              sx={{
                objectFit: 'cover',
                width: '100%',
                maxHeight: 500,
                backgroundColor: '#f5f5f5'
              }}
              onError={(e) => {
                e.target.src = defaultNewsImage;
              }}
            />

            {/* 多图指示器 */}
            {imageUrls.length > 1 && (
              <Box sx={{
                position: 'absolute',
                bottom: 16,
                left: 0,
                right: 0,
                display: 'flex',
                justifyContent: 'center',
                gap: 1
              }}>
                {imageUrls.map((_, index) => (
                  <Box
                    key={index}
                    onClick={() => setActiveImageIndex(index)}
                    sx={{
                      width: 10,
                      height: 10,
                      borderRadius: '50%',
                      bgcolor: index === activeImageIndex ? 'primary.main' : 'rgba(255,255,255,0.5)',
                      cursor: 'pointer',
                      transition: 'background-color 0.3s',
                      '&:hover': {
                        bgcolor: index === activeImageIndex ? 'primary.main' : 'rgba(255,255,255,0.8)'
                      }
                    }}
                  />
                ))}
              </Box>
            )}

            {/* 图片数量标签 */}
            <Chip
              icon={<ImageIcon />}
              label={`${news.imageCount || imageUrls.length}张图片`}
              sx={{
                position: 'absolute',
                top: 16,
                right: 16,
                bgcolor: 'rgba(0,0,0,0.7)',
                color: 'white',
                fontWeight: 'bold'
              }}
            />
          </Box>
        )}

        {/* 新闻内容区域 */}
        <CardContent sx={{ p: 4 }}>
          {/* 分类标签 */}
          <Stack direction="row" spacing={1} sx={{ mb: 3 }}>
            {news.category && (
              <Chip
                icon={<CategoryIcon />}
                label={formatCategory(news.category)}
                color="primary"
                variant="outlined"
                sx={{ fontWeight: 'bold' }}
              />
            )}
            {news.subcategory && (
              <Chip
                icon={<Tag />}
                label={news.subcategory}
                variant="outlined"
                size="small"
              />
            )}
            {news.isHot && (
              <Chip
                icon={<TrendingUp />}
                label="热门"
                color="error"
                size="small"
              />
            )}
          </Stack>

          {/* 新闻标题 */}
          <Typography
            variant="h3"
            component="h1"
            gutterBottom
            sx={{
              fontWeight: 700,
              lineHeight: 1.2,
              mb: 3
            }}
          >
            {news.title}
          </Typography>

          {/* 新闻元信息 */}
          <Grid container spacing={2} sx={{ mb: 4 }}>
            <Grid item xs={12} md={6}>
              <Stack direction="row" spacing={3} sx={{ color: 'text.secondary' }}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <AccessTime fontSize="small" sx={{ mr: 1 }} />
                  <Typography variant="body2">
                    {formatDateTime(news.publishTime)}
                    {news.publishTime && ` (${formatTimeAgo(news.publishTime)})`}
                  </Typography>
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <Visibility fontSize="small" sx={{ mr: 1 }} />
                  <Typography variant="body2">
                    浏览 {news.viewCount || 0}
                  </Typography>
                </Box>
              </Stack>
            </Grid>
            <Grid item xs={12} md={6}>
              <Stack direction="row" spacing={3} sx={{ color: 'text.secondary' }}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <Favorite fontSize="small" sx={{ mr: 1 }} />
                  <Typography variant="body2">
                    点赞 {news.likeCount || 0}
                  </Typography>
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <Bookmark fontSize="small" sx={{ mr: 1 }} />
                  <Typography variant="body2">
                    收藏 {news.collectCount || 0}
                  </Typography>
                </Box>
              </Stack>
            </Grid>
          </Grid>

          <Divider sx={{ mb: 4 }} />

          {/* 新闻摘要 */}
          {news.abstractText && (
            <Box sx={{ mb: 4 }}>
              <Typography variant="h6" color="primary" gutterBottom>
                摘要
              </Typography>
              <Typography
                variant="body1"
                sx={{
                  lineHeight: 1.8,
                  fontSize: '1.1rem',
                  color: 'text.primary'
                }}
              >
                {news.abstractText}
              </Typography>
            </Box>
          )}

          {/* 原文链接 */}
          {news.url && (
            <Box sx={{ mb: 4 }}>
              <Typography variant="h6" color="primary" gutterBottom>
                原文链接
              </Typography>
              <Button
                variant="outlined"
                endIcon={<OpenInNew />}
                href={news.url}
                target="_blank"
                rel="noopener noreferrer"
                sx={{ borderRadius: 2 }}
              >
                查看原文
              </Button>
            </Box>
          )}

          {/* 热度分数 */}
          {news.heatScore !== undefined && news.heatScore > 0 && (
            <Box sx={{ mb: 4 }}>
              <Typography variant="h6" color="primary" gutterBottom>
                热度分析
              </Typography>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Typography variant="body1">
                  热度分数: {news.heatScore.toFixed(2)}
                </Typography>
                <Box sx={{ flexGrow: 1, height: 8, borderRadius: 4, bgcolor: '#e0e0e0' }}>
                  <Box
                    sx={{
                      height: '100%',
                      borderRadius: 4,
                      bgcolor: 'primary.main',
                      width: `${Math.min(100, news.heatScore * 10)}%`,
                      transition: 'width 0.5s'
                    }}
                  />
                </Box>
              </Box>
            </Box>
          )}

          {/* 多图展示 */}
          {hasImages && imageUrls.length > 1 && (
            <Box sx={{ mb: 4 }}>
              <Typography variant="h6" color="primary" gutterBottom>
                新闻图片 ({imageUrls.length}张)
              </Typography>
              <Grid container spacing={2}>
                {imageUrls.map((imgUrl, index) => (
                  <Grid item xs={12} sm={6} md={4} key={index}>
                    <Card
                      sx={{
                        cursor: 'pointer',
                        border: index === activeImageIndex ? 2 : 0,
                        borderColor: 'primary.main',
                        transition: 'transform 0.2s',
                        '&:hover': {
                          transform: 'translateY(-4px)'
                        }
                      }}
                      onClick={() => setActiveImageIndex(index)}
                    >
                      <CardMedia
                        component="img"
                        height="140"
                        image={imgUrl}
                        alt={`新闻图片 ${index + 1}`}
                        sx={{ objectFit: 'cover' }}
                        onError={(e) => {
                          e.target.src = defaultNewsImage;
                        }}
                      />
                    </Card>
                  </Grid>
                ))}
              </Grid>
            </Box>
          )}

          <Divider sx={{ my: 4 }} />

          {/* 用户交互按钮 */}
          <Box sx={{ mt: 4, display: 'flex', gap: 3, alignItems: 'center' }}>
            <Button
              variant="outlined"
              startIcon={liked ? <Favorite color="error" /> : <FavoriteBorder />}
              onClick={handleLike}
              disabled={behaviorLoading}
              sx={{
                borderRadius: 2,
                '&:hover': { bgcolor: liked ? '#ffebee' : 'inherit' }
              }}
            >
              {liked ? '取消点赞' : '点赞'}
              <Typography sx={{ ml: 1 }}>{news.likeCount || 0}</Typography>
            </Button>

            <Button
              variant="outlined"
              startIcon={collected ? <Bookmark color="primary" /> : <BookmarkBorder />}
              onClick={handleCollect}
              disabled={behaviorLoading}
              sx={{
                borderRadius: 2,
                '&:hover': { bgcolor: collected ? '#e3f2fd' : 'inherit' }
              }}
            >
              {collected ? '取消收藏' : '收藏'}
              <Typography sx={{ ml: 1 }}>{news.collectCount || 0}</Typography>
            </Button>
          </Box>

          {/* 调试信息 */}
          {import.meta.env.DEV && (
            <Typography
              variant="caption"
              color="text.disabled"
              sx={{ display: 'block', mt: 4, textAlign: 'center' }}
            >
              新闻ID: {news.id || newsId} | 用户ID: {userId} | 登录状态: {isLoggedIn ? '已登录' : '未登录'}
            </Typography>
          )}
        </CardContent>
      </Paper>
      )}

      {activeTab === 'ai' && (
      <NewsAI 
        newsId={newsId}
        newsTitle={news?.title}
        isLoggedIn={isLoggedIn}
        userId={userId}
      />
    )}

      {/* 操作提示框 */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={3000}
        onClose={closeSnackbar}
        message={snackbar.message}
        sx={{
          '& .MuiSnackbarContent-root': {
            backgroundColor: snackbar.type === 'error' ? '#f44336' : snackbar.type === 'warning' ? '#ff9800' : '#4caf50',
            color: 'white',
            fontSize: '1rem'
          }
        }}
      />
    </Container>
  );
};

export default NewsDetail;