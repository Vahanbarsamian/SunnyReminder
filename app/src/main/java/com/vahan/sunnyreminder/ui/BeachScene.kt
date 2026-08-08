package com.vahan.sunnyreminder.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vahan.sunnyreminder.CalendarEvent
import com.vahan.sunnyreminder.SettingsManager
import com.vahan.sunnyreminder.WeatherManager
import com.vahan.sunnyreminder.WeatherState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.sin

@OptIn(ExperimentalTextApi::class)
@Composable
fun BeachScene(
    events: List<CalendarEvent>,
    onSunClick: () -> Unit,
    onTowelClick: () -> Unit,
    onVendorClick: () -> Unit
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { 
                currentTime = System.currentTimeMillis()
            }
        }
    }

    val density = LocalDensity.current
    val pBase = with(density) { sin(currentTime / 8000.0 * Math.PI).toFloat() * 10.dp.toPx() }

    val scope = rememberCoroutineScope()
    var crabVisible by remember { mutableStateOf(false) }
    val crabXOffset = remember { Animatable(0f) }
    var castlePos by remember { mutableStateOf<Offset?>(null) }
    var showInfoPanel by remember { mutableStateOf(false) }
    val textMeasurer = rememberTextMeasurer()

    val weather = WeatherManager.currentWeather
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val ledColor = Color(settingsManager.getLedColor())

    Canvas(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTapGestures(
                onLongPress = { offset ->
                    if (offset.y > size.height * 0.65f) {
                        castlePos = offset
                    }
                },
                onTap = { offset ->
                    // Check for Info Icon (Top Left)
                    val infoCenter = Offset(35.dp.toPx(), 35.dp.toPx())
                    if ((offset - infoCenter).getDistance() < 25.dp.toPx()) {
                        showInfoPanel = !showInfoPanel
                    }

                    val sunCenter = Offset(size.width * 0.8f, size.height * 0.15f)
                    val sunRadius = 60.dp.toPx()
                    if ((offset - sunCenter).getDistance() < sunRadius) {
                        onSunClick()
                    }

                    val vendorOffset = Offset(size.width * 0.12f, size.height * 0.76f)
                    val vendorSize = Size(70.dp.toPx(), 80.dp.toPx())
                    if (offset.x in (vendorOffset.x + pBase)..(vendorOffset.x + vendorSize.width + pBase) &&
                        offset.y in vendorOffset.y..(vendorOffset.y + vendorSize.height)) {
                        onVendorClick()
                    }

                    val towelOffset = Offset(size.width * 0.4f, size.height * 0.8f)
                    val towelSize = Size(80.dp.toPx(), 120.dp.toPx())
                    if (offset.x in (towelOffset.x + pBase)..(towelOffset.x + towelSize.width + pBase) &&
                        offset.y in (towelOffset.y)..(towelOffset.y + towelSize.height)) {
                        
                        if (!crabVisible) {
                            crabVisible = true
                            scope.launch {
                                crabXOffset.snapTo(0f)
                                crabXOffset.animateTo(40.dp.toPx(), tween(800))
                                delay(400)
                                crabXOffset.animateTo(-10.dp.toPx(), tween(800))
                                crabVisible = false
                            }
                        }
                        onTowelClick()
                    }
                }
            )
        }
    ) {
        // --- PARALLAX DRIFT ---
        val parallaxBase = pBase
        
        // --- ATMOSPHERIC COLORS ---
        val skyColors = when(weather) {
            WeatherState.SUNNY -> listOf(Color(0xFF87CEEB), Color(0xFFE0F7FA))
            WeatherState.CLOUDY -> listOf(Color(0xFFB0BEC5), Color(0xFFECEFF1))
            WeatherState.RAINY -> listOf(Color(0xFF78909C), Color(0xFFCFD8DC))
            WeatherState.STORM -> listOf(Color(0xFF37474F), Color(0xFF90A4AE))
        }
        
        val waterColor = when(weather) {
            WeatherState.SUNNY -> Color(0xFF0077BE)
            WeatherState.CLOUDY -> Color(0xFF546E7A)
            WeatherState.RAINY -> Color(0xFF455A64)
            WeatherState.STORM -> Color(0xFF263238)
        }

        val sandColor = when(weather) {
            WeatherState.SUNNY -> Color(0xFFF4A460)
            else -> Color(0xFFC2B280).copy(alpha = 0.8f)
        }

        drawRect(brush = Brush.verticalGradient(skyColors), size = size)
        
        // --- INFO ICON ---
        drawInfoIcon(35.dp.toPx(), 35.dp.toPx(), showInfoPanel)

        // --- LED AURA SIMULATION (Samsung fix) ---
        val auraPulse = 0.5f + 0.5f * sin(currentTime / 1000.0 * Math.PI).toFloat()
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(ledColor.copy(alpha = 0.15f * auraPulse), Color.Transparent),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.width
            ),
            size = size,
            blendMode = BlendMode.Screen
        )

        if (weather == WeatherState.SUNNY) {
            val sunPulse = 1f + 0.12f * abs(sin(currentTime / 1500.0 * Math.PI)).toFloat()
            drawSunPro(sunPulse, parallaxBase * 0.2f)
        }

        // --- LAYER 2: DISTANT CLOUDS ---
        drawClouds(currentTime, weather, parallaxBase * 0.4f)

        // --- LAYER 3: SEA BASE ---
        val waveOffset = 6.dp.toPx() * sin(currentTime / 2500.0 * Math.PI).toFloat()
        drawSeaBasePro(waterColor)

        // --- LAYER 4: MARITIME TRAFFIC (On top of sea base) ---
        drawMaritimeTrafficPro(currentTime, parallaxBase * 0.6f)
        
        drawSeagullsPro(currentTime, weather, parallaxBase * 0.7f)

        // --- LAYER 5: SEA FOAM & WAVES ---
        drawSeaWavesPro(waveOffset, currentTime)

        // --- LAYER 6: BEACH ---
        drawBeachPro(sandColor)
        
        translate(left = parallaxBase) {
            castlePos?.let { drawSandCastle(it.x, it.y) }
            
            val towelX = size.width * 0.4f
            val towelY = size.height * 0.82f
            val palmX = size.width * 0.75f
            val palmY = size.height * 0.88f
            
            drawPalmShadowPro(towelX, towelY, weather)
            drawBeachShelterPro(towelX - 110.dp.toPx(), towelY + 5.dp.toPx())
            drawTowelPro(towelX, towelY)
            drawBeachBagPro(towelX + 10.dp.toPx(), towelY + 15.dp.toPx())
            drawFlipFlopsPro(towelX + 50.dp.toPx(), towelY + 65.dp.toPx())
            drawLifebuoyPro(towelX + 100.dp.toPx(), towelY + 45.dp.toPx())
            drawBeachBallPro(towelX - 65.dp.toPx(), towelY + 85.dp.toPx() + (waveOffset * 0.2f))
            
            drawVendorPro(size.width * 0.12f, size.height * 0.76f)
            drawParasolPro(size.width * 0.08f, size.height * 0.82f, Color.Red, currentTime)
            drawParasolPro(size.width * 0.88f, size.height * 0.84f, Color.Blue, currentTime)

            drawPalmTreePro(palmX, palmY, currentTime, weather)
            
            if (crabVisible) {
                drawCrabPro(size.width * 0.45f + crabXOffset.value, size.height * 0.87f)
            }
        }

        // --- LAYER 6: WEATHER OVERLAY (RAIN) ---
        if (weather == WeatherState.RAINY || weather == WeatherState.STORM) {
            drawRain(currentTime, weather == WeatherState.STORM)
        }

        // --- LAYER 7: PLANES ---
        val planeCycle = 12000L
        val colors = listOf(Color.Red, Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFB8C00), Color(0xFF8E24AA))
        events.forEachIndexed { index, event ->
            val staggeredTime = currentTime - (index * 1800L)
            val rawProgress = (staggeredTime % planeCycle) / planeCycle.toFloat()
            val progress = -1.2f + (rawProgress * 3.4f)
            val altitude = (size.height * 0.25f) - (index * 45.dp.toPx())
            val color = colors[index % colors.size]
            drawPlanePro(progress, altitude, event.title, color, textMeasurer)
        }

        if (showInfoPanel) {
            drawInfoPanelPro(currentTime, textMeasurer)
        }
    }
}

private fun DrawScope.drawInfoIcon(x: Float, y: Float, active: Boolean) {
    val radius = 18.dp.toPx()
    drawCircle(
        color = if (active) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.4f),
        radius = radius,
        center = Offset(x, y)
    )
    // Draw 'i'
    val strokeWidth = 2.dp.toPx()
    drawLine(Color.DarkGray, Offset(x, y - 8.dp.toPx()), Offset(x, y + 8.dp.toPx()), strokeWidth)
    drawCircle(Color.DarkGray, radius = 2.dp.toPx(), center = Offset(x, y - 12.dp.toPx()))
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawInfoPanelPro(time: Long, textMeasurer: TextMeasurer) {
    val df = SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH)
    val tf = SimpleDateFormat("HH:mm", Locale.FRENCH)
    val cal = Calendar.getInstance(Locale.FRENCH).apply { timeInMillis = time }
    val weekNum = cal.get(Calendar.WEEK_OF_YEAR)
    
    val dateStr = df.format(Date(time))
    val timeStr = tf.format(Date(time))
    val extraInfoStr = "Semaine: $weekNum  |  Temp: ${WeatherManager.temperature}°C  |  Vent: ${WeatherManager.windSpeed} km/h"

    val w = 260.dp.toPx()
    val h = 100.dp.toPx()
    val x = 20.dp.toPx()
    val y = 60.dp.toPx()

    // Glass effect
    drawRoundRect(
        color = Color.White.copy(alpha = 0.25f),
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(16.dp.toPx())
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.4f),
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(16.dp.toPx()),
        style = Stroke(1.dp.toPx())
    )

    fun dText(txt: String, ty: Float, size: Float, bold: Boolean = false) {
        val style = TextStyle(fontSize = size.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = Color.DarkGray)
        drawText(textMeasurer, txt, topLeft = Offset(x + 16.dp.toPx(), y + ty), style = style)
    }

    dText(timeStr, 12.dp.toPx(), 24f, true)
    dText(dateStr, 42.dp.toPx(), 14f)
    dText(extraInfoStr, 68.dp.toPx(), 12f)
}

private fun DrawScope.drawSunPro(pulse: Float, parallaxX: Float) {
    val center = Offset(size.width * 0.8f + parallaxX, size.height * 0.15f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFD700), Color(0xFFFFE44D).copy(alpha = 0.2f), Color.Transparent),
            center = center,
            radius = 120.dp.toPx() * pulse
        ),
        radius = 120.dp.toPx() * pulse,
        center = center
    )
    drawCircle(Color(0xFFFFD700), radius = 45.dp.toPx(), center = center)
}

private fun DrawScope.drawClouds(time: Long, weather: WeatherState, parallaxX: Float) {
    val cloudColor = when(weather) {
        WeatherState.SUNNY -> Color.White.copy(alpha = 0.4f)
        else -> Color.LightGray.copy(alpha = 0.6f)
    }
    fun drawOneCloud(x: Float, y: Float, scale: Float) {
        val drift = (time / 50.0 % 2000).toFloat() - 500f
        val px = x + drift * scale + parallaxX
        drawCircle(cloudColor, radius = 30.dp.toPx() * scale, center = Offset(px, y))
        drawCircle(cloudColor, radius = 40.dp.toPx() * scale, center = Offset(px + 25.dp.toPx() * scale, y + 10.dp.toPx() * scale))
        drawCircle(cloudColor, radius = 25.dp.toPx() * scale, center = Offset(px - 20.dp.toPx() * scale, y + 5.dp.toPx() * scale))
    }
    drawOneCloud(size.width * 0.2f, size.height * 0.25f, 1.2f)
    drawOneCloud(size.width * 0.6f, size.height * 0.15f, 0.8f)
}

private fun DrawScope.drawSeaBasePro(baseColor: Color) {
    drawRect(baseColor, topLeft = Offset(0f, size.height * 0.5f), size = Size(size.width, size.height * 0.15f))
}

private fun DrawScope.drawSeaWavesPro(waveOff: Float, time: Long) {
    // Sea Shimmer (Sparkles)
    for (i in 0..15) {
        val x = (abs(sin(i.toDouble() + time / 1000.0)) * size.width).toFloat()
        val y = size.height * 0.52f + (abs(sin(i.toDouble() * 2)) * size.height * 0.08f).toFloat()
        val alpha = (abs(sin(time / 500.0 + i)) * 0.4f).toFloat()
        drawCircle(Color.White.copy(alpha = alpha), radius = 2.dp.toPx(), center = Offset(x, y))
    }
    // Dynamic wave line
    val wavePath = Path().apply {
        moveTo(0f, size.height * 0.6f + waveOff)
        quadraticTo(size.width * 0.25f, size.height * 0.62f + waveOff, size.width * 0.5f, size.height * 0.6f + waveOff)
        quadraticTo(size.width * 0.75f, size.height * 0.58f + waveOff, size.width, size.height * 0.6f + waveOff)
        lineTo(size.width, size.height * 0.65f); lineTo(0f, size.height * 0.65f); close()
    }
    drawPath(wavePath, Color.White.copy(alpha = 0.3f))
}

private fun DrawScope.drawBeachPro(color: Color) {
    drawRect(color, topLeft = Offset(0f, size.height * 0.6f), size = Size(size.width, size.height * 0.4f))
}

private fun DrawScope.drawPalmTreePro(x: Float, y: Float, time: Long, weather: WeatherState) {
    val trunkColor = Color(0xFF5D4037)
    val leafColor1 = Color(0xFF2E7D32); val leafColor2 = Color(0xFF4CAF50)
    val windIntensity = when(weather) {
        WeatherState.STORM -> 15f
        WeatherState.RAINY -> 8f
        else -> 3f
    }
    val trunkPath = Path().apply {
        moveTo(x - 6.dp.toPx(), y)
        quadraticTo(x - 25.dp.toPx(), y - 100.dp.toPx(), x - 45.dp.toPx(), y - 190.dp.toPx())
        lineTo(x - 30.dp.toPx(), y - 190.dp.toPx())
        quadraticTo(x - 10.dp.toPx(), y - 100.dp.toPx(), x + 6.dp.toPx(), y)
        close()
    }
    drawPath(trunkPath, trunkColor)
    for (i in 1..8) {
        val ly = y - (i * 20.dp.toPx())
        drawLine(Color.Black.copy(alpha = 0.1f), Offset(x - 20.dp.toPx(), ly), Offset(x, ly), strokeWidth = 1.dp.toPx())
    }
    val topX = x - 37.dp.toPx(); val topY = y - 190.dp.toPx()
    val leafSway = sin(time / (1500.0 / (windIntensity/3.0)) * Math.PI).toFloat() * windIntensity
    fun drawLeaf(angle: Float) {
        rotate(angle + leafSway, pivot = Offset(topX, topY)) {
            val leafP = Path().apply {
                moveTo(topX, topY)
                quadraticTo(topX + 50.dp.toPx(), topY - 15.dp.toPx(), topX + 90.dp.toPx(), topY + 25.dp.toPx())
                quadraticTo(topX + 50.dp.toPx(), topY + 15.dp.toPx(), topX, topY); close()
            }
            drawPath(leafP, Brush.linearGradient(listOf(leafColor1, leafColor2)))
        }
    }
    for (a in listOf(0f, 60f, 120f, 180f, 240f, 300f)) drawLeaf(a)
}

private fun DrawScope.drawSeagullsPro(time: Long, weather: WeatherState, parallaxX: Float) {
    val wingAnim = 5.dp.toPx() * sin(time / 400.0 * Math.PI).toFloat()
    val speed = if (weather == WeatherState.STORM) 2.5f else 1.5f
    fun drawG(xOff: Float, y: Float, scale: Float) {
        val drift = ((time * speed / 10.0) % (size.width + 400.dp.toPx())).toFloat() - 200.dp.toPx()
        val px = (xOff + drift) % (size.width + 400.dp.toPx()) - 200.dp.toPx() + parallaxX
        val path = Path().apply {
            moveTo(px - 10.dp.toPx() * scale, y + wingAnim * scale)
            quadraticTo(px, y - 5.dp.toPx() * scale, px + 10.dp.toPx() * scale, y + wingAnim * scale)
        }
        drawPath(path, Color.White, style = Stroke(2.dp.toPx() * scale))
    }
    drawG(100f, size.height * 0.2f, 1.2f)
    drawG(450f, size.height * 0.25f, 0.7f)
    drawG(800f, size.height * 0.15f, 1.0f)
    drawG(1200f, size.height * 0.22f, 0.5f)
    drawG(1500f, size.height * 0.28f, 1.4f)
}

private fun DrawScope.drawBeachShelterPro(x: Float, y: Float) {
    val tentColor = Color(0xFF039BE5); val stripeColor = Color.White
    val w = 80.dp.toPx(); val h = 60.dp.toPx()
    val path = Path().apply { moveTo(x, y); quadraticTo(x + w/2, y - h, x + w, y); close() }
    drawPath(path, tentColor)
    for (i in 1..3) {
        val sx = x + (w / 4) * i
        drawLine(stripeColor, Offset(sx, y), Offset(x + w/2, y - h + 5.dp.toPx()), 2.dp.toPx())
    }
}

private fun DrawScope.drawParasolPro(x: Float, y: Float, color: Color, time: Long) {
    val sway = sin(time / 2000.0 * Math.PI).toFloat() * 5.dp.toPx()
    val tx = x + sway
    drawOval(Color.Black.copy(alpha = 0.1f), topLeft = Offset(x - 20.dp.toPx(), y - 5.dp.toPx()), size = Size(60.dp.toPx(), 15.dp.toPx()))
    drawLine(Color(0xFF4E342E), Offset(x, y), Offset(tx, y - 60.dp.toPx()), 3.dp.toPx())
    val p = Path().apply { moveTo(tx - 40.dp.toPx(), y - 60.dp.toPx()); quadraticTo(tx, y - 95.dp.toPx(), tx + 40.dp.toPx(), y - 60.dp.toPx()); close() }
    drawPath(p, color)
}

private fun DrawScope.drawVendorPro(x: Float, y: Float) {
    val base = Color(0xFFF06292); val counter = Color.White
    drawRect(base, topLeft = Offset(x, y), size = Size(70.dp.toPx(), 80.dp.toPx()))
    drawRect(counter, topLeft = Offset(x - 5.dp.toPx(), y + 30.dp.toPx()), size = Size(80.dp.toPx(), 8.dp.toPx()))
    val roof = Path().apply { moveTo(x - 10.dp.toPx(), y); lineTo(x + 80.dp.toPx(), y); lineTo(x + 35.dp.toPx(), y - 30.dp.toPx()); close() }
    drawPath(roof, Color.White)
    for(i in 0..2) {
        val rx = x + i * 25.dp.toPx()
        drawPath(Path().apply { moveTo(rx, y); lineTo(rx + 12.dp.toPx(), y); lineTo(x + 35.dp.toPx(), y - 30.dp.toPx()); close() }, base)
    }
}

private fun DrawScope.drawPalmShadowPro(tx: Float, ty: Float, weather: WeatherState) {
    if (weather != WeatherState.SUNNY) return
    drawOval(Color.Black.copy(alpha = 0.1f), topLeft = Offset(tx - 40.dp.toPx(), ty + 10.dp.toPx()), size = Size(140.dp.toPx(), 60.dp.toPx()))
}

private fun DrawScope.drawRain(time: Long, isStorm: Boolean) {
    val rainColor = if (isStorm) Color(0xFFAABBCF).copy(alpha = 0.6f) else Color.LightGray.copy(alpha = 0.4f)
    for (i in 0..40) {
        val x = ((i * 12345 + time / 2) % size.width.toInt()).toFloat()
        val y = ((i * 6789 + time * 1.5) % size.height.toInt()).toFloat()
        drawLine(rainColor, Offset(x, y), Offset(x - 5.dp.toPx(), y + 15.dp.toPx()), strokeWidth = 1.dp.toPx())
    }
    if (isStorm && time % 3000 < 100) drawRect(Color.White.copy(alpha = 0.2f), size = size)
}

private fun DrawScope.drawTowelPro(x: Float, y: Float) {
    val w = 85.dp.toPx(); val h = 110.dp.toPx()
    val pink = Color(0xFFFF80AB); val cream = Color(0xFFFFF9C4)
    val path = Path().apply { moveTo(x + 15.dp.toPx(), y); lineTo(x + w - 15.dp.toPx(), y); lineTo(x + w + 30.dp.toPx(), y + h); lineTo(x - 30.dp.toPx(), y + h); close() }
    drawPath(path, cream)
    for(i in 0..2) {
        val sx = x + w * (0.2f + i * 0.3f)
        drawPath(Path().apply { moveTo(sx, y); lineTo(sx + 10.dp.toPx(), y); lineTo(sx + 20.dp.toPx(), y + h); lineTo(sx + 5.dp.toPx(), y + h); close() }, pink)
    }
}

private fun DrawScope.drawBeachBagPro(x: Float, y: Float) {
    drawRoundRect(Color(0xFF4DB6AC), topLeft = Offset(x, y + 5.dp.toPx()), size = Size(35.dp.toPx(), 25.dp.toPx()), cornerRadius = CornerRadius(4.dp.toPx()))
    drawCircle(Color(0xFF795548), radius = 8.dp.toPx(), center = Offset(x + 10.dp.toPx(), y), style = Stroke(1.5.dp.toPx()))
    drawCircle(Color(0xFF795548), radius = 8.dp.toPx(), center = Offset(x + 25.dp.toPx(), y), style = Stroke(1.5.dp.toPx()))
}

private fun DrawScope.drawFlipFlopsPro(x: Float, y: Float) {
    fun d(ox: Float) {
        drawOval(Color.Cyan, topLeft = Offset(x + ox, y), size = Size(10.dp.toPx(), 20.dp.toPx()))
        drawLine(Color.White, Offset(x + ox + 5.dp.toPx(), y + 4.dp.toPx()), Offset(x + ox + 1.dp.toPx(), y + 10.dp.toPx()), 1.5.dp.toPx())
        drawLine(Color.White, Offset(x + ox + 5.dp.toPx(), y + 4.dp.toPx()), Offset(x + ox + 9.dp.toPx(), y + 10.dp.toPx()), 1.5.dp.toPx())
    }
    d(0f); d(15.dp.toPx())
}

private fun DrawScope.drawLifebuoyPro(x: Float, y: Float) {
    drawCircle(Color(0xFFFF5722), radius = 15.dp.toPx(), center = Offset(x, y))
    rotate(45f, Offset(x,y)) { drawRect(Color.White, topLeft = Offset(x-4.dp.toPx(), y-15.dp.toPx()), size = Size(8.dp.toPx(), 4.dp.toPx())) }
    rotate(225f, Offset(x,y)) { drawRect(Color.White, topLeft = Offset(x-4.dp.toPx(), y-15.dp.toPx()), size = Size(8.dp.toPx(), 4.dp.toPx())) }
}

private fun DrawScope.drawBeachBallPro(x: Float, y: Float) {
    drawCircle(Color(0xFFFFEB3B), radius = 18.dp.toPx(), center = Offset(x, y))
    drawCircle(Color.White.copy(alpha = 0.3f), radius = 6.dp.toPx(), center = Offset(x - 5.dp.toPx(), y - 5.dp.toPx()))
}

private fun DrawScope.drawCrabPro(x: Float, y: Float) {
    drawOval(Color(0xFFFF5722), topLeft = Offset(x, y), size = Size(20.dp.toPx(), 12.dp.toPx()))
    drawCircle(Color.Black, radius = 1.5.dp.toPx(), center = Offset(x + 6.dp.toPx(), y + 2.dp.toPx()))
    drawCircle(Color.Black, radius = 1.5.dp.toPx(), center = Offset(x + 14.dp.toPx(), y + 2.dp.toPx()))
}

private fun DrawScope.drawSandCastle(x: Float, y: Float) {
    val c = Color(0xFFC2B280)
    drawRect(c, topLeft = Offset(x - 12.dp.toPx(), y - 18.dp.toPx()), size = Size(24.dp.toPx(), 18.dp.toPx()))
    drawRect(c, topLeft = Offset(x - 4.dp.toPx(), y - 30.dp.toPx()), size = Size(8.dp.toPx(), 12.dp.toPx()))
    val roof = Path().apply { moveTo(x - 6.dp.toPx(), y - 30.dp.toPx()); lineTo(x + 6.dp.toPx(), y - 30.dp.toPx()); lineTo(x, y - 40.dp.toPx()); close() }
    drawPath(roof, Color(0xFF8B4513))
}

private fun DrawScope.drawMaritimeTrafficPro(time: Long, parallaxX: Float) {
    // We simulate 5 boats with different cycles and positions
    fun drawTrafficBoat(seed: Int, cycle: Long, y: Float, scale: Float) {
        val trafficTime = (time + seed * 1000) % cycle
        val visibilityTime = cycle / 2
        if (trafficTime < visibilityTime) {
            val progress = trafficTime / visibilityTime.toFloat()
            val startX = -150.dp.toPx()
            val endX = size.width + 150.dp.toPx()
            // Some go left to right, some right to left
            val tx = if (seed % 2 == 0) startX + (endX - startX) * progress else endX - (endX - startX) * progress
            drawSailboat(tx + parallaxX, y, scale)
        }
    }

    // 5 Boats with varied depth and MUCH slower speeds
    drawTrafficBoat(1, 90000L, size.height * 0.51f, 0.6f) // Horizon, very slow
    drawTrafficBoat(2, 70000L, size.height * 0.52f, 0.8f) // Far, slow
    drawTrafficBoat(3, 55000L, size.height * 0.54f, 1.1f) // Medium, cruising
    drawTrafficBoat(4, 110000L, size.height * 0.515f, 0.7f) // Very far, static-like
    drawTrafficBoat(5, 80000L, size.height * 0.53f, 1.0f) // Distant
}

private fun DrawScope.drawSailboat(x: Float, y: Float, scale: Float) {
    val w = 35.dp.toPx() * scale; val h = 8.dp.toPx() * scale
    drawPath(Path().apply { moveTo(x, y); lineTo(x + w, y); lineTo(x + w * 0.85f, y + h); lineTo(x + w * 0.15f, y + h); close() }, Color.White)
    drawLine(Color.DarkGray, Offset(x + w * 0.5f, y), Offset(x + w * 0.5f, y - 22.dp.toPx() * scale), 1.5.dp.toPx() * scale)
    drawPath(Path().apply { moveTo(x + w * 0.52f, y - 20.dp.toPx() * scale); lineTo(x + w * 0.52f, y - 3.dp.toPx() * scale); lineTo(x + w * 0.85f, y - 3.dp.toPx() * scale); close() }, Color(0xFFF0F0F0))
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawPlanePro(progress: Float, y: Float, text: String, color: Color, textMeasurer: TextMeasurer) {
    val pw = 75.dp.toPx(); val ph = 22.dp.toPx()
    val x = size.width * progress
    translate(left = x, top = y) {
        drawPath(Path().apply { 
            moveTo(0f, ph * 0.3f); lineTo(pw * 0.2f, 0f); lineTo(pw * 0.7f, 0f)
            quadraticTo(pw * 0.95f, 0f, pw, ph * 0.5f); quadraticTo(pw * 0.95f, ph, pw * 0.7f, ph)
            lineTo(pw * 0.2f, ph); lineTo(0f, ph * 0.7f); close() 
        }, color)
        drawPath(Path().apply { moveTo(pw * 0.35f, 0f); quadraticTo(pw * 0.5f, -10.dp.toPx(), pw * 0.65f, 0f); close() }, Color(0xFFB3E5FC))
        drawPath(Path().apply { moveTo(pw*0.3f, ph*0.5f); lineTo(pw*0.4f, ph*0.2f); lineTo(pw*0.7f, ph*0.2f); lineTo(pw*0.6f, ph*0.5f); close() }, color)
        drawLine(Color.White.copy(alpha = 0.8f), Offset(pw * 0.42f, ph * 0.28f), Offset(pw * 0.68f, ph * 0.28f), 1.5.dp.toPx())
        drawPath(Path().apply { moveTo(0f, ph * 0.3f); lineTo(-10.dp.toPx(), -8.dp.toPx()); lineTo(0f, -8.dp.toPx()); close() }, color)
        drawPath(Path().apply { moveTo(pw*0.05f, ph*0.5f); lineTo(-8.dp.toPx(), ph*0.35f); lineTo(0f, ph*0.35f); lineTo(pw*0.15f, ph*0.5f); close() }, color)
        drawLine(Color.White.copy(alpha = 0.8f), Offset(-5.dp.toPx(), ph * 0.4f), Offset(0f, ph * 0.4f), 1.dp.toPx())
        drawCircle(Color(0xFF333333), radius = 4.dp.toPx(), center = Offset(pw*0.65f, ph+6.dp.toPx()))
        drawCircle(Color(0xFF333333), radius = 4.dp.toPx(), center = Offset(pw*0.75f, ph+6.dp.toPx()))
        drawCircle(Color(0xFF333333), radius = 2.5.dp.toPx(), center = Offset(pw*0.05f, ph*0.7f+4.dp.toPx()))
        val angle = (System.currentTimeMillis() % 250) / 250f * 360f
        rotate(angle, pivot = Offset(pw, ph * 0.5f)) {
            drawLine(Color.DarkGray, Offset(pw, ph*0.5f-18.dp.toPx()), Offset(pw, ph*0.5f+18.dp.toPx()), 3.dp.toPx())
        }
        val tl = 30.dp.toPx()
        drawLine(Color.Black, Offset(0f, ph*0.5f), Offset(-tl, ph*0.5f), 1.dp.toPx())
        val layout = textMeasurer.measure(AnnotatedString(text), TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray))
        val bw = layout.size.width + 24.dp.toPx(); val bh = layout.size.height + 24.dp.toPx()
        val bx = -tl - bw
        drawRect(Color.White.copy(alpha = 0.9f), topLeft = Offset(bx, ph*0.5f-bh/2), size = Size(bw, bh))
        drawRect(color.copy(alpha = 0.6f), topLeft = Offset(bx, ph*0.5f-bh/2), size = Size(bw, bh), style = Stroke(1.dp.toPx()))
        drawText(textMeasurer, text, topLeft = Offset(bx + 12.dp.toPx(), ph*0.5f - bh/2 + 12.dp.toPx()), style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray))
    }
}
