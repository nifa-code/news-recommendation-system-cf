import Cube from '../components/Cube.jsx';
import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { 
  TextField, Button, Container, Box, 
  Typography, Alert, Fade, Paper, Zoom
} from '@mui/material'
import { PersonAdd, Rocket, Login } from '@mui/icons-material'
import axiosInstance from '../utils/axiosInstance' // 导入封装的axios

// 复用动画数据生成函数（和Login组件一致）
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

const Register = () => {
  const navigate = useNavigate()
  // 注册需要 userId + username + password（匹配后端要求）
  const [formData, setFormData] = useState({
    userId: '',
    username: '',
    email: '',
    password: '',
  })
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  
  const [meteorData] = useState(generateMeteorData)
  const [starData] = useState(generateStarData)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    
    // 表单校验
    if (!formData.userId.trim() || !formData.username.trim() || !formData.password.trim()) {
      setError('用户ID、用户名和密码不能为空')
      return
    }
    
    setIsLoading(true)
    try {
      // 调用后端注册接口（POST请求，传后端需要的参数）
      await axiosInstance.post('/auth/register', {
        userId: formData.userId,
        username: formData.username,
        password: formData.password,
        email: formData.email
      })
      
      // 注册成功处理
      setSuccess('注册成功！即将跳转到登录页...')
      setTimeout(() => {
        navigate('/auth/login')
      }, 1500)
    } catch (err) {
      // 注册失败处理（后端返回的错误信息）
      setError(err.response?.data?.message || '注册失败，请稍后重试')
    } finally {
      setIsLoading(false)
    }
  }



  return (
    // 界面样式和Login组件一致，只修改表单部分
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
      <Cube 
      size={80} 
      rotationSpeed={18} 
      top="auto" 
      right="auto" 
      bottom="50px" 
      left="50px" 
      zIndex={1} 
      opacity={0.6}
    />
      {/* 流星/星星动画（和Login组件一致，省略重复代码） */}
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
                    注册账号 · 开启个性化新闻之旅
                  </Typography>
                </Box>

                {/* 错误/成功提示 */}
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
                {success && (
                  <Alert 
                    severity="success" 
                    sx={{ 
                      mb: 3,
                      background: 'rgba(76, 175, 80, 0.1)',
                      color: '#81c784',
                      border: '1px solid rgba(76, 175, 80, 0.3)',
                      borderRadius: 2,
                    }}
                  >
                    {success}
                  </Alert>
                )}

                <form onSubmit={handleSubmit}>
                  {/* 新增 username 输入框 */}
                  <TextField
                    fullWidth
                    label="用户名"
                    variant="outlined"
                    margin="normal"
                    value={formData.username}
                    onChange={(e) => setFormData({...formData, username: e.target.value})}
                    required
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        color: '#fff',
                        '& fieldset': {
                          borderColor: 'rgba(255, 255, 255, 0.2)',
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
    label="邮箱"
    variant="outlined"
    margin="normal"
    type="email"
    value={formData.email}
    onChange={(e) => setFormData({...formData, email: e.target.value})}
    required
    placeholder="请输入邮箱（如：test@xxx.com）"
    sx={{
      '& .MuiOutlinedInput-root': {
        color: '#fff',
        '& fieldset': {
          borderColor: 'rgba(255, 255, 255, 0.2)',
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
      '& .Mui-focused .MuiInputLabel-root': {
        color: '#4fc3f7',
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
                      '&:hover': {
                        transform: 'translateY(-3px)',
                        boxShadow: '0 12px 35px rgba(41, 121, 255, 0.4)',
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
                          }}
                        />
                        注册中...
                      </>
                    ) : (
                      <>
                        <PersonAdd sx={{ mr: 1 }} />
                        完成注册
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
                      已有账号？
                    </Typography>
                    <Button 
                      variant="outlined"
                      onClick={() => navigate('/login')}
                      sx={{
                        color: '#4fc3f7',
                        borderColor: 'rgba(79, 195, 247, 0.3)',
                        borderRadius: '12px',
                        px: 4,
                        py: 1,
                        '&:hover': {
                          borderColor: '#4fc3f7',
                          background: 'rgba(79, 195, 247, 0.05)',
                          transform: 'translateY(-2px)',
                        },
                      }}
                    >
                      <Login sx={{ mr: 1 }} />
                      立即登录
                    </Button>
                  </Box>
                </form>
              </Box>
            </Fade>
          </Paper>
        </Zoom>
      </Container>
    </Box>
  )
}

export default Register