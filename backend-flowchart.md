# Backend System Flowchart

This document provides a visual representation of the backend architecture for the Weather Information Network (WIN) application.

## System Architecture

```mermaid
flowchart TD
    Client[Client] -->|HTTP Request| Controller

    subgraph Backend["Backend System"]
        Controller[WeatherController] -->|getOrFetchWeatherForecast| Service
        Controller -->|getWeatherForecast| Service

        Service[WeatherService] -->|findByCityAndForecastDateBetween| Repository
        Service -->|saveAll| Repository
        Service -->|deleteByForecastDateBefore| Repository

        Repository[WeatherRepository] -->|CRUD Operations| Database[(Database)]

        Service -->|API Request| OpenWeatherMap[OpenWeatherMap API]
        OpenWeatherMap -->|JSON Response| Service
    end

    %% Scheduled Tasks
    Scheduler[Scheduler] -->|Monthly Cleanup| Service
```

## Data Flow

```mermaid
sequenceDiagram
    participant Client
    participant Controller as WeatherController
    participant Service as WeatherService
    participant Repository as WeatherRepository
    participant Database
    participant API as OpenWeatherMap API

    %% Get Weather for City
    Client->>Controller: GET /api/weather/{city}
    Controller->>Service: getOrFetchWeatherForecast(city, lang)
    Service->>Repository: findByCityAndForecastDateBetween(city, now, fiveDaysLater)
    Repository->>Database: Query
    Database-->>Repository: Weather Data
    Repository-->>Service: Weather Data

    alt No data found or incomplete data
        Service->>API: HTTP Request
        API-->>Service: OpenWeatherMapResponse
        Service->>Service: Transform to WeatherData
        Service->>Repository: saveAll(weatherDataList)
        Repository->>Database: Save
        Database-->>Repository: Saved Data
        Repository-->>Service: Saved Weather Data
    end

    Service-->>Controller: Weather Data
    Controller-->>Client: JSON Response

    %% Get Weather for City in Date Range
    Client->>Controller: GET /api/weather/{city}/range?startDate=X&endDate=Y
    Controller->>Service: getWeatherForecast(city, startDateTime, endDateTime)
    Service->>Repository: findByCityAndForecastDateBetween(city, startDate, endDate)
    Repository->>Database: Query
    Database-->>Repository: Weather Data
    Repository-->>Service: Weather Data
    Service-->>Controller: Weather Data
    Controller-->>Client: JSON Response

    %% Scheduled Cleanup
    Scheduler->>Service: scheduledCleanup()
    Service->>Repository: deleteByForecastDateBefore(startOfPreviousMonth)
    Repository->>Database: Delete Query
    Database-->>Repository: Deleted Count
    Repository-->>Service: Deleted Count
```

## Component Details

### Controller Layer
- **WeatherController**: Handles HTTP requests and routes them to the appropriate service methods.
    - `GET /api/weather/{city}`: Gets weather forecast for a city
    - `GET /api/weather/{city}/range`: Gets weather forecast for a city within a date range

### Service Layer
- **WeatherService**: Contains business logic for fetching, storing, and retrieving weather data.
    - `getOrFetchWeatherForecast`: Gets weather data from DB or fetches from API if needed
    - `fetchAndSaveWeatherForecast`: Fetches data from OpenWeatherMap API and saves to DB
    - `getWeatherForecast`: Retrieves weather data from DB
    - `deleteOldWeatherData`: Cleans up outdated weather data
    - `scheduledCleanup`: Scheduled task for monthly cleanup, keeping data from the current month and the previous month

### Repository Layer
- **WeatherRepository**: JPA repository for database operations on WeatherData entities.
    - `findByCityAndForecastDateBetween`: Finds weather data by city and date range
    - `deleteByForecastDateBefore`: Deletes old weather data

### Model Layer
- **WeatherData**: Entity model for storing weather information in the database
- **OpenWeatherMapResponse**: Data model for parsing the JSON response from OpenWeatherMap API

### External Services
- **OpenWeatherMap API**: External service providing weather forecast data