//src/components/NewsInteraction.jsx
import React from 'react';
import { Button, Box, Typography } from '@mui/material';
import { Favorite, FavoriteBorder, Bookmark, BookmarkBorder } from '@mui/icons-material';

const NewsInteraction = ({ 
  news, 
  userId, 
  liked, 
  collected, 
  behaviorLoading, 
  handleLike, 
  handleCollect 
}) => {
  if (!userId) return null; // 未登录隐藏交互按钮

  return (
    <Box sx={{ mt: 4, display: 'flex', gap: 3, alignItems: 'center' }}>
      <Button
        variant="outlined"
        startIcon={liked ? <Favorite color="error" /> : <FavoriteBorder />}
        onClick={handleLike}
        disabled={behaviorLoading}
        sx={{ 
          borderRadius: 2, 
          '&:hover': { bgcolor: liked ? '#ffebee' : 'inherit' } 
        }}
      >
        {liked ? '取消点赞' : '点赞'}
        <Typography sx={{ ml: 1 }}>{news.likeCount || 0}</Typography>
      </Button>

      <Button
        variant="outlined"
        startIcon={collected ? <Bookmark color="primary" /> : <BookmarkBorder />}
        onClick={handleCollect}
        disabled={behaviorLoading}
        sx={{ 
          borderRadius: 2, 
          '&:hover': { bgcolor: collected ? '#e3f2fd' : 'inherit' } 
        }}
      >
        {collected ? '取消收藏' : '收藏'}
        <Typography sx={{ ml: 1 }}>{news.collectCount || 0}</Typography>
      </Button>
    </Box>
  );
};

export default NewsInteraction;