package org.fizmo.dropwizard.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.yammer.dropwizard.Bundle;
import com.yammer.dropwizard.config.Environment;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.Collections;

import static org.mockito.Mockito.*;

public class TestGuiceBundle {

    @Path("/")
    public static class RootResource
    {
        @GET
        public String get() { return "hello, world!"; }
    }

    private class RootResourceModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(RootResource.class);
        }
    }

    @Test
    public void TestSimpleBundle() throws ServletException {
        Bundle bundle = new GuiceBundle.Builder().withModules(new RootResourceModule()).build();
        runContainer(bundle);
    }

    @Test
    public void TestParentedBundle() throws ServletException {
        Bundle bundle = new GuiceBundle.Builder().withParent(Guice.createInjector(new RootResourceModule())).build();
        runContainer(bundle);
    }

    @Test
    public void TestStagedBundle() throws ServletException {
        Bundle bundle = new GuiceBundle.Builder().withStage(Stage.TOOL).withModules(new RootResourceModule()).build();
        runContainer(bundle);
    }

    // TODO. Verify some state in the modules.
    private void runContainer(Bundle bundle) throws ServletException {
        Environment environment = mock(Environment.class);

        bundle.run(environment);

        ArgumentCaptor<GuiceContainer> captor = ArgumentCaptor.forClass(GuiceContainer.class);
        verify(environment).setJerseyServletContainer(captor.capture());

        GuiceContainer container = captor.getValue();
        ServletConfig config = mock(ServletConfig.class);
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Collections.<String>emptyList()));
        ServletContext context = mock(ServletContext.class);
        when(config.getServletContext()).thenReturn(context);

        container.init(config);
    }

}
