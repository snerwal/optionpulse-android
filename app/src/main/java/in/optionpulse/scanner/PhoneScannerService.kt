package com.optionpulse.scanner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class PhoneScannerService : Service() {
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
 private var scannerJob:Job?=null
 override fun onCreate(){
  super.onCreate()
  val manager=getSystemService(NotificationManager::class.java)
  manager.createNotificationChannel(NotificationChannel(STATUS_CHANNEL,"Scanner status",NotificationManager.IMPORTANCE_LOW))
  manager.createNotificationChannel(NotificationChannel(ALERT_CHANNEL,"Trading alerts",NotificationManager.IMPORTANCE_HIGH))
 }
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
  if(intent?.action==ACTION_STOP){scannerJob?.cancel();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY}
  if(!CredentialStore(this).configured()){show("Setup required • open OptionPulse");return START_NOT_STICKY}
  show("Starting phone scanner…")
  if(scannerJob?.isActive!=true)scannerJob=scope.launch{
   while(isActive){
    try{LivePhoneScanner(this@PhoneScannerService,::show).run()}
    catch(e:CancellationException){throw e}
    catch(e:Exception){show("Scanner error: ${e.message?.take(80)?:"unknown"}");delay(30_000)}
   }
  }
  return START_STICKY
 }
 private fun show(text:String){
  getSharedPreferences("scanner_state",MODE_PRIVATE).edit().putBoolean("running",true).putString("status",text).apply()
  startForeground(ID,NotificationCompat.Builder(this,STATUS_CHANNEL).setSmallIcon(android.R.drawable.stat_notify_sync)
   .setContentTitle("OptionPulse scanner").setContentText(text).setOngoing(true).setOnlyAlertOnce(true).build())
 }
 override fun onDestroy(){scope.cancel();getSharedPreferences("scanner_state",MODE_PRIVATE).edit().putBoolean("running",false).apply();super.onDestroy()}
 override fun onBind(intent:Intent?):IBinder?=null
 companion object{
  const val ACTION_STOP="com.optionpulse.scanner.STOP"
  const val ALERT_CHANNEL="scanner_alerts"
  private const val STATUS_CHANNEL="scanner_status"
  private const val ID=9101
 }
}
