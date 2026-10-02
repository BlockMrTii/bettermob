package eu.northsoft.bettermob.api;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Collection;
import java.util.Optional;

/**
 * Oeffentliche Schnittstelle von BetterMob fuer andere Plugins. Wird vom Plugin beim
 * Start als Bukkit-Service registriert; ueber {@link #get()} oder den ServicesManager
 * abrufbar. In der plugin.yml des eigenen Plugins {@code depend: [BetterMob]} bzw.
 * {@code softdepend: [BetterMob]} eintragen.
 */
public interface BetterMobAPI {
    /** @throws IllegalStateException wenn BetterMob nicht geladen/aktiv ist. */
    static BetterMobAPI get() {
        RegisteredServiceProvider<BetterMobAPI> provider = org.bukkit.Bukkit.getServicesManager().getRegistration(BetterMobAPI.class);
        if (provider == null) throw new IllegalStateException("BetterMob ist nicht aktiv.");
        return provider.getProvider();
    }

    /** Mob-Definition anhand der ID (Gross-/Kleinschreibung egal). */
    Optional<MobInfo> getMob(String id);

    /** Alle registrierten Mob-Definitionen. */
    Collection<MobInfo> getMobs();

    /** True, wenn das Entity ein von BetterMob gespawnter bzw. wiederhergestellter Mob ist. */
    boolean isBetterMob(Entity entity);

    /** Definition des BetterMob-Mobs, oder leer wenn das Entity keiner ist. */
    Optional<MobInfo> getMobInfo(Entity entity);

    /** Spawnt einen registrierten Mob; leer, wenn die ID unbekannt ist. */
    Optional<LivingEntity> spawn(String id, Location location);

    /** IDs aller registrierten Skills. */
    Collection<String> getSkillIds();

    /** Fuehrt einen Skill mit dem Entity als Caster aus; false, wenn der Skill unbekannt ist. */
    boolean runSkill(String skillId, LivingEntity caster);

    /** Wie {@link #runSkill(String, LivingEntity)}, mit zusaetzlichem Ausloeser (Ziel von {@code @trigger}). */
    boolean runSkill(String skillId, LivingEntity caster, LivingEntity trigger);

    /**
     * Registriert eine eigene Mechanic, die dann in Skill-Zeilen als {@code name{param=wert} @targeter}
     * nutzbar ist. Eingebaute Mechanics koennen nicht ueberschrieben werden. Wird automatisch
     * entfernt, wenn das besitzende Plugin deaktiviert wird.
     *
     * @return false, wenn der Name bereits vergeben ist
     */
    boolean registerMechanic(Plugin owner, String name, CustomMechanic mechanic);

    /** Entfernt eine zuvor registrierte Mechanic. */
    void unregisterMechanic(String name);

    /** Laedt Config, Mobs, Skills und Packs neu - wie {@code /bettermob reload}. */
    void reload();
}
