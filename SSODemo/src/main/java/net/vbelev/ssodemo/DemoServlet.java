package net.vbelev.ssodemo;

import java.io.IOException;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
/*
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
*/
//import jakarta.servlet.RequestDispatcher;

/**
 * A demo of a javax WebServlet, including JSP forwarding, multiple urls and query parameters
 */
@WebServlet({"/DemoServlet", "/DemoServlet/*"})
public class DemoServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DemoServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		if (request.getRequestURI().toLowerCase().endsWith("/hello"))
		{
			String[] names = request.getParameterMap().get("name");
			String name;
			if (names == null || names.length == 0)
				name = "(noname)";
			else
				name = String.join(", ", names);
			
		RequestDispatcher dispatcher = request.getRequestDispatcher("/hello.jsp");
		request.setAttribute("name", name);
		dispatcher.forward(request, response);
		}
		else		
		{
			String msg = "Served at: " + request.getContextPath() + " for " + request.getRequestURI();
			//response.getWriter().append("Served at: ").append(request.getContextPath());
			response.getWriter().append(msg);
		}
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
