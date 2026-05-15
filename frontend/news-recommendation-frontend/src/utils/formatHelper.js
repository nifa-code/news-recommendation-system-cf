/**
 * 格式化工具函数
 */

/**
 * 格式化新闻分类
 */
export const formatCategory = (category) => {
  if (!category) return '综合';
  
  const categoryMap = {
    'politics': '政治',
    'economy': '经济',
    'sports': '体育',
    'entertainment': '娱乐',
    'technology': '科技',
    'health': '健康',
    'international': '国际',
    'military': '军事',
    'society': '社会'
  };
  
  return categoryMap[category] || category;
};

/**
 * 格式化日期时间
 */
export const formatDateTime = (dateString) => {
  if (!dateString) return '未知时间';
  
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return dateString;
    
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    });
  } catch{
    return dateString;
  }
};

/**
 * 获取完整的图片URL
 */
export const getFullImageUrl = (url) => {
  if (!url) return null;
  
  // 如果已经是完整URL，直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }
  
  // 如果是相对路径，添加服务器基础URL
  
  //const baseUrl = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api';
  const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
  // 处理以斜杠开头的路径
  if (url.startsWith('/')) {
    return `${baseUrl}${url}`;
  }
  
  // 其他情况直接拼接
  return `${baseUrl}/${url}`;
};

/**
 * 处理图片URL数组
 */
export const processImageUrls = (imageUrls) => {
  if (!imageUrls || !Array.isArray(imageUrls)) return [];
  
  return imageUrls
    .map(url => getFullImageUrl(url))
    .filter(url => url && url.trim() !== '');
};

/**
 * 默认新闻图片
 */
export const defaultNewsImage = '/images/default-news.jpg';

/**
 * 简化版：如果没有图片，使用占位图
 */
export const getSafeImageUrl = (url) => {
  const fullUrl = getFullImageUrl(url);
  return fullUrl || defaultNewsImage;
};