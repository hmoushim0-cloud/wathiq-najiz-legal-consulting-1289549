package sa.factory.wathiqnajizlegalco.data.repository

import sa.factory.wathiqnajizlegalco.domain.repository.WathiqNajizRepository
import sa.factory.wathiqnajizlegalco.hardware.HardwareSensorDriver
import sa.factory.wathiqnajizlegalco.model.WathiqNajizItem
import sa.factory.wathiqnajizlegalco.model.WathiqNajizOrder
import sa.factory.wathiqnajizlegalco.model.WathiqNajizTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WathiqNajizRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : WathiqNajizRepository {

    private val _catalog = MutableStateFlow<List<WathiqNajizItem>>(listOf(
        WathiqNajizItem("1", "عقد عمل موثق بريميوم", "عقد عمل معتمد من جهات قانونية مع ختم HSM رقمي غير قابل للتزوير", 450.0, "معتمد", "عقود عمل"),
        WathiqNajizItem("2", "خدمة فيزا عاجلة", "توفير عقد عمل خلال 72 ساعة مع أولوية التحقق عبر HSM", 750.0, "فوري", "خدمات فيزا"),
        WathiqNajizItem("3", "حزمة توثيق أساسية", "توثيق عقد عمل موجود مع تقرير سلامة HSM مشفر", 220.0, "جديد", "توثيق قانوني")
    ))
    override val catalogFlow: Flow<List<WathiqNajizItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<WathiqNajizItem>>(emptyList())
    override val activeCart: StateFlow<List<WathiqNajizItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<WathiqNajizTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            WathiqNajizTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: WathiqNajizItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: WathiqNajizItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): WathiqNajizOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = WathiqNajizOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
