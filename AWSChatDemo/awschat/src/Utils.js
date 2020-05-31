export class Utils {  

  static formatDateTime(dt) {
      var m = dt || new Date();
      if (typeof (m.getDate) != "function") {
        console.log("formatDateTime FAILED: " + m);
        return "" + m;
      }
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

  static formatDate(dt) {
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
  }

  static dateRegexp =  /^([0-9]{1,2})\/([0-9]{1,2})\/([0-9]{4})$/;
  static dateTimeRegexp =  /^([0-9]{1,2})\/([0-9]{1,2})\/([0-9]{4}) ([0-9]{1,2}):([0-9]{1,2}):([0-9]{1,2})$/;

  static parseDateTime(dateStr) {
    if (!dateStr) return null;

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
  }

  static parseDate(datetimeStr) {
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
  }

  static squash(obj, depth = 0) {
        if (!obj) return "";
        if ((depth || 0) < 0)  return "" + obj; // flatten the object
    
        if (Array.isArray(obj)) {
          let res = [];
          for (let p in obj) {
            let tp = typeof (obj[p]);
            if (tp != "function" && obj.hasOwnProperty(p)) {
            //  res[p] = "" + obj[p];
              if (tp != "object") 
                res.push(obj[p]);
              else 
                res.push( Utils.squash(obj[p], (depth || 0) - 1));
            }
          }
          return res;
          
        } else if (typeof(obj) == "object") {
    
          let res = {};
          let count = 0;
          if (obj) {
            for (let p in obj) {
              let tp = typeof (obj[p]);
              if (tp != "function" && obj.hasOwnProperty(p)) {
              //  res[p] = "" + obj[p];
                if (tp != "object") 
                  res[p] = obj[p];
                else 
                  res[p] = Utils.squash(obj[p], (depth || 0) - 1);
              }
              count++;
            }
          }
          if (count == 0) { // something immutable, like a Date
            console.log("squash found immutable: " + JSON.stringify(obj));
            return obj;
          }
          else {
            return res;
          }
        } else { 
          return "" + obj;
        }
    }
    
    static squashStr(obj, depth = 0) {
        return JSON.stringify(Utils.squash(obj, depth));
    }
 
    static cleanStr(str, removeSpaces) {
      if (!str) return "";
      let res = removeSpaces? str.replace(/\s+/g, '') : str.trim().replace(/\s+/g, ' ');

      return res.replace(/[^\w] /g,"_");
    }
}