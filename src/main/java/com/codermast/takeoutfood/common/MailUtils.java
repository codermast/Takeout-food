package com.codermast.takeoutfood.common;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import java.util.Date;

/**
 * @Description: 邮件工具类
 * @author: CoderMast
 * @date: 2022/12/1
 * @Blog: <a href="https://www.codermast.com/">codermast</a>
 */
@Component
public class MailUtils {
    /**
     * 注入邮件工具类
     */
    @Autowired
    private JavaMailSenderImpl javaMailSender;

    @Value("${spring.mail.username}")
    private String sendMailer;

    /**
     * 检测邮件信息类
     *
     * @param to
     * @param subject
     * @param text
     */
    private void checkMail(String to, String subject, String text) {
        if (StringUtils.isEmpty(to)) {
            throw new RuntimeException("邮件收信人不能为空");
        }
        if (StringUtils.isEmpty(subject)) {
            throw new RuntimeException("邮件主题不能为空");
        }
        if (StringUtils.isEmpty(text)) {
            throw new RuntimeException("邮件内容不能为空");
        }
    }

    /**
     * 发送纯文本邮件
     *
     * @param to
     * @param subject
     * @param text
     */
    public void sendTextMailMessage(String to, String subject, String text) {

        try {
            //true 代表支持复杂的类型
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(javaMailSender.createMimeMessage(), true);
            //邮件发信人
            mimeMessageHelper.setFrom(sendMailer);
            //邮件收信人  1或多个
            mimeMessageHelper.setTo(to.split(","));
            //邮件主题
            mimeMessageHelper.setSubject(subject);
            //邮件内容
            mimeMessageHelper.setText(text);
            //邮件发送时间
            mimeMessageHelper.setSentDate(new Date());

            //发送邮件
            javaMailSender.send(mimeMessageHelper.getMimeMessage());
            System.out.println("发送邮件成功：" + sendMailer + "->" + to);

        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("发送邮件失败：" + e.getMessage());
        }
    }
}

