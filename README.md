# Docker Compose Setup for Win Application

This README provides instructions on how to build and run the Win application using Docker Compose.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) installed on your machine
- [Docker Compose](https://docs.docker.com/compose/install/) installed on your machine
- [OpenWeatherMap API Key](#openweathermap-api-key) (required for weather data)

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

## Important: PostgreSQL Port Conflict Warning

Before running the application, ensure that you don't have any other PostgreSQL instances running on your computer. The application uses PostgreSQL on port 5432, and having another instance running on the same port will cause conflicts and prevent the application from starting properly.

To check for running PostgreSQL instances:
- On Linux/Mac: `ps aux | grep postgres`
- On Windows: Check Task Manager or run `netstat -ano | findstr 5432`

If you have another PostgreSQL instance running, you have two options:
1. Stop the existing PostgreSQL service before running this application
2. Modify the `docker-compose.yml` file to use a different port for the PostgreSQL service

## Building and Running the Application

1. Navigate to the project root directory:
   ```bash
   cd /path/to/win
   ```

2. Build and start all services using Docker Compose:
   ```bash
   docker-compose up --build
   ```

   This command will:
   - Build the application image using the Dockerfile
   - Pull the PostgreSQL and Redis images if not already available
   - Start all three services (app, db, redis)
   - Connect the application to the database and Redis services

3. To run the services in the background (detached mode):
   ```bash
   docker-compose up --build -d
   ```

4. Once all services are running, the application will be available at:
   ```
   http://localhost:8080
   ```

## Stopping the Application

1. If running in the foreground, press `Ctrl+C` to stop all services.

2. If running in detached mode, use:
   ```bash
   docker-compose down
   ```

3. To stop and remove all containers, networks, and volumes:
   ```bash
   docker-compose down -v
   ```

## Service Details

- **Application**: Spring Boot application running on port 8080
- **PostgreSQL**: Database running on port 5432
  - Database: win_db
  - Username: win_user
  - Password: win_pass
- **Redis**: Cache running on port 6379

## Viewing Logs

To view logs for a specific service:
```bash
docker-compose logs app    # Application logs
docker-compose logs db     # PostgreSQL logs
docker-compose logs redis  # Redis logs
```

Add the `-f` flag to follow the logs:
```bash
docker-compose logs -f app
```
