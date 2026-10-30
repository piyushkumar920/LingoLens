package com.example.core.state

import com.example.core.result.DomainError

/**
 * Standard UI State pattern to ensure every screen supports
 * Loading, Success, Error, Empty, PermissionDenied, and Offline states.
 */
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data class Loading(val message: String? = null) : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Empty(val message: String = "No content found.") : UiState<Nothing>
    data class Error(val error: DomainError) : UiState<Nothing>
    data class PermissionRequired(val permission: String, val rationale: String) : UiState<Nothing>
    data class Offline(val fallbackData: Any? = null, val message: String = "Operating in offline mode") : UiState<Nothing>
}
