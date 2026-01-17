package com.rep.updater;

public class UpdateConfig {
    // Configuración de TU repositorio GitHub
    public static final String GITHUB_USER = "Toffy22Cj";
    public static final String GITHUB_REPO = "REP";

    // URLs de la API de GitHub
    public static final String LATEST_RELEASE_URL = "https://api.github.com/repos/" + GITHUB_USER + "/" + GITHUB_REPO
            + "/releases/latest";

    // Configuración local
    public static final String VERSION_FILE = "app.version";
    public static final String UPDATE_FLAG_FILE = "update.available";
    public static final String BACKUP_DIR_PREFIX = "backup_";

    // Comportamiento
    public static final boolean AUTO_CHECK = true;
    public static final long CHECK_INTERVAL_HOURS = 24; // Verificar cada 24 horas
    public static final boolean CREATE_BACKUP = true;
}
