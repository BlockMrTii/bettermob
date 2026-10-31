package eu.northsoft.bettermob.skill.mechanic;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.SkillTags.TAG_PREFIX;

public final class TagMechanic implements Mechanic {
    private final boolean add;

    public TagMechanic(boolean add) {
        this.add = add;
    }

    @Override
    public void execute(MechanicCall call) {
        String tag = firstParam(call.params(), "t", "tag");
        if (tag == null || call.target().entity() == null) return;
        if (add) call.target().entity().addScoreboardTag(TAG_PREFIX + tag.trim());
        else call.target().entity().removeScoreboardTag(TAG_PREFIX + tag.trim());
    }
}
