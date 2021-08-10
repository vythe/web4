/**
 * methods pertaining to the website/user interaction,
 * separate from the bhclient
 * bhUserArgs: {apiURL, logger }
 */

function BHUser(bhUserArgs)
{
	
	var  apiURL = "/";
	var logger = function(msg, type) {
		console.log((type || "NOTYPE") + ": " + msg); 
	};
		
	if(typeof(bhUserArgs) == "object") {
		if (bhUserArgs && bhUserArgs.apiURL) {
			apiURL = bhUserArgs.apiURL + ""; // make it a string
		} else if (args) {
			apiURL = bhUserArgs + "";
		}
		
		if (bhUserArgs && typeof(bhUserArgs.logger) == "function") {
			logger = bhUserArgs.logger;
		}
	} else {
		apiURL = (bhUserArgs || "/") + "";
	}
	
	/**
	 * A simple wrapper over fetch()
	 */
	var getJSON = function(url, args, onSuccess, onError) {

		if (args)
		{
			var oUrl = new URL(url);
			for (var k in args) {
				var val = args[k] || "";
				if (typeof(val) == "object") {
					for (var k2 in val) {
						oUrl.searchParams.append(k, val[k2] || "");
					}
				} else {
					oUrl.searchParams.append(k, val);
				}
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
	// === bag API ===
	var createBag = function (userKey, bag, callback) {
		//joinSession(this, sid);
		getJSON(apiURL + "bag/create", {
			//user: bhclient.userKey,
			bag: bag,
			user: userKey
		}, function(res) {
			//alert("my apiUrl is " + this.url + ", join res=" + res);
			//console.log("bag/create result: " + res + ", callback=" + callback);
			if (typeof(callback) == "function") {
				callback(res);
			}
		},
		function (err) {
			logger("bag/create: " + err, "ERROR");
			callback(null);
		});
	}

	var getBagValue = function(bag, name, callback) {
		getJSON(apiURL + "bag/get", {
			bag : bag,
			name: name
		}, function(res) {
			if (typeof(callback) == "function") {
				callback(res);
			}
		},
		function (err) {
			logger("bag/get: " + err, "ERROR");
			callback(null);
		});
	}

	var putBagValue = function(bag, name, value, callback) {
		getJSON(apiURL +"bag/put", {
			bag : bag,
			name: name,
			value: value
		}, function(res) {
			if (typeof(callback) == "function") {
				callback(res);
			}
		},
		function (err) {
			logger("bag/put: " + err, "ERROR");
			callback(null);
		});
	}

	var bagSummary = function(bag, callback) {
		getJSON(apiURL +"bag/summary", {
			bag : bag
		}, function(res) {
			if (typeof(callback) == "function") {
				callback(res);
			}
		},
		function (err) {
			logger("bag/summary: " + err, "ERROR");
			callback(null);
		});
	}

	var getBag = function(bag, callback) {
		getJSON(apiURL +"bag", {
			bag : bag,
			userKey: userKey
		}, function(res) {
			if (typeof(callback) == "function") {
				callback(res);
			}
		},
		function (err) {
			logger("bag: " + err, "ERROR");
			callback(null);
		});
	}	

	var getUserInfo = function() {
	}
	
	return {
		createBag: createBag,
		getBagValue: getBagValue,
		putBagValue: putBagValue,
		bagSummary: bagSummary,
		getBag: getBag,
		setUserKey: function (key) { userKey = key || ""; }
	};
}
