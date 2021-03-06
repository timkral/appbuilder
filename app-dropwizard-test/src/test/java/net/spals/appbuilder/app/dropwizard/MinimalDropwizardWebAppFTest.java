package net.spals.appbuilder.app.dropwizard;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.DropwizardTestSupport;
import net.spals.appbuilder.app.dropwizard.minimal.MinimalDropwizardWebApp;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.validation.Validator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Functional tests for a minimal {@link DropwizardWebApp}
 *
 * @author tkral
 */
public class MinimalDropwizardWebAppFTest {

    private final DropwizardTestSupport<Configuration> testServerWrapper =
            new DropwizardTestSupport<>(MinimalDropwizardWebApp.class, new Configuration());
    private DropwizardWebApp webAppDelegate;

    @BeforeTest
    void classSetup() {
        testServerWrapper.before();
        webAppDelegate = ((MinimalDropwizardWebApp)testServerWrapper.getApplication()).getDelegate();
    }

    @AfterTest
    void classTearDown() {
        testServerWrapper.after();
    }

    @Test
    public void testDropwizardWebAppLogger() {
        assertThat(webAppDelegate.getLogger(), notNullValue());
    }

    @Test
    public void testDropwizardWebAppName() {
        assertThat(webAppDelegate.getName(), is("MinimalDropwizardWebApp"));
    }

    @DataProvider
    Object[][] defaultServiceInjectorProvider() {
        return new Object[][] {
                {TypeLiteral.get(Environment.class)},
                {TypeLiteral.get(HealthCheckRegistry.class)},
                {TypeLiteral.get(MetricRegistry.class)},
                {TypeLiteral.get(Validator.class)},
        } ;
    }

    @Test(dataProvider = "defaultServiceInjectorProvider")
    public void testDefaultServiceInjector(final TypeLiteral<?> typeLiteral) {
        final Injector serviceInjector = webAppDelegate.getServiceInjector();
        assertThat(serviceInjector.getInstance(Key.get(typeLiteral)), notNullValue());
    }
}
