package org.fizmo.dropwizard.guice;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

public class GuiceBundleService extends Service<GuiceBundleConfiguration>
{
    @Override
    public void initialize(Bootstrap<GuiceBundleConfiguration> bootstrap)
    {
        GuiceBundle<GuiceBundleConfiguration> guiceBundle =
                new GuiceBundle.Builder().withModules(new GuiceBundleModule()).build();
        bootstrap.addBundle(guiceBundle);
    }

    @Override
    public void run(GuiceBundleConfiguration configuration, Environment environment) throws Exception
    {

    }
}
