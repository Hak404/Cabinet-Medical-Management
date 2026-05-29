<%@ page import="com.cabinet.model.User" %>
<%@ page import="com.cabinet.util.SessionUtil" %>
<%
    User sessionUser = SessionUtil.getAuthenticatedUser(request.getSession(false));
    if (sessionUser == null) {
        response.sendRedirect(request.getContextPath() + "/login?timeout=1");
        return;
    }
%>