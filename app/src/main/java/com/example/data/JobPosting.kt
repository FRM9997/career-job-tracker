package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "job_postings")
data class JobPosting(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val company: String,
    val description: String,
    val platforms: String, // Comma-separated sources e.g. "사람인, 잡코리아"
    val originalPlatforms: String, // Original individual platform name e.g. "사람인"
    val url: String,
    val salary: String,
    val location: String,
    val experience: String, // e.g. "신입", "경력 2년", "경력무관"
    val techStack: String, // Comma-separated skills
    val postedDate: String, // Date string
    val deadline: String = "상시채용", // Deadline e.g. "2026-06-30", "상시채용"
    val isFavorite: Boolean = false,
    val isRead: Boolean = false,
    val isHidden: Boolean = false,
    val isDuplicate: Boolean = false,
    val duplicateGroupId: String? = null,
    val applicationStatus: String = "관심", // 관심, 작성예정, 작성중, 제출완료, 서류합격, 면접예정, 면접완료, 불합격, 보류
    val memo: String = "",
    val memoExperience: String = "",
    val memoCompetency: String = "",
    val memoQuestions: String = "",
    val memoExpectedInterviews: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
