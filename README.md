# Docker Compose Setup for Win Application

This README provides instructions on how to build and run the Win application using Docker Compose.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) installed on your machine
- [Docker Compose](https://docs.docker.com/compose/install/) installed on your machine

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