package eu.pascariu.shoppinglist.testutil

import eu.pascariu.shoppinglist.data.network.HealthApi
import eu.pascariu.shoppinglist.data.network.HealthResponse
import retrofit2.HttpException
import retrofit2.Response

class FakeHealthApi(var behavior: () -> HealthResponse = {
    HealthResponse(status = "ok")
}) : HealthApi {
    override suspend fun getHealth(): HealthResponse = behavior()

    override suspend fun probeHealth(clientId: String, clientSecret: String): HealthResponse = behavior()
}

fun httpException(code: Int): HttpException =
    HttpException(Response.error<Any>(code, okhttp3.ResponseBody.create(null, "")))
