package eu.northsoft.bettermob.skill.mechanic;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

import static eu.northsoft.bettermob.skill.Params.firstParam;

public final class TitleMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        String title = firstParam(call.params(), "t", "title");
        String subtitle = firstParam(call.params(), "st", "subtitle");
        if ((title == null && subtitle == null) || !(call.target().entity() instanceof Player receiver)) return;
        String caster = call.context().caster().getName();
        Component main = title == null ? Component.empty() : MessageText.render(title, receiver, caster);
        Component sub = subtitle == null ? Component.empty() : MessageText.render(subtitle, receiver, caster);
        Title.Times times = Title.Times.times(
                ticks(MessageText.ticks(firstParam(call.params(), "fi", "fadein"), 10)),
                ticks(MessageText.ticks(firstParam(call.params(), "d", "duration", "stay"), 70)),
                ticks(MessageText.ticks(firstParam(call.params(), "fo", "fadeout"), 20)));
        receiver.showTitle(Title.title(main, sub, times));
    }

    private static Duration ticks(int ticks) {
        return Duration.ofMillis(ticks * 50L);
    }
}
