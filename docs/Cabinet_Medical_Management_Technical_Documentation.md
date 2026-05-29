# Cabinet Medical Management - Technical Documentation

**Document type:** Production system audit & technical reference  
**Project:** CabinetMedicalManagement  
**Stack:** Java 20, Jakarta Servlet 6.0 (Tomcat 10), JSP/JSTL, MySQL 8, Maven WAR  
**Audit date:** May 2026  
**Scope:** Read-only analysis — no code modifications performed for this document  

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [System Architecture](#2-system-architecture)
3. [Database Design](#3-database-design)
4. [Controllers (Servlets)](#4-controllers-servlets)
5. [DAO Layer](#5-dao-layer)
6. [Models](#6-models)
7. [JSP Views](#7-jsp-views)
8. [Business Logic Flow](#8-business-logic-flow)
9. [Issues Found](#9-issues-found)
10. [Recommendations](#10-recommendations)

---

## 1. Introduction

### 1.1 Purpose

**Cabinet Medical Management** is a web application for managing a network of medical cabinets: user accounts with role-based access, appointment booking, consultations, prescriptions, lab test requests, doctor leave days, and patient self-registration with optional confirmation email.

### 1.2 Technology stack

| Layer | Technology |
|-------|------------|
| Runtime | Apache Tomcat 10+ (Jakarta EE 10 / Servlet 6.0) |
| Language | Java 20 (Maven `pom.xml`; user may deploy on Java 11+ with alignment) |
| Presentation | JSP, JSTL, Bootstrap 5 (doctor/admin), custom CSS (patient/register) |
| Persistence | JDBC, MySQL (`cabinet_medical` database) |
| Security | BCrypt passwords, `AuthFilter` session + role checks |
| Email | Jakarta Mail (`EmailUtil`), Gmail SMTP via properties |
| Build | Maven WAR (`CabinetMedicalManagement.war`) |

### 1.3 Project structure

```
CabinetMedicalManagement/
├── pom.xml
├── src/main/
│   ├── java/com/cabinet/
│   │   ├── controller/     # 24 HTTP servlets (@WebServlet)
│   │   ├── dao/            # 10 JDBC data access classes
│   │   ├── filter/         # AuthFilter, CharacterEncodingFilter
│   │   ├── model/          # 13 entity POJOs
│   │   ├── service/        # AuthService, RendezVousService
│   │   └── util/           # DB, email, session, password, holidays
│   ├── resources/
│   │   ├── schema.sql
│   │   ├── email.properties / email.local.properties
│   │   └── data/seed_admin.sql
│   └── webapp/
│       ├── WEB-INF/web.xml
│       ├── admin/, medecin/, patient/, secretaire/, pharmacie/
│       ├── shared/ (navbar, sidebar)
│       └── *.jsp (login, register, index, error)
└── docs/ (this file)
```

### 1.4 Actors (roles)

| Role | Description |
|------|-------------|
| **ADMIN** | Global administration: patients, doctors, cabinets |
| **MEDECIN** | Clinical workflow: appointments, consultations, prescriptions, analyses, leave |
| **PATIENT** | Self-registration, login, book/cancel appointments |
| **SECRETAIRE** | Role exists; dashboard is a placeholder |
| **PHARMACIE** | Role exists; dashboard is a placeholder; ordonnance linkage in DB not fully used in UI |

---

## 2. System Architecture

### 2.1 Architectural style (MVC)

The application follows a **classic Java web MVC** pattern without a formal framework (no Spring):

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────┐     ┌──────────┐
│  JSP View   │ ◄── │ Servlet (Controller)│ ──► │ Service (opt)│ ──► │   DAO    │ ──► MySQL
└─────────────┘     └──────────────────┘     └─────────────┘     └──────────┘
       ▲                      │
       │                      ▼
       │               HttpSession (User)
       │                      │
       └──────── AuthFilter / CharacterEncodingFilter
```

- **View:** JSP pages under `webapp/`
- **Controller:** Servlets annotated with `@WebServlet`
- **Model:** POJOs in `com.cabinet.model`
- **Persistence:** DAO classes with raw JDBC
- **Cross-cutting:** Filters for UTF-8 and authentication/authorization

### 2.2 Request lifecycle

1. HTTP request hits Tomcat.
2. **CharacterEncodingFilter** (`/*`) sets UTF-8 on request/response.
3. **AuthFilter** (`/*`) evaluates URI path:
   - Public paths (register, index, static assets) → pass through.
   - Login paths → allow guests; redirect authenticated users to role dashboard.
   - Role-prefixed paths (`/admin/`, `/medecin/`, `/patient/`, etc.) → require session user with matching `User.Role`.
4. Servlet handles GET/POST, calls DAO/Service, sets request attributes, **forwards** to JSP or **redirects**.
5. JSP renders HTML (often with data from request attributes).

### 2.3 Session management

- Session timeout: **30 minutes** (`web.xml`).
- Cookie: **HTTP-only** enabled.
- After login, `SessionUtil.bindUser()` stores `User` in session (password cleared from session copy).
- Session keys: `SessionConstants.ATTR_USER`, `ATTR_USER_ID`, `ATTR_USER_EMAIL`, `ATTR_ROLE`.

### 2.4 Servlet registration

Servlets use **`@WebServlet` annotations** (not declared in `web.xml`). Only filters, welcome file, error pages, and session config are in `web.xml`.

### 2.5 What works well architecturally

- Clear package separation (`controller`, `dao`, `model`, `service`, `util`, `filter`).
- Centralized auth filter with role-based URL prefixes.
- Password hashing with BCrypt (`PasswordUtil`).
- Parameterized SQL throughout DAO layer (low SQL injection risk).
- Transactional writes for critical flows (`ConsultationDAO.saveFullConsultation`, `CabinetDAO.createCabinetWithMedecin`, `PatientDAO.savePatient`).
- Password stripped from session after login.
- UTF-8 enforced globally.

---

## 3. Database Design

### 3.1 Database

- **Name:** `cabinet_medical`
- **Charset:** `utf8mb4` / `utf8mb4_unicode_ci`
- **Script:** `src/main/resources/schema.sql` (drops and recreates DB — destructive)

### 3.2 Entity-relationship overview

```
user (base)
 ├── admin (1:1)
 ├── medecin (1:1) ──► cabinet (1:1 medecin)
 ├── patient (1:1)
 ├── secretaire (1:1) ──► medecin, cabinet
 └── pharmacie (1:1) ──► cabinet (optional)

medecin ──► conge (1:N)
cabinet + medecin + patient ──► rendez_vous (N)
rendez_vous (1:1) ──► consultation
consultation (1:1) ──► ordonnance ──► medicament_ordonnance (1:N)
consultation (1:N) ──► analyse_demandee
```

### 3.3 Tables summary

| Table | Purpose | Key constraints |
|-------|---------|-----------------|
| `user` | All login accounts | `uk_user_email`, `role`, `active` |
| `admin` | Admin profile extension | FK → `user`, CASCADE delete |
| `medecin` | Doctor hours & specialty | `heure_debut < heure_fin`, FK → `user` |
| `cabinet` | Physical cabinet | `uk_cabinet_nom`, `uk_cabinet_medecin` (1 doctor = 1 cabinet) |
| `patient` | Patient demographics | `uk_patient_cin`, FK → `user` |
| `secretaire` | Secretary linked to doctor/cabinet | FKs to `user`, `medecin`, `cabinet` |
| `pharmacie` | Pharmacy partner account | Optional `cabinet_id` |
| `conge` | Doctor leave dates | `uk_conge_medecin_date` |
| `rendez_vous` | Appointments | `uk_rdv_medecin_date_start`, status enum in app |
| `consultation` | Visit record | `uk_consultation_rdv` (1 RDV = 1 consultation) |
| `ordonnance` | Prescription header | `uk_ordonnance_consultation`, optional `pharmacie_id` |
| `medicament_ordonnance` | Prescription lines | FK → `ordonnance`, CASCADE |
| `analyse_demandee` | Lab test codes per consultation | `uk_analyse_dem_cons_code` |

### 3.4 Appointment status values (application)

`EN_ATTENTE`, `CONFIRME`, `EN_COURS`, `TERMINE`, `ANNULE` — stored as `VARCHAR(20)` in `rendez_vous.statut`. Cancellations are **soft** (status update, no DELETE).

### 3.5 Seed data

- `data/seed_admin.sql` — default admin account (documented in schema comments: `admin@cabinet.com` / `Admin123` after BCrypt seed).

### 3.6 Connection configuration

`DBConnection.java` uses hardcoded JDBC URL:

- Host: `localhost:3306/cabinet_medical`
- User: `root`
- Password: empty string

No connection pool (new connection per DAO operation).

---

## 4. Controllers (Servlets)

All controllers live in `com.cabinet.controller`. Unless noted, servlets rely on **AuthFilter** for role enforcement.

### 4.1 Authentication & registration

#### `LoginServlet` — `/login`

| Method | Purpose | Input | Output / DB |
|--------|---------|-------|-------------|
| `doGet` | Show login or redirect if already authenticated | — | Forward `login.jsp` or redirect to role home |
| `doPost` | Authenticate user | `email`, `password` | `AuthService.login()` → `UserDAO.authenticate`; on success `SessionUtil.bindUser`, redirect by role |

**Why it exists:** Entry point for all roles.

**Issues:** No email trim; no brute-force protection.

---

#### `LogoutServlet` — `/logout`

| Method | Purpose | Input | Output |
|--------|---------|-------|--------|
| `doGet` / `doPost` | End session | — | `SessionUtil.invalidate`, redirect `/login?success=deconnexion` |

**Issues:** Logout via GET (minor CSRF/prefetch risk).

---

#### `RegisterPatientServlet` — `/patient/register`

| Method | Purpose | Input | Output / DB |
|--------|---------|-------|-------------|
| `doGet` | Display registration form | — | Forward `register.jsp` |
| `doPost` | Register patient + send confirmation email | `nom`, `prenom`, `telephone`, `email`, `cin`, `adresse`, `dateNaissance`, `password` | Duplicate check `UserDAO.findByEmail`; `AuthService.registerPatient` → `PatientDAO.savePatient`; then `EmailUtil.sendRegistrationConfirmation` |

**Business flow:** DB insert **first**; email **after** success. Email failure does **not** rollback patient (redirect to login with `warning=envoi_email`).

**Issues:** Confirmation code is **not stored or verified** in DB; code logged at INFO; no password strength rules.

---

#### `ConfirmOTPServlet` — `/patient/confirm-otp` *(legacy stub)*

| Method | Purpose |
|--------|---------|
| `doGet` / `doPost` | Redirect to `/login.jsp` |

**Status:** **Dead code** — OTP flows removed; JSPs `register_verify_otp.jsp` and `patient/verify_otp.jsp` still exist but are unused.

---

### 4.2 Patient zone

#### `DashboardPatientServlet` — `/patient/dashboard`

| Method | Purpose | DB |
|--------|---------|-----|
| `doGet` | Patient home: upcoming appointments, stats | `RendezVousDAO.findByPatient`, counts |

---

#### `RendezVousServlet` — `/patient/rendezvous`

| Method | Purpose | DB / logic |
|--------|---------|------------|
| `doGet` | Booking UI: cabinets, doctors, slots | `CabinetDAO.findAll`, `MedecinDAO.findAll`, `RendezVousService.getAvailableSlots`, `HolidayUtil.isClosed` |
| `doPost` | Book appointment | Same-day check `hasRendezVousOnSameDay`; `RendezVousService.bookAppointment` |

**Issues:** Lists all doctors/cabinets (no filtering); broad exception messages in redirect URL; no explicit role check in servlet (filter only).

---

#### `PatientRendezVousCancelServlet` — `/patient/rdv/cancel`

| Method | Purpose | DB |
|--------|---------|-----|
| `doGet` / `doPost` | Cancel own appointment | Load RDV; `RendezVousCancellationPolicy` (48h rule); `updateStatut(ANNULE)` |

**Issues:** **State-changing GET**; no CSRF token.

---

### 4.3 Doctor (médecin) zone

#### `MedecinServlet` — `/medecin/dashboard`

| Method | Purpose | DB |
|--------|---------|-----|
| `doGet` | Dashboard: today's RDVs, week summary, next appointment | `RendezVousDAO`, `ConsultationDAO`, `CongeDAO` |

---

#### `ConsultationServlet` — `/medecin/consultation`

| Method | Purpose | DB |
|--------|---------|-----|
| `doGet` | Open consultation for `rdvId`; auto-set RDV `EN_COURS` if not `TERMINE` | `RendezVousDAO`, `ConsultationDAO`, `OrdonnanceDAO`, `AnalyseDemandeeDAO` |
| `doPost` | Save diagnostic, medicines, analyses | `ConsultationDAO.saveFullConsultation` (transaction) |

**Business logic:** Whitelist of analysis codes (`ANALYSES_DISPONIBLES`); parses dynamic medicament rows from form.

---

#### `ConsultationsServlet` — `/medecin/consultations`

| Method | Purpose | DB |
|--------|---------|-----|
| `doGet` | List consultations for date | `ConsultationDAO.findByMedecinAndDate` |

---

#### `MedecinRendezVousServlet` — `/medecin/rendezvous`

| Method | Purpose | DB |
|--------|---------|-----|
| `doGet` | List doctor's appointments | `RendezVousDAO.findByMedecinAndDate` |
| `doPost` | Update status: confirm / attente / cancel | `updateStatut` (ownership checked) |

---

#### `MedecinPatientsServlet` — `/medecin/patients`

| Method | Purpose | DB |
|--------|---------|-----|
| `doGet` | Patients with activity on date | `PatientDAO.findByMedecinAndDateWithActivity` |

---

#### `MedecinOrdonnancesServlet` — `/medecin/ordonnances`

| Method | Purpose | DB |
|--------|---------|-----|
| `doGet` | Prescriptions for date | `OrdonnanceDAO.findByMedecinAndDate` |

---

#### `MedecinAnalysesServlet` — `/medecin/analyses`

| Method | Purpose | DB |
|--------|---------|-----|
| `doGet` | Requested analyses for date | `AnalyseDemandeeDAO.findByMedecinAndDate` |

---

#### `CongesServlet` — `/medecin/conges`

| Method | Purpose | DB |
|--------|---------|-----|
| `doGet` | List leave days | `CongeDAO.findByMedecin` |
| `doPost` | Add leave or `action=delete` | `CongeDAO.save` / `delete` |

**Issues:** Delete by `congeId` **without verifying** it belongs to current doctor (IDOR risk).

---

#### `SecretaireServlet` — `/medecin/secretaire/add`

| Method | Purpose | DB |
|--------|---------|-----|
| `doPost` | Doctor creates secretary for own cabinet | `SecretaireDAO.saveSecretaire` |

---

### 4.4 Admin zone

#### `AdminServlet` — `/admin/dashboard`

| Method | Purpose | DB |
|--------|---------|-----|
| `doGet` | Admin overview counts and lists | `PatientDAO`, `MedecinDAO`, `CabinetDAO`, `RendezVousDAO` |

---

#### `AddPatientServlet` — `/admin/patients/add`

| Method | Purpose | DB |
|--------|---------|-----|
| `doPost` | Admin creates patient | `PatientDAO.savePatient` (BCrypt password) |

**Issues:** Minimal validation; no duplicate email check.

---

#### `DeletePatientServlet` — `/admin/patients/delete`

| Method | Purpose | DB |
|--------|---------|-----|
| `doPost` | Delete patient by `id` | `PatientDAO.deleteById` (deletes `user`, CASCADE `patient`) |

---

#### `DeleteMedecinServlet` — `/admin/medecins/delete`

| Method | Purpose | DB |
|--------|---------|-----|
| `doPost` | Delete doctor by `id` | `MedecinDAO.deleteById` |

---

#### `AddCabinetMedecinServlet` — `/admin/cabinets-medecins/add`

| Method | Purpose | DB |
|--------|---------|-----|
| `doPost` | Create cabinet + doctor atomically | `CabinetDAO.createCabinetWithMedecin` |

---

#### `AddCabinetServlet` — `/admin/cabinets/add`

| Method | Purpose |
|--------|---------|
| `doPost` | Always redirects with error `use_cabinet_medecin_form` |

**Status:** **Dead code** — `CabinetDAO` instantiated but never used.

---

### 4.5 Other roles

#### `DashboardSecretaireServlet` — `/secretaire/dashboard`

| Method | Purpose |
|--------|---------|
| `doGet` | Forward empty placeholder JSP |

#### `DashboardPharmacieServlet` — `/pharmacie/dashboard`

| Method | Purpose |
|--------|---------|
| `doGet` | Forward empty placeholder JSP |

---

### 4.6 Filters

#### `AuthFilter`

| Method | Purpose |
|--------|---------|
| `doFilter` | Route protection: public / login / role-based paths |

**Gap:** Paths without role prefix (e.g. direct JSP access to some files) may pass without auth if not under `/patient/` etc.

#### `CharacterEncodingFilter`

| Method | Purpose |
|--------|---------|
| `doFilter` | Force UTF-8 encoding |

---

## 5. DAO Layer

All DAOs use `DBConnection.getConnection()` and **PreparedStatement**. Pattern: try-with-resources or `finally` close.

### 5.1 `UserDAO`

| Method | Parameters | Returns | SQL operation |
|--------|------------|---------|---------------|
| `findByEmail` | `email` | `User` or null | `SELECT` from `user` WHERE email |
| `authenticate` | `email`, `plainPassword` | `User` or null | Delegates to `findByEmail` + BCrypt verify; checks `active` |
| `countByRole` | `Role` | `long` | `COUNT(*)` by role |

---

### 5.2 `PatientDAO`

| Method | Parameters | Returns | SQL operation |
|--------|------------|---------|---------------|
| `findById` | `id` | `Patient` | JOIN `user` + `patient` |
| `findAll` | — | `List<Patient>` | JOIN, ORDER BY name |
| `findByMedecinAndDateWithActivity` | `medecinId`, `date` | `List<Patient>` | DISTINCT via `rendez_vous` |
| `countAll` | — | `long` | `COUNT(*)` |
| `savePatient` | `Patient` | error code or `null` | **TX:** `INSERT user`, `INSERT patient` |
| `deleteById` | `patientId` | void | `DELETE user` (CASCADE) |

---

### 5.3 `MedecinDAO`

| Method | Parameters | Returns | SQL operation |
|--------|------------|---------|---------------|
| `findById` | `id` | `Medecin` | JOIN `user`, `medecin`, LEFT JOIN `cabinet` |
| `findAll` | — | `List<Medecin>` | Same |
| `countAll` | — | `long` | `COUNT(*)` |
| `addMedecin` | `Medecin`, hours | error or `null` | **TX:** INSERT user + medecin |
| `deleteById` | `id` | void | DELETE user |

---

### 5.4 `SecretaireDAO`

| Method | Parameters | Returns | SQL operation |
|--------|------------|---------|---------------|
| `saveSecretaire` | `Secretaire` | error or `null` | **TX:** INSERT user + secretaire |

---

### 5.5 `CabinetDAO`

| Method | Parameters | Returns | SQL operation |
|--------|------------|---------|---------------|
| `findById` | `id` | `Cabinet` | SELECT |
| `findAll` | — | `List<Cabinet>` | SELECT ORDER BY nom |
| `countAll` | — | `long` | COUNT |
| `addCabinet` | `Cabinet` | error or `null` | Existence checks + INSERT |
| `deleteById` | `id` | void | DELETE cabinet |
| `createCabinetWithMedecin` | `Cabinet`, `Medecin` | error or `null` | **TX:** user + medecin + cabinet |

---

### 5.6 `RendezVousDAO`

| Method | Parameters | Returns | SQL operation |
|--------|------------|---------|---------------|
| `findById` | `id` | `RendezVous` | SELECT + JOINs |
| `findByMedecinAndDate` | `medecinId`, `date` | `List` | SELECT |
| `findByMedecinBetweenDates` | `medecinId`, from, to | `List` | BETWEEN |
| `findByPatient` | `patientId` | `List` | SELECT |
| `findNextForMedecin` | `medecinId` | `RendezVous` | SELECT LIMIT 1 |
| `save` | `RendezVous` | `RendezVous` | INSERT |
| `updateStatut` | `rdvId`, `Statut` | void | UPDATE |
| `hasConflict` | `medecinId`, date, start, end | `boolean` | COUNT overlap |
| `hasRendezVousOnSameDay` | `patientId`, `cabinetId`, `date` | `boolean` | COUNT |
| `countToday` / `countByMedecinGroupedByDate` | various | aggregates | COUNT / GROUP BY |

**Issue:** `hasConflict`, `hasRendezVousOnSameDay` return `false` on DB error (**fail-open**).

---

### 5.7 `ConsultationDAO`

| Method | Parameters | Returns | SQL operation |
|--------|------------|---------|---------------|
| `findByRendezVousId` | `rdvId` | `Consultation` | SELECT |
| `save` / `update` | `Consultation` | entity / void | INSERT / UPDATE |
| `saveFullConsultation` | rdvId, diagnostic, remarque, meds, codes | `Long` consultation id | **Single TX:** consultation, ordonnance, medicaments, analyses, RDV → TERMINE |
| `findByMedecinAndDate` | `medecinId`, `date` | `List` | JOIN query |
| `countCompletedTodayForMedecin` | `medecinId` | `long` | Uses MySQL `CURDATE()` |

---

### 5.8 `OrdonnanceDAO`

| Method | Parameters | Returns | SQL operation |
|--------|------------|---------|---------------|
| `findOrdonnanceIdByConsultationId` | `consultationId` | `Long` | SELECT |
| `findMedicamentsByOrdonnanceId` | `ordonnanceId` | `List` | SELECT lines |
| `ensureOrdonnance` | `consultationId` | `Long` | SELECT then INSERT (race possible) |
| `replaceMedicaments` | `ordonnanceId`, list | void | **TX:** DELETE + batch INSERT |
| `findByMedecinAndDate` | `medecinId`, `date` | `List` view DTO | JOIN |

---

### 5.9 `AnalyseDemandeeDAO`

| Method | Parameters | Returns | SQL operation |
|--------|------------|---------|---------------|
| `findCodesByConsultationId` | `consultationId` | `Set<String>` | SELECT |
| `replaceDemandes` | `consultationId`, codes | void | **TX:** DELETE all + batch INSERT |
| `findByMedecinAndDate` | `medecinId`, `date` | `List` view | JOIN |

---

### 5.10 `CongeDAO`

| Method | Parameters | Returns | SQL operation |
|--------|------------|---------|---------------|
| `save` | `Conge` | void | INSERT |
| `findByMedecin` | `medecinId` | `List` | SELECT |
| `delete` | `id` | void | DELETE |
| `isMedecinInConge` | `medecinId`, `date` | `boolean` | COUNT (fail-open on error) |

---

## 6. Models

POJOs in `com.cabinet.model` — **no business logic** except where noted. Mapping done in DAO `map*` methods.

### 6.1 `User`

**Purpose:** Base account for all actors.

| Field | Type | Notes |
|-------|------|-------|
| id, nom, prenom, email, password, telephone | various | password = BCrypt hash in DB |
| role | `enum Role` | ADMIN, MEDECIN, SECRETAIRE, PATIENT, PHARMACIE |
| active | boolean | Inactive users cannot login |

**Methods:** Standard getters/setters only.

---

### 6.2 `Patient` extends `User`

**Purpose:** Patient-specific data.

| Field | Type |
|-------|------|
| cin, adresse, dateNaissance | String / LocalDate |

**Constructor:** Sets `Role.PATIENT`.

---

### 6.3 `Medecin` extends `User`

**Purpose:** Doctor profile.

| Field | Type |
|-------|------|
| specialite, heureDebut, heureFin | String / LocalTime |
| cabinetId | Long (from JOIN) |

---

### 6.4 `Admin`, `Secretaire`, `Pharmacie` extends `User`

**Purpose:** Role-specific extensions; secretary links to `medecinId`, `cabinetId`; pharmacy optional `cabinetId`.

---

### 6.5 `Cabinet`

**Purpose:** Medical office tied to one doctor.

| Field | Type |
|-------|------|
| id, nom, adresse, dureeConsultationMinutes, medecinId | various |

---

### 6.6 `RendezVous`

**Purpose:** Appointment entity with display fields (cabinet name, doctor/patient full names).

| Field | Type |
|-------|------|
| statut | `enum Statut` |
| dateRendezVous, startTime, endTime | LocalDate / LocalTime |

**Methods with logic:**

| Method | Purpose |
|--------|---------|
| `isPatientCancellationAllowed()` | Delegates to `RendezVousCancellationPolicy` |
| `getPatientCancellationBlockReason()` | Human-readable denial reason |
| `isPatientCancelActionVisible()` | UI helper for cancel button |

---

### 6.7 `Consultation`, `Ordonnance`, `MedicamentOrdonnance`, `AnalyseDemandee`, `Conge`

Standard entities matching DB tables. `Ordonnance` includes `statut` and optional `pharmacieId` (pharmacy workflow not implemented in UI).

---

## 7. JSP Views

### 7.1 Active pages (servlet-backed)

| JSP | Role | Servlet URL | Function |
|-----|------|-------------|----------|
| `login.jsp` | Public | `/login` | Login form |
| `register.jsp` | Public | `/patient/register` | Patient registration |
| `patient/dashboardPatient.jsp` | PATIENT | `/patient/dashboard` | RDV list, cancel |
| `patient/rendezvous.jsp` | PATIENT | `/patient/rendezvous` | Book appointment |
| `medecin/dashboardMedecin.jsp` | MEDECIN | `/medecin/dashboard` | Doctor home |
| `medecin/consultation.jsp` | MEDECIN | `/medecin/consultation` | Consultation form |
| `medecin/consultations.jsp` | MEDECIN | `/medecin/consultations` | Consultation list |
| `medecin/rendezvous.jsp` | MEDECIN | `/medecin/rendezvous` | Manage RDVs |
| `medecin/patients.jsp` | MEDECIN | `/medecin/patients` | Patient list |
| `medecin/ordonnances.jsp` | MEDECIN | `/medecin/ordonnances` | Prescriptions |
| `medecin/analyses.jsp` | MEDECIN | `/medecin/analyses` | Lab requests |
| `medecin/conges.jsp` | MEDECIN | `/medecin/conges` | Leave management |
| `admin/dashboardAdmin.jsp` | ADMIN | `/admin/dashboard` | Admin CRUD hub |
| `secretaire/dashboardSecretaire.jsp` | SECRETAIRE | `/secretaire/dashboard` | Placeholder |
| `pharmacie/dashboardPharmacie.jsp` | PHARMACIE | `/pharmacie/dashboard` | Placeholder |

### 7.2 Shared fragments

| File | Purpose |
|------|---------|
| `shared/navbar.jsp` | Top navigation (medecin/admin UI) |
| `shared/sidebar.jsp` | Role-based menu — **contains many links to non-existent routes** |

### 7.3 Orphan / legacy JSPs

| JSP | Status |
|-----|--------|
| `register_verify_otp.jsp` | Orphan — OTP registration removed |
| `patient/verify_otp.jsp` | Orphan — RDV OTP removed |
| `WEB-INF/jspf/session-required.jsp` | Never included |

### 7.4 UI stacks

- **Bootstrap 5 + `style.css`:** Doctor and admin modules.
- **Legacy `styles.css`:** Patient, register, error pages.

### 7.5 Security on views

- **No CSRF tokens** on any form.
- Delete actions use JavaScript `confirm()` only.
- Direct JSP URL access possible for public pages (`register_verify_otp.jsp`).

---

## 8. Business Logic Flow

### 8.1 Patient registration

```
register.jsp
    → POST /patient/register (RegisterPatientServlet)
        → Validate fields + email format
        → UserDAO.findByEmail (duplicate check)
        → AuthService.registerPatient
            → PasswordUtil.hashPassword (BCrypt)
            → PatientDAO.savePatient (TX: user + patient)
        → [if DB OK] Generate 6-digit code
        → EmailUtil.sendRegistrationConfirmation (SMTP)
        → Redirect:
            - Email OK → login.jsp?success=inscription_ok
            - Email fail → login.jsp?success=inscription_ok&warning=envoi_email
```

**Note:** Confirmation code is informational only — **not validated** anywhere.

---

### 8.2 Login

```
login.jsp → POST /login
    → AuthService.login → UserDAO.authenticate
    → SessionUtil.bindUser (password cleared)
    → Redirect: /admin/dashboard | /medecin/dashboard | /patient/dashboard | ...
```

---

### 8.3 Appointment booking (patient)

```
patient/rendezvous.jsp → POST /patient/rendezvous
    → HolidayUtil.isClosed (weekend + Morocco public holidays API)
    → RendezVousDAO.hasRendezVousOnSameDay (1 RDV per cabinet per day)
    → RendezVousService.bookAppointment
        → CongeDAO.isMedecinInConge
        → Slot duration from cabinet
        → RendezVousDAO.hasConflict
        → INSERT rendez_vous (statut CONFIRME)
    → Redirect patient/dashboard?success=RDV_Confirme
```

---

### 8.4 Appointment cancellation (patient)

```
dashboardPatient.jsp → POST/GET /patient/rdv/cancel?id=
    → Load RDV, verify patientId ownership
    → RendezVousCancellationPolicy (≥ 48 hours before)
    → updateStatut(ANNULE)
```

---

### 8.5 Consultation (doctor)

```
medecin/consultation.jsp?rdvId=
    → GET: load/create consultation; RDV → EN_COURS
    → POST: ConsultationDAO.saveFullConsultation
        → Upsert consultation
        → Ordonnance + medicaments
        → Analyse codes (whitelist)
        → RDV → TERMINE
```

---

### 8.6 Admin: create doctor + cabinet

```
admin/dashboardAdmin.jsp → POST /admin/cabinets-medecins/add
    → CabinetDAO.createCabinetWithMedecin (single transaction)
```

---

### 8.7 Service layer summary

#### `AuthService`

| Method | Purpose |
|--------|---------|
| `login` | Email/password authentication |
| `registerPatient` | Validate + hash + persist patient |

#### `RendezVousService`

| Method | Purpose |
|--------|---------|
| `getAvailableSlots` | Generate time slots; mark conflicts; respect leave |
| `bookAppointment` | Final validation + persist CONFIRME RDV |

---

### 8.8 Utility classes (non-DAO)

| Class | Purpose |
|-------|---------|
| `DBConnection` | JDBC connection factory |
| `PasswordUtil` | BCrypt hash/verify |
| `SessionUtil` | Session bind/read/invalidate/redirects |
| `SessionConstants` | Session attribute names |
| `EmailUtil` / `EmailConfig` / `EmailSendResult` | Registration confirmation email only |
| `HolidayUtil` | Weekend + Nager.Date API (Morocco holidays), in-memory cache |
| `RendezVousCancellationPolicy` | 48-hour cancellation rule |

---

## 9. Issues Found

### 9.1 Critical / high severity

| ID | Issue | Location | Impact |
|----|-------|----------|--------|
| H1 | **No CSRF protection** | All POST forms | Forged requests can book/cancel RDV, delete users, save consultations |
| H2 | **Confirmation code not stored/verified** | `RegisterPatientServlet` | Email OTP is cosmetic; accounts active without verification |
| H3 | **Hardcoded DB credentials** | `DBConnection` | Production security risk; no environment-based config |
| H4 | **No connection pool** | All DAOs | Poor scalability; connection overhead per query |
| H5 | **State-changing GET** | `PatientRendezVousCancelServlet.doGet` | Accidental/malicious cancellation via link |
| H6 | **IDOR on congé delete** | `CongesServlet` | Any doctor could delete another's leave if ID is known |
| H7 | **Secrets in repo example** | `email.properties.example` (historically) | Credential exposure if real passwords committed |

### 9.2 Medium severity

| ID | Issue | Location | Impact |
|----|-------|----------|--------|
| M1 | **Fail-open conflict checks** | `RendezVousDAO.hasConflict`, `CongeDAO.isMedecinInConge` | Double booking possible if DB errors |
| M2 | **Sensitive code in logs** | `RegisterPatientServlet` | Confirmation code in plain log |
| M3 | **Password hash in SELECT lists** | Patient/Medecin DAO findAll | Unnecessary exposure in memory |
| M4 | **Ordonnance race** | `OrdonnanceDAO.ensureOrdonnance` | Duplicate ordonnance possible under concurrency |
| M5 | **External API dependency** | `HolidayUtil` | Booking blocked/allowed incorrectly if API down |
| M6 | **Error messages in URL** | `RendezVousServlet` | Information leakage |
| M7 | **Direct JSP access** | Role JSPs under `/medecin/` etc. | Empty/broken pages without servlet data |

### 9.3 Low severity / technical debt

| ID | Issue | Location |
|----|-------|----------|
| L1 | Dead servlet `ConfirmOTPServlet` | controller |
| L2 | Dead servlet `AddCabinetServlet` | controller |
| L3 | Orphan JSPs (OTP pages) | webapp |
| L4 | Orphan `session-required.jsp` | WEB-INF |
| L5 | Sidebar links to non-existent routes | `shared/sidebar.jsp` |
| L6 | Stray comments `[cite: 1]`, `// Jdid` | `RendezVousService` |
| L7 | `printStackTrace()` in catch blocks | Several servlets/DAOs |
| L8 | Inconsistent error handling (null vs throw) | DAO layer |
| L9 | SECRETAIRE / PHARMACIE roles incomplete | dashboards only |
| L10 | No password strength / email normalization | registration, admin add user |
| L11 | Java 20 in pom vs "Java 8/11" deployment note | build config mismatch |
| L12 | Pharmacie workflow not implemented | ordonnance.pharmacie_id unused in UI |

### 9.4 Duplicated code patterns

| Pattern | Occurrences |
|---------|-------------|
| `parseLong` / `parseDate` private helpers | Multiple servlets |
| Manual encoding redirect helpers | RendezVousServlet, PatientRendezVousCancelServlet |
| Role check `user.getRole() != X` | Some servlets (redundant with filter) |
| INSERT user + role table transaction | PatientDAO, MedecinDAO, SecretaireDAO, CabinetDAO |
| Dashboard forward + role guard | Dashboard* servlets |

### 9.5 What is working correctly

- Role-based URL protection via `AuthFilter` for main modules.
- BCrypt password storage and verification.
- Appointment slot generation with conflict detection (under normal DB conditions).
- 48-hour patient cancellation policy (well-tested utility class).
- Full consultation save in a **single database transaction**.
- Soft-delete semantics for appointments (ANNULE status).
- UTF-8 encoding filter.
- Parameterized SQL (no dynamic SQL injection found).
- Patient registration persists before email; email failure does not rollback (as designed).
- Morocco holiday + weekend closure for booking.
- Admin atomic create cabinet+doctor.

---

## 10. Recommendations

### 10.1 Security (priority 1)

1. **Add CSRF tokens** to all state-changing forms (login exception optional).
2. **Remove GET from cancel servlet** — POST only with CSRF.
3. **Externalize DB config** via JNDI DataSource or environment variables; add **connection pool** (HikariCP).
4. **Verify congé ownership** on delete in `CongesServlet`.
5. **Stop logging confirmation codes** in production; use masked logs.
6. Either **implement email verification** (store hashed code + expiry in DB) or **remove code generation** and send plain welcome email only.

### 10.2 Architecture (priority 2)

1. Introduce a thin **service layer** for all modules (not only auth/RDV).
2. Centralize **exception handling** — avoid `printStackTrace` and raw messages in URLs.
3. **Block direct JSP access** — only allow forwards from servlets (or move JSPs under `WEB-INF`).
4. Remove **dead code**: `ConfirmOTPServlet`, `AddCabinetServlet`, orphan JSPs.
5. Fix **sidebar navigation** to match real servlet mappings.

### 10.3 Data layer (priority 2)

1. Return **typed results** (Optional, Result enum) instead of null/0 on errors.
2. Change fail-open methods to **fail-closed** or propagate exceptions.
3. Wrap `ensureOrdonnance` in same transaction as consultation save.
4. Avoid loading **password** column except for authentication queries.

### 10.4 Features — implemented vs missing

| Feature | Status |
|---------|--------|
| Multi-role login | ✅ Implemented |
| Patient self-registration | ✅ Implemented |
| Registration email | ✅ Sent (not verified) |
| Patient book/cancel RDV | ✅ Implemented |
| Doctor consultation + ordonnance + analyses | ✅ Implemented |
| Doctor leave (congé) | ✅ Implemented |
| Admin CRUD patients/doctors/cabinets | ✅ Partial (single dashboard) |
| Secretary module | ❌ Placeholder only |
| Pharmacy module | ❌ Placeholder only |
| Email OTP verification | ❌ Removed / stub |
| Password reset | ❌ Missing |
| Audit logs | ❌ Missing |
| Reports / statistics | ❌ Minimal (counts on dashboards) |
| Reminder emails / background jobs | ❌ Not present (by design) |
| API REST layer | ❌ Missing |
| Unit / integration tests | ❌ Not found in project |

### 10.5 Operations & production readiness

1. Use `email.local.properties` or env vars for SMTP; never commit secrets.
2. Document **Tomcat restart** required after email config changes.
3. Add health-check endpoint or servlet for DB connectivity.
4. Align **Java version** in documentation, `pom.xml`, and deployment server.
5. Add automated tests for `RendezVousCancellationPolicy`, `RendezVousService` slot logic, and DAO transactions.

### 10.6 Documentation maintenance

- Keep this document updated when servlets or schema change.
- Maintain an **OpenAPI or route table** if REST is added later.
- Add ER diagram image generated from `schema.sql` for onboarding.

---

## Appendix A — Servlet URL quick reference

| URL | Class | Role |
|-----|-------|------|
| `/login` | LoginServlet | Public |
| `/logout` | LogoutServlet | Public |
| `/patient/register` | RegisterPatientServlet | Public |
| `/patient/confirm-otp` | ConfirmOTPServlet (stub) | Public |
| `/patient/dashboard` | DashboardPatientServlet | PATIENT |
| `/patient/rendezvous` | RendezVousServlet | PATIENT |
| `/patient/rdv/cancel` | PatientRendezVousCancelServlet | PATIENT |
| `/medecin/dashboard` | MedecinServlet | MEDECIN |
| `/medecin/consultation` | ConsultationServlet | MEDECIN |
| `/medecin/consultations` | ConsultationsServlet | MEDECIN |
| `/medecin/rendezvous` | MedecinRendezVousServlet | MEDECIN |
| `/medecin/patients` | MedecinPatientsServlet | MEDECIN |
| `/medecin/ordonnances` | MedecinOrdonnancesServlet | MEDECIN |
| `/medecin/analyses` | MedecinAnalysesServlet | MEDECIN |
| `/medecin/conges` | CongesServlet | MEDECIN |
| `/medecin/secretaire/add` | SecretaireServlet | MEDECIN |
| `/admin/dashboard` | AdminServlet | ADMIN |
| `/admin/patients/add` | AddPatientServlet | ADMIN |
| `/admin/patients/delete` | DeletePatientServlet | ADMIN |
| `/admin/medecins/delete` | DeleteMedecinServlet | ADMIN |
| `/admin/cabinets-medecins/add` | AddCabinetMedecinServlet | ADMIN |
| `/admin/cabinets/add` | AddCabinetServlet (dead) | ADMIN |
| `/secretaire/dashboard` | DashboardSecretaireServlet | SECRETAIRE |
| `/pharmacie/dashboard` | DashboardPharmacieServlet | PHARMACIE |

---

## Appendix B — Dependencies (`pom.xml`)

| Dependency | Version | Purpose |
|------------|---------|---------|
| jakarta.servlet-api | 6.0.0 | Servlet API (provided) |
| jakarta.servlet.jsp.jstl | 3.0.0 | JSP JSTL |
| mysql-connector-j | 9.6.0 | MySQL driver |
| jbcrypt | 0.4 | Password hashing |
| jakarta.mail | 2.0.1 | Email sending |

---

*End of document — Cabinet Medical Management Technical Documentation*
