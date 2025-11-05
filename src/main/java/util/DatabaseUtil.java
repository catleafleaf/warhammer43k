package util;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库工具类
 */
public class DatabaseUtil {

    /**
     * 执行更新操作（INSERT, UPDATE, DELETE）
     */
    public static int executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection conn = config.DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            setParameters(pstmt, params);
            return pstmt.executeUpdate();
        }
    }

    /**
     * 执行查询操作
     */
    public static ResultSet executeQuery(String sql, Object... params) throws SQLException {
        Connection conn = config.DatabaseConfig.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        setParameters(pstmt, params);
        return pstmt.executeQuery();
    }

    /**
     * 设置 PreparedStatement 参数
     */
    private static void setParameters(PreparedStatement pstmt, Object... params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
        }
    }

    /**
     * 关闭连接
     */
    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            System.err.println("关闭数据库连接时出错: " + e.getMessage());
        }
    }

    /**
     * 批量插入
     */
    public static int[] executeBatch(String sql, List<Object[]> paramList) throws SQLException {
        try (Connection conn = config.DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Object[] params : paramList) {
                setParameters(pstmt, params);
                pstmt.addBatch();
            }

            return pstmt.executeBatch();
        }
    }
}