<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!doctype html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Vérification email – Inscription</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body { background-color: #f8f9fa; }
        .verification-card {
            border: none;
            border-radius: 20px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
        }
        .otp-field {
            letter-spacing: 10px;
            font-size: 2rem;
            font-weight: bold;
            text-align: center;
            border-radius: 12px;
            border: 2px solid #dee2e6;
        }
        .otp-field:focus {
            border-color: #0d6efd;
            box-shadow: none;
        }
    </style>
</head>
<body>
<div class="container d-flex justify-content-center align-items-center min-vh-100 py-4">
    <div class="card p-5 verification-card text-center" style="max-width: 450px;">
        <h2 class="fw-bold mb-2">Vérification de l’email</h2>
        <p class="text-muted mb-4">Entrez le code à 6 chiffres envoyé à votre adresse pour finaliser l’inscription.</p>

        <% if ("envoi_email".equals(request.getParameter("warning"))) { %>
        <div class="alert alert-warning py-2 small mb-4 text-start">
            L’email de vérification n’a pas pu être envoyé. Vous pouvez tout de même saisir le code si vous l’avez reçu,
            ou réessayer plus tard. En développement, consultez les logs serveur (<code>[OTP-DEV]</code>).
        </div>
        <% } %>

        <form action="${pageContext.request.contextPath}/patient/confirm-otp" method="post">
            <div class="mb-4">
                <label class="form-label small fw-bold text-uppercase">Code reçu</label>
                <input type="text" name="otp_input" class="form-control otp-field"
                       placeholder="000000" maxlength="6" required autocomplete="one-time-code"/>
            </div>
            <% if (request.getParameter("error") != null) { %>
            <div class="alert alert-danger py-2 small mb-4">Code incorrect, veuillez réessayer.</div>
            <% } %>
            <button type="submit" class="btn btn-primary w-100 py-2 fw-semibold rounded-3">Confirmer mon inscription</button>
        </form>
        <div class="mt-4 small">
            <a href="${pageContext.request.contextPath}/patient/register" class="text-decoration-none">Retour au formulaire</a>
        </div>
    </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
