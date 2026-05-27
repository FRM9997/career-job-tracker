package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM job_postings ORDER BY timestamp DESC")
    fun getAllPostingsFlow(): Flow<List<JobPosting>>

    @Query("SELECT * FROM job_postings WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoritePostingsFlow(): Flow<List<JobPosting>>

    @Query("SELECT * FROM job_postings WHERE id = :id")
    suspend fun getPostingById(id: Int): JobPosting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosting(posting: JobPosting): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPostings(postings: List<JobPosting>)

    @Update
    suspend fun updatePosting(posting: JobPosting)

    @Delete
    suspend fun deletePosting(posting: JobPosting)

    @Query("DELETE FROM job_postings")
    suspend fun clearAllPostings()

    @Query("SELECT COUNT(*) FROM job_postings")
    suspend fun getCount(): Int

    @Query("SELECT * FROM platform_configs ORDER BY name ASC")
    fun getAllPlatformConfigsFlow(): Flow<List<PlatformConfig>>

    @Query("SELECT * FROM platform_configs")
    suspend fun getAllPlatformConfigs(): List<PlatformConfig>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlatformConfigs(configs: List<PlatformConfig>)

    @Update
    suspend fun updatePlatformConfig(config: PlatformConfig)

    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(userProfile: UserProfile)
}
