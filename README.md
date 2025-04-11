# Docker Setup for Win Application

This README provides instructions on how to build and run the Win application using Docker.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) installed on your machine
- [OpenWeatherMap API Key](#openweathermap-api-key) (required for weather data)
- PostgreSQL database (can be run separately or in another container)

## OpenWeatherMap API Key

This application uses the OpenWeatherMap API to fetch weather data. Follow these steps to get your API key:

1. Register for a free account at [OpenWeatherMap](https://home.openweathermap.org/users/sign_up)
2. After registration and login, go to your [API keys](https://home.openweathermap.org/api_keys) page
3. Generate a new API key or use the default one provided
4. Create a `.env` file in the root directory of the project with the following content:
   ```
   OPENWEATHERMAP_API_KEY=your_api_key_here
   ```
5. Replace `your_api_key_here` with your actual OpenWeatherMap API key

**Note:** The `.env` file is already added to `.gitignore` to ensure your API key is not committed to the repository.

## Important: Database Configuration

The application requires a PostgreSQL database to function properly. The Dockerfile is configured to connect to a PostgreSQL database with the following default settings:

- Host: db
- Port: 5432
- Database: win_db
- Username: win_user
- Password: win_pass

You can modify these settings by providing environment variables when running the Docker container.

## Building and Running the Application

1. Navigate to the project root directory:
   ```bash
   cd /path/to/win
   ```

2. Build the Docker image:
   ```bash
   docker build -t win-app .
   ```

3. Run the application container:
   ```bash
   docker run -p 8080:8080 \
     -e SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/win_db \
     -e SPRING_DATASOURCE_USERNAME=win_user \
     -e SPRING_DATASOURCE_PASSWORD=win_pass \
     -e OPENWEATHERMAP_API_KEY=your_api_key_here \
     win-app
   ```

   Replace:
   - `your-db-host` with the hostname or IP address of your PostgreSQL database
   - `your_api_key_here` with your actual OpenWeatherMap API key

4. To run the container in the background (detached mode):
   ```bash
   docker run -d -p 8080:8080 \
     -e SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/win_db \
     -e SPRING_DATASOURCE_USERNAME=win_user \
     -e SPRING_DATASOURCE_PASSWORD=win_pass \
     -e OPENWEATHERMAP_API_KEY=your_api_key_here \
     win-app
   ```

5. Once the container is running, the application will be available at:
   ```
   http://localhost:8080
   ```

## Running with a PostgreSQL Container

If you don't have a PostgreSQL server, you can run one in a Docker container:

1. Start a PostgreSQL container:
   ```bash
   docker run -d \
     --name postgres \
     -e POSTGRES_DB=win_db \
     -e POSTGRES_USER=win_user \
     -e POSTGRES_PASSWORD=win_pass \
     -p 5432:5432 \
     postgres:17.4
   ```

2. Run the application container, connecting to the PostgreSQL container:
   ```bash
   docker run -d -p 8080:8080 \
     -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/win_db \
     -e SPRING_DATASOURCE_USERNAME=win_user \
     -e SPRING_DATASOURCE_PASSWORD=win_pass \
     -e OPENWEATHERMAP_API_KEY=your_api_key_here \
     --link postgres \
     win-app
   ```

## Stopping the Application

1. If running in the foreground, press `Ctrl+C` to stop the container.

2. If running in detached mode, use:
   ```bash
   docker stop <container_id>
   ```

   You can get the container ID with:
   ```bash
   docker ps
   ```

## Service Details

- **Application**: Spring Boot application running on port 8080
- **PostgreSQL**: Database running on port 5432
  - Database: win_db
  - Username: win_user
  - Password: win_pass

## API Endpoints

### Weather API

- `GET /api/weather/{city}` - Get weather forecast for a city
- `GET /api/weather/{city}/range?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` - Get weather forecast for a city within a date range

### Health Check

- `GET /api/health` - Check the health of the application
  - Returns 200 OK if the application is healthy
  - Returns 503 Service Unavailable if there are issues (e.g., database connection problems)
  - Response includes status of the application and its dependencies

## Viewing Logs

To view logs for the Docker container:
```bash
docker logs <container_id>
```

Add the `-f` flag to follow the logs:
```bash
docker logs -f <container_id>
```

You can get the container ID with:
```bash
docker ps
```
