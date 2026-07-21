package com.advertisementdesign.back.service;

import com.advertisementdesign.back.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EmailCodeMailService {
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String configuredFrom;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public void sendCode(String email, String code) {
        String from = StringUtils.hasText(configuredFrom) ? configuredFrom : mailUsername;
        if (!StringUtils.hasText(from)) {
            throw new ApiException(500, "邮件服务尚未配置，请联系管理员");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("广告设计工作台邮箱验证码");
        message.setText("您的验证码是：" + code + "。验证码 60 秒内有效，请勿转发给他人。");
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new ApiException(500, "验证码邮件发送失败，请稍后重试");
        }
    }
}
