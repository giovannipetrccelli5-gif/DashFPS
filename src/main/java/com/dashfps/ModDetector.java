package com.dashfps;

import net.fabricmc.loader.api.FabricLoader;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class ModDetector {
    private static final LinkedHashMap<String, String> KNOWN = new LinkedHashMap<>();
    private static final LinkedHashMap<String, String> DETECTED = new LinkedHashMap<>();

    static {
        KNOWN.put("sodium", "Sodium");
        KNOWN.put("lithium", "Lithium");
        KNOWN.put("ferritecore", "FerriteCore");
        KNOWN.put("entityculling", "Entity Culling");
        KNOWN.put("moreculling", "MoreCulling");
        KNOWN.put("immediatelyfast", "ImmediatelyFast");
        KNOWN.put("krypton", "Krypton");
        KNOWN.put("c2me", "C2ME");
        KNOWN.put("scalablelux", "ScalableLux");
        KNOWN.put("modernfix", "ModernFix");
        KNOWN.put("badoptimizations", "BadOptimizations");
    }

    private ModDetector() {}

    public static void scan() {
        DETECTED.clear();
        FabricLoader loader = FabricLoader.getInstance();
        KNOWN.forEach((id, name) -> {
            if (loader.isModLoaded(id)) DETECTED.put(id, name);
        });
    }

    public static boolean loaded(String id) {
        return DETECTED.containsKey(id);
    }

    public static int count() {
        return DETECTED.size();
    }

    public static String names() {
        return DETECTED.isEmpty() ? "none" : String.join(", ", DETECTED.values());
    }

    public static String ids() {
        return DETECTED.isEmpty() ? "none" : String.join(", ", DETECTED.keySet());
    }

    public static Map<String, String> detected() {
        return Map.copyOf(DETECTED);
    }

    public static String delegationSummary() {
        return DETECTED.entrySet().stream().map(entry -> switch (entry.getKey()) {
            case "sodium" -> "Sodium: DashFPS does not replace chunk rendering, frustum/occlusion culling, or atlas work";
            case "lithium" -> "Lithium: DashFPS does not replace game-logic/tick optimizations";
            case "entityculling", "moreculling" -> entry.getValue() + ": DashFPS does not implement entity/render culling";
            case "immediatelyfast" -> "ImmediatelyFast: DashFPS does not replace immediate-mode/render fast paths";
            case "krypton" -> "Krypton: DashFPS does not replace network optimization";
            case "scalablelux" -> "ScalableLux: DashFPS does not replace the lighting engine";
            case "ferritecore", "modernfix" -> entry.getValue() + ": DashFPS does not replace memory optimization";
            default -> entry.getValue() + ": detected and left in charge of its own optimization domain";
        }).collect(Collectors.joining(" | "));
    }
}