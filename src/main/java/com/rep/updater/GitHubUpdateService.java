package com.rep.updater;

import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class GitHubUpdateService {

    public static ReleaseInfo getLatestRelease() throws IOException {
        URL url = new URL(UpdateConfig.LATEST_RELEASE_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        conn.setRequestProperty("User-Agent", "SistemaEducativo-Updater");

        if (conn.getResponseCode() == 200) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return parseReleaseInfo(response.toString());
        }
        throw new IOException("Error HTTP " + conn.getResponseCode());
    }

    private static ReleaseInfo parseReleaseInfo(String json) {
        JSONObject obj = new JSONObject(json);
        ReleaseInfo info = new ReleaseInfo();

        info.setVersion(obj.getString("tag_name").replace("v", ""));
        info.setName(obj.getString("name"));
        info.setReleaseNotes(obj.getString("body"));
        info.setPublishedAt(obj.getString("published_at"));

        // Extraer zipball_url o similar si es necesario,
        // pero para JARs solemos buscar en assets
        if (obj.has("zipball_url")) {
            info.setDownloadUrl(obj.getString("zipball_url"));
        }

        return info;
    }

    public static class ReleaseInfo {
        private String version;
        private String name;
        private String releaseNotes;
        private String publishedAt;
        private String downloadUrl;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getReleaseNotes() {
            return releaseNotes;
        }

        public void setReleaseNotes(String releaseNotes) {
            this.releaseNotes = releaseNotes;
        }

        public String getPublishedAt() {
            return publishedAt;
        }

        public void setPublishedAt(String publishedAt) {
            this.publishedAt = publishedAt;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public void setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
        }
    }
}
