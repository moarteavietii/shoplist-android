package eu.pascariu.shoppinglist.testutil

import eu.pascariu.shoppinglist.data.network.HealthApi
import eu.pascariu.shoppinglist.data.network.HealthResponse
import eu.pascariu.shoppinglist.data.network.ShopProbeEntry
import retrofit2.HttpException
import retrofit2.Response

class FakeHealthApi(
    var healthBehavior: () -> HealthResponse = { HealthResponse(status = "ok") },
    var probeBehavior: () -> List<ShopProbeEntry> = { emptyList() },
) : HealthApi {
    override suspend fun getHealth(): HealthResponse = healthBehavior()

    override suspend fun probeShops(clientId: String, clientSecret: String): List<ShopProbeEntry> = probeBehavior()
}

fun httpException(code: Int): HttpException =
    HttpException(Response.error<Any>(code, okhttp3.ResponseBody.create(null, "")))
