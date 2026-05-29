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
    <title>Médecin - Mes patients</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css" rel="stylesheet">
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
    <link rel="stylesheet" href="<%= ctx %>/css/sidebar.css">
</head>
<body>
<jsp:include page="/shared/navbar.jsp">
    <jsp:param name="title" value="Mes Patients"/>
    <jsp:param name="userLabel" value="Médecin"/>
</jsp:include>
<div class="app-shell">
    <jsp:include page="/shared/sidebar.jsp">
        <jsp:param name="type" value="medecin"/>
    </jsp:include>
    <main class="app-main">
        <div class="container-fluid px-0">
            <div class="cm-card p-3 mb-3">
                <form method="get" action="<%= ctx %>/medecin/patients" class="row g-2 align-items-end">
                    <div class="col-md-4">
                        <label class="form-label fw-semibold">Date</label>
                        <input type="date" class="form-control" name="selectedDate" value="${selectedDate}">
                    </div>
                    <div class="col-md-4">
                        <label class="form-label fw-semibold">Cabinet</label>
                        <input type="text" class="form-control" value="${currentCabinetId}" disabled>
                    </div>
                    <div class="col-md-2">
                        <button class="btn btn-primary w-100" type="submit">Appliquer</button>
                    </div>
                </form>
            </div>
            <div class="cm-card">
                <div class="card-header p-3 fw-bold">Patients du ${selectedDate}</div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0">
                            <thead><tr><th class="ps-3">Patient</th><th>CIN</th><th>Email</th></tr></thead>
                            <tbody>
                            <c:forEach items="${patients}" var="p">
                                <tr>
                                    <td class="ps-3">${p.nom} ${p.prenom}</td>
                                    <td>${p.cin}</td>
                                    <td>${p.email}</td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty patients}">
                                <tr><td class="ps-3 text-muted" colspan="3">Aucun patient aujourd’hui.</td></tr>
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

