// src/components/NewsCardGrid.jsx
import React from 'react';
import { Grid, Box, Typography } from '@mui/material';
import NewsCard from './NewsCard';

/**
 * 新闻卡片网格布局组件
 * @param {Object} props
 * @param {Array} props.newsList - 新闻列表
 * @param {Function} [props.onCardClick] - 卡片点击回调
 * @param {string} [props.emptyMessage] - 空数据提示
 * @param {Object} [props.gridConfig] - 网格配置
 */
const NewsCardGrid = ({ 
  newsList = [], 
  onCardClick,
  emptyMessage = '暂无新闻数据',
  gridConfig = {
    xs: 12,    // 手机：1列
    sm: 6,     // 平板：2列
    md: 4,     // 桌面：3列
    lg: 3,     // 大屏：4列
    xl: 2.4    // 超大屏：5列
  }
}) => {
  if (!newsList || newsList.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', my: 8, py: 4 }}>
        <Typography color="text.secondary">
          {emptyMessage}
        </Typography>
      </Box>
    );
  }

  return (
    <Grid container spacing={3}>
      {newsList.map((news) => {
        if (!news || !news.id) return null;
        
        return (
          <Grid 
            key={news.id} 
            item 
            xs={gridConfig.xs}
            sm={gridConfig.sm}
            md={gridConfig.md}
            lg={gridConfig.lg}
            xl={gridConfig.xl}
            sx={{ 
              display: 'flex',
              // 关键：确保卡片占满Grid item
              '& > *': {
                width: '100%'
              }
            }}
          >
            <NewsCard 
              news={news}
              onClick={onCardClick}
              showDetailsButton={true}
            />
          </Grid>
        );
      })}
    </Grid>
  );
};

export default NewsCardGrid;