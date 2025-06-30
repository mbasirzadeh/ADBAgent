package com.example.adbagent.di

import com.example.adbagent.data.datasource.local.datastore.DataStore
import com.example.adbagent.domain.usecase.ConfigureAdbUseCase
import com.example.adbagent.presentation.ui.main.MainViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val AppModule = module {
    single { DataStore(androidApplication()) }
    single { ConfigureAdbUseCase(get(), get()) }
    viewModel { MainViewModel(get(), get()) }
}