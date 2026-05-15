import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Container,
  Paper,
  Typography,
  Box,
  Avatar,
  Button,
  Divider,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Tabs,
  Tab,
  CircularProgress,
  Alert,
  Card,
  CardMedia,
  CardContent,
  IconButton,
  Pagination,
  Snackbar,
  Grid,
  Chip,
  TextField
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  Person as PersonIcon,
  Email as EmailIcon,
  DateRange as DateIcon,
  Edit as EditIcon,
  Save as SaveIcon,
  History as HistoryIcon,
  Bookmark as BookmarkIcon,
  DeleteOutline as DeleteOutlineIcon,
  Close as CloseIcon,
  BookmarkBorder as UncollectIcon
} from '@mui/icons-material';
// 导入封装的API
import {
  getUserViewHistory,
  getUserCollectList,
  clearViewHistory,
  cancelCollect
} from '../api/behavior';
import axiosInstance from '../utils/axiosInstance';

//const defaultNewsImage = '../assets/news.jpg';
//const DEFAULT_IMAGE = "https://picsum.photos/400/200?random=news";
// 根据 news.id 生成不同图片
const Profile = () => {
  const navigate = useNavigate();
  const user = JSON.parse(localStorage.getItem('user') || '{}');

  // 标签页状态
  const [activeTab, setActiveTab] = useState(0); // 0:个人信息 1:浏览历史 2:收藏列表

  // 通用加载/错误状态
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [snackbar, setSnackbar] = useState({ open: false, message: '', type: 'success' });

  // 用户资料状态
  const [profile, setProfile] = useState(null);
  const [loadingProfile, setLoadingProfile] = useState(false);
  const [profileError, setProfileError] = useState('');

  // 编辑状态
  const [editing, setEditing] = useState(false);
  const [editForm, setEditForm] = useState({ username: '', email: '' });
  const [saving, setSaving] = useState(false);

  // 分页状态
  const [viewPage, setViewPage] = useState(1);
  const [collectPage, setCollectPage] = useState(1);
  const [viewTotalPages, setViewTotalPages] = useState(1);
  const [collectTotalPages, setCollectTotalPages] = useState(1);

  // 数据列表
  const [viewHistoryList, setViewHistoryList] = useState([]);
  const [collectList, setCollectList] = useState([]);

  const closeSnackbar = () => setSnackbar(prev => ({ ...prev, open: false }));

  // ========== 获取用户资料 ==========
const fetchProfile = async () => {
  setLoadingProfile(true);
  setProfileError('');
  try {
    const data = await axiosInstance.get('/auth/me');

    console.log("用户信息：", data); // 控制台能看到userId、username等

    if (data?.userId) {
      setProfile(data);
    } else {
      setProfileError('获取资料失败');
    }
  } catch (err) {
    console.error('获取用户资料失败', err);
    setProfileError('登录已过期，请重新登录');
  } finally {
    setLoadingProfile(false);
  }
};
  // ========== 编辑相关函数 ==========
  const handleEdit = () => {
    setEditForm({
      username: profile?.username || '',
      email: profile?.email || ''
    });
    setEditing(true);
  };

  const handleCancel = () => {
    setEditing(false);
    setEditForm({ username: '', email: '' });
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setEditForm(prev => ({ ...prev, [name]: value }));
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      await axiosInstance.put('/auth/v1/user/uprofile', editForm);
      await fetchProfile(); // 重新获取最新资料
      setSnackbar({ open: true, message: '资料更新成功', type: 'success' });
      setEditing(false);
    } catch (err) {
      console.error('更新资料失败', err);
      setSnackbar({ open: true, message: '网络错误，更新失败', type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  // ========== 获取浏览历史 ==========
  const fetchViewHistory = async (pageNum = 1) => {
    try {
      setLoading(true);
      setError('');
      const response = await getUserViewHistory(pageNum, 10);
      if (response.code === 'SUCCESS' || response.code === 'success') {
        setViewHistoryList(response.data.list || []);
        setViewTotalPages(response.data.totalPages || 1);
        setViewPage(pageNum);
      } else {
        setError(response.message || '获取浏览历史失败');
      }
    } catch (err) {
      setError('网络错误，无法获取浏览历史');
      console.error('获取浏览历史失败:', err);
    } finally {
      setLoading(false);
    }
  };

  // ========== 获取收藏列表 ==========
  const fetchCollectList = async (pageNum = 1) => {
    try {
      setLoading(true);
      setError('');
      const response = await getUserCollectList(pageNum, 10);
      if (response.code === 'SUCCESS' || response.code === 'success') {
        setCollectList(response.data.list || []);
        setCollectTotalPages(response.data.totalPages || 1);
        setCollectPage(pageNum);
      } else {
        setError(response.message || '获取收藏列表失败');
      }
    } catch (err) {
      setError('网络错误，无法获取收藏列表');
      console.error('获取收藏列表失败:', err);
    } finally {
      setLoading(false);
    }
  };




  // ========== 清空浏览历史 ==========
  const handleClearViewHistory = async () => {
    try {
      const response = await clearViewHistory();
      if (response.code === 'SUCCESS' || response.code === 'success') {
        setSnackbar({ open: true, message: '浏览历史清空成功', type: 'success' });
        fetchViewHistory(1);
      } else {
        setSnackbar({ open: true, message: response.message || '清空失败', type: 'error' });
      }
    } catch (err) {
      setSnackbar({ open: true, message: '网络错误，清空失败', type: 'error' });
      console.error('清空浏览历史失败:', err);
    }
  };

  // ========== 取消收藏 ==========
  const handleCancelCollect = async (newsId, e) => {
    e.stopPropagation();
    try {
      const response = await cancelCollect(newsId);
      if (response.code === 'SUCCESS' || response.code === 'success') {
        setSnackbar({ open: true, message: '取消收藏成功', type: 'success' });
        fetchCollectList(collectPage);
      } else {
        setSnackbar({ open: true, message: response.message || '取消收藏失败', type: 'error' });
      }
    } catch (err) {
      setSnackbar({ open: true, message: '网络错误，取消收藏失败', type: 'error' });
      console.error('取消收藏失败:', err);
    }
  };

  // ========== 新闻卡片点击跳转 ==========
  const handleNewsClick = (newsId) => {
    navigate(`/news/${newsId}`);
  };

  // ========== 标签页切换 ==========
  const handleTabChange = (event, newValue) => {
    setActiveTab(newValue);
    if (newValue === 1) {
      fetchViewHistory(1);
    } else if (newValue === 2) {
      fetchCollectList(1);
    }
  };

  // ========== 分页切换 ==========
  const handleViewPageChange = (event, value) => {
    fetchViewHistory(value);
  };
  const handleCollectPageChange = (event, value) => {
    fetchCollectList(value);
  };

  // ========== 初始化 ==========
  useEffect(() => {
    if (!localStorage.getItem('token')) {
      navigate('/login');
    }
  }, [navigate]);

  useEffect(() => {
    if (localStorage.getItem('token')) {
      fetchProfile();
    }
  }, []);

  return (
    <Container maxWidth="md" sx={{ mt: 4, mb: 4 }}>
      <Button startIcon={<BackIcon />} onClick={() => navigate(-1)} sx={{ mb: 3 }}>
        返回
      </Button>

      <Paper sx={{ p: 4 }}>
        {/* 个人信息头部 */}
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 4 }}>
          <Avatar sx={{ width: 80, height: 80, mr: 3 }}>
            {profile?.username?.[0] || user?.username?.[0] || 'U'}
          </Avatar>
          <Box>
            <Typography variant="h4">
              {profile?.username || user?.username || '用户'}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              欢迎来到个人中心
            </Typography>
          </Box>
        </Box>

        <Divider sx={{ mb: 3 }} />

        {/* 标签页切换 */}
        <Tabs value={activeTab} onChange={handleTabChange} sx={{ mb: 3 }} variant="fullWidth">
          <Tab label="个人信息" icon={<PersonIcon />} iconPosition="start" />
          <Tab label="浏览历史" icon={<HistoryIcon />} iconPosition="start" />
          <Tab label="我的收藏" icon={<BookmarkIcon />} iconPosition="start" />
        </Tabs>

        {/* 1. 个人信息标签页 */}
        {activeTab === 0 && (
          <>
            {loadingProfile ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
                <CircularProgress />
              </Box>
            ) : profileError ? (
              <Alert 
                severity="error" 
                sx={{ mb: 2 }}
                action={
                  <Button color="inherit" size="small" onClick={fetchProfile}>
                    重试
                  </Button>
                }
              >
                {profileError}
              </Alert>
            ) : profile ? (
              <Box>
                {/* 编辑/取消按钮 */}
                <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
                  {!editing ? (
                    <Button startIcon={<EditIcon />} onClick={handleEdit} variant="outlined" size="small">
                      编辑资料
                    </Button>
                  ) : (
                    <Box>
                      <Button onClick={handleCancel} sx={{ mr: 1 }} disabled={saving}>
                        取消
                      </Button>
                      <Button 
                        variant="contained" 
                        onClick={handleSave} 
                        disabled={saving}
                        startIcon={saving ? <CircularProgress size={20} /> : <SaveIcon />}
                      >
                        {saving ? '保存中...' : '保存'}
                      </Button>
                    </Box>
                  )}
                </Box>

                <List>
                  <ListItem>
                    <ListItemIcon><PersonIcon /></ListItemIcon>
                    {editing ? (
                      <TextField
                        fullWidth
                        name="username"
                        label="用户名"
                        value={editForm.username}
                        onChange={handleChange}
                        size="small"
                      />
                    ) : (
                      <ListItemText primary="用户名" secondary={profile.username || '未设置'} />
                    )}
                  </ListItem>
                  
                  <ListItem>
                    <ListItemIcon><EmailIcon /></ListItemIcon>
                    {editing ? (
                      <TextField
                        fullWidth
                        name="email"
                        label="邮箱"
                        value={editForm.email}
                        onChange={handleChange}
                        size="small"
                        type="email"
                      />
                    ) : (
                      <ListItemText primary="邮箱" secondary={profile.email || '未设置'} />
                    )}
                  </ListItem>
                  
                  <ListItem>
                    <ListItemIcon><DateIcon /></ListItemIcon>
                    <ListItemText 
                      primary="注册时间" 
                      secondary={profile.createdAt ? new Date(profile.createdAt).toLocaleString() : '未知'} 
                    />
                  </ListItem>
                  
                  {profile.lastLogin && (
                    <ListItem>
                      <ListItemIcon><HistoryIcon /></ListItemIcon>
                      <ListItemText 
                        primary="上次登录" 
                        secondary={new Date(profile.lastLogin).toLocaleString()} 
                      />
                    </ListItem>
                  )}
                </List>
              </Box>
            ) : (
              <Alert severity="warning">无法加载用户资料，请刷新页面</Alert>
            )}
          </>
        )}

        {/* 2. 浏览历史标签页 */}
        {activeTab === 1 && (
          <Box sx={{ width: '100%' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
              <Typography variant="h6">浏览历史</Typography>
              <Button
                variant="outlined"
                color="error"
                startIcon={<DeleteOutlineIcon />}
                onClick={handleClearViewHistory}
                disabled={loading || viewHistoryList.length === 0}
              >
                清空历史
              </Button>
            </Box>

            {loading && (
              <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
                <CircularProgress />
              </Box>
            )}

            {error && !loading && (
              <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>
            )}

            {!loading && !error && viewHistoryList.length === 0 && (
              <Alert severity="info" sx={{ mb: 2 }}>暂无浏览历史</Alert>
            )}

            {!loading && viewHistoryList.length > 0 && (
              <>
                <Grid container spacing={2} sx={{ mb: 3 }}>
                  {viewHistoryList.map((item, index) => (
                    <Grid item xs={12} key={index}>
                      <Card 
                        sx={{ 
                          cursor: 'pointer',
                          transition: 'transform 0.2s',
                          '&:hover': { transform: 'translateY(-2px)' }
                        }}
                        onClick={() => handleNewsClick(item.newsId)}
                      >
                        <Box sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' } }}>
                          <CardMedia
                            component="img"
                            sx={{ 
                              width: { xs: '100%', sm: 150 },
                              height: { xs: 120, sm: 100 },
                              objectFit: 'cover'
                            }}
                            //image={item.coverImageUrl || item.imageUrl || DEFAULT_IMAGE}
                            image={item.coverImageUrl || item.imageUrl || `https://picsum.photos/400/200?random=${item.newsId}`}
                            alt={item.title}
                            onError={(e) => {
  e.target.src = `https://picsum.photos/400/200?random=${item.newsId}`;
}}
                            //onError={(e) => e.target.src = DEFAULT_IMAGE}
                          />
                          <CardContent sx={{ flex: 1 }}>
                            <Typography variant="subtitle1" fontWeight="bold" noWrap>
                              {item.title}
                            </Typography>
                            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                              浏览时间: {new Date(item.collectTime).toLocaleString()}
                            </Typography>
                            {item.category && (
                              <Chip label={item.category} size="small" sx={{ mt: 1 }} variant="outlined" />
                            )}
                          </CardContent>
                        </Box>
                      </Card>
                    </Grid>
                  ))}
                </Grid>

                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
                  <Pagination 
                    count={viewTotalPages} 
                    page={viewPage} 
                    onChange={handleViewPageChange}
                    color="primary"
                  />
                </Box>
              </>
            )}
          </Box>
        )}

        {/* 3. 收藏列表标签页 */}
        {activeTab === 2 && (
          <Box sx={{ width: '100%' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
              <Typography variant="h6">我的收藏</Typography>
            </Box>

            {loading && (
              <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
                <CircularProgress />
              </Box>
            )}

            {error && !loading && (
              <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>
            )}

            {!loading && !error && collectList.length === 0 && (
              <Alert severity="info" sx={{ mb: 2 }}>暂无收藏内容</Alert>
            )}

            {!loading && collectList.length > 0 && (
              <>
                <Grid container spacing={2} sx={{ mb: 3 }}>
                  {collectList.map((item, index) => (
                    <Grid item xs={12} key={index}>
                      <Card 
                        sx={{ 
                          cursor: 'pointer',
                          transition: 'transform 0.2s',
                          '&:hover': { transform: 'translateY(-2px)' }
                        }}
                        onClick={() => handleNewsClick(item.newsId)}
                      >
                        <Box sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' } }}>
                          <CardMedia
                            component="img"
                            sx={{ 
                              width: { xs: '100%', sm: 150 },
                              height: { xs: 120, sm: 100 },
                              objectFit: 'cover'
                            }}
                            //image={item.coverImageUrl || item.imageUrl || DEFAULT_IMAGE}
                            image={item.coverImageUrl || item.imageUrl || `https://picsum.photos/400/200?random=${item.newsId}`}
                            alt={item.title}
                            onError={(e) => {
                              e.target.src = `https://picsum.photos/400/200?random=${item.newsId}`;
                            }}
                            //onError={(e) => e.target.src = DEFAULT_IMAGE}
                          />
                          <CardContent sx={{ flex: 1, position: 'relative' }}>
                            <IconButton
                              sx={{ 
                                position: 'absolute', 
                                top: 0, 
                                right: 0,
                                color: 'error.main'
                              }}
                              onClick={(e) => handleCancelCollect(item.newsId, e)}
                            >
                              <UncollectIcon />
                            </IconButton>
                            
                            <Typography variant="subtitle1" fontWeight="bold" noWrap>
                              {item.title}
                            </Typography>
                            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                              收藏时间: {new Date(item.behaviorTime).toLocaleString()}
                            </Typography>
                            {item.category && (
                              <Chip label={item.category} size="small" sx={{ mt: 1 }} variant="outlined" />
                            )}
                          </CardContent>
                        </Box>
                      </Card>
                    </Grid>
                  ))}
                </Grid>

                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
                  <Pagination 
                    count={collectTotalPages} 
                    page={collectPage} 
                    onChange={handleCollectPageChange}
                    color="primary"
                  />
                </Box>
              </>
            )}
          </Box>
        )}

        {/* 底部操作按钮 */}
        <Box sx={{ mt: 4, textAlign: 'center' }}>
          <Button
            variant="outlined"
            color="error"
            onClick={() => {
              localStorage.clear();
              navigate('/login');
            }}
            sx={{ mr: 2 }}
          >
            退出登录
          </Button>
          <Button
            variant="contained"
            onClick={() => navigate('/news')}
          >
            查看新闻推荐
          </Button>
        </Box>
      </Paper>

      {/* 操作提示弹窗 */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={3000}
        onClose={closeSnackbar}
        message={snackbar.message}
        action={
          <IconButton size="small" color="inherit" onClick={closeSnackbar}>
            <CloseIcon fontSize="small" />
          </IconButton>
        }
        sx={{
          '& .MuiSnackbarContent-root': {
            backgroundColor: snackbar.type === 'error' ? '#f44336' : '#4caf50',
            color: 'white'
          }
        }}
      />
    </Container>
  );
};

export default Profile;
