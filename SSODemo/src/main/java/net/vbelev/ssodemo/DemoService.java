package net.vbelev.ssodemo;

import java.io.*;
import java.nio.file.Files;
import java.util.Properties;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
//import javax.servlet.ServletContext;
//import javax.servlet.http.*;
//import jakarta.servlet.annotation.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import net.vbelev.sso.core.*;
import net.vbelev.utils.Utils;

// note that the real URL is concatenated from the Tomcat application name 'SSODemo', the application suffix 'api' in DemoServiceApp,
// the class' path (if not "/") and the method's path (if provided): http://localhost:8080/SSODemo/api/simple2/33
@Path("/") // declaration with @Path gives a special RESTful services support
//@Path("/simple") // the path can be here or on the method itself 
//@WebServlet("/") // if a class extends HttpServlet, it can be marked with @WebsServlet instead of web.xml registration
public class DemoService {

    private static final SSOAuthService _authService;
    
    static 
    {
        String initToken = null;
        String failoverPath = null;
        String homeFolder = ".";
        try
        {
            // from inside a servlet, the class loader points to a war and allows to read web-info/lib.
            // from inside junit, when loading Jetty, the class loader points to build/classes, 
            // there is no web-info, and access to ".." is denied.
            // What about Tomcat?
            File propFile;
            java.net.URL servletResource = DemoService.class.getClassLoader().getResource("..");
            if (servletResource != null)
            {
                propFile = new File(servletResource.getPath() + "/lib/ssodemo.properties");
            }
            else
            {
                servletResource = DemoService.class.getClassLoader().getResource(".");
                propFile = new File(servletResource.getPath() + "/ssodemo_test.properties");
            }
            if (propFile.exists() && propFile.isFile())
            {
                Properties p = new Properties();
                p.load(new FileInputStream(propFile));
                initToken = p.getProperty("init_token");
                failoverPath = p.getProperty("failover_path");
            }            
            homeFolder = System.getenv("APPDATA");
            if (Utils.isBlank(homeFolder))
                homeFolder = System.getProperty("user.home");
            if (Utils.isBlank(homeFolder))
                homeFolder = java.nio.file.Path.of(".").toAbsolutePath().toString();
                    
        }
        catch (IOException x)
        {
        }
        if ("NONE".equals(failoverPath))
        {
            failoverPath = "";
        }
        else if (failoverPath == null || "AUTO".equals(failoverPath))
        {
            failoverPath = java.nio.file.Path.of(homeFolder, "SSOFailoverTest").toAbsolutePath().toString();
        }
        else if (!java.nio.file.Path.of(failoverPath).isAbsolute())
        {
            failoverPath =java.nio.file.Path.of(homeFolder, failoverPath).toString(); 
        }
        SSOAuthService auth;
        if (Utils.isBlank(failoverPath))
        {
            auth = new SSOAuthService();
        }
        else
        {
            try
            {
                auth = new SSOAuthService(failoverPath);
            }
            catch (IOException x)
            {
                auth = new SSOAuthService();
            }
        }
        _authService = auth;
    }
    
    static SSOAuthService getAuthService()
    {
        return _authService;
    }
    
    @Context HttpServletRequest request;    
    @PUT
    @Path("/request")
    @Consumes(MediaType.TEXT_PLAIN)
    public String requestAuth(String reqbody) throws IOException    
    //public String requestAuth() throws IOException
    {
        //request.getAsyncContext().
        // Jakarta doesn't want to give me the reader, saying that it's already streamed
        //String[] lines = request.getReader().lines().toArray(String[]::new); //toArray();
        //return request.getRequestURI() + "\n" +  String.join("\n,", lines);
        
        return request.getRequestURI() + "\n" +  reqbody;
        //_authService.requestAuth(myId, authenticatorId, identityId);
    }
    
    public class SimpleObjA
    {
        private String _fieldA;
        public String getFieldA() { return _fieldA; }
        public void setFieldA(String val) {_fieldA = val; }
    }
    
    @GET
    @Path("/fieldA")
    @Produces(MediaType.APPLICATION_JSON)
    public SimpleObjA getFieldA()
    {
        SimpleObjA res = new SimpleObjA();
        res.setFieldA("test");
        return res;
    }
    
	@GET
	//@Path("/simple2") // when the @Path is set both on the class and on the method, they concatenate as /simple/simple2
	@Path("/info/{code}")
	@Produces(MediaType.TEXT_PLAIN)
	public String getSimpleString(@PathParam("code") String code) throws Exception
	{
        try {
            if ("path".equals(code))
            {
                String path = this.getClass().getClassLoader().getResource(".").getPath();
                return path;
            }
            //java.nio.file.Path pp = java.nio.file.Path.of("%LOCALAPPDATA%/ssodemo/data.txt");
            //System.out.println(pp);

            //load a properties file from class path, inside static method
            //props.load(this.getClass().getClassLoader().getResourceAsStream("myprop1.properties"));
            String path = this.getClass().getClassLoader().getResource(".").getPath() + "/myprop.properties";
            //path = this.getClass().getClassLoader().getResource("..").getPath() + "/lib/myprop1.properties";
            //load the file handle for main.properties
            FileInputStream file = new FileInputStream(path);

            String res = "Hello, service3 with " + code + "!<br/>";
            
            Properties props = new Properties();
            
            //load all the properties from this file
            props.load(file);

            //we have loaded the properties, so close the file handle
            file.close();
            //get the property value and print it out
            res += props.getProperty("read") + "<br/>\n";
            res += props.getProperty("write") + "<br/>\n";

            return res;
        } 
        catch (IOException ex) {
            ex.printStackTrace();
            throw ex;
        }   		
		
	}

	/*@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getSimpleString(@QueryParam("name") String name)
	{
		return "Hello, service " + name;
	}*/
}
