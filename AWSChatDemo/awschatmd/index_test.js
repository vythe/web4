



var http = require('http');
var url = require('url');
var querystring = require('querystring');

http.createServer(function (req, res) {
  res.writeHead(200, {'Content-Type': 'text/html'});
  //res.end('Hello World!');
//	var q = url.parse(req.url);
var q = url.parse("http://google.com:8080/hello/test?parm=abs&otherp=dsa#ff");
var q2 = querystring.parse(q.query);
	res.end(JSON.stringify(q));
}).listen(8080); 


/*
{"protocol":null,
"slashes":null,
"auth":null,
"host":null,
"port":null,
"hostname":null,
"hash":null,
"search":"?parm=abs&otherp=dsa",
"query":"parm=abs&otherp=dsa",
"pathname":"/hello/test",
"path":"/hello/test?parm=abs&otherp=dsa",
"href":"/hello/test?parm=abs&otherp=dsa"
}


{
"protocol":"http:",
"slashes":true,
"auth":null,
"host":"google.com:8080",
"port":"8080",
"hostname":"google.com",
"hash":"#ff",
"search":"?parm=abs&otherp=dsa",
"query":"parm=abs&otherp=dsa",
"pathname":"/hello/test",
"path":"/hello/test?parm=abs&otherp=dsa",
"href":"http://google.com:8080/hello/test?parm=abs&otherp=dsa#ff"
}
*/