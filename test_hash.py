import bcrypt; print(bcrypt.checkpw(b'admin123', b'$2a$10$N.ZOn9MHSbEU0Oq6lZXaF.kXBqb0h.VDd7jqDeYRcP1XlVLYN9Cku')); print(bcrypt.hashpw(b'admin123', bcrypt.gensalt()).decode())
