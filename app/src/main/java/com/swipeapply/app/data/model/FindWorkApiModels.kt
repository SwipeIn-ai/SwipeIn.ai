package com.swipeapply.app.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * API response models for FindWork.dev API
 */
data class FindWorkApiResponse(
    @SerializedName("count")
    val count: Int,
    @SerializedName("next")
    val next: String?,
    @SerializedName("previous")
    val previous: String?,
    @SerializedName("results")
    val results: List<FindWorkJob>
)

data class FindWorkJob(
    @SerializedName("id")
    val id: String,
    @SerializedName("role")
    val role: String,
    @SerializedName("company_name")
    val companyName: String,
    @SerializedName("location")
    val location: String? = null,
    @SerializedName("remote")
    val remote: Boolean = false,
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("date_posted")
    val datePosted: String? = null,
    @SerializedName("keywords")
    val keywords: List<String>? = null,
    @SerializedName("source")
    val source: String? = null,
    @SerializedName("employment_type")
    val employmentType: String? = null,
    @SerializedName("logo")
    val logo: String? = null,
    @SerializedName("company_num_employees")
    val companyNumEmployees: String? = null  
)