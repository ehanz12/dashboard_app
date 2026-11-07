package com.reihan.dashboard.data.network

import com.reihan.dashboard.data.model.*
import okhttp3.ResponseBody
import retrofit2.http.*

interface ApiService {

    // --- AUTHENTICATION ---
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): AuthResponse

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): AuthResponse

    @POST("auth/verify-email")
    suspend fun verifyEmail(
        @Body request: VerifyEmailRequest
    ): AuthResponse

    @POST("auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequest
    ): AuthResponse

    @POST("auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): AuthResponse

    @GET("auth/me")
    suspend fun getMe(): MeResponse

    @PUT("auth/edit")
    suspend fun editProfile(
        @Body request: EditProfileRequest
    ): MeResponse

    @PATCH("auth/change-password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): AuthResponse

    @POST("auth/google")
    suspend fun googleAuth(
        @Body request: GoogleSignUpRequest
    ): AuthResponse

    // --- CATEGORIES ---
    @GET("category")
    suspend fun getCategories(): CategoryListResponse

    @POST("category")
    suspend fun createCategory(
        @Body request: CategoryRequest
    ): CategoryResponse

    @PATCH("category/{id}")
    suspend fun updateCategory(
        @Path("id") id: String,
        @Body request: CategoryRequest
    ): CategoryResponse

    @DELETE("category/{id}")
    suspend fun deleteCategory(
        @Path("id") id: String
    ): ResponseBody

    // --- TRANSACTIONS ---
    @GET("transactions")
    suspend fun getTransactions(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("type") type: String? = null,
        @Query("category_id") categoryId: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("search") search: String? = null
    ): TransactionsQueryResponse

    @POST("transactions")
    suspend fun createTransaction(
        @Body request: CreateTransactionRequest
    ): TransactionResponse

    @PATCH("transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") id: String,
        @Body request: UpdateTransactionRequest
    ): TransactionResponse

    @DELETE("transactions/{id}")
    suspend fun deleteTransaction(
        @Path("id") id: String
    ): ResponseBody

    // --- HABITS ---
    @GET("habits")
    suspend fun getHabits(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("search") search: String? = null
    ): HabitsQueryResponse

    @POST("habits")
    suspend fun createHabit(
        @Body request: CreateHabitRequest
    ): HabitSingleResponse

    @PATCH("habits/{id}")
    suspend fun updateHabit(
        @Path("id") id: String,
        @Body request: CreateHabitRequest
    ): HabitSingleResponse

    @DELETE("habits/{id}")
    suspend fun deleteHabit(
        @Path("id") id: String
    ): ResponseBody

    @GET("habits/logs/today")
    suspend fun getHabitLogsToday(): HabitLogsTodayList

    @POST("habits/toggle")
    suspend fun toggleHabitLog(
        @Body request: ToggleHabitLogRequest
    ): ToggleHabitLogResponse

    // --- TASKS ---
    @GET("tasks")
    suspend fun getTasks(
        @Query("completed") completed: Boolean? = null
    ): TaskListResponse

    @POST("tasks")
    suspend fun createTask(
        @Body request: CreateTaskRequest
    ): TaskSingleResponse

    @PATCH("tasks/{id}")
    suspend fun updateTask(
        @Path("id") id: String,
        @Body request: UpdateTaskRequest
    ): TaskSingleResponse

    @PATCH("tasks/{id}/toggle")
    suspend fun toggleTaskStatus(
        @Path("id") id: String
    ): TaskSingleResponse

    @DELETE("tasks/{id}")
    suspend fun deleteTask(
        @Path("id") id: String
    ): okhttp3.ResponseBody

    // --- TIMEBLOCKS ---
    @GET("timeblocks")
    suspend fun getTimeblocks(
        @Query("day_of_week") dayOfWeek: Int? = null
    ): TimeblockListResponse

    @POST("timeblocks")
    suspend fun createTimeblock(
        @Body request: CreateTimeblockRequest
    ): TimeblockSingleResponse

    @PATCH("timeblocks/{id}")
    suspend fun updateTimeblock(
        @Path("id") id: String,
        @Body request: CreateTimeblockRequest
    ): TimeblockSingleResponse

    @DELETE("timeblocks/{id}")
    suspend fun deleteTimeblock(
        @Path("id") id: String
    ): okhttp3.ResponseBody

    // --- SCHEDULE ---
    @GET("schedule/today")
    suspend fun getSchedule(
        @Query("date") date: String? = null // YYYY-MM-DD
    ): ScheduleDataResponse

    // --- DASHBOARD ---
    @GET("dashboard")
    suspend fun getDashboard(): DashboardResponse

    // --- ANALYTICS ---
    @GET("analytics/summary")
    suspend fun getAnalyticsSummary(
        @Query("type") type: String,
        @Query("month") month: Int? = null,
        @Query("year") year: Int
    ): AnalyticsSummaryResponse

    @GET("analytics/finance")
    suspend fun getFinanceAnalytics(
        @Query("type") type: String,
        @Query("month") month: Int? = null,
        @Query("year") year: Int
    ): FinanceAnalyticsResponse

    @GET("analytics/categories")
    suspend fun getCategoryAnalytics(
        @Query("type") type: String,
        @Query("month") month: Int? = null,
        @Query("year") year: Int
    ): CategoryAnalyticsResponse

    @GET("analytics/habits")
    suspend fun getHabitAnalytics(
        @Query("type") type: String,
        @Query("month") month: Int? = null,
        @Query("year") year: Int
    ): HabitAnalyticsResponse

    @GET("analytics/tasks")
    suspend fun getTaskAnalytics(
        @Query("type") type: String,
        @Query("month") month: Int? = null,
        @Query("year") year: Int
    ): TaskAnalyticsResponse

    @GET("analytics/streak")
    suspend fun getStreakAnalytics(): StreakAnalyticsResponse

    // --- DEVICES ---
    @POST("devices")
    suspend fun saveDevice(
        @Body request: UserDeviceRequest
    ): retrofit2.Response<okhttp3.ResponseBody>

    @HTTP(method = "DELETE", path = "devices", hasBody = true)
    suspend fun deleteDevice(
        @Body request: UserDeviceRequest
    ): retrofit2.Response<okhttp3.ResponseBody>
}
