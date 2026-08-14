// hooks/useAuth.js

// hooks/useAuth.js - 简化版避免React警告
import { useState, useEffect } from 'react';

export const useAuth = () => {
  // 直接计算状态，不使用useSyncExternalStore
  const getAuthState = () => {
    const token = localStorage.getItem('token');
    let userId = localStorage.getItem('userId');
    
    if (token && !userId) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        userId = payload.sub || payload.userId || payload.id;
        if (userId) {
          localStorage.setItem('userId', userId);
        }
      } catch (error) {
        console.error('解析token失败:', error);
      }
    }
    
    return {
      userId,
      isLoggedIn: !!(token && userId)
    };
  };

  const [authState, setAuthState] = useState(getAuthState());

  // 监听storage变化
  useEffect(() => {
    const handleStorageChange = () => {
      const newState = getAuthState();
      setAuthState(newState);
    };

    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, []);

  return authState;
};
// import { useSyncExternalStore } from 'react';

// // 自定义store
// const authStore = {
//   getSnapshot: () => {
//     const token = localStorage.getItem('token');
//     let userId = localStorage.getItem('userId');
    
//     if (token && !userId) {
//       try {
//         const payload = JSON.parse(atob(token.split('.')[1]));
//         userId = payload.sub || payload.userId || payload.id;
//         if (userId) {
//           localStorage.setItem('userId', userId);
//         }
//       } catch (error) {
//         console.error('解析token失败:', error);
//       }
//     }
    
//     return {
//       userId,
//       isLoggedIn: !!(token && userId)
//     };
//   },
  
//   subscribe: (callback) => {
//     // 监听storage变化
//     const handleStorageChange = () => {
//       callback();
//     };
    
//     window.addEventListener('storage', handleStorageChange);
//     return () => window.removeEventListener('storage', handleStorageChange);
//   }
// };

// export const useAuth = () => {
//   const authState = useSyncExternalStore(
//     authStore.subscribe,
//     authStore.getSnapshot
//   );

//   // 登录
//   const login = (token, userId) => {
//     localStorage.setItem('token', token);
//     localStorage.setItem('userId', userId);
//     // 触发更新
//     window.dispatchEvent(new Event('storage'));
//   };

//   // 登出
//   const logout = () => {
//     localStorage.removeItem('token');
//     localStorage.removeItem('userId');
//     localStorage.removeItem('user');
//     // 触发更新
//     window.dispatchEvent(new Event('storage'));
//   };

//   return {
//     userId: authState.userId,
//     isLoggedIn: authState.isLoggedIn,
//     login,
//     logout
//   };
// };