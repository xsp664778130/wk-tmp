package com.skillport.server.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordResetService {
    private final PasswordResetStore resetStore;
    private final PasswordResetMailSender mailSender;

    public PasswordResetService(PasswordResetStore resetStore, PasswordResetMailSender mailSender) {
        this.resetStore = resetStore;
        this.mailSender = mailSender;
    }

    public void requestCode(String email) {
        if (!mailSender.configured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "邮件验证码服务尚未配置");
        }
        PasswordResetStore.IssuedCode issued = resetStore.issueCode(email);
        if (issued == null) return;
        try {
            mailSender.sendCode(issued.email(), issued.displayName(), issued.code());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "验证码邮件暂时发送失败，请稍后重试", exception);
        }
    }

    public void resetPassword(String email, String code, String newPassword) {
        resetStore.resetPassword(email, code, newPassword);
    }
}
