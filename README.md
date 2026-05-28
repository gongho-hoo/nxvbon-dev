NxVibeOn Spring API
Main backend API for NxVibeOn.

Responsibilities:

Main REST API
User/project/permission/audit management
Java project analysis
Git/SVN SCM integration
Maven/Gradle build tool integration
FastAPI AI Worker orchestration

Run:

mvn spring-boot:run -Dspring-boot.run.profiles=local

Health:

curl -sS http://127.0.0.1:18000/api/v1/health
curl -sS http://127.0.0.1:18000/actuator/health

Swagger UI:

http://127.0.0.1:18000/swagger-ui/index.html