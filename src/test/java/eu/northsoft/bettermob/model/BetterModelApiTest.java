package eu.northsoft.bettermob.model;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BetterModelApiTest {
    private static final String BM = "kr.toxicity.model.api.";

    private record Call(String type, String method, int parameters) {
        Call(String type, String method) {
            this(type, method, 0);
        }
    }

    private static final List<Call> CALLS = List.of(
            new Call(BM + "tracker.EntityTracker", "registry"),
            new Call(BM + "tracker.EntityTrackerRegistry", "mountedHitBox"),
            new Call(BM + "tracker.EntityTrackerRegistry$MountedHitBox", "entity"),
            new Call(BM + "tracker.EntityTrackerRegistry$MountedHitBox", "hitBox"),
            new Call(BM + "tracker.EntityTrackerRegistry$MountedHitBox", "dismount"),
            new Call(BM + "nms.HitBox", "groupName"),
            new Call(BM + "bone.BoneName", "name"),
            new Call(BM + "platform.PlatformEntity", "uuid"),
            new Call(BM + "tracker.EntityTrackerRegistry", "registries"),
            new Call(BM + "tracker.EntityTrackerRegistry", "uuid"),
            new Call(BM + "tracker.EntityTrackerRegistry", "registry", 1));

    @Test
    void everyBetterModelCallTheRiderLookupMakesExistsInTheInstalledJars() throws Exception {
        String paths = System.getProperty("bettermodel.jars");
        assumeTrue(paths != null, "set -Dbettermodel.jars=<api jar>:<bukkit api jar> to run this check");
        List<URL> urls = new ArrayList<>();
        for (String path : paths.split(File.pathSeparator)) {
            assertTrue(new File(path).isFile(), () -> "bettermodel.jars names a missing file: " + path);
            urls.add(new File(path).toURI().toURL());
        }
        List<String> missing = new ArrayList<>();
        try (URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader())) {
            for (Call call : CALLS) {
                try {
                    boolean found = false;
                    for (java.lang.reflect.Method method : Class.forName(call.type(), false, loader).getMethods()) {
                        if (method.getName().equals(call.method()) && method.getParameterCount() == call.parameters()) found = true;
                    }
                    if (!found) missing.add(call.type() + "#" + call.method());
                } catch (ReflectiveOperationException | LinkageError exception) {
                    missing.add(call.type() + "#" + call.method() + " " + exception);
                }
            }
        }
        assertTrue(missing.isEmpty(), () -> String.join("\n", missing));
    }
}
