package com.swipeapply.app.data.ranking

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.IOException

/**
 * Configuration-driven skill normalizer.
 * Maps aliases, abbreviations, and variations to canonical skill names.
 * 
 * DESIGN PRINCIPLE: All mappings are loaded from JSON config, not hardcoded.
 * This ensures:
 * - Easy extensibility without code changes
 * - Auditability of all normalization rules
 * - Testability and reproducibility
 */
class SkillNormalizer(context: Context) {
    
    companion object {
        private const val TAG = "SkillNormalizer"
        private const val CONFIG_FILE = "skill_mappings.json"
        
        @Volatile
        private var INSTANCE: SkillNormalizer? = null
        
        fun getInstance(context: Context): SkillNormalizer {
            return INSTANCE ?: synchronized(this) {
                SkillNormalizer(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // alias -> canonical name
    private val aliasToCanonical: MutableMap<String, String> = mutableMapOf()
    
    // canonical name -> list of aliases (for reverse lookup)
    private val canonicalToAliases: MutableMap<String, MutableSet<String>> = mutableMapOf()
    
    // ecosystem expansions: "aws" -> [EC2, S3, Lambda, IAM, ...]
    private val ecosystemExpansions: MutableMap<String, Set<String>> = mutableMapOf()
    
    init {
        loadMappingsFromAssets(context)
        loadDefaultMappings() // Fallback if JSON not available
    }
    
    /**
     * Load skill mappings from assets/skill_mappings.json
     */
    private fun loadMappingsFromAssets(context: Context) {
        try {
            val jsonString = context.assets.open(CONFIG_FILE).bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            
            // Load aliases
            if (json.has("aliases")) {
                val aliases = json.getJSONObject("aliases")
                aliases.keys().forEach { canonical ->
                    val variations = aliases.getJSONArray(canonical)
                    canonicalToAliases[canonical.lowercase()] = mutableSetOf()
                    for (i in 0 until variations.length()) {
                        val alias = variations.getString(i).lowercase()
                        aliasToCanonical[alias] = canonical.lowercase()
                        canonicalToAliases[canonical.lowercase()]?.add(alias)
                    }
                    // Map canonical to itself
                    aliasToCanonical[canonical.lowercase()] = canonical.lowercase()
                }
            }
            
            // Load ecosystem expansions
            if (json.has("ecosystems")) {
                val ecosystems = json.getJSONObject("ecosystems")
                ecosystems.keys().forEach { ecosystem ->
                    val components = ecosystems.getJSONArray(ecosystem)
                    val componentSet = mutableSetOf<String>()
                    for (i in 0 until components.length()) {
                        componentSet.add(components.getString(i).lowercase())
                    }
                    ecosystemExpansions[ecosystem.lowercase()] = componentSet
                }
            }
            
            Log.d(TAG, "Loaded ${aliasToCanonical.size} skill aliases and ${ecosystemExpansions.size} ecosystems from config")
            
        } catch (e: IOException) {
            Log.w(TAG, "skill_mappings.json not found, using defaults: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing skill_mappings.json: ${e.message}")
        }
    }
    
    /**
     * Fallback default mappings if JSON config is missing
     */
    private fun loadDefaultMappings() {
        // Only add if not already loaded from JSON
        val defaults = mapOf(
            // JavaScript ecosystem
            "javascript" to listOf("js", "ecmascript", "es6", "es2015", "es2020"),
            "typescript" to listOf("ts"),
            "node.js" to listOf("node", "nodejs", "node js"),
            "react" to listOf("reactjs", "react.js", "react js"),
            "vue.js" to listOf("vue", "vuejs", "vue js"),
            "angular" to listOf("angularjs", "angular.js", "angular 2+"),
            "next.js" to listOf("next", "nextjs"),
            
            // Python ecosystem
            "python" to listOf("py", "python3", "python 3"),
            "django" to listOf("django rest", "drf"),
            "flask" to listOf("flask-restful"),
            "fastapi" to listOf("fast api"),
            
            // JVM
            "kotlin" to listOf("kt"),
            "java" to listOf("java 8", "java 11", "java 17", "jdk"),
            "spring" to listOf("spring boot", "springboot", "spring framework"),
            
            // Go
            "go" to listOf("golang", "go-lang", "go lang"),
            
            // Rust
            "rust" to listOf("rust-lang", "rustlang"),
            
            // Mobile
            "android" to listOf("android sdk", "android development"),
            "ios" to listOf("ios development", "iphone development"),
            "swift" to listOf("swift ui", "swiftui"),
            "react native" to listOf("reactnative", "react-native", "rn"),
            "flutter" to listOf("dart/flutter"),
            
            // Databases
            "postgresql" to listOf("postgres", "psql", "pg"),
            "mongodb" to listOf("mongo", "mongo db"),
            "mysql" to listOf("my sql", "mariadb"),
            "redis" to listOf("redis cache"),
            "elasticsearch" to listOf("elastic", "elastic search", "es"),
            
            // Cloud
            "aws" to listOf("amazon web services", "amazon aws"),
            "gcp" to listOf("google cloud", "google cloud platform"),
            "azure" to listOf("microsoft azure", "ms azure"),
            
            // DevOps
            "docker" to listOf("containerization", "docker containers"),
            "kubernetes" to listOf("k8s", "kube"),
            "terraform" to listOf("tf", "infrastructure as code"),
            "ci/cd" to listOf("cicd", "ci cd", "continuous integration"),
            
            // ML/AI
            "machine learning" to listOf("ml", "machine-learning"),
            "deep learning" to listOf("dl", "neural networks"),
            "tensorflow" to listOf("tf", "tensor flow"),
            "pytorch" to listOf("torch", "py torch"),
            
            // Other
            "graphql" to listOf("graph ql", "gql"),
            "rest api" to listOf("restful", "rest", "restful api"),
            "git" to listOf("github", "gitlab", "version control"),
            "sql" to listOf("structured query language")
        )
        
        defaults.forEach { (canonical, aliases) ->
            if (!canonicalToAliases.containsKey(canonical)) {
                canonicalToAliases[canonical] = aliases.toMutableSet()
                aliases.forEach { alias ->
                    aliasToCanonical[alias] = canonical
                }
                aliasToCanonical[canonical] = canonical
            }
        }
        
        // Default ecosystem expansions
        val defaultEcosystems = mapOf(
            "aws" to setOf("ec2", "s3", "lambda", "iam", "rds", "dynamodb", "sqs", "sns", "cloudfront", "route53", "eks", "ecs"),
            "gcp" to setOf("compute engine", "cloud storage", "cloud functions", "bigquery", "pubsub", "gke", "cloud run"),
            "azure" to setOf("azure functions", "blob storage", "cosmos db", "aks", "azure sql", "service bus")
        )
        
        defaultEcosystems.forEach { (ecosystem, components) ->
            if (!ecosystemExpansions.containsKey(ecosystem)) {
                ecosystemExpansions[ecosystem] = components
            }
        }
    }
    
    /**
     * Normalize a single skill to its canonical form.
     * Returns the canonical name or the original if no mapping exists.
     */
    fun normalize(skill: String): String {
        val cleaned = skill.lowercase().trim()
            .replace(Regex("[^a-z0-9.+#/ -]"), "") // Remove special chars except common ones
            .replace(Regex("\\s+"), " ")           // Normalize whitespace
        
        return aliasToCanonical[cleaned] ?: cleaned
    }
    
    /**
     * Normalize a set of skills.
     */
    fun normalizeAll(skills: Collection<String>): Set<String> {
        return skills.map { normalize(it) }.toSet()
    }
    
    /**
     * Expand ecosystem skills (e.g., "aws" -> includes EC2, S3, Lambda, etc.)
     * Returns the original skill plus any ecosystem components.
     */
    fun expandEcosystem(skill: String): Set<String> {
        val normalized = normalize(skill)
        val result = mutableSetOf(normalized)
        ecosystemExpansions[normalized]?.let { result.addAll(it) }
        return result
    }
    
    /**
     * Check if two skills are equivalent (same canonical form).
     */
    fun areEquivalent(skill1: String, skill2: String): Boolean {
        return normalize(skill1) == normalize(skill2)
    }
    
    /**
     * Get all known aliases for a canonical skill.
     */
    fun getAliases(canonicalSkill: String): Set<String> {
        val normalized = normalize(canonicalSkill)
        return canonicalToAliases[normalized] ?: emptySet()
    }
    
    /**
     * Check if a skill is a known ecosystem skill.
     */
    fun isEcosystemSkill(skill: String): Boolean {
        return ecosystemExpansions.containsKey(normalize(skill))
    }
    
    /**
     * Get ecosystem components for a skill.
     */
    fun getEcosystemComponents(skill: String): Set<String> {
        return ecosystemExpansions[normalize(skill)] ?: emptySet()
    }
}
