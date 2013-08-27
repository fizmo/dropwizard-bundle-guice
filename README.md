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

### Caveats

There are a couple of important notes to be aware of with the current version of the module.

#### Do not bind generic interfaces or base classes

The following binding will not work:

    bind(new TypeReference<MessageBodyReader<MyCustomType>>(){}).to(MyCustomMessageBodyReader.class);

Jersey only supports classes in its DI support. Any types bound as type references (to capture generic parameters)
will be ignored by the underlying jersey-guice library. To properly register the provider it must be bound as a
concrete class:

    bind(MyCustomMessageBodyReader.class);

#### Be aware of the jersey-guice binding life-cycle

Jersey supports class binding, where the bound class's scope is per-request by default, except for providers which
are always singletons; or instance binding, where the instance is naturally a singleton.

Instance bindings are used to create object when the constructor needs parameters that are not available to the
injector. Also, in jersey are sometimes to bind instances of generic types, since generic types can't be bound as
classes. An example of this is the `BasicAuthProvider` in the dropwizard-auth module, which is usually registered as a
singleton provider instance.

Guice provides instance bindings as well, through `bind(type).toInstance(instance)`. However, the jersey-guice
module does not expose the details of the binding to Jersey. Instead it looks at the bound classes for `@Path` or
`@Provider` annotations, and only exposes the annotated classes to Jersey.

This restriction and the one above have practical implications for the dropwizard-auth module, whose providers have type
parameters and are not annotated with `@Provider`, because they are designed to be registered as singletons and not classes.

To solve both issues, you must create a non-parameterized derived class annotated with `@Provider`.

    @javax.ws.rs.ext.Provider
    public class MyBasicAuthProvider extends BasicAuthProvider<User> {
        public AnnotatedBasicAuthProvider(Authenticator<BasicCredentials, User> authenticator, String realm) {
            super(authenticator, realm);
        }
    }

    bind(MyBasicAuthProvider.class);

It is hoped these limitations can be lifted in future versions.

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
