package org.fizmo.dropwizard.guice;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.servlet.GuiceFilter;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.sun.jersey.spi.container.servlet.WebConfig;
import com.yammer.dropwizard.ConfiguredBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;
import com.yammer.metrics.core.HealthCheck;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.ImmutableList.of;

public class GuiceBundle<T> implements ConfiguredBundle<T> {

    private final Iterable<Module> modules;
    private final Optional<Injector> parentInjector;

    private static final Key<Set<HealthCheck>> healthChecksKey = Key.get(new TypeLiteral<Set<HealthCheck>>() {});

    private GuiceBundle(final Iterable<Module> modules, final Optional<Injector> parentInjector) {
        this.modules = modules;
        this.parentInjector = parentInjector;
    }

    @Override
    public void initialize(final Bootstrap<?> bootstrap) {

    }

    @Override
    public void run(final T configuration, final Environment environment)
    {
        final Iterable<Module> configedModules = configureModules(configuration, modules);
        final DropwizardModule<T> dwModule = new DropwizardModule<T>(configuration, environment);

        final Injector injector = parentInjector.or(Guice.createInjector())
                .createChildInjector(Iterables.concat(of(dwModule), configedModules));

        environment.addFilter(injector.getInstance(GuiceFilter.class), "*");

        // Support Module definition of health checks
        if (!injector.findBindingsByType(healthChecksKey.getTypeLiteral()).isEmpty()) {
            final Set<HealthCheck> healthChecks = injector.getInstance(healthChecksKey);
            for (HealthCheck hc : healthChecks) {
                environment.addHealthCheck(hc);
            }
        }

    }

    private Iterable<Module> configureModules(final T config, final Iterable<Module> modules)
    {
        final List<Module> configedModules = Lists.newArrayList();

        for (Module m : modules)
        {
            if (m instanceof ConfiguredModule<?>)
            {
                // TODO: reflection to pre-validate if it's got the right type parameter
                final ConfiguredModule<T> cm = (ConfiguredModule<T>) m;
                configedModules.add(cm.withConfiguration(config));
            }
            else
            {
                configedModules.add(m);
            }
        }

        return configedModules;
    }

    private static abstract class AbstractBuilder<T extends AbstractBuilder> {
        private final List<Module> modules;

        protected AbstractBuilder() {
            modules = new LinkedList<Module>();
        }

        protected abstract T self();

        protected abstract Optional<Injector> injector();

        public T withModules(Module module)
        {
            modules.add(module);
            return self();
        }

        public T withModules(Module module1, Module module2, Module... otherModules)
        {
            modules.add(module1);
            modules.add(module2);
            Collections.addAll(modules, otherModules);
            return self();
        }

        public T withModules(Iterable<Module> newModules)
        {
            for (Module m : newModules)
                modules.add(m);
            return self();
        }

        protected Collection<Module> modules() {
            return modules;
        }

        public <CT extends Configuration> GuiceBundle<CT> build(){
            return new GuiceBundle<CT>(modules(), injector());
        }
    }

    public static class ParentedBuilder extends AbstractBuilder<ParentedBuilder> {
        private final Injector parentInjector;

        ParentedBuilder(Injector parentInjector) {
            this.parentInjector = parentInjector;
        }

        @Override
        protected ParentedBuilder self() { return this; }

        @Override
        protected Optional<Injector> injector() {
            return Optional.of(parentInjector);
        }
    }

    public static class StagedBuilder extends AbstractBuilder<StagedBuilder> {

        private final Stage stage;

        StagedBuilder(Stage stage) {
            this.stage = stage;
        }

        @Override
        protected StagedBuilder self() { return this; }

        @Override
        protected Optional<Injector> injector() {
            return Optional.of(Guice.createInjector(stage));
        }
    }

    public static class Builder extends AbstractBuilder<Builder> {
        protected Builder self() { return this; }

        public ParentedBuilder withParent(Injector parentInjector) {
            return new ParentedBuilder(parentInjector).withModules(modules());
        }

        public StagedBuilder withStage(Stage stage) {
            return new StagedBuilder(stage).withModules(modules());
        }

        @Override
        protected Optional<Injector> injector() {
            return Optional.absent();
        }
    }

}
