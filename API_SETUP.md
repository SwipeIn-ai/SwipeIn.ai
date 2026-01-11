# API Integration Setup

## FindWork.dev API Key

To use real job data from FindWork.dev API, you need to add your API key:

### Step 1: Get Your API Key
1. Go to [FindWork.dev Developers](https://findwork.dev/developers/)
2. Sign up or log in
3. Copy your API key

### Step 2: Add API Key to the App
1. Open the file: `app/src/main/java/com/swipeapply/app/data/config/ApiConfig.kt`
2. Replace `YOUR_API_KEY_HERE` with your actual API key:

```kotlin
const val FINDWORK_API_KEY = "your_actual_api_key_here"
```

### Step 3: Customize Search Parameters (Optional)
You can also customize the default search parameters in the same file:

```kotlin
const val DEFAULT_SEARCH_QUERY = "android kotlin"  // Change search keywords
const val DEFAULT_LOCATION = "San Francisco"       // Add location filter
const val DEFAULT_REMOTE_ONLY = true               // Filter for remote jobs only
```

## How It Works

The app uses an **offline-first architecture**:

1. **First Load**: Fetches jobs from FindWork.dev API
2. **Caching**: Stores jobs locally in Room database
3. **Offline**: Shows cached jobs when offline
4. **Refresh**: Pull to refresh or reopen app to fetch new jobs

## API Features

- ✅ Real job data from FindWork.dev
- ✅ Local caching with Room database
- ✅ Offline support
- ✅ Error handling with user-friendly messages
- ✅ Automatic retry with cached data fallback

## Troubleshooting

### "Failed to load jobs" Error
- Check your internet connection
- Verify your API key is correct
- Check if you've exceeded the rate limit (60 requests/minute)

### No Jobs Showing
- Make sure you've added your API key
- Try changing the search query in `ApiConfig.kt`
- Check the Logcat for detailed error messages

## API Documentation

For more details about the FindWork.dev API:
- [API Documentation](https://findwork.dev/developers/)
- [API Playground](https://findwork.dev/developers/playground/)
