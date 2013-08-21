package org.fizmo.dropwizard.guice;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.sun.jersey.spi.container.servlet.WebConfig;
import com.yammer.dropwizard.config.Environment;

import java.util.Map;

public class DropwizardGuiceContainer extends GuiceContainer
{
    private final Environment environment;

    @Inject
    DropwizardGuiceContainer(Environment environment, Injector injector)
    {
        super(injector);

        this.environment = environment;
        environment.setJerseyServletContainer(this);
    }

    @Override
    public ResourceConfig getDefaultResourceConfig(Map<String, Object> props, WebConfig webConfig) {
        return environment.getJerseyResourceConfig();
    }
}
