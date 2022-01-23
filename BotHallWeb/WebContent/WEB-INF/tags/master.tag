<%@tag description="Common page template" pageEncoding="UTF-8"%>
<%@attribute name="title" fragment="false" %> 
<%@attribute name="header" fragment="true" %>
<%--
<%@attribute name="footer" fragment="true" %>
--%>
<%-- need to import jakarta standard tag library for this - maybe later
<% @ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="${(empty title) ? "BH Page" : title}" />
--%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>${(empty title) ? "BH Page" : title}</title>
	<script src="js/jquery-3.5.1.js"></script>
	<script src="js/utils.js"></script>
	<script src="js/bhclient.js"></script>
	<jsp:invoke fragment="header"/>
</head>
  <body>
      <jsp:doBody/>
      <%-- 
    <div id="pagefooter">
      <jsp:invoke fragment="footer"/>
    </div>
    --%>
    <div style="text-align: center;">
    <hr/>
    That's it.
    </div>
  </body>
</html>