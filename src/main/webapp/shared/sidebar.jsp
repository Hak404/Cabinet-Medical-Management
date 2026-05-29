<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String ctx = request.getContextPath();
    String uri = request.getRequestURI();
    if (uri == null) uri = "";

    // Rôle: priorité à sessionScope.user.role, sinon param "type" (compat)
    String role = null;
    try {
        Object u = session.getAttribute("user");
        if (u != null) {
            try {
                Object r = u.getClass().getMethod("getRole").invoke(u);
                role = (r == null) ? null : String.valueOf(r);
            } catch (Exception ignore1) {
                try {
                    java.lang.reflect.Field f = u.getClass().getDeclaredField("role");
                    f.setAccessible(true);
                    Object r = f.get(u);
                    role = (r == null) ? null : String.valueOf(r);
                } catch (Exception ignore2) {
                    role = null;
                }
            }
        }
    } catch (Exception ignore) {
        role = null;
    }
    if (role == null || role.trim().isEmpty()) role = request.getParameter("type");
    if (role == null) role = "";
    role = role.trim().toUpperCase();
    String selectedDate = request.getParameter("selectedDate");
    if (selectedDate == null || selectedDate.trim().isEmpty()) {
        selectedDate = request.getParameter("date");
    }
    if (selectedDate == null || selectedDate.trim().isEmpty()) {
        selectedDate = java.time.LocalDate.now().toString();
    }
    String dateQuery = "?selectedDate=" + selectedDate;

    boolean isAdmin = role.equals("ADMIN");
    boolean isMedecin = role.equals("MEDECIN") || role.equals("MEDECIN ") || role.equals("DOCTOR") || role.equals("MEDECIN");
    boolean isSecretaire = role.equals("SECRETAIRE") || role.equals("SECRETAIRE ") || role.equals("SECRETARY");
    boolean isPatient = role.equals("PATIENT");
    boolean isPharmacie = role.equals("PHARMACIE") || role.equals("PHARMACY");
%>
<%!
    public String activeClass(String uri, String containsPath) {
        if (uri == null) uri = "";
        if (containsPath == null) return "";
        return uri.contains(containsPath) ? "active" : "";
    }
%>

<aside class="offcanvas-md offcanvas-start app-sidebar cm-sidebar" tabindex="-1" id="appSidebar" aria-labelledby="appSidebarLabel">
    <div class="offcanvas-header border-bottom align-items-center">
        <div class="d-flex align-items-center gap-2">
            <span class="brand-mark">
                <i class="fa-solid fa-heart-pulse"></i>
            </span>
            <div class="cm-sidebar-brand">
                <div class="fw-bold text-primary lh-1">Cabinet</div>
                <div class="small text-muted lh-1">Management System</div>
            </div>
        </div>

        <div class="d-flex align-items-center gap-2">
            <button type="button"
                    class="btn btn-sm btn-light border d-none d-md-inline-flex"
                    id="sidebarCollapseBtn"
                    aria-label="Réduire la sidebar"
                    title="Réduire / Développer">
                <i class="fa-solid fa-angles-left"></i>
            </button>
            <button type="button" class="btn-close d-md-none" data-bs-dismiss="offcanvas" aria-label="Fermer"></button>
        </div>
    </div>

    <div class="offcanvas-body d-flex flex-column p-3">
        <div class="sidebar-section">
            <div class="sidebar-title">Menu</div>
            <ul class="nav nav-pills flex-column gap-1 cm-sidebar-nav">
                <% if (isAdmin) { %>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/admin/dashboard") %>" href="<%= ctx %>/admin/dashboard">
                        <i class="fa-solid fa-gauge-high"></i><span class="cm-nav-text">Dashboard</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/admin/cabinets") %>" href="<%= ctx %>/admin/cabinets">
                        <i class="fa-solid fa-hospital"></i><span class="cm-nav-text">Cabinets</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/admin/medecins") %>" href="<%= ctx %>/admin/medecins">
                        <i class="fa-solid fa-user-doctor"></i><span class="cm-nav-text">Médecins</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/admin/patients") %>" href="<%= ctx %>/admin/patients">
                        <i class="fa-solid fa-user-injured"></i><span class="cm-nav-text">Patients</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/admin/secretaire") %>" href="<%= ctx %>/admin/secretaire">
                        <i class="fa-solid fa-user-tie"></i><span class="cm-nav-text">Secrétaires</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/admin/pharmacies") %>" href="<%= ctx %>/admin/pharmacies">
                        <i class="fa-solid fa-pills"></i><span class="cm-nav-text">Pharmacies</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/admin/settings") %>" href="<%= ctx %>/admin/settings">
                        <i class="fa-solid fa-gear"></i><span class="cm-nav-text">Settings</span>
                    </a>
                </li>

                <% } else if (isMedecin) { %>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/medecin/dashboard") %>" href="<%= ctx %>/medecin/dashboard<%= dateQuery %>">
                        <i class="fa-solid fa-gauge-high"></i><span class="cm-nav-text">Dashboard</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/medecin/patients") %>" href="<%= ctx %>/medecin/patients<%= dateQuery %>">
                        <i class="fa-solid fa-users"></i><span class="cm-nav-text">Mes Patients</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/medecin/rendezvous") %>" href="<%= ctx %>/medecin/rendezvous<%= dateQuery %>">
                        <i class="fa-solid fa-calendar-check"></i><span class="cm-nav-text">Rendez-vous</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/medecin/consultations") %>" href="<%= ctx %>/medecin/consultations<%= dateQuery %>">
                        <i class="fa-solid fa-stethoscope"></i><span class="cm-nav-text">Consultations</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/medecin/ordonnances") %>" href="<%= ctx %>/medecin/ordonnances<%= dateQuery %>">
                        <i class="fa-solid fa-file-prescription"></i><span class="cm-nav-text">Ordonnances</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/medecin/analyses") %>" href="<%= ctx %>/medecin/analyses<%= dateQuery %>">
                        <i class="fa-solid fa-vials"></i><span class="cm-nav-text">Analyses</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/medecin/conges") %>" href="<%= ctx %>/medecin/conges<%= dateQuery %>">
                        <i class="fa-solid fa-umbrella-beach"></i><span class="cm-nav-text">Congés</span>
                    </a>
                </li>

                <% } else if (isSecretaire) { %>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/secretaire/dashboard") %>" href="<%= ctx %>/secretaire/dashboard">
                        <i class="fa-solid fa-gauge-high"></i><span class="cm-nav-text">Dashboard</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/secretaire/rendezvous") %>" href="<%= ctx %>/secretaire/rendezvous">
                        <i class="fa-solid fa-calendar-check"></i><span class="cm-nav-text">Rendez-vous</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/secretaire/patients") %>" href="<%= ctx %>/secretaire/patients">
                        <i class="fa-solid fa-user-injured"></i><span class="cm-nav-text">Patients</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/secretaire/agenda-medecin") %>" href="<%= ctx %>/secretaire/agenda-medecin">
                        <i class="fa-solid fa-calendar-days"></i><span class="cm-nav-text">Agenda Médecin</span>
                    </a>
                </li>

                <% } else if (isPatient) { %>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/patient/dashboard") %>" href="<%= ctx %>/patient/dashboard">
                        <i class="fa-solid fa-gauge-high"></i><span class="cm-nav-text">Dashboard</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/patient/prendre-rendezvous") %>" href="<%= ctx %>/patient/prendre-rendezvous">
                        <i class="fa-solid fa-calendar-plus"></i><span class="cm-nav-text">Prendre Rendez-vous</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/patient/rendezvous") %>" href="<%= ctx %>/patient/rendezvous">
                        <i class="fa-solid fa-calendar-check"></i><span class="cm-nav-text">Mes Rendez-vous</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/patient/ordonnances") %>" href="<%= ctx %>/patient/ordonnances">
                        <i class="fa-solid fa-file-prescription"></i><span class="cm-nav-text">Ordonnances</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/patient/analyses") %>" href="<%= ctx %>/patient/analyses">
                        <i class="fa-solid fa-vials"></i><span class="cm-nav-text">Analyses</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/patient/historique") %>" href="<%= ctx %>/patient/historique">
                        <i class="fa-solid fa-notes-medical"></i><span class="cm-nav-text">Historique médical</span>
                    </a>
                </li>

                <% } else if (isPharmacie) { %>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/pharmacie/dashboard") %>" href="<%= ctx %>/pharmacie/dashboard">
                        <i class="fa-solid fa-gauge-high"></i><span class="cm-nav-text">Dashboard</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/pharmacie/ordonnances") %>" href="<%= ctx %>/pharmacie/ordonnances">
                        <i class="fa-solid fa-file-prescription"></i><span class="cm-nav-text">Ordonnances reçues</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/pharmacie/stock") %>" href="<%= ctx %>/pharmacie/stock">
                        <i class="fa-solid fa-boxes-stacked"></i><span class="cm-nav-text">Stock Médicaments</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= activeClass(uri, "/pharmacie/confirmation") %>" href="<%= ctx %>/pharmacie/confirmation">
                        <i class="fa-solid fa-circle-check"></i><span class="cm-nav-text">Confirmation disponibilité</span>
                    </a>
                </li>
                <% } else { %>
                <li class="nav-item">
                    <a class="nav-link active" href="#">
                        <i class="fa-solid fa-gauge-high"></i><span class="cm-nav-text">Dashboard</span>
                    </a>
                </li>
                <% } %>

                <li class="nav-item mt-2">
                    <a class="nav-link text-danger <%= activeClass(uri, "/logout") %>" href="<%= ctx %>/logout">
                        <i class="fa-solid fa-right-from-bracket"></i><span class="cm-nav-text">Logout</span>
                    </a>
                </li>
            </ul>
        </div>

        <div class="mt-auto pt-3 border-top cm-sidebar-footer">
            <div class="text-muted small">
                <i class="fa-solid fa-shield-heart me-1"></i>
                Interface médicale • rôle: <span class="fw-semibold"><%= (role.isEmpty() ? "N/A" : role) %></span>
            </div>
        </div>
    </div>
</aside>

<script>
(() => {
  const key = "cm.sidebar.collapsed";
  const root = document.documentElement;
  const btn = document.getElementById("sidebarCollapseBtn");
  const apply = (collapsed) => {
    root.classList.toggle("cm-sidebar-collapsed", !!collapsed);
    if (btn) {
      const icon = btn.querySelector("i");
      if (icon) icon.className = collapsed ? "fa-solid fa-angles-right" : "fa-solid fa-angles-left";
      btn.setAttribute("aria-label", collapsed ? "Développer la sidebar" : "Réduire la sidebar");
      btn.title = collapsed ? "Développer" : "Réduire";
    }
  };

  try {
    const saved = localStorage.getItem(key);
    apply(saved === "1");
  } catch (e) {
    apply(false);
  }

  if (btn) {
    btn.addEventListener("click", () => {
      const collapsed = !root.classList.contains("cm-sidebar-collapsed");
      apply(collapsed);
      try { localStorage.setItem(key, collapsed ? "1" : "0"); } catch (e) {}
    });
  }
})();
</script>
