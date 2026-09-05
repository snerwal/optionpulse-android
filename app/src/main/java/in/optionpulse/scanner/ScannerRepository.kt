package com.optionpulse.scanner

import com.optionpulse.scanner.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST

interface ScannerRepository {
    suspend fun marketStatus(): MarketStatus
    suspend fun signals(): List<Signal>
}

interface ScannerApi {
    @GET("v1/status") suspend fun status(): MarketStatus
    @GET("v1/signals") suspend fun signals(): List<Signal>
    @POST("v1/devices/register") suspend fun registerDevice(@Body request: DeviceToken)
}

data class DeviceToken(val token: String)

class LiveScannerRepository(private val api: ScannerApi = ApiFactory.api) : ScannerRepository {
    override suspend fun marketStatus() = api.status()
    override suspend fun signals() = api.signals()
}

internal object ApiFactory {
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
            if (BuildConfig.DEVICE_ENROLLMENT_TOKEN.isNotBlank()) builder.header("Authorization", "Bearer ${BuildConfig.DEVICE_ENROLLMENT_TOKEN}")
            chain.proceed(builder.build())
        }
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()
    val api: ScannerApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(ScannerApi::class.java)
}

class DemoScannerRepository : ScannerRepository {
    override suspend fun marketStatus() = MarketStatus()
    override suspend fun signals() = listOf(
        Signal("1", "VOLTAS", Direction.CALL, "BULLISH BREAKOUT", 1440.50, 85,
            "VOLTAS SEP 1450 CE", 38.50, 0.5, 184_500, 96_240, 1400.0, 1418.77, 1437.67, 1475.83, 1553.67,
            1418.70, 30.0, 16.2, "10:15:02 IST",
            listOf(Check("Gann 90° close", true, "Closed above ₹1,437.67"), Check("Volume surge", true, "3.1× SMA20"),
                Check("Anchored VWAP", true, "Spot above VWAP"), Check("Pattern", true, "Bullish Marubozu"),
                Check("Astro guard", true, "Moon–Saturn 18.4°"), Check("OI wall", true, "No wall within 0.5%"))),
        Signal("2", "BALKRISIND", Direction.PUT, "BEARISH BREAKDOWN", 2748.20, 82,
            "BALKRISIND SEP 2700 PE", 44.10, 0.8, 131_000, 72_800, 2800.0, 2773.60, 2747.33, 2695.17, 2592.34,
            2773.70, 34.0, 13.8, "11:05:12 IST",
            listOf(Check("Gann 90° close", true, "Closed below ₹2,747.33"), Check("Volume surge", true, "2.8× SMA20"),
                Check("Anchored VWAP", true, "Spot below VWAP"), Check("Pattern", true, "Bearish Engulfing"),
                Check("Astro guard", true, "No conjunction"), Check("OI wall", true, "Clear below spot")))
    )
}
