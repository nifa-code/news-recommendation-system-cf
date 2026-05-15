// src/pages/NewsRecommendation.jsx
// import React, { useState, useEffect, useCallback } from 'react';
// import { 
//   Box, Typography, Button, Alert, CircularProgress, Container 
// } from '@mui/material';
// import { useNavigate } from 'react-router-dom'; 
// import axiosInstance from '../utils/axiosInstance';
// import NewsCardGrid from '../components/NewsCardGrid'; // 使用统一的网格组件

// const NewsRecommendation = () => {
//   // 状态定义
//   const [newsList, setNewsList] = useState([]);
//   const [loading, setLoading] = useState(false);
//   const [error, setError] = useState('');
//   const [isHotNews, setIsHotNews] = useState(false);
//   const navigate = useNavigate();

//   // 加载热门新闻
//   const loadHotRecommendations = useCallback(async () => {
//     try {
//       const response = await axiosInstance.get('/v1/recommend/hot', {
//         params: { count: 40 }
//       });
      
//       setNewsList(response?.recommendations || []);
//       setIsHotNews(true);
//     } catch(err) {
//       console.error('加载热门新闻失败:', err);
//       setError('热门新闻加载失败，请刷新页面');
//       setNewsList([]);
//     }
//   }, []);

//   // 加载个性化推荐
//   const loadPersonalizedRecommendations = useCallback(async () => {
//     try {
//       setLoading(true);
//       setError('');
      
//       const response = await axiosInstance.get('/v1/recommend', {
//         params: { count: 20 }
//       });

//       const { recommendations } = response || {};

//       if (recommendations && recommendations.length > 0) {
//         setNewsList(recommendations);
//         setIsHotNews(false);
//       } else {
//         setError('暂无个性化推荐，为您展示热门新闻');
//         await loadHotRecommendations();
//       }
//     } catch (err) {
//       console.error("获取个性化推荐失败", err);
//       const errorMsg = `个性化推荐加载失败（${err.response?.status || '网络错误'}），已展示热门新闻`;
//       setError(errorMsg);
//       await loadHotRecommendations();
//     } finally {
//       setLoading(false);
//     }
//   }, [loadHotRecommendations]);

//   // 初始化加载
//   useEffect(() => {
//     const token = localStorage.getItem('token');
//     if (!token) {
//       navigate('/login');
//       return;
//     }
//     loadPersonalizedRecommendations();
//   }, [navigate, loadPersonalizedRecommendations]);

//   // 刷新推荐
//   const handleRefresh = useCallback(async () => {
//     try {
//       setLoading(true);
//       await axiosInstance.post('/v1/recommend/refresh');
//       await loadPersonalizedRecommendations();
//       setError('');
//     } catch (err) {
//       const errMsg = `刷新失败：${err.response?.data?.message || '请稍后再试'}`;
//       setError(errMsg);
//     } finally {
//       setLoading(false);
//     }
//   }, [loadPersonalizedRecommendations]);

//   // 处理卡片点击
//   const handleCardClick = (newsId) => {
//     navigate(`/news/${newsId}`);
//   };

//   return (
//     <Container maxWidth="xl" sx={{ py: 3 }}>
//       {/* 页面标题和刷新按钮 */}
//       <Box sx={{ 
//         display: 'flex', 
//         justifyContent: 'space-between', 
//         alignItems: 'center', 
//         mb: 4,
//         px: { xs: 0, sm: 2 }
//       }}>
//         <Typography variant="h4" sx={{ fontWeight: 600 }}>
//           {isHotNews ? '🔥 热门新闻' : '🎯 为您推荐'}
//         </Typography>
//         <Button 
//           variant="outlined" 
//           onClick={handleRefresh} 
//           disabled={loading}
//           sx={{ height: 'fit-content' }}
//         >
//           {loading ? '加载中...' : '刷新推荐'}
//         </Button>
//       </Box>

//       {/* 加载状态 */}
//       {loading && (
//         <Box sx={{ display: 'flex', justifyContent: 'center', my: 8 }}>
//           <CircularProgress />
//         </Box>
//       )}

//       {/* 错误提示 */}
//       {error && !loading && (
//         <Alert severity="warning" sx={{ mb: 4, mx: { xs: 0, sm: 2 } }}>
//           {error}
//         </Alert>
//       )}

//       {/* 新闻列表 - 使用统一的 NewsCardGrid 组件 */}
//       {!loading && (
//         <NewsCardGrid 
//           newsList={newsList}
//           onCardClick={handleCardClick}
//           emptyMessage="暂无新闻数据"
//           gridConfig={{
//             xs: 12,    // 手机：1列
//             sm: 6,     // 平板：2列
//             md: 4,     // 桌面：3列
//             lg: 3,     // 大屏：4列
//             xl: 2.4    // 超大屏：5列
//           }}
//         />
//       )}
//     </Container>
//   );
// };

// export default NewsRecommendation;

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { 
  Box, Typography, Button, Alert, CircularProgress, Container,
  TextField, InputAdornment, IconButton, Chip
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import ClearIcon from '@mui/icons-material/Clear';
import FilterListIcon from '@mui/icons-material/FilterList';
import { useNavigate } from 'react-router-dom';
import axiosInstance from '../utils/axiosInstance';
import NewsCardGrid from '../components/NewsCardGrid'; // 保留原有组件

const NewsRecommendation = () => {
  // 保留你原本的核心状态（仅新增搜索/分类相关状态）
  const [newsList, setNewsList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [isHotNews, setIsHotNews] = useState(false);
  const [searchQuery, setSearchQuery] = useState(''); // 新增
  const [showByCategory, setShowByCategory] = useState(false); // 新增
  const navigate = useNavigate();

  // 新增：搜索过滤（增加空值保护）
  const filteredNews = useMemo(() => {
    if (!searchQuery.trim() || !Array.isArray(newsList)) return newsList;
    
    const query = searchQuery.toLowerCase();
    return newsList.filter(news => {
      if (!news) return false; // 空值保护
      return (
        (news.title && news.title.toLowerCase().includes(query)) ||
        (news.abstractText && news.abstractText.toLowerCase().includes(query)) ||
        (news.category && news.category.toLowerCase().includes(query))
      );
    });
  }, [newsList, searchQuery]);

  // 新增：分类分组（增加空值保护）
  const groupedNews = useMemo(() => {
    if (!showByCategory || !Array.isArray(filteredNews)) return null;
    
    const grouped = {};
    filteredNews.forEach(news => {
      if (!news) return; // 空值保护
      const category = news.category || '未分类';
      if (!grouped[category]) grouped[category] = [];
      grouped[category].push(news);
    });
    return grouped;
  }, [filteredNews, showByCategory]);

  // 新增：分类列表
  //const categories = useMemo(() => {
    //if (!groupedNews) return [];
    //return Object.keys(groupedNews).sort();
  //}, [groupedNews]);

  // 新增 所有分类标签
  const allCategories = useMemo(() => {
    if (!Array.isArray(newsList)) return [];
    const uniqueCats = [...new Set(newsList.map(n => n?.category).filter(Boolean))];
    return uniqueCats.slice(0, 8);
  }, [newsList]);
  const loadHotRecommendations = useCallback(async () => {
    try {
      const response = await axiosInstance.get('/v1/recommend/hot', {
        params: { count: 40 }
      });
      
      setNewsList(response?.recommendations || []);
      setIsHotNews(true);
    } catch(err) {
      console.error('加载热门新闻失败:', err);
      setError('热门新闻加载失败，请刷新页面');
      setNewsList([]);
    }
  }, []);

  const loadPersonalizedRecommendations = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      
      const response = await axiosInstance.get('/v1/recommend', {
        params: { count: 20 }
      });

      const { recommendations } = response || {};

      if (recommendations && recommendations.length > 0) {
        setNewsList(recommendations);
        setIsHotNews(false);
      } else {
        setError('暂无个性化推荐，为您展示热门新闻');
        await loadHotRecommendations();
      }
    } catch (err) {
      console.error("获取个性化推荐失败", err);
      const errorMsg = `个性化推荐加载失败（${err.response?.status || '网络错误'}），已展示热门新闻`;
      setError(errorMsg);
      await loadHotRecommendations();
    } finally {
      setLoading(false);
    }
  }, [loadHotRecommendations]);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return;
    }
    loadPersonalizedRecommendations();
  }, [navigate, loadPersonalizedRecommendations]);

  const handleRefresh = useCallback(async () => {
    try {
      setLoading(true);
      await axiosInstance.post('/v1/recommend/refresh');
      await loadPersonalizedRecommendations();
      setError('');
      setSearchQuery(''); // 新增：刷新清空搜索
    } catch (err) {
      const errMsg = `刷新失败：${err.response?.data?.message || '请稍后再试'}`;
      setError(errMsg);
    } finally {
      setLoading(false);
    }
  }, [loadPersonalizedRecommendations]);

  const handleCardClick = (newsId) => {
    navigate(`/news/${newsId}`);
  };

  // 新增：搜索相关方法
  const handleSearchChange = (e) => {
    setSearchQuery(e.target.value);
  };
  const handleClearSearch = () => {
    setSearchQuery('');
  };
  const handleCategoryClick = (category) => {
    setSearchQuery(category);
    setShowByCategory(true);
  };

  return (
    <Container maxWidth="xl" sx={{ py: 3 }}>
      {/* 保留你原本的标题区域（仅新增分类按钮） */}
      <Box sx={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center', 
        mb: 4,
        px: { xs: 0, sm: 2 },
        flexWrap: 'wrap',
        gap: 2
      }}>
        <Typography variant="h4" sx={{ fontWeight: 600 }}>
          {isHotNews ? '热门新闻' : '为您推荐'}
        </Typography>
        <Box sx={{ display: 'flex', gap: 1 }}>
          {/* 新增：分类显示按钮 */}
          <Button
            variant={showByCategory ? "contained" : "outlined"}
            startIcon={<FilterListIcon />}
            onClick={() => setShowByCategory(!showByCategory)}
            size="small"
          >
            {showByCategory ? '合并显示' : '分类显示'}
          </Button>
          {/* 保留原本的刷新按钮 */}
          <Button 
            variant="outlined" 
            onClick={handleRefresh} 
            disabled={loading}
            sx={{ height: 'fit-content' }}
          >
            {loading ? '加载中...' : '刷新推荐'}
          </Button>
        </Box>
      </Box>

      {/* 新增：搜索框区域（完全兼容原有逻辑） */}
      <Box sx={{ mb: 4, px: { xs: 0, sm: 2 } }}>
        <TextField
          fullWidth
          variant="outlined"
          placeholder="搜索新闻标题、摘要或分类..."
          value={searchQuery}
          onChange={handleSearchChange}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
            endAdornment: searchQuery && (
              <InputAdornment position="end">
                <IconButton onClick={handleClearSearch} edge="end" size="small">
                  <ClearIcon />
                </IconButton>
              </InputAdornment>
            ),
          }}
        />
        {/* 分类标签（有数据时显示） */}
        {!loading && newsList.length > 0 && !searchQuery && (
          <Box sx={{ mt: 1, display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
            {allCategories.map(category => (
              <Chip
                key={category}
                label={category}
                size="small"
                onClick={() => handleCategoryClick(category)}
                sx={{ cursor: 'pointer' }}
              />
            ))}
          </Box>
        )}
      </Box>

      {/* 保留你原本的加载/错误提示 */}
      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', my: 8 }}>
          <CircularProgress />
        </Box>
      )}
      {error && !loading && (
        <Alert severity="warning" sx={{ mb: 4, mx: { xs: 0, sm: 2 } }}>
          {error}
        </Alert>
      )}

      {/* 新闻列表：兼容原有 Grid 逻辑 + 新增分类显示 */}
      {!loading && (
        <>
          {showByCategory && groupedNews && Object.keys(groupedNews).length > 0 ? (
            // 分类显示模式（兼容原有 Grid 配置）
            Object.keys(groupedNews).map(category => (
              <Box key={category} sx={{ mb: 4, px: { xs: 0, sm: 2 } }}>
                <Typography variant="h6" sx={{ mb: 2, pl: 2, borderLeft: '4px solid #1976d2' }}>
                  {category}（{groupedNews[category].length}篇）
                </Typography>
                <NewsCardGrid 
                  newsList={groupedNews[category]}
                  onCardClick={handleCardClick}
                  emptyMessage="暂无该分类新闻"
                  gridConfig={{
                    xs: 12, sm: 6, md: 4, lg: 3, xl: 2.4
                  }}
                  cardVariant="compact" // 传递紧凑模式（缩小卡片）
                />
              </Box>
            ))
          ) : (
            // 保留你原本的合并显示模式
            <NewsCardGrid 
              newsList={filteredNews}
              onCardClick={handleCardClick}
              emptyMessage={searchQuery ? `未找到包含"${searchQuery}"的新闻` : "暂无新闻数据"}
              gridConfig={{
                xs: 12, sm: 6, md: 4, lg: 3, xl: 2.4
              }}
              cardVariant="compact" // 传递紧凑模式（缩小卡片）
            />
          )}
        </>
      )}
    </Container>
  );
};

export default NewsRecommendation;