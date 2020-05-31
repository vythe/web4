let Utils = require('./utils.js').Utils;

exports.Controller = function(context) {

    let RoomService = require('./room_service').RoomService(context);

    return {

        // returns enhanced room records with users[] of full user info
        allrooms: function(req, res) {
            if (!context.rooms) {
                context.rooms = []
            }
            let resData = [];
            console.log("allrooms, context.rooms=" + Utils.squashStr(context.rooms, 99));
            console.log("allrooms, context.users=" + Utils.squashStr(context.users, 99));
            context.rooms.forEach(r => {
                let resRoom = {
                    roomID: r.roomID,
                    roomName: r.roomName,
                    userIDs: r.userIDs,
                    users: []
                };
                if (r.userIDs) {
                    r.userIDs.forEach(q => {
                        let user = context.users.find(u => u.userID == q);
                        //console.log("")
                        if (user) {
                            resRoom.users.push(user);
                        }
                    });
                }
                resRoom.users = resRoom.users.sort((a, b) => { return a.login > b.login; });
                resData.push(resRoom);
            });
            res.status(200).send(resData);	
        },

        // takes optional id (roomID)
        roomusers: function (req, res) {
            let room = context.rooms.find(q => q.roomID = req.query.id);
            resData = [];
            if (room && room.roomusers) {
                room.roomusers.forEach(q => {
                    let user = context.users.find(u => u.userID == q);
                    if (user) {
                        resData.push(user);
                    }
                });
            }
            resData = resData.sort((a, b) => { return a.login > b.login; });
            res.status(200).send(resData);	
        }, 

        // takes optional id (roomID), works similar to allrooms
        getroom: function (req, res) {
            let room;
            if (req.query.id) {
                room = context.rooms.find(q => q.roomID == req.query.id);
            }
            else {
                room = context.rooms.find(q => q.roomID == context.roomID);
            }

            if (!room) {
                room = {
                    users: []
                };
            }

            let resRoom = {
                roomID: room.roomID,
                roomName: room.roomName,
                userIDs: room.userIDs,
                users: []
            };
            //if (room && room.roomID) {
            //    resRoom.room = context.rooms.find(q => q.roomID == resRoom.roomID);
            //}

            if (room && room.userIDs) {
                room.userIDs.forEach(q => {
                    let user = context.users.find(u => u.userID == q);
                    if (user) {
                        resRoom.users.push(user);
                    }
                });
                resRoom.users = resRoom.users.sort((a, b) => { return a.login > b.login; });
            }
            res.status(200).send(resRoom);	
        }, 

        // takes either id (roomId), name (roomName) or neither        
        // returns { room ID, room name, subscription ID }
        join: function(req, res) {
            let error = "";
            let room = null;
            let roomName = Utils.cleanStr(req.query.name);

            if (req.session.userID === null) {
            //context.userID = 0; // debug
                res.status(500).send({Error: "Need to login first"});
                return;
            }

            if (req.session.userID === null) {
                error = "You need to login first: " + context.userID;
            }
            else if (+ req.query.id > 0) {
                room = context.rooms.find(q => q.roomID == req.query.id);
                if (!room) {
                    error = "Room #" + context.rooms.id + " not found";
                }
            }
            else if (roomName) {
                room = context.rooms.find(q => Utils.cleanStr(q.roomName) == roomName);
                if (!room) {
                    room = {
                        roomID: null,
                        roomName: roomName,
                        queueID: null,
                        users: []
                    }
                }
            }
            else
            {
                room = { // going to be a new room,
                    roomID: null,
                    roomName: null,
                    queueID: null,
                    users: []
                };
            }
            if (!error) {
                room.roomID = RoomService.join(req.session.userID, room.roomID, room.roomName);
                if (!room.roomID) {
                    error = "Failed to join the room";
                }

            }
            if (error) {
                res.status(500).send({Error: error});
            }
            else{
                if (req.session.roomID != room.roomID) {
                    RoomService.leave(req.session.userID, req.session.roomID);
                }
                req.session.roomID = room.roomID;

                console.log("Rc.join, got roomid=" + room.roomID + ", all rooms: " + Utils.squashStr(context.rooms, 99));
                room = context.rooms.find(q => q.roomID === room.roomID);
                let sub = context.mymq.subscribe(room.roomID);
                //console.log("RC.join, found ret room=" + Utils.squashStr(room, 99));
                //console.log("join Room " + room.roomID + ", sub: " + JSON.stringify(sub));
                let resData = {
                    roomID: room.roomID,
                    roomName: room.roomName,
                    subscriptionID: sub
                };
                console.log("joinRoom, resDAta="  + JSON.stringify(resData));
                res.status(200).send(resData);	
            }
        },

        leave: function(req, res) {
            if (context.roomID && context.userID) {
                RoomService.leave(context.userID, context.roomID);
            }
            context.roomID = null;
            res.status(200).send({status: "OK"});
        }

    };
}