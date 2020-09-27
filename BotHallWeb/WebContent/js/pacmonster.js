/**
 * Put the monster logic here: some client data and the onUpdate handler.
 * The bhclient will need an event provider, and it will probably be called onTick
 */


var pacmanValidDirs = [1,2,3,6];

/** 
 * Monsters may choose a new direction if 
 a) it is not currently moving;
 b) the current direction is blocked;
 c) it is on a crossroads (more than two exits).
 */
function mayTurnMonster(closest, currentDir) {

	//if (currentDir != 0) return false;

	if (currentDir == 0) return true;
	if (!closest[currentDir]) {
		console.log("INVALID currentDir: " + currentDir);
		return true;
	}
	
	if (closest[currentDir].terrain != "LAND") return true;
	
	var cnt = 0;
	for (var k = 0; k < pacmanValidDirs.length && cnt < 3; k++) {
		if (closest[pacmanValidDirs[k]].terrain == "LAND") cnt++;		
	}
	return cnt != 2;		 
}
 
function randomDir(closest) {
	var cnt = 0;
	for (var k = 0; k < pacmanValidDirs.length; k++) {
		if (closest[pacmanValidDirs[k]].terrain == "LAND") cnt++;		
	}
	//console.log("randomDir, cnt=" + cnt);

	if (cnt == 0) return 0;
	var dir1 = Math.floor(Math.random() * cnt + 1);
	//console.log("randomDir, dir1=" + dir1);
	cnt = 0;
	for (var k = 0; k < pacmanValidDirs.length; k++) {
		if (closest[pacmanValidDirs[k]].terrain == "LAND") cnt++;
		if (cnt == dir1) return pacmanValidDirs[k];
	}
	return 0;
}

function shamblerMonster() {
	
	var res = {
		itemId : null,
		lastPos: {},
		invoke: null //cowMonsterMove.bind(res) //.bind(this)
	};
	res.invoke = shamblerMonsterMove.bind(res);
	return res;
}

function shamblerMonsterMove(mybh, statusData) {
	//console.log("console = cowInvoke, elementType=" + elementType);
	//console.log("cowInvoke, this="); // + JSON.stringify(this));
	//console.dir(this);
	//if (elementType != "STATUS") return;
	var mobileId = this.itemId; // || elementData.controlledMobileID;
	if (!mobileId) {
		//mybh.log("cowInvoke: no controlled Id, returning");
		//mybh.log("cow did not find the monster, id=" + mobileId, "ERROR");
	}
	var me = mybh.mobiles[mobileId];
	if (!me) {
		//mybh.log("cow did not find the monster, id=" + mobileId, "INFO");
		//console.log("console = cow did not find the monster, id=" + mobileId);
		return;
	} else {
		//mybh.log("cow found monster: " + JSON.stringify(me), "INFO");
	}
	var closest = mybh.getClosest(me.x, me.y);
	var closestTiles = closest.map(q => q.terrain);
	var openTiles = [];
	for (var p = 0; p < pacmanValidDirs.length; p++) {
		if (closestTiles[pacmanValidDirs[p]] == "LAND") openTiles.push(mybh.cellShifts[pacmanValidDirs[p]][4]);
	}
	$("#infoString").text("My id=" + me.id + ", x=" + me.x + ", y=" + me.y
			+ ", dir=" + me.dir
			+ ", open=" + JSON.stringify(openTiles)
			);
	
	var lastPos = this.lastPos || {};
	//console.log("cow, lastPos=" + JSON.stringify(lastPos));
	if (me.dir != 0 && me.x == lastPos.x && me.y == lastPos.y) {
		// don't turn until at least one step is done
	}
	else if (mayTurnMonster(closest, me.dir)) {
		var newDir = randomDir(closest);
		//console.log("on turn, newDir=" + newDir);
		if (newDir > 0 && newDir != me.dir) {
			this.lastPos = {x: me.x, y: me.y};
			moveIt(newDir, mobileId);
		}
	} else {
		//console.log("on turn - not allowed, current dir=" + me.dir);
	}
}



function bestDir(mybh, closest, dx, dy) {
	var res = 0;
	var resProd = -1000;
	for (var k = 0; k < pacmanValidDirs.length; k++) {
		if (closest[pacmanValidDirs[k]].terrain == "LAND") {
			
			var shift = mybh.cellShifts[pacmanValidDirs[k]];
			//console.log("bestDir, closest[0]
			var mobs = mybh.getMobiles(closest[0].x + shift[0], closest[0].y + shift[1]);
			if (mobs.length == 0) {
			
				var testProd = shift[0] * dx + shift[1] * dy;
				if (testProd > resProd) {
					res = pacmanValidDirs[k];
					resProd = testProd;
				}
			}
		}
	}
	
	return res;
}


function houndMonster() {
	
	var res = {
		itemId : null,
		lastPos: {},
		invoke: null //cowMonsterMove.bind(res) //.bind(this)
	};
	res.invoke = houndMonsterMove.bind(res);
	return res;
}

function houndMonsterMove(mybh, statusData) {
	//console.log("console = cowInvoke, elementType=" + elementType);
	//console.log("cowInvoke, this="); // + JSON.stringify(this));
	//console.dir(this);
	//if (elementType != "STATUS") return;
	//console.log("hound invoked, itemId=" + this.itemId + ", tick=" + mybh.loopTimecode);
	var ind;
	var mobileId = this.itemId; // || elementData.controlledMobileID;
	if (!mobileId) {
		//mybh.log("cowInvoke: no controlled Id, returning");
		//mybh.log("cow did not find the monster, id=" + mobileId, "ERROR");
		console.log("hound does not have mobileId");
	}
	var me = mybh.mobiles[mobileId];	
	if (!me) {
		//mybh.log("cow did not find the monster, id=" + mobileId, "INFO");
		//console.log("console = cow did not find the monster, id=" + mobileId);
		console.log("hound did not find the monster, id=" + mobileId);
		return;
	} else {
		//mybh.log("cow found monster: " + JSON.stringify(me), "INFO");
	}
	
	var lastPos = this.lastPos || {};
	//console.log("cow, lastPos=" + JSON.stringify(lastPos));
	if (me.dir != 0 && me.x == lastPos.x && me.y == lastPos.y) {
		// don't turn until at least one step is done
		//console.log("hound defer turning, lastpos=" + JSON.stringify(lastPos));
		return;
	}
	
	var hero = null;
	for (ind in mybh.mobiles) {
		if (mybh.mobiles[ind].mobiletype == "HERO") {
			hero = mybh.mobiles[ind];
			break;
		}
	}
	if (!hero) {
		console.log("hound did not find the hero=");
		return; //nobody to chase
	}
	
	var closest = mybh.getClosest(me.x, me.y);
	var closestTiles = closest.map(q => q.terrain);
	var openTiles = [];
	for (ind = 0; ind < pacmanValidDirs.length; ind++) {
		if (closestTiles[pacmanValidDirs[ind]] == "LAND") openTiles.push(mybh.cellShifts[pacmanValidDirs[ind]][4]);
	}
	$("#infoString").text("My id=" + me.id + ", x=" + me.x + ", y=" + me.y
			+ ", dir=" + me.dir
			+ ", open=" + JSON.stringify(openTiles)
			);
	
	if (mayTurnMonster(closest, me.dir)) {
		var newDir = bestDir(mybh, closest, hero.x - me.x, hero.y - me.y);
		console.log("hound id=" + me.id + " on turn, newDir=" + newDir + ", tick=" + mybh.loopTimecode);
		if (newDir == 0) {
			console.log("closest: " + closest);
		}
		if (newDir > 0 && newDir != me.dir) {
			this.lastPos = {x: me.x, y: me.y};
			moveIt(newDir, mobileId);
		}
	} else {
		console.log("hound id=" + me.id + ": turn not allowed, current dir=" + me.dir + ", tick=" + mybh.loopTimecode);
	}
}

function moveIt(dir, id) {
	/*
	$.ajax({
		method: "GET",
		url: apiURL + "moveit",
		data: {direction: dir},
		dataType: "text"
	}).done(function(data, textStatus, jqXHR) {
		//alert("moveit: " + data);
		console.log("moveit: " + data + " at " + new Date());
	}).fail(function(jqXHR, textStatus, errorThrown ) {
		alert("error: " + JSON.stringify(errorThrown));
	}).always(function() {
	});
	*/
	getJSON(mybh, "moveit", {direction: dir, id: id || ""}, function() {
		console.log("posted moveit, dir=" + dir + ", id=" + id);
	});
}