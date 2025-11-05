package model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class User {
    private long id;
    private String username;
    private String passwordHash; // 存储密码的哈希值
    private String salt; // 密码盐值
    private int permissionLevel; // 1:管理者, 0:玩家
    private long characterId; // 关联的角色ID

    public User(long id, String username, String password, int permissionLevel, long characterId) {
        this.id = id;
        this.username = username;
        this.permissionLevel = permissionLevel;
        this.characterId = characterId;
        // 生成盐值并加密密码
        this.salt = generateSalt();
        this.passwordHash = hashPassword(password, this.salt);
    }

    // 验证密码
    public boolean verifyPassword(String password) {
        String hashToCheck = hashPassword(password, this.salt);
        return this.passwordHash.equals(hashToCheck);
    }

    // 更新密码
    public void updatePassword(String newPassword) {
        this.salt = generateSalt();
        this.passwordHash = hashPassword(newPassword, this.salt);
    }

    // 生成随机盐值
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // 密码哈希函数
    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("密码加密错误", e);
        }
    }

    public boolean isManager() {
        return permissionLevel == 1;
    }

    // Getters and Setters
    public long getId() { return id; }
    public String getUsername() { return username; }
    public int getPermissionLevel() { return permissionLevel; }
    public long getCharacterId() { return characterId; }
    public String getPasswordHash() { return passwordHash; }
    public String getSalt() { return salt; }

    // 用于从数据库重建对象
    public User(long id, String username, String passwordHash, String salt,
                int permissionLevel, long characterId) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.permissionLevel = permissionLevel;
        this.characterId = characterId;
    }
}