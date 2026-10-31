package eu.northsoft.bettermob.api.custom;

@FunctionalInterface
public interface CustomMechanic {
    void execute(MechanicContext context);
}
