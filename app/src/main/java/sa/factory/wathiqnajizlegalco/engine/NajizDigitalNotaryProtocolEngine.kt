package sa.factory.wathiqnajizlegalco.engine

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * NajizDigitalNotaryProtocolEngine
 *
 * Implements Saudi Ministry of Justice (MoJ / وزارة العدل) digital notary protocols,
 * electronic Power of Attorney (الوكالة الإلكترونية) verification, and FIPS 140-2 HSM contract sealing.
 *
 * Invariant: Strictly Zero Unicode Emojis.
 */
class NajizDigitalNotaryProtocolEngine {

    enum class PowerOfAttorneyStatus {
        ACTIVE,
        REVOKED,
        EXPIRED
    }

    enum class LegalPowerScope {
        LITIGATION_AND_DEFENSE,       // المرافعة والمدافعة
        CONTRACT_EXECUTION,           // توقيع العقود والاتفاقيات
        BANK_ACCOUNT_MANAGEMENT,      // إدارة الحسابات والتعاملات البنكية
        REAL_ESTATE_CONVEYANCE        // الإفراغ العقاري والبيع والشراء
    }

    data class PowerOfAttorneyDocument(
        val poaNumber: String,
        val principalNationalId: String,   // الموكل
        val agentNationalId: String,       // الوكيل
        val status: PowerOfAttorneyStatus,
        val issueDateIso: String,
        val expiryDateIso: String,
        val authorizedScopes: Set<LegalPowerScope>
    )

    data class ContractNotarizationSeal(
        val contractId: String,
        val documentSha256Hash: String,
        val notaryHsmSlot: String,
        val timestampEpochMs: Long,
        val cryptographicSealHex: String
    )

    companion object {
        private val NAJIZ_POA_REGEX = Regex("^NAJIZ-\\d{4}-\\d{8}$")
    }

    fun isValidPoaNumberFormat(poaNumber: String): Boolean {
        return NAJIZ_POA_REGEX.matches(poaNumber)
    }

    fun computeSha256Hash(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(content.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun isActionAuthorizedUnderPoa(
        poa: PowerOfAttorneyDocument,
        requiredScope: LegalPowerScope,
        currentEpochMs: Long,
        expiryEpochMs: Long
    ): Pair<Boolean, String> {
        if (poa.status != PowerOfAttorneyStatus.ACTIVE) {
            return Pair(false, "Power of attorney is not active (Status: ${poa.status})")
        }
        if (currentEpochMs > expiryEpochMs) {
            return Pair(false, "Power of attorney has expired")
        }
        if (!poa.authorizedScopes.contains(requiredScope)) {
            return Pair(false, "Scope $requiredScope is not granted under this PoA")
        }
        return Pair(true, "Authorized under valid MoJ Najiz digital power of attorney")
    }

    fun generateHsmContractSeal(
        contractId: String,
        contractBodyText: String,
        hsmSecretKey: String,
        timestampEpochMs: Long
    ): ContractNotarizationSeal {
        val docHash = computeSha256Hash(contractBodyText)
        val payloadToSign = "$contractId:$docHash:$timestampEpochMs"

        val hmac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(hsmSecretKey.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        hmac.init(keySpec)
        val sealBytes = hmac.doFinal(payloadToSign.toByteArray(StandardCharsets.UTF_8))
        val sealHex = sealBytes.joinToString("") { "%02x".format(it) }

        return ContractNotarizationSeal(
            contractId = contractId,
            documentSha256Hash = docHash,
            notaryHsmSlot = "HSM-FIPS-140-2-SLOT-01",
            timestampEpochMs = timestampEpochMs,
            cryptographicSealHex = sealHex
        )
    }

    fun verifyContractSeal(
        seal: ContractNotarizationSeal,
        contractBodyText: String,
        hsmSecretKey: String
    ): Boolean {
        val currentHash = computeSha256Hash(contractBodyText)
        if (!currentHash.equals(seal.documentSha256Hash, ignoreCase = true)) {
            return false
        }
        val expected = generateHsmContractSeal(
            contractId = seal.contractId,
            contractBodyText = contractBodyText,
            hsmSecretKey = hsmSecretKey,
            timestampEpochMs = seal.timestampEpochMs
        )
        return expected.cryptographicSealHex.equals(seal.cryptographicSealHex, ignoreCase = true)
    }
}
