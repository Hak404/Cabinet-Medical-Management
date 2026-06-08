<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!doctype html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Inscription Patient</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body>
<div class="container">
    <div class="card">
        <div class="header">
            <h2>Inscription (Patient)</h2>
            <a class="btn btn-secondary" href="${pageContext.request.contextPath}/login">Retour</a>
        </div>

        <%
            String error = request.getParameter("error");
            String success = request.getParameter("success");
        %>
        <%
		String errorMsg = "";
		
		if ("champ_manquant".equals(error)) {
		    errorMsg = "Veuillez remplir tous les champs obligatoires.";
		} else if ("date_invalide".equals(error)) {
		    errorMsg = "Date de naissance invalide.";
		} else if ("email_invalide".equals(error)) {
		    errorMsg = "Adresse email invalide.";
		} else if ("email_deja_utilise".equals(error)) {
		    errorMsg = "Cette adresse email est déjà utilisée.";
		} else if ("envoi_email".equals(error)) {
		    String detail = request.getParameter("detail");
		    if ("smtp_desactive".equals(detail)) {
		        errorMsg = "L’envoi d’email est désactivé (email.enabled=false). "
		            + "Créez src/main/resources/email.local.properties avec email.enabled=true "
		            + "et vos identifiants Gmail (mot de passe d’application), puis redémarrez Tomcat.";
		    } else if ("smtp_non_configure".equals(detail)) {
		        errorMsg = "Configuration SMTP incomplète. Renseignez mail.smtp.user, mail.smtp.password et mail.from "
		            + "dans email.local.properties (voir email.properties.example).";
		    } else if ("echec_smtp".equals(detail)) {
		        errorMsg = "Connexion SMTP refusée. Vérifiez l’adresse Gmail, le mot de passe d’application "
		            + "(pas le mot de passe du compte) et le port 587 avec STARTTLS.";
		    } else {
		        errorMsg = "L’envoi du code par email a échoué. Consultez les logs Tomcat (catalina.out) pour le détail.";
		    }
		} else {
		    errorMsg = error;
		}
		%>
        
        <%-- Hna l-tshih: khassna ntsikiw wach error kayn nit qbel ma n3tiw l-badge --%>
        <% if (error != null) { %>
        <div class="badge badge-danger" style="margin-bottom: 12px;">
            <%= errorMsg %>
        </div>
        <% } %>

        <% if (success != null) { %>
        <div class="badge badge-success" style="margin-bottom: 12px;">
            <%= success %>
        </div>
        <% } %>

        <form method="post" action="${pageContext.request.contextPath}/patient/register">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"/>
            <div class="row" style="grid-template-columns: repeat(2,minmax(0,1fr));">
                <div>
                    <label>Nom</label>
                    <input type="text" name="nom" required/>
                </div>
                <div>
                    <label>Prénom</label>
                    <input type="text" name="prenom" required/>
                </div>
            </div>

            <div class="row">
                <div>
                    <label>Téléphone</label>
                    <input type="tel" name="telephone" required/>
                </div>
                <div>
                    <label>Email</label>
                    <input type="email" name="email" required/>
                </div>
            </div>

            <div class="row">
                <div>
                    <label>CIN</label>
                    <input type="text" name="cin" required/>
                </div>
                <div>
                    <label>Date de naissance</label>
                    <input type="date" name="dateNaissance" required/>
                </div>
            </div>

            <div class="row">
                <div style="grid-column: span 2;">
                    <label>Adresse</label>
                    <textarea name="adresse" rows="3" required></textarea>
                </div>
            </div>

            <div class="row">
                <div style="grid-column: span 2;">
                    <label>Mot de passe</label>
                    <input type="password" name="password" required/>
                </div>
            </div>

            <div style="margin-top: 16px;">
                <button class="btn" type="submit">Créer mon compte</button>
            </div>
        </form>
    </div>
</div>
</body>
</html>