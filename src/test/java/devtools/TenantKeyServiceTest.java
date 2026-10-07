package devtools;

public final class TenantKeyServiceTest {
    public static void main(String[] args) {
        TenantKeyService.TenantCredential c = new TenantKeyService.TenantCredential("p", "u", "k", "secret");
        if (!c.keyId().equals("k") || !c.userId().equals("u")) throw new AssertionError("ownership must be complete");
        System.out.println("TenantKeyServiceTest: PASS");
    }
}
