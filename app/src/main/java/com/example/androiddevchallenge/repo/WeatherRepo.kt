package com.example.androiddevchallenge.repo

import com.example.androiddevchallenge.R
import com.example.androiddevchallenge.data.WeatherInfo
import java.util.*
import kotlin.collections.ArrayList


class WeatherRepo {

    private val weatherPics = arrayOf(
        R.drawable.cloudy, R.drawable.rainy,
        R.drawable.shower, R.drawable.sunny
    )

    private val calendar: Calendar = Calendar.getInstance()

    /**
     * Just a dummy information for the upcoming ten days.
     */
    fun dummyTemperatureInformation(): ArrayList<WeatherInfo> {
        val listOfTemperature = ArrayList<WeatherInfo>()

        for (number in 0..9) {
            calendar.add(Calendar.DATE, 1)
            listOfTemperature.add(
                WeatherInfo(
                    "${(8..40).random()}°C",
                    "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}",
                    weatherPics[(0..3).random()]
                )
            )
        }

        return listOfTemperature
    }
}