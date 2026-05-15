SELECT id, role_name, role_id FROM role ORDER BY id;

SELECT phone_number, otp_code, expires_at, attempts, verified FROM otp_verification ORDER BY created_on DESC LIMIT 1;

SELECT id, phone_number, username, referral_code, status FROM app_user;

SELECT id, first_name, flat_number, block, user_id FROM user_profile;

SELECT phone_number, otp_code FROM otp_verification 
WHERE phone_number = '9000000001' 
ORDER BY created_on DESC LIMIT 1;
