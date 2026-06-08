<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String ctx = request.getContextPath();
    String error = request.getParameter("error");
    String success = request.getParameter("success");
    String warning = request.getParameter("warning");
    String timeout = request.getParameter("timeout");

    if ("1".equals(timeout)) {
        error = "session_expire";
    }

    String errorMessage = null;
    if (error != null) {
        switch (error) {
            case "identifiants_incorrects":
                errorMessage = "Email ou mot de passe incorrect.";
                break;
            case "session_expire":
                errorMessage = "Votre session a expiré. Veuillez vous reconnecter.";
                break;
            case "forbidden":
                errorMessage = "Accès refusé pour votre profil.";
                break;
            case "role_invalide":
                errorMessage = "Rôle utilisateur non reconnu.";
                break;
            default:
                errorMessage = error;
        }
    }

    String successMessage = null;
    if (success != null) {
        switch (success) {
            case "deconnexion":
                successMessage = "Vous êtes déconnecté.";
                break;
            case "inscription_ok":
                successMessage = "Inscription réussie. Connectez-vous avec votre compte.";
                break;
            default:
                successMessage = success;
        }
    }

    String warningMessage = null;
    if ("envoi_email".equals(warning)) {
        String detail = request.getParameter("detail");
        if ("smtp_desactive".equals(detail)) {
            warningMessage = "Votre compte a été créé, mais l'envoi d'email est désactivé (email.enabled=false). "
                + "Activez SMTP dans email.local.properties puis redémarrez Tomcat.";
        } else if ("smtp_non_configure".equals(detail)) {
            warningMessage = "Votre compte a été créé, mais la configuration SMTP est incomplète. "
                + "Renseignez mail.smtp.user, mail.smtp.password et mail.from dans email.local.properties.";
        } else if ("echec_smtp".equals(detail)) {
            warningMessage = "Votre compte a été créé, mais l'email de confirmation n'a pas pu être envoyé. "
                + "Vérifiez la configuration Gmail (mot de passe d'application, port 587).";
        } else {
            warningMessage = "Votre compte a été créé, mais l'email de confirmation n'a pas pu être envoyé.";
        }
    }
%>
<!doctype html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Connexion - Cabinet Medical</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css" rel="stylesheet">
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body class="d-flex align-items-center" style="min-height:100vh;">
<div class="container">
    <div class="row justify-content-center">
        <div class="col-12 col-md-9 col-lg-5">
            <div class="cm-card p-4">
                <div class="d-flex align-items-center gap-3 mb-3">
                    <span class="brand-mark">
                        <i class="fa-solid fa-heart-pulse"></i>
                    </span>
                    <div>
                        <h4 class="mb-0 fw-bold">Connexion</h4>
                        <div class="text-muted small">Session active jusqu'à déconnexion ou expiration (30 min)</div>
                    </div>
                </div>

                <% if (errorMessage != null) { %>
                    <div class="alert alert-danger border-0 shadow-sm"><%= errorMessage %></div>
                <% } %>
                <% if (successMessage != null) { %>
                    <div class="alert alert-success border-0 shadow-sm"><%= successMessage %></div>
                <% } %>
                <% if (warningMessage != null) { %>
                    <div class="alert alert-warning border-0 shadow-sm"><%= warningMessage %></div>
                <% } %>

                <form method="post" action="<%= ctx %>/login" class="row g-3">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"/>
                    <div class="col-12">
                        <label class="form-label fw-semibold">Email</label>
                        <input class="form-control" type="email" name="email" required>
                    </div>
                    <div class="col-12">
                        <label class="form-label fw-semibold">Mot de passe</label>
                        <input class="form-control" type="password" name="password" required>
                    </div>
                    <div class="col-12 d-grid">
                        <button class="btn btn-primary" type="submit">
                            <i class="fa-solid fa-right-to-bracket me-2"></i>Se connecter
                        </button>
                    </div>
                    <div class="col-12 text-center small text-muted">
                        Patient ? <a href="<%= ctx %>/patient/register">Créer un compte</a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

