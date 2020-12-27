/*
 * API:
 /#/create?protected=Y
  * 	create a new session (optionally, protected)
  * 	returns SessionInfo with sessionKey
 /#/destroy?sessionkey=something
  * 	destroys the session
  * 	returns the same session key, or empty if the session could not be destroyed
 /#/list
  * 	returns [SessionInfo] of all existing sessions, their mobiles and clients
 /#/cycle?sessionkey=something&run=Y
  * 	starts/stops the session; Y = start, N = stop, O = one cycle
  * 	returns the engine timecode as string 
 /#/join?session=something&atom=something&sessionkey=something
  * 	create a client
  * 	session: the public session ID
  * 	atom: the mobile to control; without a controlled mobile, the client will be an observer
  * 	sessionkey: protected sessions require the session key to create a client
  * 	returns the client key (or empty if it could not be created)
 /#/leave?key=something
  * 	destroy the client and release the controlled mobile
 /#/update?key=something&full=Y
  * 	get the update bin - changes since the previous update request
  * 	if full=Y, get the update since the session start (the full data, including the map)
 /#/map?session=something&key=something
  * 	get the full map for this session, using either a public session ID or a client key
  * 	maps can be retrieved without 
 
 * Status:
 		public int timecode;
		public long updateTS;
		public int sessionID;
		public int cycleMsec;
		public int cycleLoad;
		public String sessionStatus;
		
	Message:
		public int id; 
		public String targetType;
		public int targetID;
		public String message;
		
	Mobile:
		public int id;
		public int status;
		
		public int x;
		public int y;
		public int z;
		
		// here will be some movement info
		
		public String mobiletype;
		public String name;
		
	Item
		public int id;
		public int status;
		
		public int x;
		public int y;
		public int z;
		
		public String itemtype;
		
	Cell:
		public int id;
		
		public int x;
		public int y;
		public int z;
		
		public String terrain;
		
	Buff:
		public int id;
		
		public String type;
		public int ticks;
		public long timecode;
		public boolean isCancelled;
 */

function BHClient(url) {
	return {
		// == config, status ==
		url: url,
		loopMilliseconds: 50,
		runLoop: false,
		
		// == statistics and misc ==
		info: {
			ping: 0, // server ping, tested in getUpdate()
			serverLoopMsec: 0, // server loop time, msec
			loopLoad: 0, // the loop load in %
			loopTimecode: 0, // count the loops
		},
		
		// == session props ==
		userKey: null,
		sessionId: null,
		sessionKey: null,
		clientKey: null, 
		controlledMobileID: null, 

		cells: {},
		items: {},
		mobiles: {},
		buffs: {},
		
		// "events" are not updated from the server; they are handled with subscribe() and unsubscribe() 
		events: {}, // events are stored as "eventname": [], 
				
		// == utility, events ==
		logger: function(message, messageLevel){ // the logger function, it will be called as logger(message, messageLevel);
			console.log("bhclient " + messageLevel + ": " + message);
		},
		
		log: function(message, messageLevel) {
			if (typeof(this.logger) == "function") this.logger(message, messageLevel);
		},
		
		// onUpdate is called from the update loop; you do not need to call it directly
		onUpdate: function(elementType, elementData) {

			this.trigger("update", elementType, elementData);
			
			if (elementType == "STATUS") {
				this.trigger("status", elementData);
			}
		},
				
		// the array elements are functions (or objects with invoke() methods)  
		subscribe: function(eventName, invoke) {
			if (!invoke || !eventName) 
				return;
			else if (typeof(invoke) != "function" && !(typeof(invoke) == "object" && typeof(invoke.invoke) ==  "function")) {
				throw "Invalid event subcriber for " + eventName;
			}
			else if (!this.events[eventName]) {
				this.events[eventName] = [invoke];
			} else {
				var isThere = false;
				for (var e in this.events[eventName]) {
					if (this.events[eventName] === invoke) {
						isThere = true;
						break;
					}
				}
				if (!isThere) {
					this.events[eventName].push(invoke);
				}
			}
		},
		
		unsubscribe: function(eventName, invoke) {
			if (!invoke || !eventName) return;
			var evt = this.events[eventName];
			if (!evt || !evt.length) return;
			var i;
			for (i = 0; i < evt.length; i++) {
				if (evt[i] === invoke) {
					evt.splice(i, 1);
					i--;
				}
			}			
		},
		
		trigger: function(eventName) {

			var evt = this.events[eventName];
			if (!evt || !evt.length) return;
			
			for (var e in evt) {
				var ef = (typeof(evt[e]) == "function") ? evt[e] : evt[e].invoke; 
				var args = [this];
				if (arguments && arguments.length > 1) {
					for (var a = 1; a < arguments.length; a++) {
						args.push(arguments[a]);
					}
				}
				ef.apply(null, args);
			}
		},
		
		// == session mechanics ==
		cellShifts: bhclient_cellShifts,
		
		getCell: function (x, y) { return getCell(this, x, y); },
		getClosest: function(x, y)  { return getClosest(this, x, y); },
		getMobiles: function (x, y) { return getMobiles(this, x, y); },
		
		// start/stop client polling
		cycle: function(flag) {
			if (flag && this.runLoop) {
				return;
			}
			else if (!flag) {
				this.runLoop = false;
				return;
			}
			else 
			{
				this.runLoop = flag == "O"? "once" : true;
				mainLoop(this);
			}
		},
		
		// === API ===
		getuserkey: function(callback) {
			return getuserkey(this, callback);
		},

		create: function(isProtected, callback) {
			return create(this, isProtected, callback);
		},
		
		destroy: function(sessionKey, callback) {
			return destroy(this, sessionKey, callback);
		},
		
		list: function(callback) {
			return list(this, callback);
		},
		
		join: function(sid, atomID, sKey, callback) { 
			return join(this, sid, atomID, sKey, callback); 
		},			
		
		leave: function(callback) {
			return leave(this, callback);
		},
		
		command: function(cmd, args, callback) {
			return command(this, cmd, args, callback);
		},
		
		// == start/stop server looping, if you own the session 
		sessionCycle: function (flag) { 
			return sessionCycle(this, flag); 
		}, 
		
		// === bag API ===
		/**
		 * If the provided bag key is valid, returns the same bag key;
		 * if the bag key is absent or expired, returns a new bag key;
		 * if the user key is not valid, returns ""
		 */
		createBag: function(bag, callback) {
			return createBag(this, bag, callback);
		},
				
		/**
		 * Note that name can be a comma-separated list of names
		 */
		getBagValue: function(bagKey, name, callback) {
			return getBagValue(this, bagKey, name, callback);
		},
		
		putBagValue: function(bagKey, name, value, callback) {
			return putBagValue(this, bagKey, name, value, callback);
		},
		
		/**
		 * returns a {name: value} object with all bag values
		 */
		bag: function(bagKey, callback) {
			return getBag(this, bagKey, callback);
		},		

		/**
		 * returns the bag content as a string (for debugging purposes)
		 */
		bagSummary: function(bagKey, callback) {
			return bagSummary(this, bagKey, callback);
		}		
	}
}

//  main loop using setTimeout()
function mainLoopOld(bhclient, overtime) {
	
	if (!bhclient || !bhclient.runLoop)
	{
		return;
	}
	var ts0 = new Date().getTime();
	var ts1 = ts0 + bhclient.loopMilliseconds - (overtime || 0);
	//var ts1 = new Date().getTime() + bhclient.loopMilliseconds;
	bhclient.loopTimecode++;
	
	getUpdate(bhclient, function() {
		var ts2 = new Date().getTime();
		var sleepTime = ts1 > ts2? ts1 - ts2: 1;
		bhclient.loopLoad = Math.round((ts2 - ts1 + bhclient.loopMilliseconds) * 100 / bhclient.loopMilliseconds);
		
		if (bhclient.runLoop == "once") {
			bhclient.log("cycled once and stopped", "INFO");
			bhclient.runLoop = false;
		} 
		else if (bhclient.runLoop)
		{
			//bhclient.log("sleep for " + sleepTime, "INFO");
			console.log("loop proc=" + (ts2 - ts0) + ", sleep=" + sleepTime);
			/*
			setTimeout(function() {
				var ts3 = new Date().getTime();
				var ot = 0;
				if (ts3 > ts1 + 1) {
					console.log("overtime " + (ts3 - ts1));
					ot = ts3 - ts1;
				}
				mainLoop(bhclient, ot);
			}, sleepTime);
			*/
			setTimeout(mainLoopOld, sleepTime, bhclient);
		}
	});
}

// main loop using setInterval()
function mainLoop(bhclient) {
	
	if (!bhclient || !bhclient.runLoop)
	{
		return;
	}
	var ts0 = new Date().getTime();
	var ts1 = ts0 + bhclient.loopMilliseconds;
	//var ts1 = new Date().getTime() + bhclient.loopMilliseconds;
	bhclient.loopTimecode++;
	
	var mainLoopTimer = setInterval(function() {
		ts0 = new Date().getTime();
		getUpdate(bhclient, function() {
			var ts2 = new Date().getTime();
			//var sleepTime = ts1 > ts2? ts1 - ts2: 1;
			bhclient.loopLoad = Math.round((ts2 - ts0) * 100 / bhclient.loopMilliseconds);
			//ts0 = ts2;
			
			if (bhclient.runLoop == "once") {
				bhclient.log("cycled once and stopped", "INFO");
				bhclient.runLoop = false;
			} 
			if (!bhclient.runLoop) {
				clearInterval(mainLoopTimer);
			} else {
				bhclient.loopTimecode++;
			}
		});
	}, bhclient.loopMilliseconds);
}


/**
 * Takes a promise from fetch and returns a promise with json.
 * Similar to the standard fetch/response.json() but handles an empty response correctly
 */
function fetchJson(response) {
	if (!response) {
		return new Promise((resolve, reject) => { resolve(""); });
	}
	
	return response
	.text()
	.then(function(respText) {
		//console.log("fetchJson text: ");
		//console.dir(respText);
		var retObj;
		if (!respText) 
			retObj = "";
		else 
			retObj = JSON.parse(respText);
		return new Promise((resolve, reject) => { resolve(retObj); });
	});
}

/**
 * A simple wrapper over fetch()
 */
function getJSON(url, args, onSuccess, onError) {

	if (args)
	{
		var oUrl = new URL(url);
		for (var k in args) {
			oUrl.searchParams.append(k, args[k] || "");
		}
		//console.dir(oUrl);
		url = oUrl.href;				
	}
	
	fetch(url
	).then(function(response) {
		if (response) { 
			return fetchJson(response);
		} else {
			if (typeof (onError) == "function") onError("call failed");
			//return null;
		}
	}).then(function (resp) {
		if (typeof(onSuccess) == "function") onSuccess(resp);
		
	}).catch(function(error) {
		if (typeof (onError) == "function") onError("call failed: " + error);
		//return null;
	});
}

function getJSONOld(bhclient, action, args, callback) {
	var url = bhclient.url + action;
	//console.log("getJSON called for action=" + action + ", args=" + JSON.stringify(args));
	if (args)
	{
		var oUrl = new URL(url);
		for (var k in args) {
			oUrl.searchParams.append(k, args[k] || "");
		}
		//console.dir(oUrl);
		url = oUrl.href;				
	}
	bhclient.log("getJSON: " + url, "INFO");
	
	fetch(url
	).then(function(response) {
		if (response) { 
			return fetchJson(response);
		} else {
			bhclient.log(action + " failed", "ERROR");
			callback(null);
		}
	}).then(function (resp) {
		bhclient.log(action + " complete", "INFO");
		callback(resp);
		
	}).catch(function(error) {
	    bhclient.log("getJSON failed for " + action + ": " + error, "ERROR");
	    callback(null);
	});
}

/** getUpdate is different in the way that it invokes callback(elementType, elementData) 
 * for each received element separately
 */
function getUpdate(bhclient, callback, arg1, arg2, arg3)
{
	bhclient.log("getUpdate start " + new Date(), "INFO");
	bhclient.log("url: " + bhclient.url + "update", "INFO");

	var url = buildQuery(bhclient.url + "update", {
		id: bhclient.sessionId,
		client: bhclient.clientKey
	});
	
	var pingTS = new Date().getTime();
	fetch(url
	).then(function(response) {
		bhclient.ping = new Date().getTime() - pingTS;
		
		if (response) return response.json();
		else {
			bhclient.log("getUpdate failed", "ERROR");
			return null;
		}
	}).then(function (resp) {
		// expected response: {cells, items, mobiles, buffs, messages, status} from BHSession.UpdateBin
		if (resp.cells) {
			for (var c in resp.cells) {
				var cell = resp.cells[c];
				bhclient.cells[cell.id] = cell;
				bhclient.onUpdate("CELL", cell);
			}
		}
		if (resp.items) {
			for (var i in resp.items) {
				var item = resp.items[i];
				bhclient.items[item.id] = item;
				bhclient.onUpdate("ITEM", item);
			}
		}
		if (resp.mobiles) {
			for (var m in resp.mobiles) {
				var mobile = resp.mobiles[m];
				bhclient.mobiles[mobile.id] = mobile;
				bhclient.onUpdate("MOBILE", mobile);
			}
		}
		if (resp.messages) {
			for (var s in resp.messages) {
				var msg = resp.messages[s];
				//bhclient.items[item.id] = item;
				bhclient.onUpdate("MESSAGE", msg);
			}
		}
		
		var newBuffs = {};
		var buff;
		var b;
		for (b in resp.buffs) {
			buff = resp.buffs[b];
			buff.isCancelled = b.isCancelled || false;
			newBuffs[buff.id] = buff;
		}
		
		for (var b in bhclient.buffs) {
			if (!newBuffs[b]) {
				buff = bhclient.buffs[b];
				buff.isCancelled = true;
				bhclient.onUpdate("BUFF", buff);
			}
		}
		
		bhclient.buffs = {}; //newBuffs;
		for (b in newBuffs) {
			buff = newBuffs[b];
			//buff.isCancelled = false;
			if (!buff.isCancelled) {
				bhclient.buffs[buff.id] = buff;
			}
			bhclient.onUpdate("BUFF", buff);
		}
		
		if (resp.status) {
			bhclient.serverLoopMsec = resp.status.cycleMsec || 0;
			bhclient.controlledMobileID = resp.status.controlledMobileID || null;
		}
		bhclient.onUpdate("STATUS", resp.status || {});	
		bhclient.log("getUpdate complete", "INFO");
		if (typeof(callback) == "function") {
			callback(arg1, arg2, arg3);
		}
		
	}).catch(function(error) {
	    bhclient.log(error,"ERROR");
	});
}
/** Same as BHLandscape.cellShifts;
 *  shifts are 0, 1, -1
 *   0 -> 0, 1 -> 1, -1 -> 2
 *   the index is (q.m_x - x + 3) % 3 + ((q.m_y - y + 3) % 3)* 3 + ((q.m_z - z + 3) % 3) * 9
 *   the names follow the screen convention: x = 0, y = 0 is the top left corner; however, "up" is z++, "down" is z--; 
 * */
var bhclient_cellShifts = [
	[ 0,  0,  0,  0, "P"], // 0 : self
	[ 1,  0,  0,  2, "E"], // 1: x++
	[-1,  0,  0,  1, "W"], // 2: x--
	[ 0,  1,  0,  6, "S"], // 3: y++
	[ 1,  1,  0,  8, "SE"],
	[-1,  1,  0,  7, "SW"], 
	[ 0, -1,  0,  3, "N"], // 6: y--
	[ 1, -1,  0,  5, "NE"],
	[-1, -1,  0,  4, "NW"],
	[ 0,  0,  1, 18, "U"], // 9 : z++
	[ 1,  0,  1, 20, "EU"], //10
	[-1,  0,  1, 19, "WU"],
	[ 0,  1,  1, 24, "EU"],
	[ 1,  1,  1, 26, "SEU"], 
	[-1,  1,  1, 25, "SWU"], 
	[ 0, -1,  1, 21, "NU"], // 15
	[ 1, -1,  1, 23, "NEU"],
	[-1, -1,  1, 22, "NWU"],
	[ 0,  0, -1,  9, "D"], //18 : z--
	[ 1,  0, -1, 11, "ED"],
	[-1,  0, -1, 10, "WD"], //20
	[ 0,  1, -1, 15, "SD"],
	[ 1,  1, -1, 17, "SED"],
	[-1,  1, -1, 16, "SWD"], 
	[ 0, -1, -1, 12, "ND"], 
	[ 1, -1, -1, 14, "NED"],
	[-1, -1, -1, 13, "NWD"] // 26
];

function getCell (bh, x, y) {
	
	for (var k in bh.cells) {
		var cell = bh.cells[k];
		if (cell && cell.x == x && cell.y == y) {
			return cell;
		}
	}
	return {
		id: null,
		x: x,
		y: y,
		z: 0,
		terrain: "VOID"
	};
}

function getClosest(bh, x, y) { // here, z = 0 and it returns only the first 9 closest cells
	var res = [];
	var ind;
	for (var k in bh.cells) {
		var cell = bh.cells[k];
		if (cell 
			&& cell.x >= x - 1 && cell.x <= x + 1 
			&& cell.y >= y - 1 && cell.y <= y + 1
			&& cell.z == 0
		) {
			ind = (cell.x - x + 3) % 3 + ((cell.y - y + 3) % 3)* 3 // + ((cell.z - z + 3) % 3) * 9
			res[ind] = cell;
		}
	}
	for (ind = 0; ind < 9; ind++) {
		if (!res[ind]) {
			res[ind] = {
				id: null,
				x: x + bhclient_cellShifts[ind][0],
				y: y + bhclient_cellShifts[ind][1],
				z: 0,
				terrain: "VOID"
			};
		}
	}
	return res;
}

function getMobiles(bh, x, y) {
	var res = [];
	for (var k in bh.mobiles) {
		var mob = bh.mobiles[k];
		if (mob.x == x && mob.y == y) {
			res.push(mob);
		}
	}
	//console.log("getMobiles, x=" + x + ", y=" + y + ", res.length=" + res.length);
	return res;
}

//======= API functions =========

/** 
 * get user key (aka login)
 */
function getuserkey(bhclient, callback) {
	getJSON(bhclient.url + "getuserkey", {
	}, function(res) {
		if (typeof(callback) == "function") {
			callback(res);
		}
	},
	function (err) {
		bhclient.log("getuserkey: " + err, "ERROR");
		callback(null);
	});
}

/** 
 * create session
 */
function create(bhclient, isProtected, callback) {
	getJSON(bhclient.url + "create", {
		user: bhclient.userKey,
		protected: isProtected
	}, function(res) {
		if (typeof(callback) == "function") {
			callback(res);
		}
	},
	function (err) {
		bhclient.log("create: " + err, "ERROR");
		callback(null);
	});
}

/** 
 * destroy session
 */
function destroy(bhclient, sessionKey, callback) {
	getJSON(bhclient.url + "destroy", {
		session: sessionKey
	}, function(res) {
		if (typeof(callback) == "function") {
			callback(res);
		}
	},
	function (err) {
		bhclient.log("destroy: " + err, "ERROR");
		callback(null);
	});
}

/** 
 * list sessions
 */
function list(bhclient, callback) {
	getJSON(bhclient.url + "list", {
	}, function(res) {
		if (typeof(callback) == "function") {
			callback(res);
		}
	},
	function (err) {
		bhclient.log("list: " + err, "ERROR");
		callback(null);
	});
}


function sessionCycle(bhclient, flag) {
	if (bhclient.sessionId && bhclient.sessionKey) {
		getJSON(bhclient.url + "cycle", {
			session: bhclient.sessionKey,
			run : flag? (flag == "O" ? "O" : "Y") : "N"
		}, 
			function(res) {
			//alert("my apiUrl is " + this.url + ", join res=" + res);
			bhclient.log("cycleMode returns timecode=" + res, "INFO");
		}, 
		function(err) {
			bhclient.log("cycle: " + err, "ERROR");
		});
	} else {
		bhclient.log("No session owned");
	}
}

function join(bhclient, sid, mobileID, sKey, callback) {
	//joinSession(this, sid);
	getJSON(bhclient.url + "join", {
		sid : sid,
		atom: mobileID,
		session: sKey
	}, 
	function(res) {
		//alert("my apiUrl is " + this.url + ", join res=" + res);
		bhclient.clientKey = res;
		bhclient.trigger("joinSession");
		if (typeof(callback) == "function") {
			callback(res);
		}
	}, 
	function(err) {
		bhclient.log("join: " + err, "ERROR");
	});
}

function leave(bhclient, callback) {
	//joinSession(this, sid);
	getJSON(bhclient.url + "leave", {
		client: bhclient.clientKey
	}, function(res) {
		//alert("my apiUrl is " + this.url + ", join res=" + res);
		bhclient.clientKey = "";
		bhclient.trigger("leaveSession");
		if (typeof(callback) == "function") {
			callback(res);
		}
	},
	function (err) {
		bhclient.log("leave: " + err, "ERROR");
		callback(null);
	});
}

function command(bhclient, cmd, args, callback) {
	if (!args) {
		args = [];
	}
	else if (args && typeof(args) != "object") {
		args = [args];
	}
		
	getJSON(bhclient.url + "command", {
		client: bhclient.clientKey,
		cmd: cmd,
		args: args
	}, function(res) {
		if (typeof(callback) == "function") {
			callback(res);
		}
	},
	function (err) {
		bhclient.log("command: " + err, "ERROR");
		callback(null);
	});
}

// === bag API ===
function createBag(bhclient, bag, callback) {
	//joinSession(this, sid);
	getJSON(bhclient.url + "bag/create", {
		user: bhclient.userKey,
		bag: bag
	}, function(res) {
		//alert("my apiUrl is " + this.url + ", join res=" + res);
		//console.log("bag/create result: " + res + ", callback=" + callback);
		if (typeof(callback) == "function") {
			callback(res);
		}
	},
	function (err) {
		bhclient.log("bag/create: " + err, "ERROR");
		callback(null);
	});
}

function getBagValue(bhclient, bag, name, callback) {
	getJSON(bhclient.url + "bag/get", {
		bag : bag,
		name: name
	}, function(res) {
		if (typeof(callback) == "function") {
			callback(res);
		}
	},
	function (err) {
		bhclient.log("bag/get: " + err, "ERROR");
		callback(null);
	});
}

function putBagValue(bhclient, bag, name, value, callback) {
	getJSON(bhclient.url +"bag/put", {
		bag : bag,
		name: name,
		value: value
	}, function(res) {
		if (typeof(callback) == "function") {
			callback(res);
		}
	},
	function (err) {
		bhclient.log("bag/put: " + err, "ERROR");
		callback(null);
	});
}

function bagSummary(bhclient, bag, callback) {
	getJSON(bhclient.url +"bag/summary", {
		bag : bag
	}, function(res) {
		if (typeof(callback) == "function") {
			callback(res);
		}
	},
	function (err) {
		bhclient.log("bag/summary: " + err, "ERROR");
		callback(null);
	});
}

function getBag(bhclient, bag, callback) {
	getJSON(bhclient.url +"bag", {
		bag : bag
	}, function(res) {
		if (typeof(callback) == "function") {
			callback(res);
		}
	},
	function (err) {
		bhclient.log("bag: " + err, "ERROR");
		callback(null);
	});
}

// == general purpose javascript helpers ==

function formatDateTime(dt) {
    var m = dt || new Date();
    var dateString =
    ("0" + m.getDate()).slice(-2) + "/" +
    ("0" + (m.getMonth()+1)).slice(-2) + "/" +
    m.getFullYear() + " " +
    ("0" + m.getHours()).slice(-2) + ":" +
    ("0" + m.getMinutes()).slice(-2) + ":" +
    ("0" + m.getSeconds()).slice(-2)
    ;
    return dateString;
}


function buildQuery(url, args) {
	if (!args) return url;
	var isFirst = (url || "").indexOf("?") < 0;
	if (typeof(args) != "object") 
		return url + (isFirst? "?" : "&") + args;
	var res = url;
	for (var k in args) {
		res += isFirst? "?" : "&";
		isFirst = false;
		res += encodeURIComponent(k) + "=" + encodeURIComponent(args[k]);
	}
	return res;
}

function setCookie(name, value) {
	document.cookie = name + "="+ value + ";";
}
function getCookie(name) {
    var b = document.cookie.match('(^|;)\\s*' + name + '\\s*=\\s*([^;]+)');
    return b ? b.pop() : '';
}


function getQueryParam(name, searchString) {
	var match = (new RegExp('[?&]'+encodeURIComponent(name)+'=([^&]*)')).exec(searchString || location.search)
	   if(match)
	      return decodeURIComponent(match[1]);
	return null;
}

