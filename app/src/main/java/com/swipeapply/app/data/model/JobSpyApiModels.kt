package com.swipeapply.app.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * API response models for JobSpy API (api.sudhirsharma.dev)
 */
data class JobSpyApiResponse(
    @SerializedName("count")
    val count: Int,
    @SerializedName("page")
    val page: Int,
    @SerializedName("page_size")
    val pageSize: Int,
    @SerializedName("total_pages")
    val totalPages: Int,
    @SerializedName("jobs")
    val jobs: List<JobSpyJob>
)

data class JobSpyJob(
    @SerializedName("id")
    val id: String,
    @SerializedName("source")
    val source: String? = null,
    @SerializedName("title")
    val title: String,
    @SerializedName("company")
    val company: String,
    @SerializedName("location")
    val location: String? = null,
    @SerializedName("city")
    val city: String? = null,
    @SerializedName("state")
    val state: String? = null,
    @SerializedName("country")
    val country: String? = null,
    @SerializedName("job_type")
    val jobType: String? = null,
    @SerializedName("is_remote")
    val isRemote: Boolean = false,
    @SerializedName("date_posted")
    val datePosted: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("salary_min")
    val salaryMin: Long? = null,
    @SerializedName("salary_max")
    val salaryMax: Long? = null,
    @SerializedName("salary_currency")
    val salaryCurrency: String? = null,
    @SerializedName("salary_period")
    val salaryPeriod: String? = null,
    @SerializedName("job_url")
    val jobUrl: String? = null,
    @SerializedName("company_url")
    val companyUrl: String? = null,
    @SerializedName("logo_url")
    val logoUrl: String? = null,
    @SerializedName("experience_level")
    val experienceLevel: String? = null,
    @SerializedName("company_industry")
    val companyIndustry: String? = null,
    @SerializedName("emails")
    val emails: List<String>? = null
)
