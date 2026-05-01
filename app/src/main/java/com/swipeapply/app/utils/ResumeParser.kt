package com.swipeapply.app.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.swipeapply.app.data.ai.GroqApiClient
import com.swipeapply.app.data.model.*
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object ResumeParser {
    private const val TAG = "ResumeParser"

    private val jsonParser = Json { 
        ignoreUnknownKeys = true 
        isLenient = true 
        coerceInputValues = true
    }

    /**
     * Parses a resume from a PDF file using Groq AI via GroqApiClient.
     * Includes automatic retry, rate-limit, and timeout handling.
     * Falls back to ResumeParserV2 (regex) if Groq fails.
     */
    suspend fun parseResume(context: Context, uri: Uri): UserProfile? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Extracting text from PDF...")
            val pdfText = extractTextFromPdf(context, uri)
            if (pdfText.isBlank()) {
                Log.w(TAG, "PDF file is empty or unreadable")
                return@withContext null
            }
            
            Log.d(TAG, "Extracted ${pdfText.length} characters from PDF")

            Log.d(TAG, "Sending resume to Groq for analysis...")
            val prompt = buildResumeParsePrompt(pdfText)
            
            val result = GroqApiClient.complete(
                prompt = prompt,
                temperature = 0.3,
                maxTokens = 4096
            )

            when (result) {
                is GroqApiClient.GroqResult.Success -> {
                    val cleanedJson = cleanJsonResponse(result.content)
                    Log.d(TAG, "Attempting to parse JSON...")
                    val profile = jsonParser.decodeFromString<UserProfile>(cleanedJson)
                    Log.d(TAG, "Successfully parsed resume for: ${profile.fullName}")
                    profile
                }
                is GroqApiClient.GroqResult.Error -> {
                    Log.e(TAG, "Groq failed: ${result.message}, falling back to ResumeParserV2")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing resume: ${e.message}", e)
            null
        }
    }

    /**
     * Builds the prompt for the AI to parse resume information.
     */
    private fun buildResumeParsePrompt(resumeText: String): String {
        return """
            You are a resume parser. Extract the following information from the resume text below and return ONLY a valid JSON object.
            Do not include markdown formatting (like ```json or ```).
            Do not include any explanation or thinking - ONLY output the JSON.
            If information is not found, use empty strings for text fields and empty arrays for list fields.
            
            Structure required (return valid JSON only):
            {
              "fullName": "string",
              "email": "string",
              "phone": "string",
              "bio": "short professional summary string",
              "skills": ["skill1", "skill2"],
              "techStack": ["tech1", "tech2"],
              "education": [{"school": "string", "degree": "string", "year": "string"}],
              "experience": [{"company": "string", "role": "string", "duration": "string", "description": "string"}],
              "projects": [{"name": "string", "description": "string", "techUsed": "string"}]
            }

            Resume Text:
            $resumeText
        """.trimIndent()
    }

    /**
     * Cleans AI response by removing markdown code blocks, thinking tags, and extra whitespace.
     */
    private fun cleanJsonResponse(response: String): String {
        var cleaned = response
            .replace(Regex("```json\\s*"), "")  // Remove ```json
            .replace(Regex("```\\s*"), "")       // Remove ```
            .replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "") // Remove <think> blocks
            .trim()
        
        // Find the JSON object boundaries
        val jsonStart = cleaned.indexOf('{')
        val jsonEnd = cleaned.lastIndexOf('}')
        
        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            cleaned = cleaned.substring(jsonStart, jsonEnd + 1)
        }
        
        return cleaned
    }

    /**
     * Extracts text from a PDF file using PDFBox.
     */
    private fun extractTextFromPdf(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                
                // Extract text page by page with logging
                val text = stripper.getText(document)
                Log.d(TAG, "PDF loaded successfully. Pages: ${document.numberOfPages}")
                
                document.close()
                text
            } ?: run {
                Log.e(TAG, "Could not open input stream for URI: $uri")
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "PDF read error: ${e.message}", e)
            ""
        }
    }
}
