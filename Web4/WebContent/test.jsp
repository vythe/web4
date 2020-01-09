<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    import="net.vbelev.web4.core.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
</head>
<body>
<h2>Up</h2>
<%
double val = 0;
for (int i = 0; i < 20; i++) {
	String si = ("0" + i);
	si = si.substring(si.length() - 2);
	out.write(si + ": " + val + "<br/>");
	val = GBProfile.calculateStep(val, 1);
}
%>
<h2>Up2</h2>
<%
double val2 = 0;
for (int i = 0; i < 40; i++) {
	String si = ("0" + i);
	si = si.substring(si.length() - 2);
	out.write(si + ": " + val2 + "<br/>");
	val2 = GBProfile.calculateStep(val2, 0.5);
}
%>
<h2>Down</h2>
<%
for (int i = 19; i >= 0; i--) {
	String si = ("0" + i);
	si = si.substring(si.length() - 2);
	out.write(si + ": " + val + "<br/>");
	val = GBProfile.calculateStep(val, -0.5);
}
%>
</body>
</html>