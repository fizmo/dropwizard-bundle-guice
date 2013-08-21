package org.fizmo.dropwizard.guice.resources;

import com.google.inject.Inject;
import com.sun.jersey.api.core.HttpContext;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/test")
@Produces(MediaType.APPLICATION_JSON)
public class GuiceBundleResource
{
    private final HttpContext context;
    private final HttpServletRequest request;

    @Inject
    GuiceBundleResource(HttpContext context, HttpServletRequest request)
    {
        this.context = context;
        this.request = request;
    }

    @GET
    public Object getRemotePort()
    {
        return request.getRemotePort();
    }
}
