package net.spals.appbuilder.app.dropwizard.sample;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.name.Names;
import net.spals.appbuilder.annotations.config.ApplicationName;
import net.spals.appbuilder.annotations.service.AutoBindModule;

/**
 * A sample {@link Module} for testing module auto-binding.
 *
 * @author tkral
 */
@AutoBindModule
class SampleAutoBoundModule implements Module {

    private final String applicationName;

    @Inject
    SampleAutoBoundModule(@ApplicationName final String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public void configure(final Binder binder) {
        binder.bind(String.class).annotatedWith(Names.named("AutoBoundModule"))
                .toInstance(applicationName + ":" + this.getClass().getName());
    }
}
