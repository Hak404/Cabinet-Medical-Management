package com.cabinet.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Vérification des jours de fermeture du cabinet (week-end et jours fériés marocains).
 *
 * <p><b>Rôle :</b> déterminer si une date est indisponible pour la prise de rendez-vous.</p>
 * <p><b>Objectif :</b> combiner règle week-end (samedi/dimanche) et jours fériés officiels
 * récupérés via l'API Nager.Date, avec cache en mémoire par année.</p>
 * <p><b>Place MVC :</b> règle métier légère invoquée par les servlets ou services de rendez-vous
 * avant validation côté DAO — sans accès HTTP servlet direct.</p>
 *
 * @see com.cabinet.service.RendezVousService
 * @since 1.0
 */
public class HolidayUtil {

    /** Cache des dates fériées déjà chargées pour éviter des appels API répétés. */
    private static Set<LocalDate> holidaysCache = new HashSet<>();

    /** Année correspondant au contenu actuel du cache. */
    private static int cachedYear = -1;

    /**
     * Charge les jours fériés du Maroc pour une année via l'API Nager.Date.
     * <p>Parse manuellement le JSON renvoyé pour extraire les champs {@code date}.</p>
     *
     * @param year année civile concernée
     */
    private static void fetchHolidaysFromAPI(int year) {
        // Si les données pour cette année sont déjà en cache, on ne fait rien
        if (cachedYear == year) return;

        try {
            // Construction de l'URL pour l'API Nager.Date (Pays : MA pour Maroc)
            URL url = URI.create("https://date.nager.at/api/v3/PublicHolidays/" + year + "/MA").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            // Vérification si la réponse est positive (Code 200 OK)
            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();

                // Extraction manuelle des dates du format JSON [{"date":"YYYY-MM-DD",...}]
                String json = response.toString();
                holidaysCache.clear();
                String[] entries = json.split("\"date\":\"");

                for (int i = 1; i < entries.length; i++) {
                    // On récupère les 10 premiers caractères (le format de la date est ISO YYYY-MM-DD)
                    String dateStr = entries[i].substring(0, 10);
                    holidaysCache.add(LocalDate.parse(dateStr));
                }
                // Mise à jour de l'année en cache
                cachedYear = year;
            }
        } catch (Exception e) {
            // En cas d'erreur (réseau, etc.), on affiche l'erreur dans la console
            System.err.println("Erreur API Jours Fériés : " + e.getMessage());
        }
    }

    /**
     * Indique si le cabinet est fermé à la date donnée (week-end ou jour férié).
     *
     * @param date date à vérifier
     * @return {@code true} si samedi, dimanche ou jour férié marocain ; {@code false} sinon
     */
    public static boolean isClosed(LocalDate date) {
        // 1. Vérification du week-end (Samedi et Dimanche)
        String dayOfWeek = date.getDayOfWeek().name();
        if (dayOfWeek.equals("SATURDAY") || dayOfWeek.equals("SUNDAY")) {
            return true;
        }

        // 2. Chargement des jours fériés de l'année concernée via l'API
        fetchHolidaysFromAPI(date.getYear());

        // 3. Retourne vrai si la date se trouve dans la liste des jours fériés
        return holidaysCache.contains(date);
    }
}
