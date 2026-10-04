package eu.northsoft.bettermob.api;

@FunctionalInterface
public interface CustomPlaceholder {
    String resolve(String key, PlaceholderContext context);
}
