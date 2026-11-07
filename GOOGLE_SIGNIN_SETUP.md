# Google Sign-In Implementation Guide

## ✅ Implementation Complete

I've successfully implemented Google Sign-In/Register for your Android app with the following features:

### What's Implemented:

#### 1. **Dependencies Added**
- Google Play Services Auth (v20.7.0) for Google Sign-In

#### 2. **Data Models** (`Models.kt`)
- `GoogleSignInRequest` - For sending `id_token` to backend during login
- `GoogleSignUpRequest` - For sending `id_token` + optional profile info during register

#### 3. **API Endpoints** (`ApiService.kt`)
- `POST /auth/google/login` - Authenticate with Google ID token
- `POST /auth/google/register` - Register with Google ID token

#### 4. **ViewModel Methods** (`AuthViewModel.kt`)
- `googleLogin(idToken: String, onSuccess: () -> Unit)` - Handle Google login
- `googleRegister(idToken: String, name: String?, phoneNumber: String?, bio: String?, onRegistered: () -> Unit)` - Handle Google register

#### 5. **UI Components** (`AuthScreen.kt`)
- Google Sign-In button with Material Design styling
- Divider with "or continue with" text
- Loading state handling
- Error/Success notifications

#### 6. **Helper Utility** (`GoogleSignInHelper.kt`)
- Centralized Google Sign-In configuration
- ID token extraction
- User information retrieval
- Sign-out functionality

#### 7. **Activity Integration** (`MainActivity.kt`)
- Activity Result Contract for handling Google Sign-In response
- Automatic token extraction and ViewModel integration
- Navigation handling after successful authentication

---

## 🔧 Backend Integration (What You Need to Do)

Your backend should implement the following endpoint:

### Google Auth Endpoint (Login + Register)
```
POST /auth/google
Content-Type: application/json

Request Body:
{
  "id_token": "<jwt_from_google>",
  "name": "User Name",           // optional - only for new users
  "nomor_hp": "081234567890",    // optional - only for new users
  "bio": "User bio"              // optional - only for new users
}

Response (200):
{
  "token": "<your_jwt_token>",
  "message": "Successfully logged in" or "Successfully registered"
}

Response (401):
{
  "message": "Invalid token"
}
```

**Backend Logic:**
1. Verify the Google `id_token` using Google's API or JWT library
2. Check the `aud` (audience) claim matches your `GOOGLE_CLIENT_ID`
3. Extract email from the token
4. **Find user by email**
   - If user NOT found → Create new user (register) with provided info
   - If user found → Login existing user
5. Generate and return your app's JWT token

---

## 📝 Google Cloud Setup (Already Done)

The Client ID is already configured in the app:
```
GOOGLE_CLIENT_ID = 350850678512-5c8579vu92dsenqaet17u7iec0k4gr2v.apps.googleusercontent.com
```

**Important:** Make sure this Client ID is configured in Google Cloud Console with:
- Application Type: Android
- Package Name: `com.reihan.dashboard`
- The correct SHA-1 fingerprint of your signing certificate

---

## 🚀 How to Use

### For User - Login with Google:
1. User taps "Continue with Google" button
2. Google Sign-In dialog appears
3. User selects their Google account
4. App receives `id_token` from Google
5. App sends `id_token` to your backend's `/auth/google/login`
6. Backend verifies token and returns JWT
7. User is authenticated and navigated to home screen

### For User - Register with Google:
1. Same process as login
2. Backend checks if user exists
3. If not, creates new account (optionally with profile info)
4. Returns JWT token

---

## 🛡️ Security Notes

1. **ID Token Verification**: Your backend MUST verify the Google ID token
   - Check signature using Google's public keys
   - Verify `aud` matches your CLIENT_ID
   - Check `exp` (expiration) hasn't passed
   - Verify `iss` is `https://accounts.google.com`

2. **Token Storage**: The returned JWT is stored in SharedPreferences via SessionManager
   - Tokens are checked for expiration before use
   - SessionManager handles token refresh logic

3. **HTTPS Only**: Always use HTTPS for backend communication in production

---

## 🔌 Example Backend Implementation (Node.js/Express)

```javascript
const { OAuth2Client } = require('google-auth-library');

const googleClient = new OAuth2Client('YOUR_GOOGLE_CLIENT_ID');

app.post('/auth/google', async (req, res) => {
  try {
    const { id_token, name, nomor_hp, bio } = req.body;
    
    // Verify token
    const ticket = await googleClient.verifyIdToken({
      idToken: id_token,
      audience: 'YOUR_GOOGLE_CLIENT_ID'
    });
    
    const { email } = ticket.getPayload();
    
    // Find user by email
    let user = await User.findOne({ email });
    
    if (!user) {
      // User not found - create new user (register)
      user = await User.create({ 
        email, 
        name: name || ticket.getPayload().name,
        nomor_hp,
        bio
      });
    }
    
    // Generate JWT
    const token = generateJWT(user);
    
    res.json({ 
      token,
      message: 'Successfully logged in' // or 'Successfully registered'
    });
  } catch (error) {
    res.status(401).json({ message: 'Invalid token' });
  }
});
```

---

## 📱 Testing

1. Build and run the app: `./gradlew build`
2. Open the Auth screen
3. Tap "Continue with Google"
4. Select a test Google account
5. Verify token is sent to your backend
6. Check that login/register works correctly

---

## ⚠️ Troubleshooting

| Issue | Solution |
|-------|----------|
| Google Sign-In button not working | Check `GOOGLE_CLIENT_ID` matches your Firebase/GCP project |
| "Invalid CLIENT_ID" error | Verify SHA-1 fingerprint in GCP console matches your app's signing certificate |
| Token not being received | Check Google Cloud project has correct OAuth scopes enabled |
| Backend not recognizing token | Make sure backend uses `google-auth-library` to verify token signature |

---

## 📚 File Reference

### Modified Files:
- `app/build.gradle.kts` - Added Google Play Services dependency
- `app/src/main/java/com/reihan/dashboard/data/model/Models.kt` - Added Google models
- `app/src/main/java/com/reihan/dashboard/data/network/ApiService.kt` - Added Google endpoints
- `app/src/main/java/com/reihan/dashboard/ui/auth/AuthViewModel.kt` - Added Google methods
- `app/src/main/java/com/reihan/dashboard/ui/auth/AuthScreen.kt` - Updated UI with Google button
- `app/src/main/java/com/reihan/dashboard/MainActivity.kt` - Added Google Sign-In handling

### New Files:
- `app/src/main/java/com/reihan/dashboard/ui/auth/GoogleSignInHelper.kt` - Google Sign-In utility

---

## ✨ Next Steps

1. **Set up backend endpoints** for Google token verification
2. **Test with your Google account**
3. **Deploy backend** with proper token validation
4. **Test end-to-end** login and register flow
5. **Monitor logs** for any issues

