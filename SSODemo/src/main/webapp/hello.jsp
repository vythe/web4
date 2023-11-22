<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Insert title here</title>
<%
String name = (String)request.getAttribute("name");
if (name == null)
	name = "(JSP noname)";
%>
</head>
<body>
<h1>Hello, <%=name %> at SSODemo</h1>
</body>
</html>