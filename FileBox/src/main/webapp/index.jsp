<%@ page language="java" contentType="text/html; charset=UTF-8"
import="java.util.*,java.util.stream.*,net.vbelev.filebox.*"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Insert title here</title>
</head>
<body>
<h1>Hello, JSP</h1>
default charset: <%=java.nio.charset.Charset.defaultCharset()%>
<%
/*TextConstellation test = TextConstellation.TestInit();
for (String s : test.lines)
{
	out.write(s + "<br/>\n");
}
*/
/*
WordMetric.WordIterable iter = new WordMetric.WordIterable("   blue! fox-животное moon   ");
for (String s : iter)
{
	out.write("[" + s + "]<br/>\n");
}
String keys = new WordMetric().test();
out.write("KEYS:<br/>" + keys);
*/
/*
WordMetric w = new WordMetric();
WordMetric.Vector v = w.parse("Спустя с 10 месяцев СВО темпы продвижения в ДНР невысоки. Многие спрашивают — неужели невозможно уничтожить оборонительные рубежи с помощью ковровых бомбардировок? Верно ли утверждение, что выстроенная Киевом на данном направлении оборона не имеет аналогов в новейшей истории характер");
WordMetric.Vector v2 = w.parse(" С точки зрения директивного контроля, эти противоречия малосущественны. В конечном итоге все реальные рычаги формирования марионеточной администрации находятся в руках США, и от помех, мешающих генеральному курсу, они могут легко избавиться. Это касается как Зеленского, так и Залужного. Их личная конкуренция малосущественна, если США устраивает текущий характер формирования колониальной администрации.");
for (Map.Entry<Integer, Float> entry : v.diff(v2).values(false))
{
	float vValue = v.get(entry.getKey());
	out.write(entry.getKey() + " " + w.getWord(entry.getKey()) + ": " + vValue + "/" + entry.getValue() + "<br/>");
}
*/
TextConstellation test = new TextConstellation();
test.TestInit();
/*
int bestText = 0;
float bestDist = 0;

for (int i = 0; i < test.texts().size(); i++)
{
	TextStar text = test.texts().get(i);
	float textDist = text.vector.diff(test.getCenter()).norm();
	
	if (textDist > bestDist)
	{
		bestText = i;
		bestDist = textDist;
	}
}
out.write("Constellation count=" + test._texts.size()+ "<br/><br/>");
TextStar ts = test._texts.get(bestText);

out.write("Best text: " + ts.source + ", dist=" + bestDist);

for (Map.Entry<Integer, Float> entry : 
	WordMetric.streamToIterable(
			WordMetric.iterableToStream(test.getCenter().values(false))
		.sorted((q1, q2) -> q2.getValue().compareTo(q1.getValue()))
		))
{
	float best = ts.vector.get(entry.getKey());
	out.write(entry.getKey() + " " + test.getMetric().getWord(entry.getKey())  + ": c=" + entry.getValue() + ", b=" + best + "<br/>");
}
*/
out.write("domain count: " + test._domains.size() + "<br/>");
out.write("<h4>Top Weights</h4>\n");
out.write(test.weightStr + "<br/><br/>");
for (int dispDomain = 0; dispDomain < test._domains.size(); dispDomain++)
{
	//out.write("<h4>domain " + dispDomain + " " + test._domains.get(dispDomain).diff(test.getCenter()).TopEntriesText(10) +  "</h4>\n");
	out.write("<h4>domain " + dispDomain + " " + test._domains.get(dispDomain).TopEntriesText(20) +  "</h4>\n");
	for (TextStar ts: test.texts())
	{
		if (ts.domainIndex != dispDomain)
			continue;
		
		float tsDist = ts.vector.diff(test._domains.get(dispDomain)).norm();
		float centerDist = ts.vector.diff(test.getCenter()).norm();
		out.write(ts.source + ", domainD=" + tsDist + ", centerD=" + centerDist + "<br/>");
		out.write(ts.vector.TopEntriesText(10) + "<br/>");
	}
	out.write("<p/>\n");
}
%>

<%--
<h3>First text: <%=ts.source %></h3>
<%
for (Map.Entry<Integer, Float> entry : ts.vector.values(false))
{
	out.write(entry.getKey() + " " + test.getMetric().getWord(entry.getKey())  + ": " + entry.getValue() + "<br/>");
}
%>
 --%>
</body>
</html>