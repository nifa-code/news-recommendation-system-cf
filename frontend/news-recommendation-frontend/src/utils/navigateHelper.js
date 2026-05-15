// src/utils/navigateHelper.js

import { createRef } from 'react';

// 创建导航ref，用于非组件环境跳转
const navigateRef = createRef();

// 初始化导航ref（在App.js中调用）
export const setNavigate = (navigate) => {
  navigateRef.current = navigate;
};

// 获取导航实例
export const getNavigate = () => {
  return navigateRef.current;
};
// let navigate = null;

// // 暴露给App.jsx设置navigate实例
// export const setNavigate = (navInstance) => {
//   navigate = navInstance;
// };

// // 供axios拦截器使用的跳转方法
// export const getNavigate = () => navigate;