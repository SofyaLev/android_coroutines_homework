package com.tinkoff.android_homework.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinkoff.android_homework.domain.main.usecases.SubscribeOperationsUseCase
import com.tinkoff.android_homework.domain.main.usecases.SubscribeTotalUseCase
import com.tinkoff.android_homework.presentation.mappers.operations.OperationToUiItemMapper
import com.tinkoff.android_homework.presentation.model.operations.OperationItem
import com.tinkoff.android_homework.presentation.model.operations.OperationType
import com.tinkoff.android_homework.presentation.model.total.TotalItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * @author d.shtaynmets
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val subscribeTotalUseCase: SubscribeTotalUseCase,
    private val subscribeOperationsUseCase: SubscribeOperationsUseCase,
    val uiMapper: OperationToUiItemMapper,
) : ViewModel() {

    private val _operations: MutableStateFlow<List<OperationItem>> = MutableStateFlow(emptyList())
    val operations: StateFlow<List<OperationItem>> = _operations.asStateFlow()

    private val _total: MutableStateFlow<TotalItem?> = MutableStateFlow(null)
    val total: StateFlow<TotalItem?> = _total.asStateFlow()

    init {
        viewModelScope.launch {
            _operations.value =
                subscribeOperationsUseCase
                .invoke()
                .operations
                .map { uiMapper.invoke(it) }

            _total.value = subscribeTotalUseCase
                    .invoke()
                    .map { total ->
                        val income = _operations
                            .value
                            .filter { it.operationType == OperationType.INCOME }
                            .map { it.operationSum }.sum()

                        val outcome = _operations
                            .value
                            .filter { it.operationType == OperationType.OUTCOME }
                            .map { it.operationSum }.sum()

                        val progress = (outcome.toFloat() / income.toFloat()) * 100f

                        TotalItem(
                            total = total.amount,
                            income = income,
                            outcome = outcome,
                            progress = progress
                        )
                    }.first()
            }
        }
    }
