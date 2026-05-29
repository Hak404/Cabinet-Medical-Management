<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mes documents médicaux</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body>
<div class="container">
    <div class="card">
        <div class="header">
            <h2>Mes documents médicaux</h2>
            <div style="display:flex; gap:10px;">
                <a class="btn btn-secondary" href="${pageContext.request.contextPath}/patient/dashboard">Tableau de bord</a>
                <a class="btn btn-secondary" href="${pageContext.request.contextPath}/logout">Déconnexion</a>
            </div>
        </div>

        <p class="text-muted" style="margin-top:0;">
            Ordonnances, demandes d'analyses et comptes rendus générés par votre médecin après consultation.
        </p>

        <c:choose>
            <c:when test="${empty documents}">
                <div class="badge" style="margin-top:12px;">Aucun document disponible pour le moment.</div>
            </c:when>
            <c:otherwise>
                <table class="table" style="margin-top:16px;">
                    <thead>
                    <tr>
                        <th>Type</th>
                        <th>Titre</th>
                        <th>Date</th>
                        <th>Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="doc" items="${documents}">
                        <tr>
                            <td>${doc.typeDocumentLabel}</td>
                            <td>${doc.titre}</td>
                            <td>${doc.dateCreation}</td>
                            <td style="white-space:nowrap;">
                                <a class="btn btn-secondary" style="font-size:0.85rem; margin-right:6px;"
                                   href="${pageContext.request.contextPath}/patient/documents/file?id=${doc.id}"
                                   target="_blank" rel="noopener">Voir</a>
                                <a class="btn btn-secondary" style="font-size:0.85rem;"
                                   href="${pageContext.request.contextPath}/patient/documents/file?id=${doc.id}&amp;download=1">
                                    Télécharger
                                </a>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>
</div>
</body>
</html>
