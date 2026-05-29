<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!doctype html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Vérification du rendez-vous</title>
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
        .btn-verify {
            padding: 12px;
            font-weight: 600;
            border-radius: 12px;
        }
    </style>
</head>
<body>
<div class="container d-flex justify-content-center align-items-center min-vh-100 py-4">
    <div class="card p-5 verification-card text-center" style="max-width: 480px;">
        <div class="mb-4">
            <div class="bg-primary bg-opacity-10 p-3 rounded-circle d-inline-block">
                <i class="fa-solid fa-envelope fa-2x text-primary"></i>
            </div>
        </div>

        <h2 class="fw-bold mb-2">Vérification du rendez-vous</h2>
        <p class="text-muted mb-4">
            Un code à 6 chiffres a été envoyé à votre adresse email.
            Saisissez-le pour <strong>confirmer</strong> votre rendez-vous.
        </p>

        <% if ("1".equals(request.getParameter("resent"))) { %>
        <div class="alert alert-success py-2 small mb-4">
            Un nouveau code a été envoyé par email.
        </div>
        <% } %>

        <% if ("envoi_email".equals(request.getParameter("warning"))) { %>
        <div class="alert alert-warning py-2 small mb-4 text-start">
            L'email n'a pas pu être envoyé. Saisissez le code si vous l'avez reçu,
            ou utilisez « Renvoyer le code ». En développement, consultez les logs serveur.
        </div>
        <% } %>

        <% if ("expired".equals(request.getParameter("error"))) { %>
        <div class="alert alert-danger py-2 small mb-4">
            Le code a expiré (15 minutes). Cliquez sur « Renvoyer le code » pour en obtenir un nouveau.
        </div>
        <% } else if (request.getParameter("error") != null) { %>
        <div class="alert alert-danger py-2 small mb-4">
            Code incorrect, veuillez réessayer.
        </div>
        <% } %>

        <form action="${pageContext.request.contextPath}/patient/confirm-otp" method="post" class="mb-3">
            <div class="mb-4">
                <label class="form-label small fw-bold text-uppercase">Code reçu par email</label>
                <input type="text" name="otp_input" class="form-control otp-field"
                       placeholder="000000" maxlength="6" pattern="[0-9]{6}" required
                       autocomplete="one-time-code" inputmode="numeric"/>
            </div>
            <button type="submit" class="btn btn-primary w-100 btn-verify">
                Confirmer mon rendez-vous
            </button>
        </form>

        <form action="${pageContext.request.contextPath}/patient/rendezvous/resend-otp" method="post" class="mb-3">
            <button type="submit" class="btn btn-outline-secondary w-100">
                Renvoyer le code
            </button>
        </form>

        <div class="mt-3 small">
            <a href="${pageContext.request.contextPath}/patient/rendezvous" class="text-decoration-none">
                Annuler et retour aux créneaux
            </a>
        </div>
    </div>
</div>
<link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css" rel="stylesheet">
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
