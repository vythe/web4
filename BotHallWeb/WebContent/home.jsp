<%@page contentType="text/html" pageEncoding="UTF-8" 
    import="java.util.*, net.vbelev.utils.*, net.vbelev.bothall.core.*, net.vbelev.bothall.web.*"
 %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%
/*
HttpSession session1 = request.getSession();
String userName1 = "(no name)";
if (session != null)
{
	userName1 = "" + session.getAttribute("userName");
}
*/
WebAPI.WebSessionPack pack = WebAPI.getWebSessionPack(request);
List<PacmanSession> sessions = PacmanSession.sessionList();
List<WebAPI.BHSessionMessage> messages = pack.getMessages();
pack.flushMessages();

	//S essionInfo item = new SessionInfo();
	//item.sessionId = s.getID();
	//item.status = s.getEngine().isRunning? "Running" : "Idle";
	//item.createdDate = Utils.formatDateTime(s.createdDate);
	//item.description = "Session #" + s.getID();
	//item.isProtected = s.isProtected? "Y": "";
%>
<c:set var="bodyContent">
<% if (messages.size() > 0) { %> 
	<div style="border: solid black 1px;"><ul>
	<% for (WebAPI.BHSessionMessage msg : messages) { 
		if (msg.kind == WebAPI.BHSessionMessageKind.INFO) {
			%><li>INFO: <%=msg.message%></li><%
		} else {
			%><li>ERROR: <%=msg.message%></li><%
		} 
	} %>
	</ul></div>
<% } %>	
Hello again, <%=pack.user.userName%><br/>
<form action="api/user/auth" method="POST">
Name: <input name="name" value="<%=Utils.encodeHTMLAttr(pack.user.userName)%>"><br/>
<input type="submit" value="Name It"/>
</form>
<hr/>
<table>
<thead>
<tr><th colspan="3">Sessions</th>
<th colspan="3">Mobiles</th>
</tr>
<tr>
<th style="width: 5em;">ID</th>
<th>Created</th>
<th>Description</th>
<th>Key</th>
<th>Status</th>
<th></th>
<%-- 
<th>Mobile ID</th>
<th>Driver</th>
<th>Client Key</th>
 --%>
</tr>
</thead>
<tbody>

<%for (PacmanSession s : PacmanSession.sessionList())
{
	WebAPI.BHSessionInfo sInfo = Utils.FirstOrDefault(pack.sessionList, q -> q.sessionID == s.getID());
	//pack
	List<BHClientRegistration> agents = BHClientRegistration.agentList(s.getID());
	BHClientRegistration sAgent = Utils.FirstOrDefault(agents,  
			q -> Utils.FirstOrDefault(pack.agentList, q2 -> { return q.clientKey.equals(q2.agentKey); }) != null
	);
	boolean hasAgent = (sAgent != null);
	
%><tr bh_session_id="<%=s.getID()%>">
	<td><%=s.getID() + (s.getEngine().isRunning? " (Running)" : "")%></td>
	<td><%=Utils.formatDateTime(s.createdDate)%></td>
	<td><%=Utils.encodeHTML(s.getDescription())%></td>
	<td><%=sInfo == null? "" : sInfo.sessionKey%></td>
	<td><%=BHSession.PS_STATUS.toString(s.sessionStatus) %></td>
	<td>
	<% if (s.sessionStatus == BHSession.PS_STATUS.NEW) { %>
		<button class="sessionStart">Start</button>
	<% } else { %>
		<button class="sessionStop">Start</button>
	<% } %>
	</td>
<%--	
	<td>(Join as a watcher)</td>
	<%if (hasAgent && sAgent.atomID <= 0) {%>
	<td><%=sAgent.clientKey%></td>
	<td>Open!</td>
	<%} else if (!hasAgent) {%>
	<td></td>
	<td>Join!</td>
	<%} else {%>
	<td></td>
	<td>---</td>
	<% } %>
	<td></td>
--%>
</tr>
<%-- big row - list of monsters within the session --%>
<tr><td></td><td colspan="5" style="text-align: center;">
<%--	<h3>Mobiles</h3> --%>
	<table><thead><tr>
		<th>Mobile ID</th>
		<th>Driver</th>
		<th>Client Key</th>
	</tr>
	</thead>
	<tbody>
	<tr>
	<td>(Join as a watcher)</td>
	<%if (hasAgent && sAgent.atomID <= 0) {%>
	<td><%=sAgent.clientKey%></td>
	<td>Open!</td>
	<%} else if (!hasAgent) {%>
	<td></td>
	<td>Join!</td>
	<%} else {%>
	<td></td>
	<td>---</td>
	<% } %>
	</tr>	
<% BHCollection.Atom hero = Utils.FirstOrDefault(s.getCollection().all(), q -> q.getGrade() == BHCollection.Atom.GRADE.HERO);
	BHClientRegistration heroAgent;
	if (hero != null)
	{
		heroAgent = Utils.FirstOrDefault(agents, q -> q.atomID == hero.getID());
		WebAPI.BHAgentInfo userAgent = null;
		if (heroAgent != null)
		{
			userAgent = Utils.FirstOrDefault(pack.agentList, q -> q.agentKey.equals(heroAgent.clientKey));
		}
%>
<tr bh_session_id="<%=s.getID()%>" bh_atom_id="<%=hero.getID()%>" bh_session_key="<%=sInfo == null? "" : sInfo.sessionKey%>">
	<td>Hero #<%=hero.getID()%></td>
	<td><%=heroAgent == null? "" : heroAgent.userName%></td>
	<% if (heroAgent == null) { %>
	<td><button class="createClient">Join as the hero</button></td>
	<% } else if (userAgent != null) {%>
	<td><a href="field.html?client=<%=userAgent.agentKey%>&sid=<%=s.getID()%>&session=<%=sInfo == null? "" : sInfo.sessionKey%>" target="_blank">Open <%=userAgent.agentKey%></a>
	</td>
	<% } else {%>
	<td></td>
	<%}%>	
</tr>
<%	}	// end if hero	
	// rows - list of monsters
for (BHCollection.Atom a : s.getCollection().all()) { 
	if (a.getGrade() != BHCollection.Atom.GRADE.MONSTER) { 
		continue;
	}
	BHClientRegistration agent = Utils.FirstOrDefault(agents, q -> q.atomID == a.getID());
	WebAPI.BHAgentInfo userAgent = null;
	if (agent != null)
	{
		userAgent = Utils.FirstOrDefault(pack.agentList, q -> q.agentKey.equals(agent.clientKey));
	}
	%>
	<tr bh_session_id="<%=s.getID()%>" bh_atom_id="<%=a.getID()%>" bh_session_key="<%=sInfo == null? "" : sInfo.sessionKey%>">
	<td>Monster #<%=a.getID()%></td>
	<td><%=agent == null? "" : agent.userName%></td>
	<% if (agent == null && !hasAgent) { %>
	<td>Join as this monster!</td>
	<% } else if (userAgent != null) {%>
	<td>Open <%=userAgent.agentKey%>!</td>
	<% } else {%>
	<td></td>
	<%}%>	
</tr>
<%} // end s.getCollection()
// end of list of monsters
%>
</tbody></table>
</td></tr>
<%-- end of big row - list of monsters within the session --%>

<%
} // end for bhsession%>
</tbody>
</table>
<br/>
<form action="api/user/createsession" method="GET">
<input type="submit" value="Create Session"/>
</form>
</c:set>
<t:master title="BH List">
<jsp:attribute name="header">
<script type="text/javascript">
console.log("header in!");

//var apiURL = "/BotHallWeb/api/";
var apiURL = "api/";
var fullApiURL = new URL(apiURL, window.location.href);


$(document).ready(function() {
	$("button.sessionStart").click(function(evt) {
		evt.preventDefault();
		var sid = $(this).closest("tr").attr("bh_session_id");
		if (!sid) return;
		
		submitForm("mainform", "api/user/startsession", "get", {sessionID: sid});
				
	});
	
	$("button.createClient").click(function(evt) {
		var row = $(this).closest("tr");
		var sessionID = row.attr("bh_session_id");
		var atomID = row.attr("bh_atom_id");
		//var sessionKey = row.attr("bh_session_key");
		if (!sessionID || !atomID) {
			alert("Invalid call to createClient");
			return;
		}
		/*
		var bh = BHClient(fullApiURL.href);
		bh.join(sessionID, atomID, sessionKey, function(key) {
			if (!key) {
				alert("Could not join sessionId=" + sessionID + ", atomId=" + atomID);
			} else {
				window.location.reload();
			}
		});
		*/
		submitForm("mainform", "api/user/createclient", "get", {sid: sessionID, atom: atomID});
		
	});
});
</script>
</jsp:attribute>
<jsp:body>
${bodyContent}
</jsp:body>
</t:master>