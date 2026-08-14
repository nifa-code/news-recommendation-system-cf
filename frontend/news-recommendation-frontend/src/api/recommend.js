// src/api/recommendation.js
import axiosInstance from '../utils/axiosInstance';

export const recommendationApi = {
  // 获取个性化推荐
  getRecommendations: (count = 10) => {
    return axiosInstance.get(`/v1/recommend?count=${count}`);
  },

  // 获取热门新闻
  getHotRecommendations: (count = 10) => {
    return axiosInstance.get(`/v1/recommend/hot?count=${count}`);
  },

  // 刷新推荐
  refreshRecommendations: () => {
    return axiosInstance.post('/v1/recommend/refresh');
  },

  // 记录用户行为
  recordBehavior: (data) => {
    return axiosInstance.post('/v1/behavior/record', data);
  },

  // 点赞新闻
  likeNews: (newsId) => {
    return axiosInstance.post(`/v1/behavior/like?newsId=${newsId}`);
  },

  // 收藏新闻
  collectNews: (newsId) => {
    return axiosInstance.post(`/v1/behavior/collect?newsId=${newsId}`);
  },
};