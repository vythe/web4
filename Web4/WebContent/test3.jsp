<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    import="net.vbelev.web4.core.*, net.vbelev.web4.xml.*, net.vbelev.web4.utils.*, net.vbelev.web4.*, java.util.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
</head>
<body>
<%
net.vbelev.web4.GBMongoStorage storage = new net.vbelev.web4.GBMongoStorage("mongodb://web4:vbelevweb4@vbelev.net");
org.bson.BsonDocument cmd = MongoStorage.mdbDocument(
"find", "bills",
"filter", MongoStorage.mdbDocument(
		"ID", MongoStorage.mdbDocument("$gte", 2),
		"ID", MongoStorage.mdbDocument("$lte", 4)
		),
"projection", MongoStorage.mdbDocument("_id", 0) //, "ID", "1", "title", "1")
);

org.bson.BsonDocument cmd2 = MongoStorage.mdbDocument(
	"delete", "bills",
	"deletes", MongoStorage.mdbArray(
		 MongoStorage.mdbDocument(
				"q", MongoStorage.mdbDocument("ID", 9),
			"limit", 1
		)
	)
);
String cmdString = cmd.toJson(new org.bson.json.JsonWriterSettings(true));
String resString = "";
try
{
	org.bson.Document res = storage.mongoDB.runCommand(cmd);
	resString = res.toJson(new org.bson.json.JsonWriterSettings(true));
}
catch (Exception x)
{
	resString = "EXCEPTION: " + x.getMessage();
}

//Integer i1 = storage.getNewBillID(false);
//Integer i2 = storage.getNewWebUserID(false);

%>
<h1>Command:</h1>
<%=cmdString%>
<h1>Result:</h1>
<%=resString%>
</body>
</html>