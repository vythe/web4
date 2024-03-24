package net.vbelev.ssodemo.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

import org.apache.jasper.servlet.JspServlet;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.jsp.JettyJspServlet;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.*;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.resource.*;

import org.eclipse.jetty.webapp.*;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.glassfish.jersey.server.*;
import org.glassfish.jersey.servlet.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.vbelev.sso.core.SSOFailover;
import net.vbelev.sso.web.*;
import net.vbelev.ssodemo.*;
import net.vbelev.utils.HttpUtils;
import net.vbelev.utils.Utils;

class SSOWebTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	//@Test
	void test() {
		//fail("Not yet implemented");
		AuthServlet servlet = new AuthServlet();
		VerySimpleServletHttpServer serv = new VerySimpleServletHttpServer("/auth", servlet);
		try		
		{
		serv.start(8081);
		System.out.print("server started: "+ serv.getServerPort());
		Thread.sleep(10000000);
		}
		catch (IOException x)
		{
			System.out.print("Exception: " + x.toString());
		}
		catch (InterruptedException x)
		{
			System.out.print("Interrupted");
		}
	}

	//@Test
	void testUtils()
	{
		try
		{
		Hashtable<String, String[]> parsed = HttpUtils.parseQueryString("a=b&c=d");
System.out.print(parsed.toString());
		}
		catch (Error x)
		{
			System.out.print("Exception: " + x.toString());
		}
	}
	
	private static final String RESOURCES_URL = "/rs";
	private static final String CONTEXT = "/app_context";
	private static final String DS_CONFIG = "/jetty-ds-test.xml";
	private String baseResourceUrl;

	//@Test
    public void startJetty() throws Exception
    {
        // from here: https://github.com/eclipse-ee4j/jersey/issues/3222
        Server server = new Server(8082);
        ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        //server.setHandler(servletHandler);

        // here, "jetty" is the web app name, similar to what Tomcat uses for a deployed webapp (SSODemo)
        servletHandler.setContextPath("/jetty/*");
        
        ServletContainer jersey = new ServletContainer(ResourceConfig.forApplicationClass(DemoServiceApp.class));
        ServletHolder restHolder = new ServletHolder(jersey);

        // Jetty 11 ignores the @ApplicationPath annotation, so we need to set the path to the same value.
        servletHandler.addServlet(restHolder, "/api/*");

        // add an explicit servlet to the same servlet handler
        //ServletHolder servletHolder = servletHandler.addServlet(AuthServlet.class, "/auth/*");
        servletHandler.addServlet(AuthServlet.class, "/auth/*");
        servletHandler.addServlet(DemoServlet.class, "/d2/*");
        
        WebAppContext webHandler = new WebAppContext("src/main/webapp", "/web/*");
        enableEmbeddedJspSupport(webHandler);
        // multi-handlers here: https://git.eclipse.org/c/jetty/org.eclipse.jetty.project.git/tree/examples/embedded/src/main/java/org/eclipse/jetty/embedded/ManyHandlers.java
        //HandlerCollection hc = new HandlerCollection();
        HandlerList hc = new HandlerList();
        hc.addHandler(servletHandler);
        //hc.addHandler(webHandler);
        server.setHandler(hc);
      
        // try to chain the handlers
        //
        //servletHandler.setHandler(webHandler);
        //server.setHandler(servletHandler);
        //
        //webHandler.setHandler(servletHandler);
        //server.setHandler(webHandler);
        
        server.start();
        server.join();
    }

    /**
     * from https://stackoverflow.com/questions/65637135/how-to-enable-jsp-in-a-jetty-server-in-a-jar-file
     * @param servletContextHandler
     * @throws IOException
     */
    private static void enableEmbeddedJspSupport(ServletContextHandler servletContextHandler) throws IOException
    {
        // Establish Scratch directory for the servlet context (used by JSP compilation)
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File scratchDir = new File(tempDir.toString(), "embedded-jetty-jsp");

        if (!scratchDir.exists()) {
            if (!scratchDir.mkdirs()) {
                throw new IOException("Unable to create scratch directory: " + scratchDir);
            }
        }
        servletContextHandler.setAttribute("javax.servlet.context.tempdir", scratchDir);

        // Set Classloader of Context to be sane (needed for JSTL)
        // JSP requires a non-System classloader, this simply wraps the
        // embedded System classloader in a way that makes it suitable
        // for JSP to use
        ClassLoader jspClassLoader = new URLClassLoader(new URL[0], SSOWebTest.class.getClassLoader());
        servletContextHandler.setClassLoader(jspClassLoader);

        // Manually call JettyJasperInitializer on context startup
        servletContextHandler.addBean(new JspStarter(servletContextHandler));

        // Create / Register JSP Servlet (must be named "jsp" per spec)
        ServletHolder holderJsp = new ServletHolder("jsp", JettyJspServlet.class);
        holderJsp.setInitOrder(0);
        holderJsp.setInitParameter("logVerbosityLevel", "DEBUG");
        holderJsp.setInitParameter("fork", "false");
        holderJsp.setInitParameter("xpoweredBy", "false");
        holderJsp.setInitParameter("compilerTargetVM", "1.8");
        holderJsp.setInitParameter("compilerSourceVM", "1.8");
        holderJsp.setInitParameter("keepgenerated", "true");
        servletContextHandler.addServlet(holderJsp, "*.jsp");
    }

    /**
      * JspStarter for embedded ServletContextHandlers
      *
      * This is added as a bean that is a jetty LifeCycle on the ServletContextHandler.
      * This bean's doStart method will be called as the ServletContextHandler starts,
      * and will call the ServletContainerInitializer for the jsp engine.
      *
      */
    public static class JspStarter extends AbstractLifeCycle implements ServletContextHandler.ServletContainerInitializerCaller
    {
        JettyJasperInitializer sci;
        ServletContextHandler context;

        public JspStarter(ServletContextHandler context)
        {
            StandardJarScanner jarScanner = new StandardJarScanner();
            /* it works without the additional filter, too
             *
             */
            this.context = context;

            StandardJarScanFilter jarScanFilter = new StandardJarScanFilter();
            jarScanFilter.setTldScan("taglibs-standard-impl-*");
            jarScanFilter.setTldSkip("apache-*,ecj-*,jetty-*,asm-*,javax.servlet-*,javax.annotation-*,taglibs-standard-spec-*");
            jarScanner.setJarScanFilter(jarScanFilter);
            this.context.setAttribute("org.apache.tomcat.JarScanner", jarScanner);            
            
            this.sci = new JettyJasperInitializer();
        }

        @Override
        protected void doStart() throws Exception
        {
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(context.getClassLoader());
            try {
                sci.onStartup(null, context.getServletContext());
                super.doStart();
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        }
    }

	public void startJettyOld() throws Exception 
	{
		Server server = new Server(0); // see notice 1
		server.setHandler(new WebAppContext("src/main/webapp", CONTEXT)); // see notice 2

		// see notice 3
		URL jettyConfURL = SSOWebTest.class.getResource(DS_CONFIG);
		// InputStream jettyConfFile = SSOWebTest.class.getResourceAsStream(DS_CONFIG);
		// if (jettyConfFile == null) {
		// throw new FileNotFoundException(DS_CONFIG);
		// }
		// XmlConfiguration config = new XmlConfiguration(jettyConfFile);
		
		// see https://github.com/jetty/jetty.project/issues/9973
		Resource jettyResource = JarResource.newResource(jettyConfURL);
		//ResourceFactory resourceFactory = new JarResource(jettyConfURL);
		
		//XmlConfiguration config = new XmlConfiguration(jettyResource);
		//config.configure(server);

		ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/jetty");

		ServletHolder servletHolder = context.addServlet(AuthServlet.class, "/auth/*");
		//ServletHolder servletHolder2 = context.addServlet(net.vbelev.ssodemo.DemoServiceApp.class, "/demo/*");
		//context.a

		server.setHandler(context);
		
		ServerConnector conn1 = (ServerConnector)server.getConnectors()[0];
		conn1.setPort(8082);
		server.start();

		// see notice 1
		ServerConnector conn = (ServerConnector)server.getConnectors()[0];
		//int actualPort = server.getConnectors()[0].getLocalPort();
		int actualPort = conn.getLocalPort();
		baseResourceUrl = "http://localhost:" + actualPort + CONTEXT + RESOURCES_URL;

		server.join();
		
		while(true)
		{
			Thread.sleep(1000);
		}
	}
	
	//@Test
	public void propTest() throws Exception
	{
	    //Map<String, String> envMap = System.getenv();
	    //Properties props = System.getProperties();
	    Properties props = new Properties();
	    
	    try {
	        //load a properties file from class path, inside static method
	        props.load(this.getClass().getClassLoader().getResourceAsStream("myprop.properties"));

	        //get the property value and print it out
	        System.out.println(props.getProperty("read"));
	        System.out.println(props.getProperty("write"));

	    } 
	    catch (IOException ex) {
	        ex.printStackTrace();
	        throw ex;
	    }	    
	    
	    //System.out.println(props.toString());
	    //URL prop = this.getClass().getClassLoader().getResource("myprop.properties");
	    //System.out.println(prop);
	}
	
	@Test
	public void testFailover() throws IOException
	{
	    String homeFolder = System.getenv("APPDATA");
	    if (Utils.isBlank(homeFolder))
	        homeFolder = System.getProperty("user.home");
	    SSOFailover sf = new SSOFailover(Path.of(homeFolder, "SSOFailoverTest").toAbsolutePath().toString(), "simple", null, 5);
	    sf.reset();
	    sf.write("A", "Line 1");
        sf.write("B", "Line 2");
        sf.write("A", "Line 3");
        sf.write("B", "Line 4");
        sf.write("A", "Line 5");
        sf.write("B", "Line 6");
        sf.write("A", "Line 7");
        sf.write("B", "Line 8");
        sf.write("A", "Line 9");
        sf.write("B", "Line 10");
        sf.write("A", "Line 11");
        sf.write("B", "Line 12");
        sf.write("A", "Line 13");
        sf.write("B", "Line 14");
        sf.write("A", "Line 15");
        sf.write("B", "Line 16");
        sf.write("A", "Line 17");
        sf.write("B", "Line 18");
        
        SSOFailover sf2 = new SSOFailover(Path.of(homeFolder, "SSOFailoverTest").toAbsolutePath().toString(), "simple", null, 5);
        Map<String, String> entities = sf2.restore();
        assertEquals(2, entities.size());
        assertEquals("Line 17", entities.get("A"));
        assertEquals("Line 18", entities.get("B"));
        sf2.write("C", "Line 19");
	}
}
