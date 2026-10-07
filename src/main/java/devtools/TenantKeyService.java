package devtools;

import java.util.UUID;

public final class TenantKeyService {
    private final InfraiClient client;
    public TenantKeyService(InfraiClient client) { this.client = client; }
    public TenantCredential provision(String projectId, String email) {
        String user = client.createUser(email, "user-" + UUID.randomUUID());
        String userId = value(user, "user_id");
        if (userId.isBlank()) throw new IllegalStateException("user creation returned no id");
        try {
            String created = client.createKey(projectId, "key-" + UUID.randomUUID());
            String keyId = value(created, "key_id");
            if (keyId.isBlank()) throw new IllegalStateException("key creation returned no id");
            return new TenantCredential(projectId, userId, keyId, value(created, "key"));
        } catch (RuntimeException failure) {
            try { client.deleteUser(userId); }
            catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
    public Diagnostic offboard(TenantCredential c) {
        if (c.keyId().isBlank() || c.userId().isBlank()) return new Diagnostic(false, "missing ownership records");
        client.revokeKey(c.keyId());
        client.deleteUser(c.userId());
        return new Diagnostic(true, "credential revoked and owner deleted");
    }
    private String value(String json, String field) {
        String marker = "\"" + field + "\":\""; int i = json.indexOf(marker); if (i < 0) return "";
        int start = i + marker.length(), end = json.indexOf('"', start); return end < 0 ? "" : json.substring(start, end);
    }
    public record TenantCredential(String projectId, String userId, String keyId, String plaintextKey) {}
    public record Diagnostic(boolean accepted, String message) {}
    public record BuildEvent(String tenantId, String commit) {}
    public record ReleaseOperation(String tenantId, String version) {}
}
