package nie.translator.rtranslatordevedition.voice_translation.engines;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Instance-scoped registry that requires explicit engine-family lookup. */
public final class EngineFactoryRegistry {
    private final Map<EngineType, EngineFactory> factories = new EnumMap<>(EngineType.class);

    public synchronized void register(EngineFactory factory) {
        Objects.requireNonNull(factory, "factory");
        EngineType type = Objects.requireNonNull(factory.getType(), "factory type");
        if (factories.containsKey(type)) {
            throw new IllegalArgumentException("Factory already registered for " + type);
        }
        factories.put(type, factory);
    }

    public synchronized EngineFactory get(EngineType type) {
        Objects.requireNonNull(type, "type");
        EngineFactory factory = factories.get(type);
        if (factory == null) {
            throw new IllegalStateException("No factory registered for " + type);
        }
        return factory;
    }
}
