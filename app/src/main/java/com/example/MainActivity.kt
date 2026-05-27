package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.JobRepository
import com.example.ui.JobDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.JobViewModel
import com.example.viewmodel.JobViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // 1. Initialize Room Database and Repository
    val database = AppDatabase.getDatabase(applicationContext)
    val repository = JobRepository(database.jobDao())

    // 2. Initialize ViewModel via factory
    val factory = JobViewModelFactory(repository)
    val viewModel = ViewModelProvider(this, factory)[JobViewModel::class.java]

    setContent {
      MyApplicationTheme {
        JobDashboard(viewModel = viewModel)
      }
    }
  }
}
