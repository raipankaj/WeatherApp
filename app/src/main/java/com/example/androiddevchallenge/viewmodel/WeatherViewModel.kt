package com.example.androiddevchallenge.viewmodel

import androidx.lifecycle.ViewModel
import com.example.androiddevchallenge.data.WeatherInfo
import com.example.androiddevchallenge.repo.WeatherRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WeatherViewModel(weatherRepo: WeatherRepo): ViewModel() {

    private val _weatherPredictions = MutableStateFlow<List<WeatherInfo>?>(null)

    init {
        _weatherPredictions.value = weatherRepo.dummyTemperatureInformation()
    }

    val weatherPredictions = _weatherPredictions.asStateFlow()
}