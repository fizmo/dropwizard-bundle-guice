package org.fizmo.dropwizard.guice;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.yammer.metrics.core.HealthCheck;
import org.fizmo.dropwizard.guice.health.GuiceBundleHealthCheck;
import org.fizmo.dropwizard.guice.resources.GuiceBundleResource;

import java.util.Set;

public class GuiceBundleModule extends AbstractModule
{
    @Override
    protected void configure() {
        bind(GuiceBundleResource.class);
        final Multibinder<HealthCheck> multibinder = Multibinder.newSetBinder(binder(), HealthCheck.class);
        multibinder.addBinding().to(GuiceBundleHealthCheck.class);
    }
}
