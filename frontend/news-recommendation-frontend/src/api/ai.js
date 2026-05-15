// src/api/ai.js
import axiosInstance from '../utils/axiosInstance';

/**
 * 生成新闻摘要
 * @param {string} newsId 新闻ID
 * @returns {Promise} 摘要任务信息
 */
export const generateSummary = (newsId) => {
  return axiosInstance.post(`/ai/summary?newsId=${newsId}`);
};

/**
 * 获取摘要生成结果
 * @param {string} taskId 任务ID
 * @returns {Promise} 摘要结果
 */
export const getSummaryResult = (taskId) => {
  return axiosInstance.get(`/ai/summary/result?taskId=${taskId}`);
};

/**
 * 向新闻提问
 * @param {string} newsId 新闻ID
 * @param {string} question 问题内容
 * @returns {Promise} 问答任务信息
 */
export const askQuestion = (newsId, question) => {
  return axiosInstance.post('/ai/qna', { newsId, question });
};

/**
 * 获取问答结果
 * @param {string} taskId 任务ID
 * @returns {Promise} 问答结果
 */
export const getQnAResult = (taskId) => {
  return axiosInstance.get(`/ai/qna/result?taskId=${taskId}`);
};