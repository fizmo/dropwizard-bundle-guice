package org.fizmo.dropwizard.guice.health;

import com.yammer.dropwizard.config.Environment;
import com.yammer.metrics.core.HealthCheck;
import org.fizmo.dropwizard.guice.DropwizardGuiceContainer;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GuiceBundleHealthCheck extends HealthCheck
{
    private final Environment environment;

    @Inject
    public GuiceBundleHealthCheck(Environment environment)
    {
        super("guice-bundle");

        this.environment = environment;
    }

    @Override
    protected Result check() throws Exception
    {
        if (environment.getJerseyServletContainer() instanceof DropwizardGuiceContainer)
            return Result.healthy();
        return Result.unhealthy("Wrong container type");
    }
}
