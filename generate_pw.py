import bcrypt
h = bcrypt.hashpw(b'admin123', bcrypt.gensalt()).decode()
sql = f"UPDATE user_info SET password = '{h}' WHERE login_name = 'admin';\n"
sql += f"SELECT login_name, password, LENGTH(password) as len FROM user_info WHERE login_name = 'admin';\n"
with open('fix_pw_final.sql', 'w', encoding='utf-8') as f:
    f.write(sql)
print(f"Generated hash: {h}")
print(f"Length: {len(h)}")
