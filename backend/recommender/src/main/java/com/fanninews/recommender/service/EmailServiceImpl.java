package com.fanninews.recommender.service;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Future;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {


    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.email.from-name:新闻推荐系统}")
    private String fromName;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.email.reset-password.expire-hours:2}")
    private int resetPasswordExpireHours;

    /**
     * 发送重置密码邮件
     */
    @Override
    //@Async("emailTaskExecutor")
    public Future<Void> sendResetPasswordEmail(String toEmail, String username, String resetToken) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            String subject = "密码重置请求 - 新闻推荐系统";
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
            String content = buildResetPasswordEmailHtml(username, resetLink, resetPasswordExpireHours);
            sendHtmlEmail(toEmail, subject, content);
            log.info("重置密码邮件已发送: to={}, username={}", toEmail, username);
            future.complete(null); // 成功完成
        } catch (Exception e) {
            log.error("发送重置密码邮件失败: to={}, username={}", toEmail, username, e);
            future.completeExceptionally(e); // 封装异常
        }
        return future;
    }

    /**
     * 发送密码修改成功邮件
     */
    @Override
    @Async("emailTaskExecutor")
    public Future<Void> sendPasswordChangedEmail(String toEmail, String username) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {

            String subject = "密码修改成功 - 新闻推荐系统";
            String content = buildPasswordChangedEmailHtml(username);

            sendHtmlEmail(toEmail, subject, content);
            log.info("密码修改成功邮件已发送: to={}, username={}", toEmail, username);
            return CompletableFuture.completedFuture(null);
        }catch(Exception e){
            log.error("发送重置密码邮件失败", e);
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * 发送验证邮件
     */
    @Override
    @Async("emailTaskExecutor")
    public Future<Void> sendVerificationEmail(String toEmail, String username, String verificationToken) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            String subject = "邮箱验证 - 新闻推荐系统";
            String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;
            String content = buildVerificationEmailHtml(username, verificationLink);
            sendHtmlEmail(toEmail, subject, content);
            log.info("邮箱验证邮件已发送: to={}, username={}", toEmail, username);
            return CompletableFuture.completedFuture(null);
        }catch (Exception e){

            log.error("发送重置密码邮件失败", e);
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * 发送通用HTML邮件
     */
    @Override
    @Async("emailTaskExecutor")
    public void sendEmail(String toEmail, String subject, String content) {
        try {
            sendHtmlEmail(toEmail, subject, content);
            log.info("通用HTML邮件已发送: to={}, subject={}", toEmail, subject);
        } catch (Exception e) {
            log.error("发送通用HTML邮件失败: to={}, subject={}", toEmail, subject, e);
            throw new RuntimeException("通用邮件发送失败", e);
        }
    }

    /**
     * 发送纯文本邮件
     */
    @Override
    @Async("emailTaskExecutor")
    public void sendTextEmail(String toEmail, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);

            mailSender.send(message);
            log.debug("纯文本邮件发送成功: to={}, subject={}", toEmail, subject);
        } catch (Exception e) {
            log.error("发送纯文本邮件失败: to={}, error={}", toEmail, e.getMessage());
            throw new RuntimeException("邮件发送失败");
        }
    }

    // ============ 私有方法 ============

    /**
     * 发送HTML邮件
     */
    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        try {
//            MimeMessage message = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
//
//            // 设置发件人
//            helper.setFrom(String.format("%s <%s>", fromName, fromEmail));
//            helper.setTo(toEmail);
//            helper.setSubject(subject);
//            helper.setText(htmlContent, true);  // true表示是HTML
//
//            mailSender.send(message);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 修复：QQ邮箱不支持别名格式，直接填发件人邮箱
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);  // true表示是HTML

            mailSender.send(message);

            log.debug("HTML邮件发送成功: to={}, subject={}", toEmail, subject);
        } catch (MessagingException e) {
            log.error("发送HTML邮件失败: to={}, error={}", toEmail, e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("邮件发送异常: to={}, error={}", toEmail, e.getMessage());
            throw new RuntimeException("邮件发送异常");
        }
    }

    /**
     * 构建重置密码邮件HTML内容
     */
    private String buildResetPasswordEmailHtml(String username, String resetLink, int expireHours) {
        return """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>密码重置</title>
            <style>
                body {
                    font-family: 'Helvetica Neue', Arial, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    margin: 0;
                    padding: 20px;
                    background-color: #f5f5f5;
                }
                .container {
                    max-width: 600px;
                    margin: 0 auto;
                    background-color: #ffffff;
                    border-radius: 10px;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    overflow: hidden;
                }
                .header {
                    background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                    color: white;
                    padding: 30px;
                    text-align: center;
                }
                .header h1 {
                    margin: 0;
                    font-size: 24px;
                }
                .content {
                    padding: 30px;
                }
                .greeting {
                    font-size: 18px;
                    margin-bottom: 20px;
                    color: #555;
                }
                .reset-info {
                    background-color: #f8f9fa;
                    border-left: 4px solid #667eea;
                    padding: 15px;
                    margin: 20px 0;
                    border-radius: 0 5px 5px 0;
                }
                .button-container {
                    text-align: center;
                    margin: 30px 0;
                }
                .reset-button {
                    display: inline-block;
                    background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                    color: white;
                    padding: 14px 28px;
                    text-decoration: none;
                    border-radius: 25px;
                    font-weight: bold;
                    font-size: 16px;
                    transition: transform 0.2s, box-shadow 0.2s;
                }
                .reset-button:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
                }
                .link-text {
                    word-break: break-all;
                    color: #666;
                    font-size: 14px;
                    margin-top: 15px;
                    padding: 10px;
                    background-color: #f8f9fa;
                    border-radius: 5px;
                }
                .warning {
                    color: #e74c3c;
                    background-color: #ffebee;
                    padding: 12px;
                    border-radius: 5px;
                    margin: 20px 0;
                    font-size: 14px;
                }
                .footer {
                    margin-top: 30px;
                    padding-top: 20px;
                    border-top: 1px solid #eee;
                    text-align: center;
                    color: #999;
                    font-size: 12px;
                }
                .signature {
                    margin-top: 10px;
                    color: #667eea;
                    font-weight: bold;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>🔐 密码重置请求</h1>
                </div>
                <div class="content">
                    <p class="greeting">亲爱的 <strong>%s</strong>，您好！</p>
                    
                    <div class="reset-info">
                        <p>我们收到了您重置密码的请求。请点击下方按钮重置您的密码：</p>
                    </div>
                    
                    <div class="button-container">
                        <a href="%s" class="reset-button" target="_blank">
                            立即重置密码
                        </a>
                    </div>
                    
                    <p class="link-text">如果按钮无法点击，请复制以下链接到浏览器打开：<br>
                    <a href="%s">%s</a></p>
                    
                    <div class="warning">
                        ⚠️ <strong>安全提示：</strong><br>
                        1. 此链接将在 %d 小时后失效<br>
                        2. 如果您没有请求重置密码，请忽略此邮件<br>
                        3. 请不要将此链接分享给他人
                    </div>
                    
                    <p>如果您有任何问题，请随时联系我们。</p>
                    
                    <div class="footer">
                        <p>此邮件由系统自动发送，请勿直接回复。</p>
                        <p class="signature">—— 新闻推荐系统团队 ——</p>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.formatted(username, resetLink, resetLink, resetLink, expireHours);
    }

    /**
     * 构建密码修改成功邮件HTML内容
     */
    private String buildPasswordChangedEmailHtml(String username) {
        return """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>密码修改成功</title>
            <style>
                body {
                    font-family: 'Helvetica Neue', Arial, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    margin: 0;
                    padding: 20px;
                    background-color: #f5f5f5;
                }
                .container {
                    max-width: 600px;
                    margin: 0 auto;
                    background-color: #ffffff;
                    border-radius: 10px;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    overflow: hidden;
                }
                .header {
                    background: linear-gradient(135deg, #4CAF50 0%%, #2E7D32 100%%);
                    color: white;
                    padding: 30px;
                    text-align: center;
                }
                .header h1 {
                    margin: 0;
                    font-size: 24px;
                }
                .content {
                    padding: 30px;
                }
                .success-icon {
                    text-align: center;
                    font-size: 60px;
                    color: #4CAF50;
                    margin: 20px 0;
                }
                .greeting {
                    font-size: 18px;
                    margin-bottom: 20px;
                    color: #555;
                    text-align: center;
                }
                .message {
                    background-color: #e8f5e9;
                    border-radius: 8px;
                    padding: 20px;
                    margin: 20px 0;
                    text-align: center;
                }
                .message h3 {
                    color: #2E7D32;
                    margin-top: 0;
                }
                .warning {
                    background-color: #fff3e0;
                    border-left: 4px solid #ff9800;
                    padding: 15px;
                    margin: 20px 0;
                    border-radius: 0 5px 5px 0;
                }
                .tips {
                    background-color: #e3f2fd;
                    border-radius: 8px;
                    padding: 15px;
                    margin: 20px 0;
                }
                .tips h4 {
                    color: #1976d2;
                    margin-top: 0;
                }
                .footer {
                    margin-top: 30px;
                    padding-top: 20px;
                    border-top: 1px solid #eee;
                    text-align: center;
                    color: #999;
                    font-size: 12px;
                }
                .signature {
                    margin-top: 10px;
                    color: #4CAF50;
                    font-weight: bold;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>✅ 密码修改成功</h1>
                </div>
                <div class="content">
                    <div class="success-icon">✓</div>
                    
                    <p class="greeting">亲爱的 <strong>%s</strong>，您好！</p>
                    
                    <div class="message">
                        <h3>您的密码已经成功修改！</h3>
                        <p>密码修改时间：%s</p>
                    </div>
                    
                    <div class="warning">
                        <strong>⚠️ 安全提示：</strong><br>
                        • 如果您没有进行此操作，您的账户可能已经泄露<br>
                        • 请立即<a href="%s/reset-password">重新设置密码</a><br>
                        • 并检查账户最近的活动记录
                    </div>
                    
                    <div class="tips">
                        <h4>📝 账户安全建议：</h4>
                        <ul>
                            <li>定期更换密码（建议每3个月一次）</li>
                            <li>不要使用与其他网站相同的密码</li>
                            <li>避免使用简单的密码（如生日、电话号码等）</li>
                            <li>启用双重验证（如果支持）</li>
                        </ul>
                    </div>
                    
                    <p>感谢您使用我们的服务！</p>
                    
                    <div class="footer">
                        <p>此邮件由系统自动发送，请勿直接回复。</p>
                        <p class="signature">—— 新闻推荐系统团队 ——</p>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.formatted(username,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                frontendUrl);
    }

    /**
     * 构建邮箱验证邮件HTML内容
     */
    private String buildVerificationEmailHtml(String username, String verificationLink) {
        return """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>邮箱验证</title>
            <style>
                body {
                    font-family: 'Helvetica Neue', Arial, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    margin: 0;
                    padding: 20px;
                    background-color: #f5f5f5;
                }
                .container {
                    max-width: 600px;
                    margin: 0 auto;
                    background-color: #ffffff;
                    border-radius: 10px;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    overflow: hidden;
                }
                .header {
                    background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                    color: white;
                    padding: 30px;
                    text-align: center;
                }
                .header h1 {
                    margin: 0;
                    font-size: 24px;
                }
                .content {
                    padding: 30px;
                }
                .greeting {
                    font-size: 18px;
                    margin-bottom: 20px;
                    color: #555;
                    text-align: center;
                }
                .button-container {
                    text-align: center;
                    margin: 30px 0;
                }
                .reset-button {
                    display: inline-block;
                    background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                    color: white;
                    padding: 14px 28px;
                    text-decoration: none;
                    border-radius: 25px;
                    font-weight: bold;
                    font-size: 16px;
                }
                .link-text {
                    word-break: break-all;
                    color: #666;
                    font-size: 14px;
                    margin-top: 15px;
                    padding: 10px;
                    background-color: #f8f9fa;
                    border-radius: 5px;
                    text-align: center;
                }
                .footer {
                    margin-top: 30px;
                    padding-top: 20px;
                    border-top: 1px solid #eee;
                    text-align: center;
                    color: #999;
                    font-size: 12px;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>📧 邮箱验证</h1>
                </div>
                <div class="content">
                    <p class="greeting">亲爱的 <strong>%s</strong>，您好！</p>
                    <p style="text-align:center; font-size:16px;">感谢您注册新闻推荐系统！请完成邮箱验证以激活账户</p>
                    
                    <div class="button-container">
                        <a href="%s" class="reset-button">验证邮箱</a>
                    </div>
                    
                    <p class="link-text">如果按钮无法点击，请复制以下地址到浏览器：<br>%s</p>
                    
                    <div class="footer">
                        <p>此邮件由系统自动发送，请勿直接回复。</p>
                        <p>—— 新闻推荐系统团队 ——</p>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.formatted(username, verificationLink, verificationLink);
    }
}
