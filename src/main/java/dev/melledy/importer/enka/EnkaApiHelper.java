package dev.melledy.importer.enka;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import dev.melledy.importer.utils.SelfExpiringHashMap;
import emu.lunarcore.util.JsonUtils;

public class EnkaApiHelper {
    private final static HttpClient client = HttpClient.newHttpClient();
    private final static SelfExpiringHashMap<Long, EnkaUserData> cache = new SelfExpiringHashMap<>();
    private final static Map<Integer, String> errorCodes = Map.of(
            400, "Wrong UID format",
            404, "Player does not exist",
            424, "Game maintenance / everything is broken after the game update",
            429, "Rate-limited",
            500, "General server error"
    );
    
    public static CompletableFuture<EnkaUserData> fetchAsync(long uid) {
        EnkaUserData data = cache.get(uid);
        if (data != null) {
            return CompletableFuture.supplyAsync(() -> data);
        }
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://enka.network/api/hsr/uid/" + uid))
                .setHeader("User-Agent", "LunarCore Importer Plugin")
                .GET()
                .build();
        
        return client.sendAsync(request, BodyHandlers.ofString()).thenApply(EnkaApiHelper::onFetch);
    }
    
    private static EnkaUserData onFetch(HttpResponse<String> res) {
        if (res.statusCode() == 200) {
            var data = JsonUtils.decode(res.body(), EnkaUserData.class);
            
            if (data.getDetailInfo() != null) {
                cache.put(data.getDetailInfo().getUid(), data, data.getTtl() * 1000);
                return data;
            } else {
                throw new RuntimeException("No user detail returned");
            }
        }
        
        // Lazy
        String errorMessage = errorCodes.getOrDefault(res.statusCode(), "Status code " + res.statusCode());
        throw new RuntimeException(errorMessage);
    }
}
