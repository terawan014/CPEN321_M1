# CPEN321_26W1_M1

_Keep this README up to date with the steps required to build and run the frontend and backend (including any scripts, config files, and environment variables). TAs ill follow these instructions._

## Requirements

Install the following before the frontend or backend setup steps:

- [git](https://git-scm.com/install/)


--- 

## Frontend Setup

### Requirements

- [Android Studio](https://developer.android.com/studio) (latest version)
- [Java 17](https://adoptium.net/temurin/releases/?version=17)
- [Android SDK](https://developer.android.com/studio#command-tools) with API level 36+ (Android 16)

Java must be available from the command line. Verify with:

```bash
java -version
```
On Windows, if java is not found, set JAVA_HOME to the Java 17 installation directory and add %JAVA_HOME%\bin to PATH

### Setup

1. **Open project**: Open the `frontend/` directory in Android Studio
2. **Sync Gradle**: Android Studio will automatically prompt you to sync the project. Click "Sync Now". You can also manually run `cd frontend && ./gradlew build` to trigger the sync and download the necessary dependencies.
3. **Configure Android SDK**: Ensure you have Android SDK 36 installed.
4. **Set up emulator/device**:
   - Create a new AVD (Android Virtual Device) by selecting Pixel 9 as the device and Android Baklava (API level 36) as the system image.
   - Alternatively, connect a physical Android device running Android 16 (API level 36).
5. **Setup app config**: Copy the example file, then fill in local values:
   ```bash
   cp frontend/local.properties.example frontend/local.properties
   ```
Set at least:

- `sdk.dir`: path to your Android SDK. Android Studio usually writes this the first time you open `frontend/`.
- `API_BASE_URL`: backend URL baked into the APK.
  - For the deployed M1 backend, use:
    `https://34.123.228.126`
  - For local development using an Android emulator, use:
    `http://10.0.2.2:3000`
- `GOOGLE_CLIENT_ID`: Google OAuth Web Client ID used for Google Sign-In.

Example:

```properties
sdk.dir=<YOUR_ANDROID_SDK_PATH>
API_BASE_URL=https://34.123.228.126
GOOGLE_CLIENT_ID=645452205793-vt9m94i74rijk3qb929h0ij6v7iohhik.apps.googleusercontent.com
```


### Build and Run

- **Debug build**: Click the green play button in the toolbar, to compile the code, package a debug APK, and install it on the connected device or running emulator. Alternatively, from the project root, run `./scripts/run-frontend.sh`.
- **Release build**: Go to Build -> Generate Signed App Bundle or APK -> APK. Follow the on-screen instructions to create a key, and select the "release" build variant. You will then have to manually install the generated APK on your device or the running emulator.


### Backend Configuration

Ensure the backend server is running and update the base URL in the app configuration if needed.

---
## Backend Setup

You can run the backend in one of two ways:
* Locally via Node.js 
* Via Docker Compose

Both ways use the same `backend/.env` file (see below).

### Environment configuration

From the project root:

```bash
cp backend/.env.example backend/.env
```

Set at least:
- `JWT_SECRET`: a long random string used to sign auth tokens. Do not commit the real value.
- `MONGODB_URI`: MongoDB connection string used for local development. Do not commit database credentials.
- `PORT` (optional): defaults to `3000` if unset.
- `SERVER_PUBLIC_IP`: public IP returned by the backend server IP endpoint. For the deployed M1 backend, use `34.123.228.126`.


### Option 1: Run locally

**Requirements:** 
- [Node.js](https://nodejs.org/en/download/) 22+
- [npm](https://docs.npmjs.com/downloading-and-installing-node-js-and-npm) 10+

**Setup:** 
1. Install dependencies:

   ```bash
   cd backend
   npm install
   ```

2. **Development** (TypeScript with auto-reload):

   ```bash
   npm run dev
   ```

3. **Production build** (optional):

   ```bash
   npm run build
   npm start
   ```

### Option 2: Run with Docker Compose

**Requirements:** 
- [Docker](https://docs.docker.com/desktop/setup/install) and [Docker Compose](https://docs.docker.com/desktop/setup/install) v2.24+
- [curl](https://curl.se/download.html)

**Setup**
1. **Start** (from the project root):

   ```bash
   ./scripts/run-backend.sh
   ```

   Or run Compose directly:

   ```bash
   docker compose up --build -d
   ```

2. **Stop**:

   ```bash
   docker compose down
   ```

## Additional Setup

_Please specify any other additional setup steps non-specific to either frontend nor backend_