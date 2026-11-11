# منصة توثيق وناجز للاستشارات القانونية — العقود والوكالات (`wathiq-najiz-legal-consulting-1289549`)
> توثيق قانوني معتمد بالربط مع وزارة العدل وبوابة ناجز وحماية HSM المشفرة (FIPS 140-2)

- **Project ID**: `#1289549`
- **Commercial Archetype**: `legal_consulting`
- **Bound Hardware Peripheral**: `FIPS 140-2 Cryptographic HSM Security Token`
- **Package Name**: `sa.factory.wathiqnajizlegalco`
- **Zero-Emoji Invariant**: Strictly Enforced (100% Vector Icons)

---

## [Sidekick Autonomous Update] - Iteration 86

### Production Engines Implemented
1. **Najiz Digital Notary Protocol Engine (`NajizDigitalNotaryProtocolEngine.kt`)**:
   - Implements Saudi Ministry of Justice (MoJ / وزارة العدل) electronic Power of Attorney (الوكالة الإلكترونية) verification protocols.
   - Validates official Najiz PoA number formatting (`^NAJIZ-\\d{4}-\\d{8}$`) and lifecycle states (Active, Revoked, Expired).
   - Validates authorized legal powers and scopes (Litigation & Defense, Contract Execution, Bank Management, Real Estate Conveyance).
   - Emulates FIPS 140-2 Level 3 Hardware Security Module (HSM) digital contract signing via HMAC-SHA256 tamper-evident sealing.

2. **Legal Consulting Retainer Engine (`LegalConsultingRetainerEngine.kt`)**:
   - Implements corporate legal retainer contract modeling compliant with Saudi Bar Association standards.
   - Calculates monthly retainer fees, included advisory hours, litigation appearance allowances, and arbitration tribunal deposits (SCCA compliant).
   - Computes Real Estate/Commercial Tax (15% VAT).
   - Validates Saudi National IDs (10 digits starting with 1 or 2) and Commercial Registration numbers (10 digits starting with 1 or 7).

3. **Comprehensive Regression Test Suite (`WathiqNajizRegressionTest.kt`)**:
   - 10 unit test cases verifying Najiz PoA format validation, SHA-256 contract hashing, legal scope access authorization, revocation checks, HSM contract sealing and verification, tamper rejection, Saudi ID/CR validation, retainer fee calculation with VAT, and strict zero-emoji integrity.

---

### Build & Verification Commands
```bash
# Run unit tests
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64 ./gradlew test

# Assemble debug APK
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64 ./gradlew assembleDebug
```
