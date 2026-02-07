package com.swipeapply.app.data.ranking

import android.content.Context
import android.util.Log

/**
 * DETERMINISTIC JOB RANKING ENGINE
 * 
 * CRITICAL DESIGN PRINCIPLE:
 * This engine uses ONLY rule-based, deterministic logic.
 * NO AI, NO ML models, NO neural networks, NO embeddings.
 * 
 * The ranking is:
 * - FAST: O(n) per job, cacheable
 * - PREDICTABLE: Same inputs always produce same outputs
 * - AUDITABLE: Every score component is traceable
 * - TESTABLE: Unit tests can verify exact behavior
 * 
 * AI is ONLY used in FitReasoningModule for generating explanations
 * AFTER ranking is complete. AI cannot influence job order.
 */
class JobRankingEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "JobRankingEngine"
        
        @Volatile
        private var INSTANCE: JobRankingEngine? = null
        
        fun getInstance(context: Context): JobRankingEngine {
            return INSTANCE ?: synchronized(this) {
                JobRankingEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val normalizer = SkillNormalizer.getInstance(context)
    private val taxonomy = SkillTaxonomy.getInstance(context)
    
    /**
     * MAIN RANKING FUNCTION
     * 
     * Computes a deterministic relevance score and detailed match breakdown
     * for a candidate-job pair.
     * 
     * PSEUDO-CODE:
     * 1. Normalize all skills (candidate and job)
     * 2. Compute skill match score (50% weight)
     *    - For each required skill: find best match in candidate skills
     *    - For each preferred skill: find best match (lower penalty for missing)
     *    - Award bonus for extra relevant skills
     * 3. Compute experience match score (25% weight)
     *    - Perfect fit: 1.0
     *    - Each year under: -0.1 penalty (floor at 0.3)
     *    - Overqualified: slight penalty
     * 4. Compute role alignment score (15% weight)
     *    - Title similarity
     *    - Seniority match
     *    - Domain match
     * 5. Compute tech stack depth score (10% weight)
     *    - Breadth of ecosystem knowledge
     * 6. Combine: finalScore = Σ(component * weight)
     * 7. Determine fit category from score thresholds
     */
    fun computeMatch(
        candidate: CandidateProfile,
        job: JobRequirements
    ): MatchBreakdown {
        Log.d(TAG, "Computing match for job: ${job.jobId}")
        
        // Step 1: Normalize skills
        val candidateSkills = normalizer.normalizeAll(candidate.normalizedSkills)
        val candidateTechStack = normalizer.normalizeAll(candidate.techStack)
        val allCandidateSkills = candidateSkills + candidateTechStack
        
        val requiredSkills = normalizer.normalizeAll(job.requiredSkills)
        val preferredSkills = normalizer.normalizeAll(job.preferredSkills)
        
        // Step 2: Skill matching
        val skillResult = computeSkillMatch(allCandidateSkills, requiredSkills, preferredSkills)
        
        // Step 3: Experience matching
        val experienceResult = computeExperienceMatch(
            candidate.yearsOfExperience, 
            job.minExperience, 
            job.maxExperience
        )
        
        // Step 4: Role alignment
        val roleResult = computeRoleAlignment(candidate, job)
        
        // Step 5: Tech stack depth
        val techStackDepthScore = computeTechStackDepth(allCandidateSkills, requiredSkills + preferredSkills)
        
        // Step 6: Weighted combination
        val weights = ScoreWeights()
        val skillScore = skillResult.matchPercentage
        val experienceScore = 1.0 + experienceResult.penalty // penalty is negative or zero
        val roleScore = roleResult.alignmentScore
        
        val finalScore = (
            skillScore * weights.skills +
            experienceScore.coerceIn(0.0, 1.0) * weights.experience +
            roleScore * weights.role +
            techStackDepthScore * weights.techStackDepth
        ).coerceIn(0.0, 1.0)
        
        val scoreComponents = ScoreComponents(
            skillScore = skillScore,
            experienceScore = experienceScore.coerceIn(0.0, 1.0),
            roleScore = roleScore,
            techStackDepthScore = techStackDepthScore,
            weights = weights
        )
        
        // Step 7: Determine fit category
        val fitCategory = FitCategory.fromScore(finalScore)
        
        Log.d(TAG, "Match computed: score=$finalScore, fit=${fitCategory.label}")
        
        return MatchBreakdown(
            jobId = job.jobId,
            finalScore = finalScore,
            skillMatch = skillResult,
            experienceMatch = experienceResult,
            roleAlignment = roleResult,
            overallFit = fitCategory,
            scoreComponents = scoreComponents
        )
    }
    
    /**
     * Compute skill match score with detailed breakdown.
     */
    private fun computeSkillMatch(
        candidateSkills: Set<String>,
        requiredSkills: Set<String>,
        preferredSkills: Set<String>
    ): SkillMatchResult {
        val requiredMatched = mutableListOf<SkillMatch>()
        val requiredMissing = mutableListOf<String>()
        val preferredMatched = mutableListOf<SkillMatch>()
        val preferredMissing = mutableListOf<String>()
        
        // Score for required skills - higher weight
        var requiredScore = 0.0
        for (required in requiredSkills) {
            val (bestMatch, similarity) = findBestSkillMatch(required, candidateSkills)
            
            if (similarity >= SkillTaxonomy.SAME_CATEGORY) {
                val matchType = when {
                    similarity == SkillTaxonomy.EXACT_MATCH -> MatchType.EXACT
                    similarity >= SkillTaxonomy.SAME_SUBCATEGORY -> MatchType.PARTIAL
                    else -> MatchType.PARTIAL
                }
                requiredMatched.add(SkillMatch(
                    candidateSkill = bestMatch ?: required,
                    jobSkill = required,
                    matchType = matchType,
                    weight = similarity
                ))
                requiredScore += similarity
            } else {
                requiredMissing.add(required)
            }
        }
        
        // Score for preferred skills - lower weight for missing
        var preferredScore = 0.0
        for (preferred in preferredSkills) {
            val (bestMatch, similarity) = findBestSkillMatch(preferred, candidateSkills)
            
            if (similarity >= SkillTaxonomy.SAME_CATEGORY) {
                val matchType = when {
                    similarity == SkillTaxonomy.EXACT_MATCH -> MatchType.EXACT
                    similarity >= SkillTaxonomy.SAME_SUBCATEGORY -> MatchType.PARTIAL
                    else -> MatchType.PARTIAL
                }
                preferredMatched.add(SkillMatch(
                    candidateSkill = bestMatch ?: preferred,
                    jobSkill = preferred,
                    matchType = matchType,
                    weight = similarity
                ))
                preferredScore += similarity
            } else {
                preferredMissing.add(preferred)
            }
        }
        
        // Bonus skills (candidate has but job doesn't require)
        val allJobSkills = requiredSkills + preferredSkills
        val bonusSkills = candidateSkills.filter { 
            it !in allJobSkills && taxonomy.isKnownSkill(it) 
        }
        
        // Calculate overall match percentage
        // Required skills are worth 70%, preferred worth 30%
        val maxRequiredScore = requiredSkills.size.toDouble().coerceAtLeast(1.0)
        val maxPreferredScore = preferredSkills.size.toDouble().coerceAtLeast(1.0)
        
        val requiredPercentage = if (requiredSkills.isEmpty()) 1.0 else requiredScore / maxRequiredScore
        val preferredPercentage = if (preferredSkills.isEmpty()) 1.0 else preferredScore / maxPreferredScore
        
        val matchPercentage = (requiredPercentage * 0.7 + preferredPercentage * 0.3).coerceIn(0.0, 1.0)
        
        return SkillMatchResult(
            requiredMatched = requiredMatched,
            requiredMissing = requiredMissing,
            preferredMatched = preferredMatched,
            preferredMissing = preferredMissing,
            bonusSkills = bonusSkills,
            matchPercentage = matchPercentage
        )
    }
    
    /**
     * Find the best matching candidate skill for a job skill.
     * Uses both normalizer (aliases) and taxonomy (related skills).
     */
    private fun findBestSkillMatch(jobSkill: String, candidateSkills: Set<String>): Pair<String?, Double> {
        // First, check for exact/normalized match
        if (candidateSkills.contains(jobSkill)) {
            return Pair(jobSkill, SkillTaxonomy.EXACT_MATCH)
        }
        
        // Check aliases via normalizer
        val normalizedJob = normalizer.normalize(jobSkill)
        for (candidate in candidateSkills) {
            if (normalizer.normalize(candidate) == normalizedJob) {
                return Pair(candidate, SkillTaxonomy.EXACT_MATCH)
            }
        }
        
        // Check ecosystem expansion
        if (normalizer.isEcosystemSkill(jobSkill)) {
            val components = normalizer.getEcosystemComponents(jobSkill)
            for (candidate in candidateSkills) {
                if (components.contains(normalizer.normalize(candidate))) {
                    return Pair(candidate, SkillTaxonomy.SAME_SUBCATEGORY)
                }
            }
        }
        
        // Use taxonomy for partial matching
        return taxonomy.findBestMatch(jobSkill, candidateSkills)
    }
    
    /**
     * Compute experience match with smooth penalty curve.
     */
    private fun computeExperienceMatch(
        candidateYears: Int,
        minRequired: Int,
        maxRequired: Int?
    ): ExperienceMatchResult {
        val fit: ExperienceFit
        var penalty = 0.0
        
        when {
            candidateYears >= minRequired && (maxRequired == null || candidateYears <= maxRequired) -> {
                fit = ExperienceFit.MATCHES
                penalty = 0.0
            }
            candidateYears > (maxRequired ?: Int.MAX_VALUE) -> {
                fit = ExperienceFit.EXCEEDS
                // Slight penalty for being overqualified (may not stay long)
                penalty = -0.05
            }
            candidateYears >= minRequired - 2 -> {
                fit = ExperienceFit.SLIGHTLY_UNDER
                // -0.1 per year under
                penalty = (candidateYears - minRequired) * 0.1 // negative
            }
            else -> {
                fit = ExperienceFit.SIGNIFICANTLY_UNDER
                // Steeper penalty for significant gap
                penalty = -0.3 - (minRequired - candidateYears - 2) * 0.05
                penalty = penalty.coerceAtLeast(-0.7) // Floor
            }
        }
        
        return ExperienceMatchResult(
            candidateYears = candidateYears,
            requiredYears = minRequired,
            experienceFit = fit,
            penalty = penalty
        )
    }
    
    /**
     * Compute role and domain alignment.
     */
    private fun computeRoleAlignment(
        candidate: CandidateProfile,
        job: JobRequirements
    ): RoleAlignmentResult {
        // Simple title matching (could be enhanced with taxonomy)
        val candidateRolesLower = candidate.roles.map { it.lowercase() }
        val targetRoleLower = job.targetRole.lowercase()
        
        // Check for role title similarity
        var titleScore = 0.0
        for (role in candidateRolesLower) {
            when {
                role.contains(targetRoleLower) || targetRoleLower.contains(role) -> {
                    titleScore = 1.0
                    break
                }
                role.split(" ").intersect(targetRoleLower.split(" ").toSet()).isNotEmpty() -> {
                    titleScore = maxOf(titleScore, 0.5)
                }
            }
        }
        
        // Seniority matching
        val seniorityMatch = computeSeniorityMatch(candidateRolesLower, job.seniority)
        val seniorityScore = when (seniorityMatch) {
            SeniorityMatch.EXACT -> 1.0
            SeniorityMatch.STRETCH_UP -> 0.6
            SeniorityMatch.OVERQUALIFIED -> 0.8
        }
        
        // Domain matching
        val domainMatch = job.domain?.let { candidate.domains.contains(it.lowercase()) } ?: true
        val domainScore = if (domainMatch) 1.0 else 0.5
        
        // Weighted combination
        val alignmentScore = (titleScore * 0.4 + seniorityScore * 0.4 + domainScore * 0.2).coerceIn(0.0, 1.0)
        
        return RoleAlignmentResult(
            candidateRoles = candidate.roles,
            targetRole = job.targetRole,
            domainMatch = domainMatch,
            seniorityMatch = seniorityMatch,
            alignmentScore = alignmentScore
        )
    }
    
    /**
     * Determine seniority match based on role titles.
     */
    private fun computeSeniorityMatch(candidateRoles: List<String>, jobSeniority: String): SeniorityMatch {
        val seniorityLevels = mapOf(
            "intern" to 0,
            "junior" to 1,
            "mid" to 2,
            "senior" to 3,
            "staff" to 4,
            "principal" to 5,
            "lead" to 4,
            "manager" to 4,
            "director" to 5
        )
        
        // Extract candidate's highest seniority
        var candidateLevel = 2 // Default to mid-level
        for (role in candidateRoles) {
            for ((keyword, level) in seniorityLevels) {
                if (role.contains(keyword)) {
                    candidateLevel = maxOf(candidateLevel, level)
                }
            }
        }
        
        val jobLevel = seniorityLevels[jobSeniority.lowercase()] ?: 2
        
        return when {
            candidateLevel == jobLevel -> SeniorityMatch.EXACT
            candidateLevel < jobLevel -> SeniorityMatch.STRETCH_UP
            else -> SeniorityMatch.OVERQUALIFIED
        }
    }
    
    /**
     * Compute tech stack depth score.
     * Rewards candidates with deep ecosystem knowledge.
     */
    private fun computeTechStackDepth(
        candidateSkills: Set<String>,
        jobSkills: Set<String>
    ): Double {
        if (jobSkills.isEmpty()) return 0.5 // Neutral if no requirements
        
        // Find which ecosystems the job uses
        val jobEcosystems = mutableSetOf<String>()
        for (skill in jobSkills) {
            taxonomy.getTaxonomyInfo(skill)?.let { 
                jobEcosystems.add(it.subcategory)
            }
        }
        
        if (jobEcosystems.isEmpty()) return 0.5
        
        // Count candidate's skills in those ecosystems
        var ecosystemSkillCount = 0
        for (skill in candidateSkills) {
            taxonomy.getTaxonomyInfo(skill)?.let { 
                if (it.subcategory in jobEcosystems) {
                    ecosystemSkillCount++
                }
            }
        }
        
        // More skills in relevant ecosystems = higher score
        // Cap at 5 skills for max score
        return (ecosystemSkillCount.toDouble() / 5.0).coerceIn(0.0, 1.0)
    }
    
    /**
     * Rank multiple jobs for a candidate.
     * Returns jobs sorted by relevance score (descending).
     */
    fun rankJobs(
        candidate: CandidateProfile,
        jobs: List<JobRequirements>
    ): List<MatchBreakdown> {
        Log.d(TAG, "Ranking ${jobs.size} jobs for candidate")
        
        return jobs
            .map { job -> computeMatch(candidate, job) }
            .sortedByDescending { it.finalScore }
    }
    
    /**
     * Extract candidate profile from UserProfile.
     */
    fun extractCandidateProfile(userProfile: com.swipeapply.app.data.model.UserProfile): CandidateProfile {
        val allSkills = (userProfile.skills + userProfile.techStack).toSet()
        val normalizedSkills = normalizer.normalizeAll(allSkills)
        
        // Extract years of experience from experience items
        val yearsOfExperience = estimateYearsOfExperience(userProfile.experience)
        
        // Extract roles from experience
        val roles = userProfile.experience.map { it.role }
        
        // Extract domains (could be enhanced based on company/industry)
        val domains = inferDomains(userProfile)
        
        return CandidateProfile(
            normalizedSkills = normalizedSkills,
            techStack = normalizer.normalizeAll(userProfile.techStack.toSet()),
            yearsOfExperience = yearsOfExperience,
            roles = roles,
            domains = domains
        )
    }
    
    /**
     * Estimate total years of experience from experience items.
     */
    private fun estimateYearsOfExperience(experience: List<com.swipeapply.app.data.model.ExperienceItem>): Int {
        var totalYears = 0
        for (exp in experience) {
            // Try to parse duration like "2 years", "3 months", "2019-2022"
            val duration = exp.duration.lowercase()
            val yearMatch = Regex("(\\d+)\\s*year").find(duration)
            val monthMatch = Regex("(\\d+)\\s*month").find(duration)
            val rangeMatch = Regex("(\\d{4})\\s*[-–]\\s*(\\d{4}|present)").find(duration)
            
            when {
                yearMatch != null -> {
                    totalYears += yearMatch.groupValues[1].toIntOrNull() ?: 0
                }
                rangeMatch != null -> {
                    val start = rangeMatch.groupValues[1].toIntOrNull() ?: 0
                    val end = if (rangeMatch.groupValues[2].lowercase() == "present") {
                        java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                    } else {
                        rangeMatch.groupValues[2].toIntOrNull() ?: start
                    }
                    totalYears += (end - start).coerceAtLeast(0)
                }
                monthMatch != null -> {
                    totalYears += (monthMatch.groupValues[1].toIntOrNull() ?: 0) / 12
                }
            }
        }
        return totalYears
    }
    
    /**
     * Infer domains from profile (companies, roles, projects).
     */
    private fun inferDomains(profile: com.swipeapply.app.data.model.UserProfile): Set<String> {
        val domains = mutableSetOf<String>()
        val domainKeywords = mapOf(
            "fintech" to listOf("bank", "payment", "trading", "finance", "fintech"),
            "healthcare" to listOf("health", "medical", "hospital", "pharma"),
            "ecommerce" to listOf("shop", "retail", "commerce", "marketplace"),
            "saas" to listOf("saas", "b2b", "enterprise", "subscription"),
            "social" to listOf("social", "community", "network", "messaging"),
            "gaming" to listOf("game", "gaming", "esports"),
            "edtech" to listOf("education", "learning", "school", "edtech")
        )
        
        val searchText = (
            profile.experience.joinToString(" ") { "${it.company} ${it.role} ${it.description}" } +
            profile.projects.joinToString(" ") { "${it.name} ${it.description}" }
        ).lowercase()
        
        for ((domain, keywords) in domainKeywords) {
            if (keywords.any { searchText.contains(it) }) {
                domains.add(domain)
            }
        }
        
        return domains
    }
    
    /**
     * Extract job requirements from JobCard.
     */
    fun extractJobRequirements(jobCard: com.swipeapply.app.data.model.JobCard): JobRequirements {
        // Parse role description for requirements
        val description = jobCard.roleDescription.lowercase()
        
        // Extract required vs preferred skills from tech stack
        // For simplicity, treat all as required (could be enhanced with NLP)
        val requiredSkills = normalizer.normalizeAll(jobCard.techStack.toSet())
        
        // Estimate experience from title/description
        val (minExp, maxExp) = estimateExperienceFromTitle(jobCard.title, description)
        
        // Extract seniority from title
        val seniority = extractSeniority(jobCard.title)
        
        return JobRequirements(
            jobId = jobCard.id,
            requiredSkills = requiredSkills,
            preferredSkills = emptySet(), // Would need NER to distinguish
            minExperience = minExp,
            maxExperience = maxExp,
            targetRole = jobCard.title,
            seniority = seniority,
            domain = null // Would need classification
        )
    }
    
    private fun estimateExperienceFromTitle(title: String, description: String): Pair<Int, Int?> {
        val text = "$title $description".lowercase()
        
        // Look for explicit year requirements
        val expMatch = Regex("(\\d+)\\+?\\s*years?").find(text)
        if (expMatch != null) {
            val years = expMatch.groupValues[1].toIntOrNull() ?: 0
            return Pair(years, years + 3)
        }
        
        // Infer from seniority
        return when {
            text.contains("senior") || text.contains("sr.") -> Pair(5, null)
            text.contains("lead") || text.contains("principal") -> Pair(7, null)
            text.contains("junior") || text.contains("jr.") -> Pair(0, 2)
            text.contains("intern") -> Pair(0, 0)
            else -> Pair(2, 5) // Mid-level default
        }
    }
    
    private fun extractSeniority(title: String): String {
        val titleLower = title.lowercase()
        return when {
            titleLower.contains("intern") -> "intern"
            titleLower.contains("junior") || titleLower.contains("jr.") -> "junior"
            titleLower.contains("senior") || titleLower.contains("sr.") -> "senior"
            titleLower.contains("staff") -> "staff"
            titleLower.contains("principal") -> "principal"
            titleLower.contains("lead") -> "lead"
            titleLower.contains("manager") -> "manager"
            titleLower.contains("director") -> "director"
            else -> "mid"
        }
    }
}
