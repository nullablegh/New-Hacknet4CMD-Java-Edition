package hnsMgr;

import java.util.*;

public class VirtualFileSystem {
    public static class FSNode {
        public String name;
        public boolean isDirectory;
        public String content; // 僅檔案使用
        public Map<String, FSNode> children = new HashMap<>();
        public FSNode parent;

        public FSNode(String name, boolean isDirectory, FSNode parent) {
            this.name = name;
            this.isDirectory = isDirectory;
            this.parent = parent;
            this.content = "";
        }
    }

    private final Map<String, FSNode> nodes = new HashMap<>(); // 儲存不同 IP/主機的 root
    private FSNode currentRoot;
    private FSNode currentDir;
    private String currentNodeId = "localhost";

    public VirtualFileSystem() {
        initDefaultNodes();
    }

    public void initDefaultNodes() {
        // 初始化預設節點：192.168.1.104
        FSNode root104 = new FSNode("/", true, null);

        FSNode sys = new FSNode("sys", true, root104);
        FSNode log = new FSNode("log", true, root104);
        FSNode home = new FSNode("home", true, root104);

        FSNode netLog = new FSNode("auth.log", false, log);
        netLog.content = "[2026-08-22 18:20] SSH session opened by root\n[2026-08-22 18:22] Warning: Failed login from 10.0.0.45";
        log.children.put("auth.log", netLog);

        FSNode note = new FSNode("passwords.txt", false, home);
        note.content = "CORE_GATEWAY_KEY: PROMETHEUS_9981\nBACKUP_PIN: 7712";
        home.children.put("passwords.txt", note);

        root104.children.put("sys", sys);
        root104.children.put("log", log);
        root104.children.put("home", home);

        nodes.put("192.168.1.104", root104);
        mount("192.168.1.104");
    }

    public void mount(String nodeId) {
        if (!nodes.containsKey(nodeId)) {
            FSNode newRoot = new FSNode("/", true, null);
            nodes.put(nodeId, newRoot);
        }
        this.currentNodeId = nodeId;
        this.currentRoot = nodes.get(nodeId);
        this.currentDir = currentRoot;
    }

    public String getCurrentPath() {
        if (currentDir == currentRoot) return "/";
        List<String> pathParts = new ArrayList<>();
        FSNode curr = currentDir;
        while (curr != null && curr != currentRoot) {
            pathParts.add(curr.name);
            curr = curr.parent;
        }
        Collections.reverse(pathParts);
        return "/" + String.join("/", pathParts);
    }

    public void ls() {
        if (currentDir.children.isEmpty()) {
            System.out.println(EColors.GRAY + " (empty directory)" + EColors.RESET);
            return;
        }
        for (FSNode node : currentDir.children.values()) {
            if (node.isDirectory) {
                System.out.println(EColors.CYAN + "[DIR]  " + node.name + EColors.RESET);
            } else {
                System.out.println(EColors.WHITE + "[FILE] " + node.name + EColors.RESET);
            }
        }
    }

    public boolean cd(String target) {
        if (target.equals("/") || target.equals("~")) {
            currentDir = currentRoot;
            return true;
        }
        if (target.equals("..")) {
            if (currentDir.parent != null) {
                currentDir = currentDir.parent;
            }
            return true;
        }
        if (currentDir.children.containsKey(target) && currentDir.children.get(target).isDirectory) {
            currentDir = currentDir.children.get(target);
            return true;
        }
        return false;
    }

    public String cat(String fileName) {
        if (currentDir.children.containsKey(fileName)) {
            FSNode node = currentDir.children.get(fileName);
            if (!node.isDirectory) {
                return node.content;
            }
            return "Error: Cannot cat a directory.";
        }
        return "Error: File not found: " + fileName;
    }

    public boolean rm(String name) {
        return currentDir.children.remove(name) != null;
    }

    public void touch(String name, String content) {
        FSNode file = new FSNode(name, false, currentDir);
        file.content = content;
        currentDir.children.put(name, file);
    }
    public String getCurrentNodeId() {
        return this.currentNodeId;
    }
}