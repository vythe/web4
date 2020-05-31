exports.Utils = {
     
    cleanStr: function (str, replaceSpaces) {
        if (!str) return "";
        //let res = removeSpaces? str.replace(/\s+/g, '') : str.trim().replace(/\s+/g, ' ');
        let res = str.trim().replace(/\s+/g, ' ');
        res = res.replace(/[^a-zA-Z0-9\_]/g,"_");
        if (replaceSpaces !== null) {
            return res.replace(/ /g, replaceSpaces);
        }
    },
  
    formatDateTime(dt) {
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
    },

    formatDate(dt) {
      var m = dt || new Date();
      if (typeof (m.getDate) != "function") {
        console.log("formatDate FAILED: " + m);
        return "" + m;
      }
      var dateString =
      ("0" + m.getDate()).slice(-2) + "/" +
      ("0" + (m.getMonth()+1)).slice(-2) + "/" +
      m.getFullYear() 
      ;
      return dateString;
    },
  
    dateRegexp:  /^([0-9]{1,2})\/([0-9]{1,2})\/([0-9]{4})$/,
    dateTimeRegexp:  /^([0-9]{1,2})\/([0-9]{1,2})\/([0-9]{4}) ([0-9]{1,2}):([0-9]{1,2}):([0-9]{1,2})$/,
    // the full json datetime: "2020-05-31T09:38:31.587Z"
    dateTimeJsonRegexp: /^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]+Z$/,
  
    parseDateTime(dateStr) {
      if (!dateStr) return null;
  
      if (dateStr.match(Utils.dateTimeJsonRegexp)) {
        return new Date(dateStr);
      }
      let mtch = dateStr.trim().match(Utils.dateTimeRegexp);
      if (!mtch && dateStr.trim().match(Utils.dateRegexp)) {
        return this.parseDate(dateStr);      
      }
      if (mtch) {
        return new Date(+mtch[3], +mtch[2], +mtch[1], +mtch[4], +mtch[5], +mtch[6]);
      }
      else {
        return null;
       }
    },
  
    parseDate(datetimeStr) {
      if (!datetimeStr) return null;
  
      let mtch = datetimeStr.trim().match(Utils.dateRegexp);
      if (!mtch) {
        //return this.parseDate(dateStr);      
        mtch = datetimeStr.trim().match(Utils.dateTimeRegexp)
      }
      if (mtch) {
        return new Date(+mtch[3], +mtch[2], +mtch[1], 0 ,0, 0);
      } else {
        return null;
      }
    },
  
    squash: squash,

    
    squashStr(obj, depth = 0) {
        return JSON.stringify(squash(obj, depth));
    }
 

};


function     squash(obj, depth = 0) {
    if (!obj) return "";
    if ((depth || 0) < 0)  return "" + obj; // flatten the object

    if (Array.isArray(obj)) {
      let res = [];
      for (var p in obj) {
        let tp = typeof (obj[p]);
        if (tp != "function" && obj.hasOwnProperty(p)) {
        //  res[p] = "" + obj[p];
          if (tp != "object") 
            res.push(obj[p]);
          else 
            res.push( squash(obj[p], (depth || 0) - 1));
        }
      }
      return res;
    } else if (typeof(obj) == "object" && typeof(obj.hasOwnProperty) != "function") {
      console.log("squash found weird: " + JSON.stringify(obj));
      return obj;
    } else if (typeof(obj) == "object") {

      let res = {};
      let count = 0;
      if (obj) {
        for (var p in obj) {
          let tp = typeof (obj[p]);
          if (tp != "function" && obj.hasOwnProperty(p)) {
          //  res[p] = "" + obj[p];
            if (tp != "object") 
              res[p] = obj[p];
            else 
              res[p] = squash(obj[p], (depth || 0) - 1);
          }
          count++;
        }
      }
      if (count == 0) { // something immutable, like a Date
        //console.log("squash found immutable: " + JSON.stringify(obj));
        return obj;
      }
      else {
        return res;
      }
    } else { 
      return "" + obj;
    }
}
