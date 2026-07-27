    -- =====================================================================
    -- V2__reference_data.sql
    -- Initial reference/master data
    -- =====================================================================

    -- ---------------------------------------------------------------------
    -- Notification Templates
    -- ---------------------------------------------------------------------

    CREATE EXTENSION IF NOT EXISTS pgcrypto;

    INSERT INTO notification_templates (id, type, subject, body, channel)
    VALUES (
        uuidv7(),
        'EMAIL_VERIFICATION',
        'Verify your email',
        'Hi {{firstName}}, please verify your email by clicking: {{link}}',
        'EMAIL'
    )
    ON CONFLICT (type) DO NOTHING;


    -- ---------------------------------------------------------------------
    -- Languages
    -- ---------------------------------------------------------------------

    INSERT INTO languages (id, code, name, native_name)
    VALUES
    (uuidv7(), 'en', 'English', 'English')
    ON CONFLICT (code) DO NOTHING;


    -- ---------------------------------------------------------------------
    -- Currencies
    -- ---------------------------------------------------------------------

    INSERT INTO currencies (id, code, name, symbol, decimal_places)
    VALUES
    (uuidv7(), 'INR', 'Indian Rupee', '₹', 2),
    (uuidv7(), 'USD', 'US Dollar', '$', 2),
    (uuidv7(), 'EUR', 'Euro', '€', 2),
    (uuidv7(), 'GBP', 'British Pound', '£', 2),
    (uuidv7(), 'JPY', 'Japanese Yen', '¥', 0),
    (uuidv7(), 'AUD', 'Australian Dollar', 'A$', 2),
    (uuidv7(), 'CAD', 'Canadian Dollar', 'C$', 2),
    (uuidv7(), 'CHF', 'Swiss Franc', 'CHF', 2),
    (uuidv7(), 'CNY', 'Chinese Yuan', '¥', 2),
    (uuidv7(), 'SGD', 'Singapore Dollar', 'S$', 2),
    (uuidv7(), 'AED', 'UAE Dirham', 'د.إ', 2),
    (uuidv7(), 'SAR', 'Saudi Riyal', '﷼', 2),
    (uuidv7(), 'THB', 'Thai Baht', '฿', 2),
    (uuidv7(), 'MYR', 'Malaysian Ringgit', 'RM', 2),
    (uuidv7(), 'IDR', 'Indonesian Rupiah', 'Rp', 2),
    (uuidv7(), 'KRW', 'South Korean Won', '₩', 0),
    (uuidv7(), 'BRL', 'Brazilian Real', 'R$', 2),
    (uuidv7(), 'ZAR', 'South African Rand', 'R', 2),
    (uuidv7(), 'NZD', 'New Zealand Dollar', 'NZ$', 2),
    (uuidv7(), 'SEK', 'Swedish Krona', 'kr', 2),
    (uuidv7(), 'NOK', 'Norwegian Krone', 'kr', 2),
    (uuidv7(), 'DKK', 'Danish Krone', 'kr', 2),
    (uuidv7(), 'HKD', 'Hong Kong Dollar', 'HK$', 2),
    (uuidv7(), 'TWD', 'Taiwan Dollar', 'NT$', 2),
    (uuidv7(), 'PKR', 'Pakistani Rupee', '₨', 2),
    (uuidv7(), 'BDT', 'Bangladeshi Taka', '৳', 2),
    (uuidv7(), 'LKR', 'Sri Lankan Rupee', 'Rs', 2),
    (uuidv7(), 'NPR', 'Nepalese Rupee', 'Rs', 2),
    (uuidv7(), 'KWD', 'Kuwaiti Dinar', 'د.ك', 3),
    (uuidv7(), 'BHD', 'Bahraini Dinar', 'BD', 3),
    (uuidv7(), 'OMR', 'Omani Rial', '﷼', 3)
    ON CONFLICT (code) DO NOTHING;