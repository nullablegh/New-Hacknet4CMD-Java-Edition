package hnsMgr;

public class TerminalInput {
    static {
        // 載入編譯好的 DLL (若放在專案根目錄)
        System.loadLibrary("TerminalInputNative");
        // 或者使用絕對路徑載入：
        // System.load("C:\\Users\\123\\source\\repos\\Dll1\\x64\\Debug\\Dll1.dll");
    }

    // 原有的底層非同步輸入與緩衝控制
    public static native void initInput();
    public static native void stopInput();
    public static native void setPrompt(String prompt);
    public static native String getLastInput();
    public static native boolean isEnterPressed();
    public static native boolean isEscPressed();

    // 新增：支援「按任意鍵瞬間繼續」的即時單鍵偵測
    public static native int getLastSingleKey();
    public static native boolean isKeyDetected();
    public static native boolean setFont(String fontName, int fontSize);

    // 載入自訂 TTF 檔案並套用 (ttfPath: TTF 檔案路徑, fontName: 該 TTF 的內部名稱, fontSize: 字級)
    public static native boolean loadAndSetCustomFont(String ttfPath, String fontName, int fontSize);
}