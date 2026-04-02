
# **Android App Specification: AAP Remote Control**

## **1\. Executive Summary**

**Project:** A lean, lightweight Android application acting as a remote control for the Ansible Automation Platform (AAP). **Goal:** Allow users to securely authenticate with their AAP instance, view available job templates, trigger playbooks, and monitor job statuses directly from their mobile device. **Development Approach:** "AI Vibe Coding" (LLM-assisted development). The tech stack is intentionally chosen for having massive amounts of high-quality AI training data and minimal boilerplate.

## **2\. Tech Stack & Architecture (Lean & Modern)**

To keep the app as lightweight as possible while ensuring the AI generates clean code, enforce this exact stack in your prompts:

* **Language:** Kotlin (100%). *Do not allow the AI to generate Java.*  
* **UI Toolkit:** Jetpack Compose. *Strictly no XML layouts or fragments. Single Activity Architecture.*  
* **Architecture Pattern:** MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF).  
* **Networking:** Retrofit with Kotlin Serialization (or Moshi). It is the industry standard, and AI rarely makes mistakes with it.  
* **Dependency Injection (DI):** Koin. (Lighter and requires less boilerplate/setup than Hilt, making it perfect for AI generation).  
* **Local Storage/Security:** Jetpack Security (EncryptedSharedPreferences) for securely storing the AAP API Bearer token.

## **3\. Core Features & MVP Scope**

### **Phase 1: Authentication & Setup**

* **Instance Setup:** User inputs the base URL of their AAP instance.  
* **Login:** User authenticates via Personal Access Token (PAT) or OAuth2 credentials.  
* **Secure Storage:** The app encrypts and stores the token locally.

### **Phase 2: Remote Control (Dashboard & Execution)**

* **Template List:** Fetch and display a list of available Job Templates (/api/v2/job\_templates/).  
* **Launch Job:** A "Play" button next to a template to trigger it (/api/v2/job\_templates/{id}/launch/).  
* **Extra Variables:** (Optional but recommended) A simple text field or dynamic form to pass extra\_vars JSON before launching.

### **Phase 3: Monitoring**

* **Job Status:** Polling or WebSocket connection to show the status of the launched job (Pending, Running, Successful, Failed).  
* **Recent Jobs List:** View previously run jobs.

## **4\. API Integration Basis (Ansible AAP)**

The app will heavily rely on the AAP REST API. The AI should be instructed to build an API Interface mirroring these endpoints:

| Feature | HTTP Method | AAP Endpoint | Description |
| :---- | :---- | :---- | :---- |
| **Auth** | POST | /api/v2/tokens/ | Exchange credentials for a token (if not using a pre-generated PAT). |
| **Get Templates** | GET | /api/v2/job\_templates/ | Retrieves the list of executable playbooks. |
| **Launch Job** | POST | /api/v2/job\_templates/{id}/launch/ | Triggers the job. Send extra\_vars in the JSON body if needed. |
| **Get Job Status** | GET | /api/v2/jobs/{id}/ | Checks if the job is running, failed, or successful. |

**Important Header Requirement:** All network calls (except initial auth) must include the header: Authorization: Bearer \<YOUR\_TOKEN\>

## **5\. "AI Vibe Coding" System Rules**

When you start coding this with your AI assistant (like Cursor, GitHub Copilot, or even chatting with me), paste these exact rules into your initial context prompt so the AI doesn't hallucinate legacy code:  
**System Instructions for AI:**

1. You are an expert modern Android developer.  
2. We are building a lean Android app. Write strictly in modern Kotlin.  
3. Use Jetpack Compose for ALL UI. No XML, no Fragments. Use ComponentActivity.  
4. Use Material Design 3 (Material You) components.  
5. Use MVVM architecture. ViewModels should expose UI state via StateFlow.  
6. Use Koin for Dependency Injection.  
7. Use Retrofit \+ Coroutines for network calls.  
8. Keep dependencies to an absolute minimum to reduce APK size.

## **6\. Security Best Practices**

* **No Hardcoded Secrets:** Never hardcode AAP URLs or tokens.  
* **Network Security Configuration:** Enforce cleartext traffic restriction (HTTPS only) via network\_security\_config.xml to prevent token interception.  
* **Token Handling:** Only store tokens using Android's EncryptedSharedPreferences. Never use standard SharedPreferences or local SQLite databases for credentials.

Would you like me to write the exact first prompt you should feed into your coding assistant to generate the foundational project structure and authentication screen?  
By the way, to unlock the full functionality of all Apps, enable [Gemini Apps Activity](https://myactivity.google.com/product/gemini).

Here is the exact prompt you should feed into your AI coding assistant.  
This prompt is designed to act as a "system prompt" combined with your first task. It firmly establishes the boundaries so the AI doesn't hallucinate legacy code, and asks it to generate the foundational layer and the Authentication screen.  
Copy and paste everything inside the blockquote below:  
\> \*\*Role:\*\* You are an expert modern Android developer. We are doing "AI vibe coding" to build a lean, lightweight remote control app for the Ansible Automation Platform (AAP).  
\> \*\*Core Tech Stack & Rules:\*\*  
\>  \* \*\*Language:\*\* 100% Kotlin. No Java.  
\>  \* \*\*UI:\*\* Jetpack Compose (Material 3). Strictly no XML layouts, no Fragments. Use a single ComponentActivity.  
\>  \* \*\*Architecture:\*\* MVVM with Unidirectional Data Flow. ViewModels must expose UI state via StateFlow.  
\>  \* \*\*Networking:\*\* Retrofit \+ Kotlin Serialization \+ Coroutines.  
\>  \* \*\*DI:\*\* Koin (keep it lightweight).  
\>  \* \*\*Security:\*\* EncryptedSharedPreferences for token storage. No plaintext secrets.  
\> \*\*Task 1: Project Scaffolding & Authentication Feature\*\*  
\> Please generate the foundational code for the app, focusing on the Authentication layer. I need the following components:  
\>  1\. \*\*Dependencies Setup:\*\* List the exact Gradle dependencies I need to add for Compose, Retrofit, Kotlin Serialization, Koin, and Jetpack Security Crypto.  
\>  2\. \*\*Network Layer:\*\* \>     \* A Retrofit interface AapApiService with a simple GET request to /api/v2/me/ (or similar) just to validate the base URL and Bearer token.  
\>    \* A Koin module networkModule providing the Retrofit instance and OkHttpClient.  
\>  3\. \*\*Data Layer:\*\* \>     \* A TokenManager class using EncryptedSharedPreferences to save and retrieve the Bearer token and Base URL.  
\>    \* An AuthRepository that interacts with the TokenManager and AapApiService.  
\>  4\. \*\*Presentation Layer (ViewModel):\*\*  
\>    \* An AuthViewModel that handles the login attempt. It should have a StateFlow representing AuthState (Idle, Loading, Success, Error).  
\>  5\. \*\*UI Layer (Compose):\*\*  
\>    \* An AuthScreen Composable. It needs a text field for the "AAP Base URL", a secure text field for the "Personal Access Token (PAT)", and a "Connect" button. It should react to the AuthViewModel's state (e.g., showing a CircularProgressIndicator while loading).  
\> Please output the code organized by file names so I can easily copy them into my project structure.  
\>   
\#\#\# Why this prompt works:  
 \* \*\*Context Control:\*\* It immediately shuts down the AI's tendency to suggest older Android paradigms like XML or Hilt (which has a lot of boilerplate).  
 \* \*\*Clear Output Structure:\*\* Asking it to separate the output by file names makes it incredibly easy for you to just copy, paste, and run.  
 \* \*\*Functional Goal:\*\* Using the /api/v2/me/ endpoint is the perfect way to test if the credentials work before we start doing the heavier lifting of fetching playbooks.  
