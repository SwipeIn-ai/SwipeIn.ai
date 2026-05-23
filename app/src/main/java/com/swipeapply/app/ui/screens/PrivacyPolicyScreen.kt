package com.swipeapply.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            SectionTitle("Last Updated")
            BodyText("May 1, 2026")

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle("Introduction")
            BodyText(
                "SwipeIn (\"we\", \"our\", or \"us\") is committed to protecting your privacy. " +
                "This Privacy Policy explains how we collect, use, disclose, and safeguard your " +
                "information when you use our mobile application. Please read this policy carefully. " +
                "If you do not agree with the terms of this policy, please do not use the app."
            )

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle("Information We Collect")
            SubsectionTitle("Account Information")
            BodyText(
                "When you create an account, we collect information you provide such as your name, " +
                "email address, and profile details through Google or LinkedIn sign-in."
            )
            Spacer(modifier = Modifier.height(8.dp))
            SubsectionTitle("Profile Information")
            BodyText(
                "You may choose to provide additional information such as your bio, skills, " +
                "tech stack, work experience, education, and phone number. If you upload a resume, " +
                "we process it locally on your device to extract profile data."
            )
            Spacer(modifier = Modifier.height(8.dp))
            SubsectionTitle("Usage Data")
            BodyText(
                "We collect information about how you interact with the app, including job swipes, " +
                "saved jobs, and application status updates. This data helps us improve your experience " +
                "and provide better job recommendations."
            )

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle("How We Use Your Information")
            BulletPoint("To provide and maintain the app's functionality")
            BulletPoint("To match you with relevant job opportunities")
            BulletPoint("To help you discover referral contacts at companies")
            BulletPoint("To generate personalized outreach messages (only with your explicit consent)")
            BulletPoint("To send you notifications about new job matches and follow-ups (optional)")
            BulletPoint("To improve our services and user experience")

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle("AI-Powered Features")
            BodyText(
                "Some features use AI to generate personalized email drafts and rank jobs by relevance. " +
                "These features require your explicit opt-in consent. When enabled, relevant profile " +
                "details are sent to our AI service provider to generate content. You always review " +
                "and edit any AI-generated content before it is sent."
            )

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle("Data Storage & Security")
            BodyText(
                "Your account data is stored securely using industry-standard encryption. " +
                "Job swipe history and preferences are stored locally on your device. " +
                "We implement appropriate technical and organizational measures to protect " +
                "your personal information against unauthorized access, alteration, or destruction."
            )

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle("Third-Party Services")
            BodyText(
                "We use the following third-party services:"
            )
            BulletPoint("Google and LinkedIn for authentication")
            BulletPoint("Supabase for secure account management")
            BulletPoint("AI service providers for optional personalization features")
            Spacer(modifier = Modifier.height(8.dp))
            BodyText(
                "Each third-party service has its own privacy policy governing the use of your information. " +
                "We encourage you to review their policies."
            )

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle("Your Rights & Choices")
            BulletPoint("Access and update your profile information at any time")
            BulletPoint("Opt out of AI personalization features")
            BulletPoint("Decline notification permissions without affecting core functionality")
            BulletPoint("Delete your account and associated data from Settings")
            BulletPoint("Sign out and clear local data at any time")

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle("Data Retention")
            BodyText(
                "We retain your account data for as long as your account is active. " +
                "When you delete your account, we remove your personal data from our systems. " +
                "Local data on your device is cleared when you sign out or uninstall the app."
            )

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle("Children's Privacy")
            BodyText(
                "SwipeIn is not intended for use by anyone under the age of 16. " +
                "We do not knowingly collect personal information from children under 16. " +
                "If we become aware that we have collected personal data from a child, " +
                "we will take steps to delete that information."
            )

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle("Changes to This Policy")
            BodyText(
                "We may update this Privacy Policy from time to time. We will notify you of any " +
                "changes by posting the new policy within the app. You are advised to review this " +
                "policy periodically for any changes."
            )

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle("Contact Us")
            BodyText(
                "If you have questions or concerns about this Privacy Policy or your data, " +
                "please contact us at:"
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "swipein.ai@gmail.com",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SubsectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun BulletPoint(text: String) {
    Text(
        text = "  •  $text",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
