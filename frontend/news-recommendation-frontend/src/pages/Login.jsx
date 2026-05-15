import React, { useState, useEffect } from 'react'; // 添加useEffect
import { useNavigate } from 'react-router-dom'
import { 
  TextField, Button, Container, Box, 
  Typography, Alert, Fade, Paper, Zoom
} from '@mui/material'
import { Login as LoginIcon, Rocket, Stars, Logout } from '@mui/icons-material' // 添加Logout图标
import axiosInstance from '../utils/axiosInstance'

// 把随机数据生成逻辑抽离成独立的纯函数（在组件外部）
const generateMeteorData = () => {
  return Array.from({ length: 8 }, () => ({
    left: Math.random() * 100,
    duration: 3 + Math.random() * 5,
    delay: Math.random() * 5,
  }))
}

const generateStarData = () => {
  return Array.from({ length: 20 }, () => ({
    width: Math.random() * 3,
    height: Math.random() * 3,
    top: Math.random() * 100,
    left: Math.random() * 100,
    delay: Math.random() * 2,
  }))
}

const Login = () => {
  const navigate = useNavigate()
  const [formData, setFormData] = useState({
    userId: '',
    password: '',
  })
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [isLoggedIn, setIsLoggedIn] = useState(false) // 添加登录状态
  
  // 直接在 useState 初始化时生成随机数据（只执行一次）
  const [meteorData] = useState(generateMeteorData())
  const [starData] = useState(generateStarData())

  // 检查是否已登录
  useEffect(() => {
    const token = localStorage.getItem('token');
    setIsLoggedIn(!!token);
  }, []);

  // 已登录用户看到的界面
  if (isLoggedIn) {
    return (
      <Box
        sx={{
          minHeight: '100vh',
          background: 'linear-gradient(135deg, #2c3e50 0%, #4a6491 50%, #1a2530 100%)',
          color: '#fff',
          position: 'relative',
          overflow: 'hidden',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          '&::before': {
            content: '""',
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: 'radial-gradient(circle at 20% 30%, rgba(64, 115, 158, 0.1) 0%, transparent 50%)',
          }
        }}
      >
        {/* 流星动画 - 使用预生成的数据 */}
        {meteorData.map((meteor, i) => (
          <Box
            key={i}
            sx={{
              position: 'absolute',
              top: 0,
              left: `${meteor.left}%`,
              width: '2px',
              height: '100px',
              background: 'linear-gradient(180deg, rgba(79, 195, 247, 0.8) 0%, transparent 100%)',
              animation: `meteor ${meteor.duration}s linear infinite`,
              animationDelay: `${meteor.delay}s`,
              transform: 'rotate(-45deg)',
              opacity: 0,
              '@keyframes meteor': {
                '0%': {
                  transform: 'rotate(-45deg) translateX(0) translateY(-100px)',
                  opacity: 0,
                },
                '10%': { opacity: 1 },
                '70%': { opacity: 0.5 },
                '100%': {
                  transform: 'rotate(-45deg) translateX(500px) translateY(500px)',
                  opacity: 0,
                },
              },
            }}
          />
        ))}

        {/* 星星背景 - 使用预生成的数据 */}
        {starData.map((star, i) => (
          <Box
            key={`star-${i}`}
            sx={{
              position: 'absolute',
              width: `${star.width}px`,
              height: `${star.height}px`,
              background: '#fff',
              borderRadius: '50%',
              top: `${star.top}%`,
              left: `${star.left}%`,
              animation: 'twinkle 2s infinite alternate',
              animationDelay: `${star.delay}s`,
              '@keyframes twinkle': {
                '0%': { opacity: 0.2, transform: 'scale(1)' },
                '100%': { opacity: 1, transform: 'scale(1.5)' },
              },
            }}
          />
        ))}

        {/* 装饰光效 */}
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
          }}
        />

        <Container maxWidth="sm">
          <Zoom in timeout={800}>
            <Paper
              sx={{
                p: 6,
                background: 'rgba(255, 255, 255, 0.05)',
                backdropFilter: 'blur(20px)',
                border: '1px solid rgba(255, 255, 255, 0.1)',
                borderRadius: 4,
                boxShadow: '0 25px 50px rgba(0, 0, 0, 0.3)',
                position: 'relative',
                overflow: 'hidden',
                textAlign: 'center',
                '&::before': {
                  content: '""',
                  position: 'absolute',
                  top: 0,
                  left: 0,
                  right: 0,
                  height: '4px',
                  background: 'linear-gradient(90deg, #4fc3f7 0%, #2979ff 100%)',
                }
              }}
            >
              {/* 装饰元素 */}
              <Box
                sx={{
                  position: 'absolute',
                  top: -50,
                  right: -50,
                  width: 100,
                  height: 100,
                  background: 'radial-gradient(circle, rgba(79, 195, 247, 0.1) 0%, transparent 70%)',
                  borderRadius: '50%',
                  filter: 'blur(20px)',
                }}
              />

              <Typography 
                variant="h3" 
                gutterBottom
                sx={{ 
                  fontWeight: 800,
                  background: 'linear-gradient(90deg, #4fc3f7 0%, #2979ff 100%)',
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                  letterSpacing: '0.05em',
                }}
              >
                您已登录
              </Typography>
              
              <Typography 
                variant="h6" 
                sx={{ 
                  color: 'rgba(255, 255, 255, 0.8)',
                  fontWeight: 300,
                  mb: 4,
                }}
              >
                当前已处于登录状态
              </Typography>

              <Box sx={{ display: 'flex', justifyContent: 'center', gap: 2, mt: 4, mb: 4 }}>
                <Button
                  variant="contained"
                  onClick={() => navigate('/home')}
                  sx={{
                    background: 'linear-gradient(45deg, #2979ff 0%, #4fc3f7 100%)',
                    color: '#fff',
                    px: 4,
                    py: 1.5,
                    borderRadius: '12px',
                    fontSize: '1rem',
                    fontWeight: 600,
                    boxShadow: '0 8px 25px rgba(41, 121, 255, 0.3)',
                    transition: 'all 0.3s ease',
                    '&:hover': {
                      transform: 'translateY(-3px)',
                      boxShadow: '0 12px 35px rgba(41, 121, 255, 0.4)',
                    }
                  }}
                >
                  返回首页
                </Button>
                
                <Button
                  variant="outlined"
                  onClick={() => {
                    localStorage.removeItem('token');
                    localStorage.removeItem('user');
                    setIsLoggedIn(false);
                    navigate('/login'); // 刷新页面
                  }}
                  sx={{
                    color: '#ff6b6b',
                    borderColor: 'rgba(255, 107, 107, 0.3)',
                    borderRadius: '12px',
                    px: 4,
                    py: 1.5,
                    fontSize: '1rem',
                    fontWeight: 600,
                    transition: 'all 0.3s ease',
                    '&:hover': {
                      borderColor: '#ff6b6b',
                      background: 'rgba(255, 107, 107, 0.1)',
                      transform: 'translateY(-3px)',
                    },
                  }}
                >
                  <Logout sx={{ mr: 1 }} />
                  退出登录
                </Button>
              </Box>
            </Paper>
          </Zoom>
        </Container>
      </Box>
    );
  }

  // 未登录用户：显示正常的登录表单
  const handleSubmit = async (e) => { 
  e.preventDefault()
  setError('')
  
  if (!formData.userId.trim() || !formData.password.trim()) {
    setError('用户ID和密码不能为空')
    return
  }
  
  setIsLoading(true)
  try {
    // 修复1：接口路径添加/api前缀（匹配后端@RequestMapping("/api/auth")）
    const response = await axiosInstance.post('/auth/login', {
      userId: formData.userId,
      password: formData.password
    })
    
    // 修复2：token取值从response.data.token改为response.token（适配拦截器返回值）
    const token = response.token 
    localStorage.setItem('token', token)
    
    navigate('/home')
  } catch (err) {
    // 优化：兼容不同的错误数据格式
    const errorMsg = err.response?.data || err.message || '用户ID或密码错误'
    setError(typeof errorMsg === 'string' ? errorMsg : '登录失败，请重试')
  } finally {
    setIsLoading(false)
  }
}
  // const handleSubmit = async (e) => { // 改为 async 函数
  //   e.preventDefault()
  //   setError('')
    
  //   if (!formData.userId.trim() || !formData.password.trim()) {
  //     setError('用户ID和密码不能为空')
  //     return
  //   }
    
  //   setIsLoading(true)
  //   try {
  //     // 调用后端登录接口（POST请求，传后端需要的 userId + password）
  //     const response = await axiosInstance.post('/auth/login', {
  //       userId: formData.userId,
  //       password: formData.password
  //     })
      
  //     // 登录成功：存储后端返回的token到localStorage
  //     const token = response.data.token // 假设后端返回 { token: "xxx" }
  //     localStorage.setItem('token', token)
      
  //     // 跳转到首页
  //     navigate('/home')
  //   } catch (err) {
  //     // 登录失败：展示后端返回的错误信息
  //     setError(err.response?.data?.message || '用户ID或密码错误')
  //   } finally {
  //     setIsLoading(false)
  //   }
  // }

  return (
    <Box
      sx={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #2c3e50 0%, #4a6491 50%, #1a2530 100%)',
        color: '#fff',
        position: 'relative',
        overflow: 'hidden',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        '&::before': {
          content: '""',
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'radial-gradient(circle at 20% 30%, rgba(64, 115, 158, 0.1) 0%, transparent 50%)',
        }
      }}
    >
      {/* 流星动画 - 使用预生成的数据 */}
      {meteorData.map((meteor, i) => (
        <Box
          key={i}
          sx={{
            position: 'absolute',
            top: 0,
            left: `${meteor.left}%`,
            width: '2px',
            height: '100px',
            background: 'linear-gradient(180deg, rgba(79, 195, 247, 0.8) 0%, transparent 100%)',
            animation: `meteor ${meteor.duration}s linear infinite`,
            animationDelay: `${meteor.delay}s`,
            transform: 'rotate(-45deg)',
            opacity: 0,
            '@keyframes meteor': {
              '0%': {
                transform: 'rotate(-45deg) translateX(0) translateY(-100px)',
                opacity: 0,
              },
              '10%': { opacity: 1 },
              '70%': { opacity: 0.5 },
              '100%': {
                transform: 'rotate(-45deg) translateX(500px) translateY(500px)',
                opacity: 0,
              },
            },
          }}
        />
      ))}

      {/* 星星背景 - 使用预生成的数据 */}
      {starData.map((star, i) => (
        <Box
          key={`star-${i}`}
          sx={{
            position: 'absolute',
            width: `${star.width}px`,
            height: `${star.height}px`,
            background: '#fff',
            borderRadius: '50%',
            top: `${star.top}%`,
            left: `${star.left}%`,
            animation: 'twinkle 2s infinite alternate',
            animationDelay: `${star.delay}s`,
            '@keyframes twinkle': {
              '0%': { opacity: 0.2, transform: 'scale(1)' },
              '100%': { opacity: 1, transform: 'scale(1.5)' },
            },
          }}
        />
      ))}

      {/* 装饰光效 */}
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
        }}
      />

      <Container maxWidth="sm">
        <Zoom in timeout={800}>
          <Paper
            sx={{
              p: 6,
              background: 'rgba(255, 255, 255, 0.05)',
              backdropFilter: 'blur(20px)',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              borderRadius: 4,
              boxShadow: '0 25px 50px rgba(0, 0, 0, 0.3)',
              position: 'relative',
              overflow: 'hidden',
              '&::before': {
                content: '""',
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                height: '4px',
                background: 'linear-gradient(90deg, #4fc3f7 0%, #2979ff 100%)',
              }
            }}
          >
            {/* 装饰元素 */}
            <Box
              sx={{
                position: 'absolute',
                top: -50,
                right: -50,
                width: 100,
                height: 100,
                background: 'radial-gradient(circle, rgba(79, 195, 247, 0.1) 0%, transparent 70%)',
                borderRadius: '50%',
                filter: 'blur(20px)',
              }}
            />

            <Fade in timeout={1200}>
              <Box>
                <Box sx={{ textAlign: 'center', mb: 4 }}>
                  <Rocket
                    sx={{
                      fontSize: 60,
                      color: '#4fc3f7',
                      mb: 2,
                      filter: 'drop-shadow(0 0 10px rgba(79, 195, 247, 0.5))',
                    }}
                  />
                  <Typography 
                    variant="h3" 
                    gutterBottom
                    sx={{ 
                      fontWeight: 800,
                      background: 'linear-gradient(90deg, #4fc3f7 0%, #2979ff 100%)',
                      WebkitBackgroundClip: 'text',
                      WebkitTextFillColor: 'transparent',
                      letterSpacing: '0.05em',
                    }}
                  >
                    AI新闻推荐系统
                  </Typography>
                  <Typography 
                    variant="h6" 
                    sx={{ 
                      color: 'rgba(255, 255, 255, 0.8)',
                      fontWeight: 300,
                      mb: 1,
                    }}
                  >
                    智能登录 · 开启个性化新闻之旅
                  </Typography>
                </Box>

                {error && (
                  <Alert 
                    severity="error" 
                    sx={{ 
                      mb: 3,
                      background: 'rgba(244, 67, 54, 0.1)',
                      color: '#ff8a80',
                      border: '1px solid rgba(244, 67, 54, 0.3)',
                      borderRadius: 2,
                    }}
                  >
                    {error}
                  </Alert>
                )}

                <form onSubmit={handleSubmit}>
                  <TextField
                    fullWidth
                    label="用户ID"
                    variant="outlined"
                    margin="normal"
                    value={formData.userId}
                    onChange={(e) => setFormData({...formData, userId: e.target.value})}
                    required
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        color: '#fff',
                        '& fieldset': {
                          borderColor: 'rgba(255, 255, 255, 0.2)',
                          transition: 'all 0.3s ease',
                        },
                        '&:hover fieldset': {
                          borderColor: 'rgba(79, 195, 247, 0.5)',
                        },
                        '&.Mui-focused fieldset': {
                          borderColor: '#4fc3f7',
                          boxShadow: '0 0 20px rgba(79, 195, 247, 0.2)',
                        },
                      },
                      '& .MuiInputLabel-root': {
                        color: 'rgba(255, 255, 255, 0.7)',
                      },
                    }}
                  />
                  <TextField
                    fullWidth
                    label="密码"
                    type="password"
                    variant="outlined"
                    margin="normal"
                    value={formData.password}
                    onChange={(e) => setFormData({...formData, password: e.target.value})}
                    required
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        color: '#fff',
                        '& fieldset': {
                          borderColor: 'rgba(255, 255, 255, 0.2)',
                          transition: 'all 0.3s ease',
                        },
                        '&:hover fieldset': {
                          borderColor: 'rgba(79, 195, 247, 0.5)',
                        },
                        '&.Mui-focused fieldset': {
                          borderColor: '#4fc3f7',
                          boxShadow: '0 0 20px rgba(79, 195, 247, 0.2)',
                        },
                      },
                      '& .MuiInputLabel-root': {
                        color: 'rgba(255, 255, 255, 0.7)',
                      },
                    }}
                  />
                  
                  <Button
                    fullWidth
                    variant="contained"
                    type="submit"
                    disabled={isLoading}
                    sx={{
                      mt: 4,
                      mb: 2,
                      py: 1.5,
                      background: 'linear-gradient(45deg, #2979ff 0%, #4fc3f7 100%)',
                      color: '#fff',
                      fontSize: '1.1rem',
                      fontWeight: 600,
                      borderRadius: '12px',
                      boxShadow: '0 8px 25px rgba(41, 121, 255, 0.3)',
                      transition: 'all 0.3s ease',
                      position: 'relative',
                      overflow: 'hidden',
                      '&:hover': {
                        transform: 'translateY(-3px)',
                        boxShadow: '0 12px 35px rgba(41, 121, 255, 0.4)',
                        background: 'linear-gradient(45deg, #2979ff 30%, #4fc3f7 90%)',
                      },
                      '&::before': {
                        content: '""',
                        position: 'absolute',
                        top: 0,
                        left: '-100%',
                        width: '100%',
                        height: '100%',
                        background: 'linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent)',
                        transition: 'left 0.5s ease',
                      },
                      '&:hover::before': {
                        left: '100%',
                      },
                      '&:disabled': {
                        opacity: 0.6,
                        cursor: 'not-allowed',
                      },
                    }}
                  >
                    {isLoading ? (
                      <>
                        <Box
                          sx={{
                            display: 'inline-block',
                            width: 20,
                            height: 20,
                            mr: 2,
                            border: '2px solid rgba(255, 255, 255, 0.3)',
                            borderTopColor: '#fff',
                            borderRadius: '50%',
                            animation: 'spin 1s linear infinite',
                            '@keyframes spin': {
                              '0%': { transform: 'rotate(0deg)' },
                              '100%': { transform: 'rotate(360deg)' },
                            },
                          }}
                        />
                        登录中...
                      </>
                    ) : (
                      <>
                        <LoginIcon sx={{ mr: 1 }} />
                        登录系统
                      </>
                    )}
                  </Button>

                  <Box sx={{ textAlign: 'center', mt: 4 }}>
                    <Typography 
                      variant="body2" 
                      sx={{ 
                        color: 'rgba(255, 255, 255, 0.6)',
                        mb: 2,
                      }}
                    >
                      还没有账号？
                    </Typography>
                    <Button 
                      variant="outlined"
                      onClick={() => navigate('/register')}
                      sx={{
                        color: '#4fc3f7',
                        borderColor: 'rgba(79, 195, 247, 0.3)',
                        borderRadius: '12px',
                        px: 4,
                        py: 1,
                        transition: 'all 0.3s ease',
                        '&:hover': {
                          borderColor: '#4fc3f7',
                          background: 'rgba(79, 195, 247, 0.05)',
                          transform: 'translateY(-2px)',
                        },
                      }}
                    >
                      <Stars sx={{ mr: 1 }} />
                      立即注册新账号
                    </Button>
                  </Box>
                </form>
              </Box>
            </Fade>

            <Typography 
              variant="caption" 
              sx={{ 
                display: 'block',
                textAlign: 'center',
                mt: 4,
                color: 'rgba(255, 255, 255, 0.4)',
                fontSize: '0.75rem',
              }}
            >
              © fann30191@gmail.com AI News Recommender · Collaborative Filtering News Recommendation System
            </Typography>
          </Paper>
        </Zoom>
      </Container>
    </Box>
  )
}

export default Login