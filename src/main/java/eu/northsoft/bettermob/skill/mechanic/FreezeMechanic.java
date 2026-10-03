package eu.northsoft.bettermob.skill.mechanic;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class FreezeMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        if (call.target().entity() == null) return;
        int ticks = parseInt(firstParam(call.params(), "ticks", "t", "d", "duration"), 140);
        call.target().entity().setFreezeTicks(Math.max(ticks, call.target().entity().getFreezeTicks()));
    }
}
