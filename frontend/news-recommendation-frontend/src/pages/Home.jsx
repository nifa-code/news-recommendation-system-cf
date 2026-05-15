import React, { useState} from 'react'; // 添加 useState 和 useEffect
import Cube from '../components/Cube.jsx';
import { 
  Container, 
  Typography, 
  Box, 
  Button, 
  Grid, 
  Paper,
  Fade,
  Zoom,
  IconButton,
  Menu,
  MenuItem,
  Avatar,
  Tooltip,
  Divider // 添加 Divider 组件
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import {
  Timeline,
  TrendingUp,
  Palette,
  Science,
  Lightbulb,
  Security,
  AccountCircle,
  Logout,
  Settings
} from '@mui/icons-material';

const Home = () => {
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState(null); // 用户菜单状态
  //const [userName, setUserName] = useState('用户'); // 用户名称
const [userName] = useState(() => {
    const userData = localStorage.getItem('user');
    if (userData) {
      try {
        const user = JSON.parse(userData);
        return user.name || user.username || '用户';
      } catch (err) {
        console.error('解析用户信息失败:', err);
        return '用户';
      }
    }
    return '用户';
  });


  // // 获取用户信息 - 优化版本
  // useEffect(() => {
  //   // 从localStorage获取用户信息
  //   const userData = localStorage.getItem('user');
  //   if (userData) {
  //     try {
  //       const user = JSON.parse(userData);
  //       // 只在用户信息确实存在且不同时更新
  //       const newName = user.name || user.username || '用户';
  //       if (newName !== userName) {
  //         setUserName(newName);
  //       }
  //     } catch (err) {
  //       console.error('解析用户信息失败:', err);
  //     }
  //   }
  // }, [userName]); // 添加依赖项

  // 登出函数
  const handleLogout = () => {
    // 清除认证信息
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('user');
    // 关闭菜单
    handleCloseMenu();
    // 跳转到登录页
    navigate('/login');
  };

  // 用户菜单相关函数
  const handleOpenMenu = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleCloseMenu = () => {
    setAnchorEl(null);
  };

  // 导航到新闻推荐页
  const goToNews = () => {
    navigate('/news');
  };

  // 导航到个人资料页
  const goToProfile = () => {
    navigate('/profile'); // 需要创建Profile页面
  };
  return (
    <Box
      sx={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #2c3e50 0%, #4a6491 50%, #1a2530 100%)',
        color: '#fff',
        pt: 8,
        pb: 12,
        position: 'relative',
        overflow: 'visible',
        '&::before': {
          content: '""',
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'radial-gradient(circle at 20% 30%, rgba(64, 115, 158, 0.1) 0%, transparent 50%)',
          zIndex: 1,
        }
      }}
    >
      <Cube 
      size={100} 
      rotationSpeed={20} 
      top="50px" 
      right="50px" 
      zIndex={2} 
      opacity={0.7}
    />
      {/* 用户菜单和登出按钮 - 固定在右上角 */}
      <Box sx={{ 
        position: 'fixed', 
        top: 20, 
        right: 20, 
        zIndex: 10,
        display: 'flex',
        gap: 1,
        alignItems: 'center'
      }}>
        {/* 新闻推荐按钮 */}
        <Tooltip title="新闻推荐">
          <Button
            variant="outlined"
            onClick={goToNews}
            sx={{
              color: '#fff',
              borderColor: 'rgba(255, 255, 255, 0.3)',
              borderRadius: '20px',
              px: 2,
              py: 1,
              '&:hover': {
                borderColor: '#4fc3f7',
                backgroundColor: 'rgba(79, 195, 247, 0.1)',
              }
            }}
          >
            新闻推荐
          </Button>
        </Tooltip>

        {/* 用户头像和菜单 */}
        <Tooltip title="用户菜单">
          <IconButton
            onClick={handleOpenMenu}
            sx={{
              backgroundColor: 'rgba(255, 255, 255, 0.1)',
              border: '2px solid rgba(255, 255, 255, 0.2)',
              color: '#fff',
              '&:hover': {
                backgroundColor: 'rgba(255, 255, 255, 0.2)',
              }
            }}
          >
            <AccountCircle />
          </IconButton>
        </Tooltip>
      </Box>

      {/* 用户下拉菜单 */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleCloseMenu}
        PaperProps={{
          sx: {
            mt: 1.5,
            backgroundColor: 'rgba(30, 40, 60, 0.95)',
            backdropFilter: 'blur(10px)',
            border: '1px solid rgba(255, 255, 255, 0.1)',
            color: '#fff',
            minWidth: 180,
          }
        }}
      >
        <MenuItem 
          disabled
          sx={{
            opacity: 0.8,
            '&:hover': {
              backgroundColor: 'transparent',
            }
          }}
        >
          <AccountCircle sx={{ mr: 1, fontSize: 20 }} />
          欢迎，{userName}
        </MenuItem>
        <Divider sx={{ my: 0.5, backgroundColor: 'rgba(255, 255, 255, 0.1)' }} />
        <MenuItem onClick={goToProfile}>
          <AccountCircle sx={{ mr: 1, fontSize: 20 }} />
          个人资料
        </MenuItem>
        <MenuItem onClick={goToNews}>
          <TrendingUp sx={{ mr: 1, fontSize: 20 }} />
          新闻推荐
        </MenuItem>
        <MenuItem onClick={handleCloseMenu}>
          <Settings sx={{ mr: 1, fontSize: 20 }} />
          设置
        </MenuItem>
        <Divider sx={{ my: 0.5, backgroundColor: 'rgba(255, 255, 255, 0.1)' }} />
        <MenuItem 
          onClick={handleLogout}
          sx={{
            color: '#ff6b6b',
            '&:hover': {
              backgroundColor: 'rgba(255, 107, 107, 0.1)',
            }
          }}
        >
          <Logout sx={{ mr: 1, fontSize: 20 }} />
          退出登录
        </MenuItem>
      </Menu>
      
      {/* 装饰元素 */}
      <Box
        sx={{
          position: 'absolute',
          top: '10%',
          right: '10%',
          width: 300,
          height: 300,
          background: 'radial-gradient(circle, rgba(64, 115, 158, 0.2) 0%, transparent 70%)',
          borderRadius: '50%',
          filter: 'blur(40px)',
          zIndex: 1,
        }}
      />
      
      <Box
        sx={{
          position: 'absolute',
          bottom: '20%',
          left: '10%',
          width: 200,
          height: 200,
          background: 'radial-gradient(circle, rgba(86, 149, 199, 0.15) 0%, transparent 70%)',
          borderRadius: '50%',
          filter: 'blur(30px)',
          zIndex: 1,
        }}
      />

      <Container maxWidth="lg" sx={{ position: 'relative', zIndex: 2 }}>
        <Fade in timeout={1000}>
          <Box sx={{ textAlign: 'center', mb: 8 }}>
            <Typography 
              variant="h2" 
              gutterBottom 
              sx={{ 
                fontWeight: 800,
                background: 'linear-gradient(90deg, #4fc3f7 0%, #2979ff 100%)',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
                mb: 2,
                letterSpacing: '0.1em',
              }}
            >
              AI新闻推荐系统
            </Typography>
            
            <Typography 
              variant="h5" 
              sx={{ 
                color: 'rgba(255, 255, 255, 0.85)',
                mb: 4,
                maxWidth: 800,
                mx: 'auto',
                fontWeight: 300,
              }}
            >
              基于改进的协同过滤推荐算法 · 为您量身定制新闻阅读体验
            </Typography>
            
            <Zoom in timeout={1200} style={{ transitionDelay: '300ms' }}>
              <Box sx={{ display: 'flex', justifyContent: 'center', gap: 3, mt: 6 }}>
                <Button
                  variant="contained"
                  size="large"
                  onClick={goToNews}
                  sx={{
                    background: 'linear-gradient(45deg, #2979ff 0%, #4fc3f7 100%)',
                    color: '#fff',
                    px: 6,
                    py: 1.5,
                    borderRadius: '50px',
                    fontSize: '1.1rem',
                    fontWeight: 600,
                    boxShadow: '0 8px 32px rgba(41, 121, 255, 0.3)',
                    transition: 'all 0.3s ease',
                    '&:hover': {
                      transform: 'translateY(-3px)',
                      boxShadow: '0 12px 40px rgba(41, 121, 255, 0.4)',
                    }
                  }}
                >
                  <Lightbulb sx={{ mr: 1 }} />
                  开始阅读
                </Button>
                
                <Button
                  variant="outlined"
                  size="large"
                  onClick={handleLogout}
                  sx={{
                    borderColor: 'rgba(255, 255, 255, 0.3)',
                    color: '#fff',
                    px: 6,
                    py: 1.5,
                    borderRadius: '50px',
                    fontSize: '1.1rem',
                    fontWeight: 600,
                    transition: 'all 0.3s ease',
                    '&:hover': {
                      borderColor: '#2979ff',
                      background: 'rgba(41, 121, 255, 0.1)',
                      transform: 'translateY(-3px)',
                    }
                  }}
                >
                  <Security sx={{ mr: 1 }} />
                  退出登录
                </Button>
              </Box>
            </Zoom>
          </Box>
        </Fade>

        <Grid container spacing={4} sx={{ mt: 8 }}>
          {[
            {
              icon: <Timeline sx={{ fontSize: 40, color: '#4fc3f7' }} />,
              title: '智能算法',
              desc: '基于协同过滤与深度学习模型，精准预测用户兴趣',
              gradient: 'linear-gradient(135deg, rgba(79, 195, 247, 0.1) 0%, rgba(41, 121, 255, 0.05) 100%)'
            },
            {
              icon: <TrendingUp sx={{ fontSize: 40, color: '#2979ff' }} />,
              title: '实时推荐',
              desc: '动态学习用户行为，实时调整推荐策略',
              gradient: 'linear-gradient(135deg, rgba(41, 121, 255, 0.1) 0%, rgba(79, 195, 247, 0.05) 100%)'
            },
            {
              icon: <Palette sx={{ fontSize: 40, color: '#29b6f6' }} />,
              title: '个性化定制',
              desc: '千人千面，为每个用户打造专属新闻流',
              gradient: 'linear-gradient(135deg, rgba(41, 182, 246, 0.1) 0%, rgba(30, 136, 229, 0.05) 100%)'
            },
            {
              icon: <Science sx={{ fontSize: 40, color: '#5c6bc0' }} />,
              title: 'A/B测试',
              desc: '持续优化推荐效果，提升用户体验',
              gradient: 'linear-gradient(135deg, rgba(92, 107, 192, 0.1) 0%, rgba(57, 73, 171, 0.05) 100%)'
            }
          ].map((feature, index) => (
            <Grid item key={index} xs={12} sm={6} md={3}> {/* 添加 item 属性 */}
              <Fade in timeout={1500} style={{ transitionDelay: `${index * 200}ms` }}>
                <Paper
                  sx={{
                    p: 4,
                    height: '100%',
                    background: feature.gradient,
                    backdropFilter: 'blur(10px)',
                    border: '1px solid rgba(255, 255, 255, 0.1)',
                    borderRadius: 3,
                    transition: 'all 0.3s ease',
                    '&:hover': {
                      transform: 'translateY(-8px)',
                      boxShadow: '0 20px 40px rgba(0, 0, 0, 0.3)',
                      borderColor: 'rgba(41, 121, 255, 0.3)',
                    }
                  }}
                >
                  <Box sx={{ textAlign: 'center', mb: 3 }}>
                    {feature.icon}
                  </Box>
                  <Typography 
                    variant="h6" 
                    gutterBottom
                    sx={{ 
                      fontWeight: 600,
                      textAlign: 'center',
                      mb: 2
                    }}
                  >
                    {feature.title}
                  </Typography>
                  <Typography 
                    variant="body2" 
                    sx={{ 
                      color: 'rgba(255, 255, 255, 0.85)',
                      textAlign: 'center',
                      lineHeight: 1.6
                    }}
                  >
                    {feature.desc}
                  </Typography>
                </Paper>
              </Fade>
            </Grid>
          ))}
        </Grid>

        {/* 数据展示 */}
        <Paper
          sx={{
            mt: 10,
            p: 6,
            background: 'rgba(255, 255, 255, 0.05)',
            backdropFilter: 'blur(10px)',
            border: '1px solid rgba(255, 255, 255, 0.1)',
            borderRadius: 4,
          }}
        >
          <Typography 
            variant="h4" 
            gutterBottom
            sx={{ 
              textAlign: 'center',
              mb: 6,
              fontWeight: 700,
              background: 'linear-gradient(90deg, #4fc3f7 0%, #2979ff 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
            }}
          >
            系统优势
          </Typography>
          
          <Grid container spacing={4}>
            {[
              { value: '99.8%', label: '推荐准确率', color: '#4fc3f7' },
              { value: '50ms', label: '响应时间', color: '#2979ff' },
              { value: '1000万+', label: '新闻数据', color: '#29b6f6' },
              { value: '24/7', label: '实时更新', color: '#5c6bc0' },
            ].map((stat, index) => (
              <Grid item key={index} xs={6} sm={6} md={3}> {/* 修正 Grid 使用方式 */}
                <Box sx={{ textAlign: 'center' }}>
                  <Typography
                    variant="h2"
                    sx={{
                      fontWeight: 800,
                      color: stat.color,
                      mb: 1,
                      textShadow: `0 0 20px ${stat.color}40`,
                    }}
                  >
                    {stat.value}
                  </Typography>
                  <Typography
                    variant="h6"
                    sx={{
                      color: 'rgba(255, 255, 255, 0.9)',
                      fontWeight: 500,
                    }}
                  >
                    {stat.label}
                  </Typography>
                </Box>
              </Grid>
            ))}
          </Grid>
        </Paper>

        {/* 底部CTA */}
        <Box sx={{ mt: 10, textAlign: 'center' }}>
          <Typography 
            variant="h3" 
            gutterBottom
            sx={{ 
              fontWeight: 700,
              mb: 3,
              background: 'linear-gradient(90deg, #4fc3f7 0%, #2979ff 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
            }}
          >
            立即开启智能新闻之旅
          </Typography>
          
          <Typography 
            variant="h6" 
            sx={{ 
              color: 'rgba(255, 255, 255, 0.85)',
              mb: 6,
              maxWidth: 600,
              mx: 'auto',
              fontWeight: 300,
            }}
          >
            加入数万用户的行列，体验AI驱动的个性化新闻推荐
          </Typography>
          
          <Button
            variant="contained"
            size="large"
            onClick={() => navigate('/register')}
            sx={{
              background: 'linear-gradient(45deg, #2979ff 0%, #4fc3f7 100%)',
              color: '#fff',
              px: 8,
              py: 2,
              borderRadius: '50px',
              fontSize: '1.2rem',
              fontWeight: 700,
              boxShadow: '0 10px 40px rgba(41, 121, 255, 0.4)',
              transition: 'all 0.3s ease',
              '&:hover': {
                transform: 'translateY(-5px) scale(1.05)',
                boxShadow: '0 15px 50px rgba(41, 121, 255, 0.6)',
              }
            }}
          >
            免费注册 →
          </Button>
        </Box>
      </Container>
    </Box>
  );
};

export default Home;