package config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static HikariDataSource dataSource;

    static {
        createDatabaseIfNotExists();  // 先创建数据库
        initializeDataSource();       // 再初始化连接池
        initializeDatabase();         // 最后初始化表结构
    }

    /**
     * 创建数据库（如果不存在）
     */
    private static void createDatabaseIfNotExists() {
        // 先连接到默认数据库（如mysql）来创建我们的数据库
        String tempUrl = "jdbc:mysql://localhost:3306/mysql?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=utf8";

        try (Connection conn = DriverManager.getConnection(tempUrl, "root", "catleafleaf070105!");
             Statement stmt = conn.createStatement()) {

            // 创建数据库
            String createDbSql = "CREATE DATABASE IF NOT EXISTS wh40k_rpg CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
            stmt.execute(createDbSql);
            logger.info("数据库 wh40k_rpg 创建成功或已存在");

        } catch (SQLException e) {
            logger.error("创建数据库失败", e);
            throw new RuntimeException("无法创建数据库 wh40k_rpg", e);
        }
    }

    private static void initializeDataSource() {
        try {
            HikariConfig config = new HikariConfig();

            // MySQL 配置 - 现在数据库应该存在了
            config.setJdbcUrl("jdbc:mysql://localhost:3306/wh40k_rpg?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=utf8");
            config.setUsername("root");
            config.setPassword("catleafleaf070105!");
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // 连接池配置
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);

            // 性能优化配置
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");

            dataSource = new HikariDataSource(config);
            logger.info("数据库连接池初始化成功");

        } catch (Exception e) {
            logger.error("数据库连接池初始化失败", e);
            throw new RuntimeException("数据库连接池初始化失败", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            logger.error("数据库连接测试失败", e);
            return false;
        }
    }

    public static void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("数据库连接池已关闭");
        }
    }

    private static void initializeDatabase() {
        String[] createTables = {
                // 用户表
                """
            CREATE TABLE IF NOT EXISTS users (
                id BIGINT PRIMARY KEY,
                username VARCHAR(50) UNIQUE NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                salt VARCHAR(255) NOT NULL,
                permission_level INT NOT NULL DEFAULT 0,
                character_id BIGINT DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_username (username),
                INDEX idx_character_id (character_id)
            )
            """,

                // 角色表
                """
            CREATE TABLE IF NOT EXISTS characters (
                id BIGINT PRIMARY KEY,
                name VARCHAR(50) NOT NULL,
                title VARCHAR(100),
                faction VARCHAR(50) NOT NULL,
                created_at BIGINT NOT NULL,
                updated_at BIGINT NOT NULL,
                INDEX idx_name (name),
                INDEX idx_faction (faction)
            )
            """,

                // 单位表
                """
            CREATE TABLE IF NOT EXISTS units (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                character_id BIGINT NOT NULL,
                name VARCHAR(100) NOT NULL,
                type VARCHAR(50) NOT NULL,
                count INT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE,
                INDEX idx_character_id (character_id)
            )
            """,

                // 资源表
                """
            CREATE TABLE IF NOT EXISTS resources (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                character_id BIGINT NOT NULL,
                name VARCHAR(100) NOT NULL,
                quantity INT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE,
                INDEX idx_character_id (character_id)
            )
            """,

                // 战役表
                """
            CREATE TABLE IF NOT EXISTS campaigns (
                id BIGINT PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                description TEXT,
                creator_id BIGINT NOT NULL,
                status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
                current_batch_index INT DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_creator_id (creator_id),
                INDEX idx_status (status)
            )
            """,

                // 战役参与者表
                """
            CREATE TABLE IF NOT EXISTS campaign_participants (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                campaign_id BIGINT NOT NULL,
                character_id BIGINT NOT NULL,
                joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE,
                FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE,
                UNIQUE KEY unique_participant (campaign_id, character_id),
                INDEX idx_campaign_id (campaign_id),
                INDEX idx_character_id (character_id)
            )
            """,

                // 敌人批次表
                """
            CREATE TABLE IF NOT EXISTS enemy_batches (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                campaign_id BIGINT NOT NULL,
                name VARCHAR(100) NOT NULL,
                description TEXT,
                spawn_round INT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE,
                INDEX idx_campaign_id (campaign_id)
            )
            """,

                // 敌人单位表
                """
            CREATE TABLE IF NOT EXISTS enemy_units (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                batch_id BIGINT NOT NULL,
                name VARCHAR(100) NOT NULL,
                type VARCHAR(50) NOT NULL,
                count INT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (batch_id) REFERENCES enemy_batches(id) ON DELETE CASCADE,
                INDEX idx_batch_id (batch_id)
            )
            """
        };

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            for (String sql : createTables) {
                try {
                    stmt.execute(sql);
                    logger.debug("执行SQL: {}", sql.split("\n")[1].trim()); // 记录表名
                } catch (SQLException e) {
                    logger.error("执行SQL失败: {}", sql, e);
                    throw e;
                }
            }
            logger.info("数据库表结构初始化成功");

        } catch (SQLException e) {
            logger.error("数据库表结构初始化失败", e);
            throw new RuntimeException("数据库初始化失败", e);
        }
    }
}