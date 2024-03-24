package net.vbelev.ssodemo;

import java.io.*;
import java.util.Properties;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
//import javax.servlet.ServletContext;
//import javax.servlet.http.*;
//import jakarta.servlet.annotation.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

// note that the real URL is concatenated from the Tomcat application name 'SSODemo', the application suffix 'api' in DemoServiceApp,
// the class' path (if not "/") and the method's path (if provided): http://localhost:8080/SSODemo/api/simple2/33
@Path("/") // declaration with @Path gives a special RESTful services support
//@Path("/simple") // the path can be here or on the method itself 
//@WebServlet("/") // if a class extends HttpServlet, it can be marked with @WebsServlet instead of web.xml registration
public class DemoService {

	@GET
	//@Path("/simple2") // when the @Path is set both on the class and on the method, they concatenate as /simple/simple2
	@Path("/simple2/{code}")
	@Produces(MediaType.TEXT_PLAIN)
	public String getSimpleString(@PathParam("code") String code) throws Exception
	{
		String res = "Hello, service3 with " + code + "!<br/>";
		
        Properties props = new Properties();
        
        try {
            //load a properties file from class path, inside static method
            //props.load(this.getClass().getClassLoader().getResourceAsStream("myprop1.properties"));
            String path = this.getClass().getClassLoader().getResource(".").getPath() + "/myprop.properties";
            //path = this.getClass().getClassLoader().getResource("..").getPath() + "/lib/myprop1.properties";
            //load the file handle for main.properties
            FileInputStream file = new FileInputStream(path);
java.nio.file.Path pp = java.nio.file.Path.of("%LOCALAPPDATA%/ssodemo/data.txt");
System.out.println(pp);
            //load all the properties from this file
            props.load(file);

            //we have loaded the properties, so close the file handle
            file.close();
            //get the property value and print it out
            res += props.getProperty("read") + "<br/>\n";
            res += props.getProperty("write") + "<br/>\n";

        } 
        catch (IOException ex) {
            ex.printStackTrace();
            throw ex;
        }   		
		
		return res;
	}

	/*@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getSimpleString(@QueryParam("name") String name)
	{
		return "Hello, service " + name;
	}*/
}
