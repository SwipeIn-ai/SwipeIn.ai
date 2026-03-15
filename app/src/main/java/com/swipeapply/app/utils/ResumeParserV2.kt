package com.swipeapply.app.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.swipeapply.app.data.model.EducationItem
import com.swipeapply.app.data.model.ExperienceItem
import com.swipeapply.app.data.model.ProjectItem
import com.swipeapply.app.data.model.UserProfile
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DETERMINISTIC RESUME PARSER — No AI, No LLM, No Network, No Hallucinations.
 *
 * Strategy:
 *  1. Extract raw text from PDF via PDFBox
 *  2. Split text into logical SECTIONS using heading detection
 *  3. Extract fields per section using regex + heuristics
 *
 * Design goals:
 *  - 100% offline, runs in <500ms on any device
 *  - Zero hallucination: only extracts what it literally finds in text
 *  - Graceful degradation: if a section isn't found, that field stays empty
 *  - Progress callback: reports 0.0→1.0 as each stage completes
 *
 * Parsing stages (weight):
 *  10%  PDF text extraction
 *  10%  Section splitting
 *  15%  Contact info (name, email, phone)
 *  15%  Skills & tech stack extraction
 *  20%  Experience parsing
 *  15%  Education parsing
 *  10%  Projects parsing
 *   5%  Bio synthesis
 */
object ResumeParserV2 {

    private const val TAG = "ResumeParserV2"

    // ─── Progress callback type ───────────────────────────────────────────────
    /**
     * @param progress Float 0.0 to 1.0
     * @param stage Human-readable label for current stage
     */
    typealias ProgressCallback = (progress: Float, stage: String) -> Unit

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Parse a PDF resume into a UserProfile. Fully deterministic — no AI.
     * @param context Android context
     * @param uri     PDF file URI
     * @param onProgress Optional progress callback (0.0 → 1.0)
     * @return UserProfile filled with whatever was found, or null if PDF unreadable
     */
    suspend fun parseResume(
        context: Context,
        uri: Uri,
        onProgress: ProgressCallback = { _, _ -> }
    ): UserProfile? = withContext(Dispatchers.Default) {
        try {
            // ── Stage 1: PDF extraction (10%) ─────────────────────────────────
            onProgress(0.00f, "Reading PDF file…")
            val rawText = extractTextFromPdf(context, uri)
            if (rawText.isBlank()) {
                Log.w(TAG, "PDF text is empty")
                return@withContext null
            }
            Log.d(TAG, "Extracted ${rawText.length} chars from PDF")
            onProgress(0.10f, "PDF loaded successfully")

            // ── Stage 2: Section splitting (10%) ──────────────────────────────
            onProgress(0.12f, "Detecting resume sections…")
            val sections = splitIntoSections(rawText)
            Log.d(TAG, "Detected ${sections.size} sections: ${sections.keys}")
            onProgress(0.20f, "Found ${sections.size} sections")

            // ── Stage 3: Contact info (15%) ───────────────────────────────────
            onProgress(0.22f, "Extracting contact info…")
            val name = extractName(rawText, sections)
            val email = extractEmail(rawText)
            val phone = extractPhone(rawText)
            Log.d(TAG, "Contact: name='$name', email='$email', phone='$phone'")
            onProgress(0.35f, "Contact info extracted")

            // ── Stage 4: Skills & tech (15%) ──────────────────────────────────
            onProgress(0.37f, "Extracting skills & technologies…")
            val skillsRaw = extractSkills(rawText, sections)
            val (skills, techStack) = categorizeSkills(skillsRaw)
            Log.d(TAG, "Skills: ${skills.size}, Tech: ${techStack.size}")
            onProgress(0.50f, "Found ${skills.size + techStack.size} skills")

            // ── Stage 5: Experience (20%) ─────────────────────────────────────
            onProgress(0.52f, "Parsing work experience…")
            val experience = extractExperience(sections)
            Log.d(TAG, "Experience entries: ${experience.size}")
            onProgress(0.70f, "${experience.size} positions found")

            // ── Stage 6: Education (15%) ──────────────────────────────────────
            onProgress(0.72f, "Parsing education…")
            val education = extractEducation(sections)
            Log.d(TAG, "Education entries: ${education.size}")
            onProgress(0.85f, "${education.size} degrees found")

            // ── Stage 7: Projects (10%) ───────────────────────────────────────
            onProgress(0.87f, "Parsing projects…")
            val projects = extractProjects(sections)
            Log.d(TAG, "Projects: ${projects.size}")
            onProgress(0.95f, "${projects.size} projects found")

            // ── Stage 8: Bio synthesis (5%) ───────────────────────────────────
            onProgress(0.96f, "Building professional summary…")
            val bio = extractBio(sections, rawText, name, experience)
            onProgress(1.00f, "Resume parsed successfully!")

            return@withContext UserProfile(
                fullName = name,
                email = email,
                phone = phone,
                bio = bio,
                skills = skills,
                techStack = techStack,
                education = education,
                experience = experience,
                projects = projects
            )
        } catch (e: Exception) {
            Log.e(TAG, "Parse failed: ${e.message}", e)
            onProgress(1.0f, "Error: ${e.message}")
            return@withContext null
        }
    }

    // ─── PDF TEXT EXTRACTION ──────────────────────────────────────────────────

    private fun extractTextFromPdf(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                }
                val text = stripper.getText(document)
                document.close()
                text
            } ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "PDF read error: ${e.message}", e)
            ""
        }
    }

    // ─── SECTION DETECTION ───────────────────────────────────────────────────
    // Resumes are organized by headings. We detect common heading patterns
    // and split the text into named sections.

    private val SECTION_PATTERNS = listOf(
        "summary|professional\\s*summary|profile\\s*summary|about\\s*me|objective|career\\s*objective|profile" to "summary",
        "experience|work\\s*experience|employment|professional\\s*experience|work\\s*history|career\\s*history" to "experience",
        "education|academic|qualification|academics|schooling|certifications?\\s*(?:&|and)?\\s*education" to "education",
        "skills|technical\\s*skills|core\\s*skills|key\\s*skills|competencies|areas?\\s*of\\s*expertise|proficienc" to "skills",
        "projects|personal\\s*projects|academic\\s*projects|key\\s*projects|side\\s*projects|notable\\s*projects" to "projects",
        "certifications?|licenses?|credentials|professional\\s*certifications?" to "certifications",
        "languages?|programming\\s*languages?" to "languages",
        "awards?|achievements?|honors?|accomplishments?" to "awards",
        "interests?|hobbies|extracurricular|volunteer" to "interests",
        "publications?|research|papers?" to "publications",
        "references?" to "references",
        "tech\\s*stack|technologies|tools|frameworks|platforms" to "techstack"
    )

    /**
     * Split full resume text into named sections.
     * Returns a map of sectionName -> sectionText.
     */
    private fun splitIntoSections(text: String): Map<String, String> {
        val lines = text.lines()
        val sectionMap = mutableMapOf<String, MutableList<String>>()
        var currentSection = "header" // Everything before the first known heading
        sectionMap[currentSection] = mutableListOf()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) {
                sectionMap.getOrPut(currentSection) { mutableListOf() }.add("")
                continue
            }

            // Check if this line is a section heading
            val detectedSection = detectSectionHeading(trimmed)
            if (detectedSection != null) {
                currentSection = detectedSection
                sectionMap.getOrPut(currentSection) { mutableListOf() }
            } else {
                sectionMap.getOrPut(currentSection) { mutableListOf() }.add(trimmed)
            }
        }

        return sectionMap.mapValues { it.value.joinToString("\n").trim() }
            .filter { it.value.isNotBlank() }
    }

    private fun detectSectionHeading(line: String): String? {
        val cleaned = line
            .replace(Regex("[:\\-–—|•#*_=]+$"), "") // Strip trailing delimiters
            .replace(Regex("^[:\\-–—|•#*_=]+"), "") // Strip leading delimiters
            .trim()
            .lowercase()

        // A heading should be relatively short (< 60 chars)
        if (cleaned.length > 60) return null
        // Should not contain too many words (likely a sentence, not a heading)
        if (cleaned.split("\\s+".toRegex()).size > 6) return null

        for ((pattern, sectionName) in SECTION_PATTERNS) {
            if (Regex("^\\s*$pattern\\s*$", RegexOption.IGNORE_CASE).matches(cleaned)) {
                return sectionName
            }
        }
        return null
    }

    // ─── CONTACT INFO EXTRACTION ─────────────────────────────────────────────

    // Email regex — handles standard emails
    private val EMAIL_REGEX = Regex(
        "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"
    )

    // Phone regex — international formats
    private val PHONE_REGEX = Regex(
        "(?:\\+?\\d{1,3}[\\s.-]?)?\\(?\\d{2,4}\\)?[\\s.-]?\\d{3,4}[\\s.-]?\\d{3,4}"
    )

    // Common URL patterns to EXCLUDE from name detection
    private val URL_REGEX = Regex("https?://|www\\.|linkedin\\.com|github\\.com|@")

    private fun extractEmail(text: String): String {
        return EMAIL_REGEX.find(text)?.value ?: ""
    }

    private fun extractPhone(text: String): String {
        // Search first 30 lines for phone (usually in header)
        val headerText = text.lines().take(30).joinToString("\n")
        return PHONE_REGEX.find(headerText)?.value?.trim() ?: ""
    }

    /**
     * Name extraction strategy:
     * 1. First non-empty line that isn't an email/phone/URL/heading
     * 2. Must look like a human name (2-4 capitalized words)
     */
    private fun extractName(fullText: String, sections: Map<String, String>): String {
        val headerText = sections["header"] ?: fullText
        val lines = headerText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        for (line in lines.take(10)) {
            // Skip lines with emails, phones, URLs
            if (EMAIL_REGEX.containsMatchIn(line)) continue
            if (URL_REGEX.containsMatchIn(line)) continue
            if (PHONE_REGEX.containsMatchIn(line)) continue
            
            // Skip lines that look like addresses or dates
            if (line.contains(",") && Regex("\\d{5}").containsMatchIn(line)) continue
            
            // Skip known section headings
            if (detectSectionHeading(line) != null) continue

            // Name heuristic: 1-5 words, mostly capitalized or title-case
            val words = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (words.size in 1..5) {
                val looksLikeName = words.all { word ->
                    // Each word should start uppercase, or be a common particle
                    val particles = setOf("de", "van", "von", "al", "el", "bin", "da", "di", "le", "la", "del", "dos")
                    word[0].isUpperCase() || word.lowercase() in particles
                }
                if (looksLikeName && words.joinToString(" ").length <= 60) {
                    // Filter out lines that are likely titles ("Software Engineer", "Resume")
                    val suspectTitles = setOf("resume", "curriculum", "vitae", "cv", "portfolio")
                    if (words.none { it.lowercase() in suspectTitles }) {
                        return words.joinToString(" ")
                    }
                }
            }
        }
        return ""
    }

    // ─── SKILLS EXTRACTION ───────────────────────────────────────────────────

    /**
     * Massive keyword dictionary for technology detection.
     * These are matched case-insensitively against the resume text.
     */
    private val KNOWN_TECHNOLOGIES = setOf(
        // Languages
        "java", "python", "javascript", "typescript", "kotlin", "swift", "c++", "c#", "c",
        "go", "golang", "rust", "ruby", "php", "scala", "perl", "r", "matlab",
        "objective-c", "dart", "lua", "haskell", "erlang", "elixir", "clojure", "groovy",
        "shell", "bash", "powershell", "sql", "nosql", "graphql", "html", "css",
        "sass", "scss", "less", "xml", "json", "yaml", "toml",
        // Frontend frameworks
        "react", "react.js", "reactjs", "react native", "angular", "angularjs", "vue",
        "vue.js", "vuejs", "svelte", "sveltekit", "next.js", "nextjs", "nuxt", "nuxt.js",
        "gatsby", "remix", "astro", "solid.js", "solidjs", "htmx", "alpine.js",
        "tailwind", "tailwindcss", "tailwind css", "bootstrap", "material ui", "mui",
        "chakra ui", "ant design", "styled-components", "emotion", "css modules",
        // Backend frameworks
        "node.js", "nodejs", "node", "express", "express.js", "expressjs", "fastify",
        "nest.js", "nestjs", "spring", "spring boot", "springboot", "django",
        "flask", "fastapi", "rails", "ruby on rails", "laravel", "symfony",
        "asp.net", ".net", "dotnet", ".net core", "gin", "echo", "fiber",
        "actix", "rocket", "phoenix", "ktor",
        // Mobile
        "android", "ios", "flutter", "react native", "xamarin", "swiftui", "uikit",
        "jetpack compose", "compose", "kotlin multiplatform", "kmp",
        // Databases
        "mysql", "postgresql", "postgres", "mongodb", "sqlite", "redis",
        "cassandra", "dynamodb", "couchdb", "neo4j", "elasticsearch", "solr",
        "mariadb", "oracle", "mssql", "sql server", "firebase", "firestore",
        "supabase", "planetscale", "cockroachdb", "timescaledb",
        // Cloud & DevOps
        "aws", "amazon web services", "gcp", "google cloud", "azure",
        "docker", "kubernetes", "k8s", "terraform", "ansible", "jenkins",
        "github actions", "gitlab ci", "circleci", "travis ci",
        "nginx", "apache", "caddy", "vercel", "netlify", "heroku", "railway",
        "cloudflare", "digitalocean", "linode",
        // Data / ML / AI
        "tensorflow", "pytorch", "keras", "scikit-learn", "sklearn", "pandas",
        "numpy", "scipy", "matplotlib", "seaborn", "opencv", "nltk", "spacy",
        "hugging face", "huggingface", "transformers", "langchain",
        "openai", "gpt", "llm", "machine learning", "deep learning",
        "neural network", "nlp", "natural language processing",
        "computer vision", "data science", "data engineering",
        "apache spark", "spark", "hadoop", "kafka", "airflow", "dbt",
        "tableau", "power bi", "looker", "metabase",
        // Tools & Platforms
        "git", "github", "gitlab", "bitbucket", "jira", "confluence",
        "figma", "sketch", "adobe xd", "postman", "swagger", "graphql",
        "rest", "restful", "rest api", "grpc", "websocket",
        "rabbitmq", "celery", "sidekiq", "linux", "unix", "macos", "windows",
        "vim", "vs code", "vscode", "intellij", "android studio", "xcode",
        // Testing
        "junit", "pytest", "jest", "mocha", "chai", "cypress", "selenium",
        "playwright", "testing library", "enzyme", "espresso", "xctest",
        "mockito", "testng", "rspec",
        // Architecture / Patterns
        "microservices", "monolith", "serverless", "event-driven",
        "ci/cd", "cicd", "agile", "scrum", "kanban", "tdd", "bdd",
        "mvvm", "mvc", "mvp", "clean architecture", "solid",
        "design patterns", "oop", "functional programming",
        // Security
        "oauth", "oauth2", "jwt", "saml", "authentication", "authorization",
        "encryption", "ssl", "tls", "https", "owasp",
        // Misc
        "blockchain", "web3", "solidity", "ethereum", "smart contracts",
        "iot", "embedded", "arduino", "raspberry pi",
        "unity", "unreal engine", "game development",
        "sap", "salesforce", "shopify", "wordpress", "contentful", "strapi",
        "prisma", "sequelize", "typeorm", "hibernate", "room", "realm",
        "retrofit", "okhttp", "axios", "fetch", "graphql",
        "webpack", "vite", "esbuild", "rollup", "parcel", "babel",
        "storybook", "chromatic", "npm", "yarn", "pnpm", "gradle", "maven",
        "cocopods", "spm", "pip", "poetry", "conda"
    )

    /**
     * Common soft skills / professional skills.
     */
    private val SOFT_SKILLS = setOf(
        "leadership", "communication", "teamwork", "problem solving", "problem-solving",
        "critical thinking", "project management", "time management", "analytical",
        "collaboration", "mentoring", "presentation", "public speaking",
        "strategic planning", "stakeholder management", "decision making",
        "conflict resolution", "negotiation", "adaptability", "creativity",
        "interpersonal", "organizational", "detail-oriented", "multitasking",
        "customer service", "client relations", "business development",
        "product management", "product design", "ux design", "ui design",
        "ux/ui", "ui/ux", "user research", "wireframing", "prototyping",
        "data analysis", "business intelligence", "requirements gathering",
        "system design", "technical writing", "documentation",
        "cross-functional", "remote work", "distributed teams"
    )

    /**
     * Classify skills into tech vs soft skills.
     */
    private val TECH_KEYWORDS = setOf(
        // Sub-categories of techs that should go to techStack specifically
        "aws", "gcp", "azure", "docker", "kubernetes", "k8s", "terraform",
        "jenkins", "github actions", "gitlab ci", "circleci",
        "react", "react.js", "angular", "vue", "vue.js", "svelte", "next.js",
        "node.js", "express", "spring", "spring boot", "django", "flask", "fastapi",
        "django", "rails", "laravel", "dotnet", ".net",
        "mysql", "postgresql", "mongodb", "redis", "elasticsearch", "firebase",
        "supabase", "dynamodb", "sqlite",
        "android", "ios", "flutter", "react native", "jetpack compose",
        "tensorflow", "pytorch", "pandas", "numpy", "scikit-learn",
        "kafka", "spark", "hadoop", "airflow",
        "docker", "kubernetes", "nginx", "linux",
        "git", "github", "jira", "figma", "postman",
        "retrofit", "okhttp", "gradle", "maven", "webpack", "vite"
    )

    private fun extractSkills(fullText: String, sections: Map<String, String>): Set<String> {
        val foundSkills = mutableSetOf<String>()
        val textLower = fullText.lowercase()

        // 1. Scan the skills section specifically (highest priority)
        val skillsSectionText = listOfNotNull(
            sections["skills"],
            sections["techstack"],
            sections["languages"]
        ).joinToString("\n").lowercase()

        // 2. Scan skills section for comma/pipe/bullet separated items
        if (skillsSectionText.isNotBlank()) {
            val items = skillsSectionText
                .replace(Regex("[•·▪▸►➤➢⊳⊲✓✔★☆●○■□◆◇]"), ",") // bullets to commas
                .replace("|", ",")
                .replace(";", ",")
                .replace("\n", ",")
                .split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() && it.length > 1 && it.length < 50 }

            for (item in items) {
                // Check exact matches first
                if (item in KNOWN_TECHNOLOGIES || item in SOFT_SKILLS) {
                    foundSkills.add(item)
                }
                // Check if a known tech appears within this item
                for (tech in KNOWN_TECHNOLOGIES) {
                    if (tech.length >= 3 && item.contains(tech)) {
                        foundSkills.add(tech)
                    }
                }
            }
        }

        // 3. Full-text scan for technology keywords (catches skills mentioned in experience/projects)
        for (tech in KNOWN_TECHNOLOGIES) {
            if (tech.length < 3) continue // Skip very short ones like "c", "r" in full-text scan
            // Word-boundary check to avoid false positives
            val pattern = Regex("\\b${Regex.escape(tech)}\\b", RegexOption.IGNORE_CASE)
            if (pattern.containsMatchIn(textLower)) {
                foundSkills.add(tech)
            }
        }

        // Also check 1-2 char techs only in skills section
        val shortTechs = setOf("c", "r", "go")
        for (tech in shortTechs) {
            val sectionText = skillsSectionText
            val pattern = Regex("\\b${Regex.escape(tech)}\\b", RegexOption.IGNORE_CASE)
            if (pattern.containsMatchIn(sectionText)) {
                foundSkills.add(tech)
            }
        }

        // 4. Scan for soft skills in full text 
        for (skill in SOFT_SKILLS) {
            if (skill.length >= 5) {
                val pattern = Regex("\\b${Regex.escape(skill)}\\b", RegexOption.IGNORE_CASE)
                if (pattern.containsMatchIn(textLower)) {
                    foundSkills.add(skill)
                }
            }
        }

        return foundSkills
    }

    /**
     * Separate found skills into (softSkills, techStack).
     */
    private fun categorizeSkills(allSkills: Set<String>): Pair<List<String>, List<String>> {
        val skills = mutableListOf<String>() // soft / general skills
        val techStack = mutableListOf<String>() // technical tools / frameworks / languages

        for (skill in allSkills) {
            val normalized = skill.lowercase().trim()
            when {
                normalized in SOFT_SKILLS -> skills.add(titleCase(normalized))
                normalized in TECH_KEYWORDS -> techStack.add(titleCase(normalized))
                normalized in KNOWN_TECHNOLOGIES -> techStack.add(titleCase(normalized))
                else -> skills.add(titleCase(normalized))
            }
        }

        return Pair(
            skills.distinct().sorted(),
            techStack.distinct().sorted()
        )
    }

    // ─── EXPERIENCE EXTRACTION ───────────────────────────────────────────────

    // Date patterns (Jan 2020 - Dec 2023, 2020-2023, etc.)
    private val DATE_RANGE_REGEX = Regex(
        "(?:" +
        "(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)" +
        "\\s*\\.?\\s*\\d{2,4}" +
        "|\\d{1,2}/\\d{2,4}" +
        "|\\d{4}" +
        ")" +
        "\\s*[-–—to]+\\s*" +
        "(?:" +
        "(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)" +
        "\\s*\\.?\\s*\\d{2,4}" +
        "|\\d{1,2}/\\d{2,4}" +
        "|\\d{4}" +
        "|[Pp]resent|[Cc]urrent|[Nn]ow|[Oo]ngoing" +
        ")",
        RegexOption.IGNORE_CASE
    )

    /**
     * Parse experience section into structured entries.
     *
     * Strategy:
     * Every time we see a date range pattern on a line, we consider it a 
     * new experience entry. We look at surrounding lines for role/company.
     */
    private fun extractExperience(sections: Map<String, String>): List<ExperienceItem> {
        val experienceText = sections["experience"] ?: return emptyList()
        val lines = experienceText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val entries = mutableListOf<ExperienceItem>()
        val entryBlocks = splitIntoEntryBlocks(lines)

        for (block in entryBlocks) {
            val entry = parseExperienceBlock(block)
            if (entry != null && (entry.company.isNotBlank() || entry.role.isNotBlank())) {
                entries.add(entry)
            }
        }

        return entries
    }

    /**
     * Split lines into blocks. Each block starts when we detect a date range
     * or a likely new entry (bold/caps line followed by a different context).
     */
    private fun splitIntoEntryBlocks(lines: List<String>): List<List<String>> {
        val blocks = mutableListOf<MutableList<String>>()
        var currentBlock = mutableListOf<String>()

        for (line in lines) {
            val hasDate = DATE_RANGE_REGEX.containsMatchIn(line)
            val isLikelyHeader = isLikelyEntryHeader(line)

            if ((hasDate || isLikelyHeader) && currentBlock.isNotEmpty()) {
                blocks.add(currentBlock)
                currentBlock = mutableListOf(line)
            } else {
                currentBlock.add(line)
            }
        }
        if (currentBlock.isNotEmpty()) {
            blocks.add(currentBlock)
        }

        return blocks
    }

    /**
     * Detect if a line looks like a new entry header.
     * Heuristics: All caps, or short line that doesn't start with a bullet.
     */
    private fun isLikelyEntryHeader(line: String): Boolean {
        if (line.startsWith("•") || line.startsWith("-") || line.startsWith("–") || line.startsWith("*")) return false
        // Company/role lines are usually < 80 chars
        if (line.length > 100) return false
        // Check for common role keywords
        val rolePattern = Regex(
            "(?:software|senior|junior|lead|principal|staff|intern|associate|manager|director|head|vp|engineer|developer|designer|analyst|consultant|architect|devops|sre|sde|frontend|backend|fullstack|full-stack|full stack)",
            RegexOption.IGNORE_CASE
        )
        return rolePattern.containsMatchIn(line)
    }

    private fun parseExperienceBlock(block: List<String>): ExperienceItem? {
        if (block.isEmpty()) return null

        var role = ""
        var company = ""
        var duration = ""
        val descriptionLines = mutableListOf<String>()

        for (line in block) {
            // Extract date range
            val dateMatch = DATE_RANGE_REGEX.find(line)
            if (dateMatch != null && duration.isBlank()) {
                duration = dateMatch.value.trim()
            }

            val lineWithoutDate = if (dateMatch != null) {
                line.replace(dateMatch.value, "").trim().trimEnd(',', '-', '–', '|', '•')
            } else line

            // Lines with bullets are description
            if (line.startsWith("•") || line.startsWith("-") || line.startsWith("–") ||
                line.startsWith("*") || line.startsWith("▪") || line.startsWith("►")) {
                descriptionLines.add(line.removePrefix("•").removePrefix("-").removePrefix("–")
                    .removePrefix("*").removePrefix("▪").removePrefix("►").trim())
                continue
            }

            // Try to identify role vs company from non-bullet lines
            if (role.isBlank() && lineWithoutDate.isNotBlank()) {
                // If line contains common role words, it's the role
                val rolePattern = Regex(
                    "(?:software|senior|junior|lead|principal|staff|intern|associate|manager|director|head|vp|engineer|developer|designer|analyst|consultant|architect|devops|sre|sde)",
                    RegexOption.IGNORE_CASE
                )
                if (rolePattern.containsMatchIn(lineWithoutDate)) {
                    // Check if company is also on this line (separated by | or ,  or "at")
                    val parts = lineWithoutDate.split(Regex("\\s*[|,]\\s*|\\s+at\\s+|\\s*[-–—]\\s*"))
                    if (parts.size >= 2) {
                        val first = parts[0].trim()
                        val second = parts[1].trim()
                        if (rolePattern.containsMatchIn(first)) {
                            role = first
                            company = second
                        } else {
                            company = first
                            role = second
                        }
                    } else {
                        role = lineWithoutDate.trim()
                    }
                    continue
                }

                // If we haven't found a role, and this looks like a company name (not a bullet)
                if (company.isBlank() && lineWithoutDate.isNotBlank() && lineWithoutDate.length < 80) {
                    // Could be company name
                    if (role.isNotBlank()) {
                        company = lineWithoutDate.trim()
                    } else {
                        // First non-bullet line might be role or company; store as role, might swap later
                        role = lineWithoutDate.trim()
                    }
                    continue
                }
            } else if (company.isBlank() && lineWithoutDate.isNotBlank() && lineWithoutDate.length < 80) {
                company = lineWithoutDate.trim().trimEnd(',', '-', '–', '|')
                continue
            }

            // Remaining lines are description
            if (lineWithoutDate.isNotBlank()) {
                descriptionLines.add(lineWithoutDate)
            }
        }

        val description = descriptionLines
            .filter { it.isNotBlank() }
            .take(5) // cap description bullets
            .joinToString(". ")
            .trim()

        return ExperienceItem(
            company = cleanField(company),
            role = cleanField(role),
            duration = cleanField(duration),
            description = description
        )
    }

    // ─── EDUCATION EXTRACTION ────────────────────────────────────────────────

    private val DEGREE_KEYWORDS = listOf(
        "ph\\.?d", "doctorate", "doctor of",
        "master(?:'?s)?", "m\\.?s\\.?", "m\\.?a\\.?", "m\\.?b\\.?a\\.?",
        "m\\.?tech", "m\\.?sc", "m\\.?eng",
        "bachelor(?:'?s)?", "b\\.?s\\.?", "b\\.?a\\.?", "b\\.?sc", "b\\.?e\\.?",
        "b\\.?tech", "b\\.?eng", "b\\.?com",
        "associate(?:'?s)?", "a\\.?s\\.?", "a\\.?a\\.?",
        "diploma", "certificate", "certification",
        "high school", "secondary", "ged"
    )

    private val DEGREE_REGEX = Regex(
        "(?:${DEGREE_KEYWORDS.joinToString("|")})",
        RegexOption.IGNORE_CASE
    )

    private val YEAR_REGEX = Regex("(?:19|20)\\d{2}")

    private fun extractEducation(sections: Map<String, String>): List<EducationItem> {
        val educationText = sections["education"] ?: return emptyList()
        val lines = educationText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val entries = mutableListOf<EducationItem>()
        val blocks = splitEducationBlocks(lines)

        for (block in blocks) {
            val entry = parseEducationBlock(block)
            if (entry != null && (entry.school.isNotBlank() || entry.degree.isNotBlank())) {
                entries.add(entry)
            }
        }

        return entries
    }

    private fun splitEducationBlocks(lines: List<String>): List<List<String>> {
        val blocks = mutableListOf<MutableList<String>>()
        var current = mutableListOf<String>()

        for (line in lines) {
            val hasDegree = DEGREE_REGEX.containsMatchIn(line)
            val hasYear = YEAR_REGEX.containsMatchIn(line)
            val isShortLine = line.length < 80 && !line.startsWith("•") && !line.startsWith("-")

            if ((hasDegree || (hasYear && isShortLine)) && current.isNotEmpty()) {
                blocks.add(current)
                current = mutableListOf(line)
            } else {
                current.add(line)
            }
        }
        if (current.isNotEmpty()) blocks.add(current)
        return blocks
    }

    private fun parseEducationBlock(block: List<String>): EducationItem? {
        if (block.isEmpty()) return null

        val allText = block.joinToString(" ")
        var school = ""
        var degree = ""
        var year = ""

        // Extract year
        val yearMatches = YEAR_REGEX.findAll(allText).toList()
        year = if (yearMatches.size >= 2) {
            "${yearMatches.first().value} - ${yearMatches.last().value}"
        } else {
            yearMatches.firstOrNull()?.value ?: ""
        }

        // Extract degree
        val degreeMatch = DEGREE_REGEX.find(allText)
        if (degreeMatch != null) {
            // Take the full line containing the degree as the degree text
            for (line in block) {
                if (DEGREE_REGEX.containsMatchIn(line)) {
                    degree = line.replace(YEAR_REGEX, "").trim().trimEnd(',', '-', '–', '|', '(', ')')
                    break
                }
            }
        }

        // School is typically the line that doesn't contain the degree keyword
        for (line in block) {
            if (!DEGREE_REGEX.containsMatchIn(line) && line.length < 100) {
                val cleaned = line.replace(YEAR_REGEX, "").trim().trimEnd(',', '-', '–', '|', '(', ')')
                if (cleaned.isNotBlank() && cleaned.length > 2) {
                    school = cleaned
                    break
                }
            }
        }

        // If no separate school line, try to split degree line at common separators
        if (school.isBlank() && degree.isNotBlank()) {
            val parts = degree.split(Regex("\\s*[,|;]\\s*|\\s+[-–—]\\s+|\\s+from\\s+|\\s+at\\s+"))
            if (parts.size >= 2) {
                degree = parts[0].trim()
                school = parts[1].trim()
            }
        }

        return EducationItem(
            school = cleanField(school),
            degree = cleanField(degree),
            year = cleanField(year)
        )
    }

    // ─── PROJECTS EXTRACTION ─────────────────────────────────────────────────

    private fun extractProjects(sections: Map<String, String>): List<ProjectItem> {
        val projectText = sections["projects"] ?: return emptyList()
        val lines = projectText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val entries = mutableListOf<ProjectItem>()
        val blocks = splitProjectBlocks(lines)

        for (block in blocks) {
            val entry = parseProjectBlock(block)
            if (entry != null && entry.name.isNotBlank()) {
                entries.add(entry)
            }
        }

        return entries
    }

    private fun splitProjectBlocks(lines: List<String>): List<List<String>> {
        val blocks = mutableListOf<MutableList<String>>()
        var current = mutableListOf<String>()

        for (line in lines) {
            // New project entry: short non-bullet line followed by bullets
            val isBullet = line.startsWith("•") || line.startsWith("-") || line.startsWith("–") || line.startsWith("*")
            val isShortTitle = !isBullet && line.length < 80

            if (isShortTitle && current.isNotEmpty() && current.any {
                    it.startsWith("•") || it.startsWith("-") || it.startsWith("–")
                }) {
                blocks.add(current)
                current = mutableListOf(line)
            } else {
                current.add(line)
            }
        }
        if (current.isNotEmpty()) blocks.add(current)
        return blocks
    }

    private fun parseProjectBlock(block: List<String>): ProjectItem? {
        if (block.isEmpty()) return null

        val name = block.first()
            .replace(Regex("[•\\-–*▪►]"), "")
            .trim()
            .take(100)

        val descriptionParts = mutableListOf<String>()
        val techParts = mutableListOf<String>()

        for (line in block.drop(1)) {
            val cleaned = line
                .removePrefix("•").removePrefix("-").removePrefix("–")
                .removePrefix("*").removePrefix("▪").removePrefix("►")
                .trim()

            // Check if this line mentions technologies
            val techFound = KNOWN_TECHNOLOGIES.filter { tech ->
                tech.length >= 3 && Regex("\\b${Regex.escape(tech)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(cleaned)
            }

            if (cleaned.lowercase().startsWith("tech") || cleaned.lowercase().startsWith("built with") ||
                cleaned.lowercase().startsWith("stack") || cleaned.lowercase().startsWith("tools")) {
                techParts.addAll(techFound.map { titleCase(it) })
                // Also add the raw items from the tech line
                cleaned.split(Regex("[,;|]")).map { it.trim() }.filter { it.isNotBlank() && it.length < 30 }.forEach { item ->
                    val lower = item.lowercase()
                    if (lower in KNOWN_TECHNOLOGIES) techParts.add(titleCase(lower))
                }
            } else {
                descriptionParts.add(cleaned)
                techParts.addAll(techFound.map { titleCase(it) })
            }
        }

        return ProjectItem(
            name = cleanField(name),
            description = descriptionParts.take(3).joinToString(". ").take(300),
            techUsed = techParts.distinct().take(10).joinToString(", ")
        )
    }

    // ─── BIO SYNTHESIS ───────────────────────────────────────────────────────

    /**
     * Extract bio from summary section, or synthesize from first few lines.
     * No AI — just grabs text that's already in the resume.
     */
    private fun extractBio(
        sections: Map<String, String>,
        fullText: String,
        name: String,
        experience: List<ExperienceItem>
    ): String {
        // 1. Check for explicit summary/objective section
        val summaryText = sections["summary"]
        if (!summaryText.isNullOrBlank()) {
            return summaryText.lines()
                .filter { it.isNotBlank() }
                .take(5)
                .joinToString(" ")
                .take(500)
                .trim()
        }

        // 2. Check header section for multi-sentence text (likely a bio)
        val headerText = sections["header"] ?: ""
        val headerLines = headerText.lines().filter { it.isNotBlank() }
        for (line in headerLines) {
            // Skip name, email, phone lines
            if (line == name) continue
            if (EMAIL_REGEX.containsMatchIn(line)) continue
            if (PHONE_REGEX.containsMatchIn(line)) continue
            if (URL_REGEX.containsMatchIn(line)) continue
            // If it's a longer line, it might be a summary
            if (line.length > 60) return line.take(500)
        }

        // 3. Synthesize from experience (purely deterministic, no AI)
        if (experience.isNotEmpty()) {
            val latest = experience.first()
            val roleStr = if (latest.role.isNotBlank()) latest.role else "Professional"
            val companyStr = if (latest.company.isNotBlank()) " at ${latest.company}" else ""
            return "$roleStr$companyStr."
        }

        return ""
    }

    // ─── UTILITIES ───────────────────────────────────────────────────────────

    private fun titleCase(input: String): String {
        // Special cases for known tech casing
        val specialCases = mapOf(
            "javascript" to "JavaScript", "typescript" to "TypeScript",
            "node.js" to "Node.js", "nodejs" to "Node.js",
            "react.js" to "React.js", "reactjs" to "React.js",
            "vue.js" to "Vue.js", "vuejs" to "Vue.js",
            "next.js" to "Next.js", "nextjs" to "Next.js",
            "express.js" to "Express.js", "expressjs" to "Express.js",
            "angular" to "Angular", "react" to "React", "vue" to "Vue",
            "react native" to "React Native",
            "svelte" to "Svelte", "sveltekit" to "SvelteKit",
            "python" to "Python", "java" to "Java", "kotlin" to "Kotlin",
            "swift" to "Swift", "rust" to "Rust", "go" to "Go", "golang" to "Go",
            "ruby" to "Ruby", "php" to "PHP", "scala" to "Scala",
            "c++" to "C++", "c#" to "C#", "c" to "C", "r" to "R",
            "dart" to "Dart", "flutter" to "Flutter",
            "spring" to "Spring", "spring boot" to "Spring Boot",
            "springboot" to "Spring Boot",
            "django" to "Django", "flask" to "Flask", "fastapi" to "FastAPI",
            "rails" to "Rails", "laravel" to "Laravel",
            "docker" to "Docker", "kubernetes" to "Kubernetes",
            "terraform" to "Terraform", "ansible" to "Ansible",
            "jenkins" to "Jenkins",
            "aws" to "AWS", "gcp" to "GCP", "azure" to "Azure",
            "mysql" to "MySQL", "postgresql" to "PostgreSQL",
            "postgres" to "PostgreSQL", "mongodb" to "MongoDB",
            "redis" to "Redis", "sqlite" to "SQLite",
            "firebase" to "Firebase", "supabase" to "Supabase",
            "elasticsearch" to "Elasticsearch",
            "linux" to "Linux", "nginx" to "Nginx",
            "git" to "Git", "github" to "GitHub", "gitlab" to "GitLab",
            "figma" to "Figma", "jira" to "Jira",
            "graphql" to "GraphQL", "rest" to "REST", "grpc" to "gRPC",
            "sql" to "SQL", "nosql" to "NoSQL",
            "html" to "HTML", "css" to "CSS", "sass" to "Sass",
            "tailwind" to "Tailwind", "tailwindcss" to "Tailwind CSS",
            "tailwind css" to "Tailwind CSS",
            "bootstrap" to "Bootstrap",
            "tensorflow" to "TensorFlow", "pytorch" to "PyTorch",
            "pandas" to "Pandas", "numpy" to "NumPy",
            "scikit-learn" to "Scikit-learn",
            "android" to "Android", "ios" to "iOS",
            "jetpack compose" to "Jetpack Compose",
            "retrofit" to "Retrofit", "okhttp" to "OkHttp",
            "gradle" to "Gradle", "maven" to "Maven",
            "junit" to "JUnit", "pytest" to "Pytest",
            "jest" to "Jest", "cypress" to "Cypress",
            "selenium" to "Selenium", "postman" to "Postman",
            "webpack" to "Webpack", "vite" to "Vite",
            "npm" to "npm", "yarn" to "Yarn",
            "agile" to "Agile", "scrum" to "Scrum",
            "ci/cd" to "CI/CD", "cicd" to "CI/CD",
            "mvvm" to "MVVM", "mvc" to "MVC",
            "oauth" to "OAuth", "jwt" to "JWT",
            "solid" to "SOLID", "oop" to "OOP", "tdd" to "TDD",
            "machine learning" to "Machine Learning",
            "deep learning" to "Deep Learning",
            "nlp" to "NLP", "natural language processing" to "NLP",
            "computer vision" to "Computer Vision",
            "data science" to "Data Science",
            "data engineering" to "Data Engineering",
            ".net" to ".NET", "dotnet" to ".NET", ".net core" to ".NET Core",
            "asp.net" to "ASP.NET",
            "android studio" to "Android Studio", "xcode" to "Xcode",
            "vs code" to "VS Code", "vscode" to "VS Code",
            "intellij" to "IntelliJ"
        )

        val lower = input.lowercase().trim()
        specialCases[lower]?.let { return it }

        // Default: capitalize first letter of each word
        return input.trim().split("\\s+".toRegex()).joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    private fun cleanField(value: String): String {
        return value
            .trim()
            .trimEnd(',', '-', '–', '|', '•', ':', ';')
            .trimStart(',', '-', '–', '|', '•', ':', ';')
            .trim()
    }
}
