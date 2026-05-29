<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String ctx = request.getContextPath();
    String pageTitle = request.getParameter("title");
    if (pageTitle == null || pageTitle.trim().isEmpty()) pageTitle = "Dashboard";

    String userLabel = request.getParameter("userLabel");
    if (userLabel == null || userLabel.trim().isEmpty()) userLabel = "Utilisateur";
%>

<nav class="navbar navbar-expand-lg navbar-light bg-white border-bottom sticky-top app-navbar">
    <div class="container-fluid px-3 px-lg-4">
        <button class="btn btn-outline-primary d-md-none me-2"
                type="button"
                data-bs-toggle="offcanvas"
                data-bs-target="#appSidebar"
                aria-controls="appSidebar"
                aria-label="Ouvrir le menu">
            <i class="fa-solid fa-bars"></i>
        </button>

        <a class="navbar-brand d-flex align-items-center gap-2 fw-bold text-primary" href="<%= ctx %>/">
            <span class="brand-mark">
                <i class="fa-solid fa-heart-pulse"></i>
            </span>
            <span>Cabinet Medical</span>
        </a>

        <div class="d-none d-lg-flex align-items-center ms-3">
            <span class="text-muted small">/</span>
            <span class="ms-2 fw-semibold text-dark"><%= pageTitle %></span>
        </div>

        <div class="ms-auto d-flex align-items-center gap-2">
            <form class="d-none d-lg-block" role="search" action="#" method="get">
                <div class="input-group input-group-sm app-search">
                    <span class="input-group-text bg-white border-end-0">
                        <i class="fa-solid fa-magnifying-glass text-muted"></i>
                    </span>
                    <input class="form-control border-start-0" type="search" placeholder="Rechercher…" aria-label="Rechercher">
                </div>
            </form>

            <div class="dropdown">
                <button class="btn btn-light border dropdown-toggle d-flex align-items-center gap-2"
                        type="button"
                        data-bs-toggle="dropdown"
                        aria-expanded="false">
                    <span class="avatar-circle">
                        <i class="fa-solid fa-user-doctor"></i>
                    </span>
                    <span class="d-none d-sm-inline"><%= userLabel %></span>
                </button>
                <ul class="dropdown-menu dropdown-menu-end shadow-sm">
                    <li class="dropdown-header text-muted">Compte</li>
                    <li>
                        <a class="dropdown-item" href="#">
                            <i class="fa-solid fa-id-badge me-2 text-muted"></i>Profil
                        </a>
                    </li>
                    <li>
                        <a class="dropdown-item" href="#">
                            <i class="fa-solid fa-gear me-2 text-muted"></i>Paramètres
                        </a>
                    </li>
                    <li><hr class="dropdown-divider"></li>
                    <li>
                        <a class="dropdown-item text-danger" href="<%= ctx %>/logout">
                            <i class="fa-solid fa-right-from-bracket me-2"></i>Déconnexion
                        </a>
                    </li>
                </ul>
            </div>
        </div>
    </div>
</nav>
