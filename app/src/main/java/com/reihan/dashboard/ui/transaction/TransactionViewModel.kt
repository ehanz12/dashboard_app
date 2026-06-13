package com.reihan.dashboard.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reihan.dashboard.data.model.*
import com.reihan.dashboard.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class TransactionState {
    object Loading : TransactionState()
    data class Success(
        val transactions: List<TransactionResponse>,
        val categories: List<CategoryResponse>,
        val total: Int,
        val totalPages: Int
    ) : TransactionState()
    data class Error(val message: String) : TransactionState()
}

class TransactionViewModel : ViewModel() {

    private val _state = MutableStateFlow<TransactionState>(TransactionState.Loading)
    val state: StateFlow<TransactionState> = _state

    private var currentPage = 1
    private var currentLimit = 10
    private var currentType: String? = null
    private var currentCategoryId: String? = null
    private var currentSearch: String? = null
    private var currentStartDate: String? = null
    private var currentEndDate: String? = null

    fun loadTransactionsAndCategories(
        page: Int = 1,
        type: String? = null,
        categoryId: String? = null,
        search: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ) {
        currentPage = page
        currentType = if (type == "All") null else type
        currentCategoryId = if (categoryId == "All") null else categoryId
        currentSearch = if (search.isNullOrBlank()) null else search
        currentStartDate = if (startDate.isNullOrBlank()) null else startDate
        currentEndDate = if (endDate.isNullOrBlank()) null else endDate

        viewModelScope.launch {
            _state.value = TransactionState.Loading
            try {
                // Fetch categories
                val categoriesRes = RetrofitClient.apiService.getCategories()
                val categories = categoriesRes.data

                // Fetch filtered transactions
                val transactionsRes = RetrofitClient.apiService.getTransactions(
                    page = currentPage,
                    limit = currentLimit,
                    type = currentType,
                    categoryId = currentCategoryId,
                    search = currentSearch,
                    startDate = currentStartDate,
                    endDate = currentEndDate
                )

                _state.value = TransactionState.Success(
                    transactions = transactionsRes.data,
                    categories = categories,
                    total = transactionsRes.total,
                    totalPages = transactionsRes.totalPages
                )
            } catch (e: Exception) {
                _state.value = TransactionState.Error(e.localizedMessage ?: "Failed to fetch data")
            }
        }
    }

    fun addTransaction(
        categoryId: String?,
        amount: Double,
        type: String,
        description: String,
        transactionDate: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val req = CreateTransactionRequest(
                    categoryId = if (categoryId == "") null else categoryId,
                    amount = amount,
                    type = type.lowercase(),
                    description = description,
                    transactionDate = transactionDate
                )
                RetrofitClient.apiService.createTransaction(req)
                loadTransactionsAndCategories(currentPage, currentType, currentCategoryId, currentSearch, currentStartDate, currentEndDate)
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to add transaction")
            }
        }
    }

    fun editTransaction(
        id: String,
        categoryId: String?,
        amount: Double,
        type: String,
        description: String,
        transactionDate: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val req = UpdateTransactionRequest(
                    categoryId = if (categoryId == "") null else categoryId,
                    amount = amount,
                    type = type.lowercase(),
                    description = description,
                    transactionDate = transactionDate
                )
                RetrofitClient.apiService.updateTransaction(id, req)
                loadTransactionsAndCategories(currentPage, currentType, currentCategoryId, currentSearch, currentStartDate, currentEndDate)
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to edit transaction")
            }
        }
    }

    fun removeTransaction(id: String, onFailure: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.deleteTransaction(id)
                loadTransactionsAndCategories(currentPage, currentType, currentCategoryId, currentSearch, currentStartDate, currentEndDate)
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to delete transaction")
            }
        }
    }

    fun addCategory(name: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.createCategory(CategoryRequest(name))
                loadTransactionsAndCategories(currentPage, currentType, currentCategoryId, currentSearch, currentStartDate, currentEndDate)
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to add category")
            }
        }
    }

    fun editCategory(id: String, name: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.updateCategory(id, CategoryRequest(name))
                loadTransactionsAndCategories(currentPage, currentType, currentCategoryId, currentSearch, currentStartDate, currentEndDate)
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to edit category")
            }
        }
    }

    fun removeCategory(id: String, onFailure: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.deleteCategory(id)
                loadTransactionsAndCategories(currentPage, currentType, currentCategoryId, currentSearch, currentStartDate, currentEndDate)
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to delete category")
            }
        }
    }
}
