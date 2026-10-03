package eu.northsoft.bettermob.skill.mechanic;

public final class CancelEventMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        if (call.context().event() != null) call.context().event().setCancelled(true);
    }
}
