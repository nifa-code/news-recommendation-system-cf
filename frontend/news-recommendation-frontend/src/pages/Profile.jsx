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
  Favorite as FavoriteIcon,
  DeleteOutline as DeleteOutlineIcon,
  Close as CloseIcon,
  BookmarkBorder as UncollectIcon
} from '@mui/icons-material';

import {
  getUserViewHistory,
  getUserCollectList,
  getUserLikeList,
  clearViewHistory,
  cancelCollect,
  cancelLike
} from '../api/behavior';
import axiosInstance from '../utils/axiosInstance';

const Profile = () => {
  const navigate = useNavigate();
  const user = JSON.parse(localStorage.getItem('user') || '{}');

  const [activeTab, setActiveTab] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [snackbar, setSnackbar] = useState({ open: false, message: '', type: 'success' });

  const [profile, setProfile] = useState(null);
  const [loadingProfile, setLoadingProfile] = useState(false);
  const [profileError, setProfileError] = useState('');
  const [editing, setEditing] = useState(false);
  const [editForm, setEditForm] = useState({ username: '', email: '' });
  const [saving, setSaving] = useState(false);

  const [viewPage, setViewPage] = useState(1);
  const [collectPage, setCollectPage] = useState(1);
  const [likePage, setLikePage] = useState(1);

  const [viewTotalPages, setViewTotalPages] = useState(1);
  const [collectTotalPages, setCollectTotalPages] = useState(1);
  const [likeTotalPages, setLikeTotalPages] = useState(1);

  const [viewHistoryList, setViewHistoryList] = useState([]);
  const [collectList, setCollectList] = useState([]);
  const [likeList, setLikeList] = useState([]);

  const closeSnackbar = () => setSnackbar(prev => ({ ...prev, open: false }));

  const fetchProfile = async () => {
    setLoadingProfile(true);
    setProfileError('');
    try {
      const data = await axiosInstance.get('/auth/me');
      if (data?.userId) {
        setProfile(data);
      } else {
        setProfileError('获取资料失败');
      }
    } catch {
      setProfileError('登录已过期，请重新登录');
    } finally {
      setLoadingProfile(false);
    }
  };

  const handleEdit = () => {
    setEditForm({
      username: profile?.username || user.username || '',
      email: profile?.email || user.email || ''
    });
    setEditing(true);
  };
  const handleCancel = () => setEditing(false);
  const handleChange = (e) => {
    const { name, value } = e.target;
    setEditForm(prev => ({ ...prev, [name]: value }));
  };
  const handleSave = async () => {
    setSaving(true);
    try {
      await axiosInstance.put('/auth/v1/user/uprofile', editForm);
      await fetchProfile();
      setSnackbar({ open: true, message: '资料更新成功', type: 'success' });
      setEditing(false);
    } catch {
      setSnackbar({ open: true, message: '更新失败', type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const fetchViewHistory = async (pageNum = 1) => {
    try {
      setLoading(true);
      setError('');
      const res = await getUserViewHistory(pageNum, 10);
      if (res.code === 'SUCCESS') {
        setViewHistoryList(res.data.list || []);
        setViewTotalPages(res.data.totalPages || 1);
        setViewPage(pageNum);
      }
    } catch {
      setError('获取浏览历史失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchCollectList = async (pageNum = 1) => {
    try {
      setLoading(true);
      setError('');
      const res = await getUserCollectList(pageNum, 10);
      if (res.code === 'SUCCESS') {
        setCollectList(res.data.list || []);
        setCollectTotalPages(res.data.totalPages || 1);
        setCollectPage(pageNum);
      }
    } catch {
      setError('获取收藏失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchLikeList = async (pageNum = 1) => {
    try {
      setLoading(true);
      setError('');
      const res = await getUserLikeList(pageNum, 10);
      if (res.code === 'SUCCESS') {
        setLikeList(res.data.list || []);
        setLikeTotalPages(res.data.totalPages || 1);
        setLikePage(pageNum);
      }
    } catch {
      setError('获取点赞列表失败');
    } finally {
      setLoading(false);
    }
  };

  const handleClearViewHistory = async () => {
    try {
      await clearViewHistory();
      setSnackbar({ open: true, message: '清空成功', type: 'success' });
      fetchViewHistory(1);
    } catch {
      setSnackbar({ open: true, message: '清空失败', type: 'error' });
    }
  };

  const handleCancelCollect = async (newsId, e) => {
    e.stopPropagation();
    try {
      await cancelCollect(newsId);
      setSnackbar({ open: true, message: '取消收藏成功', type: 'success' });
      fetchCollectList(collectPage);
    } catch {
      setSnackbar({ open: true, message: '取消失败', type: 'error' });
    }
  };

  const handleCancelLike = async (newsId, e) => {
    e.stopPropagation();
    try {
      await cancelLike(newsId);
      setSnackbar({ open: true, message: '取消点赞成功', type: 'success' });
      fetchLikeList(likePage);
    } catch {
      setSnackbar({ open: true, message: '取消点赞失败', type: 'error' });
    }
  };

  const handleNewsClick = (newsId) => {
    navigate(`/news/${newsId}`);
  };

  const handleTabChange = (event, newValue) => {
    setActiveTab(newValue);
    if (newValue === 1) fetchViewHistory(1);
    else if (newValue === 2) fetchCollectList(1);
    else if (newValue === 3) fetchLikeList(1);
  };

  const handleViewPageChange = (e, v) => fetchViewHistory(v);
  const handleCollectPageChange = (e, v) => fetchCollectList(v);
  const handleLikePageChange = (e, v) => fetchLikeList(v);

  useEffect(() => {
    if (!localStorage.getItem('token')) navigate('/login');
  }, [navigate]);

  useEffect(() => {
    if (localStorage.getItem('token')) fetchProfile();
  }, []);

  const displayUsername = profile?.username || user.username || '用户';
  const displayInitial = displayUsername[0] || 'U';

  return (
    <Container maxWidth="md" sx={{ mt: 4, mb: 4 }}>
      <Button startIcon={<BackIcon />} onClick={() => navigate(-1)} sx={{ mb: 3 }}>
        返回
      </Button>

      <Paper sx={{ p: 4 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 4 }}>
          <Avatar sx={{ width: 80, height: 80, mr: 3 }}>
            {displayInitial}
          </Avatar>
          <Box>
            <Typography variant="h4">{displayUsername}</Typography>
            <Typography variant="body2" color="text.secondary">欢迎来到个人中心</Typography>
          </Box>
        </Box>

        <Divider sx={{ mb: 3 }} />

        <Tabs value={activeTab} onChange={handleTabChange} sx={{ mb: 3 }} variant="fullWidth">
          <Tab label="个人信息" icon={<PersonIcon />} iconPosition="start" />
          <Tab label="浏览历史" icon={<HistoryIcon />} iconPosition="start" />
          <Tab label="我的收藏" icon={<BookmarkIcon />} iconPosition="start" />
          <Tab label="我的点赞" icon={<FavoriteIcon />} iconPosition="start" />
        </Tabs>

        {activeTab === 0 && (
          <>
            {loadingProfile ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
                <CircularProgress />
              </Box>
            ) : profileError ? (
              <Alert severity="error" action={<Button size="small" onClick={fetchProfile}>重试</Button>}>{profileError}</Alert>
            ) : (
              <Box>
                <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
                  {!editing ? (
                    <Button startIcon={<EditIcon />} onClick={handleEdit} variant="outlined" size="small">编辑资料</Button>
                  ) : (
                    <Box>
                      <Button onClick={handleCancel} disabled={saving}>取消</Button>
                      <Button variant="contained" onClick={handleSave} disabled={saving} startIcon={saving ? <CircularProgress size={20} /> : <SaveIcon />}>
                        {saving ? '保存中...' : '保存'}
                      </Button>
                    </Box>
                  )}
                </Box>

                <List>
                  <ListItem>
                    <ListItemIcon><PersonIcon /></ListItemIcon>
                    {editing ? (
                      <TextField fullWidth name="username" label="用户名" value={editForm.username} onChange={handleChange} size="small" />
                    ) : (
                      <ListItemText primary="用户名" secondary={profile?.username || user.username || '未设置'} />
                    )}
                  </ListItem>
                  <ListItem>
                    <ListItemIcon><EmailIcon /></ListItemIcon>
                    {editing ? (
                      <TextField fullWidth name="email" label="邮箱" value={editForm.email} onChange={handleChange} size="small" type="email" />
                    ) : (
                      <ListItemText primary="邮箱" secondary={profile?.email || user.email || '未设置'} />
                    )}
                  </ListItem>
                </List>
              </Box>
            )}
          </>
        )}

        {activeTab === 1 && (
          <Box sx={{ width: '100%' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
              <Typography variant="h6">浏览历史</Typography>
              <Button variant="outlined" color="error" startIcon={<DeleteOutlineIcon />} onClick={handleClearViewHistory} disabled={viewHistoryList.length === 0}>清空历史</Button>
            </Box>

            {loading && <Box sx={{ textAlign: 'center' }}><CircularProgress /></Box>}
            {error && <Alert severity="error">{error}</Alert>}
            {!loading && viewHistoryList.length === 0 && <Alert severity="info">暂无浏览记录</Alert>}

            {viewHistoryList.length > 0 && (
              <>
                <Grid container spacing={2}>
                  {viewHistoryList.map((item, i) => (
                    <Grid item xs={12} key={i}>
                      <Card onClick={() => handleNewsClick(item.newsId)} sx={{ cursor: 'pointer' }}>
                        <Box sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' } }}>
                          <CardMedia component="img" sx={{ width: { sm: 150 }, height: 100 }} image={`https://picsum.photos/400/200?random=${item.newsId}`} />
                          <CardContent>
                            <Typography fontWeight="bold">{item.title}</Typography>
                            <Typography variant="body2" color="text.secondary">浏览时间：{new Date(item.behaviorTime).toLocaleString()}</Typography>
                            {item.category && <Chip label={item.category} size="small" sx={{ mt: 1 }} />}
                          </CardContent>
                        </Box>
                      </Card>
                    </Grid>
                  ))}
                </Grid>
                <Pagination count={viewTotalPages} page={viewPage} onChange={handleViewPageChange} sx={{ display: 'flex', justifyContent: 'center', mt: 3 }} />
              </>
            )}
          </Box>
        )}

        {activeTab === 2 && (
          <Box sx={{ width: '100%' }}>
            <Typography variant="h6" sx={{ mb: 2 }}>我的收藏</Typography>
            {loading && <Box sx={{ textAlign: 'center' }}><CircularProgress /></Box>}
            {error && <Alert severity="error">{error}</Alert>}
            {!loading && collectList.length === 0 && <Alert severity="info">暂无收藏</Alert>}

            {collectList.length > 0 && (
              <>
                <Grid container spacing={2}>
                  {collectList.map((item, i) => (
                    <Grid item xs={12} key={i}>
                      <Card onClick={() => handleNewsClick(item.newsId)} sx={{ cursor: 'pointer' }}>
                        <Box sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' } }}>
                          <CardMedia component="img" sx={{ width: { sm: 150 }, height: 100 }} image={`https://picsum.photos/400/200?random=${item.newsId}`} />
                          <CardContent sx={{ position: 'relative' }}>
                            <IconButton sx={{ position: 'absolute', top: 0, right: 0 }} onClick={(e) => handleCancelCollect(item.newsId, e)}>
                              <UncollectIcon color="error" />
                            </IconButton>
                            <Typography fontWeight="bold">{item.title}</Typography>
                            <Typography variant="body2" color="text.secondary">收藏时间：{new Date(item.behaviorTime).toLocaleString()}</Typography>
                            {item.category && <Chip label={item.category} size="small" sx={{ mt: 1 }} />}
                          </CardContent>
                        </Box>
                      </Card>
                    </Grid>
                  ))}
                </Grid>
                <Pagination count={collectTotalPages} page={collectPage} onChange={handleCollectPageChange} sx={{ display: 'flex', justifyContent: 'center', mt: 3 }} />
              </>
            )}
          </Box>
        )}

        {activeTab === 3 && (
          <Box sx={{ width: '100%' }}>
            <Typography variant="h6" sx={{ mb: 2 }}>我的点赞</Typography>
            {loading && <Box sx={{ textAlign: 'center' }}><CircularProgress /></Box>}
            {error && <Alert severity="error">{error}</Alert>}
            {!loading && likeList.length === 0 && <Alert severity="info">暂无点赞记录</Alert>}

            {likeList.length > 0 && (
              <>
                <Grid container spacing={2}>
                  {likeList.map((item, i) => (
                    <Grid item xs={12} key={i}>
                      <Card onClick={() => handleNewsClick(item.newsId)} sx={{ cursor: 'pointer' }}>
                        <Box sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' } }}>
                          <CardMedia component="img" sx={{ width: { sm: 150 }, height: 100 }} image={`https://picsum.photos/400/200?random=${item.newsId}`} />
                          <CardContent sx={{ position: 'relative' }}>
                            <IconButton sx={{ position: 'absolute', top: 0, right: 0, color: 'error.main' }} onClick={(e) => handleCancelLike(item.newsId, e)}>
                              <FavoriteIcon />
                            </IconButton>
                            <Typography fontWeight="bold">{item.title}</Typography>
                            <Typography variant="body2" color="text.secondary">点赞时间：{new Date(item.behaviorTime).toLocaleString()}</Typography>
                            {item.category && <Chip label={item.category} size="small" sx={{ mt: 1 }} />}
                          </CardContent>
                        </Box>
                      </Card>
                    </Grid>
                  ))}
                </Grid>
                <Pagination count={likeTotalPages} page={likePage} onChange={handleLikePageChange} sx={{ display: 'flex', justifyContent: 'center', mt: 3 }} />
              </>
            )}
          </Box>
        )}

        <Box sx={{ mt: 4, textAlign: 'center' }}>
          <Button variant="outlined" color="error" onClick={() => { localStorage.clear(); navigate('/login'); }} sx={{ mr: 2 }}>退出登录</Button>
          <Button variant="contained" onClick={() => navigate('/news')}>查看新闻推荐</Button>
        </Box>
      </Paper>

      <Snackbar open={snackbar.open} autoHideDuration={3000} onClose={closeSnackbar} message={snackbar.message}
        action={<IconButton size="small" color="inherit" onClick={closeSnackbar}><CloseIcon /></IconButton>}
        sx={{ '& .MuiSnackbarContent-root': { backgroundColor: snackbar.type === 'error' ? '#f44336' : '#4caf50' } }}
      />
    </Container>
  );
};

export default Profile;