export class Utils {
  static squash(obj) {
    let res = {};
    if (obj) {
      for (var p in obj) {
        if (typeof (obj[p]) != "function" || obj.hasOwnProperty(p)) {
          res[p] = "" + obj[p];
        }
      }
    }
    return res;
  }

  static cssURL(imageFile) {
    return "url(" +process.env.PUBLIC_URL + imageFile + ")";
  }

  static localURL(href) {
    return process.env.PUBLIC_URL + href;
  }

  static colourApproval(val) {
    let valColour = "#202020";
    if (val > 0.5) {
    valColour = "#" 
    + "20"
    + ((val - 0.5) * 500).toString(16).padStart(2, "0").substring(0, 2) 
    + "20";
    } else {
      valColour = "#" 
      + ((0.5 - val) * 500).toString(16).padStart(2, "0").substring(0, 2) 
      + "2020";  
    }

    return valColour;
  }

  static colourAffinity(val) {
    let valColour = "#202020";
    valColour = "#" 
    + "20"
    + ((val) * 255).toString(16).padStart(2, "0").substring(0, 2) 
    + "20";

    return valColour;
  }
}
