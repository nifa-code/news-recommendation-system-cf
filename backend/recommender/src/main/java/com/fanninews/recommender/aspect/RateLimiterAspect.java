package com.fanninews.recommender.aspect;

import com.fanninews.recommender.Exception.RateLimitException;
import com.fanninews.recommender.utils.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

@Aspect
@Component
@Slf4j
public class RateLimiterAspect {
    @Autowired
    StringRedisTemplate StringRedisTemplate;
    @Autowired
    private RedisScript<Long> limitScript;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    @Before("@annotation(rateLimiter)")
    public void doBefore(JoinPoint point, RateLimiter rateLimiter) throws Throwable {

        String combineKey = getCombineKey(rateLimiter, point);
        List<String> keys = Collections.singletonList(combineKey);
        Long result = StringRedisTemplate.execute(limitScript, keys, String.valueOf(rateLimiter.count()), String.valueOf(rateLimiter.time()));
        if (result == null) {
            log.error("限流脚本执行返回null，降级处理");
            if (!rateLimiter.fallback()) {
                throw new RuntimeException("系统繁忙，请稍后再试");
            }
            return; // 降级，允许请求
        }
        if (result == -1) {
            log.error("限流参数错误，降级处理");
            if (!rateLimiter.fallback()) {
                throw new RuntimeException("系统繁忙，请稍后再试");
            }
            return; // 降级，允许请求
        }
        if (result != null&&result.intValue() > rateLimiter.count()) {
            log.warn("接口限流触发，key: {}， 限制次数: {}， 当前请求数: {}", combineKey, rateLimiter.count(), result);
            throw new RateLimitException("访问过于频繁，请稍后再试");
        }
    }
    private String getCombineKey(RateLimiter rateLimiter, JoinPoint point) {
        String keyTemplate=rateLimiter.key();
        if (keyTemplate.contains("#") || keyTemplate.contains("$")){
            try {
                return parseSpelKey(keyTemplate, rateLimiter, point);
            } catch (Exception e) {
                log.warn("SpEL解析失败，降级使用传统模式。错误:", e);
                return buildFallbackKey(keyTemplate, point);
            }
        }
        return getLegacyCombineKey(rateLimiter,point);
    }
    private String parseSpelKey(String keyTemplate, RateLimiter rateLimiter, JoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Object[] args = point.getArgs();
        Object target = point.getTarget();
        EvaluationContext context = new StandardEvaluationContext();
        context.setVariable("target", target);
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        context.setVariable("userId", getCurrentUserId());
        context.setVariable("ip", getClientIp());
        try {
            Expression expression = parser.parseExpression(keyTemplate,
                    new TemplateParserContext("{", "}")); // 指定表达式前缀和后缀是 {}
            String resolvedKey = expression.getValue(context, String.class);
            return resolvedKey + ":" + signature.getDeclaringTypeName() + "." + method.getName();

        } catch (Exception e) {
            log.warn("解析限流Key的SpEL表达式失败，将使用原始模板。模板: {}, 错误: {}",
            keyTemplate, e.getMessage());
            return getLegacyCombineKey(rateLimiter, point);
        }
    }

    private String buildFallbackKey(String keyTemplate, JoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        return keyTemplate + ":" + signature.getDeclaringTypeName() + "." + signature.getName();
    }

    private String getLegacyCombineKey(RateLimiter rateLimiter, JoinPoint point) {
        StringBuilder keyBuilder=new StringBuilder();
        keyBuilder.append(rateLimiter.key());

        switch(rateLimiter.limitType()){
            case IP:
                String ip=getClientIp();
                if(ip!=null&&!ip.isEmpty()){
                    keyBuilder.append(":").append(ip);
                }
                break;
            case USER:
                String userId=getCurrentUserId();
                if(userId!=null&&!userId.isEmpty()){
                    keyBuilder.append(":").append(userId);
                }
                break;
            case DEFAULT:
                break;
        }
        MethodSignature signature = (MethodSignature) point.getSignature();
        keyBuilder.append(":")
                .append(signature.getDeclaringTypeName())  // 类名
                .append(".")
                .append(signature.getName());              // 方法名

        return keyBuilder.toString();
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return "unknown";
            }
            HttpServletRequest request = attributes.getRequest();
            String[] headerNames = {
                    "X-Forwarded-For",
                    "Proxy-Client-IP",
                    "WL-Proxy-Client-IP",
                    "HTTP_CLIENT_IP",
                    "HTTP_X_FORWARDED_FOR",
                    "X-Real-IP"
            };
            String ip = null;
            for (String header : headerNames) {
                ip = request.getHeader(header);
                if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                    break;
                }
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }

            // 多个代理时取第一个IP（X-Forwarded-For: client, proxy1, proxy2）
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }

            // 处理IPv6本地地址和IPv4映射
            if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
                ip = "127.0.0.1"; // 转换为IPv4本地地址
            }

            return ip == null ? "unknown" : ip;
        } catch (Exception e) {
            log.warn("获取客户端IP失败: {}", e.getMessage());
            return "unknown";
        }
    }

    private String getCurrentUserId() {
        try {
            log.debug("=== 开始获取用户ID ===");
            ServletRequestAttributes attributes = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                log.debug("RequestContextHolder中没有请求属性");
            } else {
                log.debug("请求URL: {}", attributes.getRequest().getRequestURL());
            }
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            log.debug("认证对象: {}", authentication);
            log.debug("是否已认证: {}", authentication != null && authentication.isAuthenticated());
            log.debug("是否匿名: {}", authentication instanceof AnonymousAuthenticationToken);
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                log.debug("Principal类型: {}", principal.getClass().getName());
                log.debug("Principal值: {}", principal);
                if (principal instanceof UserDetails) {
                    return ((UserDetails) principal).getUsername();
                } else if (principal instanceof String) {
                    return (String) principal;
                }
            }
        } catch (Exception e) {
            log.warn("获取当前用户ID失败: {}", e.getMessage());
        }
        return null;
    }

}
