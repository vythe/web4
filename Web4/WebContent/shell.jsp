<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    import="net.vbelev.web4.core.*, net.vbelev.web4.utils.*, org.bson.Document"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
</head>
<body>
<%
Object mongoAttr = request.getSession().getAttribute("mongo");
net.vbelev.web4.GBMongoStorage storage = null;
if (mongoAttr instanceof net.vbelev.web4.GBMongoStorage)
{
	storage = (net.vbelev.web4.GBMongoStorage)mongoAttr;
}
else
{
	storage = new net.vbelev.web4.GBMongoStorage("mongodb://web4:vbelevweb4@vbelev.net:27017/web4?authSource=admin");
	request.getSession().setAttribute("mongo", storage);
}
String query = request.getParameter("query");
String resString = "";
String resError = "";
try
{
if (!Utils.IsEmpty(query))
{
	Document res = storage.mongoDB.runCommand(org.bson.Document.parse(query));
	resString = res.toJson(new org.bson.json.JsonWriterSettings(true));
}
}
catch (Exception x) {
	resError = x.getMessage();
}
%>
<div style="width: 100%; border 1px black solid;">
<form action="" method="POST">
<h3>Query:</h3>
<textarea name="query" style="width: 100%; height: 10em;"><%=query%></textarea><br/>
<input type="submit" value="Submit"/>
</form>
<fieldset>
<legend>Result:</legend>
<pre><%=resString%></pre>
</fieldset>
<fieldset>
<legend>Error:</legend>
<%=resError%>
</fieldset>
</div>
</body>
</html>