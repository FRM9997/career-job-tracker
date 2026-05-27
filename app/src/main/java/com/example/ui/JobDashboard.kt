package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.JobPosting
import com.example.data.UserProfile
import com.example.viewmodel.JobStats
import com.example.viewmodel.JobViewModel
import com.example.viewmodel.ScraperUiState
import java.net.URL
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

// Custom Portal Colors
object PortalThemes {
    val Saramin = Color(0xFF0075FF)          // Vibrant Blue
    val JobKorea = Color(0xFFFF5000)         // Bright Orange-Red
    val Worknet = Color(0xFF0284C7)          // Sky-Navy Blue
    val Incruit = Color(0xFFFF9900)          // Warm Orange
    val Wanted = Color(0xFF3366FF)           // Deep Blue
    val Linkareer = Color(0xFF00C7AE)        // Custom Teal
    val Jasoseol = Color(0xFF9E00FF)         // Rich Purple
    val Catch = Color(0xFFE02241)            // Soft Crimson
    val JobPlanet = Color(0xFF00C362)        // Soft Green
    val RememberCareer = Color(0xFF1F2024)   // Charcoal Dark Slate
    val Unknown = Color(0xFF6750A4)          // System default

    fun getPlatformColor(name: String): Color {
        return when {
            name.contains("사람인") -> Saramin
            name.contains("잡코리아") -> JobKorea
            name.contains("워크넷") || name.contains("고용24") || name.contains("워크") -> Worknet
            name.contains("인크루트") -> Incruit
            name.contains("원티드") -> Wanted
            name.contains("링커리어") -> Linkareer
            name.contains("자소설") -> Jasoseol
            name.contains("캐치") -> Catch
            name.contains("잡플래닛") -> JobPlanet
            name.contains("리멤버") -> RememberCareer
            else -> Unknown
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun JobDashboard(
    viewModel: JobViewModel,
    modifier: Modifier = Modifier
) {
    val postings by viewModel.uiPostings.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val selectedPlatform by viewModel.selectedPlatform.collectAsStateWithLifecycle()
    val selectedExperience by viewModel.selectedExperience.collectAsStateWithLifecycle()
    val feedFilterQuery by viewModel.feedFilterQuery.collectAsStateWithLifecycle()
    val scraperState by viewModel.scraperState.collectAsStateWithLifecycle()
    val selectedPosting by viewModel.selectedPosting.collectAsStateWithLifecycle()
    val isOnlyFavorites by viewModel.isOnlyFavorites.collectAsStateWithLifecycle()
    val showDuplicates by viewModel.showDuplicates.collectAsStateWithLifecycle()
    val platformConfigs by viewModel.platformConfigs.collectAsStateWithLifecycle()
    val rawPostings by viewModel.rawDbPostings.collectAsStateWithLifecycle()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val showDImminentOnly by viewModel.showDImminentOnly.collectAsStateWithLifecycle()

    var activeScrapeKeyword by remember { mutableStateOf("") }
    var showScrapeDialog by remember { mutableStateOf(false) }
    var showSourcesDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val todayFormatted = remember {
        java.text.SimpleDateFormat("yyyy년 M월 d일", java.util.Locale.KOREAN).format(java.util.Date())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Work, contentDescription = "채용공고 Feed") },
                    label = { Text("채용공고", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_feed_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "내 스펙 프로필") },
                    label = { Text("내 정보", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_profile_tab")
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = todayFormatted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "모아잡 (MoaJob)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 22.sp,
                                letterSpacing = (-0.5).sp,
                                modifier = Modifier.testTag("app_title")
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.resetDatabase() },
                        modifier = Modifier.testTag("reset_db_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "DB 초기화",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showSourcesDialog = true },
                        modifier = Modifier.testTag("manage_sources_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "수집 소스 관리",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "K",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showScrapeDialog = true },
                    icon = { Icon(Icons.Default.CloudDownload, contentDescription = "AI 수집") },
                    text = { Text("AI 공고 수집") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.testTag("ai_scrape_fab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTab == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                // 1. Stats Panel
                StatsPanel(stats = stats)
                
                Spacer(modifier = Modifier.height(12.dp))

                // 2. Search Text Input within Current cached list
                OutlinedTextField(
                    value = feedFilterQuery,
                    onValueChange = { viewModel.feedFilterQuery.value = it },
                    placeholder = { Text("회사명, 공고명, 주요 기술스택 검색...") },
                    leadingIcon = { Icon(Icons.Default.Search, "검색") },
                    trailingIcon = {
                        if (feedFilterQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.feedFilterQuery.value = "" }) {
                                Icon(Icons.Default.Clear, "지우기")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feed_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Platform filter pill row
                PlatformFilterRow(
                    selectedPlatform = selectedPlatform,
                    onPlatformSelect = { viewModel.selectedPlatform.value = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Auxiliary Filters (Experience, Bookmark, Show Dups Toggle)
                AuxiliaryFilterSection(
                    selectedExperience = selectedExperience,
                    onExperienceSelect = { viewModel.selectedExperience.value = it },
                    favoritesOnly = isOnlyFavorites,
                    onFavoritesToggle = { viewModel.isOnlyFavorites.value = !isOnlyFavorites },
                    showDuplicates = showDuplicates,
                    onDuplicatesToggle = { viewModel.showDuplicates.value = !showDuplicates },
                    showDImminentOnly = showDImminentOnly,
                    onDImminentToggle = { viewModel.showDImminentOnly.value = !showDImminentOnly }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Application Status Filter Row
                Column {
                    Text(
                        text = "지원 상태 구분 필터",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val statuses = listOf("전체", "관심", "작성예정", "작성중", "제출완료", "서류합격", "면접예정", "면접완료", "불합격", "보류")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(statuses) { status ->
                            val active = selectedStatusFilter == status
                            val statusThemeColor = when (status) {
                                "관심" -> Color(0xFF6750A4)
                                "작성예정" -> Color(0xFF9E00FF)
                                "작성중" -> Color(0xFFF57C00)
                                "제출완료" -> Color(0xFF1976D2)
                                "서류합격" -> Color(0xFF00C362)
                                "면접예정" -> Color(0xFF00C7AE)
                                "면접완료" -> Color(0xFF388E3C)
                                "불합격" -> Color(0xFFD32F2F)
                                "보류" -> Color(0xFF7F8C8D)
                                else -> MaterialTheme.colorScheme.primary
                            }
                            
                            FilterChip(
                                selected = active,
                                onClick = { viewModel.selectedStatusFilter.value = status },
                                label = { Text(status, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = statusThemeColor.copy(alpha = 0.15f),
                                    selectedLabelColor = statusThemeColor,
                                    containerColor = Color.Transparent,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = active,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                                    selectedBorderColor = statusThemeColor.copy(alpha = 0.5f),
                                    borderWidth = 1.dp
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Job List Section
                if (postings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.WorkOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "수집된 채용공고가 없거나 필터에 일치하지 않습니다.",
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "우측 하단의 AI 수집버튼으로 수집 조건을 넣어보세요!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("job_postings_list"),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(postings, key = { it.id }) { posting ->
                            JobCard(
                                posting = posting,
                                onCardClick = { viewModel.selectPosting(posting) },
                                onFavoriteToggle = { viewModel.toggleFavorite(posting) }
                            )
                        }
                    }
                }
            }
            } else {
                UserProfileTab(viewModel = viewModel)
            }

            // AI Scraping progress / result bar
            AnimatedVisibility(
                visible = scraperState !is ScraperUiState.Idle,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
            ) {
                ScraperNotification(
                    state = scraperState,
                    onDismiss = { viewModel.resetScraperState() }
                )
            }

            // CRITICAL DETAILED VIEW (Bottom Sheet style)
            if (selectedPosting != null) {
                JobDetailsSheet(
                    posting = selectedPosting!!,
                    onDismiss = { viewModel.selectPosting(null) },
                    onFavoriteToggle = { viewModel.toggleFavorite(selectedPosting!!) },
                    onStatusChange = { viewModel.updateApplicationStatus(selectedPosting!!, it) },
                    onMemoSave = { viewModel.updateMemo(selectedPosting!!, it) },
                    viewModel = viewModel
                )
            }

            // AI Scraping Dialog Trigger
            if (showScrapeDialog) {
                AlertDialog(
                    onDismissRequest = { showScrapeDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI 포털 일치화 공고 수집")
                        }
                    },
                    text = {
                        Column {
                            Text(
                                text = "수집하고 싶으신 채용 분야나 키워드를 입력해 주세요. 사람인, 잡코리아, 원티드 등 한국의 대표 포털들로부터 실시간 정보 통합 및 중복제거 시뮬레이션을 생성합니다.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            OutlinedTextField(
                                value = activeScrapeKeyword,
                                onValueChange = { activeScrapeKeyword = it },
                                placeholder = { Text("예: 안드로이드 개발자, 리액트, UI기획") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    if (activeScrapeKeyword.isNotBlank()) {
                                        viewModel.performAiScraping(activeScrapeKeyword)
                                        showScrapeDialog = false
                                        keyboardController?.hide()
                                    }
                                }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("scrape_dialog_input"),
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (activeScrapeKeyword.isNotBlank()) {
                                    viewModel.performAiScraping(activeScrapeKeyword)
                                    showScrapeDialog = false
                                }
                            },
                            enabled = activeScrapeKeyword.isNotBlank(),
                            modifier = Modifier.testTag("scrape_dialog_confirm")
                        ) {
                            Text("수집 가동")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showScrapeDialog = false }) {
                            Text("취소")
                        }
                    }
                )
            }

            // Platform Collection Source Manager Dialog
            if (showSourcesDialog) {
                AlertDialog(
                    onDismissRequest = { showSourcesDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "채용공고 수집 소스 관리",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                        ) {
                            Text(
                                text = "합법적인 사용자 연동 API 및 수신메일을 기반으로 채용 소스를 연결합니다. 무단 크롤링을 배제한 지속 가능한 플랫폼 구조입니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                items(platformConfigs, key = { it.name }) { config ->
                                    val platformPostingsCount = rawPostings.count { 
                                        it.platforms.split(",").map { p -> p.trim() }.contains(config.name) ||
                                        it.originalPlatforms.split(",").map { p -> p.trim() }.contains(config.name)
                                    }
                                    
                                    val formattedTime = remember(config.lastUpdated) {
                                        java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.KOREAN).format(java.util.Date(config.lastUpdated))
                                    }
                                    
                                    val badgeColor = PortalThemes.getPlatformColor(config.name)
                                    val methodBgColor = when (config.collectionMethod) {
                                        "API 연동", "API" -> Color(0xFFE8DEF8)
                                        "수동등록" -> Color(0xFFE2F0D9)
                                        "알림메일" -> Color(0xFFFFF2CC)
                                        else -> Color(0xFFF2F4F7)
                                    }
                                    val methodTextColor = when (config.collectionMethod) {
                                        "API 연동", "API" -> Color(0xFF65558F)
                                        "수동등록" -> Color(0xFF385723)
                                        "알림메일" -> Color(0xFF7F6000)
                                        else -> Color(0xFF5F6368)
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (config.isActive) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface)
                                            .border(
                                                1.dp,
                                                if (config.isActive) badgeColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Badge line
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 4.dp, height = 36.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(badgeColor)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = config.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (config.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(methodBgColor)
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = config.collectionMethod,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = methodTextColor
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "${platformPostingsCount}개 수집됨",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (config.isActive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "•",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (config.isActive) "수집: $formattedTime" else "일시 정지됨",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        }
                                        
                                        Switch(
                                            checked = config.isActive,
                                            onCheckedChange = { viewModel.togglePlatformConfig(config) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = badgeColor,
                                                checkedTrackColor = badgeColor.copy(alpha = 0.3f)
                                            ),
                                            modifier = Modifier.scale(0.8f).testTag("platform_switch_${config.name}")
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { showSourcesDialog = false },
                            modifier = Modifier.testTag("sources_dialog_close")
                        ) {
                            Text("완료")
                        }
                    },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
fun StatsPanel(stats: JobStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stats_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "실시간 중복 정제 HUD",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "스마트 엔진 작동 중",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "총 수집 공고",
                    value = "${stats.totalCollected}건",
                    icon = Icons.Default.CloudQueue,
                    tint = MaterialTheme.colorScheme.primary
                )
                
                VerticalDivider(modifier = Modifier.height(40.dp), color = MaterialTheme.colorScheme.outlineVariant)
                
                StatItem(
                    label = "필터링 중복",
                    value = "${stats.duplicatesRemoved}건",
                    icon = Icons.Default.MergeType,
                    tint = Color(0xFFFF5000)
                )
                
                VerticalDivider(modifier = Modifier.height(40.dp), color = MaterialTheme.colorScheme.outlineVariant)

                StatItem(
                    label = "무중복 공고",
                    value = "${stats.uniqueCleaned}건",
                    icon = Icons.Default.Layers,
                    tint = Color(0xFF0075FF)
                )
                
                VerticalDivider(modifier = Modifier.height(40.dp), color = MaterialTheme.colorScheme.outlineVariant)

                StatItem(
                    label = "지원 완료",
                    value = "${stats.appliedCount}건",
                    icon = Icons.Default.CheckCircle,
                    tint = Color(0xFF2E7D32)
                )
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PlatformFilterRow(
    selectedPlatform: String,
    onPlatformSelect: (String) -> Unit
) {
    val platforms = listOf("전체", "사람인", "잡코리아", "원티드", "로켓펀치", "블라인드하이어")
    
    Column {
        Text(
            text = "포털 필터링",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(platforms) { platform ->
                val isSelected = selectedPlatform == platform
                
                FilterChip(
                    selected = isSelected,
                    onClick = { onPlatformSelect(platform) },
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (platform != "전체") {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(PortalThemes.getPlatformColor(platform))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(platform, fontSize = 13.sp, fontWeight = FontWeight.Medium) 
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = Color.Transparent,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = Color.Transparent,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 0.dp
                    ),
                    modifier = Modifier.testTag("platform_chip_${platform}")
                )
            }
        }
    }
}

@Composable
fun AuxiliaryFilterSection(
    selectedExperience: String,
    onExperienceSelect: (String) -> Unit,
    favoritesOnly: Boolean,
    onFavoritesToggle: () -> Unit,
    showDuplicates: Boolean,
    onDuplicatesToggle: () -> Unit,
    showDImminentOnly: Boolean,
    onDImminentToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Experience Filter Pills
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("전체", "신입", "경력").forEach { exp ->
                val active = selectedExperience == exp
                FilterChip(
                    selected = active,
                    onClick = { onExperienceSelect(exp) },
                    label = { Text(exp, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = Color.Transparent,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = active,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = Color.Transparent,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 0.dp
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // D-day imminent toggle
            FilterChip(
                selected = showDImminentOnly,
                onClick = onDImminentToggle,
                label = { Text("마감 임박", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFDEDEC),
                    selectedLabelColor = Color(0xFFE74C3C),
                    selectedLeadingIconColor = Color(0xFFE74C3C),
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = showDImminentOnly,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = Color.Transparent,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 0.dp
                ),
                modifier = Modifier.height(28.dp).testTag("imminent_deadline_toggle")
            )

            // Duplicates toggle
            FilterChip(
                selected = showDuplicates,
                onClick = onDuplicatesToggle,
                label = { Text("중복 노출", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                leadingIcon = {
                    Icon(
                        imageVector = if (showDuplicates) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = showDuplicates,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = Color.Transparent,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 0.dp
                ),
                modifier = Modifier.height(28.dp)
            )

            // Bookmarks / Favorites Toggle Button
            FilterChip(
                selected = favoritesOnly,
                onClick = onFavoritesToggle,
                label = { Text("북마크", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                leadingIcon = {
                    Icon(
                        imageVector = if (favoritesOnly) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = favoritesOnly,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = Color.Transparent,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 0.dp
                ),
                modifier = Modifier.height(28.dp)
            )
        }
    }
}

fun calculateDDay(deadlineStr: String): Int? {
    if (deadlineStr == "상시채용" || deadlineStr.isBlank()) return null
    return try {
        val dateClean = deadlineStr.replace(".", "-").replace("/", "-").trim()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.KOREAN)
        sdf.isLenient = false
        val parsedDate = sdf.parse(dateClean) ?: return null
        
        val calDate = java.util.Calendar.getInstance().apply {
            time = parsedDate
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val calToday = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        
        val diffMs = calDate.timeInMillis - calToday.timeInMillis
        val diffDays = diffMs / (1000 * 60 * 60 * 24)
        diffDays.toInt()
    } catch (e: Exception) {
        null
    }
}

@Composable
fun DDayBadge(deadline: String) {
    val dday = remember(deadline) { calculateDDay(deadline) }
    
    val (text, bgColor, textColor) = when {
        dday == null -> Triple("상시채용", Color(0xFFF2F4F7), Color(0xFF5F6368))
        dday < 0 -> Triple("마감완료", Color(0xFFE2E3E5), Color(0xFF6C757D))
        dday == 0 -> Triple("오늘마감", Color(0xFFFDE8E8), Color(0xFFE74C3C))
        dday <= 3 -> Triple("마감임박 D-$dday", Color(0xFFFFF0F0), Color(0xFFD32F2F))
        else -> Triple("D-$dday", Color(0xFFE8F8F5), Color(0xFF16A085))
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .testTag("dday_badge")
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun JobCard(
    posting: JobPosting,
    onCardClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    // Unique platform list from merged string
    val platformList = posting.platforms.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val isMerged = platformList.size > 1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("job_card_${posting.id}")
            .graphicsLayer(alpha = if (posting.isRead) 0.6f else 1.0f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isMerged) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // First Row: Merged state badge, Experience, Bookmark star
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Platform Badges Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    platformList.forEach { platform ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PortalThemes.getPlatformColor(platform).copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = platform,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PortalThemes.getPlatformColor(platform)
                            )
                        }
                    }

                    if (isMerged) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MergeType,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "중복 제거됨",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    DDayBadge(deadline = posting.deadline)
                }

                // Favorite star
                Icon(
                    imageVector = if (posting.isFavorite) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "즐겨찾기",
                    tint = if (posting.isFavorite) PortalThemes.JobKorea else MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onFavoriteToggle() }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Second Row: Company Name in primary violet color
            Text(
                text = posting.company,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Third Row: Job Title
            Text(
                text = posting.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Fourth Row: Properties and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Properties (Experience, Location) with dot separator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = posting.location.split(" ").firstOrNull() ?: posting.location,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    )
                    Text(
                        text = posting.experience,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    )
                    Text(
                        text = posting.salary,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Application CRM Status Badge
                val statusText = posting.applicationStatus
                val statusColor = when (statusText) {
                    "관심" -> Color(0xFF6750A4)
                    "작성예정" -> Color(0xFF9E00FF)
                    "작성중" -> Color(0xFFF57C00)
                    "제출완료" -> Color(0xFF1976D2)
                    "서류합격" -> Color(0xFF00C362)
                    "면접예정" -> Color(0xFF00C7AE)
                    "면접완료" -> Color(0xFF388E3C)
                    "불합격" -> Color(0xFFD32F2F)
                    "보류" -> Color(0xFF7F8C8D)
                    else -> Color(0xFF6750A4)
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("status_chip_${posting.id}")
                ) {
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            // Fifth Row: Tech Stack Tags
            if (posting.techStack.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    posting.techStack.split(",").take(4).forEach { tech ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = tech.trim(),
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PropertyBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ScraperNotification(
    state: ScraperUiState,
    onDismiss: () -> Unit
) {
    val containerColor = when (state) {
        is ScraperUiState.Loading -> MaterialTheme.colorScheme.secondaryContainer
        is ScraperUiState.Success -> Color(0xFFE8F5E9)
        is ScraperUiState.Error -> Color(0xFFFFEBEE)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scraper_notification_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                when (state) {
                    is ScraperUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "포털들에 실시간 수집을 요청하고 중복을 제거하는 중...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    is ScraperUiState.Success -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "수집 완료! ${state.count}개의 새로운 공고가 동기화 되었습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                    }
                    is ScraperUiState.Error -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB71C1C)
                        )
                    }
                    else -> {}
                }
            }

            if (state !is ScraperUiState.Loading) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// DETAILS SHEET COMPOSABLE (Unified Details Viewer and Job CRM tracker)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JobDetailsSheet(
    posting: JobPosting,
    onDismiss: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onStatusChange: (String) -> Unit,
    onMemoSave: (String) -> Unit,
    viewModel: JobViewModel
) {
    var memoText by remember(posting.id) { mutableStateOf(posting.memo) }
    var memoExperience by remember(posting.id) { mutableStateOf(posting.memoExperience) }
    var memoCompetency by remember(posting.id) { mutableStateOf(posting.memoCompetency) }
    var memoQuestions by remember(posting.id) { mutableStateOf(posting.memoQuestions) }
    var memoExpectedInterviews by remember(posting.id) { mutableStateOf(posting.memoExpectedInterviews) }
    
    var showStatusDropdown by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("job_details_sheet"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Sheet Header: Back arrow & Action bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onDismiss() }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기",
                        modifier = Modifier.testTag("details_back_button")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("공고 목록으로 돌아가기", fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onFavoriteToggle) {
                        Icon(
                            imageVector = if (posting.isFavorite) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "북마크",
                            tint = if (posting.isFavorite) PortalThemes.JobKorea else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(posting.url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Suppress
                        }
                    }, modifier = Modifier.testTag("apply_link_button")) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "지원 사이트 열기",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Body Content: Scrollable information
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Job Header Title & Company
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Sources
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                posting.platforms.split(",").forEach { p ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(PortalThemes.getPlatformColor(p).copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = p.trim(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PortalThemes.getPlatformColor(p)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = posting.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = posting.company,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Grid properties: Requirements
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "요약 정보",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            DetailSpecItem(Icons.Default.CardTravel, "경력 조건", posting.experience, Modifier.weight(1f))
                            DetailSpecItem(Icons.Default.Place, "근무지", posting.location, Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            DetailSpecItem(Icons.Default.Payments, "급여 처우", posting.salary, Modifier.weight(1f))
                            DetailSpecItem(Icons.Default.CalendarToday, "등록일", posting.postedDate, Modifier.weight(1f))
                        }
                    }
                }

                // Tech Stack tags
                if (posting.techStack.isNotBlank()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                            Text(
                                text = "핵심 역량 / 기술스택",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                posting.techStack.split(",").forEach { tech ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = tech.trim(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Detail Description (Parser simulator for simple markdown sections)
                item {
                    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                        Text(
                            text = "상세 업무 요강",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                val paragraphs = posting.description.split("\n")
                                paragraphs.forEach { paragraph ->
                                    if (paragraph.startsWith("###")) {
                                        Text(
                                            text = paragraph.replace("###", "").trim(),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                        )
                                    } else if (paragraph.startsWith("-")) {
                                        Row(
                                            modifier = Modifier.padding(start = 8.dp, bottom = 3.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text("• ", fontWeight = FontWeight.Bold)
                                            Text(
                                                text = paragraph.replaceFirst("-", "").trim(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    } else if (paragraph.isNotBlank()) {
                                        Text(
                                            text = paragraph,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // AI Helper Note banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "본 상세 요요강은 포털들로부터 통합 및 수집된 통합 버전입니다. 링크를 통해 해당 사이트 공고 원본을 보고 다이렉트로 지원할 수 있습니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // CRM tracker components: Status and Private Notes
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "나의 지원 현황",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            // CRM status change button
                            Box {
                                val currentStatusLabel = when (posting.applicationStatus) {
                                    "관심" -> "관심"
                                    "작성예정" -> "작성예정"
                                    "작성중" -> "작성중"
                                    "제출완료" -> "제출완료"
                                    "서류합격" -> "서류합격"
                                    "면접예정" -> "면접예정"
                                    "면접완료" -> "면접완료"
                                    "불합격" -> "불합격"
                                    "보류" -> "보류"
                                    else -> "지정 안 함 (관심)"
                                }
                                Button(
                                    onClick = { showStatusDropdown = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("crm_status_dropdown")
                                ) {
                                    Text(currentStatusLabel, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                                }

                                DropdownMenu(
                                    expanded = showStatusDropdown,
                                    onDismissRequest = { showStatusDropdown = false }
                                ) {
                                    listOf(
                                        "관심" to "관심",
                                        "작성예정" to "작성예정",
                                        "작성중" to "작성중",
                                        "제출완료" to "제출완료",
                                        "서류합격" to "서류합격",
                                        "면접예정" to "면접예정",
                                        "면접완료" to "면접완료",
                                        "불합격" to "불합격",
                                        "보류" to "보류"
                                    ).forEach { (status, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                onStatusChange(status)
                                                showStatusDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Personal Memo / Tracker Notes
                        OutlinedTextField(
                            value = memoText,
                            onValueChange = {
                                memoText = it
                                onMemoSave(it) // Save in flow dynamically
                            },
                            label = { Text("기본 개인 메모 (면접일정, 연락내역 등)") },
                            placeholder = { Text("자유롭게 회사 관련 특이사항이나 주요일정을 기록해두세요.") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .testTag("posting_memo_field"),
                            maxLines = 4,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // COVER LETTER & INTERVIEW COMPREHENSIVE NOTES
                        Text(
                            text = "공고 맞춤형 자소서 및 면접 노트",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = memoExperience,
                                    onValueChange = {
                                        memoExperience = it
                                        viewModel.updateDetailedMemo(posting, it, memoCompetency, memoQuestions, memoExpectedInterviews)
                                    },
                                    label = { Text("1. 이 공고에 쓸 나의 경험") },
                                    placeholder = { Text("인턴, 설계 프로젝트, 동아리 등 이 직무에 어필할 수 있는 핵심 경험...") },
                                    modifier = Modifier.fillMaxWidth().testTag("memo_experience_input"),
                                    minLines = 2,
                                    maxLines = 4
                                )

                                OutlinedTextField(
                                    value = memoCompetency,
                                    onValueChange = {
                                        memoCompetency = it
                                        viewModel.updateDetailedMemo(posting, memoExperience, it, memoQuestions, memoExpectedInterviews)
                                    },
                                    label = { Text("2. 강조할 나의 직무 역량") },
                                    placeholder = { Text("공정제어, 품질관리 등 이 공고에서 특히 강조하고 싶은 요소를 써보세요.") },
                                    modifier = Modifier.fillMaxWidth().testTag("memo_competency_input"),
                                    minLines = 2,
                                    maxLines = 4
                                )

                                OutlinedTextField(
                                    value = memoQuestions,
                                    onValueChange = {
                                        memoQuestions = it
                                        viewModel.updateDetailedMemo(posting, memoExperience, memoCompetency, it, memoExpectedInterviews)
                                    },
                                    label = { Text("3. 자기소개서 문항 및 작성 메모") },
                                    placeholder = { Text("지원동기, 성장과정 등 각 문항별 구성 전략 및 핵심 초안...") },
                                    modifier = Modifier.fillMaxWidth().testTag("memo_questions_input"),
                                    minLines = 2,
                                    maxLines = 4
                                )

                                OutlinedTextField(
                                    value = memoExpectedInterviews,
                                    onValueChange = {
                                        memoExpectedInterviews = it
                                        viewModel.updateDetailedMemo(posting, memoExperience, memoCompetency, memoQuestions, it)
                                    },
                                    label = { Text("4. 예상 면접 질문 및 대응 답변") },
                                    placeholder = { Text("이 공고의 직무 기술서나 나의 이력을 바탕으로 예상되는 날카로운 질문...") },
                                    modifier = Modifier.fillMaxWidth().testTag("memo_interviews_input"),
                                    minLines = 2,
                                    maxLines = 4
                                )

                                Button(
                                    onClick = {
                                        viewModel.updateDetailedMemo(posting, memoExperience, memoCompetency, memoQuestions, memoExpectedInterviews)
                                        android.widget.Toast.makeText(context, "공고 맞춤 메모가 기기에 안전하게 보존되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("save_detailed_memo_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("자소서 및 면접 메모 저장", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Direct Link button bottom
            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(posting.url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Suppress
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_bottom_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Launch, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                val mainPortal = posting.originalPlatforms.split(",").firstOrNull() ?: "포털"
                Text("$mainPortal 공고 원본 확인 & 바로 입사지원")
            }
        }
    }
}

@Composable
fun DetailSpecItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = content, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UserProfileTab(viewModel: JobViewModel) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    
    var major by remember(profile.id) { mutableStateOf(profile.major) }
    var desiredJob by remember(profile.id) { mutableStateOf(profile.desiredJob) }
    var targetIndustry by remember(profile.id) { mutableStateOf(profile.targetIndustry) }
    var certifications by remember(profile.id) { mutableStateOf(profile.certifications) }
    var languages by remember(profile.id) { mutableStateOf(profile.languages) }
    var projects by remember(profile.id) { mutableStateOf(profile.projects) }
    var trainingCourses by remember(profile.id) { mutableStateOf(profile.trainingCourses) }
    var strengths by remember(profile.id) { mutableStateOf(profile.strengths) }
    var weaknesses by remember(profile.id) { mutableStateOf(profile.weaknesses) }

    var targetKeywordInput by remember { mutableStateOf("") }
    var excludeKeywordInput by remember { mutableStateOf("") }

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(
                text = "내 스펙 및 관심 설정",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "나의 취업 스펙을 기록하고 공고 매칭 키워드를 커스텀 설정합니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // SPECIFICATION CARD 1: Basic Info & Specs
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("학력 및 희망 직무", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                    
                    OutlinedTextField(
                        value = major,
                        onValueChange = { major = it },
                        label = { Text("전공") },
                        placeholder = { Text("예: 기계공학과 / 전자공학") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_major_input"),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = desiredJob,
                        onValueChange = { desiredJob = it },
                        label = { Text("희망직무") },
                        placeholder = { Text("예: 생산관리, 설비보전") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_desired_job_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = targetIndustry,
                        onValueChange = { targetIndustry = it },
                        label = { Text("관심산업") },
                        placeholder = { Text("예: 자동차, 반도체 제조") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_industry_input"),
                        singleLine = true
                    )
                }
            }
        }

        // SPECIFICATION CARD 2: Certs & Languages
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CardMembership, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("자격증 및 어학 정보", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))

                    OutlinedTextField(
                        value = certifications,
                        onValueChange = { certifications = it },
                        label = { Text("자격증") },
                        placeholder = { Text("예: 전기기사, 일반기계기사") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_certs_input"),
                        maxLines = 2
                    )

                    OutlinedTextField(
                        value = languages,
                        onValueChange = { languages = it },
                        label = { Text("어학 능력") },
                        placeholder = { Text("예: OPic IM3, TOEIC 820점") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_languages_input"),
                        singleLine = true
                    )
                }
            }
        }

        // SPECIFICATION CARD 3: Experience details
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("경험 및 교육 수료", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))

                    OutlinedTextField(
                        value = projects,
                        onValueChange = { projects = it },
                        label = { Text("프로젝트 경험") },
                        placeholder = { Text("공모전, 전공 종합설계 프로젝트 등을 서술하세요.") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_projects_input"),
                        minLines = 2,
                        maxLines = 4
                    )

                    OutlinedTextField(
                        value = trainingCourses,
                        onValueChange = { trainingCourses = it },
                        label = { Text("교육 수료 내역") },
                        placeholder = { Text("예: 반도체 공정 실습 40시간 수료") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_training_input"),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }
        }

        // SPECIFICATION CARD 4: Strengths/Weaknesses
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("나의 강점 및 보완점", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))

                    OutlinedTextField(
                        value = strengths,
                        onValueChange = { strengths = it },
                        label = { Text("스스로 꼽는 강점 (역량)") },
                        placeholder = { Text("예: 신속한 문제 인지와 협업적 의사소통 능력") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_strengths_input"),
                        minLines = 2
                    )

                    OutlinedTextField(
                        value = weaknesses,
                        onValueChange = { weaknesses = it },
                        label = { Text("치유해야 할 약점 (보완할 점)") },
                        placeholder = { Text("예: 과제에 집중해 세부 조율을 지나치게 고민함") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_weaknesses_input"),
                        minLines = 2
                    )
                }
            }
        }

        // KEYWORD MANAGEMENT SECTION: Target Interest Keywords
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFF6750A4))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("수집 관심 키워드", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("이 키워드들은 수집된 목록 내 필터링이나 즉시 AI 검색용 단어로 사용됩니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = targetKeywordInput,
                            onValueChange = { targetKeywordInput = it },
                            label = { Text("새 관심 키워드") },
                            placeholder = { Text("예: 전기기사") },
                            modifier = Modifier.weight(1f).testTag("target_keyword_field"),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (targetKeywordInput.isNotBlank()) {
                                    viewModel.addTargetKeyword(targetKeywordInput)
                                    targetKeywordInput = ""
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(56.dp).testTag("add_target_keyword_button")
                        ) {
                            Text("추가")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val targets = profile.targetKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        targets.forEach { keyword ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(keyword, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "삭제",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable { viewModel.removeTargetKeyword(keyword) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // KEYWORD MANAGEMENT SECTION: Exclude Keywords
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFD32F2F))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("수집 제외 키워드", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("이 단어들이 포함된 채용공고는 피드에서 자동으로 제외 처리됩니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = excludeKeywordInput,
                            onValueChange = { excludeKeywordInput = it },
                            label = { Text("새 제외 키워드") },
                            placeholder = { Text("예: 영업직") },
                            modifier = Modifier.weight(1f).testTag("exclude_keyword_field"),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (excludeKeywordInput.isNotBlank()) {
                                    viewModel.addExcludeKeyword(excludeKeywordInput)
                                    excludeKeywordInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(56.dp).testTag("add_exclude_keyword_button")
                        ) {
                            Text("추가")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val excludes = profile.excludeKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        excludes.forEach { keyword ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .clickable { }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(keyword, fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "삭제",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable { viewModel.removeExcludeKeyword(keyword) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ACTION SAVE USER PROFILE BUTTON
        item {
            Button(
                onClick = {
                    val updated = profile.copy(
                        major = major,
                        desiredJob = desiredJob,
                        targetIndustry = targetIndustry,
                        certifications = certifications,
                        languages = languages,
                        projects = projects,
                        trainingCourses = trainingCourses,
                        strengths = strengths,
                        weaknesses = weaknesses
                    )
                    viewModel.saveUserProfile(updated)
                    android.widget.Toast.makeText(context, "내 스펙 프로필 정보가 저장되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_profile_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("프로필 정보 전체 저장")
            }
        }
    }
}
