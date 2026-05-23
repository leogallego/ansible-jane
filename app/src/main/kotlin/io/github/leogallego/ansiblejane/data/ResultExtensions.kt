package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.AppError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

sealed interface ResultState<out T> {
    data object Loading : ResultState<Nothing>
    data class Success<T>(val data: T) : ResultState<T>
    data class Error(val error: AppError) : ResultState<Nothing>
}

fun <T> Flow<T>.asResult(): Flow<ResultState<T>> =
    map<T, ResultState<T>> { ResultState.Success(it) }
        .onStart { emit(ResultState.Loading) }
        .catch { emit(ResultState.Error(AppError.from(it))) }
