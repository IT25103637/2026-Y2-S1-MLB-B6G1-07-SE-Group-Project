package com.safari.wildlife.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.core.io.ByteArrayResource;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // This dynamically grabs your email from application.properties!
    @Value("${spring.mail.username}")
    private String systemEmail;

    // @Async tells Spring to run this in the background!
    @Async
    public void sendBookingApprovalEmail(String toEmail, String customerName, String bookingRef) {
        SimpleMailMessage message = new SimpleMailMessage();

        // This sets the professional display name
        message.setFrom("Wild-Life Safari Sri Lanka <" + systemEmail + ">");

        message.setTo(toEmail);
        message.setSubject("Safari Booking Approved! - Ref: " + bookingRef);
        message.setText("Dear " + customerName + ",\n\n"
                + "Great news! Your safari booking (" + bookingRef + ") has been reviewed and approved by our Booking Manager.\n\n"
                + "Please log in to your dashboard to complete the payment and secure your 4x4 Jeep and Guide.\n\n"
                + "Best Regards,\n"
                + "Wild-Life Safari Sri Lanka Team");

        try {
            mailSender.send(message);
            System.out.println("Approval email sent successfully to " + toEmail);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }

    @Async
    public void sendSupportReplyEmail(String toEmail, String customerName, String ticketRef, String replyMessage) {
        SimpleMailMessage message = new SimpleMailMessage();

        // Uses the same professional sender name
        message.setFrom("Wild-Life Safari Sri Lanka Support <" + systemEmail + ">");

        message.setTo(toEmail);
        message.setSubject("Update on Your Support Request (" + ticketRef + ")");
        message.setText("Dear " + customerName + ",\n\n"
                + "Our team has responded to your recent support request:\n\n"
                + "-------------------------------------------------\n"
                + replyMessage + "\n"
                + "-------------------------------------------------\n\n"
                + "You can view the full details of this ticket by logging into your customer dashboard.\n\n"
                + "Best Regards,\n"
                + "Wild-Life Safari Sri Lanka Team");

        try {
            mailSender.send(message);
            System.out.println("Support reply email sent successfully to " + toEmail);
        } catch (Exception e) {
            System.err.println("Failed to send support email: " + e.getMessage());
        }
    }

    @Async
    public void sendBookingReceiptEmail(String toEmail, String customerName, String bookingRef, byte[] pdfBytes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // The 'true' flag means this is a multipart message (supports attachments)
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom("Wild-Life Safari Sri Lanka <" + systemEmail + ">");
            helper.setTo(toEmail);
            helper.setSubject("Your Safari Receipt & Confirmation - " + bookingRef);

            helper.setText("Dear " + customerName + ",\n\n"
                    + "Your payment for booking " + bookingRef + " was successful!\n\n"
                    + "Please find your official receipt attached to this email. You can present this PDF to your guide on the day of your safari.\n\n"
                    + "Best Regards,\n"
                    + "Wild-Life Safari Sri Lanka Team");

            // Attach the PDF byte array
            helper.addAttachment("Safari_Receipt_" + bookingRef + ".pdf", new ByteArrayResource(pdfBytes));

            mailSender.send(message);
            System.out.println("Receipt email sent successfully to " + toEmail);

        } catch (Exception e) {
            System.err.println("Failed to send receipt email: " + e.getMessage());
        }
    }
}