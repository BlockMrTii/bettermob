package eu.northsoft.bettermob.skill.mechanic;

public interface Mechanic {
    void execute(MechanicCall call);

    default boolean runsOnTarget() {
        return true;
    }
}
