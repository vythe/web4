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

	//S essionInfo item = new SessionInfo();
	//item.sessionId = s.getID();
	//item.status = s.getEngine().isRunning? "Running" : "Idle";
	//item.createdDate = Utils.formatDateTime(s.createdDate);
	//item.description = "Session #" + s.getID();
	//item.isProtected = s.isProtected? "Y": "";
%>
<c:set var="bodyContent">
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
<th>ID</th>
<th>Created</th>
<th>Description</th>
<th>Key</th>
<th>Mobile ID</th>
<th>Driver</th>
<th>Client Key</th>
</tr>
</thead>
<tbody>

<%for (PacmanSession s : PacmanSession.sessionList())
{
	WebAPI.BHSessionInfo sInfo = Utils.FirstOrDefault(pack.sessionList, q -> q.sessionID == s.getID());
	//pack
	List<BHClientRegistration> agents = BHClientRegistration.agentList(s.getID());
	BHClientRegistration sAgent = Utils.FirstOrDefault(agents, 
			q -> Utils.FirstOrDefault(pack.agentList, q2 -> q.clientKey.equals(q2.agentKey)) != null
	);
	boolean hasAgent = (sAgent != null);
	
%><tr>
	<td><%=s.getID() + (s.getEngine().isRunning? " (Running)" : "")%></td>
	<td><%=Utils.formatDateTime(s.createdDate)%></td>
	<td><%=Utils.encodeHTML(s.getDescription())%></td>
	<td><%=sInfo == null? "" : sInfo.sessionKey%></td>
	<td></td>
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
%><tr>
	<td colspan="4"></td>
	<td>Hero #<%=hero.getID()%></td>
	<td><%=heroAgent == null? "" : heroAgent.userName%></td>
	<% if (heroAgent == null && !hasAgent) { %>
	<td>Join as the hero!</td>
	<% } else if (userAgent != null) {%>
	<td>Open <%=userAgent.agentKey%>!</td>
	<% } else {%>
	<td></td>
	<%}%>	
</tr>
<%	}	// end if hero	
	
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
	<tr>
	<td colspan="4"></td>
	<td>Monster #<%=a.getID()%></td>
	<td><%=agent == null? "" : agent.userName%></td>
	<% if (agent == null && !hasAgent) { %>
	<td>Join as this monster!</td>
	<% } else if (userAgent != null) {%>
	<td>Open <%=userAgent.agentKey%>!</td>
	<% } else {%>
	<td></td>
	<%}%>	
<%} // end s.getCollection()%>
<%
} // end for bhsession%>
<tr><td colspan="8">

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
</script>
</jsp:attribute>
<jsp:body>
${bodyContent}
</jsp:body>
</t:master>