package hnsMgr;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class SessionManager {
    private static final String SAVE_DIR = "saves/";

    // 儲存當前工作階段
    public static boolean saveSession(String saveName, Manager manager, VirtualFileSystem vfs) {
        try {
            File dir = new File(SAVE_DIR);
            if (!dir.exists()) dir.mkdirs();

            File saveFile = new File(SAVE_DIR + saveName + ".sav");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(saveFile))) {
                // 1. 儲存所有腳本變數
                writer.write("[VARIABLES]\n");
                for (String key : manager.getVariableKeys()) {
                    writer.write(key + "=" + manager.getRawVariable(key) + "\n");
                }

                // 2. 儲存當前 VFS 狀態資訊
                writer.write("[VFS]\n");
                writer.write("current_node=" + vfs.getCurrentNodeId() + "\n");
                writer.write("current_path=" + vfs.getCurrentPath() + "\n");
            }
            return true;
        } catch (IOException e) {
            System.out.println("Error saving session: " + e.getMessage());
            return false;
        }
    }

    // 載入工作階段
    public static boolean loadSession(String saveName, Manager manager, VirtualFileSystem vfs) {
        File saveFile = new File(SAVE_DIR + saveName + ".sav");
        if (!saveFile.exists()) {
            System.out.println("Save file not found: " + saveFile.getPath());
            return false;
        }

        try {
            List<String> lines = Files.readAllLines(saveFile.toPath());
            String section = "";

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.startsWith("[") && line.endsWith("]")) {
                    section = line.toUpperCase();
                    continue;
                }

                if (section.equals("[VARIABLES]")) {
                    String[] kv = line.split("=", 2);
                    if (kv.length == 2) {
                        manager.setVariable(kv[0], kv[1]);
                    }
                } else if (section.equals("[VFS]")) {
                    String[] kv = line.split("=", 2);
                    if (kv.length == 2) {
                        if (kv[0].equals("current_node")) {
                            vfs.mount(kv[1]);
                        } else if (kv[0].equals("current_path")) {
                            vfs.cd(kv[1]);
                        }
                    }
                }
            }
            return true;
        } catch (IOException e) {
            System.out.println("Error loading session: " + e.getMessage());
            return false;
        }
    }
}