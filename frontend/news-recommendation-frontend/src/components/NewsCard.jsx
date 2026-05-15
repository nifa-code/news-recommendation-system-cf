// import React from 'react';
// import { 
//   Card, CardContent, CardMedia, Typography, Box, Button, Chip,
//   CardActionArea // 使整个卡片可点击
// } from '@mui/material';
// import { AccessTime, Visibility } from '@mui/icons-material'; // 导入图标
// import moment from 'moment'; // 引入moment.js，如果没有安装需要 npm install moment
// // 组件顶部定义
// const DEFAULT_IMAGE = "https://picsum.photos/400/200?random=news";
// /**
//  * 新闻卡片组件
//  * @param {Object} props
//  * @param {Object} props.news - 新闻数据对象 (NewsDTO)
//  * @param {Function} [props.onClick] - 卡片点击回调函数 (newsId)
//  * @param {string} [props.variant='default'] - 卡片变体，例如 'compact'
//  * @param {boolean} [props.showDetailsButton=false] - 是否显示“查看详情”按钮
//  */
// const NewsCard = ({ news, onClick, variant = 'default', showDetailsButton = false }) => {
//   if (!news || !news.id) {
//     return null; // 或者返回一个骨架屏
//   }

//   const {
//     id,
//     title,
//     abstractText,
//     category,
//     publishTime,
//     viewCount,
//     coverImageUrl, // 封面图
//     thumbnailUrl,  // 缩略图
//     hasImages,     // 是否有图
//     imageUrls      // 所有图片URL (JSON解析后的数组)
//   } = news;

//   const handleCardClick = () => {
//     if (onClick) {
//       onClick(id);
//     }
//   };

//   // 根据 variant 调整样式
//   const isCompact = variant === 'compact';

//   // 封面图逻辑：优先thumbnailUrl，其次coverImageUrl，再次imageUrls[0]，最后默认图片
//   const displayImage = thumbnailUrl || coverImageUrl || (hasImages && imageUrls && imageUrls.length > 0 ? imageUrls[0] : null);
//   //const defaultImage = 'https://via.placeholder.com/150x100?text=No+Image'; // 默认无图占位符

//   return (
//     <Card 
//       sx={{ 
//         height: '100%', 
//         display: 'flex', 
//         flexDirection: 'column',
//         borderRadius: 2,
//         boxShadow: isCompact ? 1 : 3, // 紧凑模式下阴影浅一些
//         transition: 'transform 0.2s ease-in-out',
//         '&:hover': {
//           transform: 'translateY(-4px)', // 鼠标悬停上浮效果
//           boxShadow: isCompact ? 3 : 6,
//         }
//       }}
//     >
//       <CardActionArea onClick={handleCardClick} sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
//         {/* 新闻图片 */}
//         {displayImage && (
//           <CardMedia
//             component="img"
//             sx={{ 
//               height: isCompact ? 120 : 180, // 紧凑模式图片高度更低
//               objectFit: 'cover' 
//             }}
//             image={displayImage|| DEFAULT_IMAGE}
//             alt={title}
//             onError={(e) => e.target.src = DEFAULT_IMAGE}
//           />
//         )}
//         {/* 如果没有图片，可以显示一个占位Box */}
//         {!displayImage && (
//           <Box sx={{ 
//             height: isCompact ? 120 : 180, 
//             display: 'flex', 
//             alignItems: 'center', 
//             justifyContent: 'center', 
//             backgroundColor: '#f0f0f0', 
//             color: '#888' 
//           }}>
//             <Typography variant="caption">无图</Typography>
//           </Box>
//         )}

//         {/* 新闻内容 */}
//         <CardContent sx={{ flexGrow: 1, p: isCompact ? 1.5 : 2 }}> {/* 紧凑模式内边距更小 */}
//           <Typography 
//             variant={isCompact ? "subtitle2" : "h6"} 
//             component="div" 
//             sx={{ 
//               mb: isCompact ? 0.5 : 1, 
//               fontWeight: 600,
//               overflow: 'hidden',
//               textOverflow: 'ellipsis',
//               display: '-webkit-box',
//               WebkitLineClamp: isCompact ? 2 : 3, // 紧凑模式标题限制行数更少
//               WebkitBoxOrient: 'vertical',
//             }}
//           >
//             {title || '无标题新闻'}
//           </Typography>
          
//           <Typography 
//             variant="body2" 
//             color="text.secondary" 
//             sx={{ 
//               mb: isCompact ? 1 : 1.5,
//               overflow: 'hidden',
//               textOverflow: 'ellipsis',
//               display: '-webkit-box',
//               WebkitLineClamp: isCompact ? 3 : 4, // 紧凑模式摘要限制行数更少
//               WebkitBoxOrient: 'vertical',
//             }}
//           >
//             {abstractText || '暂无摘要内容。'}
//           </Typography>

//           {/* 元信息：类别、发布时间、阅读量 */}
//           <Box sx={{ 
//             display: 'flex', 
//             justifyContent: 'space-between', 
//             alignItems: 'center', 
//             mt: isCompact ? 0.5 : 1
//           }}>
//             <Chip 
//               label={category || '未分类'} 
//               size="small" 
//               color="primary" 
//               variant="outlined" 
//               sx={{ height: isCompact ? 20 : 24, fontSize: isCompact ? '0.6rem' : '0.75rem' }} 
//             />
//             <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
//               {publishTime && (
//                 <Typography variant={isCompact ? "caption" : "body2"} color="text.secondary" sx={{ display: 'flex', alignItems: 'center' }}>
//                   <AccessTime fontSize="inherit" sx={{ mr: 0.3 }} />
//                   {moment(publishTime).fromNow()} {/* 例如: 2小时前 */}
//                 </Typography>
//               )}
//               {viewCount !== undefined && (
//                 <Typography variant={isCompact ? "caption" : "body2"} color="text.secondary" sx={{ display: 'flex', alignItems: 'center' }}>
//                   <Visibility fontSize="inherit" sx={{ mr: 0.3 }} />
//                   {viewCount}
//                 </Typography>
//               )}
//             </Box>
//           </Box>
//         </CardContent>
//       </CardActionArea>
//       {showDetailsButton && (
//         <Box sx={{ p: isCompact ? 1.5 : 2, pt: 0, display: 'flex', justifyContent: 'flex-end' }}>
//           <Button size="small" onClick={handleCardClick}>查看详情</Button>
//         </Box>
//       )}
//     </Card>
//   );
// };

// export default NewsCard;

import React from 'react';
import { 
  Card, CardContent, CardMedia, Typography, Box, Button, Chip, CardActionArea
} from '@mui/material';
import { AccessTime, Visibility } from '@mui/icons-material';
import moment from 'moment';

// 统一默认图
//const DEFAULT_IMAGE = "https://picsum.photos/400/200?random=news";
// 根据 news.id 生成不同图片
const getDefaultImage = (newsId) => {
  return `https://picsum.photos/400/200?random=${newsId}`;
};
const NewsCard = ({ news, onClick, variant = 'default', showDetailsButton = false }) => {
  if (!news || !news.id) return null;

  const {
    id, title, abstractText, category, publishTime, viewCount,
    coverImageUrl, thumbnailUrl, hasImages, imageUrls
  } = news;

  const handleCardClick = () => {
    if (onClick) onClick(id);
  };

  const isCompact = variant === 'compact';

  // 图片优先级（自动兜底）
  const displayImage = thumbnailUrl || coverImageUrl || (hasImages && imageUrls?.[0]) || null;

  return (
    <Card sx={{ 
      height: '100%', display: 'flex', flexDirection: 'column', borderRadius: 2,
      boxShadow: isCompact ? 1 : 3, transition: 'transform 0.2s',
      '&:hover': { transform: 'translateY(-4px)', boxShadow: isCompact ? 3 : 6 }
    }}>
      <CardActionArea onClick={handleCardClick} sx={{ height: '100%', flexDirection: 'column' }}>      
        <CardMedia
          component="img"
          sx={{ 
            height: isCompact ? 120 : 180,
            objectFit: 'cover' 
          }}
          image={displayImage || getDefaultImage(news.id)}
          alt={title}
          onError={(e) => {
  e.target.src = getDefaultImage(news.id);
}}
          
        />

        <CardContent sx={{ flexGrow: 1, p: isCompact ? 1.5 : 2 }}>
          <Typography 
            variant={isCompact ? "subtitle2" : "h6"} 
            sx={{ 
              mb: isCompact ? 0.5 : 1, fontWeight: 600,
              overflow: 'hidden', textOverflow: 'ellipsis',
              WebkitLineClamp: isCompact ? 2 : 3, WebkitBoxOrient: 'vertical'
            }}
          >
            {title || '无标题新闻'}
          </Typography>
          
          <Typography variant="body2" color="text.secondary" sx={{ 
            mb: isCompact ? 1 : 1.5,
            overflow: 'hidden', textOverflow: 'ellipsis',
            WebkitLineClamp: isCompact ? 3 : 4, WebkitBoxOrient: 'vertical'
          }}>
            {abstractText || '暂无摘要内容。'}
          </Typography>

          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: isCompact ? 0.5 : 1 }}>
            <Chip label={category || '未分类'} size="small" color="primary" variant="outlined" />
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              {publishTime && (
                <Typography variant="caption" color="text.secondary">
                  <AccessTime fontSize="inherit" sx={{ mr: 0.3 }} />
                  {moment(publishTime).fromNow()}
                </Typography>
              )}
              {viewCount !== undefined && (
                <Typography variant="caption" color="text.secondary">
                  <Visibility fontSize="inherit" sx={{ mr: 0.3 }} />
                  {viewCount}
                </Typography>
              )}
            </Box>
          </Box>
        </CardContent>
      </CardActionArea>
      {showDetailsButton && (
        <Box sx={{ p: isCompact ? 1.5 : 2, pt: 0, display: 'flex', justifyContent: 'flex-end' }}>
          <Button size="small" onClick={handleCardClick}>查看详情</Button>
        </Box>
      )}
    </Card>
  );
};

export default NewsCard;