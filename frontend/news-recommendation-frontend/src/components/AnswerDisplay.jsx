// src/components/AnswerDisplay.jsx
import React, { useState, useMemo, useCallback } from 'react';
import PropTypes from 'prop-types';
import {
    Box,
    Paper,
    Typography,
    IconButton,
    Tooltip,
    Chip,
    Card,
    CardContent,
    Button,
    Divider,
    Fade,
    Grow,
    Zoom
} from '@mui/material';
import {
    SmartToy,
    ContentCopy,
    CheckCircle,
    ThumbUpAlt,
    ThumbDownAlt,
    AccessTime,
    Cached,
    Article,
    QuestionMark,
    Star,
    FlashOn,
    StarBorder,
    Code,
    Psychology,
    AutoAwesome,
    Build 
} from '@mui/icons-material';

// 提取常量配置，便于维护
const THEME_CONFIG = {
    summary: {
        gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        accent: '#667eea',
        secondary: '#764ba2',
        bg: 'rgba(15, 23, 42, 0.95)',
        border: 'rgba(102, 126, 234, 0.3)',
        text: '#e2e8f0',
        shadow: '0 20px 60px rgba(102, 126, 234, 0.2)',
        glow: '0 0 40px rgba(102, 126, 234, 0.3)',
        iconBg: 'rgba(102, 126, 234, 0.15)',
        pulse: '#667eea'
    },
    answer: {
        gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
        accent: '#f093fb',
        secondary: '#f5576c',
        bg: 'rgba(15, 23, 42, 0.95)',
        border: 'rgba(240, 147, 251, 0.3)',
        text: '#e2e8f0',
        shadow: '0 20px 60px rgba(240, 147, 251, 0.2)',
        glow: '0 0 40px rgba(240, 147, 251, 0.3)',
        iconBg: 'rgba(240, 147, 251, 0.15)',
        pulse: '#f093fb'
    },
    default: {
        gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
        accent: '#4facfe',
        secondary: '#00f2fe',
        bg: 'rgba(15, 23, 42, 0.95)',
        border: 'rgba(79, 172, 254, 0.3)',
        text: '#e2e8f0',
        shadow: '0 20px 60px rgba(79, 172, 254, 0.2)',
        glow: '0 0 40px rgba(79, 172, 254, 0.3)',
        iconBg: 'rgba(79, 172, 254, 0.15)',
        pulse: '#4facfe'
    }
};

// 工具函数 - 格式化时间
const formatTime = (time) => {
    if (!time) return '';
    try {
        const date = new Date(time);
        // 增加有效性检查
        if (isNaN(date.getTime())) return '无效时间';
        return date.toLocaleTimeString([], { 
            hour: '2-digit', 
            minute: '2-digit',
            second: '2-digit'
        });
    } catch (error) {
        console.error('时间格式化失败:', error);
        return '时间解析错误';
    }
};

// 提取滚动条样式为常量，避免重复创建
const SCROLLBAR_STYLES = {
    '&::-webkit-scrollbar': {
        width: '10px'
    },
    '&::-webkit-scrollbar-track': {
        background: 'rgba(255,255,255,0.02)',
        borderRadius: '20px'
    },
    '&::-webkit-scrollbar-thumb': {
        borderRadius: '20px',
        border: '3px solid rgba(15, 23, 42, 0.95)',
        '&:hover': {
            opacity: 0.8
        }
    }
};

// 组件拆分 - 头部组件
const AnswerHeader = ({ 
    type, 
    theme, 
    timestamp, 
    cached, 
    loading, 
    copySuccess, 
    onCopy 
}) => (
    <Box sx={{ 
        p: 4,
        pb: 2.5,
        borderBottom: `1px solid ${theme.border}`
    }}>
        <Box sx={{ 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'flex-start',
            mb: 2
        }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 3 }}>
                <Box sx={{ 
                    position: 'relative',
                    width: 56,
                    height: 56,
                    borderRadius: '16px',
                    background: theme.gradient,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    overflow: 'hidden',
                    boxShadow: `0 8px 32px ${theme.accent}40`,
                    '&::before': {
                        content: '""',
                        position: 'absolute',
                        width: '100%',
                        height: '100%',
                        background: 'rgba(255,255,255,0.1)',
                        animation: 'pulse 2s infinite'
                    },
                    '@keyframes pulse': {
                        '0%, 100%': { opacity: 1 },
                        '50%': { opacity: 0.5 }
                    }
                }}>
                    {type === 'summary' ? (
                        <Article sx={{ fontSize: 28, color: 'white' }} />
                    ) : (
                        <SmartToy sx={{ fontSize: 28, color: 'white' }} />
                    )}
                    <Star 
                        sx={{ 
                            position: 'absolute',
                            top: -4,
                            right: -4,
                            fontSize: 16,
                            color: 'white',
                            filter: 'drop-shadow(0 0 8px rgba(255,255,255,0.8))'
                        }} 
                    />
                </Box>
                
                <Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
                        <Typography 
                            variant="h5" 
                            sx={{ 
                                color: 'white',
                                fontWeight: 800,
                                fontSize: '1.5rem',
                                letterSpacing: '-0.5px',
                                background: theme.gradient,
                                WebkitBackgroundClip: 'text',
                                WebkitTextFillColor: 'transparent',
                                backgroundClip: 'text'
                            }}
                        >
                            {type === 'summary' ? '智能摘要' : 'AI 回答'}
                        </Typography>
                        
                        {cached && (
                            <Chip 
                                icon={<Cached />}
                                label="缓存" 
                                size="small"
                                sx={{ 
                                    bgcolor: 'rgba(255,255,255,0.08)',
                                    color: 'white',
                                    border: `1px solid ${theme.accent}40`,
                                    height: 24,
                                    fontSize: '0.75rem',
                                    fontWeight: 600,
                                    '& .MuiChip-icon': { 
                                        fontSize: 14,
                                        color: theme.accent 
                                    },
                                    backdropFilter: 'blur(10px)',
                                    WebkitBackdropFilter: 'blur(10px)'
                                }}
                            />
                        )}
                    </Box>
                    
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <Typography 
                            variant="caption" 
                            sx={{ 
                                color: 'rgba(255,255,255,0.6)',
                                display: 'flex',
                                alignItems: 'center',
                                gap: 0.75,
                                fontSize: '0.85rem'
                            }}
                        >
                            <AccessTime sx={{ fontSize: 16 }} />
                            {timestamp ? formatTime(timestamp) : '实时生成'}
                        </Typography>
                        
                        {loading && (
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                <Box sx={{ 
                                    width: 6, 
                                    height: 6, 
                                    borderRadius: '50%',
                                    bgcolor: theme.accent,
                                    animation: 'pulse 1.5s infinite'
                                }} />
                                <Typography 
                                    variant="caption" 
                                    sx={{ 
                                        color: theme.accent,
                                        fontSize: '0.85rem'
                                    }}
                                >
                                    思考中...
                                </Typography>
                            </Box>
                        )}
                    </Box>
                </Box>
            </Box>
            
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Tooltip title={copySuccess ? "已复制" : "复制内容"}>
                    <IconButton 
                        size="medium"
                        sx={{ 
                            color: 'white',
                            bgcolor: 'rgba(255,255,255,0.08)',
                            border: `1px solid ${theme.border}`,
                            backdropFilter: 'blur(10px)',
                            WebkitBackdropFilter: 'blur(10px)',
                            '&:hover': {
                                bgcolor: theme.accent,
                                transform: 'scale(1.1)'
                            },
                            transition: 'all 0.2s'
                        }}
                        onClick={onCopy}
                    >
                        {copySuccess ? 
                            <CheckCircle sx={{ color: '#10b981' }} /> : 
                            <ContentCopy />
                        }
                    </IconButton>
                </Tooltip>
            </Box>
        </Box>
    </Box>
);

// 组件拆分 - 问题展示组件
const QuestionDisplay = ({ question, theme }) => {
    if (!question || question === '新闻摘要') return null;
    
    return (
        <Fade in={true} timeout={800}>
            <Box sx={{ 
                mt: 2,
                p: 3,
                borderRadius: '16px',
                bgcolor: 'rgba(255,255,255,0.03)',
                borderLeft: `4px solid ${theme.accent}`,
                position: 'relative',
                overflow: 'hidden'
            }}>
                <Box sx={{ 
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    width: '100%',
                    height: '100%',
                    background: theme.gradient,
                    opacity: 0.03,
                    zIndex: 0
                }} />
                
                <Box sx={{ 
                    display: 'flex',
                    alignItems: 'center',
                    gap: 2,
                    mb: 1.5,
                    position: 'relative',
                    zIndex: 1
                }}>
                    <Box sx={{ 
                        width: 36,
                        height: 36,
                        borderRadius: '10px',
                        bgcolor: theme.iconBg,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                    }}>
                        <QuestionMark sx={{ fontSize: 20, color: theme.accent }} />
                    </Box>
                    <Typography 
                        variant="subtitle1" 
                        sx={{ 
                            color: 'white',
                            fontWeight: 600,
                            fontSize: '1.1rem'
                        }}
                    >
                        问题
                    </Typography>
                </Box>
                
                <Typography 
                    variant="body1" 
                    sx={{ 
                        color: 'rgba(255,255,255,0.9)',
                        lineHeight: 1.7,
                        pl: 5,
                        position: 'relative',
                        zIndex: 1,
                        fontStyle: 'italic'
                    }}
                >
                    "{question}"
                </Typography>
            </Box>
        </Fade>
    );
};

// 组件拆分 - 回答内容组件
const AnswerContent = ({ answer, theme, hovered, loading }) => (
    <Box sx={{ p: 4 }}>
        <Paper 
            elevation={0}
            sx={{ 
                borderRadius: '20px',
                bgcolor: 'rgba(255,255,255,0.02)',
                border: `1px solid ${theme.border}`,
                overflow: 'hidden',
                position: 'relative',
                backdropFilter: 'blur(20px)',
                WebkitBackdropFilter: 'blur(20px)',
                minHeight: 200,
                maxHeight: 500,
                overflowY: 'auto',
                ...SCROLLBAR_STYLES,
                // 动态设置滚动条颜色
                '&::-webkit-scrollbar-thumb': {
                    ...SCROLLBAR_STYLES['&::-webkit-scrollbar-thumb'],
                    background: theme.gradient
                },
                '&::-webkit-scrollbar-thumb:hover': {
                    background: theme.gradient
                }
            }}
        >
            {/* 装饰性代码行 */}
            <Box sx={{ 
                position: 'absolute',
                top: 20,
                right: 20,
                display: 'flex',
                gap: 1,
                opacity: 0.1
            }}>
                <Code sx={{ fontSize: 14, color: 'white' }} />
                <Code sx={{ fontSize: 14, color: 'white' }} />
                <Code sx={{ fontSize: 14, color: 'white' }} />
            </Box>
            
            {answer ? (
                <Typography 
                    variant="body1" 
                    sx={{ 
                        lineHeight: 1.8,
                        whiteSpace: 'pre-wrap',
                        p: 4,
                        color: theme.text,
                        fontSize: '1.05rem',
                        fontFamily: '"Inter", -apple-system, BlinkMacSystemFont, sans-serif',
                        '& strong': {
                            color: theme.accent,
                            fontWeight: 700,
                            background: theme.gradient,
                            WebkitBackgroundClip: 'text',
                            WebkitTextFillColor: 'transparent',
                            backgroundClip: 'text'
                        },
                        '& em': {
                            fontStyle: 'italic',
                            color: 'rgba(255,255,255,0.7)'
                        },
                        '& a': {
                            color: theme.accent,
                            textDecoration: 'none',
                            borderBottom: `1px solid ${theme.accent}40`,
                            transition: 'all 0.2s',
                            '&:hover': {
                                borderBottom: `1px solid ${theme.accent}`
                            }
                        }
                    }}
                >
                    {answer}
                </Typography>
            ) : (
                <Box sx={{ 
                    display: 'flex', 
                    flexDirection: 'column', 
                    alignItems: 'center',
                    justifyContent: 'center',
                    minHeight: 200,
                    color: 'rgba(255,255,255,0.5)'
                }}>
                    <Psychology sx={{ fontSize: 48, mb: 2, opacity: 0.3 }} />
                    <Typography variant="h6">
                        {loading ? '正在生成内容...' : '暂无内容'}
                    </Typography>
                </Box>
            )}
            
            {/* 悬浮效果装饰 */}
            {hovered && (
                <Fade in={hovered}>
                    <Box sx={{ 
                        position: 'absolute',
                        bottom: 20,
                        left: '50%',
                        transform: 'translateX(-50%)',
                        display: 'flex',
                        gap: 1
                    }}>
                        <FlashOn sx={{ fontSize: 16, color: theme.accent }} />
                        <Star sx={{ fontSize: 16, color: theme.accent }} />
                        <AutoAwesome sx={{ fontSize: 16, color: theme.accent }} />
                    </Box>
                </Fade>
            )}
        </Paper>
    </Box>
);

// 组件拆分 - 反馈区域组件
const FeedbackSection = ({ 
    showFeedback, 
    answer, 
    theme, 
    feedbackGiven, 
    handleFeedback 
}) => (
    <Box sx={{ 
        px: 4,
        pb: 4,
        pt: 2
    }}>
        <Divider sx={{ 
            borderColor: theme.border, 
            mb: 3 
        }} />
        
        <Box sx={{ 
            display: 'flex', 
            justifyContent: 'space-between',
            alignItems: 'center'
        }}>
            <Box sx={{ display: 'flex', gap: 2 }}>
                {showFeedback && answer && (
                    <>
                        <Zoom in={!feedbackGiven} style={{ transitionDelay: !feedbackGiven ? '100ms' : '0ms' }}>
                            <Button 
                                size="medium"
                                variant="outlined"
                                startIcon={<ThumbUpAlt />}
                                onClick={() => handleFeedback('helpful')}
                                sx={{ 
                                    color: feedbackGiven === 'helpful' ? '#10b981' : 'white',
                                    borderColor: feedbackGiven === 'helpful' ? '#10b981' : theme.border,
                                    bgcolor: feedbackGiven === 'helpful' ? 'rgba(16, 185, 129, 0.1)' : 'rgba(255,255,255,0.05)',
                                    borderRadius: '12px',
                                    px: 3,
                                    py: 1,
                                    fontWeight: 600,
                                    backdropFilter: 'blur(10px)',
                                    WebkitBackdropFilter: 'blur(10px)',
                                    '&:hover': {
                                        bgcolor: feedbackGiven === 'helpful' ? 'rgba(16, 185, 129, 0.2)' : 'rgba(255,255,255,0.1)',
                                        borderColor: feedbackGiven === 'helpful' ? '#10b981' : theme.accent,
                                        transform: 'translateY(-2px)'
                                    },
                                    transition: 'all 0.2s'
                                }}
                            >
                                {feedbackGiven === 'helpful' ? '已感谢' : '有帮助'}
                            </Button>
                        </Zoom>
                        
                        <Zoom in={!feedbackGiven} style={{ transitionDelay: !feedbackGiven ? '200ms' : '0ms' }}>
                            <Button 
                                size="medium"
                                variant="outlined"
                                startIcon={<ThumbDownAlt />}
                                onClick={() => handleFeedback('needs_improvement')}
                                sx={{ 
                                    color: feedbackGiven === 'needs_improvement' ? '#ef4444' : 'white',
                                    borderColor: feedbackGiven === 'needs_improvement' ? '#ef4444' : theme.border,
                                    bgcolor: feedbackGiven === 'needs_improvement' ? 'rgba(239, 68, 68, 0.1)' : 'rgba(255,255,255,0.05)',
                                    borderRadius: '12px',
                                    px: 3,
                                    py: 1,
                                    fontWeight: 600,
                                    backdropFilter: 'blur(10px)',
                                    WebkitBackdropFilter: 'blur(10px)',
                                    '&:hover': {
                                        bgcolor: feedbackGiven === 'needs_improvement' ? 'rgba(239, 68, 68, 0.2)' : 'rgba(255,255,255,0.1)',
                                        borderColor: feedbackGiven === 'needs_improvement' ? '#ef4444' : theme.accent,
                                        transform: 'translateY(-2px)'
                                    },
                                    transition: 'all 0.2s'
                                }}
                            >
                                {feedbackGiven === 'needs_improvement' ? '已反馈' : '需改进'}
                            </Button>
                        </Zoom>
                    </>
                )}
            </Box>
            
            <Typography 
                variant="caption" 
                sx={{ 
                    color: 'rgba(255,255,255,0.4)',
                    fontSize: '0.8rem',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 0.5
                }}
            >
                <Star sx={{ fontSize: 12 }} />
                AI 生成内容
            </Typography>
        </Box>
    </Box>
);

// 主组件
const AnswerDisplay = ({ 
    answer, 
    question, 
    timestamp, 
    cached, 
    showFeedback = true,
    onCopy,
    copySuccess,
    type = 'answer',
    loading = false
}) => {
    const [hovered, setHovered] = useState(false);
    const [feedbackGiven, setFeedbackGiven] = useState(false);

    // 使用 useMemo 缓存主题配置，避免每次渲染重新计算
    const theme = useMemo(() => {
        return THEME_CONFIG[type] || THEME_CONFIG.default;
    }, [type]);

    // 使用 useCallback 缓存回调函数，避免传递给子组件时触发重渲染
    const handleFeedback = useCallback((feedbackType) => {
        setFeedbackGiven(feedbackType);
        // 这里可以发送反馈到后端
        console.log(`反馈: ${feedbackType}`);
    }, []);

    const handleMouseEnter = useCallback(() => setHovered(true), []);
    const handleMouseLeave = useCallback(() => setHovered(false), []);

    return (
        <Grow in={true} timeout={500}>
            <Card 
                variant="outlined"
                onMouseEnter={handleMouseEnter}
                onMouseLeave={handleMouseLeave}
                sx={{
                    mb: 3,
                    borderRadius: '24px',
                    border: `1px solid ${theme.border}`,
                    background: theme.bg,
                    backdropFilter: 'blur(20px)',
                    WebkitBackdropFilter: 'blur(20px)',
                    position: 'relative',
                    overflow: 'hidden',
                    transition: 'all 0.4s cubic-bezier(0.4, 0, 0.2, 1)',
                    transform: hovered ? 'translateY(-4px)' : 'translateY(0)',
                    boxShadow: hovered 
                        ? `${theme.shadow}, ${theme.glow}`
                        : theme.shadow,
                    '&::before': {
                        content: '""',
                        position: 'absolute',
                        top: 0,
                        left: 0,
                        right: 0,
                        bottom: 0,
                        background: theme.gradient,
                        opacity: hovered ? 0.1 : 0.05,
                        transition: 'opacity 0.4s ease',
                        zIndex: 0
                    },
                    '&::after': {
                        content: '""',
                        position: 'absolute',
                        top: 0,
                        left: 0,
                        right: 0,
                        height: '6px',
                        background: theme.gradient,
                        zIndex: 1,
                        animation: 'shimmer 3s infinite'
                    },
                    '@keyframes shimmer': {
                        '0%': { transform: 'translateX(-100%)' },
                        '100%': { transform: 'translateX(100%)' }
                    }
                }}
            >
                {/* 装饰性背景元素 */}
                <Box
                    sx={{
                        position: 'absolute',
                        top: -100,
                        right: -100,
                        width: 300,
                        height: 300,
                        borderRadius: '50%',
                        background: theme.gradient,
                        opacity: 0.03,
                        filter: 'blur(40px)',
                        zIndex: 0
                    }}
                />
                <Box
                    sx={{
                        position: 'absolute',
                        bottom: -50,
                        left: -50,
                        width: 200,
                        height: 200,
                        borderRadius: '50%',
                        background: theme.gradient,
                        opacity: 0.02,
                        filter: 'blur(30px)',
                        zIndex: 0
                    }}
                />

                <CardContent sx={{ p: 0, position: 'relative', zIndex: 1 }}>
                    <AnswerHeader 
                        type={type}
                        theme={theme}
                        timestamp={timestamp}
                        cached={cached}
                        loading={loading}
                        copySuccess={copySuccess}
                        onCopy={onCopy}
                    />
                    
                    <QuestionDisplay 
                        question={question}
                        theme={theme}
                    />
                    
                    <AnswerContent 
                        answer={answer}
                        theme={theme}
                        hovered={hovered}
                        loading={loading}
                    />
                    
                    <FeedbackSection 
                        showFeedback={showFeedback}
                        answer={answer}
                        theme={theme}
                        feedbackGiven={feedbackGiven}
                        handleFeedback={handleFeedback}
                    />
                </CardContent>
            </Card>
        </Grow>
    );
};

// 添加 PropTypes 类型检查
AnswerDisplay.propTypes = {
    answer: PropTypes.string,
    question: PropTypes.string,
    timestamp: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    cached: PropTypes.bool,
    showFeedback: PropTypes.bool,
    onCopy: PropTypes.func.isRequired,
    copySuccess: PropTypes.bool,
    type: PropTypes.oneOf(['summary', 'answer', 'default']),
    loading: PropTypes.bool
};

export default AnswerDisplay;