package hnsMgr;

import java.io.File;
import java.sql.*;

public class SessionManager {
    private static final String SAVE_DIR = "saves/";

    // 取得指定存檔槽的 SQLite 連線
    private static Connection getConnection(String saveName) throws SQLException {
        File dir = new File(SAVE_DIR);
        if (!dir.exists()) dir.mkdirs();
        String dbUrl = "jdbc:sqlite:" + SAVE_DIR + saveName + ".db";
        return DriverManager.getConnection(dbUrl);
    }

    // 儲存當前工作階段
    public static boolean saveSession(String saveName, Manager manager, VirtualFileSystem vfs) {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS session_data (" +
                "category TEXT NOT NULL, " +
                "key TEXT NOT NULL, " +
                "value TEXT, " +
                "PRIMARY KEY (category, key));";

        try (Connection conn = getConnection(saveName);
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTableSQL);

            // 使用交易提升寫入效能
            conn.setAutoCommit(false);

            String upsertSQL = "INSERT INTO session_data(category, key, value) VALUES(?, ?, ?) " +
                    "ON CONFLICT(category, key) DO UPDATE SET value = excluded.value;";

            try (PreparedStatement pstmt = conn.prepareStatement(upsertSQL)) {
                // 1. 儲存所有腳本變數
                for (String key : manager.getVariableKeys()) {
                    pstmt.setString(1, "VARIABLES");
                    pstmt.setString(2, key);
                    pstmt.setString(3, manager.getRawVariable(key));
                    pstmt.executeUpdate();
                }

                // 2. 儲存當前 VFS 狀態資訊
                pstmt.setString(1, "VFS");
                pstmt.setString(2, "current_node");
                pstmt.setString(3, vfs.getCurrentNodeId());
                pstmt.executeUpdate();

                pstmt.setString(1, "VFS");
                pstmt.setString(2, "current_path");
                pstmt.setString(3, vfs.getCurrentPath());
                pstmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.out.println("Error saving session: " + e.getMessage());
            return false;
        }
    }

    // 載入工作階段
    public static boolean loadSession(String saveName, Manager manager, VirtualFileSystem vfs) {
        File saveFile = new File(SAVE_DIR + saveName + ".db");
        if (!saveFile.exists()) {
            System.out.println("Save file not found: " + saveFile.getPath());
            return false;
        }

        String querySQL = "SELECT category, key, value FROM session_data;";

        try (Connection conn = getConnection(saveName);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(querySQL)) {

            while (rs.next()) {
                String category = rs.getString("category");
                String key = rs.getString("key");
                String value = rs.getString("value");

                if ("VARIABLES".equals(category)) {
                    manager.setVariable(key, value);
                } else if ("VFS".equals(category)) {
                    if ("current_node".equals(key)) {
                        vfs.mount(value);
                    } else if ("current_path".equals(key)) {
                        vfs.cd(value);
                    }
                }
            }
            return true;
        } catch (SQLException e) {
            System.out.println("Error loading session: " + e.getMessage());
            return false;
        }
    }
}