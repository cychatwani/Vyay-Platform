-- =====================================================================
    -- V4__email_otp_notification_template.sql
    -- Notification template for email-based OTP verification
    -- =====================================================================

    INSERT INTO notification_templates (id, type, subject, body, channel)
    VALUES (
        uuidv7(),
        'EMAIL_VERIFICATION_OTP',
        'Your Vyay verification code',
        'Hi {{firstName}}, your verification code is {{otp}}. It expires in {{expiryMinutes}} minutes.',
        'EMAIL'
    )
    ON CONFLICT (type) DO NOTHING;