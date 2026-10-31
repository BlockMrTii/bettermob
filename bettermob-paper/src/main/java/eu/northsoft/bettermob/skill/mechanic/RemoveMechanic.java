package eu.northsoft.bettermob.skill.mechanic;

public final class RemoveMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        if (call.target().entity() != null) call.target().entity().remove();
    }
}
