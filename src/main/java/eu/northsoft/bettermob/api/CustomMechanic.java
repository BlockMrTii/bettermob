package eu.northsoft.bettermob.api;

/** Eigene Skill-Mechanic, die per {@link BetterMobAPI#registerMechanic} in Skill-Zeilen nutzbar wird. */
@FunctionalInterface
public interface CustomMechanic {
    void execute(MechanicContext context);
}
