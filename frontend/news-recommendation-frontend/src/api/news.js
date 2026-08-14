// src/api/news.js
import axiosInstance from '../utils/axiosInstance'; // 改用统一实例

/**
 * 获取新闻详情（适配后端字段）
 * @param {string} newsId 新闻ID
 * @returns {Promise} 新闻详情数据
 */
export const getNewsDetail = (newsId) => {
  // 路径改为 /v1/news/detail（和推荐页的 /v1/recommend 保持一致）
  return axiosInstance.get(`/v1/news/detail/${newsId}`);
};

/**
 * 获取推荐新闻列表
 * @param {string} userId 用户ID
 * @param {number} limit 推荐数量
 * @returns {Promise} 推荐新闻列表
 */
export const getRecommendNews = (userId, limit = 40) => {
  return axiosInstance.get('/v1/news/recommend', {
    params: { userId, limit }
  });
};