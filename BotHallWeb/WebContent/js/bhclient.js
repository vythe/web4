/*
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
		loopMilliseconds: 100,
		runLoop: false,
		
		// == statistics and misc ==
		info: {
			ping: 0, // server ping, tested in getUpdate()
			serverLoopMsec: 0, // server loop time, msec
			loopLoad: 0, // the loop load in %
			loopTimecode: 0, // count the loops
		},
		
		// == session props ==
		sessionID: null,
		sessionKey: null,
		clientKey: null, 
		controlledMobileID: null, 

		cells: {},
		items: {},
		mobiles: {},
		buffs: {},
		
		
		// == utility, events ==
		logger: function(message, messageLevel){ // the logger function, it will be called as logger(message, messageLevel);
			console.log("bhclient " + messageLevel + ": " + message);
		},
		
		log: function(message, messageLevel) {
			if (typeof(this.logger) == "function") this.logger(message, messageLevel);
		},
		
		
		onUpdate: function(elementType, elementData) {
			//if (elementType == "STATUS") {
			//	this.log("update status: " + JSON.stringify(elementData));
			//}
			this.trigger("update", elementType, elementData);
			
			if (elementType == "STATUS") {
				this.trigger("status", elementData);
			}
		},
				
		events: {}, // events are stored as "eventname": [], 
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
			//console.log("trigger arguments: " + JSON.stringify(arguments));
			var evt = this.events[eventName];
			if (!evt || !evt.length) return;
			//console.log("bhclient trigger " + eventName + ", sub count=" + evt.length);
			for (var e in evt) {
				var ef = (typeof(evt[e]) == "function") ? evt[e] : evt[e].invoke; 
				//console.log("subscriber " + e + ": " + ef);
				var args = [this];
				//if (arguments) args = args.concat(arguments);
				if (arguments && arguments.length > 1) {
					for (var a = 1; a < arguments.length; a++) {
						args.push(arguments[a]);
					}
				}
				//console.log("call ef for " + JSON.stringify(args));
				//console.log("call ef for count=" + args.length);
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
		
		// == start/stop server looping, if you own the session 
		bhcycle: function(flag) {
			if (this.sessionID && this.sessionKey) {
				getJSON(this, "cycle", {
					id: this.sessionID,
					key: this.sessionKey,
					run : flag? (flag == "O" ? "O" : "Y") : "N"
				}, function(res) {
					//alert("my apiUrl is " + this.url + ", join res=" + res);
					this.log("cycleMode returns timecode=" + res, "INFO");
				}.bind(this));
			} else {
				this.log("No session owned");
			}
		},
		
		joinSession: function(sid, atomID, callback) {
			//joinSession(this, sid);
			getJSON(this, "joinSession", {
				id : sid,
				atom: atomID
			}, function(res) {
				//alert("my apiUrl is " + this.url + ", join res=" + res);
				this.clientKey = res;
				this.trigger("joinSession");
				if (typeof(callback) == "function") {
					callback(res);
				}
			}.bind(this));
		},
		
		leaveSession: function(callback) {
			//joinSession(this, sid);
			getJSON(this, "leaveSession", {
				id : this.sessionID,
				key: this.clientKey
			}, function(res) {
				//alert("my apiUrl is " + this.url + ", join res=" + res);
				this.clientKey = "";
				this.trigger("leaveSession");
				if (typeof(callback) == "function") {
					callback(res);
				}
			}.bind(this));
		},
		
		getViewbagValue: function(bag, name, callback) {
			getJSON(this, "getval", {
				bag : bag,
				name: name
			}, function(res) {
				if (typeof(callback) == "function") {
					callback(res);
				}
			}.bind(this));
		},
		
		setViewbagValue: function(bag, name, value, callback) {
			getJSON(this, "putval", {
				bag : bag,
				name: name,
				value: value
			}, function(res) {
				if (typeof(callback) == "function") {
					callback(res);
				}
			}.bind(this));
		},
		
		action: function(action, args, callback) {
			if (!args) {
				args = [];
			}
			else if (args && typeof(args) != "object") {
				args = [args];
			}
				
			getJSON(this, "action", {
				id : this.sessionID,
				key: this.clientKey,
				action: action,
				args: args
			}, function(res) {
				if (typeof(callback) == "function") {
					callback(res);
				}
			}.bind(this));
		},
		/*
		joinMobile: function(name) {
			getJSON(this, "joinMobile", {name: name}, function(res) {
				//alert("my apiUrl is " + this.url + ", join res=" + res);
				//this.sessionID = res;
			}.bind(this));
		}
		*/
		/*
		, reportUrl: function() {
			return this.url;
		},
		useCallback: function(callback) {
			callback();
		},
		reportUrl2: function() {
			setTimeout(function() {
				alert("self-report: " + url);
			}, 10);
		},
		reportUrl3: function() {
			this.useCallback(function() {
				alert("async3: " + this.url + ", func: " + this.reportUrl());
			}.bind(this));
		}
		*/
	}
}

function mainLoop(bhclient) {
	
	if (!bhclient || !bhclient.runLoop)
	{
		return;
	}
	var ts1 = new Date().getTime() + bhclient.loopMilliseconds;
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
			bhclient.log("sleep for " + sleepTime, "INFO");
			setTimeout(function() {
				mainLoop(bhclient);
			}, sleepTime);
		}
	});
}


/**
 * Takes a promise from fetch and returns a promise with json.
 * Similar to the standard fetch/response.json() but handles empty response correctly
 * @param response
 * @returns
 */
function fetchJson(response) {
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

function getJSON(bhclient, action, args, callback) {
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
function getUpdate(bhclient, callback)
{
	bhclient.log("getUpdate start " + new Date(), "INFO");
	bhclient.log("url: " + bhclient.url + "getUpdate", "INFO");
	//var url = new URL(bhclient.url + "getUpdate");
	//url.searchParams.append("sessionID", bhclient.sessionID);
	var url = buildQuery(bhclient.url + "getUpdate", {
		id: bhclient.sessionID,
		pwd: bhclient.clientKey
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
			callback();
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
	if (typeof(args) != "object") return url + "?" + args;
	var isFirst = true;
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

function bagSummary(bhclient, bag) {
	if (!bag) {
		bag = getCookie("viewbag");
	}
	if (!bag) {
		console.log("No bag token");
	} else {
		fetch(bhclient.url + "bagsummary?bag=" + bag
		).then(function(response) {
			if (response) {
				var fetchVal =  fetchJson(response);
				//console.log("bagsummary then fetchVal: ");
				//console.dir(fetchVal);
				return fetchVal;
			}
			else {
				bhclient.log("viewbag failed", "ERROR");
			}
		}).then(function (resp) {
			console.log("viewbag " + bag);
			console.dir(resp);
			
		}).catch(function(error) {
		    bhclient.log("getJSON failed for viewbag: " + error, "ERROR");
		    callback(null);
		});
	}
}

function getQueryParam(name, searchString) {
	var match = (new RegExp('[?&]'+encodeURIComponent(name)+'=([^&]*)')).exec(searchString || location.search)
	   if(match)
	      return decodeURIComponent(match[1]);
	return null;
}