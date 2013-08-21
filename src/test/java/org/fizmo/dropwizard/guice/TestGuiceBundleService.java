package org.fizmo.dropwizard.guice;

import com.google.common.io.Resources;
import com.sun.jersey.api.client.Client;
import com.yammer.dropwizard.testing.junit.DropwizardServiceRule;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

public class TestGuiceBundleService
{
    @ClassRule
    public static final DropwizardServiceRule<GuiceBundleConfiguration> testRule =
            new DropwizardServiceRule<GuiceBundleConfiguration>(
                GuiceBundleService.class,
                Resources.getResource("guiceBundle.yml").getPath()
            );

    @Test
    public void testService()
    {
        final int port = testRule.getLocalPort();
        final URI uri = UriBuilder.fromUri("http://localhost").port(port).path("test").build();

        final Client client = Client.create();
        final int localPort = client.resource(uri).get(Integer.class);
        Assert.assertThat(localPort, not(0));
    }
}
