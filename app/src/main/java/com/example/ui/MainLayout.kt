package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Decision
import com.example.data.JournalNote
import com.example.viewmodel.ChatMessage
import com.example.viewmodel.EchoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
    viewModel: EchoViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val profiles by viewModel.cognitiveProfiles.collectAsStateWithLifecycle()
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    val decisions by viewModel.allDecisions.collectAsStateWithLifecycle()
    val selectedDimName by viewModel.selectedDimension.collectAsStateWithLifecycle()

    val currentDimProfile = profiles.find { it.dimension == selectedDimName }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "COGNITIVE OS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B), // slate-500
                                letterSpacing = 2.5.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "EchoSelf",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Light,
                                color = Color.White,
                                letterSpacing = (-0.5).sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        // Beautiful geometric indicator circle matching top right header node
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F0F11))
                                .border(1.dp, Color(0xFF1E293B), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFBBF24)) // Glowing amber dot
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                val activeIndicatorColor = Color(0xFFFBBF24).copy(alpha = 0.15f)
                val activeIconColor = Color(0xFFFBBF24)
                val inactiveIconColor = Color(0xFF64748B)

                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.Psychology, contentDescription = "The Mirror") },
                    label = { Text("Mirror", style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = activeIconColor,
                        selectedTextColor = activeIconColor,
                        indicatorColor = activeIndicatorColor,
                        unselectedIconColor = inactiveIconColor,
                        unselectedTextColor = inactiveIconColor
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.Book, contentDescription = "Journal") },
                    label = { Text("History", style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = activeIconColor,
                        selectedTextColor = activeIconColor,
                        indicatorColor = activeIndicatorColor,
                        unselectedIconColor = inactiveIconColor,
                        unselectedTextColor = inactiveIconColor
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(imageVector = Icons.Default.Update, contentDescription = "Decisions") },
                    label = { Text("Reason", style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = activeIconColor,
                        selectedTextColor = activeIconColor,
                        indicatorColor = activeIndicatorColor,
                        unselectedIconColor = inactiveIconColor,
                        unselectedTextColor = inactiveIconColor
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> MirrorTabScreen(
                    viewModel = viewModel,
                    profiles = profiles,
                    selectedDimName = selectedDimName,
                    currentDimProfile = currentDimProfile
                )
                1 -> JournalTabScreen(
                    viewModel = viewModel,
                    notes = notes
                )
                2 -> DecisionTabScreen(
                    viewModel = viewModel,
                    decisions = decisions
                )
            }
        }
    }
}

// ==================== SCREEN 1: THE MIRROR ====================
@Composable
fun MirrorTabScreen(
    viewModel: EchoViewModel,
    profiles: List<com.example.data.CognitiveProfile>,
    selectedDimName: String,
    currentDimProfile: com.example.data.CognitiveProfile?
) {
    var subTabState by remember { mutableStateOf("Constellation") } // "Constellation", "Weekly Mirror", "Specialists"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Selector: Constellation | Weekly Mirror | Specialists
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Constellation", "Weekly Mirror", "Specialists").forEach { mode ->
                val active = subTabState == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { subTabState = mode }
                        .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode,
                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        when (subTabState) {
            "Constellation" -> {
                // Cognitive Specialist Persona panel
                val currentSpecialist by viewModel.selectedSpecialist.collectAsStateWithLifecycle()
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ACTIVE COGNITIVE SPECIALIST CORE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F0F11).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Color(0xFF1E293B).copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "Mirror" to Icons.Default.Psychology,
                            "Planner" to Icons.Default.EventNote,
                            "Critic" to Icons.Default.PsychologyAlt,
                            "Scientist" to Icons.Default.Science
                        ).forEach { (spec, icon) ->
                            val active = currentSpecialist == spec
                            val activeBg = when (spec) {
                                "Planner" -> Color(0xFFFBBF24).copy(alpha = 0.18f)
                                "Critic" -> Color(0xFFF87171).copy(alpha = 0.18f)
                                "Scientist" -> Color(0xFF34D399).copy(alpha = 0.18f)
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            }
                            val activeContent = when (spec) {
                                "Planner" -> Color(0xFFFBBF24)
                                "Critic" -> Color(0xFFF87171)
                                "Scientist" -> Color(0xFF34D399)
                                else -> MaterialTheme.colorScheme.primary
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) activeBg else Color.Transparent)
                                    .clickable { viewModel.selectSpecialist(spec) }
                                    .padding(horizontal = 4.dp)
                                    .testTag("specialist_btn_${spec.lowercase(Locale.ROOT)}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = spec,
                                        tint = if (active) activeContent else Color(0xFF94A3B8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = spec,
                                        color = if (active) Color.White else Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Constellation Canvas View Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F0F11).copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF1E293B).copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ConstellationView(
                            profiles = profiles,
                            selectedDimension = selectedDimName,
                            onDimensionSelected = { viewModel.selectDimension(it) },
                            selectedSpecialist = currentSpecialist,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(6.dp)
                        ) {
                            Text(
                                "Touch Stars to Cast",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Voice Thought Uplink Capture Interface
                VoiceUplinkSection(viewModel = viewModel)

                Spacer(modifier = Modifier.height(14.dp))

                // Selected dimension display details
                AnimatedContent(
                    targetState = currentDimProfile,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "DimensionDetails"
                ) { profile ->
                    if (profile != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dimension_card"),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF0F0F11).copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF1E293B).copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Geometric left bar element: w-1 h-4 bg-amber-500
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(14.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFF59E0B))
                                        )
                                        Text(
                                            text = profile.dimension.uppercase(Locale.ROOT),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Light,
                                            color = Color.White,
                                            fontFamily = FontFamily.SansSerif,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFFFBBF24).copy(alpha = 0.12f))
                                            .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.25f), CircleShape)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${(profile.score * 100).toInt()}% SYNCED",
                                            color = Color(0xFFFBBF24),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                
                                LinearProgressIndicator(
                                    progress = { profile.score },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                    color = Color(0xFFFBBF24),
                                    trackColor = Color(0xFF1E293B)
                                )

                                Text(
                                    text = profile.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFCBD5E1), // Slate-300
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.Light
                                )

                                val dateF = SimpleDateFormat("MMM d, yyyy - HH:mm", Locale.getDefault())
                                Text(
                                    text = "Last neural calibration: ${dateF.format(Date(profile.lastUpdated))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B), // Slate-500
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Loading neural mind space...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            "Weekly Mirror" -> {
                // Weekly report section
                val report by viewModel.weeklyReport.collectAsStateWithLifecycle()
                val isReportLoading by viewModel.isReportLoading.collectAsStateWithLifecycle()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F0F11).copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF1E293B).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(14.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF59E0B))
                                )
                                Text(
                                    text = "Cognitive Mirror Audit",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Light,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            IconButton(
                                onClick = { viewModel.generateWeeklyReport() },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color(0xFFFBBF24).copy(alpha = 0.12f)
                                ),
                                enabled = !isReportLoading
                            ) {
                                if (isReportLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFFFBBF24))
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = "Regenerate report", tint = Color(0xFFFBBF24))
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1E293B).copy(alpha = 0.6f))

                        Text(
                            text = report,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1), // Slate-300
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }

            "Specialists" -> {
                // Specialist Chat Pane
                val chatList by viewModel.chatHistory.collectAsStateWithLifecycle()
                val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
                val currentSpec by viewModel.selectedSpecialist.collectAsStateWithLifecycle()

                // Profile picker
                val specialistsList = listOf(
                    "Mirror" to Icons.Default.Circle,
                    "Planner" to Icons.Default.EventNote,
                    "Teacher" to Icons.Default.Class,
                    "Critic" to Icons.Default.PsychologyAlt,
                    "Optimist" to Icons.Default.WbSunny,
                    "Scientist" to Icons.Default.Science
                )

                Text(
                    text = "COMMUNE WITH INDEPENDENT MIND CORES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )

                // Row of specialists selection chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .horizontalScrollStateFix(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    specialistsList.forEach { (specName, icon) ->
                        val selected = currentSpec == specName
                        ElevatedFilterChip(
                            selected = selected,
                            onClick = { viewModel.selectSpecialist(specName) },
                            label = { Text(specName, fontSize = 11.sp) },
                            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                // Chat history box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11).copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B).copy(alpha = 0.5f))
                ) {
                    val chatScrollState = rememberScrollState()
                    LaunchedEffect(chatList.size) {
                        chatScrollState.animateScrollTo(chatScrollState.maxValue)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(chatScrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chatList.forEach { msg ->
                            val isSystem = msg.sender == "EchoSystem"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isSystem) Arrangement.Start else Arrangement.End
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isSystem) 2.dp else 16.dp,
                                            bottomEnd = if (isSystem) 16.dp else 2.dp
                                        ))
                                        .background(
                                            if (isSystem) Color(0xFF1E293B).copy(alpha = 0.4f)
                                            else Color(0xFF1E293B)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSystem) Color(0xFFFBBF24).copy(alpha = 0.2f) else Color(0xFF334155),
                                            shape = RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isSystem) 2.dp else 16.dp,
                                                bottomEnd = if (isSystem) 16.dp else 2.dp
                                            )
                                        )
                                        .padding(12.dp)
                                        .widthIn(max = 240.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = if (isSystem) "${currentSpec.uppercase(Locale.ROOT)} SPECIALIST" else "YOU",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSystem) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = msg.text,
                                            fontSize = 13.sp,
                                            color = Color(0xFFF1F5F9), // Slate-100
                                            lineHeight = 18.sp,
                                            fontWeight = FontWeight.Light
                                        )
                                    }
                                }
                            }
                        }
                        if (isChatLoading) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.8.dp, color = Color(0xFFFBBF24))
                            }
                        }
                    }
                }

                // Chat sender field
                var promptInput by remember { mutableStateOf("") }
                val focusManager = LocalFocusManager.current

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = { Text("Ask: 'What would my best self do?'", color = Color(0xFF64748B)) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input"),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFBBF24),
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedContainerColor = Color(0xFF0F0F11).copy(alpha = 0.2f),
                            unfocusedContainerColor = Color(0xFF0F0F11).copy(alpha = 0.2f),
                            focusedLabelColor = Color(0xFFFBBF24),
                            unfocusedLabelColor = Color(0xFF64748B)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (promptInput.isNotBlank()) {
                                viewModel.askEchoSelf(promptInput)
                                promptInput = ""
                                focusManager.clearFocus()
                            }
                        })
                    )

                    IconButton(
                        onClick = {
                            if (promptInput.isNotBlank()) {
                                viewModel.askEchoSelf(promptInput)
                                promptInput = ""
                                focusManager.clearFocus()
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFFBBF24)),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("send_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black)
                    }
                }
            }
        }
    }
}

// ==================== SCREEN 2: THE JOURNAL ====================
@Composable
fun JournalTabScreen(
    viewModel: EchoViewModel,
    notes: List<JournalNote>
) {
    var noteContent by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Thought") } // "Thought", "Study", "Work", "Ambition"
    var noteTags by remember { mutableStateOf("") }
    
    val categories = listOf("Thought", "Study", "Work", "Ambition")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form to write new entries
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11).copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, Color(0xFF1E293B).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "FEED THE COGNITIVE GRAPH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF59E0B),
                    letterSpacing = 1.8.sp,
                    fontFamily = FontFamily.Monospace
                )

                OutlinedTextField(
                    value = noteContent,
                    onValueChange = { noteContent = it },
                    label = { Text("Write your thoughts, studies, or daily breakthroughs...", color = Color(0xFF64748B)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .testTag("note_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFBBF24),
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedLabelColor = Color(0xFFFBBF24),
                        unfocusedLabelColor = Color(0xFF64748B),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Selection
                    Box(modifier = Modifier.weight(1f)) {
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth().testTag("category_select"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = "Class: $selectedCategory",
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B))
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Simple Tag Input
                    OutlinedTextField(
                        value = noteTags,
                        onValueChange = { noteTags = it },
                        placeholder = { Text("tags csv", color = Color(0xFF64748B)) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tags_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFBBF24),
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }

                Button(
                    onClick = {
                        if (noteContent.isNotBlank()) {
                            viewModel.addJournalNote(noteContent, selectedCategory, noteTags)
                            noteContent = ""
                            noteTags = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_note_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFBBF24),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calibrate Into Constellation", fontWeight = FontWeight.Bold)
                }
            }
        }

        // List of entries
        Text(
            text = "CHRONOLOGY OF MENTAL STATES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.8.sp,
            fontFamily = FontFamily.Monospace
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (notes.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF1E293B)
                        )
                        Text(
                            "Constellation is currently dark. Write some entries.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(notes) { note ->
                    val borderAccentColor = when (note.category) {
                        "Thought" -> Color(0xFFFBBF24)
                        "Study" -> Color(0xFF3B82F6)
                        "Work" -> Color(0xFF10B981)
                        else -> Color(0xFFEC4899)
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_item_${note.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11).copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, Color(0xFF1E293B).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // High-precision left indicator strip representing category
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .align(Alignment.CenterVertically)
                                    .background(borderAccentColor)
                            )
                            
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(borderAccentColor.copy(alpha = 0.12f))
                                                .border(1.dp, borderAccentColor.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = note.category.uppercase(Locale.ROOT),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = borderAccentColor
                                            )
                                        }

                                        val dateF = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                                        Text(
                                            text = dateF.format(Date(note.timestamp)),
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteNote(note.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete item",
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFFEF4444)
                                        )
                                    }
                                }

                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFCBD5E1),
                                    fontWeight = FontWeight.Light
                                )

                                if (note.tagString.isNotBlank()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        note.tagString.split(",").map { it.trim() }.forEach { tag ->
                                            if (tag.isNotEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF1E293B).copy(alpha = 0.4f))
                                                        .border(1.dp, Color(0xFF334155).copy(alpha = 0.3f), CircleShape)
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("#$tag", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF94A3B8))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== SCREEN 3: DECISION COGNITIVE REPLAY ====================
@Composable
fun DecisionTabScreen(
    viewModel: EchoViewModel,
    decisions: List<Decision>
) {
    var decisionTitle by remember { mutableStateOf("") }
    var expectedOutcome by remember { mutableStateOf("") }
    var emotionalState by remember { mutableStateOf("Calm") }
    var confidence by remember { mutableFloatStateOf(7f) }

    val emotions = listOf("Calm", "Anxious", "Excited", "Determined")

    // Retrospective states (evaluation dialog)
    var targetDecisionToReview by remember { mutableStateOf<Decision?>(null) }
    var actualOutcomeText by remember { mutableStateOf("") }
    var lessonsLearnedText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form to record new decisions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11).copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, Color(0xFF1E293B).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "SEED NEW DECISION FOR RETRO REPLAY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF59E0B),
                    letterSpacing = 1.8.sp,
                    fontFamily = FontFamily.Monospace
                )

                OutlinedTextField(
                    value = decisionTitle,
                    onValueChange = { decisionTitle = it },
                    label = { Text("What decision is being made?", color = Color(0xFF64748B)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("decision_title_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFBBF24),
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedLabelColor = Color(0xFFFBBF24),
                        unfocusedLabelColor = Color(0xFF64748B),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                OutlinedTextField(
                    value = expectedOutcome,
                    onValueChange = { expectedOutcome = it },
                    label = { Text("What expected outcome do you predict in detail?", color = Color(0xFF64748B)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .testTag("expected_outcome_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFBBF24),
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedLabelColor = Color(0xFFFBBF24),
                        unfocusedLabelColor = Color(0xFF64748B),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                // Emotion & Confidence line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Emotion Picker
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth().testTag("emotion_select"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = "Mood: $emotionalState",
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B))
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            emotions.forEach { emo ->
                                DropdownMenuItem(
                                    text = { Text(emo) },
                                    onClick = {
                                        emotionalState = emo
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Confidence Slider
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Confidence: ${confidence.toInt()}/10",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                        Slider(
                            value = confidence,
                            onValueChange = { confidence = it },
                            valueRange = 1f..10f,
                            steps = 8,
                            modifier = Modifier.testTag("confidence_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFBBF24),
                                activeTrackColor = Color(0xFFF59E0B),
                                inactiveTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }
                }

                Button(
                    onClick = {
                        if (decisionTitle.isNotBlank() && expectedOutcome.isNotBlank()) {
                            viewModel.addDecision(decisionTitle, expectedOutcome, confidence.toInt(), emotionalState)
                            decisionTitle = ""
                            expectedOutcome = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_decision_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFBBF24),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Expectation Profile", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active and Archived Predictions List
        Text(
            text = "DECISION REPLAY LOGS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.8.sp,
            fontFamily = FontFamily.Monospace
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (decisions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.AddAlarm,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF1E293B)
                        )
                        Text(
                            "No decision architectures stored yet. Formulate one above.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(decisions) { decision ->
                    val isPending = decision.actualOutcome == null
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("decision_item_${decision.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11).copy(alpha = 0.4f)),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isPending) Color(0xFFFBBF24).copy(alpha = 0.3f) else Color(0xFF1E293B).copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isPending) Color(0xFFFBBF24).copy(alpha = 0.12f)
                                                else Color(0xFF10B981).copy(alpha = 0.12f)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isPending) Color(0xFFFBBF24).copy(alpha = 0.25f) else Color(0xFF10B981).copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isPending) "PREDICTION RUNNING" else "REPLAY CALIBRATED",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPending) Color(0xFFFBBF24) else Color(0xFF10B981)
                                        )
                                    }

                                    val dateF = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                    Text(
                                        text = dateF.format(Date(decision.timestamp)),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteDecision(decision.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete decision",
                                        modifier = Modifier.size(14.dp),
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            }

                            Text(
                                text = decision.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Normal,
                                color = Color.White
                            )

                            Text(
                                text = "Prediction: ${decision.expectedOutcome}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1),
                                fontWeight = FontWeight.Light
                            )

                            if (!isPending) {
                                HorizontalDivider(color = Color(0xFF1E293B).copy(alpha = 0.6f))
                                Text(
                                    text = "Actual Outcome: ${decision.actualOutcome}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Normal
                                )
                                Text(
                                    text = "Lessons: ${decision.lessonsLearned}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Light
                                )
                            }

                            if (decision.aiPatternAnalysis != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFBBF24).copy(alpha = 0.05f))
                                        .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFFBBF24))
                                            Text("COGNITIVE AUDIT:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24), fontFamily = FontFamily.Monospace)
                                        }
                                        Text(
                                            text = decision.aiPatternAnalysis,
                                            fontSize = 11.sp,
                                            color = Color(0xFFCBD5E1),
                                            lineHeight = 15.sp,
                                            fontWeight = FontWeight.Light
                                        )
                                    }
                                }
                            }

                            if (isPending) {
                                Button(
                                    onClick = {
                                        targetDecisionToReview = decision
                                        actualOutcomeText = ""
                                        lessonsLearnedText = ""
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                        .testTag("review_decision_${decision.id}"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFBBF24).copy(alpha = 0.12f),
                                        contentColor = Color(0xFFFBBF24)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.25f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Outlined.RateReview, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Replay (Audit Outcome & Calibrate Bias)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Evaluation micro dialog Modal (Double-loop calibration screen)
    if (targetDecisionToReview != null) {
        val decision = targetDecisionToReview!!
        AlertDialog(
            onDismissRequest = { targetDecisionToReview = null },
            title = { Text("Audit Action: ${decision.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Review predictions against reality today to train your risk decision-modeling engine.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = actualOutcomeText,
                        onValueChange = { actualOutcomeText = it },
                        label = { Text("What actually happened?") },
                        modifier = Modifier.fillMaxWidth().testTag("actual_outcome_input")
                    )
                    OutlinedTextField(
                        value = lessonsLearnedText,
                        onValueChange = { lessonsLearnedText = it },
                        label = { Text("Lessons, cognitive corrections & pitfalls discovered:") },
                        modifier = Modifier.fillMaxWidth().testTag("lessons_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (actualOutcomeText.isNotBlank()) {
                            viewModel.evaluateDecision(decision.id, actualOutcomeText, lessonsLearnedText)
                            targetDecisionToReview = null
                        }
                    },
                    modifier = Modifier.testTag("confirm_evaluate_button")
                ) {
                    Text("Calibrate Double-Loop")
                }
            },
            dismissButton = {
                TextButton(onClick = { targetDecisionToReview = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun VoiceUplinkSection(
    viewModel: EchoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceState by viewModel.voiceInputState.collectAsStateWithLifecycle()
    
    // Remember controller
    val voiceController = remember(context, viewModel) {
        VoiceInputController(context, viewModel)
    }
    
    DisposableEffect(voiceController) {
        onDispose {
            voiceController.destroy()
        }
    }

    // Permission tracking
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            voiceController.startListening()
        } else {
            viewModel.setVoiceError("Audio recording permission is required to transcribe thoughts.")
        }
    }

    // Custom glowing pulsing circle animation for the active mic state
    val micPulseTransition = rememberInfiniteTransition(label = "MicPulseTransition")
    val micPulseScale by micPulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (voiceState.isListening) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MicPulseScale"
    )
    val micPulseAlpha by micPulseTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = if (voiceState.isListening) 0.15f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MicPulseAlpha"
    )

    // Precompiled high quality test thoughts for fallback & microphonic testing simplicity
    val predefinedThoughts = listOf(
        "I am feeling exceptionally focused on studying deep coding structures today." to "Focus",
        "Worrying about this high stakes decision is keeping me anxious and alert." to "Decision",
        "I need guidance about how to explore my creative curiosities without lost productivity." to "Curiosity",
        "Learning neural science architectures restores my memory pathways." to "Learning"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("voice_uplink_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11).copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, Color(0xFF1E293B).copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsVoice,
                        contentDescription = "Voice Uplink",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "NEURAL VOICE UPLINK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.8.sp
                    )
                }

                // Voice status indicator light
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (voiceState.isListening) Color(0xFFFBBF24).copy(alpha = 0.15f)
                            else Color(0xFF1E293B)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (voiceState.isListening) Color(0xFFFBBF24)
                                    else if (voiceState.isAnalyzing) Color(0xFF34D399)
                                    else Color(0xFF64748B)
                                )
                        )
                        Text(
                            text = if (voiceState.isListening) "LISTENING"
                                   else if (voiceState.isAnalyzing) "COGNIZING"
                                   else "READY",
                            color = if (voiceState.isListening) Color(0xFFFBBF24)
                                    else if (voiceState.isAnalyzing) Color(0xFF34D399)
                                    else Color(0xFF64748B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF1E293B).copy(alpha = 0.5f))

            // Main Microphone control area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Mic Sphere
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(76.dp)
                ) {
                    // Pulse Ring 1
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(micPulseScale)
                            .clip(CircleShape)
                            .background(
                                if (voiceState.isListening) Color(0xFFFBBF24).copy(alpha = micPulseAlpha)
                                else Color(0xFF1E293B).copy(alpha = 0.2f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (voiceState.isListening) Color(0xFFFBBF24).copy(alpha = 0.5f) else Color(0xFF334155),
                                shape = CircleShape
                            )
                    )

                    // Core Button
                    IconButton(
                        onClick = {
                            if (voiceState.isListening) {
                                voiceController.stopListening()
                            } else {
                                if (hasMicPermission) {
                                    voiceController.startListening()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (voiceState.isListening) Color(0xFFFBBF24) else Color(0xFF1E293B)
                        ),
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .testTag("microphone_button")
                    ) {
                        Icon(
                            imageVector = if (voiceState.isListening) Icons.Default.Square else Icons.Default.Mic,
                            contentDescription = "Voice Input Toggle",
                            tint = if (voiceState.isListening) Color.Black else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Transcription live feed
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (voiceState.isListening) "TRANSCRIBING THOUGHTS..."
                               else if (voiceState.isAnalyzing) "SENTIMENT & TOPIC MATCHING..."
                               else "TAP MICROPHONE OR SIMULATE BELOW",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = voiceState.transcript.ifBlank { "What is on your mind? Talk about learning focus, creative designs, risks, or plans..." },
                        color = if (voiceState.transcript.isNotBlank()) Color.White else Color(0xFF64748B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Light,
                        lineHeight = 18.sp
                    )

                    if (voiceState.error != null) {
                        Text(
                            text = voiceState.error ?: "",
                            color = Color(0xFFF87171),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Results Alignment Panel
            if (voiceState.analyzedTopic != null && !voiceState.isListening && !voiceState.isAnalyzing) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF070708)),
                        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF34D399))
                                    )
                                    Text(
                                        text = "ASSOCIATED ASTRO-NODE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34D399),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Text(
                                    text = "+${(voiceState.scoreDelta * 100).toInt()}% SYNC BALANCE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24),
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E293B))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "🎯 ${voiceState.analyzedTopic}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF2DD4BF).copy(alpha = 0.15f))
                                        .border(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "✨ ${voiceState.analyzedSentiment}",
                                        color = Color(0xFF2DD4BF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Text(
                                text = voiceState.insight ?: "",
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Light
                            )
                        }
                    }
                }
            }

            // Fallback & Simulation Pills Row
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "MIND VOICE INTEGRATION TESTER",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScrollStateFix(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    predefinedThoughts.forEachIndexed { i, (thought, topic) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1E293B).copy(alpha = 0.4f))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                                .clickable {
                                    viewModel.analyzeVoiceThought(thought)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("sim_voice_thought_$i"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "Simulate $topic",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom Row extension to scroll horizontally with no overflow issues
@Composable
fun Modifier.horizontalScrollStateFix(): Modifier {
    return this.horizontalScroll(rememberScrollState())
}
