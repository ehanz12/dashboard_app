package com.reihan.dashboard.data.model

import com.google.gson.annotations.SerializedName

// --- AUTHENTICATION ---
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    @SerializedName("nomor_hp") val phoneNumber: String,
    val bio: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class VerifyEmailRequest(
    @SerializedName("email") val email: String,
    @SerializedName("verification_code") val verificationCode: String
)

data class ForgotPasswordRequest(
    val email: String
)

data class ResetPasswordRequest(
    val email: String,
    val code: String,
    @SerializedName("new_password") val newPassword: String
)

data class AuthResponse(
    @SerializedName("message", alternate = ["Message"]) val message: String?,
    @SerializedName("token", alternate = ["Token"]) val token: String?,
    @SerializedName("data") val data: AuthData? = null
)

data class AuthData(
    @SerializedName("token", alternate = ["Token"]) val token: String?
)

data class EditProfileRequest(
    val name: String,
    val email: String,
    @SerializedName("nomor_hp") val phoneNumber: String,
    val bio: String
)

data class ChangePasswordRequest(
    @SerializedName("current_password") val oldPassword: String,
    @SerializedName("new_password") val newPassword: String,
    @SerializedName("new_password_confirmation") val newPasswordConfirmation: String
)

data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    @SerializedName("nomor_hp") val phoneNumber: String?,
    val bio: String?
)

data class MeResponse(
    @SerializedName("Message") val message: String?,
    @SerializedName("data") val data: UserResponse?
)

// --- GOOGLE SIGN-IN ---
data class GoogleSignInRequest(
    @SerializedName("id_token") val idToken: String
)

data class GoogleSignUpRequest(
    @SerializedName("id_token") val idToken: String,
    val name: String? = null,
    @SerializedName("nomor_hp") val phoneNumber: String? = null,
    val bio: String? = null
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
    val frequency: String, // "daily" or "weekly"
    val days: List<String>? = null, // ["Monday", "Tuesday", ...]
    @SerializedName("reminder_time") val reminderTime: String? = null,
    @SerializedName("reminder_enabled") val reminderEnabled: Boolean? = null
)

data class HabitResponse(
    val id: String,
    @SerializedName("user_id") val userId: String? = null,
    val name: String,
    val frequency: String,
    val days: List<String>?,
    @SerializedName("reminder_time") val reminderTime: String?,
    @SerializedName("reminder_enabled") val reminderEnabled: Boolean,
    @SerializedName("created_at") val createdAt: String
)

data class HabitMini(
    val id: String,
    val name: String,
    val frequency: String,
    val days: List<String>?,
    @SerializedName("reminder_time") val reminderTime: String?,
    @SerializedName("reminder_enabled") val reminderEnabled: Boolean,
    @SerializedName("created_at") val createdAt: String
)

data class HabitSingleResponse(
    @SerializedName("Message") val message: String?,
    val data: HabitResponse?
)

data class HabitsQueryResponse(
    val data: List<HabitMini>?,
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
    @SerializedName("day_of_week") val dayOfWeek: Int? = null, // 1=Monday ... 7=Sunday
    val date: String? = null // YYYY-MM-DD
)

data class TimeblockResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("activity_name") val activityName: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("color_code") val colorCode: String,
    @SerializedName("day_of_week") val dayOfWeek: Int?,
    val date: String?,
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

// --- SCHEDULE ---
data class ScheduleResponse(
    @SerializedName("habits") val habits: List<HabitLogTodayResponse>?,
    @SerializedName("timeblocks") val timeblocks: List<TimeblockResponse>?
)

data class ScheduleDataResponse(
    val message: String?,
    val data: ScheduleResponse?
)

// --- DASHBOARD ---
data class DashboardResponse(
    val message: String,
    val data: DashboardData
)

data class DashboardData(
    val finance: FinanceSummary,
    val habits: HabitSummary,
    val tasks: TaskSummary,
    val schedule: ScheduleSummary
)

data class FinanceSummary(
    val balance: Double,
    val income: Double,
    val expense: Double,
    val saving: Double
)

data class HabitSummary(
    @SerializedName("completed_today") val completedToday: Int,
    @SerializedName("total_today") val totalToday: Int,
    @SerializedName("current_streak") val currentStreak: Int,
    @SerializedName("longest_streak") val longestStreak: Int,
    @SerializedName("completion_rate") val completionRate: Int,
    @SerializedName("completed_days") val completedDays: Int
)

data class TaskSummary(
    @SerializedName("completed_today") val completedToday: Int,
    @SerializedName("total_today") val totalToday: Int,
    @SerializedName("completion_rate") val completionRate: Int,
    val overdue: Int
)

data class ScheduleSummary(
    @SerializedName("today_events") val todayEvents: Int,
    @SerializedName("next_event") val nextEvent: String?,
    @SerializedName("next_time") val nextTime: String?
)

// --- DEVICES ---
data class UserDeviceRequest(
    @SerializedName("fmc_token")
    val fmcToken: String,

    @SerializedName("device_type")
    val deviceType: String
)

// --- ANALYTICS ---

data class AnalyticsSummaryResponse(
    val message: String,
    val data: AnalyticsSummaryData
)

data class AnalyticsSummaryData(
    val finance: FinanceAnalyticsSummary,
    val habit: HabitAnalyticsSummary,
    val task: TaskAnalyticsSummary
)

data class FinanceAnalyticsSummary(
    val income: Double,
    val expense: Double,
    val balance: Double
)

data class HabitAnalyticsSummary(
    @SerializedName("today_completed") val todayCompleted: Int,
    @SerializedName("today_total") val todayTotal: Int,
    @SerializedName("completion_rate") val completionRate: Int,
    @SerializedName("current_streak") val currentStreak: Int,
    @SerializedName("longest_streak") val longestStreak: Int
)

data class TaskAnalyticsSummary(
    val completed: Int,
    val pending: Int,
    @SerializedName("completion_rate") val completionRate: Int
)

data class FinanceAnalyticsResponse(
    val message: String,
    val data: List<FinanceAnalyticsItem>
)

data class FinanceAnalyticsItem(
    val label: String,
    val income: Double,
    val expense: Double
)

data class CategoryAnalyticsResponse(
    val message: String,
    val data: List<CategoryAnalyticsItem>
)

data class CategoryAnalyticsItem(
    val category: String,
    val amount: Double,
    val percentage: Double
)

data class HabitAnalyticsResponse(
    val message: String,
    val data: List<HabitAnalyticsItem>
)

data class HabitAnalyticsItem(
    val label: String,
    val completed: Int,
    val total: Int,
    val percentage: Int
)

data class TaskAnalyticsResponse(
    val message: String,
    val data: List<TaskAnalyticsItem>
)

data class TaskAnalyticsItem(
    val label: String,
    val completed: Int
)

data class StreakAnalyticsResponse(
    val message: String,
    val data: StreakAnalyticsData
)

data class StreakAnalyticsData(
    @SerializedName("current_streak") val currentStreak: Int,
    @SerializedName("longest_streak") val longestStreak: Int,
    @SerializedName("completion_rate") val completionRate: Int
)
