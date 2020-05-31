<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    import="net.vbelev.web4.core.*, net.vbelev.web4.xml.*, net.vbelev.web4.utils.*, java.util.*"%>
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
GBEngine engine = GBEngine.loadEngine(storage);
GBGroupSet gblist = engine.getGroupSet(0);
GBBill bill = new GBBill();
bill.publishedDate = new Date();
bill.title = "Test " + Utils.formatDateTime(bill.publishedDate);
bill.description = "Description of " + bill.title;
bill.status = GBBill.StatusEnum.PUBLISHED;

// give it two affinities
bill.setInvAffinity(engine.random.nextInt(gblist.getSize()), Math.random());
bill.setInvAffinity(engine.random.nextInt(gblist.getSize()), Math.random());
bill.calculateInvAffinities(gblist);

engine.saveBill(bill);
//engine.loadBills();

net.vbelev.web4.xml.GBBillXML gb = new net.vbelev.web4.xml.GBBillXML();
gb.fromGBBill(gblist, bill);
String xml = storage.xmliser.toXMLString(gb);
GBBillXML gb2 = storage.xmliser.fromXML(GBBillXML.class, xml);
GBBill bill2 = new GBBill();
gb2.toGBBill(gblist, bill2);
Hashtable<String, Integer> wIndex = new Hashtable<String, Integer>();
//storage.loadWebUserIndex(wIndex);
//engine.loadGroups();
%>
<%=xml%>
</body>
</html>