package eu.northsoft.bettermob.skill.condition;

import org.bukkit.block.Block;

final class BlockTypes {
    private BlockTypes() {}

    static boolean contains(String paramsRaw, Block block) {
        if (paramsRaw == null) return false;
        int eq = paramsRaw.indexOf('=');
        String list = eq < 0 ? paramsRaw : paramsRaw.substring(eq + 1);
        for (String type : list.split(",")) {
            if (type.trim().equalsIgnoreCase(block.getType().name())) return true;
        }
        return false;
    }
}
