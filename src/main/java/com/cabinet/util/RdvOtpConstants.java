package com.cabinet.util;

/**
 * Attributs de session pour la vérification OTP des rendez-vous patient.
 */
public final class RdvOtpConstants {

    public static final String SESSION_OTP_FLOW = "otp_flow";
    public static final String FLOW_RDV = "RDV";

    public static final String ATTR_EMAIL_OTP = "email_otp";
    public static final String ATTR_OTP_EXPIRES_AT = "email_otp_expires_at";
    public static final String ATTR_EMAIL_DELIVERY_WARNING = "email_delivery_warning";

    public static final String TEMP_CABINET_ID = "temp_cabinetId";
    public static final String TEMP_MEDECIN_ID = "temp_medecinId";
    public static final String TEMP_DATE = "temp_date";
    public static final String TEMP_START_TIME = "temp_startTime";

    /** Durée de validité du code (minutes). */
    public static final int OTP_VALIDITY_MINUTES = 15;

    private RdvOtpConstants() {
    }
}
