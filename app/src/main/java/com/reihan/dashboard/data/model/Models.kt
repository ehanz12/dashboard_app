package com.reihan.dashboard.data.model

import com.google.gson.annotations.SerializedName

// --- AUTHENTICATION ---
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    @SerializedName("Message") val message: String?,
    @SerializedName("token") val token: String?
)

data class UserResponse(
    val id: String,
    val name: String,
    val email: String
)

data class MeResponse(
    @SerializedName("Message") val message: String?,
    @SerializedName("data") val data: UserResponse?
)

// --- CATEGORIES ---
data class CategoryRequest(
    val name: String
)

data class CategoryResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    val name: String
)

data class CategoryListResponse(
    val data: List<CategoryResponse>
)

data class CategoryMiniResponse(
    val id: String,
    val name: String
)

// --- TRANSACTIONS ---
data class CreateTransactionRequest(
    @SerializedName("category_id") val categoryId: String?,
    val amount: Double,
    val type: String, // "income" or "expense"
    val description: String,
    @SerializedName("transaction_date") val transactionDate: String // YYYY-MM-DD
)

data class UpdateTransactionRequest(
    @SerializedName("category_id") val categoryId: String? = null,
    val amount: Double? = null,
    val type: String? = null,
    val description: String? = null,
    @SerializedName("transaction_date") val transactionDate: String? = null
)

data class TransactionResponse(
    val id: String,
    val category: CategoryMiniResponse?,
    val amount: Double,
    val type: String,
    val description: String,
    @SerializedName("transaction_date") val transactionDate: String
)

data class TransactionsQueryResponse(
    val data: List<TransactionResponse>,
    val total: Int,
    val page: Int,
    val limit: Int,
    @SerializedName("totalPages") val totalPages: Int
)

// --- HABITS ---
data class CreateHabitRequest(
    val name: String,
    val frequency: String // "daily" or "weekly"
)

data class HabitResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    val name: String,
    val frequency: String,
    @SerializedName("created_at") val createdAt: String
)

data class HabitsQueryResponse(
    val data: List<HabitResponse>?,
    val total: Int,
    val page: Int,
    val limit: Int,
    @SerializedName("total_pages") val totalPages: Int
)

data class HabitLogTodayResponse(
    @SerializedName("habit_id") val habitId: String,
    val name: String,
    val completed: Boolean
)

data class HabitLogsTodayList(
    val data: List<HabitLogTodayResponse>?,
    @SerializedName("Message") val message: String?
)

data class ToggleHabitLogRequest(
    @SerializedName("habit_id") val habitId: String
)

data class ToggleHabitLogResponse(
    @SerializedName("Message") val message: String?,
    val data: HabitLogDetails?
)

data class HabitLogDetails(
    val id: String,
    val habit: HabitSuperMini,
    val date: String,
    @SerializedName("is_complete") val isComplete: Boolean
)

data class HabitSuperMini(
    val id: String,
    val name: String
)

// --- JOURNAL ---
data class CreateJournalRequest(
    val mood: Int,         // 1-5
    val content: String,
    @SerializedName("entry_date") val entryDate: String // YYYY-MM-DD
)

data class JournalResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    val mood: Int,
    val content: String,
    @SerializedName("entry_date") val entryDate: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class JournalListResponse(
    val message: String?,
    val data: List<JournalResponse>?
)

data class JournalSingleResponse(
    val message: String?,
    val data: JournalResponse?
)

// --- TASKS ---
data class CreateTaskRequest(
    val title: String,
    val description: String = "",
    val quadrant: Int = 1,        // 1=Urgent+Important, 2=Not Urgent+Important, 3=Urgent+Not Important, 4=Not Urgent+Not Important
    @SerializedName("due_date") val dueDate: String? = null  // RFC3339 or null
)

data class UpdateTaskRequest(
    val title: String? = null,
    val description: String? = null,
    val quadrant: Int? = null,
    @SerializedName("is_completed") val isCompleted: Boolean? = null,
    @SerializedName("due_date") val dueDate: String? = null
)

data class TaskResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    val title: String,
    val description: String,
    val quadrant: Int,
    @SerializedName("is_completed") val isCompleted: Boolean,
    @SerializedName("due_date") val dueDate: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class TaskListResponse(
    val message: String?,
    val data: List<TaskResponse>?
)

data class TaskSingleResponse(
    val message: String?,
    val data: TaskResponse?
)

// --- TIMEBLOCKS ---
data class CreateTimeblockRequest(
    @SerializedName("activity_name") val activityName: String,
    @SerializedName("start_time") val startTime: String,  // "HH:MM"
    @SerializedName("end_time") val endTime: String,      // "HH:MM"
    @SerializedName("color_code") val colorCode: String = "#4F46E5",
    @SerializedName("day_of_week") val dayOfWeek: Int     // 1=Monday ... 7=Sunday
)

data class TimeblockResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("activity_name") val activityName: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("color_code") val colorCode: String,
    @SerializedName("day_of_week") val dayOfWeek: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class TimeblockListResponse(
    val message: String?,
    val data: List<TimeblockResponse>?
)

data class TimeblockSingleResponse(
    val message: String?,
    val data: TimeblockResponse?
)
