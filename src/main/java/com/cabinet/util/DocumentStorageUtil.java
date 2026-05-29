package com.cabinet.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Gestion des chemins de stockage des PDF patients ({@code uploads/patients/{patientId}/}).
 */
public final class DocumentStorageUtil {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy_MM_dd");

    private DocumentStorageUtil() {
    }

    /**
     * Répertoire racine des uploads (variable d'environnement {@code CABINET_UPLOADS_DIR}
     * ou {@code uploads} sous le répertoire de travail du serveur).
     */
    public static Path getUploadsRoot() {
        String env = System.getenv("CABINET_UPLOADS_DIR");
        if (env != null && !env.isBlank()) {
            return Paths.get(env.trim()).toAbsolutePath().normalize();
        }
        String catalinaBase = System.getProperty("catalina.base");
        if (catalinaBase != null && !catalinaBase.isBlank()) {
            return Paths.get(catalinaBase, "cabinet-uploads").toAbsolutePath().normalize();
        }
        return Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();
    }

    /**
     * Répertoire patient : {@code uploads/patients/{patientId}/}.
     */
    public static Path getPatientDirectory(long patientId) throws IOException {
        Path dir = getUploadsRoot().resolve("patients").resolve(String.valueOf(patientId));
        Files.createDirectories(dir);
        return dir;
    }

    /**
     * Chemin relatif stocké en base (sous la racine uploads).
     */
    public static String toRelativePath(Path absoluteFile) {
        Path root = getUploadsRoot();
        Path normalized = absoluteFile.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return normalized.toString().replace('\\', '/');
    }

    /**
     * Résout un chemin relatif enregistré en base vers un fichier absolu.
     */
    public static Path resolveStoredPath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("Chemin fichier vide");
        }
        Path p = Paths.get(filePath);
        if (p.isAbsolute()) {
            return p.normalize();
        }
        return getUploadsRoot().resolve(filePath).normalize();
    }

    /**
     * Génère un nom de fichier sécurisé, ex. {@code ordonnance_2026_05_29.pdf}.
     */
    public static String buildFileName(String prefix, LocalDate date) {
        String safePrefix = prefix.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
        String d = FILE_DATE.format(date != null ? date : LocalDate.now());
        return safePrefix + "_" + d + "_" + System.currentTimeMillis() + ".pdf";
    }
}
