package com.swipeapply.app.data.repository

import com.swipeapply.app.data.model.*

/**
 * Mock repository providing sample job cards for development
 */
object MockJobRepository {
    
    private val companies = listOf(
        Company(
            id = "stripe",
            name = "Stripe",
            description = "Stripe is a financial infrastructure platform for businesses. Millions of companies use Stripe to accept payments, grow revenue, and accelerate new business opportunities.",
            industry = "Fintech",
            size = CompanySize.LARGE,
            founded = 2010,
            website = "stripe.com"
        ),
        Company(
            id = "notion",
            name = "Notion",
            description = "Notion is the connected workspace where better, faster work happens. We're on a mission to make toolmaking ubiquitous.",
            industry = "Productivity",
            size = CompanySize.MEDIUM,
            founded = 2016,
            website = "notion.so"
        ),
        Company(
            id = "linear",
            name = "Linear",
            description = "Linear is the issue tracking tool you'll enjoy using. Designed to streamline software projects, sprints, and bug tracking.",
            industry = "Developer Tools",
            size = CompanySize.SMALL,
            founded = 2019,
            website = "linear.app"
        ),
        Company(
            id = "figma",
            name = "Figma",
            description = "Figma is a collaborative interface design tool that's changing the way teams create and share design work.",
            industry = "Design",
            size = CompanySize.LARGE,
            founded = 2012,
            website = "figma.com"
        ),
        Company(
            id = "vercel",
            name = "Vercel",
            description = "Vercel is the platform for frontend developers, providing the speed and reliability innovators need to create at the moment of inspiration.",
            industry = "Developer Tools",
            size = CompanySize.MEDIUM,
            founded = 2015,
            website = "vercel.com"
        ),
        Company(
            id = "supabase",
            name = "Supabase",
            description = "Supabase is an open source Firebase alternative. Start your project with a Postgres database, Authentication, instant APIs, and more.",
            industry = "Developer Tools",
            size = CompanySize.SMALL,
            founded = 2020,
            website = "supabase.com"
        ),
        Company(
            id = "mercury",
            name = "Mercury",
            description = "Mercury is a financial technology company focused on creating a seamless banking experience for startups at every stage.",
            industry = "Fintech",
            size = CompanySize.MEDIUM,
            founded = 2017,
            website = "mercury.com"
        ),
        Company(
            id = "ramp",
            name = "Ramp",
            description = "Ramp is building the next generation of finance tools—from corporate cards and expense management to bill payments and more.",
            industry = "Fintech",
            size = CompanySize.MEDIUM,
            founded = 2019,
            website = "ramp.com"
        )
    )
    
    private val _jobCards = listOf(
        JobCard(
            id = "1",
            company = companies[0],
            title = "Senior Android Engineer",
            location = "San Francisco, CA",
            locationType = LocationType.HYBRID,
            salary = SalaryRange(180000, 260000),
            techStack = listOf("Kotlin", "Jetpack Compose", "Coroutines", "Dagger Hilt"),
            isHiringNow = true,
            roleDescription = "Join Stripe's mobile team to build payment experiences used by millions. You'll work on our flagship Android SDK and consumer apps.",
            matchReason = "Your experience with Kotlin and Compose matches their tech stack. Plus, you've worked on fintech apps before.",
            introTemplate = IntroTemplate(
                subject = "Android Engineer interested in Stripe",
                body = """Hi [Hiring Manager],

I came across the Senior Android Engineer role at Stripe and I'm very interested.

I've been working with Kotlin and Jetpack Compose for 4+ years, and I'm particularly excited about building payment infrastructure that developers love.

Would you have 15 minutes for a quick chat?

Best,
[Your Name]"""
            )
        ),
        JobCard(
            id = "2",
            company = companies[1],
            title = "Staff Mobile Engineer",
            location = "New York, NY",
            locationType = LocationType.REMOTE,
            salary = SalaryRange(200000, 280000),
            techStack = listOf("Kotlin", "Swift", "React Native", "GraphQL"),
            isHiringNow = true,
            roleDescription = "Lead mobile engineering efforts at Notion. Define architecture, mentor engineers, and ship features that millions use daily.",
            matchReason = "You have the staff-level experience they need, and your productivity app background is a perfect fit.",
            introTemplate = IntroTemplate(
                subject = "Staff Mobile Engineer – Notion",
                body = """Hi [Hiring Manager],

I'm reaching out about the Staff Mobile Engineer position at Notion.

As someone who's led mobile teams and shipped products at scale, I'm drawn to Notion's mission of making tools that empower everyone.

I'd love to learn more about the team and challenges you're tackling.

Best,
[Your Name]"""
            )
        ),
        JobCard(
            id = "3",
            company = companies[2],
            title = "Android Engineer",
            location = "Remote (US)",
            locationType = LocationType.REMOTE,
            salary = SalaryRange(150000, 200000),
            techStack = listOf("Kotlin", "Compose", "Ktor", "SQLDelight"),
            isHiringNow = true,
            roleDescription = "Build Linear's Android app from the ground up. You'll have significant ownership and work directly with founders.",
            matchReason = "Early-stage startup with strong technical culture. Great for someone who wants ownership.",
            introTemplate = IntroTemplate(
                subject = "Android Engineer – Linear",
                body = """Hi [Hiring Manager],

I'm excited about the Android Engineer role at Linear.

I've been following Linear since launch – the attention to design and performance is exactly what I care about in my own work.

I'd love to chat about building the Android experience.

Best,
[Your Name]"""
            )
        ),
        JobCard(
            id = "4",
            company = companies[3],
            title = "Senior Mobile Engineer",
            location = "San Francisco, CA",
            locationType = LocationType.HYBRID,
            salary = SalaryRange(190000, 270000),
            techStack = listOf("Kotlin", "Swift", "C++", "WebAssembly"),
            isHiringNow = true,
            roleDescription = "Work on Figma's mobile apps, bringing the collaborative design experience to tablets and phones.",
            matchReason = "Your design system experience and passion for UI polish make this a great match.",
            introTemplate = IntroTemplate(
                subject = "Mobile Engineer at Figma",
                body = """Hi [Hiring Manager],

I wanted to reach out about the Senior Mobile Engineer role at Figma.

As someone who cares deeply about design tools and mobile experiences, Figma's mission really resonates with me.

Would love to learn more about the mobile team's roadmap.

Best,
[Your Name]"""
            )
        ),
        JobCard(
            id = "5",
            company = companies[4],
            title = "Mobile Platform Engineer",
            location = "Remote",
            locationType = LocationType.REMOTE,
            salary = SalaryRange(170000, 240000),
            techStack = listOf("Kotlin", "TypeScript", "Next.js", "Edge Functions"),
            isHiringNow = true,
            roleDescription = "Build mobile tooling and SDKs that help developers ship faster. Work on the intersection of mobile and web.",
            matchReason = "Cross-platform experience valued here. Your full-stack knowledge is a plus.",
            introTemplate = IntroTemplate(
                subject = "Mobile Platform Engineer – Vercel",
                body = """Hi [Hiring Manager],

I'm interested in the Mobile Platform Engineer role at Vercel.

I've been using Vercel for years and love the developer experience. I'd be excited to bring that same polish to mobile SDKs.

Let me know if you'd be open to a conversation.

Best,
[Your Name]"""
            )
        ),
        JobCard(
            id = "6",
            company = companies[5],
            title = "Mobile Engineer",
            location = "Remote (Global)",
            locationType = LocationType.REMOTE,
            salary = SalaryRange(140000, 180000),
            techStack = listOf("Kotlin", "Flutter", "PostgreSQL", "Realtime"),
            isHiringNow = true,
            roleDescription = "Build Supabase's mobile client libraries and example apps. Help developers integrate Supabase into their mobile projects.",
            matchReason = "Open source contributions and SDK experience make you a strong candidate.",
            introTemplate = IntroTemplate(
                subject = "Mobile Engineer – Supabase",
                body = """Hi [Hiring Manager],

I'm reaching out about the Mobile Engineer position at Supabase.

As an open source contributor and mobile developer, I love what Supabase is building. I'd be thrilled to help make the mobile developer experience even better.

Would you have time for a quick call?

Best,
[Your Name]"""
            )
        ),
        JobCard(
            id = "7",
            company = companies[6],
            title = "Senior Android Engineer",
            location = "San Francisco, CA",
            locationType = LocationType.HYBRID,
            salary = SalaryRange(175000, 250000),
            techStack = listOf("Kotlin", "Compose", "Room", "Plaid SDK"),
            isHiringNow = true,
            roleDescription = "Build Mercury's Android banking app. Focus on security, reliability, and delightful user experiences.",
            matchReason = "Fintech background + security-conscious development experience.",
            introTemplate = IntroTemplate(
                subject = "Android Engineer – Mercury",
                body = """Hi [Hiring Manager],

I came across the Senior Android Engineer role at Mercury and wanted to reach out.

I have experience building secure, financial-grade mobile applications and I'm excited about Mercury's approach to startup banking.

I'd love to learn more about the team.

Best,
[Your Name]"""
            )
        ),
        JobCard(
            id = "8",
            company = companies[7],
            title = "Android Tech Lead",
            location = "New York, NY",
            locationType = LocationType.HYBRID,
            salary = SalaryRange(200000, 290000),
            techStack = listOf("Kotlin", "Compose", "GraphQL", "Kotlin Multiplatform"),
            isHiringNow = true,
            roleDescription = "Lead Ramp's Android team. Define architecture, drive technical decisions, and mentor engineers while staying hands-on.",
            matchReason = "Leadership experience + fintech domain expertise. You've led teams of this size before.",
            introTemplate = IntroTemplate(
                subject = "Android Tech Lead – Ramp",
                body = """Hi [Hiring Manager],

I'm interested in the Android Tech Lead position at Ramp.

I've led mobile teams at fintech companies before and I'm excited about Ramp's mission to help businesses spend smarter.

Would love to discuss how I could contribute to the team.

Best,
[Your Name]"""
            )
        )
    )
    
    fun getJobCards(): List<JobCard> = _jobCards
    
    fun getJobCardById(id: String): JobCard? = _jobCards.find { it.id == id }
}
