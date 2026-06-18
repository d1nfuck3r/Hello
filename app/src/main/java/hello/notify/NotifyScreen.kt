package hello.notify

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// NOTIFY MAIN SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun NotifyScreen() {
    val t       = LocalTheme.current
    val context = LocalContext.current

    var rules        by remember { mutableStateOf(RuleStore.load(context)) }
    var logLines     by remember { mutableStateOf("Log ข้อมูลแจ้งเตือน...") }
    var nlsEnabled   by remember { mutableStateOf(isNLSEnabled(context)) }
    var editRule     by remember { mutableStateOf<UserRule?>(null) }
    var showEdit     by remember { mutableStateOf(false) }
    var deleteRule   by remember { mutableStateOf<UserRule?>(null) }
    // (pkg, name) ใช้ตอนกด "+" ในการ์ดเดิม — ล็อกแอพไว้ ใส่แค่ keyword/template ใหม่
    var presetApp    by remember { mutableStateOf<Pair<String, String>?>(null) }
    var cardLayout   by remember { mutableStateOf(UiPrefs.getCardLayout(context)) }
    var detailGroup  by remember { mutableStateOf<AppGroup?>(null) }
    // appPackage -> index ของ trigger ที่กำลังเลือกดูอยู่ในการ์ดนั้น
    val selectedIndices = remember { mutableStateMapOf<String, Int>() }

    val groups = remember(rules) { rules.grouped() }

    fun refresh() { rules = RuleStore.load(context) }

    // Log bus
    DisposableEffect(Unit) {
        LogBus.onLog = { msg ->
            logLines = (logLines.lines().takeLast(40) + msg).joinToString("\n")
        }
        onDispose { LogBus.onLog = null }
    }

    // Start TTS service + poll NLS status every 1s so button hides without restart
    LaunchedEffect(Unit) {
        RuleStore.seedIfEmpty(context)
        rules = RuleStore.load(context)
        val svc = Intent(context, TTSForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(svc)
        else context.startService(svc)
        while (true) {
            nlsEnabled = isNLSEnabled(context)
            kotlinx.coroutines.delay(1000)
        }
    }

    // Background blobs (same style as vyxel)
    Box(modifier = Modifier.fillMaxSize().background(t.bgPrimary)) {
        NotifyBackground(t)

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ──────────────────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Hello Notify",
                        fontSize   = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color      = t.textPrimary
                    )
                    Text(
                        "สวัสดี",
                        fontSize = 13.sp,
                        color    = t.textSecondary
                    )
                }

                // Layout toggle — list (ยาว) <-> grid (คู่สี่เหลี่ยมจัตุรัส)
                IconButton(
                    onClick = {
                        cardLayout = if (cardLayout == CardLayout.LIST) CardLayout.GRID else CardLayout.LIST
                        UiPrefs.setCardLayout(context, cardLayout)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(t.bgSurface)
                ) {
                    Icon(
                        imageVector = if (cardLayout == CardLayout.LIST) Icons.Rounded.GridView else Icons.Rounded.ViewAgenda,
                        contentDescription = null,
                        tint = t.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Restart TTS button
                IconButton(
                    onClick = { TTSForegroundService.reinit(context) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(t.bgSurface)
                ) {
                    Text("🔄", fontSize = 18.sp)
                }

                Spacer(Modifier.width(8.dp))

                // Permission button - hide when already granted
                if (!nlsEnabled) {
                    Button(
                        onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                        shape   = CircleShape,
                        colors  = ButtonDefaults.buttonColors(containerColor = t.accent),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("สิทธิ์", fontSize = 13.sp, color = Color.White)
                    }
                }
            }

            // ── Status card ───────────────────────────────────────────────────
            nlsEnabled = isNLSEnabled(context)
            NotifyStatusCard(t, nlsEnabled)

            // ── Rules header ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "รายการแอพ",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = t.textPrimary,
                    modifier   = Modifier.weight(1f)
                )
                Text("กด + เพื่อเพิ่ม", fontSize = 11.sp, color = t.textSecondary)
            }

            // ── Rules area — 1 การ์ดต่อแอพ (รวมหลาย trigger) ───────────────────
            if (cardLayout == CardLayout.LIST) {
                LazyColumn(
                    modifier            = Modifier.weight(1f),
                    contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(groups, key = { it.appPackage }) { group ->
                        val selIdx = (selectedIndices[group.appPackage] ?: 0).coerceIn(0, group.rules.lastIndex)
                        AppGroupListCard(
                            group         = group,
                            theme         = t,
                            selectedIndex = selIdx,
                            onSelectIndex = { selectedIndices[group.appPackage] = it },
                            onToggle      = { rule -> RuleStore.toggle(context, rule.id); refresh() },
                            onEdit        = { rule -> editRule = rule; presetApp = null; showEdit = true },
                            onDelete      = { rule -> deleteRule = rule },
                            onAddTrigger  = {
                                editRule  = null
                                presetApp = group.appPackage to group.appName
                                showEdit  = true
                            }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns               = GridCells.Fixed(2),
                    modifier              = Modifier.weight(1f),
                    contentPadding        = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 92.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement   = Arrangement.spacedBy(10.dp)
                ) {
                    gridItems(groups, key = { it.appPackage }) { group ->
                        AppGroupSquareCard(group = group, theme = t, onTap = { detailGroup = group })
                    }
                }
            }

            // ── Log panel ─────────────────────────────────────────────────────
            NotifyLogPanel(t, logLines) { logLines = "Clear ล้างข้อมูลแจ้งเตือนแล้ว" }
        }

        // FAB
        ExtendedFloatingActionButton(
            onClick = { editRule = null; presetApp = null; showEdit = true },
            icon    = { Icon(Icons.Rounded.Add, null, tint = Color.White) },
            text    = { Text("เพิ่ม", color = Color.White, fontWeight = FontWeight.SemiBold) },
            containerColor = t.accent,
            shape   = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 100.dp)
        )
    }

    // Detail bottom sheet — เปิดจากการ์ดสี่เหลี่ยมจัตุรัสใน grid mode
    detailGroup?.let { group ->
        val liveGroup = groups.find { it.appPackage == group.appPackage } ?: group
        val selIdx    = (selectedIndices[liveGroup.appPackage] ?: 0).coerceIn(0, liveGroup.rules.lastIndex)
        AppDetailSheet(
            group         = liveGroup,
            theme         = t,
            selectedIndex = selIdx,
            onSelectIndex = { selectedIndices[liveGroup.appPackage] = it },
            onToggle      = { rule -> RuleStore.toggle(context, rule.id); refresh() },
            onEdit        = { rule -> editRule = rule; presetApp = null; showEdit = true; detailGroup = null },
            onDelete      = { rule -> deleteRule = rule; detailGroup = null },
            onAddTrigger  = {
                editRule  = null
                presetApp = liveGroup.appPackage to liveGroup.appName
                showEdit  = true
                detailGroup = null
            },
            onDismiss     = { detailGroup = null }
        )
    }

    // Edit/Add bottom sheet
    if (showEdit) {
        EditRuleSheet(
            existing   = editRule,
            lockedPkg  = presetApp?.first,
            lockedName = presetApp?.second,
            theme      = t,
            onSave     = { rule ->
                RuleStore.upsert(context, rule)
                refresh()
                showEdit  = false
                presetApp = null
            },
            onDismiss  = { showEdit = false; presetApp = null }
        )
    }

    // Delete confirm dialog
    deleteRule?.let { r ->
        AlertDialog(
            onDismissRequest = { deleteRule = null },
            title = { Text("ลบ ${r.appName}?", color = t.textPrimary) },
            text  = { Text("\"${r.keyword}\" — ${r.appName}", color = t.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    RuleStore.delete(context, r.id)
                    refresh()
                    deleteRule = null
                }) { Text("ลบ", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteRule = null }) { Text("ยกเลิก", color = t.textSecondary) }
            },
            containerColor = t.bgSurface
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BACKGROUND BLOBS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NotifyBackground(t: AppThemeColors) {
    Spacer(modifier = Modifier.fillMaxSize().drawBehind {
        val w = size.width; val h = size.height
        drawCircle(brush = Brush.radialGradient(
            listOf(t.accent.copy(0.14f), Color.Transparent),
            center = Offset(w * 0.85f, h * 0.08f), radius = w * 0.70f))
        drawCircle(brush = Brush.radialGradient(
            listOf(t.accentAlt.copy(0.10f), Color.Transparent),
            center = Offset(w * 0.10f, h * 0.75f), radius = w * 0.55f))
        drawCircle(brush = Brush.radialGradient(
            listOf(t.accentTertiary.copy(0.08f), Color.Transparent),
            center = Offset(w * 0.50f, h * 0.45f), radius = w * 0.45f))
    })
}

// ─────────────────────────────────────────────────────────────────────────────
// STATUS CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NotifyStatusCard(t: AppThemeColors, enabled: Boolean) {
    val dotColor = if (enabled) Color(0xFF22C55E) else Color(0xFFEF4444)
    val label    = if (enabled) "Notification Access เปิดอยู่" else "ยังไม่เปิด Notification Access"

    Surface(
        shape  = RoundedCornerShape(20.dp),
        color  = t.bgSurface.copy(alpha = 0.80f),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dotColor))
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 13.sp, color = if (enabled) Color(0xFF22C55E) else Color(0xFFEF4444))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// APP COLOR PALETTE  —  deterministic gradient per package
// ─────────────────────────────────────────────────────────────────────────────
private val appPalettes = mapOf(
    "jp.naver.line.android"      to Pair(Color(0xFF00B900), Color(0xFF00E676)), // LINE green
    "asuk.com.android.app"       to Pair(Color(0xFF00704A), Color(0xFF00BFA5)), // 7-11 green
    "th.co.truemoney.wallet"     to Pair(Color(0xFFFF6D00), Color(0xFFFFAB40)), // TrueMoney orange
    "com.linecorp.lineman.driver" to Pair(Color(0xFF00B900), Color(0xFF69F0AE)), // LINE MAN green
    "com.kasikorn.retail.mbanking.wap" to Pair(Color(0xFF00897B), Color(0xFF4DB6AC)), // KBank teal
    "com.scb.phone"              to Pair(Color(0xFF6A1B9A), Color(0xFFAB47BC))  // SCB purple
)

private fun packageToGradient(pkg: String): Pair<Color, Color> {
    appPalettes[pkg]?.let { return it }
    // fallback: hash package string to a pleasing hue pair
    val hue = (pkg.hashCode().and(0x7FFFFFFF) % 360).toFloat()
    val hue2 = (hue + 35f) % 360f
    return Pair(
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue,  0.65f, 0.80f))),
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue2, 0.45f, 0.95f)))
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// TRIGGER PICKER + DETAIL  —  shared by list card & grid-mode bottom sheet
// onColor = true  → ใช้บนพื้นหลังไล่สี (ตัวหนังสือขาว)
// onColor = false → ใช้บนพื้นผิวธีมปกติ (bottom sheet)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TriggerPickerAndDetail(
    group         : AppGroup,
    theme         : AppThemeColors,
    selectedIndex : Int,
    onSelectIndex : (Int) -> Unit,
    onToggle      : (UserRule) -> Unit,
    onEdit        : (UserRule) -> Unit,
    onDelete      : (UserRule) -> Unit,
    onColor       : Boolean
) {
    val t = theme
    val idx  = selectedIndex.coerceIn(0, group.rules.lastIndex)
    val rule = group.rules[idx]
    var menuExpanded by remember { mutableStateOf(false) }

    val fg     = if (onColor) Color.White else t.textPrimary
    val fgDim  = if (onColor) Color.White.copy(0.85f) else t.textSecondary
    val chipBg = if (onColor) Color.White.copy(0.22f) else t.accentContainer
    val chipFg = if (onColor) Color.White else t.onAccentContainer

    Column {
        // ── Dropdown: เลือกว่าจะดู trigger ตัวไหนของแอพนี้ ──────────────────
        Box {
            Surface(
                shape    = RoundedCornerShape(12.dp),
                color    = if (onColor) Color.White.copy(0.16f) else t.bgSurfaceHigh,
                modifier = Modifier.fillMaxWidth().clickable { menuExpanded = true }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (rule.enabled) Color(0xFF22C55E) else Color(0xFFEF4444))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        rule.keyword.ifBlank { "(ไม่มี keyword)" },
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = fg,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f)
                    )
                    if (group.total > 1) {
                        Text("${idx + 1}/${group.total}", fontSize = 10.sp, color = fgDim)
                        Spacer(Modifier.width(4.dp))
                    }
                    Icon(Icons.Rounded.ArrowDropDown, null, tint = fgDim, modifier = Modifier.size(18.dp))
                }
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                group.rules.forEachIndexed { i, r ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (r.enabled) Color(0xFF22C55E) else Color(0xFFEF4444))
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(r.keyword.ifBlank { "(ไม่มี keyword)" }, fontSize = 13.sp)
                            }
                        },
                        onClick = { onSelectIndex(i); menuExpanded = false }
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // TRIGGER row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.padding(bottom = 8.dp)
        ) {
            Surface(shape = CircleShape, color = chipBg) {
                Text(
                    "แจ้งเตือนแอพ",
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = chipFg,
                    letterSpacing = 0.5.sp,
                    modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(rule.keyword, fontSize = 13.sp, color = fgDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        // SPEAK row + actions
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = chipBg) {
                Text(
                    "คำพูด",
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = chipFg,
                    letterSpacing = 0.5.sp,
                    modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                rule.template, fontSize = 13.sp, color = fgDim,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = rule.enabled,
                onCheckedChange = { onToggle(rule) },
                modifier = Modifier.scale(0.8f),
                colors = if (onColor) SwitchDefaults.colors(
                    checkedThumbColor   = Color.White,
                    checkedTrackColor   = Color.White.copy(0.4f),
                    uncheckedThumbColor = Color.White.copy(0.6f),
                    uncheckedTrackColor = Color.White.copy(0.15f)
                ) else SwitchDefaults.colors(checkedThumbColor = t.accent)
            )
            IconButton(onClick = { onEdit(rule) }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.Edit, null, tint = fgDim, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = { onDelete(rule) }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.Delete, null, tint = fgDim.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LIST CARD  —  1 การ์ดต่อแอพ ยาวเต็มความกว้าง (รวมหลาย trigger ผ่าน dropdown)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AppGroupListCard(
    group         : AppGroup,
    theme         : AppThemeColors,
    selectedIndex : Int,
    onSelectIndex : (Int) -> Unit,
    onToggle      : (UserRule) -> Unit,
    onEdit        : (UserRule) -> Unit,
    onDelete      : (UserRule) -> Unit,
    onAddTrigger  : () -> Unit
) {
    val t = theme
    val (c1, c2) = packageToGradient(group.appPackage)
    val alpha    = if (group.enabledCount > 0) 1f else 0.45f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(
                colors  = listOf(c1.copy(alpha), c2.copy(alpha * 0.85f)),
                start   = Offset(0f, 0f),
                end     = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            ))
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-30).dp)
                .background(
                    Brush.radialGradient(listOf(Color.White.copy(0.18f), Color.Transparent)),
                    CircleShape
                )
        )

        Column(modifier = Modifier.padding(18.dp)) {

            // Row 1: avatar + name + count badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        group.appName.firstOrNull()?.uppercase() ?: "?",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    group.appName,
                    fontWeight    = FontWeight.ExtraBold,
                    fontSize      = 16.sp,
                    color         = Color.White,
                    maxLines      = 1,
                    overflow      = TextOverflow.Ellipsis,
                    modifier      = Modifier.weight(1f)
                )
                Surface(shape = RoundedCornerShape(10.dp), color = Color.White.copy(0.22f)) {
                    Text(
                        "${group.enabledCount}/${group.total} เปิด",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            TriggerPickerAndDetail(
                group = group, theme = t, selectedIndex = selectedIndex,
                onSelectIndex = onSelectIndex, onToggle = onToggle,
                onEdit = onEdit, onDelete = onDelete, onColor = true
            )

            Spacer(Modifier.height(10.dp))

            Surface(
                shape    = RoundedCornerShape(10.dp),
                color    = Color.White.copy(0.16f),
                modifier = Modifier.align(Alignment.End).clickable { onAddTrigger() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("เพิ่มเงื่อนไข", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SQUARE CARD  —  grid mode, 2 คอลัมน์, สี่เหลี่ยมจัตุรัสคู่ — แตะเพื่อเปิดดูรายละเอียด
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AppGroupSquareCard(
    group : AppGroup,
    theme : AppThemeColors,
    onTap : () -> Unit
) {
    val (c1, c2) = packageToGradient(group.appPackage)
    val alpha    = if (group.enabledCount > 0) 1f else 0.45f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(
                colors = listOf(c1.copy(alpha), c2.copy(alpha * 0.85f)),
                start  = Offset(0f, 0f),
                end    = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            ))
            .clickable { onTap() }
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.TopEnd)
                .offset(x = 24.dp, y = (-24).dp)
                .background(
                    Brush.radialGradient(listOf(Color.White.copy(0.18f), Color.Transparent)),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    group.appName.firstOrNull()?.uppercase() ?: "?",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White
                )
            }
            Column {
                Text(
                    group.appName,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 14.sp,
                    color      = Color.White,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(0.22f)) {
                    Text(
                        "${group.enabledCount}/${group.total} เงื่อนไข",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// APP DETAIL BOTTOM SHEET  —  เปิดจากการ์ดสี่เหลี่ยมจัตุรัส (grid mode)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDetailSheet(
    group         : AppGroup,
    theme         : AppThemeColors,
    selectedIndex : Int,
    onSelectIndex : (Int) -> Unit,
    onToggle      : (UserRule) -> Unit,
    onEdit        : (UserRule) -> Unit,
    onDelete      : (UserRule) -> Unit,
    onAddTrigger  : () -> Unit,
    onDismiss     : () -> Unit
) {
    val t = theme
    val (c1, c2) = packageToGradient(group.appPackage)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor    = t.bgSurface,
        dragHandle        = { BottomSheetDefaults.DragHandle(color = t.border) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(c1, c2))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        group.appName.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.appName, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = t.textPrimary)
                    Text(group.appPackage, fontSize = 10.sp, color = t.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Surface(shape = RoundedCornerShape(10.dp), color = t.accentContainer) {
                    Text(
                        "${group.enabledCount}/${group.total}",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = t.onAccentContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Surface(shape = RoundedCornerShape(16.dp), color = t.bgSurfaceAlt, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TriggerPickerAndDetail(
                        group = group, theme = t, selectedIndex = selectedIndex,
                        onSelectIndex = onSelectIndex, onToggle = onToggle,
                        onEdit = onEdit, onDelete = onDelete, onColor = false
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick  = onAddTrigger,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = t.accentContainer)
            ) {
                Icon(Icons.Rounded.Add, null, tint = t.onAccentContainer, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("เพิ่มเงื่อนไขใหม่", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = t.onAccentContainer)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LOG PANEL
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NotifyLogPanel(t: AppThemeColors, logLines: String, onClear: () -> Unit) {
    val scrollState = rememberScrollState()
    LaunchedEffect(logLines) { scrollState.animateScrollTo(scrollState.maxValue) }

    Surface(
        shape  = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color  = Color(0xF0111827),
        modifier = Modifier.fillMaxWidth().height(110.dp)
    ) {
        Box {
            Text(
                logLines,
                fontSize            = 10.sp,
                fontFamily          = FontFamily.Monospace,
                color               = Color(0xFF6EE7B7),
                lineHeight          = 15.sp,
                modifier            = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(12.dp)
            )
            IconButton(
                onClick  = onClear,
                modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
            ) {
                Icon(Icons.Rounded.Clear, null, tint = Color(0xFF6EE7B7).copy(0.6f), modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EDIT / ADD BOTTOM SHEET
// lockedPkg/lockedName != null  →  มาจากปุ่ม "+เพิ่มเงื่อนไข" ในการ์ดเดิม
//                                   ล็อกแอพไว้ ใส่แค่ keyword/template ใหม่
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRuleSheet(
    existing   : UserRule?,
    lockedPkg  : String? = null,
    lockedName : String? = null,
    theme      : AppThemeColors,
    onSave     : (UserRule) -> Unit,
    onDismiss  : () -> Unit
) {
    val t = theme
    val isLocked = existing == null && lockedPkg != null

    var pkg  by remember { mutableStateOf(existing?.appPackage ?: lockedPkg  ?: "") }
    var name by remember { mutableStateOf(existing?.appName    ?: lockedName ?: "") }
    var kw   by remember { mutableStateOf(existing?.keyword    ?: "") }
    var tpl  by remember { mutableStateOf(existing?.template   ?: "") }

    ModalBottomSheet(
        onDismissRequest  = onDismiss,
        containerColor    = t.bgSurface,
        dragHandle        = { BottomSheetDefaults.DragHandle(color = t.border) }
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                when {
                    isLocked          -> "เพิ่มเงื่อนไขใหม่"
                    existing == null  -> "เพิ่ม"
                    else              -> "แก้ไข"
                },
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = t.textPrimary
            )
            Text(
                if (isLocked) "สำหรับ ${name.ifBlank { pkg }}" else "กำหนดแอพและเงื่อนไขการพูด",
                fontSize = 13.sp, color = t.textSecondary
            )

            // App info card
            var showAppPicker by remember { mutableStateOf(false) }

            if (isLocked) {
                // แอพถูกล็อกไว้แล้ว — แสดงผลอย่างเดียว แก้ไม่ได้
                Surface(shape = RoundedCornerShape(16.dp), color = t.bgSurfaceAlt, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        val (c1, c2) = packageToGradient(pkg)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(c1, c2))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name.firstOrNull()?.uppercase() ?: "?", fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(name.ifBlank { pkg }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = t.textPrimary)
                            Text(pkg, fontSize = 10.sp, color = t.textSecondary)
                        }
                    }
                }
            } else {
                Surface(shape = RoundedCornerShape(16.dp), color = t.bgSurfaceAlt, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ข้อมูลแอพ", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = t.accent, letterSpacing = 0.5.sp, modifier = Modifier.weight(1f))
                            Surface(
                                shape    = RoundedCornerShape(10.dp),
                                color    = t.accent.copy(0.12f),
                                modifier = Modifier.clickable { showAppPicker = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
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
            }

            if (!isLocked && showAppPicker) {
                AppPickerSheet(
                    theme     = t,
                    onSelect  = { selectedPkg, selectedName ->
                        pkg  = selectedPkg
                        name = selectedName
                        showAppPicker = false
                    },
                    onDismiss = { showAppPicker = false }
                )
            }

            // Rule card
            Surface(shape = RoundedCornerShape(16.dp), color = t.bgSurfaceAlt, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("เงื่อนไขการพูด", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = t.accent, letterSpacing = 0.5.sp)
                    NotifyTextField("Keyword ที่ดัก", kw, t) { kw = it }
                    NotifyTextField("ข้อความที่พูด", tpl, t, singleLine = false) { tpl = it }
                }
            }

            // Placeholder hint
            Surface(shape = RoundedCornerShape(12.dp), color = t.accentContainer.copy(0.3f), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Placeholders ที่ใช้ได้", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = t.accent)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "{ข้อความ}  body ของแจ้งเตือน\n{หัวข้อ}     title ของแจ้งเตือน\n{ยอด}       ตัวเลข เช่น 54.00\n{แต้ม}      จำนวนเต็ม เช่น 9\n{ชื่อ}       ชื่อผู้โอน / ร้านค้า",
                        fontSize   = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color      = t.accent,
                        lineHeight = 18.sp
                    )
                }
            }

            // Save button
            Button(
                onClick = {
                    if (pkg.isBlank() || kw.isBlank() || tpl.isBlank()) return@Button
                    onSave(UserRule(
                        id         = existing?.id ?: java.util.UUID.randomUUID().toString(),
                        appPackage = pkg.trim(),
                        appName    = name.trim().ifEmpty { pkg.trim() },
                        keyword    = kw.trim(),
                        template   = tpl.trim(),
                        enabled    = existing?.enabled ?: true
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape  = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = t.accent)
            ) {
                Text("บันทึก", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun NotifyTextField(
    label     : String,
    value     : String,
    t         : AppThemeColors,
    singleLine: Boolean = true,
    onValue   : (String) -> Unit
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValue,
        label         = { Text(label, fontSize = 12.sp) },
        singleLine    = singleLine,
        minLines      = if (!singleLine) 2 else 1,
        shape         = RoundedCornerShape(12.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = t.accent,
            unfocusedBorderColor = t.borderVariant,
            focusedLabelColor    = t.accent,
            unfocusedLabelColor  = t.textSecondary,
            cursorColor          = t.accent
        ),
        modifier      = Modifier.fillMaxWidth(),
        textStyle     = LocalTextStyle.current.copy(fontSize = 13.sp, color = t.textPrimary)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// APP PICKER SHEET
// ─────────────────────────────────────────────────────────────────────────────
data class InstalledApp(val label: String, val packageName: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(
    theme    : AppThemeColors,
    onSelect : (pkg: String, name: String) -> Unit,
    onDismiss: () -> Unit
) {
    val t       = theme
    val context = LocalContext.current
    var query   by remember { mutableStateOf("") }

    val apps = remember {
        val pm = context.packageManager
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
            android.content.pm.PackageManager.PackageInfoFlags.of(android.content.pm.PackageManager.GET_ACTIVITIES.toLong())
        else null
        val packages = if (flags != null)
            pm.getInstalledPackages(flags)
        else
            @Suppress("DEPRECATION") pm.getInstalledPackages(android.content.pm.PackageManager.GET_ACTIVITIES)
        packages
            .filter { it.activities != null && it.activities!!.isNotEmpty() }
            .mapNotNull { pkg -> pkg.applicationInfo?.let { ai -> InstalledApp(pm.getApplicationLabel(ai).toString(), pkg.packageName) } }
            .sortedBy { it.label.lowercase() }
    }

    val filtered = remember(query) {
        if (query.isBlank()) apps
        else apps.filter {
            it.label.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = t.bgSurface,
        dragHandle       = { BottomSheetDefaults.DragHandle(color = t.border) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            Text(
                "เลือกแอพ",
                fontSize   = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = t.textPrimary,
                modifier   = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // Search bar
            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                placeholder   = { Text("ค้นหาแอพ...", fontSize = 13.sp) },
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = t.accent,
                    unfocusedBorderColor = t.borderVariant,
                    cursorColor          = t.accent
                ),
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = t.textPrimary)
            )

            LazyColumn(
                modifier       = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                items(filtered) { app ->
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(app.packageName, app.label) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // App icon - letter avatar
                        val (ic1, ic2) = packageToGradient(app.packageName)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(ic1, ic2))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                app.label.firstOrNull()?.uppercase() ?: "?",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = Color.White
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                color = t.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(app.packageName, fontSize = 10.sp, color = t.textSecondary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    HorizontalDivider(color = t.borderVariant.copy(0.4f), thickness = 0.5.dp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────────────────────
private fun isNLSEnabled(ctx: Context): Boolean {
    val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
    return flat.split(":").any { ComponentName.unflattenFromString(it)?.packageName == ctx.packageName }
}