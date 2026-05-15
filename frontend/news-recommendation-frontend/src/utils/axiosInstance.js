// src/utils/axiosInstance.js
import axios from 'axios';
import API_CONFIG from '../config/api';

const axiosInstance = axios.create({
  baseURL: API_CONFIG.BASE_URL, 
  timeout: API_CONFIG.TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true
});

// 请求拦截器
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    console.log('【请求配置】', {
      url: config.baseURL + config.url,
      headers: config.headers,
      params: config.params,
      data: config.data
    });
    return config;
  },
  (error) => {
    console.error('【请求拦截器错误】', error);
    return Promise.reject(error);
  }
);

// 响应拦截器
axiosInstance.interceptors.response.use(
  (response) => {
    console.log(`【响应成功】${response.config.url}`, response.data);
    return response.data;
  },
  (error) => {
    const url = error.config?.url || '';
    
    console.error(`【响应错误】${url}`, {
      status: error.response?.status,
      data: error.response?.data,
      message: error.message
    });

    // 对于401错误，只对需要认证的API跳转
    if (error.response?.status === 401) {
      // 这些是公开API，不跳转
      const publicAPIs = [
        '/v1/news/detail/',
        '/v1/news/list',
        '/v1/recommend/hot'
      ];
      
      // 这些是需要认证的API，跳转到登录页
      const authRequiredAPIs = [
        '/v1/news/',
        '/v1/user/',
        '/v1/recommend'
      ];
      
      const isPublicAPI = publicAPIs.some(api => url.includes(api));
      const isAuthRequiredAPI = authRequiredAPIs.some(api => url.includes(api));
      
      if (!isPublicAPI && isAuthRequiredAPI) {
        console.log('需要认证的API认证失败，跳转登录页面');
        localStorage.removeItem('token');
        localStorage.removeItem('userId');
        
        // 避免重复跳转
        if (!window.location.pathname.includes('/login')) {
          window.location.href = '/login';
        }
      } else {
        // 对于公开API或非认证API，只记录日志，不跳转
        console.log('公开API认证失败或无认证要求，无需跳转');
      }
    }
    
    return Promise.reject(error);
  }
);

export default axiosInstance;

// // src/utils/axiosInstance.js
// import axios from 'axios';
// import API_CONFIG from '../config/api';
// // 先注释掉有问题的getNavigate，改用更简单的方式处理跳转
// // import { getNavigate } from './navigateHelper'; 

// const axiosInstance = axios.create({
//   baseURL: API_CONFIG.BASE_URL, 
//   timeout: API_CONFIG.TIMEOUT,
//   headers: {
//     'Content-Type': 'application/json',
//   },
//   withCredentials: true
// });


// // 请求拦截器（不变）
// axiosInstance.interceptors.request.use(
//   (config) => {
//     const token = localStorage.getItem('token');
//     if (token) {
//       config.headers.Authorization = `Bearer ${token}`;
//     }
//     console.log('【请求配置】', {
//       url: config.baseURL + config.url,
//       headers: config.headers,
//       params: config.params,
//       data: config.data
//     });
//     return config;
//   },
//   (error) => {
//     console.error('【请求拦截器错误】', error);
//     return Promise.reject(error);
//   }
// );

// // 响应拦截器：修复返回值 → 提取后端的data
// axiosInstance.interceptors.response.use(
//   (response) => {
//     console.log(`【响应成功】${response.config.url}`, response.data);
//     // ✅ 核心修复：返回后端实际的data，而非整个response
//     return response.data; 
//   },
//   (error) => {
//     console.error(`【响应错误】${error.config?.url}`, {
//       status: error.response?.status,
//       data: error.response?.data,
//       message: error.message
//     });

//     if (error.response?.status === 401) {
//       localStorage.removeItem('token');
//       localStorage.removeItem('userId');
//       if (!window.location.pathname.includes('/login')) {
//         window.location.href = '/login'; 
//       }
//     }
//     return Promise.reject(error);
//   }
// );
// export default axiosInstance;