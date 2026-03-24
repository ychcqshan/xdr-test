UPDATE user_info SET password = '$2b$12$REhFW8Z2.CrkVi1SovD8hugNoJQ6b633zVDpjrvjiYTLD0UbeaeGS' WHERE login_name = 'admin';
SELECT login_name, password, LENGTH(password) as len FROM user_info WHERE login_name = 'admin';
