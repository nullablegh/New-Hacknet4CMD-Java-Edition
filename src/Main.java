import Exceptions.CountException;
import hnsMgr.*;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

private static final Scanner scanner = new Scanner(System.in);
private final Manager manager = new Manager();
private Clip theClip = null;
private int musicVolume = 100;
private int startOffset = 0;
private int loop = 0;

// 腳本跳轉控制標記
private boolean isRedirecting = false;
private String redirectTarget = null;

// 指令解析宣告
private final Command endCmd = new Command("end", 1);
private final Command keyCmd = new Command("key", 1);
private final Command callCmd = new Command("call", 1);
private final Command gotoCmd = new Command("goto", 1);
private final Command inputCmd = new Command("input", 1);
private final Command printCmd = new Command("print", -1);
private final Command printlnCmd = new Command("println", -1);
private final Command clearCmd = new Command("clear", 0);
private final Command sleepCmd = new Command("sleep", 1);
private final Command playCmd = new Command("play", 2);
private final Command printrCmd = new Command("printr", -1);
private final Command setCmd = new Command("set", 3);
private final Command letCmd = new Command("let", 2);
private final Command volumeCmd = new Command("volume", 1);
private final Command startoffsetCmd = new Command("startoffset", 1);
private final Command mloopCmd = new Command("mloop", 0);
private final Command randomCmd = new Command("random",2);
private final Command colorCmd = new Command("color",1);
// 宣告實例與指令
private final VirtualFileSystem vfs = new VirtualFileSystem();
private final Command saveCmd = new Command("save", 1);
private final Command loadCmd = new Command("load", 1);
private final Command vfsMountCmd = new Command("vfs_mount", 1);
private final Command lsCmd = new Command("ls", 0);
private final Command cdCmd = new Command("cd", 1);
private final Command catCmd = new Command("cat", 1);
private final Command rmCmd = new Command("rm", 1);
private final Command pwdCmd = new Command("pwd", 0);
void main() {
    setup();
    runScript("Scripts/start.pnv");
}
public void setup(){
    enableEColors();
    setupCommands();
    TerminalInput.loadAndSetCustomFont("fonts/CascadiaCode.ttf", "Cascadia Code ExtraLight", 22);
}
public static void enableEColors() {
    try {
        new ProcessBuilder("cmd", "/c", "echo \033[0m").inheritIO().start().waitFor();
        new ProcessBuilder("cmd", "/c", "chcp 65001").inheritIO().start().waitFor();
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
    } catch (Exception ignored) {}
}
private void setupCommands() {
    manager.add("print", (args) -> {
        String fullLine = formatVariables(String.join(" ", args));
        System.out.print(fullLine);
    });

    manager.add("println", (args) -> {
        String fullLine = formatVariables(String.join(" ", args));
        System.out.println(fullLine);
    });

    manager.add("printr", (args) -> {
        String fullLine = formatVariables(String.join(" ", args));
        System.out.print(fullLine.trim() + "\r");
        System.out.flush();
    });

    manager.add("clear", (_) -> {
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } catch (Exception e) {
            for (int i = 0; i < 100; i++) System.out.println();
        }
    });

    manager.add("sleep", (args) -> {
        try {
            Thread.sleep(Integer.parseInt(args[0]));
        } catch (InterruptedException e) {
            System.out.println("Oops! Something went wrong: " + e.getMessage());
        }
    });

    manager.add("play", (args) -> {
        if (args.length < 2) return;
        String path = "audios/musics/" + args[0] + ".wav";
        if (Objects.equals(args[1], "/bg")) {
            new Thread(() -> playSound(path, false)).start();
        } else if (Objects.equals(args[1], "/m")) {
            playSound(path, true);
        }
    });

    manager.add("set", (args) -> {
        if (args.length < 3) return;
        String varName = args[0];
        String operator = args[1];
        String operandStr = formatVariables(args[2]);
        if (operandStr.startsWith("\"") && operandStr.endsWith("\"") && operandStr.length() >= 2) {
            operandStr = operandStr.substring(1, operandStr.length() - 1);
        } // 支援變數解析

        // 取得當前變數的值
        String currentValStr = manager.getRawVariable(varName);
        if (currentValStr == null) currentValStr = "";

        // 1. 一般賦值 (=)
        if (operator.equals("=")) {
            manager.setVariable(varName, operandStr);
            return;
        }

        // 2. 嘗試當作純數字運算
        try {
            long currentVal = Long.parseLong(currentValStr.isEmpty() ? "0" : currentValStr);
            long operand = Long.parseLong(operandStr);

            long result = switch (operator) {
                case "+", "+=" -> currentVal + operand;
                case "-", "-=" -> currentVal - operand;
                case "*", "*=" -> currentVal * operand;
                case "/", "/=" -> (operand == 0) ? currentVal : currentVal / operand;
                default -> currentVal;
            };

            manager.setVariable(varName, String.valueOf(result));
            return;
        } catch (NumberFormatException ignored) {
            // 非數字，自動向下進入字串處理
        }

        // 3. 字串拼接模式 (+ 或 +=)
        if (operator.equals("+") || operator.equals("+=")) {
            manager.setVariable(varName, currentValStr + operandStr);
        } else {
            System.out.println("Error: Operator '" + operator + "' not supported for strings.");
        }
    });

    manager.add("let", (args) -> manager.setVariable(args[0], args[1]));

    manager.add("volume", (args) -> {
        musicVolume = Integer.parseInt(args[0]);
        setVolume(theClip, musicVolume * 0.01f);
    });

    manager.add("startoffset", (args) -> {
        startOffset = Integer.parseInt(args[0]);
        if (theClip != null) theClip.setMicrosecondPosition(startOffset);
    });

    manager.add("musicloop", (_) -> {
        loop = (loop == 0) ? -1 : 0;
        if (theClip != null) theClip.loop(loop);
    });

    manager.add("input", (args) -> {
        try {
            String promptText = (args.length > 0) ? formatVariables(args[0]) : "";
            String targetVar = (args.length > 1 && !args[1].isEmpty()) ? args[1] : "input";

            // 1. 設定提示字元並初始化底層 Native 監聽
            TerminalInput.setPrompt(promptText);
            TerminalInput.initInput();

            // 2. 非同步迴圈等待 Enter 或 ESC
            while (true) {
                if (TerminalInput.isEscPressed()) {
                    manager.setVariable(targetVar, "ESC");
                    break;
                }
                if (TerminalInput.isEnterPressed()) {
                    String userInput = TerminalInput.getLastInput();
                    manager.setVariable(targetVar, userInput);
                    break;
                }
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ignored) {}
            }

            TerminalInput.stopInput();
        } catch (Exception e) {
            System.out.println("Error reading user input: " + e.getMessage());
            TerminalInput.stopInput();
        }
    });

    manager.add("key", (args) -> {
        try {
            String targetVar = (args.length > 0 && !args[0].isEmpty()) ? args[0] : "key";

            // 1. 單鍵/選單模式初始化
            TerminalInput.initInput();

            // 2. 迴圈等待按鍵選擇
            while (true) {
                if (TerminalInput.isEscPressed()) {
                    manager.setVariable(targetVar, "ESC");
                    break;
                }
                if (TerminalInput.isEnterPressed()) {
                    String userKey = TerminalInput.getLastInput().trim();
                    manager.setVariable(targetVar, userKey);
                    break;
                }
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ignored) {}
            }

            TerminalInput.stopInput();
        } catch (Exception e) {
            System.out.println("Error reading key input: " + e.getMessage());
            TerminalInput.stopInput();
        }
    });
    manager.add("end", (args) -> System.exit(Integer.parseInt(args[0])));

    manager.add("key", (args) -> {
        try {
            String targetVar = (args.length > 0 && !args[0].isEmpty()) ? args[0] : "key";

            // 1. 初始化底層即時按鍵監聽
            TerminalInput.initInput();

            // 2. 迴圈等待任意按鍵（不需要按 Enter）
            while (true) {
                if (TerminalInput.isEscPressed()) {
                    manager.setVariable(targetVar, "ESC");
                    break;
                }

                // 只要偵測到任何按鍵按下，立刻讀取並結束
                if (TerminalInput.isKeyDetected()) {
                    int keyCode = TerminalInput.getLastSingleKey();
                    String keyStr;

                    // 根據 ASCII 轉成對應的友善字串或字元
                    if (keyCode == 13 || keyCode == 10) {
                        keyStr = "ENTER";
                    } else if (keyCode == 32) {
                        keyStr = "SPACE";
                    } else if (keyCode >= 32 && keyCode <= 126) {
                        keyStr = String.valueOf((char) keyCode);
                    } else {
                        keyStr = String.valueOf(keyCode);
                    }

                    manager.setVariable(targetVar, keyStr);
                    break;
                }

                try {
                    Thread.sleep(5); // 避免 CPU 滿載
                } catch (InterruptedException ignored) {}
            }

            TerminalInput.stopInput();
        } catch (Exception e) {
            System.out.println("Error reading key input: " + e.getMessage());
            TerminalInput.stopInput();
        }
    });
    // 呼叫外部子腳本
    manager.add("call", (args) -> runScript("Scripts/" + args[0]));

    manager.add("random", (args) -> {
        if (args.length < 2) {
            System.out.println("Error: 'random' requires at least min and max arguments. Usage: random [var] <min> <max> or random <min> <max>");
            return;
        }

        try {
            String targetVar = "rng";
            int min;
            int max;

            if (args.length >= 3) {
                // 語法: random <varName> <min> <max>
                targetVar = args[0];
                min = Integer.parseInt(manager.getVariable(args[1]));
                max = Integer.parseInt(manager.getVariable(args[2]));
            } else {
                // 語法: random <min> <max> (預設寫入 rng)
                min = Integer.parseInt(manager.getVariable(args[0]));
                max = Integer.parseInt(manager.getVariable(args[1]));
            }

            if (min > max) {
                int temp = min;
                min = max;
                max = temp;
            }

            // 產生 [min, max] 閉區間隨機整數
            int randomVal = min + new Random().nextInt(max - min + 1);
            manager.setVariable(targetVar, String.valueOf(randomVal));

        } catch (NumberFormatException e) {
            System.out.println("Error: 'random' arguments must be valid integers.");
        }
    });
    manager.add("color", (args) -> {
        if (args.length == 0) {
            System.out.print(EColors.RESET);
            return;
        }

        String colorName = args[0].toLowerCase();
        switch (colorName) {
            case "red" -> System.out.print(EColors.RED);
            case "bright_red", "lred" -> System.out.print(EColors.BRIGHT_RED);
            case "green" -> System.out.print(EColors.GREEN);
            case "bright_green", "lgreen" -> System.out.print(EColors.BRIGHT_GREEN);
            case "yellow" -> System.out.print(EColors.YELLOW);
            case "blue" -> System.out.print(EColors.BLUE);
            case "purple", "magenta" -> System.out.print(EColors.PURPLE);
            case "cyan" -> System.out.print(EColors.CYAN);
            case "bright_cyan", "lcyan" -> System.out.print(EColors.BRIGHT_CYAN);
            case "white" -> System.out.print(EColors.WHITE);
            case "gray", "grey" -> System.out.print(EColors.GRAY);
            case "bold" -> System.out.print(EColors.BOLD);
            case "reset" -> System.out.print(EColors.RESET);
            default -> System.out.println("Unknown color: " + colorName);
        }
    });
    manager.add("vfs_mount", (args) -> vfs.mount(args[0]));

    manager.add("ls", (_) -> vfs.ls());

    manager.add("pwd", (_) -> System.out.println(vfs.getCurrentPath()));

    manager.add("cd", (args) -> {
        if (args.length == 0 || !vfs.cd(args[0])) {
            System.out.println("Error: Directory not found: " + (args.length > 0 ? args[0] : ""));
        }
    });

    manager.add("cat", (args) -> {
        if (args.length == 0) {
            System.out.println("Error: Missing file argument.");
            return;
        }
        System.out.println(vfs.cat(args[0]));
    });

    manager.add("rm", (args) -> {
        if (args.length == 0) return;
        if (vfs.rm(args[0])) {
            System.out.println(EColors.GREEN + "[+] File deleted: " + args[0] + EColors.RESET);
        } else {
            System.out.println(EColors.RED + "[-] Failed to remove: File not found." + EColors.RESET);
        }
    });
    manager.add("save", (args) -> {
        String slot = args.length > 0 ? args[0] : "quicksave";
        if (SessionManager.saveSession(slot, manager, vfs)) {
            System.out.println(EColors.GREEN + "[+] Session saved to 'saves/" + slot + ".sav'" + EColors.RESET);
        } else {
            System.out.println(EColors.RED + "[-] Failed to save session." + EColors.RESET);
        }
    });

    manager.add("load", (args) -> {
        String slot = args.length > 0 ? args[0] : "quicksave";
        if (SessionManager.loadSession(slot, manager, vfs)) {
            System.out.println(EColors.GREEN + "[+] Session loaded from 'saves/" + slot + ".sav'" + EColors.RESET);
        } else {
            System.out.println(EColors.RED + "[-] Failed to load session." + EColors.RESET);
        }
    });
}

public void runScript(String filePath) {
    File file = new File(filePath);
    if (!file.exists()) {
        System.out.println("Error: Script not found: " + filePath);
        return;
    }

    List<String> lines;
    try {
        lines = Files.readAllLines(Path.of(filePath));
    } catch (IOException e) {
        System.out.println("Error reading script file: " + e.getMessage());
        return;
    }

    // 1. 預先掃描標籤 (Label)
    Map<String, Integer> labels = new HashMap<>();
    for (int i = 0; i < lines.size(); i++) {
        String trimmed = lines.get(i).trim();
        if (trimmed.startsWith(":") || trimmed.startsWith("@")) {
            // 支援 :LABEL 或 @LABEL 語法
            labels.put(trimmed.substring(1).trim(), i);
        }
    }

    // 2. 指令執行迴圈 (Program Counter 控制)
    for (int pc = 0; pc < lines.size(); pc++) {
        if (isRedirecting) break;

        String line = lines.get(pc).trim();
        if (line.isEmpty() || line.startsWith("#") || line.startsWith(":") || line.startsWith("@")) continue;

        // 1. 處理變數替換
        line = formatVariables(line);

        // 2. 攔截 IF 跳轉指令
        if (line.startsWith("if ")) {
            String[] parts = line.split("\\s+");
            if (parts.length >= 6 && parts[4].equalsIgnoreCase("goto")) {
                String left = parts[1];
                String op = parts[2];
                String right = parts[3];
                String targetLabel = parts[5];

                if (evaluateCondition(left, op, right)) {
                    if (labels.containsKey(targetLabel)) {
                        pc = labels.get(targetLabel); // 跳轉內部標籤
                    } else {
                        System.out.println("Error: Label not found [" + targetLabel + "]");
                    }
                }
            }
            continue; // 確保不會繼續往下跑 manager.run
        }

        // 3. 攔截單獨 GOTO 指令（支援標籤與外部檔案）
        if (line.startsWith("goto ")) {
            System.out.print(EColors.RESET);
            String target = line.split("\\s+")[1].trim();
            if (labels.containsKey(target)) {
                pc = labels.get(target); // 內部標籤跳轉
            } else {
                // 外部檔案跳轉
                isRedirecting = true;
                redirectTarget = target.endsWith(".pnv") ? "Scripts/" + target : "Scripts/" + target + ".pnv";
            }
            continue; // 關鍵：一定要加 continue，才不會當成一般指令丟進 manager
        }
        

        // 指令分發執行
        try {
            String commandName = line.split(" ")[0];
            String[] args = switch (commandName) {
                case "print" -> printCmd.allArgs(line);
                case "println" -> printlnCmd.allArgs(line);
                case "clear" -> clearCmd.allArgs(line);
                case "sleep" -> sleepCmd.allArgs(line);
                case "play" -> playCmd.allArgs(line);
                case "printr" -> printrCmd.allArgs(line);
                case "let" -> letCmd.allArgs(line);
                case "set" -> setCmd.allArgs(line);
                case "volume" -> volumeCmd.allArgs(line);
                case "startoffset" -> startoffsetCmd.allArgs(line);
                case "musicloop" -> mloopCmd.allArgs(line);
                case "input" -> inputCmd.allArgs(line);
                case "key" -> keyCmd.allArgs(line);
                case "goto" -> gotoCmd.allArgs(line);
                case "call" -> callCmd.allArgs(line);
                case "end" -> endCmd.allArgs(line);
                case "random" -> randomCmd.allArgs(line);
                case "color" -> colorCmd.allArgs(line);
                case "vfs_mount" -> vfsMountCmd.allArgs(line);
                case "ls" -> lsCmd.allArgs(line);
                case "cd" -> cdCmd.allArgs(line);
                case "cat" -> catCmd.allArgs(line);
                case "rm" -> rmCmd.allArgs(line);
                case "pwd" -> pwdCmd.allArgs(line);
                case "save" -> saveCmd.allArgs(line);
                case "load" -> loadCmd.allArgs(line);
                default -> new String[0];
            };
            manager.run(commandName, args);
        } catch (CountException e) {
            System.out.println("Oops! Something went wrong: " + e.getMessage());
        }
    }
    if (isRedirecting) {
        isRedirecting = false;
        String next = redirectTarget;
        redirectTarget = null;
        runScript(next);
    }
}

private boolean evaluateCondition(String left, String op, String right) {
    try {
        long l = Long.parseLong(left);
        long r = Long.parseLong(right);
        return switch (op) {
            case "==" -> l == r;
            case "!=" -> l != r;
            case ">" -> l > r;
            case "<" -> l < r;
            case ">=" -> l >= r;
            case "<=" -> l <= r;
            default -> false;
        };
    } catch (NumberFormatException e) {
        return switch (op) {
            case "==" -> left.equals(right);
            case "!=" -> !left.equals(right);
            default -> false;
        };
    }
}

private String formatVariables(String text) {
    for (String key : manager.getVariableKeys()) {
        text = text.replace("%" + key + "%", manager.getRawVariable(key));
    }
    return text;
}

public void playSound(String filePath, boolean waitToFinish) {
    File soundFile = new File(filePath);
    if (!soundFile.exists()) {
        System.out.println("Audio file not found: " + filePath);
        return;
    }

    try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile)) {
        Clip clip = AudioSystem.getClip();
        theClip = clip;
        clip.open(audioStream);
        clip.setFramePosition(startOffset);
        clip.start();

        if (waitToFinish) {
            long remainingMicroseconds = clip.getMicrosecondLength() - (startOffset * 1_000_000L / (long) clip.getFormat().getFrameRate());
            if (remainingMicroseconds > 0) Thread.sleep(remainingMicroseconds / 1000);
        }
    } catch (Exception e) {
        System.out.println("Failed to play audio: " + e.getMessage());
    }
}

public void setVolume(Clip clip, float volume) {
    if (clip == null) return;
    try {
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float percentage = Math.max(0.0001f, Math.min(volume, 1.0f));
            float dB = (float) (Math.log10(percentage) * 20.0);
            gainControl.setValue(Math.max(gainControl.getMinimum(), Math.min(dB, gainControl.getMaximum())));
        }
    } catch (Exception e) {
        System.out.println("Oops! Something went wrong: " + e.getMessage());
    }
}