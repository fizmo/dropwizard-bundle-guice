package org.fizmo.dropwizard.guice;

import com.google.inject.Module;

public interface ConfiguredModule<T>
{
    Module withConfiguration(T configuration);
}
