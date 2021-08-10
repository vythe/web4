/** this thing will take an svg element
*/
function BHField(args) {

	var config = {
		target: null,
		templateTile: null,
		cellWidth: 30,
		cellHeight: 30,
		glideLoopDelay: 32,
		glideLoopFlex: 10,
		terrainTiles: {
				"LAND": "/BotHallWeb/img/terrain/land.png",
				"STONE": "/BotHallWeb/img/terrain/stone.png"
		},
		spriteTiles: {
				"HERO": "/BotHallWeb/img/sprites/sinnoh_2.png",
				"GOLD": "/BotHallWeb/img/sprites/gold.png",
				"PAC": "/BotHallWeb/img/sprites/pac.png",
				"PORTAL": "/BotHallWeb/img/sprites/portal.png",
				"MONSTER": "/BotHallWeb/img/sprites/sinnoh_3.png"
		}
	};
	
	for (var k in args || {}) {
		config[k] = args[k];
	}
	
	config.target = $(config.target || this);
	config.templateTile = $(config.templateTile || config.target.find("image:nth(0)"));
	
	var tiles = [];
	var itemTiles = {};

	var getTile = function (x, y) {
		for (var k in tiles) {
			var tile = tiles[k];
			if (tile && tile.x == x &&  tile.y == y) {
				return tile;
			}
		}
		return null;
		/*
		var elem = $("#templateTile").clone();
		elem.attr("x", x * cellWidth);
		elem.attr("y", y * cellHeight);
		elem.attr("xlink:href", terrainTiles["LAND"]);
		
		var res = {
				x: x,
				y: y,
				terrain: "LAND",
				image: elem
		};
		$("#mainSVG").append(elem);
		tiles.push(res);
		return res;
		*/
	}

	var setTile = function (x, y, terrain) {
		var t = getTile(x, y);
		if (!t) {
			var elem = config.templateTile.clone();
			elem.attr("x", x * config.cellWidth);
			elem.attr("y", y * config.cellHeight);
			elem.attr("xlink:href", "");
			t = {
				x: x,
				y: y,
				terrain: "",
				image: elem
			};
			tiles.push(t);
			config.target.append(elem);
		}
		if (t.terrain != terrain) {
			t.terrain = terrain;
			t.image.attr("xlink:href", config.terrainTiles[terrain]);
		}
		return t;
	}
	
	/*
	var setTerrain = function (x, y, terrain) {
		var tile = getTile(x, y);
		tile.terrain = terrain;
		//tile.image.attr("xlink:href", tiles[terrain]);
		tile.image.attr("xlink:href", config.terrainTiles[terrain]);
	}*/

	var getItem = function (itemId) {
		
		return itemTiles[itemId] || null;
	}

	var setItem = function (itemId, x, y, sprite) {
		var t = itemTiles[itemId] || null;
		var elem;
		if (!t) {
			var elem = config.templateTile.clone();
			t = {
				itemId: itemId,
				svg: elem
			};
			itemTiles[itemId] = t;
			config.target.append(elem);
		} else {
			elem = t.svg;
		}
		
		if (x != t.x) {
			elem.attr("x", (x || 0) * config.cellWidth);
			t.x = x;
		}
		if (y != t.y) {
			elem.attr("y", (y || 0) * config.cellHeight);
			t.y = y;
		}

		if (sprite != t.sprite) {
			t.sprite = sprite;
			elem.attr("xlink:href", config.spriteTiles[sprite]);
		}
		return t;
	}	
	
	var deleteItem = function(id) {
		var item = getItem(id);
		if (item && item.svg) {
			item.svg.remove();
		}
		delete itemTiles[id];
	}
	
	//glideLoopDelay = 32;
	//glideLoopFlex = 10;

	var glidePool = {};
	var glideTimer = null;
	// this is the largest target TS in the pool. Nothing will glide beyond that.
	var glideLatestTS = 0;

	var glideItem = function(id, targetX, targetY, targetTS) {
		//console.log("glideItem id=" + id + " to x=" + targetX + ", y=" + targetY);
		var elem = itemTiles[id];
		if (!elem) return;
		elem = elem.svg;
		if (!elem) return;
		
		var glideRecord = {
				id: id,
				targetX: targetX * config.cellWidth,
				targetY: targetY * config.cellHeight,
				targetTS: targetTS,
				currentX: +elem.attr("x"),
				currentY: +elem.attr("y")			
		};
		glidePool[id] = glideRecord; // it will quietly replace the old glide record if there is any
		//console.log("glide id=" + id + " to " + JSON.stringify(glideRecord));
		if (targetTS > glideLatestTS) {
			glideLatestTS = targetTS;
		}
		
		if (!glideTimer) {
			glideTimer = setInterval(glideLoop, config.glideLoopDelay);
			glideLoop();
		}
	}

	var glideLoop = function() {
		var ts = new Date().getTime();
		var ts0 = ts - config.glideLoopFlex;
		var hasRecords = false;
		for (var id in glidePool) {
			//console.log("glide check id=" + id);
			var glideRecord = glidePool[id];
		//	console.dir(glideRecord);
			var elemTile = itemTiles[id];
			if (!elemTile || !elemTile.svg) {
				delete glidePool[id];
				continue;
			}
			var elem = elemTile.svg;
			
			if (elem.attr("x") != glideRecord.currentX || elem.attr("y") != glideRecord.currentY || id != glideRecord.id) {
				delete glidePool[id];
				//elem.attr("x", glideRecord.targetX);
				//elem.attr("y", glideRecord.targetY);
				console.log("glide interrupted for id=" + id + " - deleting"); 
				continue;
			}
			
			if (glideRecord.targetTS <= ts0) {
				elem.attr("x", glideRecord.targetX);
				elem.attr("y", glideRecord.targetY);
				//console.log("glide delete id=" + id + " because ts, jump to x=" + glideRecord.targetX + ", y=" + glideRecord.targetY); 
				delete glidePool[id];
				continue;
			}
			
			var steps = Math.ceil((glideRecord.targetTS - ts) / config.glideLoopDelay);
			var newX = +glideRecord.currentX + Math.floor((glideRecord.targetX - glideRecord.currentX) / steps);
			var newY = +glideRecord.currentY + Math.floor((glideRecord.targetY - glideRecord.currentY) / steps);
			//console.log("steps=" + steps + ", newX=" + newX + ", newY=" + newY);
			if (isNaN(newX) || isNaN(newY)) {
				delete glidePool[id];
				continue;			
			}
			elem.attr("x", newX);
			elem.attr("y", newY);
			glideRecord.currentX = newX;
			glideRecord.currentY = newY;
			
			hasRecords = true;		
		}
		//console.log("glide loop, hasRecords=" + hasRecords);
		if (!hasRecords || ts > glideLatestTS + config.glideLoopFlex) {
			clearInterval(glideTimer);
			glideTimer = null;		
		}
	}


	return {
		getTile: getTile,
		setTile: setTile,
		getItem: getItem,
		setItem: setItem,
		deleteItem: deleteItem,
		glideItem: glideItem
	};
}