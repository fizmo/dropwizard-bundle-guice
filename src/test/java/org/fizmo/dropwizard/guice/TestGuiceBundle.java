package org.fizmo.dropwizard.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.servlet.GuiceFilter;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.yammer.dropwizard.ConfiguredBundle;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;
import com.yammer.metrics.core.HealthCheck;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestGuiceBundle {

    public static class TestConfig extends Configuration {}

    /*
     * Since GuiceFilter has a static pipeline, we need to reset it between tests
     * otherwise it will generate warnings after the first test.
     */
    @After
    public void resetGuiceFilterPipeline() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method m = GuiceFilter.class.getDeclaredMethod("reset");
        m.setAccessible(true);
        m.invoke(null);
    }

    @Path("/")
    public static class RootResource
    {
        private final Environment env;

        @Inject
        public RootResource(Environment env) {

            this.env = env;
        }

        @GET
        public String get() { return "hello, world!"; }
    }

    private class RootResourceModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(RootResource.class);
        }
    }

    private class RootResourceConfigured extends AbstractModule implements ConfiguredModule<TestConfig> {

        private TestConfig config;

        @Override
        protected void configure() {
            assertTrue(config != null);
            install(new RootResourceModule());
        }

        @Override
        public Module withConfiguration(TestConfig configuration) {
            config = configuration;
            return this;
        }
    }

    private static class TestHealthCheck extends HealthCheck {

        private TestHealthCheck() {
            super("test");
        }

        @Override
        protected Result check() throws Exception {
            return Result.healthy();
        }
    }

    private class HealthCheckModule extends AbstractModule {

        @Override
        protected void configure() {
            final Multibinder<HealthCheck> multibinder = Multibinder.newSetBinder(binder(), HealthCheck.class);
            multibinder.addBinding().to(TestHealthCheck.class);
        }
    }

    @Test
    public void TestSimpleBundle() throws Exception {
        ConfiguredBundle<TestConfig> bundle = new GuiceBundle.Builder().withModules(new RootResourceModule()).build();
        runContainer(bundle);
    }

    @Test
    public void TestParentedBundle() throws Exception {
        final Module parentModule = mock(Module.class);

        ConfiguredBundle<TestConfig> bundle = new GuiceBundle.Builder()
                .withParent(Guice.createInjector(parentModule))
                .withModules(new RootResourceModule())
                .build();


        verify(parentModule).configure(any(Binder.class));

        runContainer(bundle);
    }

    @Test
    public void TestStagedBundle() throws Exception {
        ConfiguredBundle<TestConfig> bundle = new GuiceBundle.Builder().withStage(Stage.DEVELOPMENT).withModules(new RootResourceModule()).build();
        runContainer(bundle);
    }

    @Test
    public void TestHealthChecks() throws Exception {
        ConfiguredBundle<TestConfig> bundle = new GuiceBundle.Builder()
                .withModules(new RootResourceModule(), new HealthCheckModule()).build();

        Environment environment = mock(Environment.class);

        runContainer(bundle, environment);

        ArgumentCaptor<HealthCheck> captor = ArgumentCaptor.forClass(HealthCheck.class);
        verify(environment).addHealthCheck(captor.capture());

        assertTrue(captor.getValue() instanceof TestHealthCheck);
    }

    @Test
    public void TestConfiguredModule() throws Exception {
        ConfiguredBundle<TestConfig> bundle = new GuiceBundle.Builder().withModules(new RootResourceConfigured()).build();
        runContainer(bundle);
    }

    private void runContainer(ConfiguredBundle<TestConfig> bundle) throws Exception {
        Environment environment = mock(Environment.class);
        runContainer(bundle, environment);
    }

    // TODO. Verify some state in the modules.
    private void runContainer(ConfiguredBundle<TestConfig> bundle, Environment environment) throws Exception {
        ResourceConfig resourceConfig = new DefaultResourceConfig();
        when(environment.getJerseyResourceConfig()).thenReturn(resourceConfig);

        bundle.run(new TestConfig(), environment);

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
