-- Notification Templates
INSERT INTO notification_templates (id, type, subject, body, channel)
SELECT gen_random_uuid(), 'EMAIL_VERIFICATION', 'Verify your email', 'Hi {{firstName}}, please verify your email by clicking: {{link}}', 'EMAIL'
WHERE NOT EXISTS (SELECT 1 FROM notification_templates WHERE type = 'EMAIL_VERIFICATION' AND channel = 'EMAIL');

-- Languages
INSERT INTO languages (id, code, name, native_name)
SELECT gen_random_uuid(), 'en', 'English', 'English'
WHERE NOT EXISTS (SELECT 1 FROM languages WHERE code = 'en');

-- Currencies
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'INR', 'Indian Rupee', '₹', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'INR');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'USD', 'US Dollar', '$', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'USD');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'EUR', 'Euro', '€', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'EUR');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'GBP', 'British Pound', '£', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'GBP');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'JPY', 'Japanese Yen', '¥', 0 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'JPY');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'AUD', 'Australian Dollar', 'A$', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'AUD');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'CAD', 'Canadian Dollar', 'C$', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'CAD');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'CHF', 'Swiss Franc', 'CHF', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'CHF');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'CNY', 'Chinese Yuan', '¥', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'CNY');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'SGD', 'Singapore Dollar', 'S$', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'SGD');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'AED', 'UAE Dirham', 'د.إ', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'AED');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'SAR', 'Saudi Riyal', '﷼', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'SAR');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'THB', 'Thai Baht', '฿', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'THB');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'MYR', 'Malaysian Ringgit', 'RM', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'MYR');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'IDR', 'Indonesian Rupiah', 'Rp', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'IDR');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'KRW', 'South Korean Won', '₩', 0 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'KRW');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'BRL', 'Brazilian Real', 'R$', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'BRL');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'ZAR', 'South African Rand', 'R', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'ZAR');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'NZD', 'New Zealand Dollar', 'NZ$', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'NZD');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'SEK', 'Swedish Krona', 'kr', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'SEK');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'NOK', 'Norwegian Krone', 'kr', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'NOK');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'DKK', 'Danish Krone', 'kr', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'DKK');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'HKD', 'Hong Kong Dollar', 'HK$', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'HKD');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'TWD', 'Taiwan Dollar', 'NT$', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'TWD');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'PKR', 'Pakistani Rupee', '₨', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'PKR');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'BDT', 'Bangladeshi Taka', '৳', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'BDT');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'LKR', 'Sri Lankan Rupee', 'Rs', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'LKR');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'NPR', 'Nepalese Rupee', 'Rs', 2 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'NPR');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'KWD', 'Kuwaiti Dinar', 'د.ك', 3 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'KWD');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'BHD', 'Bahraini Dinar', 'BD', 3 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'BHD');
INSERT INTO currencies (id, code, name, symbol, decimal_places) SELECT gen_random_uuid(), 'OMR', 'Omani Rial', '﷼', 3 WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'OMR');
