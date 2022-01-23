function submitForm(formid, url, method, args) {

	var form = $("#" + formid);
	if (form.length == 0) {
		form = $("<form/>");
		form.hide();
		$("body").append(form);
	} else {
		form.empty();
	}
	form.attr("action", url);
	form.attr("method", method);
	if (args && typeof(args) == "object") {
		for (var k in args) {
			if (args[k]) {
				var inp = $("<input/>").attr("name", k).attr("value", args[k]).attr("type", "hidden");
				form.append(inp);
			}
		}
	}
	form[0].submit();
}

function formatDate(dt) {
    var m = dt || new Date();
    var dateString =
    ("0" + m.getDate()).slice(-2) + "/" +
    ("0" + (m.getMonth()+1)).slice(-2) + "/" +
    m.getFullYear()
    ;
    return dateString;
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

function formatTime(dt) {
    var m = dt || new Date();
    var dateString =
    ("0" + m.getHours()).slice(-2) + ":" +
    ("0" + m.getMinutes()).slice(-2) + ":" +
    ("0" + m.getSeconds()).slice(-2)
    ;
    return dateString;
}

