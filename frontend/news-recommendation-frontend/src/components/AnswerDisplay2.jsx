// src/components/AnswerDisplay.jsx
import React from 'react';
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
    Divider
} from '@mui/material';
import {
    SmartToy,
    ContentCopy,
    CheckCircle,
    ThumbUpAlt,
    ThumbDownAlt,
    AccessTime,
    Cached
} from '@mui/icons-material';

const AnswerDisplay = ({ 
    answer, 
    question, 
    timestamp, 
    cached, 
    showFeedback = true,
    onCopy,
    copySuccess
}) => {
    const formatTime = (time) => {
        return new Date(time).toLocaleTimeString([], { 
            hour: '2-digit', 
            minute: '2-digit' 
        });
    };

    return (
        <Card variant="outlined" sx={{ mb: 3, borderRadius: 2 }}>
            <CardContent>
                {/* 头部信息 */}
                <Box sx={{ 
                    display: 'flex', 
                    justifyContent: 'space-between', 
                    alignItems: 'center',
                    mb: 2
                }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <SmartToy color="primary" />
                        <Typography variant="h6" fontWeight="bold" color="primary">
                            AI 回答
                        </Typography>
                        {cached && (
                            <Chip 
                                icon={<Cached />}
                                label="缓存" 
                                size="small" 
                                color="success" 
                                variant="outlined"
                            />
                        )}
                    </Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        {timestamp && (
                            <Typography variant="caption" color="text.secondary">
                                <AccessTime fontSize="small" sx={{ mr: 0.5 }} />
                                {formatTime(timestamp)}
                            </Typography>
                        )}
                        <Tooltip title={copySuccess ? "已复制" : "复制回答"}>
                            <IconButton 
                                size="small"
                                onClick={onCopy}
                            >
                                {copySuccess ? 
                                    <CheckCircle color="success" fontSize="small" /> : 
                                    <ContentCopy fontSize="small" />
                                }
                            </IconButton>
                        </Tooltip>
                    </Box>
                </Box>
                
                {/* 问题（如果有） */}
                {question && (
                    <Box sx={{ mb: 2, p: 2, bgcolor: 'grey.100', borderRadius: 1 }}>
                        <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 0.5 }}>
                            问题：
                        </Typography>
                        <Typography variant="body1" sx={{ fontWeight: 'medium' }}>
                            {question}
                        </Typography>
                    </Box>
                )}
                
                {/* 回答内容 */}
                <Paper 
                    variant="outlined" 
                    sx={{ 
                        p: 3, 
                        bgcolor: 'grey.50',
                        borderRadius: 2,
                        borderLeft: 4,
                        borderLeftColor: 'primary.main',
                        maxHeight: 400,
                        overflow: 'auto'
                    }}
                >
                    <Typography 
                        variant="body1" 
                        sx={{ 
                            lineHeight: 1.8,
                            whiteSpace: 'pre-wrap'
                        }}
                    >
                        {answer || "暂无回答内容"}
                    </Typography>
                </Paper>
                
                {/* 反馈按钮 */}
                {showFeedback && answer && (
                    <>
                        <Divider sx={{ my: 2 }} />
                        <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
                            <Button 
                                size="small" 
                                variant="outlined"
                                startIcon={<ThumbUpAlt fontSize="small" />}
                            >
                                有帮助
                            </Button>
                            <Button 
                                size="small" 
                                variant="outlined"
                                startIcon={<ThumbDownAlt fontSize="small" />}
                            >
                                需改进
                            </Button>
                        </Box>
                    </>
                )}
            </CardContent>
        </Card>
    );
};

export default AnswerDisplay; 