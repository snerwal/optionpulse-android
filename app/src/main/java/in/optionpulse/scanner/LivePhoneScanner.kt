package com.optionpulse.scanner

import android.content.Context
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.delay
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.InputStreamReader
import java.time.ZoneId
import java.util.zip.GZIPInputStream
import kotlin.math.abs

data class Instrument(val key:String,val symbol:String,val type:String,val strike:Double=0.0,val expiry:Long=0,val underlying:String="")
data class Tick(val price:Double,val volume:Long,val bid:Double=0.0,val ask:Double=0.0,val oi:Long=0)
data class Bar(val start:Long,var open:Double,var high:Double,var low:Double,var close:Double,var volume:Long)
data class Candidate(val instrument:Instrument,val direction:Direction,val bar:Bar,val pivot:Double,val plan:GannTradePlan,val score:Int,val ratio:Double)

class LivePhoneScanner(private val context:Context, private val onStatus:(String)->Unit) {
 private val http=OkHttpClient.Builder().retryOnConnectionFailure(true).build()
 private val store=CredentialStore(context)
 private val bars=mutableMapOf<String,ArrayDeque<Bar>>()
 private val lastVolume=mutableMapOf<String,Long>()
 private val cooldown=mutableMapOf<String,Long>()
 private var alerts=0
 private lateinit var equities:List<Instrument>
 private lateinit var options:List<Instrument>

 suspend fun run() {
  require(store.configured()){"Credentials are incomplete"}
  onStatus("Downloading current NSE instrument master")
  val all=downloadMaster()
  options=all.filter{(it.type=="CE"||it.type=="PE")&&it.underlying.isNotBlank()}
  val keys=options.map{it.underlying}.toSet()
  equities=all.filter{it.type=="EQ"&&it.key in keys}.distinctBy{it.key}.take(210)
  require(equities.isNotEmpty()){"No NSE F&O equities found"}
  while(true){
   if(inWindow()){
    equities.chunked(60).forEach{batch->
     val q=quotes(batch.map{it.key})
     batch.forEach{instrument->q[instrument.key]?.let{tick->update(instrument,tick)?.let(::evaluate)}}
     delay(400)
    }
    onStatus("LIVE • scanned ${equities.size}/${equities.size} • alerts $alerts/10")
   } else onStatus("Ready • active 09:30–15:00 IST")
   delay(20_000)
  }
 }

 private fun downloadMaster():List<Instrument>{
  val request=Request.Builder().url("https://assets.upstox.com/market-quote/instruments/exchange/complete.json.gz").build()
  http.newCall(request).execute().use{response->
   check(response.isSuccessful){"Instrument master HTTP ${response.code}"}
   val reader=JsonReader(InputStreamReader(GZIPInputStream(requireNotNull(response.body).byteStream())))
   val out=ArrayList<Instrument>()
   reader.beginArray()
   while(reader.hasNext()){
    var key="";var symbol="";var type="";var segment="";var strike=0.0;var expiry=0L;var underlying=""
    reader.beginObject()
    while(reader.hasNext())when(reader.nextName()){
     "instrument_key"->key=reader.nextString()
     "trading_symbol"->symbol=reader.nextString()
     "instrument_type"->type=reader.nextString()
     "segment"->segment=reader.nextString()
     "strike_price"->strike=readDouble(reader)
     "expiry"->expiry=readLong(reader)
     "underlying_key"->underlying=readString(reader)
     else->reader.skipValue()
    }
    reader.endObject()
    if(segment=="NSE_EQ"||segment=="NSE_FO")out+=Instrument(key,symbol,type,strike,expiry,underlying)
   }
   reader.endArray();return out
  }
 }
 private fun readString(r:JsonReader)=runCatching{r.nextString()}.getOrElse{r.skipValue();""}
 private fun readDouble(r:JsonReader)=runCatching{r.nextDouble()}.getOrElse{r.skipValue();0.0}\n private fun readLong(r:JsonReader)=runCatching{r.nextLong()}.getOrElse{r.skipValue();0L}

 private fun quotes(keys:List<String>):Map<String,Tick>{
  val encoded=java.net.URLEncoder.encode(keys.joinToString(","),"UTF-8")
  val request=Request.Builder().url("https://api.upstox.com/v2/market-quote/quotes?instrument_key=$encoded")
   .header("Authorization","Bearer ${store.upstoxToken}").header("Accept","application/json").build()
  http.newCall(request).execute().use{response->
   val text=requireNotNull(response.body).string()
   check(response.isSuccessful){"Upstox HTTP ${response.code}: ${text.take(120)}"}
   val data=JSONObject(text).getJSONObject("data")
   return buildMap{
    data.keys().forEach{alias->
     val q=data.getJSONObject(alias);val depth=q.optJSONObject("depth")
     val bid=depth?.optJSONArray("buy")?.optJSONObject(0)?.optDouble("price",0.0)?:0.0
     val ask=depth?.optJSONArray("sell")?.optJSONObject(0)?.optDouble("price",0.0)?:0.0
     val tick=Tick(q.optDouble("last_price"),q.optLong("volume"),bid,ask,q.optLong("oi"))
     put(q.optString("instrument_token",alias),tick);put(alias,tick)
    }
   }
  }
 }

 private fun update(i:Instrument,t:Tick):Pair<Instrument,Bar>?{
  if(t.price<=0)return null
  val now=System.currentTimeMillis();val bucket=now-now%(300_000)
  val deque=bars.getOrPut(i.key){ArrayDeque()}
  val delta=(t.volume-(lastVolume[i.key]?:t.volume)).coerceAtLeast(0);lastVolume[i.key]=t.volume
  val current=deque.lastOrNull()
  if(current==null||current.start!=bucket){
   val complete=current
   deque.addLast(Bar(bucket,t.price,t.price,t.price,t.price,delta))
   while(deque.size>25)deque.removeFirst()
   return complete?.let{i to it}
  }
  current.high=maxOf(current.high,t.price);current.low=minOf(current.low,t.price);current.close=t.price;current.volume+=delta
  return null
 }

 private fun evaluate(pair:Pair<Instrument,Bar>){
  val i=pair.first;val bar=pair.second;val history=bars[i.key]?.dropLast(1)?:return
  if(history.size<20||alerts>=10)return
  val recent=history.takeLast(20);val average=recent.dropLast(1).map{it.volume}.average()
  if(average<=0)return
  val ratio=bar.volume/average
  val volume=recent.sumOf{it.volume}.toDouble();if(volume<=0)return
  val vwap=recent.sumOf{((it.high+it.low+it.close)/3)*it.volume}/volume
  val pivots=history.takeLast(3);val low=pivots.minOf{it.low};val high=pivots.maxOf{it.high}
  val call=GannSquareOfNine.levels(low).callPlan();val put=GannSquareOfNine.levels(high).putPlan()
  val bullish=bar.close>call.trigger&&bar.close>vwap&&bar.close>bar.open&&ratio>=2.5
  val bearish=bar.close<put.trigger&&bar.close<vwap&&bar.close<bar.open&&ratio>=2.5
  val direction=when{bullish->Direction.CALL;bearish->Direction.PUT;else->return}
  val key="${i.key}:$direction";val now=System.currentTimeMillis()
  if(now-(cooldown[key]?:0)<900_000)return
  dispatch(Candidate(i,direction,bar,if(bullish)low else high,if(bullish)call else put,85,ratio))
  cooldown[key]=now
 }

 private fun dispatch(c:Candidate){
  val option=choose(c)?:return
  val tick=runCatching{quotes(listOf(option.key))[option.key]}.getOrNull()?:return
  if(tick.price<=0||tick.oi<100_000||tick.volume<50_000||tick.bid<=0||tick.ask<=0)return
  val spread=(tick.ask-tick.bid)/tick.price*100;if(spread>1.0)return
  val side=if(c.direction==Direction.CALL)"BULLISH BREAKOUT" else "BEARISH BREAKDOWN"
  val text="""🚨 NAKED OPTION BUYING ALERT 🚨
Symbol: ${c.instrument.symbol}
Direction: $side
Spot: ₹${fmt(c.bar.close)}
Gann 90° Trigger: ₹${fmt(c.plan.trigger)}
Gann 180° Target: ₹${fmt(c.plan.target1)}
Contract: ${option.symbol}
Premium: ₹${fmt(tick.price)}
Spread: ${fmt(spread)}%
OI: ${tick.oi} | Volume: ${tick.volume}
Spot SL: ₹${fmt(c.plan.protectiveLevel)}
Premium SL: ₹${fmt(tick.price*.78)}
Volume Surge: ${fmt(c.ratio)}x
Score: ${c.score}/100
ALERT ONLY — verify live price and use a limit order."""
  val body=FormBody.Builder().add("chat_id",store.telegramChatId).add("text",text).build()
  val request=Request.Builder().url("https://api.telegram.org/bot${store.telegramToken}/sendMessage").post(body).build()
  http.newCall(request).execute().use{if(it.isSuccessful){alerts++;localAlert(c.instrument.symbol,side,option.symbol)}}
 }

 private fun choose(c:Candidate):Instrument?{
  val today=System.currentTimeMillis()
  val suffix=if(c.direction==Direction.CALL)"CE" else "PE"
  val found=options.filter{it.underlying==c.instrument.key&&it.expiry>=today&&it.type==suffix}
  val expiry=found.minOfOrNull{it.expiry}?:return null
  val step=StrikeEngine.step(c.bar.close);val atm=kotlin.math.round(c.bar.close/step)*step
  return found.filter{it.expiry==expiry}.minByOrNull{abs(it.strike-atm)}
 }
 private fun localAlert(symbol:String,side:String,contract:String){
  val manager=context.getSystemService(android.app.NotificationManager::class.java)
  manager.notify((System.currentTimeMillis()%Int.MAX_VALUE).toInt(),androidx.core.app.NotificationCompat.Builder(context,"scanner_alerts")
   .setSmallIcon(android.R.drawable.stat_notify_more).setContentTitle("$symbol • $side").setContentText(contract)
   .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build())
 }
 private fun inWindow():Boolean{val t=java.time.LocalTime.now(ZoneId.of("Asia/Kolkata"));return !t.isBefore(java.time.LocalTime.of(9,30))&&t.isBefore(java.time.LocalTime.of(15,0))}
 private fun fmt(v:Double)="%.2f".format(v)
}
