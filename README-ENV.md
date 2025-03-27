# Using Environment Variables with .env File

This document explains how to use the `.env` file to store and access environment variables, specifically the OpenWeatherMap API key, in this application.

## Setup

1. The project is configured to use the `.env` file located at the root of the project.
2. The `.env` file is already added to `.gitignore` to ensure sensitive information is not committed to the repository.
3. The `dotenv-kotlin` library is used to read environment variables from the `.env` file.

## How to Use

1. Open the `.env` file at the root of the project.
2. Replace `your_api_key_here` with your actual OpenWeatherMap API key:
   ```
   OPENWEATHERMAP_API_KEY=your_actual_api_key_here
   ```
3. Save the file.

## How It Works

The application uses the `dotenv-kotlin` library to read environment variables from the `.env` file. This is implemented in the `WeatherService` class:

```kotlin
// Load environment variables from .env file
private val dotenv = dotenv {
    directory = "./"
    ignoreIfMissing = true
}

// Get the OpenWeatherMap API key from .env file
private val apiKey: String = dotenv["OPENWEATHERMAP_API_KEY"]
```

## Example Usage

The `WeatherController` provides two endpoints to demonstrate how the API key is used:

1. `GET /api/weather/{city}` - Returns a message indicating how weather data for the specified city would be fetched using the API key.
2. `GET /api/weather/api-key` - Returns the API key (for demonstration purposes only).

## Best Practices

1. Never commit your `.env` file to version control.
2. Keep your API keys and other sensitive information in the `.env` file.
3. In a production environment, consider using a more secure method for managing secrets, such as environment variables set at the system level or a secrets management service.