// src/api/behavior.js
import axiosInstance from '../utils/axiosInstance';
// 1. 点赞/取消点赞
export const likeNews = (newsId, userId) => {
  return axiosInstance.post(`/v1/news/${newsId}/behavior/like`, {}, {
    params: { userId } // 传递userId参数给后端
  });
};

// 2. 收藏/取消收藏
export const collectNews = (newsId) => {
  return axiosInstance.post(`/v1/news/${newsId}/behavior/collect`, {});
};

// 3. 获取用户行为状态（点赞/收藏/阅读）
export const getBehaviorStatus = (newsId) => {
  return axiosInstance.get(`/v1/news/${newsId}/behavior/status`);
};

// 4. 记录点击行为
export const recordClickBehavior = (newsId) => {
  return axiosInstance.post(`/v1/news/${newsId}/behavior/click`, {});
};

// 5. 记录阅读行为
export const recordReadBehavior = (newsId) => {
  return axiosInstance.post(`/v1/news/${newsId}/behavior/read`, {});
};



// ========== 新增函数（Profile页面用） ==========
/**
 * 获取用户浏览历史（分页）
 * @param {number} pageNum - 页码（从1开始）
 * @param {number} pageSize - 每页条数
 * @returns {Promise} 包含列表和分页信息的响应
 */
export const getUserViewHistory = async (pageNum = 1, pageSize = 10) => {
  try {
    const response = await axiosInstance.get('/v1/user-center/view-history', {
      params: { pageNum, pageSize }
    });
    return response;
  } catch (error) {
    console.error('获取浏览历史失败:', error);
    throw error;
  }
};

export const getUserLikeList = async (pageNum = 1, pageSize = 10) => {
  try {
    return await axiosInstance.get('/v1/user-center/like-list', {
      params: { pageNum, pageSize }
    });
  } catch (error) {
    console.error('获取点赞列表失败:', error);
    throw error;
  }
};

/**
 * 获取用户收藏列表（分页）
 * @param {number} pageNum - 页码（从1开始）
 * @param {number} pageSize - 每页条数
 * @returns {Promise} 包含列表和分页信息的响应
 */
export const getUserCollectList = async (pageNum = 1, pageSize = 10) => {
  try {
    const response = await axiosInstance.get('/v1/user-center/collect-list', {
      params: { pageNum, pageSize }
    });
    return response;
  } catch (error) {
    console.error('获取收藏列表失败:', error);
    throw error;
  }
};




/**
 * 清空用户浏览历史
 * @returns {Promise} 操作结果
 */
export const clearViewHistory = async () => {
  try {
    const response = await axiosInstance.delete('/v1/user-center/view-history');
    return response;
  } catch (error) {
    console.error('清空浏览历史失败:', error);
    throw error;
  }
};

/**
 * 取消指定新闻的收藏（Profile页面用）
 * @param {string} newsId - 新闻ID
 * @returns {Promise} 操作结果
 */
export const cancelCollect = async (newsId) => {
  try {
    const response = await axiosInstance.delete(`/v1/user-center/collect/${newsId}`);
    return response;
  } catch (error) {
    console.error('取消收藏失败:', error);
    throw error;
  }
};


/**
 * 取消指定新闻的点赞
 * @param {String} newsId 
 * @returns 
 */
export const cancelLike = async (newsId) => {
  try {
    return await axiosInstance.delete(`/v1/user-center/like/${newsId}`);
  } catch (error) {
    console.error('取消点赞失败:', error);
    throw error;
  }
};
// import axiosInstance from '../utils/axiosInstance';

// /**
//  * 点赞/取消点赞新闻
//  * @param {string} newsId - 新闻ID
//  */
// export const likeNews = async (newsId) => {
//   try {
//     // ✅ 后端接口路径：/api/v1/news/{newsId}/behavior/like
//     // ✅ axiosInstance已直接返回后端的data，无需再取response.data
//     const response = await axiosInstance.post(`/v1/news/${newsId}/behavior/like`);
//     return response; // 直接返回（后端返回{code, data, message}）
//   } catch (error) {
//     console.error('点赞操作失败:', error);
//     throw error;
//   }
// };

// /**
//  * 收藏/取消收藏新闻
//  * @param {string} newsId - 新闻ID
//  */
// export const collectNews = async (newsId) => {
//   try {
//     const response = await axiosInstance.post(`/v1/news/${newsId}/behavior/collect`);
//     return response;
//   } catch (error) {
//     console.error('收藏操作失败:', error);
//     throw error;
//   }
// };

// /**
//  * 记录阅读行为（对应后端VIEW行为，接口路径是/read）
//  * @param {string} newsId - 新闻ID
//  */
// export const recordReadBehavior = async (newsId) => {
//   try {
//     // ✅ 修正：后端接口是/read，不是/view
//     const response = await axiosInstance.post(`/v1/news/${newsId}/behavior/read`);
//     return response;
//   } catch (error) {
//     console.error('记录阅读行为失败:', error);
//     throw error;
//   }
// };

// /**
//  * 记录点击行为（列表页点击进入详情页时调用）
//  * @param {string} newsId - 新闻ID
//  */
// export const recordClickBehavior = async (newsId) => {
//   try {
//     // ✅ 注意：后端未显式提供/click接口，但可以复用recordUserBehavior逻辑
//     // 若后端需要单独接口，需在BehaviorController添加@PostMapping("/click")
//     // 临时方案：先调用read（或让后端补充click接口）
//     const response = await axiosInstance.post(`/v1/news/${newsId}/behavior/click`);
//     return response;
//   } catch (error) {
//     console.error('记录点击行为失败:', error);
//     // 非核心行为，失败不抛错，避免影响页面
//     return null;
//   }
// };

// /**
//  * 获取用户对新闻的行为状态（是否点赞/收藏/阅读）
//  * @param {string} newsId - 新闻ID
//  */
// export const getBehaviorStatus = async (newsId) => {
//   try {
//     const response = await axiosInstance.get(`/v1/news/${newsId}/behavior/status`);
//     return response;
//   } catch (error) {
//     console.error('获取行为状态失败:', error);
//     throw error;
//   }
// };