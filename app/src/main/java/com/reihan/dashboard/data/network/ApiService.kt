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

    @GET("auth/me")
    suspend fun getMe(): MeResponse

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
    ): HabitResponse

    @PATCH("habits/{id}")
    suspend fun updateHabit(
        @Path("id") id: String,
        @Body request: CreateHabitRequest
    ): HabitResponse

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

    // --- JOURNAL ---
    @GET("journals")
    suspend fun getJournals(): JournalListResponse

    @GET("journals/date/{date}")
    suspend fun getJournalByDate(
        @Path("date") date: String // YYYY-MM-DD
    ): JournalSingleResponse

    @POST("journals")
    suspend fun createOrUpdateJournal(
        @Body request: CreateJournalRequest
    ): JournalSingleResponse

    @DELETE("journals/{id}")
    suspend fun deleteJournal(
        @Path("id") id: String
    ): okhttp3.ResponseBody

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
}
