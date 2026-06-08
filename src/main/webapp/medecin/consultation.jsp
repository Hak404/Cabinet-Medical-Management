<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<!doctype html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Médecin - Consultation</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/sidebar.css">
</head>
<body>
<jsp:include page="/shared/navbar.jsp">
    <jsp:param name="title" value="Consultation"/>
</jsp:include>

<div class="app-shell">
    <jsp:include page="/shared/sidebar.jsp">
        <jsp:param name="type" value="medecin"/>
    </jsp:include>

    <main class="app-main">
        <div class="container-fluid px-0">
            <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
                <div>
                    <h4 class="mb-1 fw-bold">Consultation</h4>
                    <div class="text-muted">
                        RDV #<span class="fw-semibold"><c:out value="${rdv.id}"/></span> • <c:out value="${rdv.dateRendezVous}"/> • <c:out value="${rdv.startTime}"/> – <c:out value="${rdv.endTime}"/>
                    </div>
                </div>
                <div class="d-flex flex-wrap gap-2">
                    <a class="btn btn-outline-primary" href="${pageContext.request.contextPath}/medecin/dashboard">
                        <i class="fa-solid fa-arrow-left me-2"></i>Retour
                    </a>
                </div>
            </div>

            <c:if test="${not empty param.success}">
                <div class="alert alert-success border-0 shadow-sm">Consultation et pièces associées enregistrées.</div>
            </c:if>
            <c:if test="${not empty param.error}">
                <div class="alert alert-danger border-0 shadow-sm">Erreur : <c:out value="${param.error}"/></div>
            </c:if>
            <c:if test="${param.docSuccess == '1'}">
                <div class="alert alert-success border-0 shadow-sm">
                    <c:out value="${param.docCount}"/> document(s) PDF généré(s) et enregistré(s).
                    <c:if test="${param.docEmailWarning == '1'}">
                        L'email au patient n'a pas pu être envoyé (vérifiez la configuration SMTP).
                    </c:if>
                </div>
            </c:if>
            <c:if test="${not empty param.docError}">
                <div class="alert alert-warning border-0 shadow-sm"><c:out value="${param.docError}"/></div>
            </c:if>

            <div class="row g-3">
                <div class="col-12 col-lg-4">
                    <div class="cm-card p-3">
                        <div class="d-flex align-items-center gap-3">
                            <div class="stat-icon info">
                                <i class="fa-solid fa-user-injured"></i>
                            </div>
                            <div>
                                <div class="fw-bold"><c:out value="${rdv.patientNomComplet}"/></div>
                                <div class="text-muted small">Patient ID: <c:out value="${rdv.patientId}"/></div>
                            </div>
                        </div>
                        <hr class="my-3">
                        <div class="small text-muted mb-1">Cabinet</div>
                        <div class="fw-semibold"><c:out value="${rdv.cabinetNom}"/></div>
                        <div class="small text-muted mt-3 mb-1">Statut RDV</div>
                        <span class="badge-soft info"><c:out value="${rdv.statut}"/></span>
                    </div>
                </div>

                <div class="col-12 col-lg-8">
                    <div class="cm-card">
                        <div class="card-header p-3">
                            <div class="fw-bold">Dossier de consultation</div>
                            <div class="small text-muted">Diagnostic, ordonnance (lignes dynamiques), demandes d'analyses.</div>
                        </div>
                        <div class="card-body p-0">

                            <form method="post" action="${pageContext.request.contextPath}/medecin/consultation" id="consultationForm">
                                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"/>
                                <input type="hidden" name="rdvId" value="${rdv.id}"/>
                                <input type="hidden" name="selectedDate" value="${selectedDate}"/>

                                <ul class="nav nav-tabs nav-fill px-3 pt-3 mb-0" id="consultTabs" role="tablist">
                                    <li class="nav-item" role="presentation">
                                        <button class="nav-link active" id="tab-diag-tab" data-bs-toggle="tab" data-bs-target="#tab-diag" type="button" role="tab" aria-controls="tab-diag" aria-selected="true">
                                            <i class="fa-solid fa-stethoscope me-2"></i>Clinique
                                        </button>
                                    </li>
                                    <li class="nav-item" role="presentation">
                                        <button class="nav-link" id="tab-ordo-tab" data-bs-toggle="tab" data-bs-target="#tab-ordo" type="button" role="tab">
                                            <i class="fa-solid fa-pills me-2"></i>Ordonnance
                                        </button>
                                    </li>
                                    <li class="nav-item" role="presentation">
                                        <button class="nav-link" id="tab-analyses-tab" data-bs-toggle="tab" data-bs-target="#tab-analyses" type="button" role="tab">
                                            <i class="fa-solid fa-flask-vial me-2"></i>Analyses
                                        </button>
                                    </li>
                                </ul>

                                <div class="tab-content border-top bg-body-tertiary p-4">
                                    <%-- TAB 1 – Diagnostic --%>
                                    <div class="tab-pane fade show active" id="tab-diag" role="tabpanel" aria-labelledby="tab-diag-tab" tabindex="0">
                                        <div class="rounded-3 bg-white shadow-sm border p-3">
                                            <div class="small text-muted mb-3">Sauvegarde en base : table <code class="small">consultation</code>.</div>
                                            <div class="mb-3">
                                                <label class="form-label fw-semibold">Diagnostic</label>
                                                <textarea class="form-control" name="diagnostic" rows="5" placeholder="Saisir le diagnostic…"><c:out value="${consultation.diagnostic}"/></textarea>
                                            </div>
                                            <div class="mb-0">
                                                <label class="form-label fw-semibold">Remarque</label>
                                                <textarea class="form-control" name="remarque" rows="3" placeholder="Remarques complémentaires…"><c:out value="${consultation.remarque}"/></textarea>
                                            </div>
                                        </div>
                                    </div>

                                    <%-- TAB 2 – Ordonnance (lignes dynamiques, POST en tableaux) --%>
                                    <div class="tab-pane fade" id="tab-ordo" role="tabpanel" tabindex="0">
                                        <div class="rounded-3 bg-white shadow-sm border p-3">
                                            <div class="d-flex flex-wrap align-items-start justify-content-between gap-2 mb-3">
                                                <div>
                                                    <div class="fw-semibold">Médicaments</div>
                                                    <div class="small text-muted">Un enregistrement d’ordonnance par consultation avec plusieurs lignes (nom / posologie / durée).</div>
                                                </div>
                                                <button type="button" class="btn btn-sm btn-outline-primary" id="addMedRowBtn">
                                                    <i class="fa-solid fa-plus me-1"></i>Ajouter un médicament
                                                </button>
                                            </div>

                                            <div class="table-responsive">
                                                <table class="table table-sm align-middle" id="medTable">
                                                    <thead class="table-light">
                                                    <tr>
                                                        <th style="width:28%">Nom</th>
                                                        <th style="width:36%">Posologie</th>
                                                        <th style="width:26%">Durée</th>
                                                        <th style="width:10%"></th>
                                                    </tr>
                                                    </thead>
                                                    <tbody id="medTbody">
                                                    <c:choose>
                                                        <c:when test="${not empty medicaments}">
                                                            <c:forEach var="med" items="${medicaments}">
                                                                <tr class="med-row">
                                                                    <td>
                                                                        <input type="text" class="form-control form-control-sm"
                                                                               name="medicament_nom"
                                                                               value="${fn:escapeXml(med.nom)}" autocomplete="off" placeholder="Nom du médicament"/>
                                                                    </td>
                                                                    <td>
                                                                        <input type="text" class="form-control form-control-sm"
                                                                               name="medicament_posologie"
                                                                               value="${fn:escapeXml(med.posologie)}" placeholder="ex. 1 cp matin/soir après repas"/>
                                                                    </td>
                                                                    <td>
                                                                        <input type="text" class="form-control form-control-sm"
                                                                               name="medicament_duree"
                                                                               value="${fn:escapeXml(med.duree)}" placeholder="ex. 7 jours"/>
                                                                    </td>
                                                                    <td class="text-end">
                                                                        <button type="button" class="btn btn-sm btn-outline-danger med-remove-btn" aria-label="Supprimer la ligne">
                                                                            <i class="fa-solid fa-trash-can"></i>
                                                                        </button>
                                                                    </td>
                                                                </tr>
                                                            </c:forEach>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <tr class="med-row">
                                                                <td>
                                                                    <input type="text" class="form-control form-control-sm"
                                                                           name="medicament_nom"
                                                                           autocomplete="off" placeholder="Nom du médicament"/>
                                                                </td>
                                                                <td>
                                                                    <input type="text" class="form-control form-control-sm"
                                                                           name="medicament_posologie" placeholder="ex. 1 cp matin/soir après repas"/>
                                                                </td>
                                                                <td>
                                                                    <input type="text" class="form-control form-control-sm"
                                                                           name="medicament_duree" placeholder="ex. 7 jours"/>
                                                                </td>
                                                                <td class="text-end">
                                                                    <button type="button" class="btn btn-sm btn-outline-danger med-remove-btn" aria-label="Supprimer la ligne">
                                                                        <i class="fa-solid fa-trash-can"></i>
                                                                    </button>
                                                                </td>
                                                            </tr>
                                                        </c:otherwise>
                                                    </c:choose>
                                                    </tbody>
                                                </table>
                                            </div>
                                            <template id="medRowTemplate">
                                                <tr class="med-row">
                                                    <td>
                                                        <input type="text" class="form-control form-control-sm med-nom-input"
                                                               name="medicament_nom"
                                                               autocomplete="off" placeholder="Nom du médicament"/>
                                                    </td>
                                                    <td>
                                                        <input type="text" class="form-control form-control-sm"
                                                               name="medicament_posologie" placeholder="Posologie"/>
                                                    </td>
                                                    <td>
                                                        <input type="text" class="form-control form-control-sm"
                                                               name="medicament_duree" placeholder="Durée"/>
                                                    </td>
                                                    <td class="text-end">
                                                        <button type="button" class="btn btn-sm btn-outline-danger med-remove-btn" aria-label="Supprimer la ligne">
                                                            <i class="fa-solid fa-trash-can"></i>
                                                        </button>
                                                    </td>
                                                </tr>
                                            </template>
                                        </div>
                                    </div>

                                    <%-- TAB 3 – Analyses demandées (checkbox multiples name="codeAnalyse") --%>
                                    <div class="tab-pane fade" id="tab-analyses" role="tabpanel" tabindex="0">
                                        <div class="rounded-3 bg-white shadow-sm border p-3">
                                            <div class="fw-semibold mb-1">Analyses courantes à demander</div>
                                            <div class="small text-muted mb-3">Cochez les examens nécessaires. Les données sont reliées à l’identifiant de la consultation après enregistrement.</div>

                                            <div class="row g-2">
                                                <c:forEach var="entry" items="${analysesDisponibles.entrySet()}">
                                                    <div class="col-md-6 col-lg-6">
                                                        <label class="d-flex gap-2 align-items-start rounded-2 border p-3 h-100 bg-light-subtle user-select-none">
                                                            <input class="form-check-input mt-1 flex-shrink-0" type="checkbox"
                                                                   name="codeAnalyse"
                                                                   value="${fn:escapeXml(entry.key)}"
                                                                   <c:if test="${analyseCochees[entry.key]}">checked="checked"</c:if>
                                                            >
                                                            <span>
                                                                <span class="fw-semibold d-block"><c:out value="${entry.key}"/></span>
                                                                <span class="small text-muted"><c:out value="${entry.value}"/></span>
                                                            </span>
                                                        </label>
                                                    </div>
                                                </c:forEach>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div class="p-3 bg-white border-top d-flex flex-wrap gap-2 justify-content-between">
                                    <div class="small text-muted align-self-center">Enregistrez d'abord la consultation, puis générez les PDF.</div>
                                    <div class="d-flex flex-wrap gap-2">
                                        <button class="btn btn-primary" type="submit">
                                            <i class="fa-solid fa-floppy-disk me-2"></i>Enregistrer tout
                                        </button>
                                        <a class="btn btn-light border" href="${pageContext.request.contextPath}/medecin/dashboard?selectedDate=<c:out value="${selectedDate}"/>">Fermer</a>
                                    </div>
                                </div>
                            </form>

                            <c:if test="${not empty consultation.id}">
                                <div class="p-3 border-top bg-light">
                                    <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-2">
                                        <div>
                                            <div class="fw-semibold">Documents médicaux PDF</div>
                                            <div class="small text-muted">Ordonnance, analyses demandées et compte rendu (selon le contenu saisi).</div>
                                        </div>
                                        <form method="post" action="${pageContext.request.contextPath}/medecin/consultation/generate-documents"
                                              onsubmit="return confirm('Générer les PDF et les envoyer par email au patient ?');">
                                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"/>
                                            <input type="hidden" name="rdvId" value="${rdv.id}"/>
                                            <input type="hidden" name="selectedDate" value="${selectedDate}"/>
                                            <button type="submit" class="btn btn-success">
                                                <i class="fa-solid fa-file-pdf me-2"></i>Générer documents
                                            </button>
                                        </form>
                                    </div>
                                    <c:if test="${not empty documentsConsultation}">
                                        <ul class="list-group list-group-flush small">
                                            <c:forEach var="doc" items="${documentsConsultation}">
                                                <li class="list-group-item d-flex justify-content-between">
                                                    <span><c:out value="${doc.typeDocumentLabel}"/> — <c:out value="${doc.titre}"/></span>
                                                    <span class="text-muted"><c:out value="${doc.dateCreation}"/></span>
                                                </li>
                                            </c:forEach>
                                        </ul>
                                    </c:if>
                                </div>
                            </c:if>
                            <c:if test="${empty consultation.id}">
                                <div class="p-3 border-top">
                                    <div class="alert alert-info mb-0 small">
                                        Enregistrez la consultation pour activer le bouton « Générer documents ».
                                    </div>
                                </div>
                            </c:if>
                        </div>
                    </div>
                </div>
            </div>

        </div>
    </main>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    (function () {
        var tbody = document.getElementById('medTbody');
        var tpl = document.getElementById('medRowTemplate');
        document.getElementById('addMedRowBtn').addEventListener('click', function () {
            var clone = tpl.content.cloneNode(true);
            tbody.appendChild(clone);
        });

        tbody.addEventListener('click', function (e) {
            var btn = e.target.closest('.med-remove-btn');
            if (!btn) return;
            var row = btn.closest('tr.med-row');
            if (!row) return;
            if (tbody.querySelectorAll('tr.med-row').length === 1) {
                row.querySelectorAll('input').forEach(function (input) {
                    input.value = '';
                });
                return;
            }
            row.remove();
        });
    })();
</script>
</body>
</html>
