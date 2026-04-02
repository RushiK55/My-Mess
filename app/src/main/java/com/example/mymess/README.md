## My Mess Manager – Android App Architecture & Implementation Plan

### 1. Overview
**My Mess Manager** is a multi-role Android application designed to streamline mess operations for students, working professionals, and mess owners. It supports three distinct user types:
- **User**: Can enroll in exactly one mess, request to join, order from enrolled mess meals, optionally order from cloud meals anytime, track orders, and manage payments.
- **Owner**: Manages mess details, approves user enrollments, handles orders, manages two meal inventories (mess meals and cloud meals), tracks payments, and views analytics.
- **Admin**: Approves new owner registrations, manages all users, controls banners, and views overall analytics.

The app uses **Firebase Realtime Database** as the backend (via REST API using Retrofit), **Cloudinary** for image storage, and **Firebase Cloud Messaging** for push notifications. It follows **MVVM** architecture with **Kotlin**, **Coroutines**, and **Hilt** for dependency injection.

---

### 2. Architecture & Tech Stack
- **Language**: Kotlin
- **UI**: XML (Material Design)
- **Architecture**: MVVM (Model-View-ViewModel) with Repository pattern
- **Asynchronous**: Kotlin Coroutines (Flows for reactive streams)
- **DI**: Hilt
- **Network**: Retrofit (for Firebase Realtime Database REST API)
- **Image Storage**: Cloudinary SDK
- **Notifications**: Firebase Cloud Messaging (FCM)
- **Charts**: MPAndroidChart (for analytics)
- **Search**: Simple search using `Filterable` or `RecyclerView` with custom filtering
- **Authentication**: Firebase Auth (optional) or custom JWT; given Firebase Realtime Database URL, we can implement email/password login with session token stored locally.

---

### 3. Data Models (Kotlin Data Classes)
```kotlin
// User
data class User(
    val uid: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String, // "user", "owner", "admin"
    val status: String, // "pending", "approved", "blocked"
    val approvedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val messId: String? = null, // legacy key
    val enrolledMessId: String? = null // active enrolled mess (single enrollment)
)

// Mess (for owners)
data class Mess(
    val messId: String,
    val ownerId: String,
    val name: String,
    val address: String,
    val city: String,
    val contact: String,
    val description: String,
    val imageUrl: String? = null,
    val isApproved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// Meal (for mess menu)
data class Meal(
    val mealId: String,
    val messId: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String?,
    val isAvailable: Boolean = true,
    val type: String, // legacy key: "mess" or "cloud"
    val mealSection: String // canonical key: "mess" or "cloud"
)

// Order
data class Order(
    val orderId: String,
    val userId: String,
    val messId: String? = null,
    val ownerId: String? = null,
    val mealId: String,
    val mealName: String,
    val quantity: Int,
    val totalPrice: Double,
    val status: String, // "pending", "accepted", "preparing", "ready", "delivered", "cancelled"
    val orderType: String, // legacy key
    val orderSource: String, // canonical key: "mess" or "cloud"
    val createdAt: Long,
    val updatedAt: Long,
    val paymentStatus: String, // "pending", "paid"
    val paymentMethod: String? = null,
    val paymentId: String? = null
)

// UserRequest (join mess request)
data class UserRequest(
    val requestId: String,
    val userId: String,
    val messId: String,
    val status: String, // "pending", "approved", "rejected"
    val reason: String? = null,
    val createdAt: Long
)

// Banner
data class Banner(
    val bannerId: String,
    val title: String,
    val imageUrl: String,
    val targetRole: String, // "user", "owner", "admin", "all"
    val isActive: Boolean = true,
    val createdAt: Long
)

// PaymentRecord (for owner/admin tracking)
data class PaymentRecord(
    val paymentId: String,
    val userId: String,
    val messId: String,
    val amount: Double,
    val status: String, // "pending", "paid"
    val dueDate: Long,
    val paidAt: Long? = null
)
```

---

### 4. Database Structure (Firebase Realtime Database)
Firebase RTDB is a NoSQL JSON database. We'll structure it as:
```
{
  "users": { uid: { ... } },
  "messes": { messId: { ... } },
  "meals": { mealId: { ... } },
  "orders": { orderId: { ... } },
  "userRequests": { requestId: { ... } },
  "banners": { bannerId: { ... } },
  "payments": { paymentId: { ... } }
}
```
We'll use REST API endpoints: `https://messappflutter-default-rtdb.firebaseio.com/{path}.json` with appropriate query parameters.

---

### 5. Authentication & Session Management
We'll implement a simple email/password login using Firebase Auth (since it's free and easy). Alternatively, we can store user credentials in RTDB and manage sessions with JWT tokens stored in `SharedPreferences`. For simplicity and integration, Firebase Auth is recommended. However, the requirement only mentions RTDB, so we'll use custom auth: after login, the app stores a session token (e.g., user UID) and uses it for all API calls. No actual JWT generation is needed as RTDB security rules can be based on UID.

We'll use **Firebase Authentication** (if allowed) to handle sign-up/sign-in. If not, we'll implement custom auth with password hashing (bcrypt) stored in RTDB. Since the given URL is for RTDB, we can extend with Firebase Auth easily.

**Plan**: Use Firebase Auth for user management. Store additional user data in RTDB under `/users/{uid}`.

---

### 6. Screens & Flows by Role

#### 6.1 Common Screens
- **Splash Screen**: Check if user is logged in; redirect to Login or appropriate dashboard.
- **Login Screen**: Email, password, "Forgot Password" (Firebase Auth).
- **Registration Screen**: Name, email, phone, password, role selection (user/owner). For owner, additional fields (mess name, address, etc.) are collected but submitted for admin approval later.
- **Profile Screen**: View/edit profile details; change password.

#### 6.2 User Screens
- **Home Screen (User)**: Two tabs: **Mess Section** (if enrolled in a mess) and **Cloud Section** (global takeaway).
  - **Mess Section**: Shows mess details, menu (list of meals), "Order Now" button, recent orders, payment status.
  - **Cloud Section**: Shows list of all owners who have uploaded "cloud" meals (type=cloud). Each meal item with image, price, "Add to Cart" option. Users can order from any mess's cloud menu without being enrolled.
  - **Banners**: Carousel at top, fetched from `/banners` with targetRole="user".
- **Order Placement**: When user clicks on a meal, open meal details bottom sheet (image, quantity, special instructions), then confirm. Creates order with status "pending". If mess section, order is sent to the owner. If cloud, an advance payment record is also created and owner must mark it paid before accepting the order.
- **Order History Screen**: List of all orders. Filter by date (day/week/month), status. Each item shows meal name, quantity, total price, status, payment status. Click to view details.
- **Mess Details Screen**: For browsing other messes (when not enrolled). Shows mess info, menu, "Request to Join" button.
- **Join Request**: User sends a join request to a mess owner. Status becomes "pending" until owner approves.
- **Payment Screen**: List of pending/paid payments for the user (if enrolled). Filter by status. Option to pay (integrate payment gateway like Razorpay or Stripe). For this plan, we'll assume manual marking of payment by owner (since requirement mentions "pending/paid" list, maybe owner updates status).
- **Search**: In mess list or cloud meals, search by name, location.

#### 6.3 Owner Screens
- **Home Screen (Owner)**:
  - **Order Requests**: List of orders with status "pending" (just placed). Accept/reject; if accept, status changes to "accepted". Option to update status further (preparing, ready, delivered).
  - **Summary Cards**: Number of enrolled users, pending orders, today's earnings.
  - **Banners**: Carousel for owner-specific banners (targetRole="owner").
- **Pending Orders**: All orders that are "accepted" or "preparing", allowing status updates.
- **Enrolled Users**: List of users who have been approved for this mess. Search by name/email. Click to see details: name, phone, email, join date, payment history. Option to **block** user (change status to blocked) with reason; blocked users cannot order from this mess.
- **New User Requests**: List of users who requested to join (status "pending"). Approve/reject. For approval, user becomes enrolled in mess; for reject, user is removed from requests.
- **Profile Screen**: Edit mess details (name, address, contact, description, image) – all updates stored in RTDB.
- **Payment Screen**: List of payment records for enrolled users. Filter by pending/paid, search by user. Option to mark payment as paid (if manual). Owner can also generate monthly bills for users (we'll implement basic logic).
- **Banners Screen**: CRUD for banners that will appear in users' home screens (targetRole="user").
- **Analytics Screen**: Charts showing: orders per day/week, revenue trend, most ordered meals, user growth.
  - **Mess Meals Management**: Owner can add/edit/delete meals for enrolled users (`mealSection="mess"`).
  - **Cloud Meals Management**: Owner can add/edit/delete cloud meals (`mealSection="cloud"`) visible to all users.

#### 6.4 Admin Screens
- **Home Screen (Admin)**:
  - **New Owner Approvals**: List of owners whose `isApproved` is false. Click to see details, approve/reject.
  - **Summary**: Total users, owners, orders, revenue (overall).
  - **Banners**: Carousel for admin-specific banners (targetRole="admin").
- **Users List**: All users (all roles). Search by name/email. Click to view details: can block/unblock, delete, etc.
- **Banners Screen**: CRUD for banners (targetRole can be user/owner/all). Admin can create banners with image, title, target role.
- **Analytics Screen**: Overall analytics: user growth, owner registrations, order volume, revenue distribution.

---

### 7. Approval Flows

#### 7.1 Owner Registration Approval
- Owner registers with mess details. Data stored in `users` (role="owner", status="pending") and `messes` (isApproved=false).
- Admin sees this in "New Owner Approvals". On approve: set user status="approved", mess isApproved=true.
- On reject: delete user or set status="rejected".

#### 7.2 User Join Request Approval
- User sends request to mess: stored in `userRequests` with status="pending".
- Owner sees request in "New User Requests". On approve: update request status="approved", and update user's `messId` to the mess ID.
- On reject: update request status="rejected".
- After approval, user can order from mess section.

**Enrollment constraint:** User can have only one active enrollment. On approval, update `enrolledMessId` (and optionally mirror `messId` for backward compatibility).

---

### 8. Cloud Section vs Mess Section
- **Mess Section**: Visible only when `user.enrolledMessId` exists. Shows only meals where `mealSection="mess"` and `meal.messId == user.enrolledMessId`.
- **Cloud Section**: Visible to all users (even non-enrolled). Shows meals where `mealSection="cloud"` from approved messes.
- **Ordering rule**: User can order from cloud anytime, but can order mess meals only from the enrolled mess.
- **Order classification**: Persist `orderSource="mess"|"cloud"` (and optionally mirror into legacy `orderType`).

---

### 9. Key Features Implementation Details

#### 9.1 Real-time Order History Tracking
- Orders are stored in RTDB. When order status changes, owner updates it; user sees real-time updates via Firestore or we can use Firebase Realtime Database listeners if we implement using Firebase SDK directly. Since we use Retrofit REST API, real-time updates can be achieved with polling or by using Firebase Realtime Database's real-time listeners via the Firebase SDK (we can mix). For simplicity, we can use Firebase SDK for real-time listeners where needed (e.g., order status updates). But requirement says Retrofit; we can still use Firebase SDK for real-time features if we separate concerns. However, to strictly follow, we can implement periodic refresh (every 5 seconds) for order statuses, or use WebSocket with Firebase. I'd recommend using Firebase Realtime Database listeners for order status to provide real-time updates, but the tech stack says "Backend : Retrofit and use this url" – it implies using REST API. So we'll use polling with `refreshData()` on pull-to-refresh or auto-refresh every 30 seconds. Not ideal but acceptable. Alternatively, we can use Firebase Cloud Messaging to push updates when order status changes, which is more efficient.

#### 9.2 Image Upload with Cloudinary
- For mess images, meal images, banner images: use Cloudinary SDK to upload from device. After upload, get the URL and store in database.
- Implementation: Create a `CloudinaryRepository` with methods like `uploadImage(bitmap, folder)` returning URL.
- Use Hilt to inject Cloudinary instance with API key/secret.

#### 9.3 Notifications with FCM
- FCM integration for all roles:
  - When order placed: Notify owner.
  - When order status updated: Notify user.
  - When owner approves user request: Notify user.
  - When admin approves owner: Notify owner.
  - When user sends join request: Notify owner.
- Use `FirebaseMessagingService` to handle incoming messages, and `NotificationUtils` to show notifications.
- Store FCM tokens in RTDB under users for targeted notifications.

#### 9.4 Payments
- **Cloud meals**: Advance payment flow. On cloud order placement, create `PaymentRecord(paymentType="cloud_advance", orderId=...)` with `status="pending"`. Owner confirms payment using **Mark Paid** in owner payments screen.
- **Mess meals**: Monthly billing flow. Owner generates per-user monthly bills from **Enrolled Users** panel. Select user → view accepted mess meal preview for current month → Generate Bill button. Bill includes all **accepted (not delivered)** mess meals for that month. Bills are visible in user home (monthly summary) and user payments screen.
- Owner marking a cloud advance record as paid updates linked order `paymentStatus="paid"`.
- Payment details remain visible to both roles: user and owner payment screens.
- Owner Payments screen separates paid vs unpaid mess bills using the **Bill State** filter.
- If `/payments` node does not exist in RTDB yet, repository treats it as empty and writes new records safely.

#### 9.5 Analytics
- Use MPAndroidChart library for bar/line charts.
- Data aggregated from orders and users. Compute counts, sums, trends based on date ranges.
- For owners: fetch orders from their mess, group by day/week/month.
- For admin: fetch all orders.

#### 9.6 Search & Filtering
- Implement `SearchView` in toolbar with `Filterable` interface for RecyclerView adapters. For remote search, call API with query parameter.

---

### 10. API Design with Retrofit

We'll define a Retrofit service interface for all REST endpoints to Firebase RTDB.

```kotlin
interface FirebaseApi {
    // Generic get with query (for lists)
    @GET("{path}.json")
    suspend fun getData(@Path("path") path: String, @Query("orderBy") orderBy: String? = null, @Query("equalTo") equalTo: String? = null): Response<Map<String, Any>>

    // Get single object
    @GET("{path}/{id}.json")
    suspend fun getObject(@Path("path") path: String, @Path("id") id: String): Response<Any>

    // Post new object (push)
    @POST("{path}.json")
    suspend fun postData(@Path("path") path: String, @Body data: Any): Response<PushResponse>

    // Put update
    @PUT("{path}/{id}.json")
    suspend fun putData(@Path("path") path: String, @Path("id") id: String, @Body data: Any): Response<Void>

    // Delete
    @DELETE("{path}/{id}.json")
    suspend fun deleteData(@Path("path") path: String, @Path("id") id: String): Response<Void>
}
```

We'll use `PushResponse` to get the generated key.

For queries, we'll use `orderBy` and `equalTo` parameters as per Firebase REST API (e.g., `orderBy="status"&equalTo="pending"`).

---

### 11. MVVM Architecture Layers

#### 11.1 Data Layer
- **Repositories**: `UserRepository`, `MessRepository`, `OrderRepository`, `PaymentRepository`, `BannerRepository`, `CloudinaryRepository`.
- **Remote Data Source**: Retrofit service with Firebase endpoints.
- **Local Data Source**: Room database for offline caching (optional). We'll implement simple caching using `SharedPreferences` for user session and maybe `DataStore` for preferences.

#### 11.2 Domain Layer (Optional)
- Use cases for each operation. But for simplicity, we can directly call repositories from ViewModels.

#### 11.3 Presentation Layer
- **Activities/Fragments**: Each screen as Fragment with ViewModel.
- **ViewModel**: Hilt-injected, uses coroutines to call repositories, exposes `StateFlow` or `LiveData` for UI state.
- **UI State**: Sealed classes for different states (Loading, Success, Error).
- **Navigation**: Use Navigation Component.

---

### 12. Dependency Injection with Hilt

Create modules:
- `NetworkModule`: Provides Retrofit instance with base URL, Gson converter, OkHttp client (with logging interceptor).
- `RepositoryModule`: Binds repositories to interfaces (optional).
- `CloudinaryModule`: Provides Cloudinary instance using API key/secret.
- `FirebaseModule`: Provides FirebaseAuth, FirebaseMessaging instances.

Example:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("https://messappflutter-default-rtdb.firebaseio.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }).build())
        .build()
}
```

---

### 13. Coroutines & Flows

- Use `viewModelScope.launch` for one-time operations.
- For data that updates, use `flow { emit(...) }` or `StateFlow` from repository.
- Repository methods are `suspend` functions.

---

### 14. Notifications with FCM

- Setup Firebase Cloud Messaging in the app.
- In `FirebaseMessagingService`, save token to RTDB under `/users/{uid}/fcmToken`.
- When an event occurs (e.g., order placed), backend (or app) can send FCM message to the target user's token.
- For simplicity, we can send notifications directly from the app when state changes (e.g., after owner approves, send to user via FCM using Retrofit to Firebase Cloud Messaging API). But the FCM API requires server key. We'll use Firebase Admin SDK or simply send from the app using FCM HTTP API with server key stored in the app (not secure). Better to have a backend server. Since we don't have a server, we can rely on Firebase Realtime Database listeners to trigger local notifications (if using Firebase SDK). But with REST API, we can implement a simple cloud function using Firebase Functions to send notifications when database changes. Given constraints, we'll assume we use Firebase Cloud Functions (optional) or we can use the app's own logic: when data is updated, the app can send notification via `NotificationManager` if the user is in foreground. For background, we can use FCM with data payload and handle in service. To keep plan feasible, we'll note that FCM integration will be implemented using Firebase Messaging, and notifications will be sent from the app when actions occur (e.g., after owner updates order status, app calls FCM API to notify user). But this requires storing FCM server key; we can store it in a secure place (BuildConfig).

---

### 15. Module Structure

```
app/
├── di/                # Hilt modules
├── data/
│   ├── models/
│   ├── remote/
│   │   └── api/
│   ├── repository/
│   └── datasource/
├── domain/
│   └── usecase/
├── presentation/
│   ├── auth/
│   ├── common/
│   ├── user/
│   │   ├── home/
│   │   ├── orders/
│   │   ├── profile/
│   │   ├── mess/
│   │   └── payment/
│   ├── owner/
│   │   ├── home/
│   │   ├── orders/
│   │   ├── users/
│   │   ├── requests/
│   │   ├── payments/
│   │   ├── banners/
│   │   └── analytics/
│   └── admin/
│       ├── home/
│       ├── users/
│       ├── banners/
│       └── analytics/
├── utils/
│   ├── ImageUploader.kt
│   ├── NotificationHelper.kt
│   └── DateUtils.kt
└── MyApplication.kt
```

---

### 16. Security Considerations
- Use Firebase Security Rules to restrict read/write based on user role and UID.
- Store API keys (Cloudinary, FCM server key) in `local.properties` and access via BuildConfig.
- Use HTTPS for all network calls.
- Validate user input on client side.
- For payment, we'll use secure payment gateway integration (like Razorpay) later.

---

### 17. Testing Strategy
- **Unit Tests**: Test repositories and ViewModels with mock data.
- **UI Tests**: Use Espresso for critical flows (login, order placement).
- **Integration Tests**: Test Retrofit API calls with MockWebServer.

---

### 18. Implementation Roadmap
1. Set up project with required dependencies.
2. Implement authentication and user registration.
3. Implement role-based navigation.
4. Implement data layer (Retrofit, models, repositories).
5. Build core screens: user home, order placement, order history.
6. Implement owner screens: orders, enrollments, requests.
7. Implement admin screens.
8. Add Cloudinary image upload.
9. Integrate FCM notifications.
10. Add analytics and charts.
11. Add search, filters, payments.
12. Test and polish.

---

### 19. Conclusion
This plan provides a detailed blueprint for building **My Mess Manager** Android app using the specified tech stack. It covers all required features, roles, and flows, ensuring a scalable, maintainable architecture. The use of MVVM, Hilt, Coroutines, and Retrofit ensures separation of concerns and testability. The app will deliver a seamless experience for mess users, owners, and admins.