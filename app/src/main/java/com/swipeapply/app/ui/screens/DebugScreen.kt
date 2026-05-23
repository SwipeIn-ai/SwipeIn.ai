package com.swipeapply.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swipeapply.app.data.api.FindWorkApiService
import com.swipeapply.app.data.config.ApiConfig
import com.swipeapply.app.data.mapper.toEntity
import com.swipeapply.app.data.mapper.toJobCard
import com.swipeapply.app.ui.theme.GradientStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(onBack: () -> Unit) {
    // Guard: never show debug info in release builds
    if (!com.swipeapply.app.BuildConfig.DEBUG) {
        onBack()
        return
    }
    var apiResponse by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                isLoading = true
                error = ""
                apiResponse = "Testing API and Data Mapping...\n\n"

                val logging = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }

                val client = OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(FindWorkApiService.BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val apiService = retrofit.create(FindWorkApiService::class.java)
                val authToken = FindWorkApiService.formatAuthToken(ApiConfig.FINDWORK_API_KEY)

                apiResponse += "Configuration\n"
                apiResponse += "API Key: ${ApiConfig.FINDWORK_API_KEY.take(15)}...\n"
                apiResponse += "Base URL: ${FindWorkApiService.BASE_URL}\n"
                apiResponse += "Search: ${ApiConfig.DEFAULT_SEARCH_QUERY ?: "ALL JOBS"}\n\n"

                apiResponse += "Step 1: API Request\n"

                val response = apiService.getJobs(
                    authToken = authToken,
                    search = ApiConfig.DEFAULT_SEARCH_QUERY,
                    location = null,
                    remote = null,
                    sortBy = "relevance"
                )

                apiResponse += "API Response Successful!\n"
                apiResponse += "Total Count: ${response.count}\n"
                apiResponse += "Results Returned: ${response.results.size}\n\n"

                apiResponse += "Step 2: Data Mapping\n"

                var successCount = 0
                var failCount = 0

                response.results.take(5).forEachIndexed { index, job ->
                    apiResponse += "\n[Job $index]\n"
                    apiResponse += "  ID: ${job.id}\n"
                    apiResponse += "  Company: ${job.companyName}\n"
                    apiResponse += "  Role: ${job.role.take(40)}...\n"
                    apiResponse += "  Location: ${job.location ?: "null -> Remote"}\n"
                    apiResponse += "  Remote: ${job.remote}\n"

                    try {
                        apiResponse += "  Attempting to map...\n"
                        val entity = job.toEntity()
                        apiResponse += "    Entity created (ID: ${entity.id})\n"
                        val jobCard = entity.toJobCard()
                        apiResponse += "    JobCard created: ${jobCard.title}\n"
                        successCount++
                    } catch (e: Exception) {
                        apiResponse += "    MAPPING FAILED: ${e.message}\n"
                        apiResponse += "    Exception: ${e::class.simpleName}\n"
                        failCount++
                    }
                }

                apiResponse += "\nStep 3: Summary\n"
                apiResponse += "Mapped Successfully: $successCount\n"
                apiResponse += "Mapping Failed: $failCount\n"

                if (successCount == 0) {
                    apiResponse += "\nISSUE: All jobs failed to map!"
                    apiResponse += "\nThis is why you see 'no jobs found' error."
                } else {
                    apiResponse += "\nJobs should now display on HomeScreen!"
                }

                isLoading = false
            } catch (e: Exception) {
                error = "ERROR: ${e.message}\n\n${e.stackTraceToString()}"
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "API & Mapping Debug",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = GradientStart)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Testing API and Mapping...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (error.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "API Test Failed",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Debug Report",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        apiResponse,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
