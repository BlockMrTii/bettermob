package eu.northsoft.bettermob.api.custom;

@FunctionalInterface
public interface CustomPlaceholder {
    String resolve(String key, PlaceholderContext context);
}
