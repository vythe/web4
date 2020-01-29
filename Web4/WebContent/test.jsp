<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    import="net.vbelev.web4.core.*, java.util.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
</head>
<body>
<%
net.vbelev.web4.GBMongoStorage storage = new net.vbelev.web4.GBMongoStorage("mongodb://web4:vbelevweb4@vbelev.net");
//Integer i1 = storage.getNewBillID(false);
//Integer i2 = storage.getNewWebUserID(false);
net.vbelev.web4.xml.GBBillXML gb = storage.loadBill(1);
Hashtable<String, Integer> wIndex = new Hashtable<String, Integer>();
storage.loadWebUserIndex(wIndex);
GBEngine engine = GBEngine.loadEngine(storage);
engine.loadGroups();
%>
<h2>Engine groups: <%=engine.getSize() %></h2>
<h2>Up</h2>
<%
double val = 0;
for (int i = 0; i < 20; i++) {
	String si = ("0" + i);
	si = si.substring(si.length() - 2);
	double newVal = GBProfile.calculateStep(val, 0.1);
	out.write(si + ": " + val + " by  " + (newVal - val) + "<br/>");
	val = newVal;
}
%>
<%--
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
--%>
<h2>Down</h2>
<%
for (int i = 19; i >= 0; i--) {
	String si = ("0" + i);
	si = si.substring(si.length() - 2);
	double newVal = GBProfile.calculateStep(val, -0.1);
	out.write(si + ": " + val + " by " + (newVal - val) + "<br/>");
	val = newVal;
}
%>
</body>
</html>