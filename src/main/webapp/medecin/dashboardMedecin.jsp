<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    String ctx = request.getContextPath();
%>
<!doctype html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Médecin - Dashboard</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css" rel="stylesheet">
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
    <link rel="stylesheet" href="<%= ctx %>/css/sidebar.css">
</head>
<body>
<jsp:include page="/shared/navbar.jsp">
    <jsp:param name="title" value="Médecin Dashboard"/>
    <jsp:param name="userLabel" value="Dr. A. Benali"/>
</jsp:include>

<div class="app-shell">
    <jsp:include page="/shared/sidebar.jsp">
        <jsp:param name="type" value="medecin"/>
    </jsp:include>

    <main class="app-main">
        <div class="container-fluid px-0">
            <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
                <div>
                    <h4 class="mb-1 fw-bold">Bonjour, Docteur</h4>
                    <div class="text-muted">Suivi des consultations, rendez-vous et patients de la journée.</div>
                </div>
                <div class="d-flex flex-wrap gap-2">
                    <button class="btn btn-outline-primary" type="button" data-bs-toggle="modal" data-bs-target="#modalAddSecretaire">
                        <i class="fa-solid fa-user-tie me-2"></i>Ajouter Secrétaire
                    </button>
                    <c:choose>
                        <c:when test="${not empty nextRdv}">
                            <a class="btn btn-soft-primary" href="<%= ctx %>/medecin/consultation?rdvId=${nextRdv.id}&selectedDate=${selectedDate}">
                                <i class="fa-solid fa-stethoscope me-2"></i>Démarrer consultation
                            </a>
                        </c:when>
                        <c:otherwise>
                            <button class="btn btn-soft-primary" type="button" disabled>
                                <i class="fa-solid fa-stethoscope me-2"></i>Aucune consultation
                            </button>
                        </c:otherwise>
                    </c:choose>
                    <a class="btn btn-outline-primary" href="<%= ctx %>/medecin/rendezvous?selectedDate=${selectedDate}">
                        <i class="fa-solid fa-calendar-plus me-2"></i>Nouveau RDV
                    </a>
                </div>
            </div>
            <c:if test="${not empty param.success}">
                <div class="alert alert-success border-0 shadow-sm">Opération réussie: ${param.success}</div>
            </c:if>
            <c:if test="${not empty param.error}">
                <div class="alert alert-danger border-0 shadow-sm">Erreur: ${param.error}</div>
            </c:if>

            <div class="row g-3 mb-3">
                <div class="col-12 col-md-6 col-xl-3">
                    <div class="stat-card p-3 h-100">
                        <div class="d-flex align-items-center justify-content-between">
                            <div>
                                <div class="text-muted small fw-semibold">Patients aujourd’hui</div>
                                <div class="stat-value mt-1">${todayPatientsCount}</div>
                                <div class="small text-muted mt-1">Planning chargé</div>
                            </div>
                            <div class="stat-icon info">
                                <i class="fa-solid fa-users"></i>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-12 col-md-6 col-xl-3">
                    <div class="stat-card p-3 h-100">
                        <div class="d-flex align-items-center justify-content-between">
                            <div>
                                <div class="text-muted small fw-semibold">Consultations complétées</div>
                                <div class="stat-value mt-1">${completedConsultations}</div>
                                <div class="small text-muted mt-1">Aujourd’hui</div>
                            </div>
                            <div class="stat-icon success">
                                <i class="fa-solid fa-circle-check"></i>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-12 col-md-6 col-xl-3">
                    <div class="stat-card p-3 h-100">
                        <div class="d-flex align-items-center justify-content-between">
                            <div>
                                <div class="text-muted small fw-semibold">Patients restants</div>
                                <div class="stat-value mt-1">${remainingPatients}</div>
                                <div class="small text-muted mt-1">À traiter</div>
                            </div>
                            <div class="stat-icon">
                                <i class="fa-solid fa-hourglass-half"></i>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-12 col-md-6 col-xl-3">
                    <div class="stat-card p-3 h-100">
                        <div class="d-flex align-items-center justify-content-between">
                            <div>
                                <div class="text-muted small fw-semibold">Prochain RDV</div>
                                <div class="stat-value mt-1">
                                    <c:choose>
                                        <c:when test="${not empty nextRdv}">${nextRdv.startTime}</c:when>
                                        <c:otherwise>—</c:otherwise>
                                    </c:choose>
                                </div>
                                <div class="small text-muted mt-1">
                                    <c:choose>
                                        <c:when test="${not empty nextRdv}">${nextRdv.patientNomComplet}</c:when>
                                        <c:otherwise>Aucun</c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                            <div class="stat-icon info">
                                <i class="fa-solid fa-clock"></i>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="row g-3 mb-3">
                <div class="col-12 col-xl-7">
                    <div class="cm-card h-100">
                        <div class="card-header p-3 d-flex flex-wrap align-items-center justify-content-between gap-2">
                            <div>
                                <div class="fw-bold">Calendrier hebdomadaire</div>
                                <div class="small text-muted">Vue simple des créneaux principaux.</div>
                            </div>
                            <div class="d-flex gap-2">
                                <button class="btn btn-sm btn-light border"><i class="fa-solid fa-chevron-left"></i></button>
                                <button class="btn btn-sm btn-light border"><i class="fa-solid fa-chevron-right"></i></button>
                            </div>
                        </div>
                        <div class="card-body p-3">
                            <div class="calendar-grid">
                                <c:forEach items="${weekDays}" var="d">
                                    <div class="calendar-day ${d.repos ? 'bg-danger-subtle border border-danger-subtle cm-day-disabled' : 'cm-day-clickable'} ${d.selected ? 'border border-primary' : ''}"
                                         data-date="${d.date}">
                                        <div class="day-name">${d.dayLabel}</div>
                                        <div class="slot">${d.rendezVousCount} RDV</div>
                                        <div class="slot">${d.consultationCount} consultations</div>
                                        <c:if test="${d.repos}">
                                            <div class="slot fw-semibold text-danger">Repos</div>
                                        </c:if>
                                    </div>
                                </c:forEach>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="col-12 col-xl-5">
                    <div class="cm-card h-100">
                        <div class="card-header p-3">
                            <div class="fw-bold">Détails du prochain rendez-vous</div>
                            <div class="small text-muted">Informations rapides patient et motif.</div>
                        </div>
                        <div class="card-body p-3">
                            <div class="d-flex align-items-start gap-3">
                                <div class="stat-icon info" style="width:46px;height:46px;border-radius:18px;">
                                    <i class="fa-solid fa-user-injured"></i>
                                </div>
                                <div class="flex-grow-1">
                                    <div class="fw-bold">
                                        <c:choose>
                                            <c:when test="${not empty nextRdv}">${nextRdv.patientNomComplet}</c:when>
                                            <c:otherwise>Aucun rendez-vous</c:otherwise>
                                        </c:choose>
                                    </div>
                                    <div class="text-muted small">
                                        <c:if test="${not empty nextRdv}">Patient ID: ${nextRdv.patientId}</c:if>
                                    </div>
                                    <div class="mt-2">
                                        <c:if test="${not empty nextRdv}">
                                            <span class="badge-soft info"><i class="fa-solid fa-clock me-1"></i>${nextRdv.dateRendezVous} ${nextRdv.startTime}</span>
                                            <span class="badge-soft success ms-2"><i class="fa-solid fa-notes-medical me-1"></i>${nextRdv.statut}</span>
                                        </c:if>
                                    </div>
                                </div>
                            </div>
                            <hr class="my-3">
                            <div class="small text-muted mb-2">Cabinet</div>
                            <div class="fw-semibold">
                                <c:choose>
                                    <c:when test="${not empty nextRdv}">${nextRdv.cabinetNom}</c:when>
                                    <c:otherwise>—</c:otherwise>
                                </c:choose>
                            </div>
                            <div class="d-flex flex-wrap gap-2 mt-3">
                                <c:if test="${not empty nextRdv}">
                                    <a class="btn btn-soft-primary" href="<%= ctx %>/medecin/consultation?rdvId=${nextRdv.id}&selectedDate=${selectedDate}">
                                        <i class="fa-solid fa-stethoscope me-2"></i>Ouvrir consultation
                                    </a>
                                </c:if>
                                <a class="btn btn-outline-primary" href="<%= ctx %>/medecin/consultations?selectedDate=${selectedDate}">
                                    <i class="fa-solid fa-file-waveform me-2"></i>Historique
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="row g-3 mb-3">
                <div class="col-12">
                    <div class="cm-card">
                        <div class="card-header p-3 d-flex flex-wrap align-items-center justify-content-between gap-2">
                            <div>
                                <div class="fw-bold">Liste patients (aujourd’hui)</div>
                                <div class="small text-muted">Accès rapide pour consulter, éditer, voir les détails.</div>
                            </div>
                            <div class="d-flex gap-2">
                                <button class="btn btn-sm btn-outline-primary"><i class="fa-solid fa-filter me-2"></i>Filtrer</button>
                                <button class="btn btn-sm btn-outline-primary"><i class="fa-solid fa-file-export me-2"></i>Exporter</button>
                            </div>
                        </div>
                        <div class="card-body p-0">
                            <div class="table-responsive">
                                <table class="table table-hover align-middle mb-0">
                                    <thead>
                                    <tr>
                                        <th class="ps-3">Patient</th>
                                        <th>Heure</th>
                                        <th>Motif</th>
                                        <th>Statut</th>
                                        <th class="text-end pe-3">Actions</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <c:forEach items="${todayRdvList}" var="r">
                                        <tr>
                                            <td class="ps-3">
                                                <div class="fw-semibold">${r.patientNomComplet}</div>
                                                <div class="text-muted small">Patient ID: ${r.patientId}</div>
                                            </td>
                                            <td>${r.startTime} - ${r.endTime}</td>
                                            <td>${r.cabinetNom}</td>
                                            <td><span class="badge-soft info">${r.statut}</span></td>
                                            <td class="text-end pe-3">
                                                <a class="btn btn-sm btn-primary" href="<%= ctx %>/medecin/consultation?rdvId=${r.id}&selectedDate=${selectedDate}" title="Consultation">
                                                    <i class="fa-solid fa-stethoscope"></i>
                                                </a>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty todayRdvList}">
                                        <tr>
                                            <td class="ps-3 text-muted" colspan="5">Aucun rendez-vous aujourd’hui.</td>
                                        </tr>
                                    </c:if>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

        </div>
    </main>
</div>

<div class="modal fade" id="modalAddSecretaire" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Ajouter une secrétaire</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fermer"></button>
            </div>
            <form method="post" action="<%= ctx %>/medecin/secretaire/add">
                <div class="modal-body">
                    <div class="row g-3">
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Nom</label>
                            <input class="form-control" name="nom" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Prénom</label>
                            <input class="form-control" name="prenom" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Email</label>
                            <input class="form-control" name="email" type="email" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Téléphone</label>
                            <input class="form-control" name="telephone" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Mot de passe</label>
                            <input class="form-control" name="password" type="password" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Bureau (optionnel)</label>
                            <input class="form-control" name="bureau">
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-light border" type="button" data-bs-dismiss="modal">Annuler</button>
                    <button class="btn btn-primary" type="submit"><i class="fa-solid fa-plus me-2"></i>Ajouter</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    (() => {
        document.querySelectorAll('.cm-day-clickable[data-date]').forEach((el) => {
            el.style.cursor = 'pointer';
            el.addEventListener('click', () => {
                const date = el.getAttribute('data-date');
                if (!date) return;
                const url = new URL(window.location.href);
                url.searchParams.set('selectedDate', date);
                window.location.href = url.toString();
            });
        });
    })();
</script>
</body>
</html>

