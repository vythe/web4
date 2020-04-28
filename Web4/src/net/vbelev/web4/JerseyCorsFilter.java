package net.vbelev.web4;

import javax.servlet.*;
import javax.servlet.http.*;

import net.vbelev.web4.utils.Utils;

import java.io.*;

public class JerseyCorsFilter //extends javax.servlet.http.HttpFilter
implements Filter
{
	private String allowOrigin = null;
	public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain arg2)
			throws IOException, ServletException 
	{
		HttpServletRequest request = (HttpServletRequest) arg0;
		HttpServletResponse response = (HttpServletResponse) arg1;
		FilterChain filterChain = arg2;

        response.addHeader("Access-Control-Allow-Credentials", "true");
        //response.addHeader("Access-Control-Allow-Origin", "*");
        if (allowOrigin != null && !allowOrigin.isEmpty())
        {
        response.addHeader("Access-Control-Allow-Origin", allowOrigin); // "http://localhost:3000"
        }
        if (request.getHeader("Access-Control-Request-Method") != null
                && "OPTIONS".equals(request.getMethod())) {
            // CORS "pre-flight" request
            //response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
            response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            response.addHeader("Access-Control-Allow-Headers", "X-Requested-With,Origin,Content-Type, Accept");
        }
        //response.addCookie(new Cookie("mystamp", new java.util.Date().toGMTString()));
        try
        {
        filterChain.doFilter(request, response);
        }
        catch (Exception x)
        {
        	System.out.println("JavaAPI got an exception: " + x.getMessage());
        	throw x;
        }
	}

	//@Override
	public void doFilter1(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws IOException, ServletException 
	{
        response.addHeader("Access-Control-Allow-Origin", "somehwere");
        if (request.getHeader("Access-Control-Request-Method") != null
                && "OPTIONS".equals(request.getMethod())) {
            // CORS "pre-flight" request
            //response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
            response.addHeader("Access-Control-Allow-Methods", "GET, POST");
            response.addHeader("Access-Control-Allow-Headers", "X-Requested-With,Origin,Content-Type, Accept");
        }
        filterChain.doFilter(request, response);			
	}

	//@Override
	public void destroy()
	{
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void init(FilterConfig arg0) throws ServletException
	{
		allowOrigin = arg0.getInitParameter("origin");		
	}
}	

