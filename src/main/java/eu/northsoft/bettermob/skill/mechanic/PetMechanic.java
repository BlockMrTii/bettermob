package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;

public final class PetMechanic implements Mechanic {
    private final SkillEngine engine;
    private final String name;

    public PetMechanic(SkillEngine engine, String name) {
        this.engine = engine;
        this.name = name;
    }

    @Override
    public void execute(MechanicCall call) {
        engine.plugin().messages().infoOnce("skill.petMechanicIgnored", "mechanic", name);
    }
}
