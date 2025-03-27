package schwarz.it.lws.win.model

import com.fasterxml.jackson.annotation.JsonProperty

data class OpenWeatherMapResponse(
    val cod: String,
    val message: Int,
    val cnt: Int,
    val list: List<ForecastItem>,
    val city: City
)

data class ForecastItem(
    val dt: Long,
    val main: Main,
    val weather: List<Weather>,
    val clouds: Clouds,
    val wind: Wind,
    val visibility: Int,
    val pop: Double,
    val rain: Rain?,
    val sys: Sys,
    @JsonProperty("dt_txt") val dtTxt: String
)

data class Main(
    val temp: Double,
    @JsonProperty("feels_like") val feelsLike: Double,
    @JsonProperty("temp_min") val tempMin: Double,
    @JsonProperty("temp_max") val tempMax: Double,
    val pressure: Int,
    @JsonProperty("sea_level") val seaLevel: Int,
    @JsonProperty("grnd_level") val grndLevel: Int,
    val humidity: Int,
    @JsonProperty("temp_kf") val tempKf: Double
)

data class Weather(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

data class Clouds(
    val all: Int
)

data class Wind(
    val speed: Double,
    val deg: Int,
    val gust: Double
)

data class Rain(
    @JsonProperty("3h") val threeHour: Double
)

data class Sys(
    val pod: String
)

data class City(
    val id: Int,
    val name: String,
    val coord: Coord,
    val country: String,
    val population: Int,
    val timezone: Int,
    val sunrise: Long,
    val sunset: Long
)

data class Coord(
    val lat: Double,
    val lon: Double
)