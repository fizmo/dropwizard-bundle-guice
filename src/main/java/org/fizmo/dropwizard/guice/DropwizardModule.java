package org.fizmo.dropwizard.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.servlet.GuiceFilter;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.yammer.dropwizard.config.Environment;

class DropwizardModule<T> extends AbstractModule
{
    private final T configuration;
    private final Environment environment;

    public DropwizardModule(T configuration, Environment environment)
    {
        this.configuration = configuration;
        this.environment = environment;
    }

    @SuppressWarnings("unchecked")
    protected void configure()
    {
        install(new JerseyServletModule());

        final Class<T> configClass = (Class<T>) configuration.getClass();
        bind(configClass).toInstance(configuration);
        bind(Environment.class).toInstance(environment);

        bind(GuiceContainer.class).to(DropwizardGuiceContainer.class).asEagerSingleton();
    }

}
