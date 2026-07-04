package pk.fuelgo.customer

import android.content.Context
import pk.fuelgo.customer.data.local.ApiConfigManager
import pk.fuelgo.customer.data.local.TokenManager
import pk.fuelgo.customer.data.remote.NetworkModule
import pk.fuelgo.customer.data.repository.AuthRepository
import pk.fuelgo.customer.data.repository.CustomerRepository
import pk.fuelgo.customer.data.repository.OrderRepository

/**
 * Minimal manual dependency container — the app is small enough that pulling in
 * Hilt/Koin would add more ceremony than it saves. One instance lives on [FuelGoApp]
 * for the whole process lifetime.
 */
class AppContainer(context: Context) {
    private val tokenManager = TokenManager(context.applicationContext)
    val apiConfigManager = ApiConfigManager(context.applicationContext)
    private val networkModule = NetworkModule(tokenManager, apiConfigManager)

    val authRepository = AuthRepository(networkModule.apiService, tokenManager, apiConfigManager)
    val customerRepository = CustomerRepository(networkModule.apiService)
    val orderRepository = OrderRepository(networkModule.apiService)
}
