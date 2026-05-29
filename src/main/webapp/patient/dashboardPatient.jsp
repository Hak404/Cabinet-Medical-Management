<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard Patient</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body>
<div class="container">
    <div class="card">
        <div class="header">
            <h2>Dashboard Patient</h2>
            <div style="display:flex; gap:10px; align-items:center;">
                <a class="btn btn-secondary" href="${pageContext.request.contextPath}/patient/rendezvous">Prendre RDV</a>
                <a class="btn btn-secondary" href="${pageContext.request.contextPath}/patient/documents">Mes documents médicaux</a>
                <a class="btn btn-secondary" href="${pageContext.request.contextPath}/logout">Déconnexion</a>
            </div>
        </div>

        <div class="grid" style="grid-template-columns: repeat(3, minmax(0,1fr));">
            <div class="badge">RDV aujourd'hui: <strong>${rdvToday}</strong></div>
            <div class="badge">RDV en attente: <strong>${rdvEnAttente}</strong></div>
            <div class="badge">Total RDV: <strong>${rendezVousList.size()}</strong></div>
        </div>

        <c:if test="${not empty flashError}">
            <div class="alert alert-error" style="margin-top: 14px; padding: 10px 14px; background: #fde8e8; color: #9b1c1c; border-radius: 6px;">
                ${flashError}
            </div>
        </c:if>
        <c:if test="${not empty flashSuccess}">
            <div class="alert alert-success" style="margin-top: 14px; padding: 10px 14px; background: #e6f6ea; color: #1e6b3a; border-radius: 6px;">
                ${flashSuccess}
            </div>
        </c:if>

        <div style="margin-top: 18px;">
            <h3>Vos rendez-vous</h3>
            <table class="table">
                <thead>
                <tr>
                    <th>Date</th>
                    <th>Heure</th>
                    <th>Statut</th>
                    <th>Médecin</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="r" items="${rendezVousList}">
                    <tr>
                        <td>${r.dateRendezVous}</td>
                        <td>${r.startTime} - ${r.endTime}</td>
                        <td><span class="badge">${r.statut}</span></td>
                        <td>${r.medecinNomComplet}</td>
                        <td>
                            <c:choose>
                                <c:when test="${r.patientCancellationAllowed}">
                                    <form method="post" action="${pageContext.request.contextPath}/patient/rdv/cancel"
                                          onsubmit="return confirm('Confirmer l\u2019annulation de ce rendez-vous ?');">
                                        <input type="hidden" name="id" value="${r.id}">
                                        <button type="submit" class="btn btn-secondary" style="font-size: 0.85rem;">
                                            Annuler RDV
                                        </button>
                                    </form>
                                </c:when>
                                <c:when test="${r.patientCancelActionVisible}">
                                    <button type="button" class="btn btn-secondary" disabled
                                            style="font-size: 0.85rem; opacity: 0.55; cursor: not-allowed;"
                                            title="${r.patientCancellationBlockReason}">
                                        Annuler RDV
                                    </button>
                                </c:when>
                                <c:otherwise>—</c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty rendezVousList}">
                    <tr>
                        <td colspan="5">Aucun rendez-vous pour le moment.</td>
                    </tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>
</body>
</html>

