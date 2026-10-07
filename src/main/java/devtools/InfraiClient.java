package devtools;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;

public final class InfraiClient {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String baseUrl;
    private final String key;
    public InfraiClient(String baseUrl, String key) { this.baseUrl = baseUrl; this.key = key; }

    public String call(String method, String path, String body) {
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json").method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
                HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
                String env = r.body();
                if (env.contains("\"ok\":false")) throw new InfraiException("ENVELOPE_ERROR", env);
                if (r.statusCode() == 429 && attempt < 3) { Thread.sleep(retryMillis(r, attempt)); continue; }
                if (r.statusCode() >= 500) throw new InfraiException("TRANSPORT_ERROR", env);
                return env;
            } catch (InfraiException e) { throw e; }
            catch (Exception e) { throw new InfraiException("TRANSPORT_ERROR", e.getMessage()); }
        }
        throw new InfraiException("RETRY_EXHAUSTED", "request retry budget exhausted");
    }
    private long retryMillis(HttpResponse<String> r, int attempt) {
        return r.headers().firstValue("Retry-After").map(v -> Long.parseLong(v) * 1000L).orElse(250L * (1L << attempt));
    }
    public String createUser(String email, String idem) { return call("POST", "/auth/user/create", "{\"email\":\""+email+"\",\"name\":\"Developer\",\"idempotency_key\":\""+idem+"\"}"); }
    public String deleteUser(String userId) { return call("DELETE", "/auth/user/delete/" + userId, null); }
    public String createKey(String projectId, String idem) { return call("POST", "/account/keys/create", "{\"project_id\":\""+projectId+"\",\"name\":\"tenant-release\",\"scopes\":[\"build\",\"release\"],\"idempotency_key\":\""+idem+"\"}"); }
    public String revokeKey(String id) { return call("DELETE", "/account/keys/revoke/" + id, null); }
}
