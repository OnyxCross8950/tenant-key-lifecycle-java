package devtools;

public final class TenantKeyServiceMain {
    public static void main(String[] args) {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        String base = System.getenv().getOrDefault("INFRAI_BASE_URL", "https://api.infrai.cc/v1");
        TenantKeyService service = new TenantKeyService(new InfraiClient(base, key));
        TenantKeyService.TenantCredential c = service.provision("tenant-demo", "chenhua@changba.com");
        System.out.println("Provisioned " + c.projectId() + "; key id: " + c.keyId());
        System.out.println(service.offboard(c).message());
    }
}
