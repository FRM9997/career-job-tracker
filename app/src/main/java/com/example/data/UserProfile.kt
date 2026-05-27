package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val major: String = "",
    val desiredJob: String = "",
    val targetIndustry: String = "",
    val certifications: String = "",
    val languages: String = "",
    val projects: String = "",
    val trainingCourses: String = "",
    val strengths: String = "",
    val weaknesses: String = "",
    val targetKeywords: String = "생산기술, 설비기술, 공정기술, 제조기술, 생산관리, 자동차, 반도체, 기계, 장비",
    val excludeKeywords: String = "경력 5년, 전기기사 필수, 영업직, 계약직"
)
