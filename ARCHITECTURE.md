# Guía de Arquitectura - Rating Service

## Índice
1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Arquitectura C4](#arquitectura-c4)
3. [Componentes Clave](#componentes-clave)
4. [Patrones de Diseño](#patrones-de-diseño)
5. [Flujos de Datos](#flujos-de-datos)
6. [Decisiones Arquitectónicas](#decisiones-arquitectónicas)

---

## Resumen Ejecutivo

**Rating Service** es un microservicio de dominio específico (DDD) que gestiona la calificación y comentarios de películas. Implementa:

- ✅ **Autenticación**: OAuth2 con Keycloak
- ✅ **Persistencia**: MySQL con ORM Hibernate/JPA
- ✅ **Mensajería**: RabbitMQ para eventos asincronos
- ✅ **Modelo de Dominio Limpio**: Value Objects y Entidades con invariantes
- ✅ **Testing**: JUnit 5 sin Mocks (ejecución real)

---

## Arquitectura C4

### Nivel 1: Contexto del Sistema

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  ┌──────────┐      ┌──────────────────┐    ┌────────────┐ │
│  │  Usuario │─────▶│  Frontend React  │───▶│ API Gateway│ │
│  └──────────┘      │     Vite SPA     │    └────────────┘ │
│       │            └──────────────────┘          │        │
│       │                                           │        │
│       └─────────────────────────────────────────▶│        │
│                                                  ▼        │
│                                          ┌──────────────┐ │
│                                          │   Rating     │ │
│                                          │   Service    │ │
│                                          └──────────────┘ │
│                                                  │        │
│                        ┌─────────────────────────┴────────┘
│                        │
│        ┌───────────────┴──────────────┬──────────────┐
│        │                              │              │
│        ▼                              ▼              ▼
│   ┌─────────┐                  ┌──────────┐  ┌─────────────┐
│   │Keycloak │                  │RabbitMQ  │  │  Catálogo   │
│   │ OAuth2  │                  │ Eventos  │  │   Service   │
│   └─────────┘                  └──────────┘  └─────────────┘
│        │                              │
│        └──────────────┬───────────────┘
│                       ▼
│                   ┌─────────┐
│                   │ MySQL   │
│                   │ ratings │
│                   └─────────┘
```

### Nivel 2: Contenedores

```
┌───────────────────────────────────────────────────────┐
│  Rating Service Microservicio                         │
├───────────────────────────────────────────────────────┤
│                                                       │
│ ┌──────────────────────────────────────────────────┐ │
│ │  API REST (Spring Boot)                          │ │
│ │  - RatingController                              │ │
│ │  - RatingRequestDto / RatingResponseDto          │ │
│ │  - RatingMapper                                  │ │
│ └──────────────────────────────────────────────────┘ │
│            │                    │                    │
│            ▼                    ▼                    │
│ ┌──────────────────┐  ┌──────────────────────────┐ │
│ │ Rating Service   │  │ Security Config          │ │
│ │ (Lógica negocio) │  │ - JWT Validation         │ │
│ │ - Crear rating   │  │ - Keycloak Integration   │ │
│ │ - Consultar      │  │ - Role-based Access      │ │
│ └──────────────────┘  └──────────────────────────┘ │
│            │                                        │
│            ▼                                        │
│ ┌──────────────────┐                               │
│ │ Rating Repository│                               │
│ │ (Spring Data JPA)│                               │
│ └──────────────────┘                               │
│            │                                        │
│            ├─────────┬──────────┬─────────────┐    │
│            ▼         ▼          ▼             ▼    │
│  ┌─────────────────────┐  ┌─────────────────────┐ │
│  │  MySQL Database     │  │ RabbitMQ Publisher  │ │
│  │  (Persistencia)     │  │ (Message Broker)    │ │
│  └─────────────────────┘  └─────────────────────┘ │
│                                                    │
└───────────────────────────────────────────────────┘
```

### Nivel 3: Componentes (API REST)

```
RatingController
    ├─▶ RatingRequestDto
    │   └─ peliculaId
    │   └─ valor
    │   └─ comentario
    │
    ├─▶ RatingMapper
    │   ├─ toModel() → Rating (Dominio)
    │   └─ toDto() → RatingResponseDto
    │
    └─▶ RatingResponseDto
        └─ id, peliculaId, usuarioId, puntaje, comentario, etc.
```

### Nivel 3: Componentes (Dominio)

```
Rating (Entidad Principal)
    ├─ id: Long (ID en BD)
    ├─ peliculaId: Long (Referencia)
    ├─ usuarioId: String (Keycloak UUID)
    ├─ puntaje: Puntaje (Value Object)
    │   └─ valor: int (1-10)
    ├─ comentario: String (opcional)
    ├─ usuarioUsername: String (enriquecido desde Keycloak)
    └─ fechaCreacion: LocalDateTime

Invariantes (Reglas de Negocio):
    • Usuario no puede calificar misma película dos veces
    • Puntaje debe estar entre 1 y 10
    • Un rating es inmutable (no editable)
    • Rating requiere película y usuario
```

### Nivel 3: Componentes (Persistencia)

```
RatingRepository (Spring Data JPA)
    ├─ save(Rating) → Rating
    ├─ findById(Long) → Optional<Rating>
    ├─ findAll() → List<Rating>
    ├─ existsByPeliculaIdAndUsuarioId(Long, String) → boolean
    ├─ findByPeliculaId(Long) → List<Rating>
    ├─ countByPeliculaId(Long) → long
    └─ delete(Rating) → void

Tabla SQL:
    rating
    ├─ id (BIGINT PRIMARY KEY AUTO_INCREMENT)
    ├─ pelicula_id (BIGINT NOT NULL)
    ├─ usuario_id (VARCHAR(255) NOT NULL)
    ├─ puntaje (INT CHECK puntaje BETWEEN 1 AND 10)
    ├─ comentario (TEXT nullable)
    ├─ usuario_username (VARCHAR(255) nullable)
    ├─ fecha_creacion (TIMESTAMP NOT NULL)
    └─ UNIQUE(pelicula_id, usuario_id)
```

### Nivel 3: Componentes (Seguridad)

```
SecurityConfiguration
    ├─ httpSecurity
    │   ├─ CSRF deshabilitado (stateless)
    │   ├─ Session Policy: STATELESS
    │   └─ OAuth2 Resource Server con JWT
    │
    └─ JwtAuthenticationConverter
        └─ Mapea claims de JWT a authorities de Spring Security

JWT Token (Keycloak):
    {
        "sub": "user-uuid",
        "preferred_username": "juan",
        "roles": ["user", "admin"],
        "iat": 1708952400,
        "exp": 1708956000
    }
```

### Nivel 3: Componentes (Mensajería)

```
RabbitMQConfig
    └─ TopicExchange: "exchange_videocloud00"

MessagePublisher
    └─ publish(Event<String, RatingActualizadoEvent>)
        └─ Routing Key: "RatingActualizadoEvent.CREATE"

RatingActualizadoEvent (Evento de Dominio)
    ├─ peliculaId: Long
    ├─ promedio: int
    └─ totalRatings: long

Consumidor (Catálogo Service):
    └─ Recibe evento → Actualiza estadísticas de película
```

---

## Componentes Clave

### 1. RatingController (Capa REST)
**Responsabilidad**: Exponer endpoints HTTP protegidos por JWT

```java
@RestController
@RequestMapping("/api/ratings")
public class RatingController {
    @PostMapping
    public ResponseEntity<RatingResponseDto> crear(
        @RequestBody RatingRequestDto req,
        @AuthenticationPrincipal Jwt jwt
    ) { ... }
    
    @GetMapping("/pelicula/{peliculaId}")
    public ResponseEntity<List<RatingResponseDto>> porPelicula(
        @PathVariable Long peliculaId
    ) { ... }
}
```

**Características**:
- Valida JWT automáticamente mediante Spring Security
- Extrae `usuarioId` del claim `sub` del JWT
- Extrae `usuarioUsername` del claim `preferred_username`
- Maneja excepciones (`DuplicateRatingException` → 409 CONFLICT)

---

### 2. RatingService (Lógica de Negocio)
**Responsabilidad**: Orquestar creación, consulta y eventos

```java
@Service
public class RatingService {
    @Transactional
    public Rating createRating(Rating rating) {
        // 1. Validar duplicados
        // 2. Guardar en BD
        // 3. Calcular estadísticas
        // 4. Publicar evento
    }
    
    public List<Rating> ratingsPorPelicula(Long peliculaId) { ... }
    public double promedioPorPelicula(Long peliculaId) { ... }
}
```

**Características**:
- Transaccional (todo o nada)
- Calcula promedio automáticamente
- Publica eventos de cambios
- Valida reglas de negocio

---

### 3. Rating (Entidad de Dominio)
**Responsabilidad**: Encapsular estado y validaciones

```java
@Entity
@Table(name = "rating", uniqueConstraints = 
    @UniqueConstraint(columnNames = {"pelicula_id", "usuario_id"})
)
public class Rating {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long peliculaId;
    
    @Column(nullable = false)
    private String usuarioId;
    
    @Embedded
    private Puntaje puntaje;
    
    // Constructor con validaciones
    public Rating(Long peliculaId, String usuarioId, 
                  int valor, String comentario) {
        assertPeliculaObligatoria(peliculaId);
        assertUsuarioObligatorio(usuarioId);
        // ...
    }
    
    private void assertPeliculaObligatoria(Long peliculaId) { ... }
}
```

**Características**:
- Validaciones en constructor (Fast-Fail)
- Value Objects embebidos (Puntaje, Comentario)
- Inmutable (no setters)
- Métodos de negocio (tell don't ask)

---

### 4. Puntaje (Value Object)
**Responsabilidad**: Encapsular lógica de validación de puntaje

```java
@Embeddable
public class Puntaje {
    @Column(name = "puntaje", nullable = false)
    private int valor;
    
    static final String ERROR_PUNTAJE_INVALIDO = 
        "El puntaje debe estar entre 1 y 10";
    
    public Puntaje(int valor) {
        assertPuntajeValido(valor);
        this.valor = valor;
    }
    
    private void assertPuntajeValido(int valor) {
        if (valor < 1 || valor > 10) {
            throw new RuntimeException(ERROR_PUNTAJE_INVALIDO);
        }
    }
}
```

---

### 5. RatingRepository (Acceso a Datos)
**Responsabilidad**: Abstracción de persistencia

```java
public interface RatingRepository extends JpaRepository<Rating, Long> {
    boolean existsByPeliculaIdAndUsuarioId(Long peliculaId, String usuarioId);
    List<Rating> findByPeliculaId(Long peliculaId);
    long countByPeliculaId(Long peliculaId);
    // Queries personalizadas generadas por Spring Data JPA
}
```

---

## Patrones de Diseño

### 1. Value Object (Puntaje, PeliculaId, UsuarioId)
Objetos inmutables que encapsulan lógica de dominio específica.

```java
public class Puntaje {
    private final int valor; // immutable
    
    public Puntaje(int valor) {
        // validaciones en constructor
    }
}
```

**Beneficios**:
- ✅ Validaciones centralizadas
- ✅ Tipos tipados (no primitivos)
- ✅ Semántica clara
- ✅ Fácil de testear

---

### 2. Entity (Rating)
Objeto con identidad única que persiste en el tiempo.

```java
@Entity
@Table(name = "rating")
public class Rating {
    @Id
    private Long id; // Identidad
    
    // Atributos
    private Long peliculaId;
    private String usuarioId;
    
    // Validaciones en constructor
    public Rating(Long peliculaId, String usuarioId, ...) {
        // ...
    }
}
```

**Beneficios**:
- ✅ Identidad única
- ✅ Persistencia
- ✅ Ciclo de vida
- ✅ Relaciones con otros agregados

---

### 3. Aggregate Root (Rating)
Rating es la raíz del agregado. Controla acceso a Value Objects.

```java
public class Rating {
    private Puntaje puntaje;        // encapsulado
    private Comentario comentario;  // encapsulado
    
    // No exposición directa, métodos de negocio
    public int obtenerPuntaje() {
        return puntaje.valor();
    }
}
```

---

### 4. Repository (RatingRepository)
Abstracción de persistencia, colección simulada.

```java
public interface RatingRepository extends JpaRepository<Rating, Long> {
    // Los métodos parecen operaciones de colección
    List<Rating> findByPeliculaId(Long peliculaId);
}

// Uso:
List<Rating> ratings = ratingRepository.findByPeliculaId(1L);
```

---

### 5. DTO (Data Transfer Object)
Objetos para comunicación entre capas REST.

```java
public class RatingRequestDto {
    public Long peliculaId;
    public int valor;
    public String comentario;
}

public class RatingResponseDto {
    public Long id;
    public Long peliculaId;
    public String usuarioId;
    public int puntaje;
    public LocalDateTime fechaCreacion;
}
```

---

### 6. Mapper
Convierte entre DTOs y Entidades de Dominio.

```java
public class RatingMapper {
    public static Rating toModel(RatingRequestDto req, 
                                 String usuarioId, 
                                 String username) {
        return new Rating(req.peliculaId, usuarioId, 
                         req.valor, req.comentario);
    }
    
    public static RatingResponseDto toDto(Rating rating) {
        var dto = new RatingResponseDto();
        dto.id = rating.id;
        // ...
        return dto;
    }
}
```

---

### 7. Service (RatingService)
Orquestación de lógica de negocio entre múltiples agregados.

```java
@Service
@Transactional
public class RatingService {
    public Rating createRating(Rating rating) {
        // 1. Validar reglas
        if (repository.existsByPeliculaIdAndUsuarioId(...)) {
            throw new DuplicateRatingException();
        }
        
        // 2. Guardar
        Rating saved = repository.save(rating);
        
        // 3. Efectos secundarios (eventos)
        publishEvent(new RatingActualizadoEvent(...));
        
        return saved;
    }
}
```

---

### 8. Event (RatingActualizadoEvent)
Evento de dominio publicado asincronamente.

```java
public class RatingActualizadoEvent {
    private Long peliculaId;
    private int promedio;
    private long totalRatings;
}

// Publicado en RabbitMQ
publisher.publish(
    new Event<>(EventType.CREATE, key, ratingActualizadoEvent)
);
```

---

## Flujos de Datos

### Flujo 1: Crear Rating

```
Usuario → [Frontend HTML Form]
    ↓
POST /api/ratings 
    ├─ Headers: Authorization: Bearer {JWT}
    └─ Body: { peliculaId: 1, valor: 8, comentario: "..." }
    ↓
[API Gateway] (valida JWT, enruta)
    ↓
[RatingController]
    ├─ Extrae usuarioId de JWT.sub
    ├─ Extrae username de JWT.preferred_username
    ├─ Mapea RatingRequestDto → Rating
    └─ Llama service.createRating(rating)
    ↓
[SecurityConfiguration] (valida JWT)
    ├─ Verifica firma de token
    ├─ Verifica expiración
    └─ Mapea claims a authorities
    ↓
[RatingService.createRating]
    ├─ Valida: ¿Usuario ya calificó película?
    │   └─ Si: Lanza DuplicateRatingException → HTTP 409
    ├─ repository.save(rating) → MySQL INSERT
    ├─ Calcula: promedioPorPelicula(peliculaId)
    ├─ Calcula: countByPeliculaId(peliculaId)
    └─ publisher.publish(RatingActualizadoEvent)
    ↓
[RabbitMQConfig]
    ├─ Exchange: exchange_videocloud00 (Topic)
    ├─ Routing Key: RatingActualizadoEvent.CREATE
    └─ Message: { peliculaId: 1, promedio: 7, totalRatings: 42 }
    ↓
[RabbitMQ]
    └─ [Catálogo Service] (consume evento, actualiza estadísticas)
    ↓
[RatingController] ← HTTP 201 Created
    ├─ RatingMapper.toDto(saved)
    └─ ResponseEntity.status(201).body(dto)
    ↓
[Frontend] ← JSON Response
    └─ Muestra confirmación al usuario
```

### Flujo 2: Consultar Ratings

```
Usuario → [Frontend - Página de Película]
    ↓
GET /api/ratings/pelicula/1
    ↓
[API Gateway] (enruta sin requerir auth)
    ↓
[RatingController.porPelicula]
    ├─ ratingService.ratingsPorPelicula(1)
    └─ Mapea Rating[] → RatingResponseDto[]
    ↓
[RatingService.ratingsPorPelicula]
    ├─ ratingRepository.findByPeliculaId(1)
    └─ MySQL SELECT * FROM rating WHERE pelicula_id = 1
    ↓
[KeycloakUserService] (opcional, enriquecimiento)
    ├─ Obtiene lista de usuarioIds únicos
    ├─ Consulta Keycloak por usernames
    └─ Mapea id → username
    ↓
[RatingController] ← HTTP 200 OK
    └─ JSON List<RatingResponseDto>
    ↓
[Frontend] ← JSON Response
    └─ Renderiza lista de ratings con usernames
```

---

## Decisiones Arquitectónicas

### 1. ¿Por qué DDD (Domain-Driven Design)?

**Decisión**: Implementar modelo de dominio rico con Value Objects y Entidades.

**Justificación**:
- ✅ Lógica de negocio centralizada y testeable
- ✅ Invariantes proteadas en constructor
- ✅ Código autodocumentado
- ✅ Facilita evolución del dominio

**Alternativa rechazada**: Modelo anémico (getters/setters).

---

### 2. ¿Por qué Value Objects para Puntaje, PeliculaId, UsuarioId?

**Decisión**: Envolver primitivos en tipos específicos.

**Justificación**:
- ✅ Validaciones centralizadas (Puntaje: 1-10)
- ✅ Semántica clara (no confundir IDs)
- ✅ Previene errores en tiempo de compilación
- ✅ Fácil agregar comportamiento sin cambiar contrato

**Código**:
```java
// ❌ Malo
public Rating(Long peliculaId, String usuarioId, int puntaje) { }

// ✅ Bueno
public Rating(PeliculaId película, UsuarioId usuario, Puntaje puntaje) { }
```

---

### 3. ¿Por qué validaciones en constructor?

**Decisión**: Lanzar excepciones en constructor, fast-fail.

**Justificación**:
- ✅ Objetos siempre válidos (no estado inconsistente)
- ✅ Validaciones únicas (no duplicadas en servicio/controller)
- ✅ Fácil testear excepciones esperadas

**Código**:
```java
public Rating(Long peliculaId, ...) {
    assertPeliculaObligatoria(peliculaId);  // Falla si inválido
    this.peliculaId = peliculaId;
}
```

---

### 4. ¿Por qué Spring Security + OAuth2?

**Decisión**: Validar JWT de Keycloak, sin sesiones.

**Justificación**:
- ✅ Stateless (escalable horizontalmente)
- ✅ Integración con Keycloak (identidad centralizada)
- ✅ Support para múltiples clientes (Frontend, Mobile, etc.)
- ✅ Seguridad estándar industria

**Configuración**:
```yaml
spring.security.oauth2.resourceserver.jwt.issuer-uri: 
  https://keycloak/realms/videocloud
```

---

### 5. ¿Por qué RabbitMQ para eventos?

**Decisión**: Publicar eventos asincronamente a través de message broker.

**Justificación**:
- ✅ Desacoplamiento (Rating no conoce Catálogo)
- ✅ Escalabilidad (múltiples consumidores)
- ✅ Durabilidad (mensajes persistidos)
- ✅ Pattern: Event Sourcing / CQRS compatible

**Evento**:
```
RatingActualizadoEvent → exchange_videocloud00 (Topic)
Routing Key: RatingActualizadoEvent.CREATE
Consumidor: Catálogo Service
```

---

### 6. ¿Por qué MySQL (no NoSQL)?

**Decisión**: Usar base de datos relacional con ACID.

**Justificación**:
- ✅ Integridad referencial (películas, usuarios)
- ✅ Transacciones ACID (consistencia)
- ✅ Queries complejas (estadísticas)
- ✅ Compatible con Spring Data JPA

**Alternativa**: NoSQL para escalabilidad masiva (no es requisito actual).

---

### 7. ¿Por qué Spring Data JPA (no SQL puro)?

**Decisión**: Usar ORM para abstracción de persistencia.

**Justificación**:
- ✅ Abstracción: cambiar BD sin cambiar código
- ✅ Queries automáticas: `findByPeliculaId(...)` generada
- ✅ Type-safe: compilación chequea tipos
- ✅ Integración: transacciones automáticas

**Ejemplo**:
```java
List<Rating> ratings = repository.findByPeliculaId(1L);
// Genera automáticamente:
// SELECT * FROM rating WHERE pelicula_id = 1
```

---

### 8. ¿Por qué Sin Mocks en Tests?

**Decisión**: Ejecutar tests reales en memoria (JPA, BD en memoria, etc.).

**Justificación**:
- ✅ Tests confables (comportamiento real)
- ✅ Código más limpio (sin boilerplate de mocks)
- ✅ Refactorización fácil (tests no frágiles)
- ✅ Tests documentan comportamiento real

**Ejemplo**:
```java
@Test
void crearRating_rechazaSiDuplicado() {
    // Setup: Rating real con entidad real
    service.createRating(new Rating(...));
    
    // Ejercitación: Service real, Repository real
    var ex = assertThrows(RuntimeException.class, () -> 
        service.createRating(new Rating(...))  // Misma película
    );
    
    // Verificación: Excepción real lanzada
    assertEquals(ERROR_MESSAGE, ex.getMessage());
}
```

---

### 9. ¿Por qué Inmutabilidad en Rating?

**Decisión**: Rating es inmutable (no editable, solo creable).

**Justificación**:
- ✅ Seguridad: usuario no puede cambiar su rating después
- ✅ Auditoría: historial completo (crear nuevos ratings)
- ✅ Concurrencia: no race conditions
- ✅ Semántica: rating es "hecho histórico"

**Implicación**: Para cambiar rating, eliminar y crear nuevo.

---

### 10. ¿Por qué Validaciones en Constructor (no setters)?

**Decisión**: Lanzar RuntimeException en constructor si inválido.

**Justificación**:
- ✅ Fail-fast: Error inmediato, no silencioso
- ✅ Objetos válidos: Nunca estado inconsistente
- ✅ No necesita setters/getters

**Patrón**:
```java
public Rating(...) {
    assertPeliculaObligatoria(peliculaId);
    assertUsuarioObligatorio(usuarioId);
    assertComentarioValido(comentario);
    
    // Si llegamos aquí, objeto es válido
    this.peliculaId = peliculaId;
    this.usuarioId = usuarioId;
    // ...
}
```

---

## Contacto y Soporte

Para preguntas sobre arquitectura, contacta al equipo de desarrollo.

---

**Última actualización**: Febrero 2025  
**Versión**: 1.0

