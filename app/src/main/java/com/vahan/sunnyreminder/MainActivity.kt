package com.vahan.sunnyreminder

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.vahan.sunnyreminder.ui.BeachScene
import com.vahan.sunnyreminder.ui.theme.SunnyReminderTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Schedule periodic sync
        val syncRequest = PeriodicWorkRequestBuilder<CalendarSyncWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "CalendarSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        // Immediate sync on startup
        val immediateRequest = OneTimeWorkRequestBuilder<CalendarSyncWorker>().build()
        WorkManager.getInstance(this).enqueue(immediateRequest)

        setContent {
            SunnyReminderTheme {
                MainNavigation()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    var showSettings by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val permissionManager = remember { PermissionManager(context) }
    val settingsManager = remember { SettingsManager(context) }
    
    var hasNotification by remember { mutableStateOf(permissionManager.hasNotificationPermission()) }
    var hasAlarm by remember { mutableStateOf(permissionManager.hasExactAlarmPermission()) }
    var hasCalendar by remember { mutableStateOf(permissionManager.hasCalendarPermission()) }
    var hasLocation by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    ) }
    var isBatteryOptimized by remember { mutableStateOf(!permissionManager.isBatteryOptimizationDisabled()) }
    var hasFullScreenIntent by remember { mutableStateOf(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            (context.getSystemService(NotificationManager::class.java)).canUseFullScreenIntent()
        } else true
    ) }
    var hasNotificationAccess by remember { mutableStateOf(
        Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.contains(context.packageName) == true
    ) }
    var hideMegaphone by remember { mutableStateOf(settingsManager.shouldHideMegaphone()) }

    // Auto-refresh permissions when returning to the app
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasNotification = permissionManager.hasNotificationPermission()
        hasAlarm = permissionManager.hasExactAlarmPermission()
        hasCalendar = permissionManager.hasCalendarPermission()
        hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        isBatteryOptimized = !permissionManager.isBatteryOptimizationDisabled()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            hasFullScreenIntent = (context.getSystemService(NotificationManager::class.java)).canUseFullScreenIntent()
        }
        hasNotificationAccess = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.contains(context.packageName) == true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sunny Reminder", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Aide")
                    }
                    if (!showSettings) {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Paramètres")
                        }
                    }
                },
                navigationIcon = {
                    if (showSettings) {
                        IconButton(onClick = { showSettings = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            AnimatedContent(
                targetState = showSettings,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "nav"
            ) { isSettings ->
                if (isSettings) {
                    DashboardScreen(
                        hasNotification = hasNotification,
                        hasAlarm = hasAlarm,
                        hasCalendar = hasCalendar,
                        hasLocation = hasLocation,
                        isBatteryOptimized = isBatteryOptimized,
                        hasFullScreen = hasFullScreenIntent,
                        hasNotificationAccess = hasNotificationAccess,
                        hideMegaphone = hideMegaphone,
                        onToggleMegaphone = {
                            settingsManager.setHideMegaphone(it)
                            hideMegaphone = it
                        }
                    )
                } else {
                    HomeScreen(
                        allGranted = hasNotification && hasAlarm && hasFullScreenIntent,
                        hideMegaphone = hideMegaphone,
                        onOpenSettings = { showSettings = true },
                        onHideMegaphone = {
                            settingsManager.setHideMegaphone(true)
                            hideMegaphone = true
                        }
                    )
                }
            }

            if (showHelp) {
                HelpDialog(onDismiss = { showHelp = false })
            }
        }
    }
}

@Composable
fun HomeScreen(
    allGranted: Boolean,
    hideMegaphone: Boolean,
    onOpenSettings: () -> Unit,
    onHideMegaphone: () -> Unit
) {
    val context = LocalContext.current
    val scheduler = remember { AlarmScheduler(context) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            BeachScene(
                events = listOf(
                    CalendarEvent("Pause Café", System.currentTimeMillis()),
                    CalendarEvent("Réunion", System.currentTimeMillis()),
                    CalendarEvent("Sport", System.currentTimeMillis())
                ),
                onSunClick = {},
                onTowelClick = {},
                onVendorClick = {}
            )
            
            if (!allGranted && !hideMegaphone) {
                // Megaphone Alert
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Campaign, 
                            contentDescription = null, 
                            tint = Color.Yellow,
                            modifier = Modifier.size(72.dp).clickable { onOpenSettings() }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "N'oubliez pas de fournir les autorisations nécessaires à l'application",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.clickable { onOpenSettings() }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "(Appuyez ici pour régler)",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { onOpenSettings() }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onHideMegaphone() }
                        ) {
                            Checkbox(
                                checked = false, // It will disappear once clicked anyway
                                onCheckedChange = { onHideMegaphone() },
                                colors = CheckboxDefaults.colors(
                                    uncheckedColor = Color.White,
                                    checkmarkColor = Color.Black
                                )
                            )
                            Text(
                                "Ne plus rappeler",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    "Mode Aperçu",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 12.sp
                )
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Prêt pour une pause ?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Testez le rappel en plein écran pour voir les animations en action.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val time = System.currentTimeMillis() + 5000
                        scheduler.schedule(time, "C'est l'heure de la détente !")
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Lancer le test (5s)", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    hasNotification: Boolean,
    hasAlarm: Boolean,
    hasCalendar: Boolean,
    hasLocation: Boolean,
    isBatteryOptimized: Boolean,
    hasFullScreen: Boolean,
    hasNotificationAccess: Boolean,
    hideMegaphone: Boolean,
    onToggleMegaphone: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val permissionManager = remember { PermissionManager(context) }
    val weatherManager = remember { WeatherManager(context) }
    val settingsManager = remember { SettingsManager(context) }
    
    var selectedSoundUri by remember { mutableStateOf(settingsManager.getNotificationSound()) }
    var selectedLedColor by remember { mutableIntStateOf(settingsManager.getLedColor()) }
    var snoozeMinutes by remember { mutableIntStateOf(settingsManager.getSnoozeDuration()) }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            uri?.let {
                settingsManager.saveNotificationSound(it.toString())
                selectedSoundUri = it.toString()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Configuration & Droits",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        StatusCard(allGranted = hasNotification && hasAlarm && hasCalendar && !isBatteryOptimized && hasFullScreen)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Autorisations de Base",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        PermissionItem(
            title = "Notifications",
            description = "Affichage de l'alerte plein écran",
            isGranted = hasNotification,
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }
            }
        )

        PermissionItem(
            title = "Alarmes exactes",
            description = "Déclenchement précis du rappel",
            isGranted = hasAlarm,
            onGrant = {
                permissionManager.requestExactAlarmPermission()
            }
        )

        PermissionItem(
            title = "Calendrier",
            description = "Synchronisation des événements",
            isGranted = hasCalendar,
            onGrant = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )

        PermissionItem(
            title = "Localisation (Météo)",
            description = "Pour adapter le paysage au ciel réel",
            isGranted = hasLocation,
            onGrant = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )

        PermissionItem(
            title = "Plein Écran (Android 14+)",
            description = "Indispensable pour afficher la plage",
            isGranted = hasFullScreen,
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, "package:${context.packageName}".toUri())
                    context.startActivity(intent)
                }
            }
        )

        PermissionItem(
            title = "Optimisation Batterie",
            description = "À désactiver pour garantir le réveil",
            isGranted = !isBatteryOptimized,
            onGrant = {
                permissionManager.requestBatteryOptimizationExemption()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Megaphone reset toggle
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Masquer l'alerte porte-voix", fontSize = 14.sp)
            Switch(
                checked = hideMegaphone,
                onCheckedChange = onToggleMegaphone
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Mode Expert (Anticipation)",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        PermissionItem(
            title = "Accès aux Notifications",
            description = "Pour détecter les alertes d'agenda",
            isGranted = hasNotificationAccess,
            onGrant = {
                val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Personnalisation Alerte",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Sonnerie", fontWeight = FontWeight.Bold)
                    Text(
                        text = try {
                            RingtoneManager.getRingtone(context, selectedSoundUri.toUri()).getTitle(context)
                        } catch (e: Exception) { "Par défaut" },
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Button(onClick = {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choisir un son")
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedSoundUri.toUri())
                    }
                    ringtoneLauncher.launch(intent)
                }) {
                    Text("Modifier", fontSize = 12.sp)
                }
            }
        }

        Surface(
            modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Couleur LED", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val ledColors = listOf(
                        Color.Red to android.graphics.Color.RED,
                        Color.Blue to android.graphics.Color.BLUE,
                        Color.Green to android.graphics.Color.GREEN,
                        Color.Yellow to android.graphics.Color.YELLOW,
                        Color.Magenta to android.graphics.Color.MAGENTA
                    )
                    ledColors.forEach { (composeColor, argb) ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(composeColor)
                                .clickable {
                                    settingsManager.saveLedColor(argb)
                                    selectedLedColor = argb
                                }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedLedColor == argb) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Délai de report (Glace)", fontWeight = FontWeight.Bold)
                    Text(text = "$snoozeMinutes minutes", fontSize = 12.sp, color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { 
                        if (snoozeMinutes > 1) {
                            snoozeMinutes--
                            settingsManager.saveSnoozeDuration(snoozeMinutes)
                        }
                    }) {
                        Icon(Icons.Default.Remove, contentDescription = "Moins")
                    }
                    IconButton(onClick = { 
                        if (snoozeMinutes < 60) {
                            snoozeMinutes++
                            settingsManager.saveSnoozeDuration(snoozeMinutes)
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Plus")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Simulation Météo (Test)",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val states = listOf(
                "☀️" to WeatherState.SUNNY,
                "☁️" to WeatherState.CLOUDY,
                "🌧️" to WeatherState.RAINY,
                "⛈️" to WeatherState.STORM
            )
            states.forEach { (icon, state) ->
                OutlinedButton(
                    onClick = { weatherManager.setWeather(state) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = if (WeatherManager.currentWeather == state) 
                        ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) 
                        else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text(icon, fontSize = 20.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                val syncRequest = OneTimeWorkRequestBuilder<CalendarSyncWorker>().build()
                WorkManager.getInstance(context).enqueue(syncRequest)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Synchroniser l'agenda maintenant", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "À propos",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Créé par : vahanbarsamian@free.fr", fontSize = 14.sp)
                Text("Version : ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}", fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Sunny Reminder - Votre plage privée", color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun StatusCard(allGranted: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (allGranted) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (allGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (allGranted) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = if (allGranted) "Configuration valide" else "Action requise",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (allGranted) Color(0xFF2E7D32) else Color(0xFFEF6C00)
                )
                Text(
                    text = if (allGranted) "L'application est prête." else "Des permissions sont manquantes.",
                    fontSize = 13.sp,
                    color = Color.Black.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Surface(
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = description, fontSize = 12.sp, color = Color.Gray)
            }
            if (isGranted) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
            } else {
                Button(
                    onClick = onGrant,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Autoriser", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Secrets de la Plage 🏖️", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                HelpItem("☀️ Soleil", "Un simple appui dessus permet de fermer le rappel (Bouton OK).")
                HelpItem("🏖️ Serviette", "Un appui dessus ouvre votre agenda et réveille le petit crabe 🦀.")
                HelpItem("🏰 Sable", "Faites un appui long n'importe où sur le sable pour construire un château.")
                HelpItem("🍦 Glaces", "Cliquez sur le stand pour programmer un futur rappel gourmand.")
                HelpItem("🛩️ Avions", "Ils défilent en parade si vous avez plusieurs rendez-vous prévus.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("J'ai compris") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun HelpItem(title: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(description, fontSize = 14.sp)
    }
}
