<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="net.vbelev.utils.*, net.vbelev.bothall.core.*, net.vbelev.bothall.web.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Insert title here</title>
<script src="https://code.jquery.com/jquery-3.5.1.js"></script>
</head>
<body>
<h2>Hello, world</h2>
<%
// this is a legacy page, it doesn't work anymore
//BHWebContext app = BHWebContext.getApplication(request.getServletContext());

/*if (app.engine.stage == BHEngine.CycleStageEnum.IDLE) {
app.engine.startCycling();
} else {
	app.engine.stopCycling();
}*/
//String res = app.engine.engineInstance + " - " + app.engine.stage;
WebAPI.WebSessionPack pack = WebAPI.getWebSessionPack(request);

PacmanSession s = Utils.FirstOrDefault(PacmanSession.sessionList(), null, null);
String res = s.status();

%>
Engine: <%=res%>
<div style="border:1px;" id="fetchres"></div>
<input type="text" id="msg" value="test"/>
<button onclick="fetch1()">Fetch it</button>

<script type="text/javascript">
function fetch1() {
	var msg = $("#msg").val();
	$.ajax({
		type: "GET",
		url :"/BotHallWeb/api/postAction",
		dataType: "text",
		withCredentials : true,
		data: {msg: msg}
	}).done((json, status, jqXHR) => {
		$("#fetchres").text(JSON.stringify(json, null, "<br/>"));
		  console.log(json);
	}).fail( (jqXHR, status, errorThrown) => { console.error("Error as " + status + ":", errorThrown); }
	);
}
</script>
</body>
</html>