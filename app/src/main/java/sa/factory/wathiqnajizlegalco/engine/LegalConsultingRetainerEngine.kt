package sa.factory.wathiqnajizlegalco.engine

/**
 * LegalConsultingRetainerEngine
 *
 * Implements Saudi legal retainer contracts, corporate advisory billing,
 * arbitration tribunal fee modeling (SCCA compliant), and Saudi ID/CR validation.
 *
 * Invariant: Strictly Zero Unicode Emojis.
 */
class LegalConsultingRetainerEngine {

    data class RetainerAgreementPlan(
        val planName: String,
        val clientCrOrId: String,
        val durationMonths: Int,
        val monthlyRetainerSar: Double,
        val includedAdvisoryHoursPerMonth: Int,
        val extraHourlyRateSar: Double,
        val extraHoursLogged: Int = 0,
        val includeArbitrationCoverage: Boolean = false,
        val arbitrationTribunalDepositSar: Double = 5000.0
    )

    data class RetainerInvoiceStatement(
        val baseRetainerSar: Double,
        val extraAdvisorySar: Double,
        val arbitrationDepositSar: Double,
        val subtotalSar: Double,
        val vat15Sar: Double,
        val totalDueSar: Double
    )

    companion object {
        private val SAUDI_ID_REGEX = Regex("^[12]\\d{9}$")
        private val SAUDI_CR_REGEX = Regex("^[17]\\d{9}$")
    }

    fun isValidSaudiId(id: String): Boolean {
        return SAUDI_ID_REGEX.matches(id)
    }

    fun isValidSaudiCommercialRegistration(cr: String): Boolean {
        return SAUDI_CR_REGEX.matches(cr)
    }

    fun calculateRetainerStatement(plan: RetainerAgreementPlan): RetainerInvoiceStatement {
        require(plan.durationMonths > 0) { "Contract duration must be at least 1 month" }
        require(isValidSaudiId(plan.clientCrOrId) || isValidSaudiCommercialRegistration(plan.clientCrOrId)) {
            "Client identifier must be a valid 10-digit Saudi National ID or Commercial Registration"
        }

        val baseRetainer = plan.monthlyRetainerSar * plan.durationMonths
        val extraAdvisory = plan.extraHoursLogged * plan.extraHourlyRateSar
        val arbitrationDeposit = if (plan.includeArbitrationCoverage) plan.arbitrationTribunalDepositSar else 0.0

        val subtotal = baseRetainer + extraAdvisory + arbitrationDeposit
        val vat = Math.round(subtotal * 0.15 * 100.0) / 100.0
        val total = Math.round((subtotal + vat) * 100.0) / 100.0

        return RetainerInvoiceStatement(
            baseRetainerSar = Math.round(baseRetainer * 100.0) / 100.0,
            extraAdvisorySar = Math.round(extraAdvisory * 100.0) / 100.0,
            arbitrationDepositSar = Math.round(arbitrationDeposit * 100.0) / 100.0,
            subtotalSar = Math.round(subtotal * 100.0) / 100.0,
            vat15Sar = vat,
            totalDueSar = total
        )
    }
}
