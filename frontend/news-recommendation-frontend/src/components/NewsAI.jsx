import React, { useState, useEffect } from 'react';
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  IconButton,
  Chip,
  CircularProgress,
  Divider,
  Alert,
  Card,
  CardContent,
  List,
  ListItem,
  ListItemText,
  Collapse,
  Tooltip,
  Stack
} from '@mui/material';
import {
  SmartToy,
  AutoAwesome,
  QuestionAnswer,
  Lightbulb,
  ExpandMore,
  ExpandLess,
  ContentCopy,
  CheckCircle
} from '@mui/icons-material';
import axiosInstance from '../utils/axiosInstance';
import AnswerDisplay from './AnswerDisplay';

// 环境检测函数
const isDevelopment = () => {
  if (typeof import.meta !== 'undefined' && import.meta.env) {
    return import.meta.env.DEV || import.meta.env.MODE === 'development';
  }
  //if (typeof process !== 'undefined' && process.env) {
    //return process.env.NODE_ENV === 'development';
  //}
  return false;
};

const NewsAI = ({ newsId, isLoggedIn }) => {
  // API 调用函数
  const generateSummary = (newsId) => {
    return axiosInstance.post(`/ai/summary?newsId=${newsId}`);
  };

  const getSummaryResult = (taskId) => {
    return axiosInstance.get(`/ai/summary/result?taskId=${taskId}`);
  };

  const askQuestion = (newsId, question) => {
    return axiosInstance.post('/ai/qna', { newsId, question });
  };

  const getQnAResult = (taskId) => {
    return axiosInstance.get(`/ai/qna/result?taskId=${taskId}`);
  };

  // ========== 状态定义 ==========
  // 摘要相关状态
  const [summary, setSummary] = useState({
    content: '',
    cached: false,
    timestamp: null
  });
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [summaryTaskId, setSummaryTaskId] = useState('');
  const [summaryPolling, setSummaryPolling] = useState(false);
  
  // 问答相关状态
  const [question, setQuestion] = useState('');
  const [answers, setAnswers] = useState([]);
  const [currentAnswer, setCurrentAnswer] = useState({
    answer: '',
    question: '',
    timestamp: null,
    cached: false
  });
  const [answerLoading, setAnswerLoading] = useState(false);
  const [answerTaskId, setAnswerTaskId] = useState('');
  const [answerPolling, setAnswerPolling] = useState(false);
  const [currentQuestionForPolling, setCurrentQuestionForPolling] = useState('');
  
  // UI 状态
  const [expandedSection, setExpandedSection] = useState('summary');
  const [copySuccess, setCopySuccess] = useState({});
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // ========== 工具函数 ==========
  const handleCopyText = async (text, key) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopySuccess(prev => ({ ...prev, [key]: true }));
      setTimeout(() => {
        setCopySuccess(prev => ({ ...prev, [key]: false }));
      }, 2000);
    } catch (err) {
      console.error('复制失败:', err);
    }
  };

  const formatTime = (timestamp) => {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  // 清除消息
  useEffect(() => {
    if (error || success) {
      const timer = setTimeout(() => {
        setError('');
        setSuccess('');
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [error, success]);

  // ========== 摘要生成函数 ==========
  const handleGenerateSummary = async () => {
    if (!isLoggedIn) {
      setError('请先登录以使用AI功能');
      return;
    }

    if (summaryLoading) return;

    setSummaryLoading(true);
    setError('');
    setSummary({
      content: '',
      cached: false,
      timestamp: null
    });

    try {
      console.log('生成摘要，新闻ID:', newsId);
      const res = await generateSummary(newsId);
      console.log('摘要接口返回:', res);
      
      if (res.cached === 'true') {
        setSummary({
          content: res.summary,
          cached: true,
          timestamp: new Date().toISOString()
        });
        setSuccess('摘要生成完成（使用缓存）');
      } else if (res.taskId) {
        setSummaryTaskId(res.taskId);
        setSummaryPolling(true);
        setSuccess(res.message || '摘要生成中，请稍候...');
      } else {
        setError(res.message || '摘要生成失败');
      }
    } catch (err) {
      console.error('生成摘要失败:', err);
      setError('生成摘要失败，请稍后重试');
    } finally {
      setSummaryLoading(false);
    }
  };

  // 轮询摘要结果
  useEffect(() => {
    if (!summaryPolling || !summaryTaskId) return;

    let isMounted = true;
    let pollCount = 0;
    const maxPolls = 30;
    const pollInterval = 2000;

    const pollSummary = async () => {
      if (pollCount >= maxPolls) {
        if (isMounted) {
          setSummaryPolling(false);
          setError('摘要生成超时，请稍后重试');
        }
        return;
      }

      try {
        const res = await getSummaryResult(summaryTaskId);
        console.log('摘要轮询结果:', res);
        
        if (!isMounted) return;

        if (res.status === 'processing') {
          pollCount++;
          setTimeout(pollSummary, pollInterval);
        } else if (res.summary) {
          setSummary({
            content: res.summary,
            cached: false,
            timestamp: new Date().toISOString()
          });
          setSummaryPolling(false);
          setSuccess('摘要生成完成');
        } else {
          setSummaryPolling(false);
          setError(res.message || '获取摘要结果失败');
        }
      } catch (err) {
        console.error('摘要轮询失败:', err);
        if (isMounted) {
          setSummaryPolling(false);
          setError('获取摘要结果失败');
        }
      }
    };

    pollSummary();

    return () => {
      isMounted = false;
    };
  }, [summaryPolling, summaryTaskId]);

  // ========== 问答函数 ==========
  const handleAskQuestion = async () => {
    if (!isLoggedIn) {
      setError('请先登录以使用AI功能');
      return;
    }

    if (!question.trim()) {
      setError('请输入问题');
      return;
    }

    if (answerLoading) return;

    setAnswerLoading(true);
    setError('');
    setCurrentAnswer({
      answer: '',
      question: '',
      timestamp: null,
      cached: false
    });

    try {
      console.log('提问，新闻ID:', newsId, '问题:', question);
      const res = await askQuestion(newsId, question);
      console.log('提问接口返回:', res);
      
      if (res.cached === 'true' || res.cached === true) {
        const cachedAnswer = res.answer || res.content;
        const newAnswer = {
          answer: cachedAnswer,
          question: question,
          timestamp: new Date().toISOString(),
          cached: true
        };
        
        setCurrentAnswer(newAnswer);
        setAnswers(prev => [...prev, newAnswer]);
        setQuestion('');
        setSuccess('问题回答完成（使用缓存）');
      } else if (res.taskId) {
        setAnswerTaskId(res.taskId);
        setAnswerPolling(true);
        setCurrentQuestionForPolling(question);
        setSuccess(res.message || '问题处理中，请稍候...');
        setQuestion('');
      } else if (res.answer) {
        const newAnswer = {
          answer: res.answer,
          question: question,
          timestamp: new Date().toISOString(),
          cached: false
        };
        
        setCurrentAnswer(newAnswer);
        setAnswers(prev => [...prev, newAnswer]);
        setQuestion('');
        setSuccess('问题回答完成');
      } else {
        setError(res.message || '提问失败，响应格式不正确');
      }
    } catch (err) {
      console.error('提问失败:', err);
      console.error('错误详情:', err.response?.data);
      setError('提问失败，请稍后重试');
    } finally {
      setAnswerLoading(false);
    }
  };

  // 轮询问答结果
  useEffect(() => {
    if (!answerPolling || !answerTaskId) return;

    let isMounted = true;
    let pollCount = 0;
    const maxPolls = 30;
    const pollInterval = 2000;

    const pollAnswer = async () => {
      if (pollCount >= maxPolls) {
        if (isMounted) {
          setAnswerPolling(false);
          setError('问答处理超时，请稍后重试');
        }
        return;
      }

      try {
        console.log(`轮询第${pollCount + 1}次，taskId:`, answerTaskId);
        const res = await getQnAResult(answerTaskId);
        console.log('轮询结果:', res);
        
        if (!isMounted) return;

        if (res.status === 'processing') {
          pollCount++;
          setTimeout(pollAnswer, pollInterval);
        } else if (res.error === '无权访问此任务') {
          setAnswerPolling(false);
          setError('您无权访问此任务');
        } else if (res.answer) {
          const newAnswer = {
            answer: res.answer,
            question: currentQuestionForPolling,
            timestamp: new Date().toISOString(),
            cached: false
          };
          
          setCurrentAnswer(newAnswer);
          setAnswers(prev => [...prev, newAnswer]);
          setAnswerPolling(false);
          setCurrentQuestionForPolling('');
          setSuccess('问题回答完成');
        } else {
          setAnswerPolling(false);
          setError(res.message || '获取回答失败');
        }
      } catch (err) {
        console.error('轮询失败:', err);
        if (isMounted) {
          setAnswerPolling(false);
          setError('获取回答失败');
        }
      }
    };

    pollAnswer();

    return () => {
      isMounted = false;
    };
  }, [answerPolling, answerTaskId, currentQuestionForPolling]);

  // ========== 调试函数 ==========
  const testAnswerDisplay = () => {
    const testAnswer = {
      answer: "这是测试回答内容。测试AI助手能否正确回答问题，包括新闻的细节、背景信息等。如果这个回答能正常显示，说明前端显示功能正常。",
      question: "测试问题",
      timestamp: new Date().toISOString(),
      cached: false
    };
    
    console.log('测试设置回答:', testAnswer);
    setCurrentAnswer(testAnswer);
    setAnswers([testAnswer]);
    setSuccess('测试回答已显示');
  };

  const testSummaryDisplay = () => {
    const testSummary = {
      content: "康涅狄格州米尔福德20岁大学体操运动员梅拉妮·科尔曼在高低杠训练时脊髓受伤，随后不幸身亡。",
      cached: true,
      timestamp: new Date().toISOString()
    };
    
    console.log('测试设置摘要:', testSummary);
    setSummary(testSummary);
    setSuccess('测试摘要已显示');
  };

  // ========== 渲染 ==========
  return (
    <Paper elevation={2} sx={{ borderRadius: 3, overflow: 'hidden', mt: 4 }}>
      {/* 头部 */}
      <Box sx={{ 
        bgcolor: 'primary.main', 
        color: 'white', 
        p: 3,
        display: 'flex',
        alignItems: 'center',
        gap: 2
      }}>
        <SmartToy fontSize="large" />
        <Box>
          <Typography variant="h5" fontWeight="bold">
            AI 助手
          </Typography>
          <Typography variant="body2" sx={{ opacity: 0.9 }}>
            基于人工智能的新闻分析与问答
          </Typography>
        </Box>
      </Box>

      {/* 主体内容 */}
      <Box sx={{ p: 3 }}>
        {/* 登录提示 */}
        {!isLoggedIn && (
          <Alert severity="info" sx={{ mb: 3 }}>
            登录后可使用 AI 摘要生成和问答功能
          </Alert>
        )}

        {/* 错误和成功提示 */}
        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
            {error}
          </Alert>
        )}
        {success && (
          <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess('')}>
            {success}
          </Alert>
        )}

        {/* 调试按钮（开发环境使用） */}
        {isDevelopment() && (
          <Box sx={{ mb: 2, p: 2, bgcolor: 'grey.100', borderRadius: 1 }}>
            <Typography variant="caption" color="text.secondary">
              调试面板
            </Typography>
            <Box sx={{ display: 'flex', gap: 1, mt: 1 }}>
              <Button 
                size="small" 
                variant="outlined"
                onClick={testSummaryDisplay}
              >
                测试摘要显示
              </Button>
              <Button 
                size="small" 
                variant="outlined"
                onClick={testAnswerDisplay}
              >
                测试回答显示
              </Button>
            </Box>
          </Box>
        )}

        {/* 摘要生成区域 */}
        <Card variant="outlined" sx={{ mb: 3 }}>
          <CardContent>
            <Box sx={{ 
              display: 'flex', 
              justifyContent: 'space-between', 
              alignItems: 'center',
              cursor: 'pointer',
              mb: expandedSection === 'summary' ? 2 : 0
            }} onClick={() => setExpandedSection(expandedSection === 'summary' ? '' : 'summary')}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <AutoAwesome color="primary" />
                <Typography variant="h6" fontWeight="bold">
                  智能摘要
                </Typography>
                <Chip 
                  label="免费" 
                  size="small" 
                  color="success" 
                  variant="outlined"
                />
              </Box>
              <IconButton size="small">
                {expandedSection === 'summary' ? <ExpandLess /> : <ExpandMore />}
              </IconButton>
            </Box>

            <Collapse in={expandedSection === 'summary'}>
              <Box>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                  基于当前新闻内容，生成简明扼要的摘要
                </Typography>
                
                <Button
                  variant="contained"
                  startIcon={summaryLoading || summaryPolling ? 
                    <CircularProgress size={20} color="inherit" /> : 
                    <Lightbulb />
                  }
                  onClick={handleGenerateSummary}
                  disabled={summaryLoading || summaryPolling || !isLoggedIn}
                  sx={{ mb: 3 }}
                >
                  {summaryLoading ? '生成中...' : 
                   summaryPolling ? '处理中...' : 
                   '生成摘要'}
                </Button>

                {/* 使用 AnswerDisplay 组件显示摘要 */}
                {summary.content && (
                  <AnswerDisplay
                    answer={summary.content}
                    question="新闻摘要"
                    timestamp={summary.timestamp}
                    cached={summary.cached}
                    showFeedback={false}
                    type="summary"//
                    loading={summaryPolling}//
                    onCopy={() => handleCopyText(summary.content, 'summary')}
                    copySuccess={copySuccess.summary}
                  />
                )}
              </Box>
            </Collapse>
          </CardContent>
        </Card>

        {/* 问答区域 */}
        <Card variant="outlined">
          <CardContent>
            <Box sx={{ 
              display: 'flex', 
              justifyContent: 'space-between', 
              alignItems: 'center',
              cursor: 'pointer',
              mb: expandedSection === 'qa' ? 2 : 0
            }} onClick={() => setExpandedSection(expandedSection === 'qa' ? '' : 'qa')}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <QuestionAnswer color="primary" />
                <Typography variant="h6" fontWeight="bold">
                  智能问答
                </Typography>
                <Chip 
                  label="免费" 
                  size="small" 
                  color="success" 
                  variant="outlined"
                />
              </Box>
              <IconButton size="small">
                {expandedSection === 'qa' ? <ExpandLess /> : <ExpandMore />}
              </IconButton>
            </Box>

            <Collapse in={expandedSection === 'qa'}>
              <Box>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                  对新闻内容进行提问，获取详细解答
                </Typography>
                
                {/* 提问输入框 */}
                <Box sx={{ mb: 3 }}>
                  <TextField
                    fullWidth
                    multiline
                    rows={3}
                    value={question}
                    onChange={(e) => setQuestion(e.target.value)}
                    placeholder="请输入关于这篇新闻的问题..."
                    variant="outlined"
                    disabled={answerLoading || answerPolling || !isLoggedIn}
                    sx={{ mb: 2 }}
                  />
                  <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                    <Button
                      variant="contained"
                      startIcon={answerLoading || answerPolling ? 
                        <CircularProgress size={20} color="inherit" /> : 
                        <SmartToy />
                      }
                      onClick={handleAskQuestion}
                      disabled={answerLoading || answerPolling || !isLoggedIn || !question.trim()}
                    >
                      {answerLoading ? '提问中...' : 
                       answerPolling ? '思考中...' : 
                       '提问'}
                    </Button>
                  </Box>
                </Box>

                {/* 使用 AnswerDisplay 组件显示当前回答 */}
                {currentAnswer.answer && (
                  <Box sx={{ mb: 3 }}>
                    <AnswerDisplay
                      answer={currentAnswer.answer}
                      question={currentAnswer.question}
                      timestamp={currentAnswer.timestamp}
                      cached={currentAnswer.cached}
                      type="answer"//
                      loading={answerPolling}//
                      onCopy={() => handleCopyText(currentAnswer.answer, 'currentAnswer')}
                      copySuccess={copySuccess.currentAnswer}
                    />
                  </Box>
                )}

                {/* 历史问答记录 */}
                {answers.length > 0 && (
                  <Box>
                    <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 2 }}>
                      历史问答记录
                    </Typography>
                    <List sx={{ maxHeight: 300, overflow: 'auto' }}>
                      {[...answers].reverse().map((item, index) => (
                        <React.Fragment key={index}>
                          <ListItem alignItems="flex-start" sx={{ flexDirection: 'column' }}>
                            <Box sx={{ width: '100%', display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                              <Typography variant="body2" color="primary" fontWeight="medium">
                                Q: {item.question}
                              </Typography>
                              <Stack direction="row" spacing={1} alignItems="center">
                                <Typography variant="caption" color="text.secondary">
                                  {formatTime(item.timestamp)}
                                </Typography>
                                {item.cached && (
                                  <Chip 
                                    label="缓存" 
                                    size="small" 
                                    variant="outlined"
                                    sx={{ height: 20, fontSize: '0.7rem' }}
                                  />
                                )}
                              </Stack>
                            </Box>
                            <Box sx={{ 
                              width: '100%', 
                              bgcolor: 'grey.50', 
                              p: 2, 
                              borderRadius: 1,
                              position: 'relative'
                            }}>
                              <Typography variant="body2" sx={{ lineHeight: 1.7, whiteSpace: 'pre-wrap', color: 'black' }}>
                                {item.answer}
                              </Typography>
                              <Tooltip title="复制回答">
                                <IconButton 
                                  size="small"
                                  sx={{ position: 'absolute', top: 4, right: 4 }}
                                  onClick={() => handleCopyText(item.answer, `answer_${index}`)}
                                >
                                  <ContentCopy fontSize="small" />
                                </IconButton>
                              </Tooltip>
                            </Box>
                          </ListItem>
                          {index < answers.length - 1 && <Divider variant="middle" />}
                        </React.Fragment>
                      ))}
                    </List>
                  </Box>
                )}
              </Box>
            </Collapse>
          </CardContent>
        </Card>

        {/* 使用提示 */}
        <Alert severity="info" sx={{ mt: 3 }}>
          <Typography variant="body2">
            💡 提示：
            1. 摘要生成和问答可能需要一些时间处理
            2. 相同问题的回答会使用缓存，提高响应速度
            3. 每个用户每小时最多使用10次AI功能
          </Typography>
        </Alert>
      </Box>
    </Paper>
  );
};

export default NewsAI;