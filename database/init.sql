-- 创建数据库
CREATE DATABASE IF NOT EXISTS wh40k_rpg_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS wh40k_rpg_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS wh40k_rpg CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建专用用户
CREATE USER IF NOT EXISTS 'wh40k_user'@'localhost' IDENTIFIED BY 'wh40k_password';
GRANT ALL PRIVILEGES ON wh40k_rpg_dev.* TO 'wh40k_user'@'localhost';
GRANT ALL PRIVILEGES ON wh40k_rpg_test.* TO 'wh40k_user'@'localhost';
GRANT ALL PRIVILEGES ON wh40k_rpg.* TO 'wh40k_user'@'localhost';
FLUSH PRIVILEGES;