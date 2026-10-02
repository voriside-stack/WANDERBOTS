package dev.voriside.wanderbots;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SkinService {
    private static final Pattern PLAYER_PATTERN = Pattern.compile(
            "@([A-Za-z0-9_]{3,16})[^\\n]{0,220}?UUID:\\s*([0-9a-fA-F]{32}|[0-9a-fA-F-]{36})");
    private static final Pattern NEXT_PATTERN = Pattern.compile("/players\\?after=(\\d+)");
    private static final Pattern VALUE_PATTERN = Pattern.compile("\\\"value\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern SIGNATURE_PATTERN = Pattern.compile("\\\"signature\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private final WanderBotsPlugin plugin;
    private final HttpClient http;
    private final Map<String, SkinData> cache = new LinkedHashMap<>();
    private final List<SkinCandidate> candidates = new ArrayList<>();
    private final Object lock = new Object();

    SkinService(WanderBotsPlugin plugin) {
        this.plugin = plugin;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
        seedBuiltIns();
        loadCache();
    }

    private void seedBuiltIns() {
        addCandidate("steve", "8667ba71-b85a-4004-af54-457a9734eed7");
        addCandidate("dream", "ec70bcaf-702f-4bb8-b48d-276fa52a780c");
        addCandidate("fariis47", "b18bc077-9bfa-4803-8220-82d14fb4d4d4");
        addCandidate("black", "e8771b28-62e6-410e-8ceb-41b825f13737");
        addCandidate("wemmbu", "8af6c508-33d5-4861-b5c2-dce1c16c6fae");
        addCandidate("yusufte", "c8a10600-4f1a-45de-8219-e231f27e3519");
        addCandidate("BEROiq", "cec1df3a-eaa7-445a-a528-c12d2cda7407");
        addCandidate("verity", "472b3843-15c5-4490-9510-73b37107ab15");
        addCandidate("gojo", "8b6e6236-f651-43db-bf74-85f6ef459979");
        addCandidate("white", "a7730ba6-6143-4cfb-59c0-ccbb66776aa1");
        addCandidate("suit", "ba50ad31-cf4b-4ab5-acb4-8717fb480d14");
        addCandidate("cat", "f0dd2877-9aed-471f-ba45-07674c0e9f8e");
        addCandidate("anime", "c7ec5fcd-7714-44bf-981e-4b72f4e329fd");
        addCandidate("herobrine", "f84c6a79-0a4e-45e0-879b-cd49ebd4c4e2");
        addCandidate("villager", "a05deec0-a7e0-4076-87cb-dc5dcaca8586");
        addCandidate("Samnuzz", "edccd5b9-d089-4c3a-9fee-628d1d058e1c");
        addCandidate("flamefrags", "8ff75664-6afd-4b6f-9677-75d8120943f1");
        addCandidate("luffy", "12c4e186-5700-42c3-a6b8-56c05f72aab1");
        addCandidate("zombie", "02b0e86d-c86a-4ae7-bc41-015d21f80c1c");
        addCandidate("king", "b2b5e42c-3c1d-46e7-a9e9-b968e5b2af13");
        addCandidate("DaquavisMC", "4a9ace26-563d-4f63-b601-0abadd03023a");
        addCandidate("knight", "457b7d2c-258f-4a1b-9243-78835e4f1e23");
        addCandidate("duck", "7aa189da-5e49-4d07-bf34-c2ec7f8133b1");
        addCandidate("creeper", "696581df-4256-4028-b55e-9452b4de40b6");
        addCandidate("noob", "17361536-e51c-4795-829c-7f4224c547c3");
        addCandidate("sukuna", "f3bfe665-54e7-40b7-ba36-5ca49186bb24");
        addCandidate("arvexx", "a6c64542-bc5d-40cf-a0c9-95b59f53f137");
        addCandidate("femboy", "33dc6d28-fa37-4d42-9903-97fe6143a7f1");
        addCandidate("boralo", "547d5b11-57b9-47d5-883d-f3d1f3c19dde");
        addCandidate("algomi", "38cbbc5c-8442-405f-a1f3-2596413c1fea");
        addCandidate("emo", "46520052-83ed-41f8-924c-7b80da037b09");
        addCandidate("zoktay", "b94d128e-2cb2-44c1-9f3d-511ab09414c6");
        addCandidate("goku", "3929e052-bad7-4b74-8177-4d5ba591fbd4");
        addCandidate("blue", "bb83bdc4-e867-4dcc-b656-8b49ad783596");
        addCandidate("enderman", "eb9adf76-efb5-41b4-b6e6-36a5455c9ff2");
        addCandidate("cool", "a85435bd-65ea-41c0-89e6-bef44d602330");
        addCandidate("army", "0b25d996-4005-4bcb-96c1-d74d51a871de");

        addCandidate("skin", "e67073b2-3b6b-4c78-aa9a-de3dbe379d0a");
        addCandidate("neymar", "47384575-f882-45fe-b411-483f6ab1ecf8");
        addCandidate("pokemon", "c6e24f93-205f-4ac2-a59b-7dbb48dd187e");
        addCandidate("hair", "8153155b-a6ba-4ff0-a352-b7ac7eb8a139");
        addCandidate("toji", "5867d21b-4b9b-42f5-a948-5c51d3a290d7");
        addCandidate("jenny", "532c5a54-db5b-437b-9055-e74e51369ca4");
        addCandidate("maid", "7192af42-d15e-4523-b374-04804e1972c5");
        addCandidate("pvp", "0113a2ab-a768-477b-aa4a-566f0f093a64");
        addCandidate("batman", "91da284d-b051-488c-a027-0e7452b3fdbb");
        addCandidate("cape", "a082180c-f6e4-481b-8625-7089b62febcb");
        addCandidate("base", "eba1796e-6c4b-4822-9724-96501f972b96");
        addCandidate("skins", "e1574da5-b857-4434-a67f-ebfd6540944c");
        addCandidate("skeleton", "6d959fcc-e0ca-44ff-8d49-f4a2ae9f8de8");
        addCandidate("messi", "ea53f492-838d-4d4b-b470-68a8c31be58c");
        addCandidate("YGG90", "917b31e9-72c3-4807-b643-80a578ace758");
        addCandidate("terno", "37ade971-c8d3-48b2-af7d-19ea56be558f");
        addCandidate("dog", "ec071825-56b4-44d3-8e9a-f48bf74bde82");
        addCandidate("furry", "8de9f5ea-1ddc-49f0-aac9-4828d1ac5935");
        addCandidate("horror", "73ad769d-f840-4919-a035-a7f00243afed");
        addCandidate("demon", "e1bd0526-aa8e-4127-a214-4cee0223f26b");
        addCandidate("purple", "d5be0d2f-2fe5-43f6-bde6-04f89e49ce38");
        addCandidate("brasil", "462a380a-c7ab-49f5-bb9a-211368ff2439");
        addCandidate("adispot", "deed59ad-ea24-4155-a19e-76fe2a0ae99e");
        addCandidate("sonic", "c00da1a7-12e3-4dbf-bfeb-9dc4b79d90ad");
        addCandidate("cute", "ad4a7397-4bcb-4b71-a583-7d6be556a573");
        addCandidate("ronaldo", "ba8e2a26-0a18-48a3-a76d-ba917f1ebcd8");
        addCandidate("green", "2e65b684-7a2f-4dd4-ac80-f61a6a6adfe5");
        addCandidate("funny", "73dc7aa6-6807-4fb5-ad2f-8cd32ca03204");
        addCandidate("roblox", "90d03bce-c958-4146-897d-c46f3ce01c75");
        addCandidate("icrimax", "105bbcff-0750-4754-ab27-01ea95f54971");
        addCandidate("hacker", "dd5e3b0b-6d24-440d-a740-ebdbddb6e76b");
        addCandidate("miku", "7a64b4f8-4954-4704-aeb8-2c9bf0629bef");
        addCandidate("fnaf", "216884fb-8ddf-4b7f-922c-6676e27a553b");

        // A few long-lived, well-known profiles.
        addCandidate("Notch", "069a79f4-44e9-4726-a5be-fca90e38aaf5");
        addCandidate("jeb_", "853c80ef-3c37-49fd-aa49-938b674adae4");
        addCandidate("CaptainSparklez", "b4ac2f6c-3f43-4f1c-a0f2-13e2b118e7e0");
        addCandidate("Grian", "22b7e2f6-ec65-4b9e-bb7f-4b5c7546b2e2");
        addCandidate("MumboJumbo", "7c0c7b40-5aa2-4c24-8a2d-26f7f5ae0c7d");
    }

    private void addCandidate(String name, String uuidText) {
        try {
            UUID uuid = UUID.fromString(uuidText);
            synchronized (lock) {
                boolean exists = candidates.stream().anyMatch(c -> c.uuid().equals(uuid));
                if (!exists) {
                    candidates.add(new SkinCandidate(name, uuid));
                }
            }
        } catch (IllegalArgumentException ignored) {
            plugin.getLogger().warning("Skipping malformed skin seed UUID for " + name);
        }
    }

    void startDiscovery() {
        if (!plugin.getConfig().getBoolean("skins.auto-discover", true)) {
            return;
        }
        CompletableFuture.runAsync(this::discoverCandidates)
                .exceptionally(error -> {
                    plugin.getLogger().warning("Skin discovery failed: " + error.getMessage());
                    return null;
                });
    }

    private void discoverCandidates() {
        int target = Math.max(100, plugin.getConfig().getInt("skins.discovery-target", 320));
        int maxPages = Math.max(1, plugin.getConfig().getInt("skins.discovery-pages", 8));
        String url = "https://minecraft.novaskin.me/players";
        int pages = 0;

        while (url != null && pages < maxPages) {
            String html = get(url);
            if (html == null) {
                return;
            }

            Matcher players = PLAYER_PATTERN.matcher(html);
            while (players.find()) {
                String name = players.group(1);
                String rawUuid = players.group(2);
                if (rawUuid.length() == 32) {
                    rawUuid = rawUuid.substring(0, 8) + "-" + rawUuid.substring(8, 12) + "-"
                            + rawUuid.substring(12, 16) + "-" + rawUuid.substring(16, 20) + "-" + rawUuid.substring(20);
                }
                addCandidate(name, rawUuid);
                synchronized (lock) {
                    if (candidates.size() >= target) {
                        plugin.getLogger().info("Skin candidate pool reached " + candidates.size() + " entries.");
                        return;
                    }
                }
            }

            String next = null;
            Matcher nextMatcher = NEXT_PATTERN.matcher(html);
            while (nextMatcher.find()) {
                next = "https://minecraft.novaskin.me/players?after=" + nextMatcher.group(1);
            }
            url = next;
            pages++;
        }

        synchronized (lock) {
            plugin.getLogger().info("Skin discovery finished with " + candidates.size() + " candidate profiles.");
        }
    }

    CompletableFuture<GameProfile> createProfile(String botName) {
        SkinCandidate candidate;
        synchronized (lock) {
            if (candidates.isEmpty()) {
                return CompletableFuture.completedFuture(new GameProfile(UUID.randomUUID(), botName));
            }
            candidate = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        }

        SkinData alreadyCached;
        synchronized (lock) {
            alreadyCached = cache.get(candidate.uuid().toString());
        }

        if (alreadyCached != null) {
            return CompletableFuture.completedFuture(buildProfile(botName, alreadyCached));
        }

        return fetchTexture(candidate.uuid()).handle((skin, error) -> {
            if (skin == null || error != null) {
                synchronized (lock) {
                    SkinData fallback = cache.values().stream().findAny().orElse(null);
                    return fallback != null ? buildProfile(botName, fallback) : new GameProfile(UUID.randomUUID(), botName);
                }
            }
            synchronized (lock) {
                cache.put(candidate.uuid().toString(), skin);
            }
            return buildProfile(botName, skin);
        });
    }

    private GameProfile buildProfile(String botName, SkinData skin) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), botName);
        profile.getProperties().put("textures", new Property("textures", skin.value(), skin.signature()));
        return profile;
    }

    private CompletableFuture<SkinData> fetchTexture(UUID uuid) {
        String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false";
        return CompletableFuture.supplyAsync(() -> {
            String json = get(url);
            if (json == null) {
                return null;
            }
            Matcher value = VALUE_PATTERN.matcher(json);
            Matcher signature = SIGNATURE_PATTERN.matcher(json);
            if (!value.find() || !signature.find()) {
                return null;
            }
            return new SkinData(value.group(1), signature.group(1));
        });
    }

    private String get(String url) {
        try {
            String ua = plugin.getConfig().getString("skins.user-agent", "WanderBots/1.0");
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", ua)
                    .header("Accept", "application/json,text/html;q=0.9,*/*;q=0.8")
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return null;
            }
            return response.body();
        } catch (IOException | InterruptedException | RuntimeException error) {
            return null;
        }
    }

    void loadCache() {
        String fileName = plugin.getConfig().getString("skins.cache-file", "skin-cache.yml");
        Path path = plugin.getDataFolder().toPath().resolve(fileName);
        if (!Files.exists(path)) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(path.toFile());
        ConfigurationSection root = yaml.getConfigurationSection("skins");
        if (root == null) {
            return;
        }
        synchronized (lock) {
            for (String key : root.getKeys(false)) {
                String value = root.getString(key + ".value");
                String signature = root.getString(key + ".signature");
                if (value != null && signature != null) {
                    cache.put(key, new SkinData(value, signature));
                }
            }
        }
    }

    void saveCache() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            YamlConfiguration yaml = new YamlConfiguration();
            synchronized (lock) {
                for (Map.Entry<String, SkinData> entry : cache.entrySet()) {
                    String path = "skins." + entry.getKey();
                    yaml.set(path + ".value", entry.getValue().value());
                    yaml.set(path + ".signature", entry.getValue().signature());
                }
            }
            String fileName = plugin.getConfig().getString("skins.cache-file", "skin-cache.yml");
            yaml.save(plugin.getDataFolder().toPath().resolve(fileName).toFile());
        } catch (IOException error) {
            plugin.getLogger().warning("Could not save skin cache: " + error.getMessage());
        }
    }

    int candidateCount() {
        synchronized (lock) {
            return candidates.size();
        }
    }

    record SkinCandidate(String name, UUID uuid) {}
    record SkinData(String value, String signature) {}
}
