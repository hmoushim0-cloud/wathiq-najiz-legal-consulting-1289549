package sa.factory.wathiqnajizlegalco

import org.junit.Assert.*
import org.junit.Test
import sa.factory.wathiqnajizlegalco.engine.LegalConsultingRetainerEngine
import sa.factory.wathiqnajizlegalco.engine.NajizDigitalNotaryProtocolEngine
import java.io.File

class WathiqNajizRegressionTest {

    private val notaryEngine = NajizDigitalNotaryProtocolEngine()
    private val retainerEngine = LegalConsultingRetainerEngine()

    private val samplePoa = NajizDigitalNotaryProtocolEngine.PowerOfAttorneyDocument(
        poaNumber = "NAJIZ-2026-44892011",
        principalNationalId = "1023456789",
        agentNationalId = "1098765432",
        status = NajizDigitalNotaryProtocolEngine.PowerOfAttorneyStatus.ACTIVE,
        issueDateIso = "2026-01-01",
        expiryDateIso = "2026-12-31",
        authorizedScopes = setOf(
            NajizDigitalNotaryProtocolEngine.LegalPowerScope.LITIGATION_AND_DEFENSE,
            NajizDigitalNotaryProtocolEngine.LegalPowerScope.CONTRACT_EXECUTION
        )
    )

    @Test
    fun testNajizPoaNumberFormatValidation() {
        assertTrue(notaryEngine.isValidPoaNumberFormat("NAJIZ-2026-44892011"))
        assertTrue(notaryEngine.isValidPoaNumberFormat("NAJIZ-2025-10002000"))
        assertFalse(notaryEngine.isValidPoaNumberFormat("WATHIQ-2026-12345678")) // wrong prefix
        assertFalse(notaryEngine.isValidPoaNumberFormat("NAJIZ-26-12345678"))     // 2-digit year
        assertFalse(notaryEngine.isValidPoaNumberFormat("NAJIZ-2026-1234"))         // short number
    }

    @Test
    fun testSha256ContractHashing() {
        val contractText = "عقد استشارات قانونية موثق بين الطرفين"
        val hash = notaryEngine.computeSha256Hash(contractText)
        assertNotNull(hash)
        assertEquals(64, hash.length) // 64 hex characters for SHA-256
    }

    @Test
    fun testPoaAuthorizationGrantedForLitigation() {
        val currentMs = 1794400000000L
        val expiryMs = 1794500000000L
        val (isAuth, reason) = notaryEngine.isActionAuthorizedUnderPoa(
            poa = samplePoa,
            requiredScope = NajizDigitalNotaryProtocolEngine.LegalPowerScope.LITIGATION_AND_DEFENSE,
            currentEpochMs = currentMs,
            expiryEpochMs = expiryMs
        )
        assertTrue(isAuth)
        assertTrue(reason.contains("Authorized"))
    }

    @Test
    fun testPoaAuthorizationDeniedForMissingScope() {
        val currentMs = 1794400000000L
        val expiryMs = 1794500000000L
        val (isAuth, reason) = notaryEngine.isActionAuthorizedUnderPoa(
            poa = samplePoa,
            requiredScope = NajizDigitalNotaryProtocolEngine.LegalPowerScope.REAL_ESTATE_CONVEYANCE, // not granted
            currentEpochMs = currentMs,
            expiryEpochMs = expiryMs
        )
        assertFalse(isAuth)
        assertTrue(reason.contains("not granted"))
    }

    @Test
    fun testPoaAuthorizationDeniedForExpiredStatus() {
        val currentMs = 1794400000000L
        val expiryMs = 1794500000000L
        val revokedPoa = samplePoa.copy(status = NajizDigitalNotaryProtocolEngine.PowerOfAttorneyStatus.REVOKED)
        val (isAuth, reason) = notaryEngine.isActionAuthorizedUnderPoa(
            poa = revokedPoa,
            requiredScope = NajizDigitalNotaryProtocolEngine.LegalPowerScope.LITIGATION_AND_DEFENSE,
            currentEpochMs = currentMs,
            expiryEpochMs = expiryMs
        )
        assertFalse(isAuth)
        assertTrue(reason.contains("REVOKED"))
    }

    @Test
    fun testHsmContractSealingAndVerification() {
        val contractId = "CONTRACT-1289549-001"
        val contractBody = "تم الاتفاق على تمثيل الشركة أمام المحاكم التجارية بالرياض"
        val hsmKey = "secret-hsm-fips-token-2026"
        val timestamp = 1794400000000L

        val seal = notaryEngine.generateHsmContractSeal(contractId, contractBody, hsmKey, timestamp)
        assertNotNull(seal.cryptographicSealHex)
        assertTrue(seal.cryptographicSealHex.isNotEmpty())

        val isValid = notaryEngine.verifyContractSeal(seal, contractBody, hsmKey)
        assertTrue(isValid)
    }

    @Test
    fun testHsmContractSealRejectionOnTamperedContent() {
        val contractId = "CONTRACT-1289549-002"
        val contractBody = "نص العقد الأصلي المعنون"
        val tamperedBody = "نص العقد المعدل بعد التوقيع"
        val hsmKey = "secret-hsm-fips-token-2026"
        val timestamp = 1794400000000L

        val seal = notaryEngine.generateHsmContractSeal(contractId, contractBody, hsmKey, timestamp)
        val isValid = notaryEngine.verifyContractSeal(seal, tamperedBody, hsmKey)
        assertFalse(isValid)
    }

    @Test
    fun testSaudiNationalIdAndCrValidation() {
        assertTrue(retainerEngine.isValidSaudiId("1023456789"))
        assertTrue(retainerEngine.isValidSaudiId("2098765432"))
        assertFalse(retainerEngine.isValidSaudiId("3023456789")) // starts with 3
        assertFalse(retainerEngine.isValidSaudiId("102345678"))  // 9 digits

        assertTrue(retainerEngine.isValidSaudiCommercialRegistration("1010123456"))
        assertTrue(retainerEngine.isValidSaudiCommercialRegistration("7001234567"))
        assertFalse(retainerEngine.isValidSaudiCommercialRegistration("2010123456")) // starts with 2
    }

    @Test
    fun testRetainerStatementCalculationWithVatAndArbitration() {
        val plan = LegalConsultingRetainerEngine.RetainerAgreementPlan(
            planName = "الباقة القانونية الشاملة للشركات",
            clientCrOrId = "7001234567",
            durationMonths = 6,
            monthlyRetainerSar = 5000.0, // 30,000 SAR
            includedAdvisoryHoursPerMonth = 15,
            extraHourlyRateSar = 400.0,
            extraHoursLogged = 5,        // 2,000 SAR
            includeArbitrationCoverage = true,
            arbitrationTribunalDepositSar = 5000.0 // 5,000 SAR
        )
        val statement = retainerEngine.calculateRetainerStatement(plan)
        assertEquals(30000.0, statement.baseRetainerSar, 0.01)
        assertEquals(2000.0, statement.extraAdvisorySar, 0.01)
        assertEquals(5000.0, statement.arbitrationDepositSar, 0.01)
        assertEquals(37000.0, statement.subtotalSar, 0.01)
        assertEquals(5550.0, statement.vat15Sar, 0.01) // 37,000 * 0.15
        assertEquals(42550.0, statement.totalDueSar, 0.01)
    }

    @Test
    fun testStrictZeroUnicodeEmojiInvariant() {
        val repoDir = File("/root/sidekick-workspace/wathiq-najiz-legal-consulting-1289549")
        val emojiRanges = listOf(
            0x1F600..0x1F64F, 0x1F300..0x1F5FF, 0x1F680..0x1F6FF,
            0x1F700..0x1F77F, 0x1F780..0x1F7FF, 0x1F800..0x1F8FF,
            0x1F900..0x1F9FF, 0x1FA00..0x1FA6F, 0x1FA70..0x1FAFF,
            0x2600..0x26FF,   0x2700..0x27BF,   0xFE00..0xFE0F,
            0x1F1E6..0x1F1FF
        )

        repoDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension in listOf("kt", "java", "xml", "md", "gradle", "kts", "json", "txt")) {
                if (!file.path.contains("/.git/") && !file.path.contains("/build/") && !file.path.contains("/.gradle/")) {
                    val content = file.readText()
                    for (ch in content) {
                        val cp = ch.code
                        for (range in emojiRanges) {
                            assertFalse("Found forbidden emoji code point U+${cp.toString(16)} in ${file.name}", cp in range)
                        }
                    }
                }
            }
        }
    }
}
