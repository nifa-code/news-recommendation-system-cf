// src/App.jsx
import React from 'react';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
// ✅ 修复：省略 .jsx 后缀（Vite 推荐，避免解析错误）
import Login from './pages/Login'; 
import Register from './pages/Register';
import Home from './pages/Home'; 
import theme from './theme'; 
import { setNavigate } from './utils/navigateHelper';
import { useNavigate } from 'react-router-dom';
// ✅ 修复：省略 .jsx 后缀
import NewsRecommendation from './pages/NewsRecommendation';
import NewsDetail from './pages/NewsDetail';
import Profile from './pages/Profile';

const ProtectedRoute = ({ children }) => {
  const token = localStorage.getItem('token');
  return token ? children : <Navigate to="/login" replace />;
};

// 路由内容组件（内部用 useNavigate）
const RouterContent = () => {
  const navigate = useNavigate(); // ✅ 在 Router 内部，合法使用
  
  // 初始化 navigate 给 axios 用
  React.useEffect(() => {
    setNavigate(navigate);
  }, [navigate]);

  return (
    <div className="app" style={{ minHeight: '100vh' }}>
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route 
          path="/login" 
          element={localStorage.getItem('token') ? <Navigate to="/home" replace /> : <Login />} 
        />
        {/* 注册页：无需登录 */} 
        <Route path="/register" element={<Register />} />
        {/* 首页：需要登录 */}
        <Route path="/home" element={<ProtectedRoute><Home /></ProtectedRoute>} />
        {/* 新闻推荐页 */}
        <Route 
          path="/news" 
          element={
            <ProtectedRoute>
              <NewsRecommendation />
            </ProtectedRoute>
          } 
        />
        {/* 新闻详情页 */}
        <Route 
          path="/news/:newsId" 
          element={
            <ProtectedRoute>
              <NewsDetail />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/profile" 
          element={<ProtectedRoute><Profile /></ProtectedRoute>} 
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  );
};

// 主 App 组件
function App() {
  return ( 
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Router>
        <RouterContent />
      </Router>
    </ThemeProvider>
  );
}

export default App;