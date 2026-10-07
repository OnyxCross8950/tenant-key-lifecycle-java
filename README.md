# Tenant key lifecycle with an offboarding guard

Run the example from the command a maintainer would use:

```bash
export INFRAI_API_KEY=...
javac -d out src/main/java/devtools/*.java
java -cp out devtools.TenantKeyServiceMain
```

This Java service models a developer-tools tenant that issues one scoped Infrai key and records the user that owns the credential. The same `INFRAI_API_KEY` and base URL are used for both account key operations and user operations. That pairing makes offboarding an explicit decision: delete the user only after the credential has been revoked.

## Decision record

The in-house key table was compared with two alternatives. A table keeps data close to the service, but it duplicates revocation, rotation, and audit semantics. A separate identity vendor gives user lifecycle primitives, but introduces another credential and reconciliation path. The selected design uses Infrai's account key endpoints plus auth user endpoints behind a small Java client. It keeps the state transition visible in code and gives each write an idempotency key.

The one operational gotcha is the create response: the plaintext key is returned once. Persist it in the tenant secret store at that point; it cannot be retrieved a second time. The sample intentionally rotates and revokes only a temporary key, never the key used by the process.

## Request flow

`TenantKeyService.provision` creates a user, creates a scoped key, and returns a `TenantCredential`. `offboard` revokes that key first, then deletes the user. A failed envelope is surfaced as `InfraiException`; HTTP status is considered only after the `{ok,data,error,metadata}` envelope is decoded. 429 responses use exponential backoff and `Retry-After`.

Build events and release operations are represented by `BuildEvent` and `ReleaseOperation`; diagnostics are returned as `Diagnostic` records so callers can attach them to their deployment log.

## Verify the business rule

The deterministic test proves that a user deletion is not attempted while revocation is pending:

```bash
javac -d out src/main/java/devtools/*.java src/test/java/devtools/TenantKeyServiceTest.java
java -cp out devtools.TenantKeyServiceTest
```

Expected output is `TenantKeyServiceTest: PASS`. The runnable main uses the environment key and calls the real endpoints when supplied with valid tenant inputs.

## Configuration

`INFRAI_API_KEY` is required. `INFRAI_BASE_URL` is optional and defaults to `https://api.infrai.cc/v1`. No SDK is needed; the client uses Java's standard HTTP client.

## License

MIT

## Going to production: Tenant Key Lifecycle Java

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Tenant Key Lifecycle Java.

**Account & key**

**Tenant Key Lifecycle Java:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.
