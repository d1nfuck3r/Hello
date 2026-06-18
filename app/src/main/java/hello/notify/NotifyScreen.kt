package hello.notify

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// APP COLOR PALETTE
// ─────────────────────────────────────────────────────────────────────────────
private val appPalettes = mapOf(
    "jp.naver.line.android"            to Pair(Color(0xFF00B900), Color(0xFF00E676)),
    "asuk.com.android.app"             to Pair(Color(0xFF00704A), Color(0xFF00BFA5)),
    "th.co.truemoney.wallet"           to Pair(Color(0xFFFF6D00), Color(0xFFFFAB40)),
    "com.linecorp.lineman.driver"      to Pair(Color(0xFF00B900), Color(0xFF69F0AE)),
    "com.kasikorn.retail.mbanking.wap" to Pair(Color(0xFF00897B), Color(0xFF4DB6AC)),
    "com.scb.phone"                    to Pair(Color(0xFF6A1B9A), Color(0xFFAB47BC)),
    "k.bank"                           to Pair(Color(0xFF1B5E20), Color(0xFF43A047))
)

private fun packageToGradient(pkg: String): Pair<Color, Color> {
    appPalettes[pkg]?.let { return it }
    val hue  = (pkg.hashCode().and(0x7FFFFFFF) % 360).toFloat()
    val hue2 = (hue + 35f) % 360f
    return Pair(
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue,  0.65f, 0.75f))),
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue2, 0.45f, 0.92f)))
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun NotifyScreen() {
    val t       = LocalTheme.current
    val context = LocalContext.current

    var rules      by remember { mutableStateOf(RuleStore.load(context)) }
    var logLines   by remember { mutableStateOf("Log ข้อมูลแจ้งเตือน...") }
    var nlsEnabled by remember { mutableStateOf(isNLSEnabled(context)) }
    var editRule   by remember { mutableStateOf<UserRule?>(null) }
    var showEdit   by remember { mutableStateOf(false) }
    var deleteRule by remember { mutableStateOf<UserRule?>(null) }
    var gridMode   by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        LogBus.onLog = { msg -> logLines = (logLines.lines().takeLast(40) + msg).joinToString("\n") }
        onDispose { LogBus.onLog = null }
    }

    LaunchedEffect(Unit) {
        RuleStore.seedIfEmpty(context)
        rules = RuleStore.load(context)
        val svc = Intent(context, TTSForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(svc)
        else context.startService(svc)
        while (true) { nlsEnabled = isNLSEnabled(context); delay(1000) }
    }

    Box(modifier = Modifier.fillMaxSize().background(t.bgPrimary)) {
        Spacer(modifier = Modifier.fillMaxSize().drawBehind {
            val w = size.width; val h = size.height
            drawCircle(Brush.radialGradient(listOf(t.accent.copy(.10f), Color.Transparent),
                center = Offset(w * .85f, h * .08f), radius = w * .65f))
            drawCircle(Brush.radialGradient(listOf(t.accentAlt.copy(.07f), Color.Transparent),
                center = Offset(w * .10f, h * .75f), radius = w * .55f))
        })

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hello Notify", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp, color = t.textPrimary)
                    Text("สวัสดี", fontSize = 13.sp, color = t.textSecondary)
                }
                IconButton(onClick = { gridMode = !gridMode },
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(t.bgSurface)) {
                    Icon(if (gridMode) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                        null, tint = t.accent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { TTSForegroundService.reinit(context) },
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(t.bgSurface)) {
                    Text("🔄", fontSize = 16.sp)
                }
                if (!nlsEnabled) {
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = t.accent),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) { Text("สิทธิ์", fontSize = 13.sp, color = Color.White) }
                }
            }

            // ── Status
            Surface(shape = RoundedCornerShape(20.dp), color = t.bgSurface.copy(.80f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape)
                        .background(if (nlsEnabled) Color(0xFF22C55E) else Color(0xFFEF4444)))
                    Spacer(Modifier.width(12.dp))
                    Text(if (nlsEnabled) "Notification Access เปิดอยู่" else "ยังไม่เปิด Notification Access",
                        fontSize = 13.sp,
                        color = if (nlsEnabled) Color(0xFF22C55E) else Color(0xFFEF4444))
                }
            }

            // ── Rules header
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("รายการแอพ", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = t.textPrimary, modifier = Modifier.weight(1f))
                Text("${rules.size} แอพ", fontSize = 11.sp, color = t.textSecondary)
            }

            // ── Rules
            if (gridMode) {
                LazyVerticalGrid(
                    columns               = GridCells.Fixed(2),
                    modifier              = Modifier.weight(1f),
                    contentPadding        = PaddingValues(start = 12.dp, end = 12.dp, bottom = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement   = Arrangement.spacedBy(10.dp)
                ) {
                    items(rules, key = { it.id }) { rule ->
                        GridRuleCard(rule = rule, theme = t,
                            onToggle = { RuleStore.toggle(context, rule.id); rules = RuleStore.load(context) },
                            onEdit   = { editRule = rule; showEdit = true },
                            onDelete = { deleteRule = rule })
                    }
                }
            } else {
                LazyColumn(
                    modifier            = Modifier.weight(1f),
                    contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(rules, key = { it.id }) { rule ->
                        ListRuleCard(rule = rule, theme = t,
                            onToggle = { RuleStore.toggle(context, rule.id); rules = RuleStore.load(context) },
                            onEdit   = { editRule = rule; showEdit = true },
                            onDelete = { deleteRule = rule })
                    }
                }
            }
            
            ExtendedFloatingActionButton(
                onClick = { editRule = null; showEdit = true },
                icon    = { Icon(Icons.Rounded.Add, null, tint = Color.White) },
                text    = { Text("LOG", color = Color.White, fontWeight = FontWeight.SemiBold) },
                containerColor = t.accent, shape = CircleShape,
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding()
                    .padding(start = 20.dp, bottom = 115.dp)
            )

            // ── Log
            val scrollLog = rememberScrollState()
            LaunchedEffect(logLines) { scrollLog.animateScrollTo(scrollLog.maxValue) }
            Surface(shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = Color(0xF0111827),
                modifier = Modifier.fillMaxWidth().height(100.dp)) {
                Box {
                    Text(logLines, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                        color = Color(0xFF6EE7B7), lineHeight = 15.sp,
                        modifier = Modifier.fillMaxSize().verticalScroll(scrollLog).padding(12.dp))
                    IconButton(onClick = { logLines = "Clear ล้างข้อมูลแจ้งเตือนแล้ว" },
                        modifier = Modifier.align(Alignment.TopEnd).size(28.dp)) {
                        Icon(Icons.Rounded.Clear, null, tint = Color(0xFF6EE7B7).copy(.5f),
                            modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { editRule = null; showEdit = true },
            icon    = { Icon(Icons.Rounded.Add, null, tint = Color.White) },
            text    = { Text("เพิ่ม", color = Color.White, fontWeight = FontWeight.SemiBold) },
            containerColor = t.accent, shape = CircleShape,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding()
                .padding(end = 20.dp, bottom = 115.dp)
        )
    }

    if (showEdit) {
        EditRuleSheet(existing = editRule, theme = t,
            onSave    = { rule -> RuleStore.upsert(context, rule); rules = RuleStore.load(context); showEdit = false },
            onDismiss = { showEdit = false })
    }

    deleteRule?.let { r ->
        AlertDialog(
            onDismissRequest = { deleteRule = null },
            title            = { Text(r.appName, color = t.textPrimary) },
            text             = { Text("ต้องการลบใช่มั้ย?", color = t.textSecondary) },
            confirmButton    = {
                TextButton(onClick = {
                    RuleStore.delete(context, r.id); rules = RuleStore.load(context); deleteRule = null
                }) { Text("ลบ", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton    = { TextButton(onClick = { deleteRule = null }) { Text("ยกเลิก", color = t.textSecondary) } },
            containerColor   = t.bgSurface
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LIST CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ListRuleCard(
    rule: UserRule, theme: AppThemeColors,
    onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    val t = theme
    val (c1, c2) = packageToGradient(rule.appPackage)
    val alpha = if (rule.enabled) 1f else 0.45f

    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .background(Brush.linearGradient(listOf(c1.copy(alpha), c2.copy(alpha * .85f)),
            Offset.Zero, Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)))
    ) {
        Box(Modifier.size(100.dp).align(Alignment.TopEnd).offset(x = 24.dp, y = (-24).dp)
            .background(Brush.radialGradient(listOf(Color.White.copy(.15f), Color.Transparent)), CircleShape))

        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(.22f)),
                    contentAlignment = Alignment.Center) {
                    Text(rule.appName.firstOrNull()?.uppercase() ?: "?", fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(rule.appName.ifEmpty { rule.appPackage }, fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${rule.triggers.size} trigger${if (rule.triggers.size != 1) "s" else ""}",
                        fontSize = 11.sp, color = Color.White.copy(.65f))
                }
                Switch(checked = rule.enabled, onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor   = c1, checkedTrackColor   = Color.White.copy(.9f),
                        uncheckedThumbColor = Color.White.copy(.6f), uncheckedTrackColor = Color.White.copy(.2f)
                    ))
            }

            Spacer(Modifier.height(12.dp))

            rule.triggers.take(3).forEach { trig ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 5.dp)) {
                    Surface(shape = CircleShape, color = Color.White.copy(.20f)) {
                        Text("แจ้งเตือนแอพ", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold,
                            color = Color.White, letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(trig.keyword, fontSize = 11.sp, color = Color.White.copy(.9f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }
            if (rule.triggers.size > 3)
                Text("+${rule.triggers.size - 3} อื่นๆ", fontSize = 10.sp, color = Color.White.copy(.6f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit,   modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Edit,   null, tint = Color.White.copy(.8f), modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Delete, null, tint = Color.White.copy(.55f), modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GRID CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GridRuleCard(
    rule: UserRule, theme: AppThemeColors,
    onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    val (c1, c2) = packageToGradient(rule.appPackage)
    val alpha = if (rule.enabled) 1f else 0.45f

    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp))
        .background(Brush.linearGradient(listOf(c1.copy(alpha), c2.copy(alpha * .80f)),
            Offset.Zero, Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)))
    ) {
        Box(Modifier.size(80.dp).align(Alignment.TopEnd).offset(x = 20.dp, y = (-20).dp)
            .background(Brush.radialGradient(listOf(Color.White.copy(.15f), Color.Transparent)), CircleShape))

        Column(modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(.22f)),
                    contentAlignment = Alignment.Center) {
                    Text(rule.appName.firstOrNull()?.uppercase() ?: "?", fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Switch(checked = rule.enabled, onCheckedChange = { onToggle() },
                    modifier = Modifier.scaleModifier(0.75f),
                    colors   = SwitchDefaults.colors(
                        checkedThumbColor   = c1, checkedTrackColor   = Color.White.copy(.9f),
                        uncheckedThumbColor = Color.White.copy(.6f), uncheckedTrackColor = Color.White.copy(.2f)
                    ))
            }
            Column {
                Text(rule.appName.ifEmpty { rule.appPackage }, fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${rule.triggers.size} trigger${if (rule.triggers.size != 1) "s" else ""}",
                    fontSize = 10.sp, color = Color.White.copy(.65f))
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onEdit,   modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Edit,   null, tint = Color.White.copy(.8f), modifier = Modifier.size(15.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Delete, null, tint = Color.White.copy(.55f), modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EDIT SHEET
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRuleSheet(
    existing: UserRule?, theme: AppThemeColors,
    onSave: (UserRule) -> Unit, onDismiss: () -> Unit
) {
    val t = theme
    var pkg      by remember { mutableStateOf(existing?.appPackage ?: "") }
    var name     by remember { mutableStateOf(existing?.appName    ?: "") }
    var triggers by remember { mutableStateOf(
        existing?.triggers?.toMutableList() ?: mutableListOf(RuleTrigger())
    )}
    var showAppPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = t.bgSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = t.border) }) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {

            Text(if (existing == null) "เพิ่ม" else "แก้ไข",
                fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = t.textPrimary)
            Text("กำหนดแอพและเงื่อนไขการพูด", fontSize = 13.sp, color = t.textSecondary)

            // App info
            Surface(shape = RoundedCornerShape(16.dp), color = t.bgSurfaceAlt, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ข้อมูลแอพ", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = t.accent, letterSpacing = 0.5.sp, modifier = Modifier.weight(1f))
                        Surface(shape = RoundedCornerShape(10.dp), color = t.accent.copy(.12f),
                            modifier = Modifier.clickable { showAppPicker = true }) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text("📱", fontSize = 13.sp)
                                Spacer(Modifier.width(4.dp))
                                Text("เลือกแอพ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = t.accent)
                            }
                        }
                    }
                    NotifyTextField("Package Name", pkg, t) { pkg = it }
                    NotifyTextField("ชื่อแอพ (ใช้แสดงใน list)", name, t) { name = it }
                }
            }

            // Triggers
            Surface(shape = RoundedCornerShape(16.dp), color = t.bgSurfaceAlt, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("แจ้งเตือนแอพ", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = t.accent, letterSpacing = 0.5.sp, modifier = Modifier.weight(1f))
                        Surface(shape = RoundedCornerShape(8.dp), color = t.accent.copy(.12f),
                            modifier = Modifier.clickable {
                                triggers = (triggers + RuleTrigger()).toMutableList()
                            }) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Icon(Icons.Rounded.Add, null, tint = t.accent, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("เพิ่ม", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = t.accent)
                            }
                        }
                    }

                    triggers.forEachIndexed { i, trig ->
                        Surface(shape = RoundedCornerShape(12.dp), color = t.bgSurface.copy(.6f),
                            modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(22.dp).clip(CircleShape).background(t.accent.copy(.15f)),
                                        contentAlignment = Alignment.Center) {
                                        Text("${i + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = t.accent)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    if (triggers.size > 1) {
                                        IconButton(onClick = {
                                            triggers = triggers.toMutableList().also { it.removeAt(i) }
                                        }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Rounded.Close, null, tint = t.textSecondary.copy(.6f),
                                                modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                                NotifyTextField("Keyword ที่ดัก", trig.keyword, t) { kw ->
                                    triggers = triggers.toMutableList().also { it[i] = it[i].copy(keyword = kw) }
                                }
                                NotifyTextField("ข้อความที่พูด", trig.template, t, singleLine = false) { tpl ->
                                    triggers = triggers.toMutableList().also { it[i] = it[i].copy(template = tpl) }
                                }
                            }
                        }
                    }
                }
            }

            // Hint
            Surface(shape = RoundedCornerShape(12.dp), color = t.accentContainer.copy(.3f),
                modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Placeholders", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = t.accent)
                    Spacer(Modifier.height(6.dp))
                    Text("{ข้อความ}  body\n{หัวข้อ}     title\n{ยอด}       ตัวเลข\n{แต้ม}      จำนวนเต็ม\n{ชื่อ}       ชื่อผู้โอน",
                        fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = t.accent, lineHeight = 18.sp)
                }
            }

            Button(onClick = {
                val valid = triggers.filter { it.keyword.isNotBlank() && it.template.isNotBlank() }
                if (pkg.isBlank() || valid.isEmpty()) return@Button
                onSave(UserRule(
                    id         = existing?.id ?: java.util.UUID.randomUUID().toString(),
                    appPackage = pkg.trim(),
                    appName    = name.trim().ifEmpty { pkg.trim() },
                    triggers   = valid,
                    enabled    = existing?.enabled ?: true
                ))
            }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = t.accent)) {
                Text("บันทึก", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showAppPicker) {
        AppPickerSheet(theme = t,
            onSelect  = { selPkg, selName -> pkg = selPkg; name = selName; showAppPicker = false },
            onDismiss = { showAppPicker = false })
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// APP PICKER
// ─────────────────────────────────────────────────────────────────────────────
data class InstalledApp(val label: String, val packageName: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(
    theme: AppThemeColors, onSelect: (String, String) -> Unit, onDismiss: () -> Unit
) {
    val t       = theme
    val context = LocalContext.current
    var query   by remember { mutableStateOf("") }

    val apps = remember {
        val pm    = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            android.content.pm.PackageManager.PackageInfoFlags.of(
                android.content.pm.PackageManager.GET_ACTIVITIES.toLong())
        else null
        val pkgs  = if (flags != null) pm.getInstalledPackages(flags)
            else @Suppress("DEPRECATION") pm.getInstalledPackages(android.content.pm.PackageManager.GET_ACTIVITIES)
        pkgs.filter { it.activities != null && it.activities!!.isNotEmpty() }
            .mapNotNull { p -> p.applicationInfo?.let { ai -> InstalledApp(pm.getApplicationLabel(ai).toString(), p.packageName) } }
            .sortedBy { it.label.lowercase() }
    }

    val filtered = remember(query) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = t.bgSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = t.border) }) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            Text("เลือกแอพ", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                color = t.textPrimary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            OutlinedTextField(value = query, onValueChange = { query = it },
                placeholder = { Text("ค้นหาแอพ...", fontSize = 13.sp) },
                singleLine = true, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = t.accent, unfocusedBorderColor = t.borderVariant, cursorColor = t.accent),
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                textStyle = TextStyle(fontSize = 13.sp, color = t.textPrimary))
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                items(filtered) { app ->
                    val (ic1, ic2) = packageToGradient(app.packageName)
                    Row(modifier = Modifier.fillMaxWidth()
                        .clickable { onSelect(app.packageName, app.label) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(listOf(ic1, ic2))),
                            contentAlignment = Alignment.Center) {
                            Text(app.label.firstOrNull()?.uppercase() ?: "?", fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                color = t.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(app.packageName, fontSize = 10.sp, color = t.textSecondary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    HorizontalDivider(color = t.borderVariant.copy(.4f), thickness = 0.5.dp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NotifyTextField(
    label: String, value: String, t: AppThemeColors,
    singleLine: Boolean = true, onValue: (String) -> Unit
) {
    OutlinedTextField(value = value, onValueChange = onValue, label = { Text(label, fontSize = 12.sp) },
        singleLine = singleLine, minLines = if (!singleLine) 2 else 1,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = t.accent, unfocusedBorderColor = t.borderVariant,
            focusedLabelColor    = t.accent, unfocusedLabelColor  = t.textSecondary, cursorColor = t.accent),
        modifier  = Modifier.fillMaxWidth(),
        textStyle = TextStyle(fontSize = 13.sp, color = t.textPrimary))
}

private fun isNLSEnabled(ctx: Context): Boolean {
    val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
    return flat.split(":").any { ComponentName.unflattenFromString(it)?.packageName == ctx.packageName }
}

private fun Modifier.scaleModifier(scale: Float) = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout((placeable.width * scale).toInt(), (placeable.height * scale).toInt()) {
        placeable.placeRelative(
            (-(placeable.width  * (1 - scale)) / 2).toInt(),
            (-(placeable.height * (1 - scale)) / 2).toInt()
        )
    }
}
