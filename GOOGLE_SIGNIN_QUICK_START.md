# Google Sign-In Quick Start

## 🎯 What Was Implemented

Your mobile app now has **full Google Sign-In/Register** integration with these components:

### Frontend (Android App):
- ✅ Google Sign-In button on Auth screen
- ✅ Automatic ID token extraction from Google
- ✅ Backend integration with ID token payload
- ✅ Token validation and session management
- ✅ Error handling and loading states

### Backend (Your API - TO DO):
- ⚠️ You need to implement Google token verification
- ⚠️ You need to implement user lookup/creation logic
- ⚠️ You need to return your JWT token

---

## 🔑 Key Files to Know

**Just the Essentials:**

| File | Purpose |
|------|---------|
| `GoogleSignInHelper.kt` | Helper for Google Sign-In configuration & token extraction |
| `AuthViewModel.kt` | Login/Register methods that send `id_token` to backend |
| `AuthScreen.kt` | UI with Google Sign-In button |
| `Models.kt` | `GoogleSignInRequest` & `GoogleSignUpRequest` data classes |
| `ApiService.kt` | Endpoints: `/auth/google/login` & `/auth/google/register` |

---

## 📡 Backend API Spec

### Single Endpoint: Google Auth
```
POST /auth/google
{
  "id_token": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjEyMyJ9...",
  "name": "John Doe",           // optional - for new users
  "nomor_hp": "081234567890",    // optional - for new users
  "bio": "My bio"                // optional - for new users
}

Response (200):
{
  "token": "your_jwt_token_here",
  "message": "Successfully logged in" or "Successfully registered"
}

Response (401):
{
  "message": "Invalid token"
}
```

**Logic:**
- If user not found → create new user (register)
- If user exists → login directly

---

## ⚡ Quick Backend Implementation (Node.js)

```javascript
const { OAuth2Client } = require('google-auth-library');

const googleClient = new OAuth2Client('350850678512-5c8579vu92dsenqaet17u7iec0k4gr2v.apps.googleusercontent.com');

// Helper to verify Google token
async function verifyGoogleToken(idToken) {
  try {
    const ticket = await googleClient.verifyIdToken({
      idToken,
      audience: 'YOUR_CLIENT_ID'
    });
    return ticket.getPayload();
  } catch (error) {
    throw new Error('Invalid token');
  }
}

// Single endpoint for both login and register
app.post('/auth/google', async (req, res) => {
  try {
    const { id_token, name, nomor_hp, bio } = req.body;
    
    // Verify token
    const payload = await verifyGoogleToken(id_token);
    const { email } = payload;
    
    // Find user
    let user = await User.findOne({ email });
    
    if (!user) {
      // User not found - create new user (register)
      user = await User.create({
        email,
        name: name || payload.name,
        nomor_hp,
        bio
      });
    }
    
    // Generate JWT
    const token = jwt.sign({ userId: user.id }, 'your_secret');
    
    res.json({ 
      token, 
      message: user.createdAt === user.updatedAt ? 'Successfully registered' : 'Successfully logged in'
    });
  } catch (error) {
    res.status(401).json({ message: error.message });
  }
});
```

---

## 🧪 How to Test

1. **Build the app**
   ```bash
   cd mobile_app
   ./gradlew build
   ```

2. **Run on emulator or device**
   ```bash
   ./gradlew installDebug
   ```

3. **Test Google Sign-In**
   - Navigate to Auth screen
   - Tap "Continue with Google"
   - Select your test Google account
   - Check app logs to see if token is sent

4. **Monitor network requests**
   - Use proxy like Charles or Fiddler to inspect the request
   - Should see POST to `/auth/google/login` with `id_token` in body

5. **Verify backend**
   - Backend should verify the `id_token`
   - Return JWT token
   - App should receive token and navigate to home

---

## 🚨 Common Issues & Fixes

**"Sign-in failed"**
- ✅ Check CLIENT_ID is correct
- ✅ Check SHA-1 fingerprint in Google Cloud Console
- ✅ Verify app's package name matches

**"Token is null"**
- ✅ Check Google Play Services are installed on device
- ✅ Check internet connection
- ✅ Check Google Sign-In dialog closes properly

**Backend returns "Invalid token"**
- ✅ Verify you're using `oauth2-client` or similar library
- ✅ Check you're verifying against the correct CLIENT_ID
- ✅ Make sure token hasn't expired

**CORS errors**
- ✅ This shouldn't happen (not browser-based)
- ✅ Check backend's CORS configuration

---

## 💡 Pro Tips

1. **Token Storage**: The JWT returned by your backend is automatically stored by SessionManager
2. **Auto-Login**: If token is valid, user stays logged in across app restarts
3. **Token Expiry**: SessionManager checks token expiry automatically
4. **Logout**: Call `GoogleSignInHelper.signOut(context)` to sign out

---

## 📞 API Endpoints Summary

| Endpoint | Method | Body | Purpose |
|----------|--------|------|---------|
| `/auth/google` | POST | `{ id_token, name?, nomor_hp?, bio? }` | Login or Register with Google |

**Logic:** User not found → Create | User exists → Login

---

## 🎓 Understanding the Flow

```
User taps "Continue with Google"
        ↓
Google Sign-In Dialog (handled by Google)
        ↓
User selects account and authenticates
        ↓
Google returns id_token to app
        ↓
App sends id_token + optional profile to backend
        ↓
Backend verifies id_token using Google's public keys
        ↓
Backend checks if user exists
        ├─ If NOT found: Create new user (register)
        └─ If found: Use existing user (login)
        ↓
Backend generates JWT and returns it
        ↓
App stores JWT in SessionManager
        ↓
User is logged in!
```

---

Done! Your Google Sign-In is ready. Now just implement the backend endpoints. 🎉
