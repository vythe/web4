package net.vbelev.ssodemo;

import java.util.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;

/**
 * The Jakarta way of registering a web service
 */
@ApplicationPath("/api")
public class DemoServiceApp extends Application
{

    private Set<Class<?>> clss;

    public DemoServiceApp()
    {
        HashSet<Class<?>>  hs = new HashSet<>();
        hs.add(DemoService.class);
        clss = Collections.unmodifiableSet(hs);
    }

    /**
     * The override from https://github.com/eclipse-ee4j/jersey/issues/3222 .
     * Somehow Tomcat uses the default implementation to scan for all classes annotated with @Path (i think).
     * For the embedded Jetty, we need to list the classes by hand.
     */
    @Override
    public Set<Class<?>> getClasses()
    {
        //return super.getClasses();
        return clss;
    }
}
