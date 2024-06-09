package net.vbelev.ssodemo;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import net.vbelev.sso.core.SSOAuthProvider;
import net.vbelev.utils.Utils;

public class DemoServiceFilter extends HttpFilter
{

    private static SSOAuthProvider _authService;
    
    static 
    {
        try
        {
        _authService = DemoService.getAuthService();
        }
        catch (Exception x)
        {
            System.out.println("x: " + x.getMessage());
        }
    }
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException 
    {
        boolean isOk = beginRequest(req, res); //before request processing
        if (!isOk)
            return;
        chain.doFilter(req, res);//calls other filters and processes request
        endRequest(req, res); //after request processing you can use res here
    }    

    private boolean beginRequest(HttpServletRequest request, HttpServletResponse response) throws IOException 
    {
        String authHeader = request.getHeader("Authorization");
        if (Utils.isBlank(authHeader))
        {
            //response.sendError(401);
            //return false;
            return true;
        }
        SSOAuthProvider.AuthResponse check = _authService.testAuthentication(null, authHeader);
        if (check.status != SSOAuthProvider.AuthStatus.ACTIVE.name())
        {
            response.sendError(401);
            return false;
        }
        request.setAttribute("requestingId", check.authenticatedIID);
        return true;
    }

    private void endRequest(HttpServletRequest request, HttpServletResponse response) {
       //...
    }    
}
