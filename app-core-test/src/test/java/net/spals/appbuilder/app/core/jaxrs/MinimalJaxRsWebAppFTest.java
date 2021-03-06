package net.spals.appbuilder.app.core.jaxrs;

import com.typesafe.config.ConfigFactory;
import net.spals.appbuilder.app.core.generic.GenericWorkerApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.ws.rs.core.Configurable;

import java.util.function.BiFunction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;

/**
 * Functional tests for a minimal {@link JaxRsWebApp}
 *
 * @author tkral
 */
public class MinimalJaxRsWebAppFTest {
    private final Logger LOGGER = LoggerFactory.getLogger(MinimalJaxRsWebAppFTest.class);

    private final Configurable<?> configurable = mock(Configurable.class);
    private final BiFunction<String, Filter, FilterRegistration.Dynamic> filterRegistration = mock(BiFunction.class);

    private final JaxRsWebApp minimalApp = new JaxRsWebApp.Builder("minimal", LOGGER)
            .setConfigurable(configurable)
            .setFilterRegistration(filterRegistration)
            .build();

    @Test
    public void testJaxRsWebAppLogger() {
        assertThat(minimalApp.getLogger(), sameInstance(LOGGER));
    }

    @Test
    public void testJaxRsWebAppName() {
        assertThat(minimalApp.getName(), is("minimal"));
    }

    @Test
    public void testMinimalServiceConfig() {
        assertThat(minimalApp.getServiceConfig(), is(ConfigFactory.empty()));
    }
}
