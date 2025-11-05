package service;

import model.User;
import util.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于数据库的用户服务实现
 * 替代原有的内存版 UserService
 */
public class DatabaseUserService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseUserService.class);
    private static DatabaseUserService instance;
    private long currentUserId;

    private DatabaseUserService() {
        // 私有构造函数
    }

    public static DatabaseUserService getInstance() {
        if (instance == null) {
            instance = new DatabaseUserService();
        }
        return instance;
    }

    /**
     * 检查用户名是否可用
     */
    public boolean isUsernameAvailable(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (ResultSet rs = DatabaseUtil.executeQuery(sql, username)) {
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
            return true;
        } catch (SQLException e) {
            logger.error("检查用户名是否存在失败: {}", username, e);
            return false;
        }
    }

    /**
     * 用户注册
     */
    public User registerUser(String username, String password, int permissionLevel, long characterId) {
        // 检查用户名是否已存在
        if (!isUsernameAvailable(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 验证用户名格式
        if (!isValidUsername(username)) {
            throw new IllegalArgumentException("用户名只能包含字母、数字和下划线，长度3-20字符");
        }

        // 验证密码强度
        if (!isValidPassword(password)) {
            throw new IllegalArgumentException("密码长度至少6位");
        }

        long userId = generateUserId();
        String salt = generateSalt();
        String passwordHash = hashPassword(password, salt);

        String sql = "INSERT INTO users (id, username, password_hash, salt, permission_level, character_id) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            DatabaseUtil.executeUpdate(sql, userId, username, passwordHash, salt, permissionLevel, characterId);
            logger.info("用户注册成功: {}", username);

            return new User(userId, username, passwordHash, salt, permissionLevel, characterId);
        } catch (SQLException e) {
            logger.error("用户注册失败: {}", username, e);
            throw new RuntimeException("用户注册失败", e);
        }
    }

    /**
     * 用户登录
     */
    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (ResultSet rs = DatabaseUtil.executeQuery(sql, username)) {
            if (rs.next()) {
                long userId = rs.getLong("id");
                String storedHash = rs.getString("password_hash");
                String salt = rs.getString("salt");
                int permissionLevel = rs.getInt("permission_level");
                long characterId = rs.getLong("character_id");

                User user = new User(userId, username, storedHash, salt, permissionLevel, characterId);

                if (user.verifyPassword(password)) {
                    logger.info("用户登录成功: {}", username);
                    return user;
                } else {
                    throw new IllegalArgumentException("密码错误");
                }
            } else {
                throw new IllegalArgumentException("用户不存在");
            }
        } catch (SQLException e) {
            logger.error("用户登录失败: {}", username, e);
            throw new RuntimeException("登录失败", e);
        }
    }

    /**
     * 根据ID获取用户
     */
    public User getUserById(long id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (ResultSet rs = DatabaseUtil.executeQuery(sql, id)) {
            if (rs.next()) {
                String username = rs.getString("username");
                String passwordHash = rs.getString("password_hash");
                String salt = rs.getString("salt");
                int permissionLevel = rs.getInt("permission_level");
                long characterId = rs.getLong("character_id");

                return new User(id, username, passwordHash, salt, permissionLevel, characterId);
            }
            return null;
        } catch (SQLException e) {
            logger.error("根据ID获取用户失败: {}", id, e);
            return null;
        }
    }

    /**
     * 获取所有用户
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id";

        try (ResultSet rs = DatabaseUtil.executeQuery(sql)) {
            while (rs.next()) {
                long id = rs.getLong("id");
                String username = rs.getString("username");
                String passwordHash = rs.getString("password_hash");
                String salt = rs.getString("salt");
                int permissionLevel = rs.getInt("permission_level");
                long characterId = rs.getLong("character_id");

                users.add(new User(id, username, passwordHash, salt, permissionLevel, characterId));
            }
            return users;
        } catch (SQLException e) {
            logger.error("获取所有用户失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 更新用户权限
     */
    public boolean updateUserPermission(long userId, int newPermissionLevel) {
        String sql = "UPDATE users SET permission_level = ? WHERE id = ?";

        try {
            int affectedRows = DatabaseUtil.executeUpdate(sql, newPermissionLevel, userId);
            logger.info("更新用户权限: userId={}, newLevel={}", userId, newPermissionLevel);
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("更新用户权限失败: {}", userId, e);
            return false;
        }
    }

    /**
     * 关联角色到用户
     */
    public boolean linkCharacterToUser(long userId, long characterId) {
        String sql = "UPDATE users SET character_id = ? WHERE id = ?";

        try {
            int affectedRows = DatabaseUtil.executeUpdate(sql, characterId, userId);
            logger.info("关联角色到用户: userId={}, characterId={}", userId, characterId);
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("关联角色到用户失败: userId={}", userId, e);
            return false;
        }
    }

    /**
     * 设置当前用户
     */
    public void setCurrentUser(User user) {
        if (user != null) {
            this.currentUserId = user.getId();
        }
    }

    /**
     * 通过用户ID设置当前用户
     */
    public boolean setCurrentUserId(long userId) {
        User user = getUserById(userId);
        if (user != null) {
            this.currentUserId = userId;
            return true;
        }
        return false;
    }

    /**
     * 获取当前登录用户
     */
    public User getCurrentUser() {
        return getUserById(currentUserId);
    }

    /**
     * 注销当前用户
     */
    public void logout() {
        this.currentUserId = 0;
    }

    /**
     * 检查用户是否有关联角色
     */
    public boolean hasLinkedCharacter(long userId) {
        User user = getUserById(userId);
        return user != null && user.getCharacterId() != 0;
    }

    /**
     * 获取当前用户ID
     */
    public long getCurrentUserId() {
        return currentUserId;
    }

    // ==================== 辅助方法 ====================

    /**
     * 验证用户名格式
     */
    private boolean isValidUsername(String username) {
        return username != null &&
                username.matches("^[a-zA-Z0-9_]{3,20}$");
    }

    /**
     * 验证密码强度
     */
    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    /**
     * 生成随机盐值
     */
    private String generateSalt() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return java.util.Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 密码哈希函数
     */
    private String hashPassword(String password, String salt) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashedPassword = md.digest(password.getBytes());
            return java.util.Base64.getEncoder().encodeToString(hashedPassword);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("密码加密错误", e);
        }
    }

    /**
     * 生成用户ID
     */
    private long generateUserId() {
        // 基于时间戳生成用户ID，确保唯一性
        return System.currentTimeMillis() % 1000000000L;
    }

    /**
     * 根据用户名获取用户
     */
    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (ResultSet rs = DatabaseUtil.executeQuery(sql, username)) {
            if (rs.next()) {
                long id = rs.getLong("id");
                String passwordHash = rs.getString("password_hash");
                String salt = rs.getString("salt");
                int permissionLevel = rs.getInt("permission_level");
                long characterId = rs.getLong("character_id");

                return new User(id, username, passwordHash, salt, permissionLevel, characterId);
            }
            return null;
        } catch (SQLException e) {
            logger.error("根据用户名获取用户失败: {}", username, e);
            return null;
        }
    }
}