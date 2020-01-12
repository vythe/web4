export class Utils {
  static squash(obj) {
    let res = {};
    if (obj) {
      for (var p in obj) {
        if (obj.hasOwnProperty(p)) {
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
}
