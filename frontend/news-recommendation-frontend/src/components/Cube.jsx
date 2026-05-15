import React from 'react';
import { Box } from '@mui/material';

// 修复后的 3D 荧光魔方组件
const Cube = ({
  size = 80, // 魔方大小（px）
  color = '#4fc3f7', // 荧光蓝色
  rotationSpeed = 15, // 旋转速度（秒/圈）
  position = 'absolute', // 定位方式
  top = 'auto', // 垂直位置（优先用 top/bottom 其一）
  bottom = 'auto', // 垂直位置
  left = 'auto', // 水平位置（优先用 left/right 其一）
  right = 'auto', // 水平位置
  opacity = 0.8, // 透明度
  zIndex = 99, // 提高默认层级，避免被覆盖
  className = ''
}) => {
  const faceSize = `${size}px`;
  const halfSize = size / 2;

  return (
    <Box
      sx={{
        position,
        top,
        bottom,
        left,
        right,
        opacity,
        zIndex,
        width: faceSize,
        height: faceSize,
        // 🔴 修复：增大透视值，确保 3D 效果明显
        perspective: '2000px',
        // 🔴 修复：强制开启 3D 渲染
        transformStyle: 'preserve-3d',
        WebkitTransformStyle: 'preserve-3d', // 兼容 Safari/Chrome
        animation: `cubeRotate ${rotationSpeed}s infinite linear`,
        // 确保动画流畅
        backfaceVisibility: 'visible',
        WebkitBackfaceVisibility: 'visible',
        // 响应式调整
        '@media (max-width: 768px)': {
          width: `${size * 0.7}px`,
          height: `${size * 0.7}px`,
        },
        // 🔴 修复：防止被父容器裁剪
        overflow: 'visible',
      }}
      className={className}
    >
      {/* 魔方核心容器 */}
      <Box
        sx={{
          width: '100%',
          height: '100%',
          position: 'relative',
          // 🔴 关键：强制子元素继承 3D 渲染
          transformStyle: 'preserve-3d',
          WebkitTransformStyle: 'preserve-3d',
          backfaceVisibility: 'visible',
          WebkitBackfaceVisibility: 'visible',
          // 🔴 修复：添加轻微的初始旋转，避免正对镜头（只显示一个面）
          transform: 'rotateX(15deg) rotateY(30deg)',
        }}
      >
        {/* 6 个面 - 每个面都添加 backface-visibility */}
        {['front', 'back', 'left', 'right', 'top', 'bottom'].map((face) => (
          <Box
            key={face}
            sx={{
              position: 'absolute',
              width: faceSize,
              height: faceSize,
              background: `radial-gradient(circle, ${color} 0%, ${color}80 100%)`,
              boxShadow: `0 0 15px ${color}, 0 0 30px ${color}60, inset 0 0 10px ${color}90`,
              border: `1px solid ${color}`,
              borderRadius: '4px',
              opacity: 0.9,
              // 🔴 修复：允许背面可见，避免被遮挡
              backfaceVisibility: 'visible',
              WebkitBackfaceVisibility: 'visible',
              // 🔴 修复：确保 3D 变换生效
              transform: getFaceTransform(face, halfSize),
              transformStyle: 'preserve-3d',
              WebkitTransformStyle: 'preserve-3d',
            }}
          />
        ))}
      </Box>

      {/* 全局动画样式（确保所有浏览器识别） */}
      <style jsx global>{`
        @keyframes cubeRotate {
          0% { transform: rotateX(0deg) rotateY(0deg); }
          100% { transform: rotateX(360deg) rotateY(360deg); }
        }
        @-webkit-keyframes cubeRotate {
          0% { -webkit-transform: rotateX(0deg) rotateY(0deg); }
          100% { -webkit-transform: rotateX(360deg) rotateY(360deg); }
        }
      `}</style>
    </Box>
  );
};

// 计算每个面的 3D 变换位置（保持不变）
const getFaceTransform = (face, halfSize) => {
  switch (face) {
    case 'front':
      return `translateZ(${halfSize}px)`;
    case 'back':
      return `rotateY(180deg) translateZ(${halfSize}px)`;
    case 'left':
      return `rotateY(-90deg) translateZ(${halfSize}px)`;
    case 'right':
      return `rotateY(90deg) translateZ(${halfSize}px)`;
    case 'top':
      return `rotateX(90deg) translateZ(${halfSize}px)`;
    case 'bottom':
      return `rotateX(-90deg) translateZ(${halfSize}px)`;
    default:
      return `translateZ(${halfSize}px)`;
  }
};

export default Cube;