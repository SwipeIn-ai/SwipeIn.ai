# Job Discovery System - Technical Design Document

## Executive Summary

This document describes a job discovery system with a **strict architectural separation** between deterministic job ranking and AI-powered explanations. The core constraint is that **AI must NOT participate in job ranking, scoring, sorting, or prioritization in any form**.

---

## 1. System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           USER INTERFACE                                     │
│  ┌─────────────────────┐    ┌─────────────────────────────────────────────┐│
│  │   Job Card Stack    │    │   "Why this job?" Button (Lazy-loaded)     ││
│  │   (Ranked Order)    │    │   Triggers AI Explanation                   ││
│  └──────────┬──────────┘    └─────────────────────┬─────────────────────┘│
└─────────────┼─────────────────────────────────────┼─────────────────────────┘
              │                                     │
              │ DETERMINISTIC                       │ AI EXPLANATION
              │ (NO AI)                             │ (LAZY, CACHED)
              ▼                                     ▼
┌─────────────────────────────┐     ┌─────────────────────────────────────────┐
│   JOB RANKING ENGINE        │     │   FIT REASONING MODULE                  │
│   (JobRankingEngine.kt)     │     │   (FitReasoningModule.kt)               │
│                             │     │                                         │
│   • Skill Matching (50%)    │────▶│   Inputs:                               │
│   • Experience Match (25%)  │     │   • Candidate Profile                   │
│   • Role Alignment (15%)    │     │   • Job Requirements                    │
│   • Tech Depth (10%)        │     │   • PRECOMPUTED MatchBreakdown          │
│                             │     │                                         │
│   Output: Sorted List       │     │   Output: Human-readable explanation    │
│   + MatchBreakdown          │     │   (Strengths, Gaps, Conclusion)         │
└──────────────┬──────────────┘     └─────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        SUPPORT MODULES                                       │
│  ┌─────────────────────┐    ┌─────────────────────────────────────────────┐│
│  │  SkillNormalizer    │    │  SkillTaxonomy                              ││
│  │  (skill_mappings.   │    │  (skill_taxonomy.json)                      ││
│  │   json driven)      │    │  Groups related skills, enables             ││
│  │  Maps aliases to    │    │  partial credit for adjacent skills         ││
│  │  canonical names    │    │                                             ││
│  └─────────────────────┘    └─────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Job Ranking Engine (No AI)

### 2.1 Objective

Rank jobs using a **deterministic, exhaustive, and extensible** similarity-based scoring function.

### 2.2 Feature Representation

```kotlin
// Candidate Profile (normalized)
data class CandidateProfile(
    val normalizedSkills: Set<String>,     // Canonical skill names
    val techStack: Set<String>,            // Tech stack (normalized)
    val yearsOfExperience: Int,
    val roles: List<String>,               // Job titles held
    val domains: Set<String>               // Industries worked in
)

// Job Requirements (normalized)
data class JobRequirements(
    val jobId: String,
    val requiredSkills: Set<String>,       // Must-have (normalized)
    val preferredSkills: Set<String>,      // Nice-to-have (normalized)
    val minExperience: Int,
    val maxExperience: Int?,
    val targetRole: String,
    val seniority: String,
    val domain: String?
)
```

### 2.3 Normalization Layer

**Configuration-driven** (not hardcoded) via `skill_mappings.json`:

```json
{
  "aliases": {
    "javascript": ["js", "ecmascript", "es6"],
    "node.js": ["node", "nodejs"],
    "go": ["golang", "go-lang"]
  },
  "ecosystems": {
    "aws": ["ec2", "s3", "lambda", "iam", "rds"]
  }
}
```

### 2.4 Skill Taxonomy

Groups related skills for **partial credit matching**:

| Relationship       | Similarity Score |
|--------------------|------------------|
| Exact match        | 1.0              |
| Same subcategory   | 0.7              |
| Same category      | 0.4              |
| Same domain        | 0.2              |
| No relation        | 0.0              |

Example: React developer gets 0.7 credit for Vue.js requirement (same subcategory: frontend-frameworks).

### 2.5 Ranking Algorithm (Pseudo-code)

```
FUNCTION computeMatch(candidate, job):
    // STEP 1: Normalize all skills
    candidateSkills = normalize(candidate.skills ∪ candidate.techStack)
    requiredSkills = normalize(job.requiredSkills)
    preferredSkills = normalize(job.preferredSkills)
    
    // STEP 2: Skill Match (50% weight)
    skillScore = 0
    FOR each skill IN requiredSkills:
        bestMatch = findBestMatch(skill, candidateSkills)
        IF bestMatch.similarity >= 0.4:  // At least same category
            skillScore += bestMatch.similarity
    requiredScore = skillScore / |requiredSkills|
    
    // Repeat for preferred skills with lower weight
    preferredScore = computePreferredScore(...)
    
    skillMatchScore = 0.7 * requiredScore + 0.3 * preferredScore
    
    // STEP 3: Experience Match (25% weight)
    experienceScore = CASE:
        candidate.years >= job.minYears: 1.0
        candidate.years >= job.minYears - 2: 0.7 - 0.1 * gap
        OTHERWISE: 0.3 - 0.05 * (gap - 2)
    
    // STEP 4: Role Alignment (15% weight)
    roleScore = computeRoleAlignment(candidate.roles, job.targetRole)
    
    // STEP 5: Tech Stack Depth (10% weight)
    depthScore = countEcosystemSkills(candidateSkills, job) / 5
    
    // STEP 6: Final Score
    finalScore = 0.50 * skillMatchScore
               + 0.25 * experienceScore
               + 0.15 * roleScore
               + 0.10 * depthScore
    
    // STEP 7: Categorize
    fitCategory = CASE:
        finalScore >= 0.75: STRONG
        finalScore >= 0.50: MODERATE
        finalScore >= 0.30: STRETCH
        OTHERWISE: WEAK
    
    RETURN MatchBreakdown(finalScore, skillMatch, experienceMatch, ...)
```

### 2.6 Output Format

```kotlin
data class MatchBreakdown(
    val jobId: String,
    val finalScore: Double,                // 0.0 to 1.0
    val skillMatch: SkillMatchResult,      // Detailed breakdown
    val experienceMatch: ExperienceMatchResult,
    val roleAlignment: RoleAlignmentResult,
    val overallFit: FitCategory,           // STRONG/MODERATE/STRETCH/WEAK
    val scoreComponents: ScoreComponents   // For auditability
)
```

---

## 3. AI Fit Reasoning Module (Explainability Only)

### 3.1 Critical Safety Constraints

```
╔════════════════════════════════════════════════════════════════╗
║  AI LIMITATIONS (STRICTLY ENFORCED)                            ║
╠════════════════════════════════════════════════════════════════╣
║  ✗ Do NOT score, rank, or compare jobs                         ║
║  ✗ Do NOT introduce new skills or assumptions                  ║
║  ✗ Do NOT infer qualifications beyond provided data            ║
║  ✓ Base ALL reasoning on PRECOMPUTED MatchBreakdown            ║
║  ✓ Clearly separate Strengths from Gaps                        ║
║  ✓ Maintain neutral, trust-building tone                       ║
╚════════════════════════════════════════════════════════════════╝
```

### 3.2 AI Prompt Template

```
## SYSTEM INSTRUCTIONS (STRICT)

You are an AI assistant generating job fit explanations. You MUST follow these rules:

1. Do NOT score, rank, or compare jobs
2. Do NOT introduce skills or qualifications not in the provided data
3. Base ALL reasoning strictly on the Match Breakdown provided
4. Clearly separate Strengths from Gaps
5. Maintain a neutral, trust-building tone

## INPUT: PRECOMPUTED MATCH BREAKDOWN

Overall Fit: ${matchBreakdown.overallFit.label}
Final Score: ${matchBreakdown.finalScore}

Skill Match: ${matchBreakdown.skillMatch.matchPercentage}%
- Required Matched: ${matchBreakdown.skillMatch.requiredMatched}
- Required Missing: ${matchBreakdown.skillMatch.requiredMissing}

Experience: ${matchBreakdown.experienceMatch.experienceFit}
- Candidate: ${candidateYears} years
- Required: ${requiredYears} years

## OUTPUT FORMAT

{
  "fitSummary": "Strong Fit | Moderate Fit | Stretch Role | Weak Fit",
  "strengthPoints": ["point 1", "point 2"],
  "gapPoints": ["gap 1", "gap 2"],
  "experienceNote": "One sentence about experience alignment",
  "conclusion": "2-3 sentences explaining why this role suits the candidate"
}
```

### 3.3 Output Format

```kotlin
data class FitExplanation(
    val jobId: String,
    val fitSummary: String,           // "Strong Fit", "Moderate Fit", etc.
    val strengthPoints: List<String>, // What matches well
    val gapPoints: List<String>,      // What's missing
    val experienceNote: String,       // Experience alignment
    val conclusion: String            // Brief summary
)
```

---

## 4. Example Input/Output

### Input: Candidate Profile

```json
{
  "skills": ["React", "TypeScript", "Node.js", "PostgreSQL"],
  "techStack": ["Next.js", "Tailwind", "Docker", "AWS"],
  "experience": [
    {"role": "Frontend Developer", "company": "TechCorp", "duration": "3 years"},
    {"role": "Full Stack Developer", "company": "StartupX", "duration": "2 years"}
  ]
}
```

### Input: Job Requirements

```json
{
  "title": "Senior Frontend Engineer",
  "requiredSkills": ["React", "TypeScript", "GraphQL", "Testing"],
  "preferredSkills": ["Next.js", "Storybook", "CI/CD"],
  "minExperience": 5
}
```

### Output: MatchBreakdown

```json
{
  "finalScore": 0.72,
  "overallFit": "MODERATE",
  "skillMatch": {
    "matchPercentage": 0.75,
    "requiredMatched": [
      {"candidateSkill": "React", "jobSkill": "React", "matchType": "EXACT"},
      {"candidateSkill": "TypeScript", "jobSkill": "TypeScript", "matchType": "EXACT"}
    ],
    "requiredMissing": ["GraphQL", "Testing"],
    "preferredMatched": [
      {"candidateSkill": "Next.js", "jobSkill": "Next.js", "matchType": "EXACT"}
    ]
  },
  "experienceMatch": {
    "candidateYears": 5,
    "requiredYears": 5,
    "fit": "MATCHES"
  }
}
```

### Output: AI Explanation

```json
{
  "fitSummary": "Moderate Fit",
  "strengthPoints": [
    "Direct experience with React and TypeScript",
    "Experience level aligns well with the role",
    "Knowledge of Next.js matches preferred requirements"
  ],
  "gapPoints": [
    "Missing required skills: GraphQL, Testing",
    "No Storybook experience listed"
  ],
  "experienceNote": "Your 5 years of experience meets the role's requirement.",
  "conclusion": "This Senior Frontend Engineer position shows moderate alignment with your background. Your React and TypeScript expertise are strong matches, but you may need to develop GraphQL and testing skills."
}
```

---

## 5. Safety & Trust Guarantees

| Guarantee | Implementation |
|-----------|----------------|
| AI cannot change job order | Ranking happens BEFORE AI module is invoked |
| Ranking is stable | Same inputs → same outputs (deterministic) |
| Ranking is testable | Unit tests verify exact score calculations |
| Explanations are auditable | AI receives PRECOMPUTED breakdown, cannot reinterpret |
| Scores are traceable | MatchBreakdown contains full component breakdown |

---

## 6. File Structure

```
app/src/main/java/com/swipeapply/app/data/
├── ranking/
│   ├── MatchBreakdown.kt      # Data classes for match results
│   ├── SkillNormalizer.kt     # Alias/abbreviation mapping
│   ├── SkillTaxonomy.kt       # Partial matching categories
│   └── JobRankingEngine.kt    # Core deterministic algorithm
├── ai/
│   └── FitReasoningModule.kt  # AI explanation generator (ONLY)
├── service/
│   ├── JobRankingService.kt   # Integration layer (NO AI)
│   └── RankedJobService.kt    # Orchestration
└── ...

app/src/main/assets/
├── skill_mappings.json        # Alias configuration
└── skill_taxonomy.json        # Skill category hierarchy
```

---

## 7. Senior-Level Summary

### Why This Design?

1. **Regulatory Compliance**: Many jurisdictions require explainability in hiring decisions. AI-based ranking is a black box; rule-based ranking is fully auditable.

2. **User Trust**: Users can understand exactly why a job ranked higher. "Your React skills matched 100%" is more trustworthy than "AI thinks this is good."

3. **Debugging & Support**: When users ask "Why didn't I see job X first?", support can trace exact score components.

4. **Consistency**: AI models can be non-deterministic (temperature, version updates). Rule-based ranking is perfectly reproducible.

5. **Performance**: Local computation is faster than API calls. Rankings can be cached indefinitely.

6. **Cost**: No LLM API costs for ranking. AI is only invoked on-demand for explanations.

### Trade-offs Acknowledged

- **Less "magical"**: Pure keyword matching lacks semantic understanding. Mitigated by taxonomy and ecosystem expansion.
- **Manual maintenance**: Skill mappings need periodic updates. Mitigated by config-driven design.
- **Edge cases**: Unusual skill combinations may not rank well. Mitigated by partial matching.

### Why AI for Explanations Only?

- Natural language generation is where AI excels
- No ranking influence = no bias concerns
- Lazy loading = cost-effective
- Cached per user-job pair = fast repeated access
