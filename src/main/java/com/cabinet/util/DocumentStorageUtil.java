package com.cabinet.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Gestion sécurisée des chemins de stockage des documents.
 */
public final class DocumentStorageUtil {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy_MM_dd");

    private DocumentStorageUtil() {}

    public static Path getUploadsRoot() {
        String catalinaBase = System.getProperty("catalina.base");
        Path root;
        if (catalinaBase != null && !catalinaBase.isBlank()) {
            root = Paths.get(catalinaBase, "cabinet-uploads");
        } else {
            root = Paths.get(System.getProperty("user.dir"), "uploads");
        }
        return root.toAbsolutePath().normalize();
    }

    public static Path getPatientDirectory(long patientId) throws IOException {
        Path dir = getUploadsRoot().resolve("patients").resolve(String.valueOf(patientId)).normalize();
        
        // Sécurité : Vérifier que le chemin reste dans la racine des uploads
        if (!dir.startsWith(getUploadsRoot())) {
            throw new SecurityException("Tentative de Path Traversal détectée");
        }

        Files.createDirectories(dir);
        return dir;
    }

    public static Path resolveStoredPath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("Chemin fichier vide");
        }
        
        // On interdit les séquences de remontée dans le chemin stocké
        if (filePath.contains("..")) {
            throw new SecurityException("Chemin de fichier invalide");
        }

        Path p = getUploadsRoot().resolve(filePath).normalize();

        // Sécurité : Vérifier que le chemin résolu reste dans la racine des uploads
        if (!p.startsWith(getUploadsRoot())) {
            throw new SecurityException("Tentative de Path Traversal détectée");
        }

        return p;
    }

    public static String buildFileName(String prefix, LocalDate date) {
        String safePrefix = prefix.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
        String d = FILE_DATE.format(date != null ? date : LocalDate.now());
        return safePrefix + "_" + d + "_" + System.currentTimeMillis() + ".pdf";
    }

    public static String toRelativePath(Path absoluteFile) {
        return getUploadsRoot().relativize(absoluteFile).toString().replace('\\', '/');
    }
}
