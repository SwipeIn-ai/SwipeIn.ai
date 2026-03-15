package com.swipeapply.app.data.model

/**
 * Represents a cold referral email ready for sending.
 * Used by the ReferralEmailComposer screen.
 */
data class ReferralEmail(
    val fromName: String = "",
    val fromEmail: String = "",
    val toName: String = "",
    val toEmail: String = "",
    val subject: String = "",
    val body: String = "",
    val companyName: String = "",
    val jobTitle: String? = null
) {
    /**
     * Builds a mailto URI for opening in email apps.
     * Properly encodes subject and body for URI safety.
     */
    fun toMailtoUri(): String {
        val encodedSubject = java.net.URLEncoder.encode(subject, "UTF-8")
            .replace("+", "%20")
        val encodedBody = java.net.URLEncoder.encode(body, "UTF-8")
            .replace("+", "%20")
        return "mailto:$toEmail?subject=$encodedSubject&body=$encodedBody"
    }
    
    /**
     * Builds a Gmail-specific intent URI for better Gmail app handling.
     */
    fun toGmailIntent(): android.content.Intent {
        return android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:")
            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(toEmail))
            putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
            putExtra(android.content.Intent.EXTRA_TEXT, body)
            // Try to force Gmail specifically
            setPackage("com.google.android.gm")
        }
    }
    
    /**
     * Builds a generic email intent that works with any mail client.
     */
    fun toEmailIntent(): android.content.Intent {
        return android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:")
            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(toEmail))
            putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
            putExtra(android.content.Intent.EXTRA_TEXT, body)
        }
    }
    
    /**
     * Check if the email has minimum required fields filled.
     */
    fun isValid(): Boolean {
        return toEmail.isNotBlank() && 
               subject.isNotBlank() && 
               body.isNotBlank() &&
               toEmail.contains("@")
    }
    
    /**
     * Get the full email as copyable text.
     */
    fun toFullText(): String {
        return buildString {
            if (toName.isNotBlank()) appendLine("To: $toName <$toEmail>")
            else appendLine("To: $toEmail")
            appendLine("Subject: $subject")
            appendLine()
            append(body)
        }
    }
}

/**
 * Configuration for referral email generation.
 */
data class ReferralEmailConfig(
    val includeContext: Boolean = true,      // Include job/company context
    val formalityLevel: FormalityLevel = FormalityLevel.PROFESSIONAL,
    val includeSignature: Boolean = true,
    val maxBodyLength: Int = 200             // Target word count
)

enum class FormalityLevel(val label: String) {
    CASUAL("Casual & Friendly"),
    PROFESSIONAL("Professional"),
    FORMAL("Formal")
}
