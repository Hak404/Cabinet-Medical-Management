<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Prendre un Rendez-vous</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
    <style>
        .readonly-select {
            background-color: #f4f4f4;
            pointer-events: none;
            touch-action: none;
        }
    </style>
</head>
<body>
<div class="container">
    <div class="card">
        <div class="header">
            <h2>Prendre un rendez-vous</h2>
            <div style="display:flex; gap:10px; align-items:center;">
                <a class="btn btn-secondary" href="${pageContext.request.contextPath}/patient/dashboard">Retour dashboard</a>
            </div>
        </div>

        <!-- ✅ Message d'erreur -->
        <c:if test="${not empty param.error}">
            <div style="margin-bottom: 12px; padding:10px; background:#f8d7da; color:#721c24; border-radius:5px; text-align:center; position:relative;">
                <strong>Erreur :</strong> ${param.error}
                <button type="button"
                        onclick="this.parentElement.style.display='none';"
                        style="position:absolute; right:10px; top:5px; border:none; background:none; font-size:18px; cursor:pointer;">
                    &times;
                </button>
            </div>
        </c:if>
        
        <c:if test="${not empty infoMsg}">
		    <div style="margin-bottom: 12px; padding:10px; background:#fff3cd; color:#856404; border-radius:5px; text-align:center;">
		        <strong>Information :</strong> ${infoMsg}
		    </div>
		</c:if>

        <form method="get" action="${pageContext.request.contextPath}/patient/rendezvous" id="appointmentForm">
            <div class="row">
                <div>
                    <label>Cabinet</label>
                    <select name="cabinetId" id="cabinetSelect" required onchange="syncMedecin()">
                        <option value="">-- Choisir --</option>
                        <c:forEach var="c" items="${cabinets}">
                            <option value="${c.id}" data-medecin="${c.medecinId}" ${c.id == selectedCabinetId ? 'selected' : ''}>
                                ${c.nom}
                            </option>
                        </c:forEach>
                    </select>
                </div>

                <div>
                    <label>Médecin</label>
                    <select name="medecinId" id="medecinSelect" required class="readonly-select">
                        <option value="">-- Choisir --</option>
                        <c:forEach var="m" items="${medecins}">
                            <option value="${m.id}" ${m.id == selectedMedecinId ? 'selected' : ''}>
                                Dr. ${m.nom} ${m.prenom} (${m.specialite})
                            </option>
                        </c:forEach>
                    </select>
                </div>
            </div>

            <div class="row">
                <div style="grid-column: span 2;">
                    <label>Date</label>
                    <input type="date" name="date" value="${date}" required/>
                </div>
            </div>

            <div style="margin-top: 14px;">
                <button class="btn" type="submit">Afficher créneaux</button>
            </div>
        </form>

        <div style="margin-top: 22px;">
            <h3>Créneaux disponibles</h3>

            <c:choose>
                <c:when test="${empty slots}">
                    <div class="badge">Aucun créneau (médecin en congé ou paramètres incomplets).</div>
                </c:when>
                <c:otherwise>
                    <table class="table">
                        <thead>
                        <tr>
                            <th>Début</th>
                            <th>Fin</th>
                            <th>Statut</th>
                            <th>Action</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="s" items="${slots}">
                            <tr>
                                <td>${s.startTime}</td>
                                <td>${s.endTime}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${s.available}">
                                            <span class="badge badge-success">Disponible</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge badge-danger">Indisponible</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${s.available}">
                                            <form method="post" action="${pageContext.request.contextPath}/patient/rendezvous" style="margin:0;">
                                                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"/>
                                                <input type="hidden" name="cabinetId" value="${selectedCabinetId}"/>
                                                <input type="hidden" name="medecinId" value="${selectedMedecinId}"/>
                                                <input type="hidden" name="date" value="${date}"/>
                                                <input type="hidden" name="startTime" value="${s.startTime}"/>
                                                <button class="btn" type="submit">Réserver</button>
                                            </form>
                                        </c:when>
                                        <c:otherwise>
                                            <button class="btn btn-secondary" disabled style="background-color: #ccc; cursor: not-allowed;">
                                                Réserver
                                            </button>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<script>
function syncMedecin() {
    var cabinetSelect = document.getElementById('cabinetSelect');
    var medecinSelect = document.getElementById('medecinSelect');
    var selectedOption = cabinetSelect.options[cabinetSelect.selectedIndex];
    var medecinId = selectedOption.getAttribute('data-medecin');

    if (medecinId) {
        medecinSelect.value = medecinId;
    } else {
        medecinSelect.value = "";
    }
}

window.onload = function() {
    if (document.getElementById('cabinetSelect').value !== "") {
        syncMedecin();
    }
};
</script>
</body>
</html>