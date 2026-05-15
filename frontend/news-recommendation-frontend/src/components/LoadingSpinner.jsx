// src/components/LoadingSpinner.jsx (MUI 版本)
import React from 'react';
import { Box, CircularProgress, Typography } from '@mui/material';
const LoadingSpinner = () => {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        height: '50vh'
      }}
    >
      <CircularProgress />
      <Typography variant="body2" sx={{ mt: 2, color: 'text.secondary' }}>
        加载中...
      </Typography>
    </Box>
  );
};
export default LoadingSpinner;