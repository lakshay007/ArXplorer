# ArXplorer

An Android application for exploring and interacting with academic research papers from arXiv using AI-powered features.

## Features

- **Paper Search & Discovery**: Browse the latest research papers from arXiv database
- **Google Sign-In**: Secure authentication with your Google account
- **PDF Viewer**: Read papers directly within the app
- **AI Chat**: Discuss papers with Google's Gemini AI for better understanding
- **Paper Summarization**: Get AI-generated summaries of complex research papers
- **Bookmarks**: Save interesting papers for later reading
- **Citation Tracking**: View citation counts and related papers
- **Dark/Light Theme**: Choose your preferred viewing experience

## Tech Stack

**Android App:**
- Kotlin
- Jetpack Compose
- Firebase (Auth & Firestore)
- Hilt (Dependency Injection)
- Google Gemini AI
- PDF Viewer

**Backend:**
- Node.js
- Express.js
- Firebase Admin SDK
- Google Generative AI

## APIs Used

- arXiv API for paper data
- CrossRef API for citation information
- Semantic Scholar API for academic metadata
- Google Gemini AI for chat and summarization

## Setup

### Prerequisites
- Android Studio
- Node.js (for backend)
- Firebase project with Authentication and Firestore enabled
- Google Gemini API key

### Android App Setup
1. Clone the repository
2. Create a `local.properties` file in the root directory:
   ```
   GEMINI_API_KEY=your_gemini_api_key_here
   ```
3. Add your `google-services.json` file to the `app/` directory
4. Build and run the app

### Backend Setup
1. Navigate to the `ArXplorerBackend` directory
2. Install dependencies:
   ```bash
   npm install
   ```
3. Create a `.env` file with your environment variables
4. Add your Firebase service account key as `firebase-service-account.json`
5. Start the server:
   ```bash
   npm start
   ```

## Usage

1. Sign in with your Google account
2. Browse featured papers or search for specific topics
3. Tap on any paper to view details and read the PDF
4. Use the AI chat feature to discuss paper contents
5. Generate summaries for quick understanding
6. Bookmark papers you want to revisit

## Contributing

Feel free to submit issues and feature requests! 