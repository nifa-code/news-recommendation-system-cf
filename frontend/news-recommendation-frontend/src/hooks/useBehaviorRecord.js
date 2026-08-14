// src/hooks/useBehaviorRecord.js
import { useEffect, useRef, useCallback } from 'react';
import { recordBehavior } from '../api/behavior';

const useBehaviorRecord = (newsId) => {
  const startTimeRef = useRef(null);

  // 用useCallback包裹，确保依赖稳定
  const startTimer = useCallback(() => {
    // 避免重复启动计时
    if (!startTimeRef.current) {
      startTimeRef.current = new Date();
      console.log('开始计时，新闻ID：', newsId);
    }
  }, [newsId]);

  const stopTimer = useCallback(() => {
    if (!startTimeRef.current || !newsId) return;

    const endTime = new Date();
    const durationSeconds = Math.floor((endTime - startTimeRef.current) / 1000);
    // 过滤掉时长过短的无效浏览（比如误触）
    if (durationSeconds < 1) return;

    const userId = localStorage.getItem('userId');
    // 校验必要参数，避免无效接口调用
    if (!userId) {
      console.warn('用户未登录，无法记录行为');
      startTimeRef.current = null;
      return;
    }

    // 记录浏览行为
    recordBehavior({
      userId,
      newsId,
      behaviorType: 'VIEW',
      durationSeconds
    }).then(() => {
      console.log('行为记录成功', { newsId, durationSeconds });
    }).catch(error => {
      console.error('行为记录失败', error);
    }).finally(() => {
      // 重置计时，避免重复上报
      startTimeRef.current = null;
    });
  }, [newsId]);

  useEffect(() => {
    // 组件挂载时自动启动计时（也可保留手动启动，根据业务需求调整）
    startTimer();

    // 监听页面隐藏/显示，更精准计算有效浏览时长
    const handleVisibilityChange = () => {
      if (document.hidden) {
        stopTimer(); // 切出标签页时暂停并上报
      } else {
        startTimer(); // 切回标签页时重新开始计时
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);

    // 组件卸载时执行清理
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      stopTimer(); // 卸载时最终上报
    };
  }, [startTimer, stopTimer]); // 加入正确的依赖

  return { startTimer, stopTimer };
};

export default useBehaviorRecord;