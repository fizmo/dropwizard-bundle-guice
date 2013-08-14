package org.fizmo.dropwizard.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.sun.jersey.spi.container.servlet.WebConfig;
import com.yammer.dropwizard.Bundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GuiceBundle implements Bundle {

    private final Injector injector;

    private GuiceBundle(Injector injector) {
        this.injector = injector;
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {

    }

    @Override
    public void run(final Environment environment) {
        final GuiceContainer container = new GuiceContainer(injector) {
            @Override
            public ResourceConfig getDefaultResourceConfig(Map<String, Object> props, WebConfig webConfig) {
                return environment.getJerseyResourceConfig();
            }
        };
        environment.setJerseyServletContainer(container);
    }

    private static abstract class AbstractBuilder<T extends AbstractBuilder> {
        private final List<Module> modules;

        protected AbstractBuilder() {
            modules = new LinkedList<Module>();
        }

        protected abstract T self();

        protected abstract Injector injector();

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

        public GuiceBundle build(){
            return new GuiceBundle(injector());
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
        protected Injector injector() {
            return parentInjector.createChildInjector(modules());
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
        protected Injector injector() {
            return Guice.createInjector(stage, modules());
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
        protected Injector injector() {
            return Guice.createInjector(modules());
        }
    }

}
