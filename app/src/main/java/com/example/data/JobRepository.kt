package com.example.data

import com.example.network.GeminiJobFetcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.util.Log

class JobRepository(private val jobDao: JobDao) {

    companion object {
        private const val TAG = "JobRepository"
    }

    // List of postings from the database, reactively map and deduplicate on the fly or display
    val rawPostings: Flow<List<JobPosting>> = jobDao.getAllPostingsFlow()

    // Deduplicated postings for display
    val deduplicatedPostings: Flow<List<JobPosting>> = jobDao.getAllPostingsFlow().map { postings ->
        deduplicatePostings(postings)
    }

    val favoritePostings: Flow<List<JobPosting>> = jobDao.getFavoritePostingsFlow().map { postings ->
        // Favorites also sorted and unique
        deduplicatePostings(postings)
    }

    // Reactive platform configs flow
    val platformConfigs: Flow<List<PlatformConfig>> = jobDao.getAllPlatformConfigsFlow()

    // Reactive user profile flow
    val userProfile: Flow<UserProfile?> = jobDao.getUserProfileFlow()

    suspend fun getUserProfile(): UserProfile? {
        return jobDao.getUserProfile()
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        jobDao.insertUserProfile(profile)
    }

    suspend fun seedInitialUserProfileIfEmpty() {
        if (jobDao.getUserProfile() == null) {
            jobDao.insertUserProfile(UserProfile())
            Log.d(TAG, "User profile was empty, seeded default.")
        }
    }

    suspend fun getPostingById(id: Int): JobPosting? {
        return jobDao.getPostingById(id)
    }

    suspend fun insertPosting(posting: JobPosting): Long {
        return jobDao.insertPosting(posting)
    }

    suspend fun updatePosting(posting: JobPosting) {
        jobDao.updatePosting(posting)
    }

    suspend fun updatePlatformConfig(config: PlatformConfig) {
        jobDao.updatePlatformConfig(config)
    }

    suspend fun clearAll() {
        jobDao.clearAllPostings()
    }

    suspend fun getDatabaseCount(): Int {
        return jobDao.getCount()
    }

    /**
     * Seeds initial jobs and platform configurations if the database is empty
     */
    suspend fun seedInitialJobsIfEmpty() {
        seedInitialPlatformsIfEmpty()
        seedInitialUserProfileIfEmpty()
        if (jobDao.getCount() == 0) {
            val sampleJobs = GeminiJobFetcher.getLocalSampleSeedJobs()
            jobDao.insertPostings(sampleJobs)
            Log.d(TAG, "Database empty, seeded ${sampleJobs.size} postings.")
        }
    }

    /**
     * Seeds 10 job portal platform configurations if empty
     */
    suspend fun seedInitialPlatformsIfEmpty() {
        if (jobDao.getAllPlatformConfigs().isEmpty()) {
            val initial = listOf(
                PlatformConfig("사람인", true, "API", System.currentTimeMillis(), "0xFF0075FF"),
                PlatformConfig("잡코리아", true, "알림메일", System.currentTimeMillis(), "0xFFFF5000"),
                PlatformConfig("워크넷/고용24", true, "API", System.currentTimeMillis() - 3600000, "0xFF003FAD"),
                PlatformConfig("인크루트", true, "알림메일", System.currentTimeMillis() - 7200000, "0xFFFF9900"),
                PlatformConfig("원티드", true, "수동등록", System.currentTimeMillis() - 10800000, "0xFF3366FF"),
                PlatformConfig("링커리어", true, "수동등록", System.currentTimeMillis() - 14400000, "0xFF00C7AE"),
                PlatformConfig("자소설닷컴", true, "수동등록", System.currentTimeMillis() - 18000000, "0xFF9E00FF"),
                PlatformConfig("캐치", true, "크롤링 예정", System.currentTimeMillis() - 21600000, "0xFFE02241"),
                PlatformConfig("잡플래닛", true, "크롤링 예정", System.currentTimeMillis() - 25200000, "0xFF00C362"),
                PlatformConfig("리멤버 커리어", true, "크롤링 예정", System.currentTimeMillis() - 28800000, "0xFF1F2024")
            )
            jobDao.insertPlatformConfigs(initial)
            Log.d(TAG, "Platform configs empty, seeded 10 platforms.")
        }
    }

    /**
     * Triggers AI scraper to query new jobs online via Gemini API, and updates local database
     */
    suspend fun runAiScraper(keyword: String) {
        try {
            Log.d(TAG, "Running AI scraper for keyword: $keyword")
            val newJobs = GeminiJobFetcher.fetchJobsFromGemini(keyword)
            if (newJobs.isNotEmpty()) {
                jobDao.insertPostings(newJobs)
                Log.d(TAG, "AI Scraper imported ${newJobs.size} postings to DB.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing AI scraper: ${e.message}", e)
        }
    }

    /**
     * Deduplication Engine:
     * Groups postings by: normalized(Company Name) + normalized(Job Title) + normalized(Deadline) + normalized(Location).
     * Consolidates duplicate listings into a single representative item carrying multiple platforms
     * and flagging duplicate replicas.
     */
    private fun deduplicatePostings(postings: List<JobPosting>): List<JobPosting> {
        if (postings.isEmpty()) return emptyList()

        val grouped = postings.groupBy { posting ->
            val normCompany = normalizeCompany(posting.company)
            val normTitle = normalizeTitle(posting.title)
            val normDeadline = normalizeDeadline(posting.deadline)
            val normLocation = normalizeLocation(posting.location)
            "${normCompany}_${normTitle}_${normDeadline}_${normLocation}"
        }

        val resultList = mutableListOf<JobPosting>()

        grouped.forEach { (_, groupList) ->
            if (groupList.size == 1) {
                resultList.add(groupList[0])
            } else {
                // Consolidate duplicates: Choose the representative (parent)
                // Sort by richness (description length) and select the representative
                val sortedByRichness = groupList.sortedWith(
                    compareByDescending<JobPosting> { it.description.length }
                        .thenBy { it.id }
                )
                val representative = sortedByRichness.first()

                // Collect unique sources from all items in duplicate group
                val allPlatforms = groupList.map { it.originalPlatforms }
                    .flatMap { it.split(",").map { p -> p.trim() } }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .sorted()
                    .joinToString(", ")

                // Create merged copy
                val mergedPosting = representative.copy(
                    platforms = allPlatforms,
                    isDuplicate = false, // Representative is visible.
                    duplicateGroupId = "group_${representative.company.hashCode()}_${representative.title.hashCode()}"
                )
                resultList.add(mergedPosting)
            }
        }

        // Return sorted by date/timestamp descending
        return resultList.sortedByDescending { it.postedDate }
    }

    /** Helper to strip common Korean business terms and trailing whitespace */
    private fun normalizeCompany(name: String): String {
        return name.replace("(주)", "")
            .replace("주식회사", "")
            .replace("(유)", "")
            .replace("유한회사", "")
            .split("[")
            .first()
            .split("(")
            .first()
            .trim()
            .lowercase()
    }

    /** Helper to normalize title string to compare tokens */
    private fun normalizeTitle(title: String): String {
        // Remove braces and contents inside them (like [신입], (경력))
        val noBraces = title.replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\{.*?\\}"), "")
        
        // Lowercase and remove symbols/spaces
        return noBraces.replace(Regex("[^a-zA-Z0-9가-힣]"), "")
            .trim()
            .lowercase()
    }

    /** Helper to normalize deadline string to ignore whitespace/case */
    private fun normalizeDeadline(deadline: String): String {
        return deadline.replace(Regex("[^a-zA-Z0-9가-힣]"), "")
            .trim()
            .lowercase()
            .ifBlank { "상시채용" }
    }

    /** Helper to normalize location to first 2 segments (e.g. '서울 송파구' or '경기 성남시') */
    private fun normalizeLocation(location: String): String {
        val words = location.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val prefix = if (words.size >= 2) "${words[0]} ${words[1]}" else (words.firstOrNull() ?: "")
        return prefix.replace(Regex("[^a-zA-Z0-9가-힣]"), "")
            .trim()
            .lowercase()
    }
}
