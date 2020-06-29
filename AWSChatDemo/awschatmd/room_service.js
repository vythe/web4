let Utils = require('./utils.js').Utils;

// add the user to the room; create the room if not there
function join(context, userID, roomID, roomName) {
    if (!userID ) {
        console.log("no nothing")
        return null;
    }
    var contextRoom = null;
    var contextUser = context.users.find(q => q.userID === userID);
//console.log("all users:" + Utils.squashStr(context.users));
//console.log("contextUser: ID=" + userID + ", data: " + Utils.squashStr(contextUser, 99));
    if (roomID) {
        contextRoom = context.rooms.find(q => q.roomID === roomID);
    } 
    if (!contextRoom) {
        contextRoom = {
            roomID: null,
            roomName: null,
            queueID: null,
            userIDs: []
        };

        if (!roomName) {
            contextRoom.roomName = Utils.cleanStr("User " + userID + " at " + Utils.formatDateTime(new Date()), "_");
        } else {
            contextRoom.roomName = roomName;
        }

        var maxID = 0; // room IDs are 1-based
        context.rooms.forEach(q => {if (q.roomID > maxID) maxID = (q.roomID || 0); });
        contextRoom.roomID = maxID + 1;
        if (!contextRoom.queueID) {
            let dummySub = context.mymq.subscribe(null); // this subscription wil live long enough for somebody else to join
            contextRoom.queueID = dummySub.queueID;
        }

        context.rooms.push(contextRoom);
        console.log("created room: " + JSON.stringify(contextRoom));
    } else {
        console.log("found room: " + JSON.stringify(contextRoom));
    }
    //if (!contextUser || !contextRoom) {
    //    console.log("return all nulls");
    //    return null;
    //}

    if (userID && contextRoom.userIDs.findIndex(q => q == userID) < 0)
    {
        contextRoom.userIDs.push(userID);                
    }
    console.log("return roomid=" + contextRoom.roomID);
    return contextRoom.roomID;
}

function leave(context, userID, roomID) {
    if (!userID || !roomID) {
        return false;
    }

    let contextRoom = context.rooms.find(q => q.roomID == roomID);
    if (contextRoom) {
        contextRoom.userIDs = contextRoom.userIDs.filter(q => q != userID);
        console.log("room_service.leave, filtered out userID=" + userID + ", remains=" + JSON.stringify(contextRoom.userIDs));
        return true;
    }
    return false;
}

exports.RoomService = function(context) {
    return {
        join: function(userID, roomID, roomName) { return join (context,  userID, roomID, roomName);},

        leave: function(userID, roomID) { return leave(context, userID, roomID); },

        close: function(roomID) {
            if (!roomID) {
                return false;
            }

            let contextRoom = context.rooms.find(q => q.roomID == roomID);
            if (contextRoom) {
                context.rooms = context.rooms.filter(q => q.roomID != roomID);                
            }
            return true;
        }

    };
}