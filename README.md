# SwipeApply / Draft

> **The "Tinder for Jobs" - A Two-Level Professional Matching Platform**

SwipeApply (conceptually "Draft") is a native Android application built with **Kotlin** and **Jetpack Compose** that reimagines the job search experience. Instead of endlessly scrolling through text-heavy lists, users engage with a dynamic, card-based interface to discover companies and connect with the right people.

## 📱 Project Overview

The app introduces a novel **Two-Level Swiping Flow**:
1.  **Company Level:** Discover companies based on culture, size, and open roles. Swipe **Right** to "Explore Team", Swipe **Left** to skip.
2.  **Employee Level:** Once interested in a company, connect directly with the humans inside. View cards for hiring managers, recruiters, or potential peers. Swipe **Right** to "Request Intro", Swipe **Left** to pass.

This approach humanizes recruitment by focusing on *who* you work with, not just *where* you work.

## ✨ Key Features

*   **Two-Level Matching Engine**: Seamless transitions from company discovery to people connection.
*   **Interactive Card Stack**: 
    *   Physics-based animations (Spring specs).
    *   Haptic feedback for satisfying interactions.
    *   Visual cues for "Like" (Green/Right) and "Pass" (Red/Left).
*   **Rich Profiles**: 
    *   **Company Cards**: Display industry, size, "Open Roles" badges, and rich descriptions.
    *   **Employee Cards**: Highlight tenure, shared connections, bio, and "Open to Intros" status.
*   **Detailed Views**: Expandable bottom sheets for deep dives into company open roles or employee full bios without leaving the flow.
*   **Intro Request System**: Integrated flow to send personalized intro templates to connections.
*   **Gamified Stats**: Track matches, intros sent, and companies viewed in real-time.

## 🛠️ Tech Stack & Architecture

*   **Language**: Kotlin 100%
*   **UI Toolkit**: Jetpack Compose (Material Site 3)
*   **Architecture**: MVVM (Model-View-ViewModel) with Unidirectional Data Flow.
*   **State Management**: Kotlin Coroutines & StateFlow.
*   **Animation**: `androidx.compose.animation` (AnimatedContent, AnimatedVisibility, AnimateColorAsState).
*   **Data Source**: Repository Pattern (Currently using `MockDataRepository` for rapid prototyping).

## 📂 Project Structure

```
com.swipeapply.app
├── data
│   ├── model          # Data classes (Company, Employee, SwipeMode)
│   └── repository     # Data access (MockDataRepository)
├── ui
│   ├── components
│   │   └── swipe      # Reusable card components (EmployeeSwipeCard, GenericSwipeStack)
│   ├── screens        # Main screens (SwipeScreen, IntroTemplateScreen)
│   ├── theme          # Design system (Type, Color, Shape)
│   └── viewmodel      # Logic holders (SwipeViewModel)
└── MainActivity.kt
```

## 🚀 Getting Started

### Prerequisites
*   Android Studio Ladybug or newer.
*   JDK 17+.

### Installation
1.  **Clone the repository**:
    ```bash
    git clone https://github.com/yourusername/swipeapply.git
    ```
2.  **Open in Android Studio**.
3.  **Sync Gradle** to download dependencies.
4.  **Run** on an Emulator (API 26+) or a physical device.

## 🎮 How to Use

1.  **Launch the App**: You start in the "Company Discovery" mode.
2.  **Swipe on Companies**:
    *   **Right**: You are interested! The app switches to "Connection Mode" for this company.
    *   **Left**: Not for you. Shows the next company.
3.  **Connect with People**:
    *   Inside a company, viewing cards of employees.
    *   **Right**: Draft an intro message to this person.
    *   **Left**: View the next employee.
4.  **Auto-Return**: If you run out of employees or swipe left on everyone, you automatically return to the global Company stack.

## 🗺️ Roadmap

*   [x] Core Swipe Animation & Logic
*   [x] Two-Level Navigation (Company -> Employee)
*   [x] Detailed Bottom Sheets
*   [ ] **Real Backend Integration** (Firebase/Supabase)
*   [ ] **User Authentication** (LinkedIn Sign-in)
*   [ ] **Intro Templates Customization**
*   [ ] **Chat/Inbox Feature** for accepted intros

## 🤝 Contributing

This project is currently in the **Prototyping Phase**. Design contributions or feature suggestions to the "Gamification" aspect are welcome!

## 📄 License

Project is proprietary. All rights reserved.
