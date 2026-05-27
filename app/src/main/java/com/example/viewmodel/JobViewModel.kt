package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.JobPosting
import com.example.data.JobRepository
import com.example.data.PlatformConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.data.UserProfile
import kotlinx.coroutines.flow.map

sealed interface ScraperUiState {
    object Idle : ScraperUiState
    object Loading : ScraperUiState
    data class Success(val count: Int) : ScraperUiState
    data class Error(val message: String) : ScraperUiState
}

class JobViewModel(private val repository: JobRepository) : ViewModel() {

    // Filter states
    val selectedPlatform = MutableStateFlow("전체")
    val selectedExperience = MutableStateFlow("전체")
    val feedFilterQuery = MutableStateFlow("")
    val isOnlyFavorites = MutableStateFlow(false)
    val showDuplicates = MutableStateFlow(false) // Toggle to see raw duplicate entries if wanted
    val selectedStatusFilter = MutableStateFlow("전체")
    val showDImminentOnly = MutableStateFlow(false)

    // UI State for background AI Scraping/gathering
    private val _scraperState = MutableStateFlow<ScraperUiState>(ScraperUiState.Idle)
    val scraperState: StateFlow<ScraperUiState> = _scraperState

    // Current detail posting selection
    private val _selectedPosting = MutableStateFlow<JobPosting?>(null)
    val selectedPosting: StateFlow<JobPosting?> = _selectedPosting

    init {
        // Automatically seed sample data if empty so the user doesn't see a blank slate on first install
        viewModelScope.launch {
            repository.seedInitialJobsIfEmpty()
        }
    }

    // Observed collections
    private val _rawDbPostings = repository.rawPostings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val rawDbPostings: StateFlow<List<JobPosting>> = _rawDbPostings

    private val _deduplicatedDbPostings = repository.deduplicatedPostings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Platform configurations flow
    val platformConfigs: StateFlow<List<PlatformConfig>> = repository.platformConfigs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Reactive user profile flow
    val userProfile: StateFlow<UserProfile> = repository.userProfile.map {
        it ?: UserProfile()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserProfile()
    )

    fun calculateDDay(deadlineStr: String): Int? {
        if (deadlineStr == "상시채용" || deadlineStr.isBlank()) return null
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.KOREAN)
            sdf.isLenient = false
            val parsedDate = sdf.parse(deadlineStr.trim()) ?: return null
            
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

    // Final filtered postings flow
    val uiPostings: StateFlow<List<JobPosting>> = combine(
        _rawDbPostings,
        _deduplicatedDbPostings,
        selectedPlatform,
        selectedExperience,
        feedFilterQuery,
        isOnlyFavorites,
        showDuplicates,
        platformConfigs,
        selectedStatusFilter,
        showDImminentOnly,
        userProfile
    ) { flows: Array<Any?> ->
        val raw = flows[0] as List<JobPosting>
        val deduped = flows[1] as List<JobPosting>
        val platform = flows[2] as String
        val experience = flows[3] as String
        val query = flows[4] as String
        val favoritesOnly = flows[5] as Boolean
        val showDups = flows[6] as Boolean
        val configs = flows[7] as List<PlatformConfig>
        val statusFilter = flows[8] as String
        val imminentOnly = flows[9] as Boolean
        val profile = flows[10] as UserProfile
        
        val activePlatformNames = configs.filter { it.isActive }.map { it.name }.toSet()
        val sourceList = if (showDups) raw else deduped
        
        val excludeList = profile.excludeKeywords.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.lowercase() }

        sourceList.filter { posting ->
            // 0. Platform Active Status match (Hides postings belonging entirely to inactive platforms)
            val originalPlatformList = posting.originalPlatforms.split(",").map { it.trim() }
            val isPlatformActive = originalPlatformList.any { op ->
                activePlatformNames.contains(op) || op == "포털"
            }
            if (!isPlatformActive) return@filter false

            // 0.5 Exclusion Keywords filter
            if (excludeList.isNotEmpty()) {
                val matchedExclude = excludeList.any { kw ->
                    posting.title.lowercase().contains(kw) ||
                    posting.description.lowercase().contains(kw) ||
                    posting.experience.lowercase().contains(kw) ||
                    posting.techStack.lowercase().contains(kw)
                }
                if (matchedExclude) return@filter false
            }

            // 0.6 D-Day Imminent filter (D-3 이내, i.e., D-Day is 0, 1, 2, or 3)
            if (imminentOnly) {
                val dday = calculateDDay(posting.deadline)
                if (dday == null || dday < 0 || dday > 3) {
                    return@filter false
                }
            }

            // 0.7 Status Filter
            if (statusFilter != "전체") {
                if (posting.applicationStatus != statusFilter) {
                    return@filter false
                }
            }

            // 1. Favorites match
            val matchesFavorite = !favoritesOnly || posting.isFavorite

            // 2. Platform match (from bottom filtering pills)
            val matchesPlatform = platform == "전체" || 
                    posting.platforms.contains(platform) || 
                    posting.originalPlatforms.contains(platform)

            // 3. Experience match
            val matchesExperience = when (experience) {
                "전체" -> true
                "신입" -> posting.experience.contains("신입") || posting.experience.contains("무관")
                "경력" -> posting.experience.contains("경력")
                else -> true
            }

            // 4. Keyword text search match (Title, Company, TechStack, Location)
            val matchesQuery = query.isBlank() || 
                    posting.title.contains(query, ignoreCase = true) ||
                    posting.company.contains(query, ignoreCase = true) ||
                    posting.techStack.contains(query, ignoreCase = true) ||
                    posting.location.contains(query, ignoreCase = true)

            matchesFavorite && matchesPlatform && matchesExperience && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Deduplication Statistics
    val stats: StateFlow<JobStats> = _rawDbPostings.combine(_deduplicatedDbPostings) { raw, deduped ->
        val totalCount = raw.size
        val uniqueCount = deduped.size
        val duplicateCount = totalCount - uniqueCount
        JobStats(
            totalCollected = totalCount,
            uniqueCleaned = uniqueCount,
            duplicatesRemoved = if (duplicateCount < 0) 0 else duplicateCount,
            appliedCount = raw.count { it.applicationStatus == "APPLIED" },
            interviewCount = raw.count { it.applicationStatus == "INTERVIEWING" }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = JobStats()
    )

    /**
     * Start the AI aggregator scraper for a specific job-hunting keyword
     */
    fun performAiScraping(keyword: String) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            _scraperState.value = ScraperUiState.Loading
            try {
                // Keep track of total listings count before scrape to show differences
                val previousCount = repository.getDatabaseCount()
                repository.runAiScraper(keyword)
                val currentCount = repository.getDatabaseCount()
                val added = currentCount - previousCount
                _scraperState.value = ScraperUiState.Success(added)
            } catch (e: Exception) {
                _scraperState.value = ScraperUiState.Error(e.localizedMessage ?: "수집 도중 오류가 발생했습니다.")
            }
        }
    }

    fun resetScraperState() {
        _scraperState.value = ScraperUiState.Idle
    }

    // Toggle job details page
    fun selectPosting(posting: JobPosting?) {
        _selectedPosting.value = posting
        if (posting != null && !posting.isRead) {
            markPostingAsRead(posting)
        }
    }

    // Mark favorite action
    fun toggleFavorite(posting: JobPosting) {
        viewModelScope.launch {
            val updated = posting.copy(isFavorite = !posting.isFavorite)
            repository.updatePosting(updated)
            // If currently open details is this posting, update it
            if (_selectedPosting.value?.id == posting.id) {
                _selectedPosting.value = updated
            }
        }
    }

    // Mark read action
    private fun markPostingAsRead(posting: JobPosting) {
        viewModelScope.launch {
            val updated = posting.copy(isRead = true)
            repository.updatePosting(updated)
        }
    }

    // Save tracking status
    fun updateApplicationStatus(posting: JobPosting, status: String) {
        viewModelScope.launch {
            val updated = posting.copy(applicationStatus = status)
            repository.updatePosting(updated)
            if (_selectedPosting.value?.id == posting.id) {
                _selectedPosting.value = updated
            }
        }
    }

    // Save personal memo
    fun updateMemo(posting: JobPosting, memoText: String) {
        viewModelScope.launch {
            val updated = posting.copy(memo = memoText)
            repository.updatePosting(updated)
            if (_selectedPosting.value?.id == posting.id) {
                _selectedPosting.value = updated
            }
        }
    }

    // Save self-introduction/resume detailed memos
    fun updateDetailedMemo(
        posting: JobPosting,
        experience: String,
        competency: String,
        questions: String,
        expectedInterviews: String
    ) {
        viewModelScope.launch {
            val updated = posting.copy(
                memoExperience = experience,
                memoCompetency = competency,
                memoQuestions = questions,
                memoExpectedInterviews = expectedInterviews
            )
            repository.updatePosting(updated)
            if (_selectedPosting.value?.id == posting.id) {
                _selectedPosting.value = updated
            }
        }
    }

    // Save user specifications profile
    fun saveUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveUserProfile(profile)
        }
    }

    // Keyword management
    fun addTargetKeyword(keyword: String) {
        if (keyword.isBlank()) return
        val current = userProfile.value
        val list = current.targetKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        val trimmed = keyword.trim()
        if (!list.contains(trimmed)) {
            list.add(trimmed)
            saveUserProfile(current.copy(targetKeywords = list.joinToString(", ")))
        }
    }

    fun removeTargetKeyword(keyword: String) {
        val current = userProfile.value
        val list = current.targetKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        val trimmed = keyword.trim()
        if (list.remove(trimmed)) {
            saveUserProfile(current.copy(targetKeywords = list.joinToString(", ")))
        }
    }

    fun addExcludeKeyword(keyword: String) {
        if (keyword.isBlank()) return
        val current = userProfile.value
        val list = current.excludeKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        val trimmed = keyword.trim()
        if (!list.contains(trimmed)) {
            list.add(trimmed)
            saveUserProfile(current.copy(excludeKeywords = list.joinToString(", ")))
        }
    }

    fun removeExcludeKeyword(keyword: String) {
        val current = userProfile.value
        val list = current.excludeKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        val trimmed = keyword.trim()
        if (list.remove(trimmed)) {
            saveUserProfile(current.copy(excludeKeywords = list.joinToString(", ")))
        }
    }

    // Reset database to seed data
    fun resetDatabase() {
        viewModelScope.launch {
            repository.clearAll()
            repository.seedInitialJobsIfEmpty()
        }
    }

    // Toggle active state of platform configuration
    fun togglePlatformConfig(config: PlatformConfig) {
        viewModelScope.launch {
            repository.updatePlatformConfig(config.copy(isActive = !config.isActive))
        }
    }
}

data class JobStats(
    val totalCollected: Int = 0,
    val uniqueCleaned: Int = 0,
    val duplicatesRemoved: Int = 0,
    val appliedCount: Int = 0,
    val interviewCount: Int = 0
)

class JobViewModelFactory(private val repository: JobRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JobViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JobViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
