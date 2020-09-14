/* A dump of old javascript things (from base.html) 
 */


function doStart(cmd) {
	$.ajax({
		method: "GET",
		url: apiURL + "start",
		data: {doStart: cmd},
		dataType: "text"
	}).done(function(data, textStatus, jqXHR) {
		alert("doStart: " + data);
		console.log("doStart: " + data + " at " + new Date());
	}).fail(function(jqXHR, textStatus, errorThrown ) {
		alert("error: " + JSON.stringify(errorThrown));
	}).always(function() {
	});
}

function joinSession1(bhclient, sessionID) {

	//var url = bhclient.url + "getUpdate";
	alert("my url: " + bhclient.url);
	var url = new URL(bhclient.url + "joinSession");
	url.searchParams.append("sessionID", sessionID);
	
	fetch(url
	).then(function(response) {
		if (response) return response.json();
		else {
			bhclient.log("joinSession failed", "ERROR");
			return null;
		}
	}).then(function (resp) {
		// do something
		bhclient.sessionID = resp;
		bhclient.log("joinSession complete, resp=" + resp, "INFO");
		//callback();
		
	}).catch(function(error) {
	    bhclient.log(error,"ERROR");
	});
}



function loadItemsOld() {
	if (!doPolling) return;
	var lastLoadTime = new Date().getTime();
	$("#lastPollTime").text(new Date(lastLoadTime));
	$.ajax({
		method: "GET",
		url: apiURL + "getItems"
	}).done(function(data, textStatus, jqXHR) {
		//alert(JSON.stringify(data));
		//console.log("getItems, length=" + data.length);
		for (var k in data) { // this is supposed to be an list of atoms
			var dataItem = data[k];
			if (dataItem.ID == mySession.myID) {
				myObj = dataItem;
				$("#meData").text(JSON.stringify(myObj, null, 2));
			}
				
		
			console.log("got item: "+ JSON.stringify(dataItem) + " at " + new Date());
			updateItem(dataItem);
		}
		var thisPollTime = new Date().getTime();
		setTimeout(loadItems, lastPollTime + pollTimeout - thisPollTime); 
		
	}).fail(function(jqXHR, textStatus, errorThrown ) {
		alert("error: " + JSON.stringify(errorThrown));
		setPolling(false);
	}).always(function() {
	});
}


function loadUpdateOld() {
	if (!doPolling) return;
	
	var lastLoadTime = new Date().getTime();
	$("#lastPollTime").text(new Date(lastLoadTime));
	$.ajax({
		method: "GET",
		url: apiURL + "getUpdate"
	}).done(function(data, textStatus, jqXHR) {
		//alert(JSON.stringify(data));
		//console.log("getItems, length=" + data.length);
		$("#serverTimecode").text(data.status.timecode);
		$("#serverLoad").text(data.status.cycleLoad);
		$("#serverTick").text(data.status.cycleMsec);
		for (var k in data.items) { // this is supposed to be an list of atoms
			var dataItem = data.items[k];
			if (dataItem.ID == mySession.myID) {
				myObj = dataItem;
				$("#meData").text(JSON.stringify(myObj, null, 2));
			}
				
		
			console.log("got item: "+ JSON.stringify(dataItem) + " at " + new Date());
			updateItem(dataItem);
		}
		for (var k in data.messages) {
			var msg = data.messages[k];
			addMessage(msg);
		}
		
		var thisPollTime = new Date().getTime();
		setTimeout(loadUpdate, lastPollTime + pollTimeout - thisPollTime); 
		
	}).fail(function(jqXHR, textStatus, errorThrown ) {
		alert("error: " + JSON.stringify(errorThrown));
		setPolling(false);
	}).always(function() {
	});
}

