package hnsMgr;
public enum EColors {
    RESET("\u001B[0m"),
    BOLD("\u001B[1m"),

    // 前景色 (文字顏色)
    RED("\u001B[31m"),
    GREEN("\u001B[32m"),
    YELLOW("\u001B[33m"),

    BLUE("\u001B[34m"),
    PURPLE("\u001B[35m"),
    CYAN("\u001B[36m"),
    WHITE("\u001B[37m"),
    GRAY("\u001B[90m"),

    // 高亮/亮色系
    BRIGHT_RED("\u001B[91m"),
    BRIGHT_GREEN("\u001B[92m"),
    BRIGHT_YELLOW("\u001B[93m"),
    BRIGHT_CYAN("\u001B[96m");
    String code;
    EColors(String s) {
        code = s;
    }
    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return code;
    }
}