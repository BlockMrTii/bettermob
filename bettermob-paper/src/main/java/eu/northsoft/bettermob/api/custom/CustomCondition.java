package eu.northsoft.bettermob.api.custom;

@FunctionalInterface
public interface CustomCondition {
    boolean test(ConditionContext context);
}
