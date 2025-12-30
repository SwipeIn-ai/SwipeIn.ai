package com.swipeapply.app.data.model

/**
 * Email intro template for requesting introductions
 */
data class IntroTemplate(
    val subject: String,
    val body: String,
    val recipientPlaceholder: String = "[Hiring Manager]"
)
