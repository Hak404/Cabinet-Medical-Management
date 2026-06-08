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
    <title>Admin - Dashboard</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css" rel="stylesheet">
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
    <link rel="stylesheet" href="<%= ctx %>/css/sidebar.css">
</head>
<body>
<jsp:include page="/shared/navbar.jsp">
    <jsp:param name="title" value="Admin Dashboard"/>
    <jsp:param name="userLabel" value="Admin"/>
</jsp:include>

<div class="app-shell">
    <jsp:include page="/shared/sidebar.jsp">
        <jsp:param name="type" value="admin"/>
    </jsp:include>

    <main class="app-main">
        <div class="container-fluid px-0">
            <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
                <div>
                    <h4 class="mb-1 fw-bold">Vue d’ensemble</h4>
                    <div class="text-muted">Suivi du système, utilisateurs, activité récente et actions rapides.</div>
                </div>
                <div class="d-flex flex-wrap gap-2">
                    <button class="btn btn-soft-primary" type="button" data-bs-toggle="modal" data-bs-target="#modalAddCabinet">
                        <i class="fa-solid fa-plus me-2"></i>Ajouter Cabinet
                    </button>
                    <button class="btn btn-outline-success" type="button" data-bs-toggle="modal" data-bs-target="#modalAddPatient">
                        <i class="fa-solid fa-user-injured me-2"></i>Ajouter Patient
                    </button>
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
                                <div class="text-muted small fw-semibold">Total utilisateurs</div>
                                <div class="stat-value mt-1">${totalDoctors + totalPatients}</div>
                                <div class="small text-muted mt-1">
                                    <i class="fa-solid fa-arrow-trend-up text-success me-1"></i>
                                    Données temps réel
                                </div>
                            </div>
                            <div class="stat-icon">
                                <i class="fa-solid fa-users"></i>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-12 col-md-6 col-xl-3">
                    <div class="stat-card p-3 h-100">
                        <div class="d-flex align-items-center justify-content-between">
                            <div>
                                <div class="text-muted small fw-semibold">Total médecins</div>
                                <div class="stat-value mt-1">${totalDoctors}</div>
                                <div class="small text-muted mt-1">Liste: ${medecins.size()}</div>
                            </div>
                            <div class="stat-icon info">
                                <i class="fa-solid fa-user-doctor"></i>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-12 col-md-6 col-xl-3">
                    <div class="stat-card p-3 h-100">
                        <div class="d-flex align-items-center justify-content-between">
                            <div>
                                <div class="text-muted small fw-semibold">Total patients</div>
                                <div class="stat-value mt-1">${totalPatients}</div>
                                <div class="small text-muted mt-1">Liste: ${patients.size()}</div>
                            </div>
                            <div class="stat-icon success">
                                <i class="fa-solid fa-user-injured"></i>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-12 col-md-6 col-xl-3">
                    <div class="stat-card p-3 h-100">
                        <div class="d-flex align-items-center justify-content-between">
                            <div>
                                <div class="text-muted small fw-semibold">RDV aujourd’hui</div>
                                <div class="stat-value mt-1">${appointmentsToday}</div>
                                <div class="small text-muted mt-1">Tous statuts</div>
                            </div>
                            <div class="stat-icon info">
                                <i class="fa-solid fa-calendar-check"></i>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="row g-3">
                <div class="col-12 col-xl-7">
                    <div class="cm-card">
                        <div class="card-header d-flex flex-wrap align-items-center justify-content-between gap-2 p-3">
                            <div>
                                <div class="fw-bold">Médecins</div>
                                <div class="small text-muted">Liste des médecins (table `medecin` + `user`).</div>
                            </div>
                        </div>
                        <div class="card-body p-0">
                            <div class="table-responsive">
                                <table class="table table-hover align-middle mb-0">
                                    <thead>
                                    <tr>
                                        <th class="ps-3">Nom</th>
                                        <th>Spécialité</th>
                                        <th>Cabinet</th>
                                        <th>Horaires</th>
                                        <th class="text-end pe-3">Actions</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <c:forEach items="${medecins}" var="m">
                                        <tr>
                                            <td class="ps-3">
                                                <div class="fw-semibold">Dr. ${m.nom} ${m.prenom}</div>
                                                <div class="text-muted small">${m.email}</div>
                                            </td>
                                            <td>${m.specialite}</td>
                                            <td>${m.cabinetNom}</td>
                                            <td>${m.heureDebut} - ${m.heureFin}</td>
                                            <td class="text-end pe-3">
                                                <form method="post" action="<%= ctx %>/admin/medecins/delete" class="d-inline" onsubmit="return confirm('Supprimer ce médecin ?');">
                                                    <input type="hidden" name="id" value="${m.id}">
                                                    <button class="btn btn-sm btn-outline-danger" type="submit" title="Supprimer">
                                                        <i class="fa-solid fa-trash"></i>
                                                    </button>
                                                </form>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty medecins}">
                                        <tr><td class="ps-3" colspan="5" class="text-muted">Aucun médecin.</td></tr>
                                    </c:if>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="col-12 col-xl-5">
                    <div class="cm-card h-100">
                        <div class="card-header p-3">
                            <div class="fw-bold">Patients</div>
                            <div class="small text-muted">Liste des patients (table `patient` + `user`).</div>
                        </div>
                        <div class="card-body p-0">
                            <div class="table-responsive">
                                <table class="table table-hover align-middle mb-0">
                                    <thead>
                                    <tr>
                                        <th class="ps-3">Nom</th>
                                        <th>CIN</th>
                                        <th class="text-end pe-3">Actions</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <c:forEach items="${patients}" var="p">
                                        <tr>
                                            <td class="ps-3">
                                                <div class="fw-semibold">${p.nom} ${p.prenom}</div>
                                                <div class="text-muted small">${p.email}</div>
                                            </td>
                                            <td>${p.cin}</td>
                                            <td class="text-end pe-3">
                                                <form method="post" action="<%= ctx %>/admin/patients/delete" class="d-inline" onsubmit="return confirm('Supprimer ce patient ?');">
                                                    <input type="hidden" name="id" value="${p.id}">
                                                    <button class="btn btn-sm btn-outline-danger" type="submit" title="Supprimer">
                                                        <i class="fa-solid fa-trash"></i>
                                                    </button>
                                                </form>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty patients}">
                                        <tr><td class="ps-3" colspan="3" class="text-muted">Aucun patient.</td></tr>
                                    </c:if>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Modals: Add Cabinet / Medecin / Patient -->
            <div class="modal fade" id="modalAddCabinet" tabindex="-1" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">Ajouter Cabinet + Médecin</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fermer"></button>
                        </div>
                        <form method="post" action="<%= ctx %>/admin/cabinets-medecins/add">
                            <div class="modal-body">
                                <div class="row g-3">
                                    <div class="col-12">
                                        <div class="fw-semibold mb-1">Cabinet</div>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label fw-semibold">Nom du cabinet</label>
                                        <input class="form-control" name="cabinetNom" required>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label fw-semibold">Durée consultation (minutes)</label>
                                        <input class="form-control" name="dureeConsultation" type="number" min="5" value="30" required>
                                    </div>
                                    <div class="col-12">
                                        <label class="form-label fw-semibold">Adresse</label>
                                        <input class="form-control" name="cabinetAdresse">
                                    </div>

                                    <div class="col-12 mt-2">
                                        <div class="fw-semibold mb-1">Médecin (unique pour ce cabinet)</div>
                                    </div>
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
                                        <label class="form-label fw-semibold">Spécialité</label>
                                        <input class="form-control" name="specialite" required>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label fw-semibold">Jours de congé</label>
                                        <input class="form-control" name="joursConge" placeholder="Ex: SAMEDI,DIMANCHE">
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label fw-semibold">Heure début</label>
                                        <input class="form-control" name="heureDebut" type="time" value="09:00" required>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label fw-semibold">Heure fin</label>
                                        <input class="form-control" name="heureFin" type="time" value="17:00" required>
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

            <div class="modal fade" id="modalAddPatient" tabindex="-1" aria-hidden="true">
                <div class="modal-dialog modal-lg modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">Ajouter un patient</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fermer"></button>
                        </div>
                        <form method="post" action="<%= ctx %>/admin/patients/add">
                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"/>
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
                                        <label class="form-label fw-semibold">CIN</label>
                                        <input class="form-control" name="cin" required>
                                    </div>
                                    <div class="col-md-8">
                                        <label class="form-label fw-semibold">Adresse</label>
                                        <input class="form-control" name="adresse" required>
                                    </div>
                                    <div class="col-md-4">
                                        <label class="form-label fw-semibold">Date naissance</label>
                                        <input class="form-control" name="dateNaissance" type="date" required>
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
        </div>
    </main>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

