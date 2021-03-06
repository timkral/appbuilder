package net.spals.appbuilder.app.core.modules;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.*;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.servlet.ServletScopes;
import net.spals.appbuilder.annotations.service.*;
import net.spals.appbuilder.annotations.service.AutoBindProvider.ProviderScope;
import net.spals.appbuilder.config.service.ServiceScan;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.inferred.freebuilder.FreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author tkral
 */
@FreeBuilder
public abstract class AutoBindServicesModule extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoBindServicesModule.class);

    public abstract Boolean getErrorOnServiceLeaks();
    public abstract ServiceScan getServiceScan();

    public static class Builder extends AutoBindServicesModule_Builder {
        public Builder() {
            setErrorOnServiceLeaks(true);
            // By default, use an empty service scan
            setServiceScan(ServiceScan.empty());
        }
    }

    @Override
    protected void configure() {
        autoBindFactories(binder());
        autoBindMaps(binder());
        autoBindProviders(binder());
        autoBindSets(binder());
        autoBindSingletons(binder());
    }

    @VisibleForTesting
    void autoBindFactories(final Binder binder) {
        final Set<Class<?>> factoryClasses = getServiceScan().getReflections()
                .getTypesAnnotatedWith(AutoBindFactory.class);
        validateFactories(factoryClasses);

        factoryClasses.forEach(factoryClazz -> {
            LOGGER.info("Binding @AutoBindFactory: {}", factoryClazz);
            binder.install(new FactoryModuleBuilder().build(factoryClazz));
        });
    }

    @VisibleForTesting
    void autoBindMaps(final Binder binder) {
        final Set<Class<?>> mapClasses = getServiceScan().getReflections()
                .getTypesAnnotatedWith(AutoBindInMap.class);
        validateSingletons(mapClasses, AutoBindInMap.class);
        checkServiceLeaks(mapClasses);

        mapClasses.stream()
            .forEach(mapClazz -> {
                final AutoBindInMap autoBindInMap = mapClazz.getAnnotation(AutoBindInMap.class);
                checkState(autoBindInMap.keyType() == String.class || autoBindInMap.keyType().isEnum(),
                        "@AutoBindInMap.keyType must be String or an Enum");
                LOGGER.info("Binding @AutoBindInMap[{}:{}]: {}", new Object[] {autoBindInMap.key(),
                    autoBindInMap.baseClass().getSimpleName(), mapClazz});

                final MapBinder mapBinder = MapBinder.newMapBinder(binder,
                        autoBindInMap.keyType(), autoBindInMap.baseClass());
                final Object key = autoBindInMap.keyType() == String.class ? autoBindInMap.key() :
                        Enum.valueOf((Class) autoBindInMap.keyType(), autoBindInMap.key());
                mapBinder.addBinding(key).to((Class) mapClazz).asEagerSingleton();
            });
    }

    @VisibleForTesting
    void autoBindProviders(final Binder binder) {
        final Set<Class<?>> providerClasses = getServiceScan().getReflections()
                .getTypesAnnotatedWith(AutoBindProvider.class);
        validateProviders(providerClasses);
        checkServiceLeaks(providerClasses);

        providerClasses.stream()
            .forEach(providerClazz -> {
                final AutoBindProvider autoBindProvider = providerClazz.getAnnotation(AutoBindProvider.class);
                checkState(autoBindProvider.bindingAnnotation() == AutoBindProvider.class
                        || autoBindProvider.bindingAnnotation().isAnnotationPresent(BindingAnnotation.class),
                        "@AutoBindProvider.bindingAnnotation must be annotated with @BindingAnnotation: %s",
                        new Object[] {autoBindProvider.bindingAnnotation()});
                LOGGER.info("Binding @AutoBindProvider: {}", providerClazz);

                // Taken from Governator's ProviderBinderUtil
                final Type providedType;
                try {
                    providedType = providerClazz.getMethod("get").getGenericReturnType();
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }

                final javax.inject.Provider autoBoundProvider =
                        new AutoBoundProvider((Class<? extends javax.inject.Provider>) providerClazz);
                final Key<?> providedTypeKey;
                if (autoBindProvider.bindingAnnotation() != AutoBindProvider.class) {
                    providedTypeKey = Key.get(TypeLiteral.get(providedType), autoBindProvider.bindingAnnotation());
                } else {
                    providedTypeKey = Key.get(TypeLiteral.get(providedType));
                }

                binder.bind(providedTypeKey).toProvider(autoBoundProvider).in(mapProviderScope(autoBindProvider.value()));
            });
    }

    @VisibleForTesting
    void autoBindSets(final Binder binder) {
        final Set<Class<?>> setClasses = getServiceScan()
                .getReflections().getTypesAnnotatedWith(AutoBindInSet.class);
        validateSingletons(setClasses, AutoBindInSet.class);
        checkServiceLeaks(setClasses);

        setClasses.stream()
            .forEach(setClazz -> {
                final AutoBindInSet autoBindInSet = setClazz.getAnnotation(AutoBindInSet.class);
                LOGGER.info("Binding @AutoBindInSet[{}]: {}", autoBindInSet.baseClass(), setClazz);

                final Multibinder multibinder = Multibinder.newSetBinder(binder, autoBindInSet.baseClass());
                multibinder.addBinding().to((Class) setClazz).asEagerSingleton();;
            });
    }

    @VisibleForTesting
    void autoBindSingletons(final Binder binder) {
        final Set<Class<?>> singletonClasses = getServiceScan().getReflections()
                .getTypesAnnotatedWith(AutoBindSingleton.class);
        validateSingletons(singletonClasses, AutoBindSingleton.class);
        checkServiceLeaks(singletonClasses);

        singletonClasses.stream()
            .forEach(singletonClazz -> {
                final AutoBindSingleton autoBindSingleton = singletonClazz.getAnnotation(AutoBindSingleton.class);
                LOGGER.info("Binding @AutoBindSingleton: {}", singletonClazz);

                // Case: Singleton binding without interface
                if (autoBindSingleton.baseClass() == Void.class || autoBindSingleton.includeImpl()) {
                    binder.bind(singletonClazz).asEagerSingleton();
                }
                // Case: Singleton binding with interface
                if (autoBindSingleton.baseClass() != Void.class) {
                    binder.bind(autoBindSingleton.baseClass()).to((Class) singletonClazz).asEagerSingleton();
                }
            });
    }

    @VisibleForTesting
    void checkServiceLeaks(final Set<Class<?>> serviceClasses) {
        final Set<Class<?>> publicServices = serviceClasses.stream()
                // Never consider Scala classes for service leak checking. This is because
                // Scala's package scoping (private[my.package] and protected[my.package])
                // translate in the JVM to a public scope.
                .filter(serviceClass -> !isScala(serviceClass))
                .filter(serviceClass -> Modifier.isPublic(serviceClass.getModifiers())
                    || Modifier.isProtected(serviceClass.getModifiers()))
                .collect(Collectors.toSet());
        if (!publicServices.isEmpty()) {
            LOGGER.warn("The following service classes are public or protected which may lead to service leaking. " +
                    "You should consider making them package-protected or private. {}", publicServices);
        }

        final Set<Class<?>> publicCtors = serviceClasses.stream()
                // See comment above about filtering Scala classes
                .filter(serviceClass -> !isScala(serviceClass))
                .flatMap(serviceClass -> Arrays.asList(serviceClass.getDeclaredConstructors()).stream())
                .filter(serviceCtor -> Modifier.isPublic(serviceCtor.getModifiers())
                        || Modifier.isProtected(serviceCtor.getModifiers()))
                .map(serviceCtor -> serviceCtor.getDeclaringClass())
                .collect(Collectors.toSet());

        final String serviceLeakMessage = String.format(
                "Service leak detected! The following services have public or protected constructors, " +
                "which means they can be instantiated outside of the appbuilder framework. " +
                "You can fix this by making them package-protected or private. %s", publicCtors);
        if (getErrorOnServiceLeaks()) {
            checkState(publicCtors.isEmpty(), serviceLeakMessage);
        } else if (!publicCtors.isEmpty()) {
            LOGGER.warn(serviceLeakMessage);
        }
    }

    @VisibleForTesting
    boolean isScala(final Class jvmClass) {
        try {
            // Use BCEL to dig out the source file name
            final JavaClass javaClass = Repository.lookupClass(jvmClass.getName());
            return javaClass.getSourceFileName().endsWith(".scala");
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        }
    }

    @VisibleForTesting
    Scope mapProviderScope(final ProviderScope providerScope) {
        switch (providerScope) {
            case NONE: return Scopes.NO_SCOPE;
            case REQUEST: return ServletScopes.REQUEST;
            case SESSION: return ServletScopes.SESSION;
            case SINGLETON:
            default:
                return Scopes.SINGLETON;
        }
    }

    @VisibleForTesting
    void validateFactories(final Set<Class<?>> factoryClasses) {
        final Set<Class<?>> invalidFactories = factoryClasses.stream()
                .filter(factoryClazz -> !factoryClazz.isInterface())
                .collect(Collectors.toSet());
        checkState(invalidFactories.isEmpty(),
                "@AutoBindFactory can only annotate interfaces: %s",
                new Object[] {invalidFactories});
    }

    @VisibleForTesting
    void validateProviders(final Set<Class<?>> providerClasses) {
        final Set<Class<?>> invalidProviders = providerClasses.stream()
                .filter(providerClazz -> providerClazz.isInterface()
                        || !javax.inject.Provider.class.isAssignableFrom(providerClazz))
                .collect(Collectors.toSet());
        checkState(invalidProviders.isEmpty(),
                "@AutoBindProvider can only annotate Provider classes: %s",
                new Object[] {invalidProviders});
    }

    @VisibleForTesting
    void validateSingletons(final Set<Class<?>> singletonClasses,
                            final Class<? extends Annotation> annotationClazz) {
        final Set<Class<?>> invalidSingletons = singletonClasses.stream()
                .filter(singletonClazz -> singletonClazz.isInterface()
                        || javax.inject.Provider.class.isAssignableFrom(singletonClazz))
                .collect(Collectors.toSet());
        checkState(invalidSingletons.isEmpty(),
                "@%s can only annotate non-Provider classes: %s",
                new Object[] {annotationClazz.getSimpleName(), invalidSingletons});
    }

    /**
     * A wrapper around a provider class annotated with @AutoBindProvider
     * which exposes it to the Guice injector.
     *
     * Taken from Governator's ProviderBinderUtil.
     *
     * @author tkral
     */
    @VisibleForTesting
    static class AutoBoundProvider implements com.google.inject.Provider {
        private final Class<? extends javax.inject.Provider> providerClazz;

        @Inject
        private Injector injector;

        public AutoBoundProvider(Class<? extends javax.inject.Provider> providerClazz) {
            this.providerClazz = providerClazz;
        }

        @Override
        public Object get() {
            javax.inject.Provider provider = injector.getInstance(providerClazz);
            return provider.get();
        }
    }
}
