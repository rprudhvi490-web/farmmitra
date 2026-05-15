package com.weekendbasket.app.otp;

import com.weekendbasket.app.exception.WeekendBasketException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import jakarta.annotation.PostConstruct;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "otp.provider", havingValue = "sns")
public class AwsSnsOtpStrategy implements OtpSendStrategy {

    private static final Logger log = LogManager.getLogger(AwsSnsOtpStrategy.class);

    @Value("${aws.access.key.id}")
    private String accessKeyId;

    @Value("${aws.secret.access.key}")
    private String secretAccessKey;

    @Value("${aws.sns.region}")
    private String region;

    private SnsClient snsClient;

    @PostConstruct
    public void init() {
        snsClient = SnsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .build();
        log.info("AWS SNS OTP strategy initialized — region: {}", region);
    }

    @Override
    public void send(String phoneNumber, String otp) {
        // E.164 format required by SNS: +91XXXXXXXXXX
        String e164Phone = "+91" + phoneNumber;
        String message   = "Your WeekendBasket OTP is: " + otp + ". Valid for 5 minutes. Do not share.";

        // NOTE: SNS SMS is charged per message (~$0.00645/SMS for India).
        // Keep otp.provider=dev during development to avoid charges.
        // Switch to otp.provider=sns only for UAT with real phones or production.
        /*
        try {
            PublishRequest request = PublishRequest.builder()
                    .phoneNumber(e164Phone)
                    .message(message)
                    .messageAttributes(Map.of(
                            // Transactional = higher delivery priority, not filtered by DND
                            "AWS.SNS.SMS.SMSType", MessageAttributeValue.builder()
                                    .dataType("String")
                                    .stringValue("Transactional")
                                    .build(),
                            // Sender ID shown on phone (max 11 chars, no spaces)
                            "AWS.SNS.SMS.SenderID", MessageAttributeValue.builder()
                                    .dataType("String")
                                    .stringValue("WKNDBASKT")
                                    .build()
                    ))
                    .build();

            snsClient.publish(request);
            log.info("SNS OTP sent to {}", e164Phone);

        } catch (Exception e) {
            log.error("SNS OTP send failed for {}: {}", e164Phone, e.getMessage());
            throw new WeekendBasketException("Failed to send OTP. Please try again.");
        }
        */
        log.warn("SNS OTP strategy is disabled to avoid charges. Switch otp.provider=sns to enable. Phone: {}", e164Phone);
        throw new WeekendBasketException("SMS OTP is currently disabled. Use dev mode for testing.");
    }
}
