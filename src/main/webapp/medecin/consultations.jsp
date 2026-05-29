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
    <title>Médecin - Historique consultations</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css" rel="stylesheet">
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
    <link rel="stylesheet" href="<%= ctx %>/css/sidebar.css">
</head>
<body>
<jsp:include page="/shared/navbar.jsp">
    <jsp:param name="title" value="Consultations"/>
    <jsp:param name="userLabel" value="Médecin"/>
</jsp:include>

<div class="app-shell">
    <jsp:include page="/shared/sidebar.jsp">
        <jsp:param name="type" value="medecin"/>
    </jsp:include>
    <main class="app-main">
        <div class="container-fluid px-0">
            <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
                <div>
                    <h4 class="mb-1 fw-bold">Consultations</h4>
                    <div class="text-muted">Historique des consultations par date.</div>
                </div>
            </div>
            <div class="cm-card p-3 mb-3">
                <form method="get" action="<%= ctx %>/medecin/consultations" class="row g-2 align-items-end">
                    <div class="col-md-4">
                        <label class="form-label fw-semibold">Filtrer par date</label>
                        <input type="date" class="form-control" name="selectedDate" value="${selectedDate}" required>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label fw-semibold">Cabinet</label>
                        <input type="text" class="form-control" value="${currentCabinetId}" disabled>
                    </div>
                    <div class="col-md-2">
                        <button class="btn btn-primary w-100" type="submit"><i class="fa-solid fa-filter me-2"></i>Filtrer</button>
                    </div>
                </form>
            </div>
            <div class="cm-card">
                <div class="card-header p-3 fw-bold">Résultats du ${selectedDate}</div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0">
                            <thead>
                            <tr>
                                <th class="ps-3">Patient</th>
                                <th>Heure</th>
                                <th>Motif</th>
                                <th>Statut</th>
                                <th>Détail</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach items="${consultations}" var="c">
                                <tr>
                                    <td class="ps-3">${c.patientNomComplet}</td>
                                    <td>${c.heureConsultation}</td>
                                    <td>${c.motif}</td>
                                    <td>
                                        <span class="badge ${c.statut == 'TERMINE' ? 'bg-success-subtle text-success' : 'bg-warning-subtle text-warning'}">
                                                ${c.statut == 'TERMINE' ? 'Terminée' : 'En cours'}
                                        </span>
                                    </td>
                                    <td>
                                        <a class="btn btn-sm btn-outline-primary" href="<%= ctx %>/medecin/consultation?rdvId=${c.rendezVousId}&selectedDate=${selectedDate}">
                                            Voir
                                        </a>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty consultations}">
                                <tr><td class="ps-3 text-muted" colspan="5">Aucune consultation ce jour.</td></tr>
                            </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </main>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

