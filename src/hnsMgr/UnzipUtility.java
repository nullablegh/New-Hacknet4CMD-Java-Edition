package hnsMgr;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipUtility {
    public static void unzip(String zipFilePath, String destDirPath) throws IOException {
        Path targetDir = Paths.get(destDirPath).toAbsolutePath().normalize();

        try (InputStream fis = Files.newInputStream(Paths.get(zipFilePath));
             ZipInputStream zis = new ZipInputStream(fis)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // 1. 將 Windows 的反斜線換成標準的斜線，避免路徑解析錯誤
                String normalizedEntryName = entry.getName().replace("\\", "/");

                // 2. 防止 Zip-Slip 漏洞
                Path targetPath = targetDir.resolve(normalizedEntryName).normalize();

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    // 建立父目錄
                    Path parent = targetPath.getParent();
                    if (parent != null && Files.notExists(parent)) {
                        Files.createDirectories(parent);
                    }
                    // 寫入檔案
                    Files.copy(zis, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }
}