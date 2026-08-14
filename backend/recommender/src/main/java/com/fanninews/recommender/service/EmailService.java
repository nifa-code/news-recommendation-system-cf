package com.fanninews.recommender.service;

import java.util.concurrent.Future;

public interface EmailService {

    /**
     * 发送重置密码邮件
     * @param toEmail 收件人邮箱
     * @param username 用户名
     * @param resetToken 重置令牌
     */
    Future<Void> sendResetPasswordEmail(String toEmail, String username, String resetToken);

    /**
     * 发送密码修改成功邮件
     * @param toEmail 收件人邮箱
     * @param username 用户名
     */
    Future<Void> sendPasswordChangedEmail(String toEmail, String username);

    /**
     * 发送验证邮件
     * @param toEmail 收件人邮箱
     * @param username 用户名
     * @param verificationToken 验证令牌
     */
    Future<Void> sendVerificationEmail(String toEmail, String username, String verificationToken);

    /**
     * 发送通用邮件
     * @param toEmail 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容（支持HTML）
     */
    void sendEmail(String toEmail, String subject, String content);

    /**
     * 发送纯文本邮件
     * @param toEmail 收件人邮箱
     * @param subject 邮件主题
     * @param text 纯文本内容
     */
    void sendTextEmail(String toEmail, String subject, String text);
}
