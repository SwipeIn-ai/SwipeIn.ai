package com.swipeapply.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
fun TermsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Terms of Service",
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
            TermsSectionTitle("Last Updated")
            TermsBodyText("May 1, 2026")

            Spacer(modifier = Modifier.height(20.dp))

            TermsSectionTitle("1. Acceptance of Terms")
            TermsBodyText(
                "By accessing or using SwipeIn (\"the App\"), you agree to be bound by these " +
                "Terms of Service (\"Terms\"). If you do not agree to these Terms, do not use the App. " +
                "We may modify these Terms at any time, and your continued use of the App constitutes " +
                "acceptance of any changes."
            )

            Spacer(modifier = Modifier.height(20.dp))

            TermsSectionTitle("2. Eligibility")
            TermsBodyText(
                "You must be at least 16 years old to use the App. By using the App, you represent " +
                "and warrant that you meet this age requirement and have the legal capacity to enter " +
                "into these Terms."
            )

            Spacer(modifier = Modifier.height(20.dp))

            TermsSectionTitle("3. Account Registration")
            TermsBodyText(
                "To use certain features, you must create an account using Google or LinkedIn sign-in. " +
                "You are responsible for maintaining the confidentiality of your account and for all " +
                "activities that occur under your account. You agree to provide accurate and complete " +
                "information and to update it as necessary."
            )

            Spacer(modifier = Modifier.height(20.dp))

            TermsSectionTitle("4. Permitted Use")
            TermsBodyText("You agree to use the App only for lawful purposes and in accordance with these Terms. You agree NOT to:")
            TermsBulletPoint("Use the App for any illegal or unauthorized purpose")
            TermsBulletPoint("Impersonate any person or entity")
            TermsBulletPoint("Send spam, unsolicited messages, or harass others")
            TermsBulletPoint("Attempt to gain unauthorized access to our systems")
            TermsBulletPoint("Use automated scripts to access or scrape the App")
            TermsBulletPoint("Upload malicious content or viruses")

            Spacer(modifier = Modifier.height(20.dp))

            TermsSectionTitle("5. Job Listings & Referrals")
            TermsBodyText(
                "Job listings displayed in the App are sourced from third-party providers. " +
                "We do not guarantee the accuracy, completeness, or availability of any job listing. " +
                "Referral contact information is provided to help you network professionally. " +
                "You agree to use this information responsibly and in compliance with applicable laws."
            )

            Spacer(modifier = Modifier.height(20.dp))

            TermsSectionTitle("6. AI-Generated Content")
            TermsBodyText(
                "The App may offer AI-powered features to generate email drafts and personalized content. " +
                "These features require your explicit consent. AI-generated content is provided as a " +
                "suggestion only. You are solely responsible for reviewing, editing, and sending any " +
                "messages. We are not responsible for the content of messages you send."
            )

            Spacer(modifier = Modifier.height(20.dp))

            TermsSectionTitle("7. User Content")
            TermsBodyText(
                "You retain ownership of any content you submit to the App (profile information, " +
                "resume data, etc.). By submitting content, you grant us a limited license to use, " +
                "process, and store it solely for the purpose of providing the App's services to you."
            )

            Spacer(modifier = Modifier.height(20.dp))

            TermsSectionTitle("8. Privacy")
            TermsBodyText(
                "Your use of the App is also governed by our Privacy Policy, which describes how " +
                "we collect, use, and protect your information. By using the App, you consent to " +
                "the collection and use of information as described in our Privacy Policy."
            )

            Spacer(modifier = Modifier.height(20.dp))

            TermsSectionTitle("9. Termination")
            TermsBodyText(
                "We reserve the right to suspend or terminate your account at any time for violation " +
                "of these Terms or for any other reason at our discretion. You may delete your account " +
                "at any time through the App's settings. Upon termination, your right to use the App " +
                "will immediately cease."
            )

            Spacer(modifier = Modifier.height(20.dp))

            TermsSectionTitle("10. Disclaimers")
            TermsBodyText(
                "The App is provided on an \"as is\" and \"as available\" basis without warranties " +
                "of any kind, either express or implied. We do not warrant that the App will be " +
                "uninterrupted, error-free, or secure. We do not guarantee any specific outcomes " +
                "from using the App, including job placement or referral success."
            )

            Spacer(modifier = Modifier.height(20.dp))

            TermsSectionTitle("11. Limitation of Liability")
            TermsBodyText(
                "To the maximum extent permitted by law, SwipeIn and its affiliates shall not be " +
                "liable for any indirect, incidental, special, consequential, or punitive damages " +
                "arising out of or related to your use of the App."
            )

            Spacer(modifier = Modifier.height(20.dp))

            TermsSectionTitle("12. Contact Us")
            TermsBodyText(
                "If you have questions about these Terms, please contact us at:"
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
private fun TermsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun TermsBodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TermsBulletPoint(text: String) {
    Text(
        text = "  •  $text",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
