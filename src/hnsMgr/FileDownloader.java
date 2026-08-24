package hnsMgr;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

public class FileDownloader {
    public static void download(String link, String path) {

        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(link))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            Path targetPath = new File(path).toPath();
            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(targetPath));
            if (response.statusCode() != 200) {
                throw new IOException("Failed to download. HTTP Status code: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            // 讓上層知道下載失敗，而不是默默用 System.exit 強制關閉
            throw new RuntimeException("Download failed", e);
        }
    }
}