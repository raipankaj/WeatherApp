/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androiddevchallenge

import android.graphics.Path
import android.graphics.Typeface
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androiddevchallenge.data.WeatherInfo
import com.example.androiddevchallenge.repo.WeatherRepo
import com.example.androiddevchallenge.ui.theme.WeatherTheme
import com.example.androiddevchallenge.viewmodel.WeatherViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {

                    Box {
                        CloudBackground()
                        Weather()
                    }
                }
            }
        }
    }
}

/**
 * Set the image of cloud as the background for the screen.
 * Also animate the scaleX, scaleY for the image so as to
 * look like the floating clouds.
 */
@Composable
private fun CloudBackground() {
    var doReverseAnimation by remember { mutableStateOf(true) }

    val animateScaleX by animateFloatAsState(
        targetValue = if (doReverseAnimation) 1f else 2.5f,
        tween(15_500)
    )

    val animateScaleY by animateFloatAsState(
        targetValue = if (doReverseAnimation) 1f else 2f,
        tween(15_500)
    )

    LaunchedEffect(Unit) {
        while (isActive) {
            doReverseAnimation = doReverseAnimation.not()
            delay(15_500)
        }
    }

    Image(
        painter = painterResource(id = R.drawable.clouds_bg),
        contentDescription = stringResource(id = R.string.description_cloud_background),
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.5f)
            .graphicsLayer {
                scaleX = animateScaleX
                scaleY = animateScaleY
            }
    )
}

/**
 * Display the weather dialer to show current temperature
 * and the list of weather predictions for the next ten
 * days at the bottom of the screen.
 */
@Composable
private fun Weather() {

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(
                text = stringResource(id = R.string.app_name),
                style = TextStyle(
                    fontWeight = FontWeight.ExtraBold, fontSize = 26.sp
                ),
                modifier = Modifier.padding(26.dp)
            )

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WeatherDialer(
                    WeatherInfo(
                        stringResource(id = R.string.sunny_24_degree),
                        stringResource(id = R.string.dummy_date),
                        R.drawable.cloudy
                    )
                )
            }

            Text(
                text = stringResource(id = R.string.current_location),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                style = TextStyle(
                    fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            )
        }

        FutureForecast()
    }
}

@Composable
fun WeatherDialer(currentWeatherInfo: WeatherInfo) {
    val localContext = LocalContext.current
    var textToSpeech: TextToSpeech? = null

    SideEffect {
        textToSpeech = TextToSpeech(localContext) { status ->
            if (status != TextToSpeech.ERROR) {
                textToSpeech?.language = Locale.UK
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
    }

    var isDialerClicked by remember { mutableStateOf(false) }

    val rotationOnY by animateFloatAsState(
        targetValue = if (isDialerClicked) 0f else 360f,
        tween(3000)
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(300.dp)
            .graphicsLayer {
                rotationY = rotationOnY
            }
            .clip(CircleShape)
            .border(4.dp, Color.LightGray, CircleShape)
            .border(6.dp, Color.White, CircleShape)
            .border(8.dp, Color.LightGray, CircleShape)
            .border(12.dp, Color.White, CircleShape)
            .graphicsLayer {
                rotationZ = rotationOnY
            }
            .clickable {
                textToSpeech?.speak(
                    currentWeatherInfo.temperature,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    null
                )
                isDialerClicked = isDialerClicked.not()
            },
    ) {

        Column {
            Image(
                painter = painterResource(id = currentWeatherInfo.weatherImage),
                contentDescription = stringResource(id = R.string.description_current_weather),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentScale = ContentScale.Crop
            )

            Text(
                text = currentWeatherInfo.temperature,
                style = TextStyle(
                    fontSize = 58.sp, fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Cursive,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(26.dp),
                textAlign = TextAlign.Center
            )
        }

        CircularText()
    }
}

@Composable
fun CircularText() {
    val paint = Paint().asFrameworkPaint()

    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxSize()
            .rotate(180f)
            .padding(12.dp)
    ) {
        paint.apply {
            isAntiAlias = false
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        drawIntoCanvas {
            val path = Path()
            path.addCircle(350f, 350f, 80f, Path.Direction.CW)
            it.nativeCanvas.drawTextOnPath(
                "click here to speak out temperature",
                path,
                0f,
                0f,
                paint
            )
        }
    }
}

@Composable
fun FutureForecast(
    weatherViewModel: WeatherViewModel = viewModel(
        factory = WeatherRepoFactory(WeatherRepo())
    )
) {
    val weatherPredictions by weatherViewModel.weatherPredictions.collectAsState()

    Column {
        WeatherPredictions(weatherPredictions)
        Quotes()
    }
}

@Composable
fun WeatherPredictions(weatherPredictions: List<WeatherInfo>?) {
    weatherPredictions?.let {
        LazyRow(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            items(weatherPredictions) {
                WeatherInformation(weatherInfo = it)
            }
        }
    } ?: Text(text = stringResource(id = R.string.please_wait))
}

@Composable
fun WeatherInformation(weatherInfo: WeatherInfo) {

    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = weatherInfo.weatherImage),
            contentDescription = stringResource(id = R.string.description_weather_icon),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(70.dp)
                .clip(CutCornerShape(20))
                .border(4.dp, Color.LightGray, CutCornerShape(20))
        )

        Spacer(modifier = Modifier.padding(4.dp))

        Text(
            text = weatherInfo.temperature,
            style = TextStyle(fontSize = 18.sp)
        )
        Text(
            text = weatherInfo.date,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Light
            )
        )
    }
}

/**
 * Show random quotes at the bottom of the screen with rounded
 * shape background and white color text.
 */
@Composable
fun Quotes() {
    val quotesArray = stringArrayResource(id = R.array.quotes)
    val quotes = quotesArray[(quotesArray.indices).random()]

    Text(
        text = quotes,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStartPercent = 40, topEndPercent = 40))
            .background(Color.DarkGray)
            .padding(12.dp),
        style = TextStyle(color = Color.White, fontStyle = FontStyle.Italic)
    )
}

/**
 * A factory class to create object for the WeatherViewModel with a
 * constructor object of WeatherRepo.
 */
class WeatherRepoFactory(val weatherRepo: WeatherRepo) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            return WeatherViewModel(weatherRepo) as T
        }

        throw IllegalStateException("Please create WeatherViewModel instance")
    }
}
