<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!doctype html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Erreur</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body>
<div class="container">
    <div class="card">
        <h2>Une erreur est survenue</h2>
        <p>
            <% String error = request.getParameter("error"); %>
            <span class="badge badge-danger"><%= (error == null) ? "Erreur inconnue" : error %></span>
        </p>
        <p>
            <a class="btn" href="${pageContext.request.contextPath}/login">Retour à la connexion</a>
        </p>
    </div>
</div>
</body>
</html>

