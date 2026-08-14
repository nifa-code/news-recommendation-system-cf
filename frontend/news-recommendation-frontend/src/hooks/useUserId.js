// hooks/useUserId.js
import { useState, useEffect } from 'react';

export const useUserId = () => {
  const [userId, setUserId] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const getUserId = () => {
      const token = localStorage.getItem('token');
      if (!token) {
        setLoading(false);
        return null;
      }

      try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const payload = JSON.parse(window.atob(base64));
        
        const userId = payload.sub || payload.userId || payload.id;
        if (userId) {
          localStorage.setItem('userId', userId);
          setUserId(userId);
        }
      } catch (error) {
        console.error('解析token失败:', error);
      } finally {
        setLoading(false);
      }
    };

    getUserId();
  }, []);

  return { userId, loading };
};