package com.optionpulse.scanner

import android.content.Context
import com.optionpulse.scanner.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ScannerRepository{suspend fun marketStatus():MarketStatus;suspend fun signals():List<Signal>}
interface ScannerApi{
 @GET("v1/status")suspend fun status():MarketStatus
 @GET("v1/signals")suspend fun signals():List<Signal>
 @POST("v1/devices/register")suspend fun registerDevice(@Body request:DeviceToken)
}
data class DeviceToken(val token:String)

class LiveScannerRepository(context:Context):ScannerRepository{
 private val prefs=context.getSharedPreferences("scanner_state",Context.MODE_PRIVATE)
 override suspend fun marketStatus():MarketStatus{
  val running=prefs.getBoolean("running",false);val text=prefs.getString("status","Scanner stopped")?:"Scanner stopped"
  val scanned=Regex("""scanned (\d+)/""").find(text)?.groupValues?.get(1)?.toIntOrNull()?:0
  val alerts=Regex("""alerts (\d+)/""").find(text)?.groupValues?.get(1)?.toIntOrNull()?:0
  return MarketStatus(connected=running&&text.startsWith("LIVE"),universe=210,scanned=scanned,alertsToday=alerts,vix=0.0,latencyMs=0,mode=if(running)text else "ALERTS ONLY • SCANNER STOPPED")
 }
 override suspend fun signals():List<Signal> = emptyList()
}

internal object ApiFactory{
 private val client=OkHttpClient.Builder().addInterceptor{chain->
  val b=chain.request().newBuilder()
  if(BuildConfig.DEVICE_ENROLLMENT_TOKEN.isNotBlank())b.header("Authorization","Bearer ${BuildConfig.DEVICE_ENROLLMENT_TOKEN}")
  chain.proceed(b.build())
 }.addInterceptor(HttpLoggingInterceptor().apply{level=HttpLoggingInterceptor.Level.BASIC}).build()
 val api:ScannerApi=Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).client(client).addConverterFactory(GsonConverterFactory.create()).build().create(ScannerApi::class.java)
}
