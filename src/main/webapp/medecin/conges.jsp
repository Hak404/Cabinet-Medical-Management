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
    <title>Médecin - Congés</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css" rel="stylesheet">
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
    <link rel="stylesheet" href="<%= ctx %>/css/sidebar.css">
</head>
<body>
<jsp:include page="/shared/navbar.jsp">
    <jsp:param name="title" value="Congés"/>
    <jsp:param name="userLabel" value="Médecin"/>
</jsp:include>
<div class="app-shell">
    <jsp:include page="/shared/sidebar.jsp">
        <jsp:param name="type" value="medecin"/>
    </jsp:include>
    <main class="app-main">
        <div class="container-fluid px-0">
            <h4 class="mb-1 fw-bold">Congés médecin</h4>
            <div class="text-muted mb-3">Ajoutez vos jours non disponibles.</div>
            <c:if test="${not empty param.deleted}">
                <div class="alert alert-success border-0 shadow-sm">Jour supprimé.</div>
            </c:if>
            <div class="cm-card p-3">
                <form method="post" action="<%= ctx %>/medecin/conges">
                    <div class="row g-3 align-items-end">
                        <div class="col-md-4">
                            <label class="form-label fw-semibold">Ajouter un jour</label>
                            <input type="date" class="form-control" name="dateConge" required>
                        </div>
                        <div class="col-md-2">
                            <button type="submit" class="btn btn-primary w-100">Ajouter</button>
                        </div>
                    </div>
                </form>
                <hr>
                <h6 class="fw-semibold mb-2">Liste des congés</h6>
                <div class="table-responsive">
                    <table class="table table-sm align-middle">
                        <thead>
                        <tr>
                            <th>Date</th>
                            <th class="text-end">Action</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="conge" items="${conges}">
                            <tr>
                                <td>${conge.dateConge}</td>
                                <td class="text-end">
                                    <form method="post" action="<%= ctx %>/medecin/conges" style="display:inline;">
                                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"/>
                                        <input type="hidden" name="action" value="delete">
                                        <input type="hidden" name="id" value="${conge.id}">
                                        <button type="submit" class="btn btn-sm btn-outline-danger">Supprimer</button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty conges}">
                            <tr>
                                <td colspan="2" class="text-muted">Aucun jour de congé enregistré.</td>
                            </tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </main>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

