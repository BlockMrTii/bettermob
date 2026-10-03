package eu.northsoft.bettermob.skill.mechanic;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class IgniteMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        if (call.target().entity() != null) {
            call.target().entity().setFireTicks(parseInt(firstParam(call.params(), "t", "ticks", "duration", "d"), 100));
        }
    }
}
