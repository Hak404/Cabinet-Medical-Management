package com.cabinet.service;

import com.cabinet.dao.CabinetDAO;
import com.cabinet.dao.MedecinDAO;
import com.cabinet.dao.PatientDAO;
import com.cabinet.dao.RendezVousDAO;
import com.cabinet.dao.CongeDAO; // Jdid
import com.cabinet.model.Cabinet;
import com.cabinet.model.Medecin;
import com.cabinet.model.Patient;
import com.cabinet.model.RendezVous;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service de gestion des créneaux et de la réservation de rendez-vous.
 *
 * <p><b>Rôle :</b> calculer les disponibilités horaires et enregistrer un rendez-vous
 * après validations métier (congé, conflit, cabinet).</p>
 * <p><b>Objectif :</b> isoler la logique de génération de créneaux et les contrôles
 * de cohérence hors des servlets patient/médecin.</p>
 * <p><b>Place MVC :</b> couche service entre {@code RendezVousServlet} (contrôleur)
 * et les DAO → table {@code rendez_vous} en MySQL ; la vue JSP consomme
 * {@link SlotAvailability} pour l'affichage.</p>
 *
 * @see com.cabinet.dao.RendezVousDAO
 * @see com.cabinet.dao.CongeDAO
 * @since 1.0
 */
public class RendezVousService {

    /**
     * Représentation d'un créneau horaire pour affichage dans la JSP de prise de rendez-vous.
     */
    public static class SlotAvailability {
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final boolean available;

        /**
         * Construit un créneau avec son état de disponibilité.
         *
         * @param startTime heure de début
         * @param endTime heure de fin
         * @param available {@code true} si le créneau peut être réservé
         */
        public SlotAvailability(LocalTime startTime, LocalTime endTime, boolean available) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.available = available;
        }

        /**
         * @return heure de début du créneau
         */
        public LocalTime getStartTime() { return startTime; }

        /**
         * @return heure de fin du créneau
         */
        public LocalTime getEndTime() { return endTime; }

        /**
         * @return {@code true} si aucun conflit n'existe sur ce créneau
         */
        public boolean isAvailable() { return available; }
    }

    private final MedecinDAO medecinDAO = new MedecinDAO();
    private final CabinetDAO cabinetDAO = new CabinetDAO();
    private final PatientDAO patientDAO = new PatientDAO();
    private final RendezVousDAO rendezVousDAO = new RendezVousDAO();
    private final CongeDAO congeDAO = new CongeDAO(); // Utilisation de la nouvelle table

    /**
     * Liste tous les créneaux d'un médecin pour une date, avec indicateur de disponibilité.
     *
     * @param cabinetId identifiant du cabinet
     * @param medecinId identifiant du médecin
     * @param date jour de consultation
     * @return liste des créneaux (vide si paramètres invalides, médecin en congé ou indisponible)
     */
    public List<SlotAvailability> getAvailableSlots(Long cabinetId, Long medecinId, LocalDate date) {
        if (cabinetId == null || medecinId == null || date == null) {
            return List.of();
        }

        // 1. Vérification si le médecin est en congé via la table 'conge'
        if (congeDAO.isMedecinInConge(medecinId, date)) {
            return List.of();
        }

        Medecin medecin = medecinDAO.findById(medecinId).orElse(null);
        if (medecin == null || !Objects.equals(medecin.getCabinetId(), cabinetId)) {
            return List.of();
        }

        Cabinet cabinet = cabinetDAO.findById(cabinetId).orElse(null);
        if (cabinet == null) return List.of();

        int durationMinutes = cabinet.getDureeConsultationMinutes(); // 20min ou 30min
        LocalTime slotStart = medecin.getHeureDebut();
        LocalTime workEnd = medecin.getHeureFin();

        if (slotStart == null || workEnd == null || !slotStart.isBefore(workEnd)) {
            return List.of();
        }

        List<SlotAvailability> result = new ArrayList<>();

        // Boucle pour générer les créneaux intelligents
        while (!slotStart.plusMinutes(durationMinutes).isAfter(workEnd)) {
            LocalTime slotEnd = slotStart.plusMinutes(durationMinutes);

            // Vérifie si le créneau est déjà pris dans la table 'rendez_vous'[cite: 1]
            boolean conflict = rendezVousDAO.hasConflict(medecinId, date, slotStart, slotEnd);
            result.add(new SlotAvailability(slotStart, slotEnd, !conflict));

            slotStart = slotStart.plusMinutes(durationMinutes);
        }

        return result;
    }

    /**
     * Réserve un rendez-vous après contrôles (congé, conflit, entités existantes).
     *
     * @param patientId identifiant du patient
     * @param cabinetId identifiant du cabinet
     * @param medecinId identifiant du médecin
     * @param date jour du rendez-vous
     * @param slotStart heure de début du créneau choisi
     * @return rendez-vous persisté avec statut {@link RendezVous.Statut#CONFIRME}
     * @throws IllegalArgumentException si un paramètre est {@code null} ou si médecin/cabinet/patient introuvable
     * @throws IllegalStateException si le médecin est en congé ou si le créneau est déjà pris
     */
    public RendezVous bookAppointment(Long patientId, Long cabinetId, Long medecinId, LocalDate date, LocalTime slotStart) {

        // 1. Validation de base[cite: 1]
        if (patientId == null || cabinetId == null || medecinId == null || date == null || slotStart == null) {
            throw new IllegalArgumentException("Paramètres invalides");
        }

        // 2. Vérification congé (Sécurité côté serveur)[cite: 1]
        if (congeDAO.isMedecinInConge(medecinId, date)) {
            throw new IllegalStateException("Le médecin est indisponible (en congé)");
        }

        Medecin medecin = medecinDAO.findById(medecinId).orElse(null);
        Cabinet cabinet = cabinetDAO.findById(cabinetId).orElse(null);
        if (medecin == null || cabinet == null) throw new IllegalArgumentException("Données introuvables");

        int durationMinutes = cabinet.getDureeConsultationMinutes();
        LocalTime slotEnd = slotStart.plusMinutes(durationMinutes);

        // 3. Vérification des conflits de dernière minute[cite: 1]
        if (rendezVousDAO.hasConflict(medecinId, date, slotStart, slotEnd)) {
            throw new IllegalStateException("Ce créneau vient d'être réservé");
        }

        Patient patient = patientDAO.findById(patientId).orElse(null);
        if (patient == null) throw new IllegalArgumentException("Patient introuvable");

        // 4. Création de l'objet RendezVous[cite: 1]
        RendezVous rv = new RendezVous();
        rv.setCabinetId(cabinetId);
        rv.setMedecinId(medecinId);
        rv.setPatientId(patientId);
        rv.setDateRendezVous(date);
        rv.setStartTime(slotStart);
        rv.setEndTime(slotEnd);
        rv.setStatut(RendezVous.Statut.CONFIRME);

        if (rendezVousDAO.save(rv)) {
            return rv;
        }
        throw new RuntimeException("Erreur lors de l'enregistrement du rendez-vous");
    }
}
