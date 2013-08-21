# Dropwizard Guice Bundle

This is a project for integrating Guice with Dropwizard. There are many like it, but this one is mine.

[![Build Status](https://travis-ci.org/fizmo/dropwizard-bundle-guice.png?branch=master)](https://travis-ci.org/fizmo/dropwizard-bundle-guice)

## Motivations

If you're here, you probably already believe in the advantages of Guice, but there are also some stylistic preferences
that motivated its creation.

### Preference over Jersey built-in dependency injection

Dropwizard does not provide any dependency injection, and instead relies on the mechanisms provided by Jersey.
Jersey's injection framework is tailored to the work of injecting Jersey-specific data into resources, whereas
Guice is designed as a general-purpose DI framework. Using Guice allows a service author to write modules that
are not Jersey-specific so that they can be re-used in other contexts.

### Modularity of concerns

Guice modules can help in exposing abstractions without awkward factory methods. Package-private implementations of
public interfaces can be exposed in a public module, enabling late-binding of implementations based on configuration.
It's likely possible to do similar things using Dropwizard's bundles or Jersey's providers; we simply prefer Guice's
mechanisms.

## Getting Started

To add Guice to your Dropwizard project, create an instance of GuiceBundle and add it to your bootstrap.

    public class HelloWorldService extends Service<HelloWorldConfiguration> {

        @Override
        public void initialize(Bootstrap<HelloWorldConfiguration> bootstrap) {
            GuiceBundle<HelloWorldConfiguration> guiceBundle = new GuiceBundle.Builder()
                    .withModules(new HelloWorldModule())
                    .build();

            bootstrap.addBundle(guiceBundle);
        }

    }

In the simplest cases, your service's `run` method will be empty, as the GuiceBundle will take care of adding
classes to the environment.

## Binding Resources and Providers

Binding resources or providers is as simple as declaring the binding in a module:


    @Path("/")
    public class MyResource { /* ... */ }

    public class MyResourceModule extends AbstractModule
    {
        @Override
        public void configure() {
            bind(MyResource.class)
        }
    }

An explicit binding is required to inform Guice of the type. The underlying GuiceContainer enables injection of
JAX-RS specific parameters at construction type, if the default per-request lifecycle is used.

## Binding Health Checks

To allow registration of multiple health classes, the bundle looks for a binding to `Set<HealthCheck>`. This binding
can be populated by hand, but it is intended for use with the
[Guice Multibinding extension](https://code.google.com/p/google-guice/wiki/Multibindings):

    public class MyHealthCheck extends HealthCheck { /* ... */ }

    public class MyHealthCheckModule extends AbstractModule
    {
        @Override
        public void configure() {
            final Multibinder<HealthCheck> multibinder = Multibinder.newSetBinder(binder(), HealthCheck.class);
            multibinder.addBinding().to(MyHealthCheck.class);
        }
    }

## Accessing Dropwizard configuration in Modules

The Dropwizard `Configuration` class used by your service is injected into classes that declare it as a dependency,
but in some cases you many need access to the configuration at bind-time. If your module also implements
`ConfiguredModule`, then the GuiceBundle will call `withConfiguration` prior to creating the injector. Note that
the binder will not be set at this time, so the configuration should be saved for use in the `configure` method.

## Future Directions

Future releases may support additional Dropwizard primitives such as Tasks or Managed lifecycle objects, as may be
appropriate.

## Alternatives

* [HubSpot](http://dev.hubspot.com) has [their own Guice bundle](https://github.com/HubSpot/dropwizard-guice), which
requires HealthCheck classes to inherit from a custom base class, but additionally supports package scanning for
automatic configuration, which this bundle does not.

* [Jared Stehler](http://mindtap.cengage.com/) has [another bundle](https://github.com/jaredstehler/dropwizard-guice)
which requires Service classes to inherit from a custom base class, but additionally supports adding tasks and managed
lifecycle objects, which this bundle does not.

* Dropwizard does support adding classes instead of instances. In this case, you must use Jersey's built-in dependency
injection, if you want any at all.

* Dropwizard also supports Bundles, such as this one, for modularity of implementations, which could meet some of the
needs of Guice modules.

## Questions

I try to keep an eye on the [dropwizard user mailing list](https://groups.google.com/forum/#!forum/dropwizard-user),
or you can ping me (`ccurrie`) on the [`#dropwizard-user` channel on freenode]
(http://webchat.freenode.net/?channels=dropwizard-user). Please use these for questions on usage, and use GitHub
issues only for defects or feature requests.
