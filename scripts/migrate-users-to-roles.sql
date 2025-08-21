-- Migration script to update existing users to use the new role system
-- This script should be run after updating the UserCredential entity

-- First, let's see what roles we have in the system
SELECT * FROM role;

-- Update existing users to use proper role IDs
-- Assuming you have users with role = 'USER' or 'ADMIN' strings

-- Update users with role = 'USER' to use Customer role (ID: 7)
UPDATE user_credential 
SET role_id = 7 
WHERE role = 'USER' OR role IS NULL;

-- Update users with role = 'ADMIN' to use Administrator role (ID: 1)
UPDATE user_credential 
SET role_id = 1 
WHERE role = 'ADMIN';

-- Verify the updates
SELECT 
    uc.id,
    uc.name,
    uc.email,
    uc.role_id,
    r.name as role_name,
    r.description as role_description
FROM user_credential uc
LEFT JOIN role r ON uc.role_id = r.id;

-- Drop the old role column (after confirming everything works)
-- ALTER TABLE user_credential DROP COLUMN role;
