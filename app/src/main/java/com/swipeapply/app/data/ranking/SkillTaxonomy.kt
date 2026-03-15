package com.swipeapply.app.data.ranking

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.IOException

/**
 * Skill Taxonomy for partial matching and adjacent skill credit.
 * 
 * DESIGN PRINCIPLE:
 * - Groups related technologies into categories
 * - Allows partial credit for adjacent/related skills
 * - Prevents exact-keyword dependency (Vue.js dev gets credit for React roles)
 * - Configuration-driven via JSON, not hardcoded
 * 
 * Similarity Rules:
 * - SAME skill (exact/normalized): 1.0
 * - SAME subcategory (React/Vue): 0.7
 * - SAME category (Frontend frameworks): 0.4
 * - SAME domain (Web development): 0.2
 * - NO relation: 0.0
 */
class SkillTaxonomy(context: Context) {
    
    companion object {
        private const val TAG = "SkillTaxonomy"
        private const val CONFIG_FILE = "skill_taxonomy.json"
        
        // Similarity weights for different relationship levels
        const val EXACT_MATCH = 1.0
        const val SAME_SUBCATEGORY = 0.7
        const val SAME_CATEGORY = 0.4
        const val SAME_DOMAIN = 0.2
        const val NO_RELATION = 0.0
        
        @Volatile
        private var INSTANCE: SkillTaxonomy? = null
        
        fun getInstance(context: Context): SkillTaxonomy {
            return INSTANCE ?: synchronized(this) {
                SkillTaxonomy(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // skill -> TaxonomyNode (category, subcategory, domain)
    private val skillTaxonomy: MutableMap<String, TaxonomyNode> = mutableMapOf()
    
    // subcategory -> list of skills in that subcategory
    private val subcategoryMembers: MutableMap<String, MutableSet<String>> = mutableMapOf()
    
    // category -> list of skills in that category
    private val categoryMembers: MutableMap<String, MutableSet<String>> = mutableMapOf()
    
    // domain -> list of skills in that domain
    private val domainMembers: MutableMap<String, MutableSet<String>> = mutableMapOf()
    
    data class TaxonomyNode(
        val skill: String,
        val subcategory: String,
        val category: String,
        val domain: String
    )
    
    init {
        loadTaxonomyFromAssets(context)
        loadDefaultTaxonomy()
    }
    
    private fun loadTaxonomyFromAssets(context: Context) {
        try {
            val jsonString = context.assets.open(CONFIG_FILE).bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            
            // Expected structure:
            // { "domains": { "web": { "categories": { "frontend": { "subcategories": { "react-ecosystem": ["react", "redux", "next.js"] } } } } } }
            
            if (json.has("domains")) {
                val domains = json.getJSONObject("domains")
                domains.keys().forEach { domain ->
                    val domainObj = domains.getJSONObject(domain)
                    if (domainObj.has("categories")) {
                        val categories = domainObj.getJSONObject("categories")
                        categories.keys().forEach { category ->
                            val categoryObj = categories.getJSONObject(category)
                            if (categoryObj.has("subcategories")) {
                                val subcategories = categoryObj.getJSONObject("subcategories")
                                subcategories.keys().forEach { subcategory ->
                                    val skills = subcategories.getJSONArray(subcategory)
                                    for (i in 0 until skills.length()) {
                                        val skill = skills.getString(i).lowercase()
                                        registerSkill(skill, subcategory, category, domain)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Log.d(TAG, "Loaded ${skillTaxonomy.size} skills into taxonomy from config")
            
        } catch (e: IOException) {
            Log.w(TAG, "skill_taxonomy.json not found, using defaults")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing skill_taxonomy.json: ${e.message}")
        }
    }
    
    private fun registerSkill(skill: String, subcategory: String, category: String, domain: String) {
        val node = TaxonomyNode(skill, subcategory, category, domain)
        skillTaxonomy[skill] = node
        
        subcategoryMembers.getOrPut(subcategory) { mutableSetOf() }.add(skill)
        categoryMembers.getOrPut(category) { mutableSetOf() }.add(skill)
        domainMembers.getOrPut(domain) { mutableSetOf() }.add(skill)
    }
    
    /**
     * Default taxonomy covering major tech stacks
     */
    private fun loadDefaultTaxonomy() {
        // Only add if not already loaded from JSON
        if (skillTaxonomy.isNotEmpty()) return
        
        // ===== WEB DEVELOPMENT DOMAIN =====
        
        // Frontend - React ecosystem
        listOf("react", "next.js", "redux", "react query", "react router").forEach {
            registerSkill(it, "react-ecosystem", "frontend-frameworks", "web")
        }
        
        // Frontend - Vue ecosystem
        listOf("vue.js", "nuxt.js", "vuex", "pinia", "vue router").forEach {
            registerSkill(it, "vue-ecosystem", "frontend-frameworks", "web")
        }
        
        // Frontend - Angular ecosystem
        listOf("angular", "rxjs", "ngrx", "angular material").forEach {
            registerSkill(it, "angular-ecosystem", "frontend-frameworks", "web")
        }
        
        // Frontend - Core
        listOf("javascript", "typescript", "html", "css", "sass", "tailwind").forEach {
            registerSkill(it, "frontend-core", "frontend", "web")
        }
        
        // Backend - Node.js ecosystem
        listOf("node.js", "express", "nestjs", "fastify", "koa").forEach {
            registerSkill(it, "nodejs-ecosystem", "backend-frameworks", "web")
        }
        
        // Backend - Python ecosystem
        listOf("django", "flask", "fastapi", "celery").forEach {
            registerSkill(it, "python-web", "backend-frameworks", "web")
        }
        
        // Backend - Java/Kotlin ecosystem
        listOf("spring", "spring boot", "micronaut", "quarkus", "ktor").forEach {
            registerSkill(it, "jvm-web", "backend-frameworks", "web")
        }
        
        // Backend - Go ecosystem
        listOf("go", "gin", "echo", "fiber").forEach {
            registerSkill(it, "go-ecosystem", "backend-frameworks", "web")
        }
        
        // Backend - Ruby ecosystem
        listOf("ruby", "rails", "sinatra").forEach {
            registerSkill(it, "ruby-ecosystem", "backend-frameworks", "web")
        }
        
        // ===== MOBILE DOMAIN =====
        
        // Android
        listOf("android", "kotlin", "jetpack compose", "android studio").forEach {
            registerSkill(it, "android-native", "mobile-native", "mobile")
        }
        
        // iOS
        listOf("ios", "swift", "swiftui", "uikit", "xcode").forEach {
            registerSkill(it, "ios-native", "mobile-native", "mobile")
        }
        
        // Cross-platform
        listOf("react native", "flutter", "dart", "xamarin", "kotlin multiplatform").forEach {
            registerSkill(it, "mobile-crossplatform", "mobile-frameworks", "mobile")
        }
        
        // ===== DATA DOMAIN =====
        
        // SQL Databases
        listOf("postgresql", "mysql", "sql server", "oracle", "sqlite").forEach {
            registerSkill(it, "sql-databases", "databases", "data")
        }
        
        // NoSQL Databases
        listOf("mongodb", "redis", "cassandra", "dynamodb", "couchbase").forEach {
            registerSkill(it, "nosql-databases", "databases", "data")
        }
        
        // Search & Analytics
        listOf("elasticsearch", "solr", "opensearch").forEach {
            registerSkill(it, "search-engines", "data-tools", "data")
        }
        
        // Data Processing
        listOf("apache spark", "kafka", "flink", "airflow", "dbt").forEach {
            registerSkill(it, "data-processing", "data-engineering", "data")
        }
        
        // ===== DEVOPS DOMAIN =====
        
        // Containers
        listOf("docker", "podman", "containerd").forEach {
            registerSkill(it, "containers", "containerization", "devops")
        }
        
        // Orchestration
        listOf("kubernetes", "docker swarm", "nomad", "openshift").forEach {
            registerSkill(it, "orchestration", "containerization", "devops")
        }
        
        // CI/CD
        listOf("jenkins", "github actions", "gitlab ci", "circleci", "travis ci").forEach {
            registerSkill(it, "cicd-tools", "cicd", "devops")
        }
        
        // IaC
        listOf("terraform", "pulumi", "cloudformation", "ansible", "chef").forEach {
            registerSkill(it, "infrastructure-as-code", "infrastructure", "devops")
        }
        
        // ===== CLOUD DOMAIN =====
        
        // AWS
        listOf("aws", "ec2", "s3", "lambda", "rds", "dynamodb", "eks", "ecs").forEach {
            registerSkill(it, "aws-services", "cloud-providers", "cloud")
        }
        
        // GCP
        listOf("gcp", "compute engine", "cloud storage", "cloud functions", "gke", "bigquery").forEach {
            registerSkill(it, "gcp-services", "cloud-providers", "cloud")
        }
        
        // Azure
        listOf("azure", "azure functions", "blob storage", "cosmos db", "aks").forEach {
            registerSkill(it, "azure-services", "cloud-providers", "cloud")
        }
        
        // ===== ML/AI DOMAIN =====
        
        // ML Frameworks
        listOf("tensorflow", "pytorch", "keras", "scikit-learn", "xgboost").forEach {
            registerSkill(it, "ml-frameworks", "machine-learning", "ml-ai")
        }
        
        // Deep Learning
        listOf("deep learning", "neural networks", "cnn", "rnn", "transformer").forEach {
            registerSkill(it, "deep-learning", "machine-learning", "ml-ai")
        }
        
        // NLP
        listOf("nlp", "huggingface", "bert", "gpt", "langchain", "llm").forEach {
            registerSkill(it, "nlp", "ai-domains", "ml-ai")
        }
        
        // MLOps
        listOf("mlflow", "kubeflow", "sagemaker", "vertex ai").forEach {
            registerSkill(it, "mlops", "ml-infrastructure", "ml-ai")
        }
        
        // Data Science
        listOf("python", "pandas", "numpy", "matplotlib", "jupyter").forEach {
            registerSkill(it, "data-science-tools", "data-science", "ml-ai")
        }
        
        Log.d(TAG, "Loaded ${skillTaxonomy.size} default taxonomy entries")
    }
    
    /**
     * Calculate similarity between two skills based on taxonomy.
     * Returns a value between 0.0 and 1.0.
     */
    fun calculateSimilarity(skill1: String, skill2: String): Double {
        val s1 = skill1.lowercase()
        val s2 = skill2.lowercase()
        
        // Exact match
        if (s1 == s2) return EXACT_MATCH
        
        val node1 = skillTaxonomy[s1]
        val node2 = skillTaxonomy[s2]
        
        // If either skill is not in taxonomy, no partial credit
        if (node1 == null || node2 == null) return NO_RELATION
        
        // Same subcategory (e.g., React and Next.js)
        if (node1.subcategory == node2.subcategory) return SAME_SUBCATEGORY
        
        // Same category (e.g., React and Vue.js - both frontend frameworks)
        if (node1.category == node2.category) return SAME_CATEGORY
        
        // Same domain (e.g., React and Express - both web development)
        if (node1.domain == node2.domain) return SAME_DOMAIN
        
        return NO_RELATION
    }
    
    /**
     * Find the best matching skill from a candidate's skills for a required skill.
     * Returns the skill and its similarity score.
     */
    fun findBestMatch(requiredSkill: String, candidateSkills: Set<String>): Pair<String?, Double> {
        val required = requiredSkill.lowercase()
        var bestMatch: String? = null
        var bestScore = 0.0
        
        for (candidate in candidateSkills) {
            val score = calculateSimilarity(required, candidate.lowercase())
            if (score > bestScore) {
                bestScore = score
                bestMatch = candidate
            }
            // Early exit on exact match
            if (score == EXACT_MATCH) break
        }
        
        return Pair(bestMatch, bestScore)
    }
    
    /**
     * Get all skills in the same subcategory.
     */
    fun getSubcategoryPeers(skill: String): Set<String> {
        val node = skillTaxonomy[skill.lowercase()] ?: return emptySet()
        return subcategoryMembers[node.subcategory] ?: emptySet()
    }
    
    /**
     * Get all skills in the same category.
     */
    fun getCategoryPeers(skill: String): Set<String> {
        val node = skillTaxonomy[skill.lowercase()] ?: return emptySet()
        return categoryMembers[node.category] ?: emptySet()
    }
    
    /**
     * Get taxonomy info for a skill.
     */
    fun getTaxonomyInfo(skill: String): TaxonomyNode? {
        return skillTaxonomy[skill.lowercase()]
    }
    
    /**
     * Check if skill exists in taxonomy.
     */
    fun isKnownSkill(skill: String): Boolean {
        return skillTaxonomy.containsKey(skill.lowercase())
    }
}
