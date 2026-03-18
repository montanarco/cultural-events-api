# homework tasks for Spring-boot.

## Task 1 - Hello-world application

**Cost**: 20 points.

![Simple HelloWorld.png](assets/Simple%20HelloWorld.png)


## Task 2 - CRUD REST application

**Cost**: 20 points.

**Create an event**
```bash
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Festival Stereo Picnic",
    "type": "MUSIC",
    "date": "2026-03-21",
    "location": "Simon Bolivar Park",
    "address": "Street 68 number 68",
    "description": "different music genres one soul."
  }' | jq
```
<img src="assets/Create%20Event.png" alt="Alt text" width="500">

**Get the List of events**
```bash
curl http://localhost:8080/events | jq
```
<img src="assets/Get%20list%20of%20events.png" alt="Alt text" width="500">

**Get an event by Id **
```bash
curl http://localhost:8080/events/1 | jq
```
<img src="assets/Get%20events%20by%20ID.png" alt="Alt text" width="500">

**update an event**
```bash
curl -X PUT http://localhost:8080/events/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Spring Music Festival - Updated",
    "type": "MUSIC",
    "date": "2026-05-11",
    "location": "Central Park",
    "address": "the offcial address will be informed soon",
    "description": "Updated description for the festival."
  }' | jq
```
<img src="assets/Update%20event.png" alt="Alt text" width="500">

**Delete an event by Id **
```bash
curl -X DELETE http://localhost:8080/events/1 | jq
```
<img src="assets/Delete%20Event.png" alt="Alt text" width="500">

## Task 3 - CRUD application: security
Cost: 20 points.

Implement authentication and authorization mechanism: OAuth2 should be used - JWT Token should be used:
- the implementation design is avilable at: [Security Design](claude/security-design.md)
- the instructions to use the security features are available at: [Security Use Instructions](claude/security-use-instructions.md)

the auth flow follow the Oauths2 password grant type, where the user sends a POST request to the /auth/login endpoint with their username and password, and receives a JWT token in response. 
this token is then used in the Authorization header of subsequent requests to access protected resources.

![Auth Process OAuth2.png](assets/Auth%20Process%20OAuth2.png)

### Demo of security features:
1. no token restricts access to protected endpoints:
```bash
curl -X GET http://localhost:8080/events
```
<img src="assets/request%20without%20token.png" alt="Alt text" width="750">

2. get a token for the simple user:
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "user123"}'
```
<img src="assets/USER%20token%20generation.png" alt="Alt text" width="750">

3. use the token to access protected endpoints:
```bash
curl -X GET http://localhost:8080/events \
  -H "Authorization: Bearer <access_token>"
```
<img src="assets/User%20authorized%20Function.png" alt="Alt text" width="750">

4. accessing endopints where simple user is not authorized:
```bash
curl -X PUT http://localhost:8080/events/4 \
  -H "Content-Type: application/json" \   
  -d {
        "name": "Summer Camping Retreat - update",
        "type": "CAMPING",
        "date": "2026-08-05T05:00:00.000Z",
        "location": "Lakeview Campgrounds",
        "address": "999 Lakeview Dr, Springfield",
        "description": "A weekend camping retreat with activities for all ages. updated"
    }
```         
<img src="assets/Update%20events%20is%20not%20autorized%20to%20users.png" alt="Alt text" width="750">

6. get a token for the admin user:
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```
<img src="assets/ADMIN%20token%20generation.png" alt="Alt text" width="750">

7. use the token to access protected endpoints:
```bash
curl -X PUT http://localhost:8080/events/4 \
  -H "Content-Type: application/json" \   
  -d {
        "name": "Summer Camping Retreat - update",
        "type": "CAMPING",
        "date": "2026-08-05T05:00:00.000Z",
        "location": "Lakeview Campgrounds",
        "address": "999 Lakeview Dr, Springfield",
        "description": "A weekend camping retreat with activities for all ages. updated"
    }
```
<img src="assets/Event%20Updated%20by%20the%20admin.png" alt="Alt text" width="750">


## Task 4 (Optional) - CRUD application: externalized configuration

- Should support different environments - local, dev, stg, prod
- Spring profiles
- Each environment - different db properties

I do not have access to resources as databases in other environments, however still I make a sample of how to configure app for different environments using Spring profiles, and how to externalize the configuration properties for each environment.
that configuration is available at: 'src/main/resources'
the way to use it is when excecuting the application, you can specify the active profile using the `--spring.profiles.active` argument. for example:
```bash
java -jar target/java-learning-epam-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

this will activate the `dev` profile and the application will use the properties defined in `application-dev.properties` file. if you do not specify any profile, the application will use the default properties defined in `application.properties` file this is the one used for local env. 
You can create different properties files for each environment and specify them using the `--spring.profiles.active` argument when running the application. or via VM options in your IDE:

```bash
--spring.profiles.active=dev
```

sensitive information such as data base conection, private services url, users and password could be stored in services like Vault, AWS Secrets Manager, or Azure Key Vault, and then you can use Spring Cloud Vault, Spring Cloud AWS, or Spring Cloud Azure to integrate  at runtime. that is out of scope for this exercise.

## Task 5 - CRUD application: data migrating
Cost: 20 points.

- Add tool for migrating data: Flyway or Liquibase

we added a dependency for FLyway on the pom.xml
```xml
<dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-flyway</artifactId>
        </dependency>
```

then we create a configuration on the application.properties file to specify the datasource properties and the location of the migration scripts:
```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
# Let Flyway own the schema Hibernate must not recreate or wipe tables
spring.jpa.hibernate.ddl-auto=none
```

finally we create a migration script in the `src/main/resources/db/migration` directory: 
- V1__init.sql, creates the table definition.
- V2__add_sample_data, inserts some sample data into the table.

now if you run the application, you will see the loaded data without need to configure or insert manually.


## Task 6 (Optional) - CRUD application: actuator

- Enable actuator
- Implement a few custom health indicators
- Implement a few  custom metrics using Prometheus


GET /actuator/health       → app + DB + custom health indicator
GET /actuator/info         → app name, version, description
GET /actuator/metrics      → list all metric names
GET /actuator/prometheus   → full Prometheus scrape output
GET /actuator/env          → environment properties

an instance of prometheus was created using docker 

```bash
docker run \
  -p 9090:9090 \
  -v "/Users/miguelmontanez/EPAM/Java Global Learning Journey/Spring Boot/java-learning-epam/prometheus.yml:/etc/prometheus/prometheus.yml" \
  prom/prometheus
```

it runs the configuration file `prometheus.yml` that is located in the project root, and it scrapes the metrics from the application every 15 seconds. the configuration file is as follows:

when the container is started it connects to the actuator metrics and is show up on localhost:9090/targets with status UP, and then you can see the metrics on localhost:9090/metrics or localhost:8080/actuator/metrics.

![Prometheus target UP.png](assets/Prometheus%20target%20UP.png)

Note: a detailed report of the diferent insight that we obtained using the actuator is available at: [claude/prometheus-metrics.md](claude/prometheus-metrics.md) and a demo of how to use the actuator endpoints is available at: [claude/prometheus-monitoring.md](claude/prometheus-monitoring.md)