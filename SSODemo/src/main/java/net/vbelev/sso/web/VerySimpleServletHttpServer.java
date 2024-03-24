package net.vbelev.sso.web;

import java.io.*;
import java.lang.reflect.*;
import java.net.InetSocketAddress;
import java.util.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * An extension of {@link com.sun.net.httpserver.HttpServer} that serves a single servlet on the given context path.
 * This is copied from the comment https://stackoverflow.com/a/20261540 
 * in https://stackoverflow.com/questions/3732109/simple-http-server-in-java-using-only-java-se-api
 * 
 * Can be used instead of Jetty?
 */
//@SuppressWarnings("deprecation")
public class VerySimpleServletHttpServer {
    HttpServer server;
    private String contextPath;
    private HttpHandler httpHandler;

    public VerySimpleServletHttpServer(String contextPath, HttpServlet servlet) {
        this.contextPath = contextPath;
        httpHandler = new HttpHandlerWithServletSupport(servlet);
    }

    public void start(int port) throws IOException {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(port);
        server = HttpServer.create(inetSocketAddress, 0);
        server.createContext(contextPath, httpHandler);
        server.setExecutor(null);
        server.start();
    }

    public void stop(int secondsDelay) {
        server.stop(secondsDelay);
    }

    public int getServerPort() {
        return server.getAddress().getPort();
    }

}

final class HttpHandlerWithServletSupport implements HttpHandler {

    private HttpServlet servlet;

    private final class RequestWrapper extends HttpServletRequestWrapper {
        private final HttpExchange ex;
        private final HttpServletRequest _request;
        private final Map<String, String[]> postData;
        private final ServletInputStream is;
        private final Map<String, Object> attributes = new HashMap<>();

        private RequestWrapper(HttpServletRequest request, HttpExchange ex, Map<String, String[]> postData, ServletInputStream is) {
            super(request);
            this.ex = ex;
            this.postData = postData;
            this.is = is;
            this._request = request;
        }

        @Override
        public String getHeader(String name) {
            return ex.getRequestHeaders().getFirst(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return new Vector<String>(ex.getRequestHeaders().get(name)).elements();
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            return new Vector<String>(ex.getRequestHeaders().keySet()).elements();
        }

        @Override
        public Object getAttribute(String name) {
            return attributes.get(name);
        }

        @Override
        public void setAttribute(String name, Object o) {
            this.attributes.put(name, o);
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            return new Vector<String>(attributes.keySet()).elements();
        }

        @Override
        public String getMethod() {
            return ex.getRequestMethod();
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return is;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        }

        @Override
        public String getPathInfo() {
            return ex.getRequestURI().getPath();
        }
        
        @Override
        public String getRequestURI()
        {
        	return ex.getRequestURI().toString();
        }

    	@Override
    	public String getContextPath()
    	{
    		return ex.getHttpContext().getPath();
    	}
    	
        @Override
        public String getParameter(String name) {
            String[] arr = postData.get(name);
            return arr != null ? (arr.length > 1 ? Arrays.toString(arr) : arr[0]) : null;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return postData;
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return new Vector<String>(postData.keySet()).elements();
        }
    }

    private final class ResponseWrapper extends HttpServletResponseWrapper {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final ServletOutputStream servletOutputStream = new ServletOutputStream() {

            @Override
            public void write(int b) throws IOException {
                outputStream.write(b);
            }

			@Override
			public boolean isReady() {
				// TODO Auto-generated method stub
				// it's our local memory stream, we are always ready to write
				return true;
			}

			@Override
			public void setWriteListener(WriteListener arg0) {
				// TODO Auto-generated method stub
				// it's our local memory stream, we are always ready to write
			}
        };

        private final HttpExchange ex;
        private final PrintWriter printWriter;
        private int status = HttpServletResponse.SC_OK;

        private ResponseWrapper(HttpServletResponse response, HttpExchange ex) {
            super(response);
            this.ex = ex;
            printWriter = new PrintWriter(servletOutputStream);
        }

        @Override
        public void setContentType(String type) {
            ex.getResponseHeaders().add("Content-Type", type);
        }

        @Override
        public void setHeader(String name, String value) {
            ex.getResponseHeaders().add(name, value);
        }

        @Override
        public jakarta.servlet.ServletOutputStream getOutputStream() throws IOException {
            return servletOutputStream;
        }

        @Override
        public void setContentLength(int len) {
            ex.getResponseHeaders().add("Content-Length", len + "");
        }

        @Override
        public void setStatus(int status) {
            this.status = status;
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            this.status = sc;
            if (msg != null) {
                printWriter.write(msg);
            }
        }

        @Override
        public void sendError(int sc) throws IOException {
            sendError(sc, null);
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return printWriter;
        }

        public void complete() throws IOException {
            try {
                printWriter.flush();
                ex.sendResponseHeaders(status, outputStream.size());
                if (outputStream.size() > 0) {
                    ex.getResponseBody().write(outputStream.toByteArray());
                }
                ex.getResponseBody().flush();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ex.close();
            }
        }
    }

    public HttpHandlerWithServletSupport(HttpServlet servlet) {
        this.servlet = servlet;
    }

    //@SuppressWarnings("deprecation")
    @Override
    public void handle(final HttpExchange ex) throws IOException {
        byte[] inBytes = getBytes(ex.getRequestBody());
        ex.getRequestBody().close();
        final ByteArrayInputStream newInput = new ByteArrayInputStream(inBytes);
        final ServletInputStream is = new ServletInputStream() {

            @Override
            public int read() throws IOException {
                return newInput.read();
            }

			@Override
			public boolean isFinished() {
				// TODO Auto-generated method stub
				return newInput.available() <= 0;
			}

			@Override
			public boolean isReady() {
				// TODO Auto-generated method stub
				// it's our local memory stream, we are always ready to read
				return true;
			}

			@Override
			public void setReadListener(ReadListener arg0) {
				// TODO Auto-generated method stub
				// it's our local memory stream, we are always ready to read
				
			}
        };
        Map<String, String[]> parsePostData = new HashMap<>();

        try {
            parsePostData.putAll(net.vbelev.utils.HttpUtils.parseQueryString(ex.getRequestURI().getQuery()));
            // check if any postdata to parse
            parsePostData.putAll(net.vbelev.utils.HttpUtils.parsePostData(inBytes.length, is));
        } catch (IllegalArgumentException e) {
            // no postData - just reset inputstream
            newInput.reset();
        }
        final Map<String, String[]> postData = parsePostData;

        RequestWrapper req = new RequestWrapper(createUnimplementAdapter(HttpServletRequest.class), ex, postData, is);

        ResponseWrapper resp = new ResponseWrapper(createUnimplementAdapter(HttpServletResponse.class), ex);

        try {
            servlet.service(req, resp);
            resp.complete();
        } catch (ServletException e) {
            throw new IOException(e);
        }
        catch (Exception x)
        {
        	resp.getWriter().append("\nException occured: "+ x.getMessage());
        	resp.complete();
        }
    }

    private static byte[] getBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (true) {
            int r = in.read(buffer);
            if (r == -1)
                break;
            out.write(buffer, 0, r);
        }
        return out.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static <T> T createUnimplementAdapter(Class<T> httpServletApi) {
        class UnimplementedHandler implements InvocationHandler {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                throw new UnsupportedOperationException("Not implemented: " + method + ", args=" + Arrays.toString(args));
            }
        }

        return (T) Proxy.newProxyInstance(UnimplementedHandler.class.getClassLoader(),
                new Class<?>[] { httpServletApi },
                new UnimplementedHandler());
    }
}
