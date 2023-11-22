package net.vbelev.ssodemo;

//import javax.servlet.ServletContext;
//import javax.servlet.http.*;
//import jakarta.servlet.annotation.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class DemoService {

	@GET
	@Path("/simple")
	@Produces(MediaType.TEXT_PLAIN)
	public String getSimpleString()
	{
		return "Hello, service!";
	}

	/*@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getSimpleString(@QueryParam("name") String name)
	{
		return "Hello, service " + name;
	}*/
}
