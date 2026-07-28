# Rating Service - El Almacén de Películas Online

## Documentacion corta de vertical

### Proposito

La vertical Rating registra calificaciones y comentarios de usuarios sobre peliculas. Calcula promedios por pelicula y publica el agregado para que Catalogo actualice la informacion visible en la ficha de cada pelicula.

### Servicios HTTP que expone

| Metodo | Endpoint interno | Proposito |
| --- | --- | --- |
| POST | `/api/ratings` | Crear rating para una pelicula. Requiere JWT salvo escenarios de test. |
| GET | `/api/ratings/pelicula/{peliculaId}` | Listar ratings de una pelicula. |
| GET | `/api/ratings/pelicula/{peliculaId}/promedio` | Obtener promedio de rating de una pelicula. |
| GET | `/api/ratings/usuario/{usuarioId}` | Listar ratings de un usuario. |
| GET | `/api/ratings/usuarios?ids={idsCsv}` | Resolver usernames desde Keycloak para una lista de ids. |
| DELETE | `/api/ratings/{id}` | Placeholder de borrado; actualmente responde `204` sin eliminar. |

Via API Gateway se consume como `/api/ratings/**`.

### Eventos que publica

| Exchange | Routing key / tipo | Proposito |
| --- | --- | --- |
| `exchange_videocloud00` | `RatingActualizadoEvent.CREATE` | Notificar a Catalogo el nuevo promedio y total de ratings de una pelicula. |

### Eventos que consume

No consume eventos actualmente. La clase `MessageListener` esta como placeholder para futuras integraciones.

## Descripción General

**Rating Service** es un microservicio especializado en la gestión de ratings, puntajes y comentarios de películas en la plataforma "El Almacén de Películas Online". Forma parte de una arquitectura de microservicios escalable y es responsable de:

- Crear y almacenar ratings de películas por usuarios
- Gestionar comentarios asociados a ratings
- Calcular promedios y estadísticas agregadas
- Validar autenticación y autorización con OAuth2/Keycloak
- Publicar eventos de cambios en ratings a través de RabbitMQ
- Integración asincrónica con otros microservicios (Catálogo, etc.)

## Características Técnicas

- **Lenguaje**: Java 21
- **Framework**: Spring Boot 3.2.12
- **Persistencia**: MySQL 8.0 + Hibernate/JPA
- **Mensajería**: RabbitMQ (Publicación de eventos)
- **Seguridad**: OAuth2 Resource Server (Keycloak)
- **Build**: Maven 3.6+
- **Testing**: JUnit 5.13 (Tests unitarios sin mocks)
- **Documentación de Arquitectura**: Structurizr DSL (Modelo C4)

## Estructura del Proyecto

```
el-almacen-de-peliculas-online-rating/
├── .structurizr/
│   └── workspace.dsl                     # Modelo C4 de arquitectura
├── src/
│   ├── main/java/unrn/rating/
│   │   ├── api/                          # Capa REST (Controllers, DTOs, Mappers)
│   │   ├── app/                          # Aplicación (Bootstrap)
│   │   ├── config/                       # Configuración (Security, RabbitMQ)
│   │   ├── dto/                          # Data Transfer Objects
│   │   ├── messaging/                    # Eventos y Publicadores
│   │   ├── model/                        # Modelo de Dominio (Rating, Puntaje, etc.)
│   │   ├── repository/                   # Acceso a Datos (Spring Data JPA)
│   │   └── service/                      # Lógica de Negocio (RatingService)
│   ├── resources/
│   │   ├── application.yml               # Configuración general
│   │   └── application-docker.properties # Configuración Docker
│   └── test/java/unrn/rating/            # Tests Unitarios e Integración
├── docker-compose.yml                    # Orquestación de dependencias
├── Dockerfile                            # Imagen Docker del servicio
├── pom.xml                               # Dependencias Maven
└── README.md                             # Este archivo

```

## Arquitectura C4

El proyecto incluye un modelo completo de arquitectura C4 (Context, Containers, Components, Code) documentado en **Structurizr DSL**. Para visualizarlo:

### Opción 1: Structurizr Lite (Recomendado)

1. **Instalar Structurizr Lite**:
   ```bash
   docker run -it --rm -p 8080:8080 \
     -v $(pwd)/.structurizr:/root/.structurizr \
     structurizr/lite:latest
   ```

2. **Acceder a la interfaz**:
   - Abre tu navegador en `http://localhost:8080`
   - El archivo `.structurizr/workspace.dsl` se cargará automáticamente

3. **Explorar las vistas**:
   - **Nivel 1 - Contexto**: Visión general del sistema y actores
   - **Nivel 2 - Contenedores**: Aplicación, BD, Message Broker
   - **Nivel 3 - Componentes**: Detalles de capas internas
   - **Flujos Dinámicos**: Secuencias de negocio

### Opción 2: Editor en línea

- Copia el contenido de `.structurizr/workspace.dsl`
- Pégalo en [Structurizr Editor](https://www.structurizr.com/dsl)
- Visualiza en tiempo real

## Flujos de Negocio Principales

### 1️ Crear Rating
```
Usuario → Frontend → API Gateway → RatingController → RatingService 
→ Rating (Dominio) → RatingRepository → MySQL
→ MessagePublisher → RabbitMQ → Catálogo Service
```

**Validaciones**:
- JWT válido de Keycloak
- Usuario no ha calificado la película antes
- Puntaje entre 1-10
- Comentario no vacío (si se proporciona)

**Respuesta**: HTTP 201 CREATED con datos del rating

### 2️ Consultar Ratings de Película
```
Usuario → Frontend → API Gateway → RatingController 
→ RatingService → RatingRepository → MySQL
→ RatingMapper → Frontend
```

**Características**:
- Sin autenticación requerida
- Retorna lista de ratings con usernames enriquecidos desde Keycloak
- Incluye estadísticas básicas (promedio, total)

## Configuración

### Variables de Entorno

```yaml
# Keycloak
spring.security.oauth2.resourceserver.jwt.issuer-uri: https://keycloak-server/realms/videocloud

# RabbitMQ
spring.rabbitmq.host: rabbitmq
spring.rabbitmq.port: 5672
spring.rabbitmq.username: guest
spring.rabbitmq.password: guest
rating.rabbitmq.exchange: exchange_videocloud00

# MySQL
spring.datasource.url: jdbc:mysql://mysql:3306/rating_db
spring.datasource.username: rating_user
spring.datasource.password: password
spring.jpa.hibernate.ddl-auto: update
```

### Levantar el proyecto con Docker Compose

```bash
docker-compose up -d
```

Servicios:
- **Rating Service**: http://localhost:8080
- **MySQL**: localhost:3306
- **RabbitMQ**: localhost:5672

## Modelo de Dominio

El proyecto sigue **principios DDD (Domain-Driven Design)**:

### Entidades
- **Rating**: Representa una calificación completa (película, usuario, puntaje, comentario, timestamp)
  - Validaciones en constructor
  - Immutable excepto por campos de auditoría
  - Eventos de dominio (RatingActualizadoEvent)

### Value Objects
- **Puntaje**: Encapsula validaciones de rango (1-10)
- **Comentario**: Texto con validaciones de no vacío
- **PeliculaId**: Identificador tipado de película
- **UsuarioId**: Identificador tipado de usuario

### Invariantes de Negocio
- Un usuario solo puede calificar una película una vez
- El puntaje debe estar entre 1 y 10
- El comentario no puede estar vacío si se proporciona
- Los ratings son inmutables (no pueden editarse, solo crearse)

## Testing

### Tests Unitarios

```bash
mvn test
```

Características:
- No usamos Mocks, Stubs ni Fakes (ejecución real en memoria)
- Estructura clara: Setup → Ejercitación → Verificación
- Nombres descriptivos: `cuestionATestear_resultadoEsperado`
- Cobertura de casos límite (nulos, vacíos, inválidos)
- Verificación de excepciones esperadas

Ejemplo:
```java
@Test
@DisplayName("Crear rating rechaza si usuario ya calificó película")
void crearRating_rechazaSiDuplicado() {
    // Setup
    var usuario = "user-123";
    var pelicula = 1L;
    var rating1 = new Rating(pelicula, usuario, 8, "Excelente");
    service.createRating(rating1);
    
    // Ejercitación
    var rating2 = new Rating(pelicula, usuario, 5, "No me gustó");
    var ex = assertThrows(RuntimeException.class, () -> {
        service.createRating(rating2);
    });
    
    // Verificación
    assertEquals(DuplicateRatingException.ERROR_MESSAGE, ex.getMessage());
}
```

## API REST

### Endpoints

#### 1. Crear Rating
```
POST /api/ratings
Authorization: Bearer {JWT}

Body:
{
  "peliculaId": 1,
  "valor": 8,
  "comentario": "Excelente película"
}

Response (201):
{
  "id": 123,
  "peliculaId": 1,
  "usuarioId": "user-123",
  "usuarioUsername": "juan",
  "puntaje": 8,
  "comentario": "Excelente película",
  "fechaCreacion": "2025-02-27T10:30:00"
}
```

#### 2. Consultar Ratings de Película
```
GET /api/ratings/pelicula/{peliculaId}

Response (200):
[
  {
    "id": 123,
    "peliculaId": 1,
    "usuarioId": "user-123",
    "usuarioUsername": "juan",
    "puntaje": 8,
    "comentario": "Excelente película",
    "fechaCreacion": "2025-02-27T10:30:00"
  }
]
```

## Seguridad

### Autenticación (OAuth2/Keycloak)

El servicio valida **JWT** emitidos por Keycloak:
- Verifica firma del token contra clave pública
- Valida tiempo de expiración
- Extrae información del usuario (sub, preferred_username, roles)

### Autorización

Basada en **roles de Keycloak**:
- `ROLE_USER`: Usuario estándar (puede crear ratings)
- `ROLE_ADMIN`: Administrador (acceso completo)

### Configuración

```java
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {
    // Configura filtros JWT y control de acceso
}
```

## Integración con RabbitMQ

### Eventos Publicados

Cada vez que se crea un rating, se publica un evento:

```
Evento: RatingActualizadoEvent
Exchange: exchange_videocloud00 (Topic)
Routing Key: RatingActualizadoEvent.CREATE
Payload:
{
  "peliculaId": 1,
  "promedio": 7,
  "totalRatings": 42
}
```

### Consumidores

- **Catálogo Service**: Usa el evento para actualizar estadísticas de película

### Configuración

```yaml
spring.rabbitmq.host: rabbitmq
spring.rabbitmq.port: 5672
rating.rabbitmq.exchange: exchange_videocloud00
```

## Desarrollo

### Build Local

```bash
mvn clean package
```

### Ejecutar sin Docker

```bash
mvn spring-boot:run
```

### Hot Reload en IDE

JetBrains IntelliJ IDEA:
1. Enable "Reload classes on save" en Settings → Advanced Settings
2. Los cambios en el código se reflejan instantáneamente

## Troubleshooting

### Error: "Invalid JWT"
- Verifica que Keycloak esté corriendo
- Revisa que el token no esté expirado
- Confirma el `realm` y `client-id` en `application.yml`

### Error: "Connection refused to RabbitMQ"
- Ejecuta `docker-compose up -d rabbitmq`
- Verifica puerto 5672 esté abierto

### Error: "Database connection failed"
- Ejecuta `docker-compose up -d mysql`
- Verifica credenciales en `application.yml`

## Contribución

Las contribuciones deben seguir:
1. Código limpio (Sin getters/setters innecesarios)
2. Tests unitarios para cada cambio
3. Documentación actualizada
4. Commits descriptivos

---
