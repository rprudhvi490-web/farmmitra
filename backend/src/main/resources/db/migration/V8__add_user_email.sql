ALTER TABLE user_profile ADD COLUMN IF NOT EXISTS email VARCHAR(255);

INSERT INTO role (role_name, role_id) VALUES ('Procurement', 'ROLE_PROCUREMENT')
ON CONFLICT (role_id) DO NOTHING;
