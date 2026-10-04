package eu.northsoft.bettermob.api;

@FunctionalInterface
public interface CustomCondition {
    boolean test(ConditionContext context);
}
