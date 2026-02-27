workspace "Rating - El Almacén de Películas Online" "Vertical de gestión de ratings y comentarios de películas" {

    model {
        !identifiers hierarchical

        # Actores/Usuarios del Sistema
        usuario = person "Usuario" "Espectador registrado que califica y comenta películas" "Usuario"
        admin = person "Administrador" "Gestiona contenido y políticas de ratings" "Administrador"

        # Sistema de Películas Online (Contexto Externo)
        sistemaPeliculas = softwareSystem "El Almacén de Películas Online" "Plataforma central de gestión de películas y catálogo" "SistemaExterno"

        # Sistema de Identificación y Seguridad (Keycloak)
        keycloak = softwareSystem "Keycloak" "Gestor de identidad y autorización con roles OAuth2/OIDC" "SistemaExterno"

        # API Gateway
        apiGateway = softwareSystem "API Gateway" "Punto de entrada único; valida JWT y enruta solicitudes hacia los microservicios" "SistemaExterno"

        # Frontend SPA
        frontend = softwareSystem "Frontend React Vite" "SPA responsable que consume servicios REST del vertical de Rating" "Cliente"

        # Message Broker
        rabbitmq = softwareSystem "RabbitMQ" "Broker de mensajería para comunicación asincrónica entre microservicios (Eventos)" "Infraestructura"

        # Catálogo (Consume los eventos de Rating)
        catalogoService = softwareSystem "Catálogo Service" "Vertical que consume eventos de actualización de ratings para mantener estadísticas agregadas" "SistemaExterno"

        # ========================================
        # VERTICAL DE RATING - Contenedores
        # ========================================
        ratingVertical = softwareSystem "Rating Service" "Vertical responsable de gestión de ratings, puntajes y comentarios de películas" "RatingService" {

            # Contenedor: API REST
            apiRest = container "API REST Rating" "Expone endpoints HTTP/REST protegidos por JWT para crear y consultar ratings" "Spring Boot REST Controller" "APIContainer" {
                ratingController = component "RatingController" "Expone endpoints REST: POST /api/ratings, GET /api/ratings/pelicula/{id}, etc." "Spring REST Controller" "ControllerComponent"
                ratingMapper = component "RatingMapper" "Mapea entre DTOs y entidades del dominio" "Mapper Utility" "MapperComponent"
                requestDto = component "RatingRequestDto" "DTO para solicitudes HTTP: peliculaId, valor, comentario" "Data Transfer Object" "DTOComponent"
                responseDto = component "RatingResponseDto" "DTO para respuestas HTTP: id, usuario, puntaje, comentario, fechaCreacion" "Data Transfer Object" "DTOComponent"

                ratingController -> ratingMapper "Convierte DTOs a modelos"
                ratingController -> requestDto "Deserializa solicitudes"
                ratingController -> responseDto "Serializa respuestas"
                ratingMapper -> requestDto "Lee datos de entrada"
                ratingMapper -> responseDto "Genera datos de salida"
            }

            # Contenedor: Servicio de Aplicación
            appService = container "Rating Service" "Contiene la lógica de negocio principal: validación, persistencia y eventos" "Spring Boot Service" "ServiceContainer" {
                ratingService = component "RatingService" "Orquesta la creación, actualización y consulta de ratings. Publica eventos asincronos." "Spring Service" "ServiceComponent"
                keycloakUserService = component "KeycloakUserService" "Integración con Keycloak para obtener usernames y validación de usuarios" "Service" "ExternalServiceComponent"
                duplicateRatingException = component "DuplicateRatingException" "Excepción personalizada para validar ratings únicos por usuario-película" "Custom Exception" "ExceptionComponent"

                ratingService -> keycloakUserService "Obtiene información de usuarios"
                ratingService -> duplicateRatingException "Lanza excepción en duplicados"
            }

            # Contenedor: Dominio (Modelo)
            domain = container "Modelo de Dominio" "Encapsula las reglas de negocio: Rating, Puntaje, Comentario, PeliculaId, UsuarioId" "Java POJOs + JPA" "DomainContainer" {
                rating = component "Rating (Entidad)" "Representa una calificación de película: peliculaId, usuarioId, puntaje, comentario, timestamp. Encapsula validaciones." "JPA Entity" "EntityComponent"
                puntaje = component "Puntaje (Value Object)" "Representa el valor numérico del rating (1-10). Encapsula validaciones de rango." "JPA Embeddable" "ValueObjectComponent"
                comentario = component "Comentario (Value Object)" "Texto del comentario asociado al rating. Validaciones de no vacío." "JPA Embeddable" "ValueObjectComponent"
                peliculaId = component "PeliculaId (Value Object)" "Identificador único de película. Encapsula tipo y validaciones." "Value Object" "ValueObjectComponent"
                usuarioId = component "UsuarioId (Value Object)" "Identificador único de usuario de Keycloak. Encapsula tipo y validaciones." "Value Object" "ValueObjectComponent"

                rating -> puntaje "Contiene"
                rating -> comentario "Contiene"
                rating -> peliculaId "Referencia"
                rating -> usuarioId "Referencia"
            }

            # Contenedor: Persistencia
            database = container "Base de Datos MySQL" "Almacena ratings, puntajes, comentarios y metadatos de usuarios" "MySQL 8.0" "DatabaseContainer"

            # Contenedor: Repositorio de Datos
            repository = container "Rating Repository" "Acceso a datos con Spring Data JPA; realiza queries y operaciones CRUD" "Spring Data JPA" "RepositoryContainer" {
                ratingRepository = component "RatingRepository" "Spring Data JPA Repository. Define queries personalizadas: existsByPeliculaIdAndUsuarioId, findByPeliculaId, countByPeliculaId" "Spring Data JPA" "RepositoryComponent"
            }

            # Contenedor: DTOs (Data Transfer Objects)
            dtos = container "DTOs y Mappers" "Objetos para transferencia de datos: RatingRequestDto, RatingResponseDto, RatingActualizadoEvent" "Java POJOs" "DTOContainer"

            # Contenedor: Configuración de Seguridad
            securityConfig = container "Configuración de Seguridad" "Configura filtros JWT, validación de tokens OAuth2 y autorización basada en roles" "Spring Security + OAuth2" "SecurityContainer" {
                securityConfiguration = component "SecurityConfiguration" "Configura filtros de seguridad, validación de JWT OAuth2 y control de acceso basado en roles" "Spring Security Config" "SecurityComponent"
                jwtConverter = component "KeycloakGrantedAuthoritiesConverter" "Convierte claims del JWT de Keycloak a autoridades de Spring Security" "JWT Converter" "SecurityComponent"

                securityConfiguration -> jwtConverter "Usa para procesar JWT"
                securityConfiguration -> keycloak "Valida tokens"
            }

            # Contenedor: Message Broker Integration
            messagingConfig = container "RabbitMQ Config & Publisher" "Configuración de exchanges y publicación de eventos de cambios en ratings" "Spring AMQP" "MessagingContainer" {
                rabbitConfig = component "RabbitMQConfig" "Define exchanges (topic), colas y configuración de JSON converter para serialización" "Spring AMQP Config" "MessagingComponent"
                messagePublisher = component "MessagePublisher" "Publica eventos RatingActualizadoEvent en el exchange de RabbitMQ" "Message Publisher" "MessagingComponent"
                ratingActualizadoEvent = component "RatingActualizadoEvent" "Evento de dominio: peliculaId, promedio, totalRatings. Routing key: RatingActualizadoEvent.CREATE" "Event DTO" "EventComponent"
                messageListener = component "MessageListener" "Placeholder para consumir eventos (actualmente deshabilitado)" "Message Listener" "MessagingComponent"

                rabbitConfig -> rabbitmq "Configura"
                messagePublisher -> ratingActualizadoEvent "Publica"
                messagePublisher -> rabbitmq "Envía a exchange_videocloud00"
                messageListener -> rabbitmq "Escucha (no usado actualmente)"
            }

            # Relaciones internas del Vertical
            apiRest -> appService "Delega lógica de negocio"
            apiRest -> securityConfig "Valida JWT y autorización"
            appService -> domain "Crea y manipula entidades del dominio"
            appService -> repository "Persiste y consulta datos"
            appService -> messagingConfig "Publica eventos de cambios"
            repository -> database "Lee/Escribe datos"
            domain -> dtos "Convierte a/desde DTOs"
            dtos -> apiRest "Serializa respuestas"

            # Relaciones adicionales para vistas dinámicas
            apiRest -> frontend "Retorna respuestas HTTP"
            appService -> appService "Calcula estadísticas"
        }

        # ========================================
        # RELACIONES EXTERNAS DEL VERTICAL
        # ========================================

        # Usuarios externos
        usuario -> frontend "Consulta catálogo y califica películas"
        usuario -> apiGateway "Realiza solicitudes autenticadas"
        admin -> apiGateway "Gestiona políticas de rating"

        # Flujo de Autenticación y Autorización
        apiGateway -> keycloak "Valida JWT con Keycloak"
        ratingVertical.securityConfig -> keycloak "Obtiene clave pública para validar JWT"

        # Frontend consume el Vertical de Rating
        frontend -> apiGateway "HTTP REST con JWT en headers"
        apiGateway -> ratingVertical.apiRest "Enruta solicitudes a /api/ratings"

        # Publicación de Eventos (RatingActualizadoEvent)
        ratingVertical.messagingConfig -> rabbitmq "Publica RatingActualizadoEvent (CREATE, UPDATE)"
        rabbitmq -> catalogoService "Consume eventos para actualizar estadísticas"

        # Sistema de Películas (Contexto)
        ratingVertical -> sistemaPeliculas "Obtiene metadatos de películas"


    }

    views {

        # ========================================
        # NIVEL 1: CONTEXTO DEL SISTEMA
        # ========================================
        systemContext ratingVertical {
            title "Contexto del Sistema - Rating Service"
            description "Visión general del vertical de Rating y sus interacciones externas (Audiencia: Stakeholders, Gerentes)"

            include *
            autolayout
        }

        # ========================================
        # NIVEL 2: CONTENEDORES
        # ========================================
        container ratingVertical {
            title "Contenedores del Vertical Rating"
            description "Elementos desplegables del microservicio: API, Servicios, BD, Message Broker (Audiencia: DevOps, SREs, Arquitectos)"

            include *
            autolayout
        }

        # ========================================
        # NIVEL 3: COMPONENTES
        # ========================================
        component ratingVertical.apiRest {
            title "Componentes - Capa API REST"
            description "Detalles internos del contenedor API REST (Audiencia: Desarrolladores)"
            include *
            autolayout
        }

        component ratingVertical.appService {
            title "Componentes - Capa de Servicios"
            description "Lógica de negocio principal del Rating Service (Audiencia: Desarrolladores)"
            include *
            autolayout
        }

        component ratingVertical.domain {
            title "Componentes - Modelo de Dominio"
            description "Entidades del dominio con reglas de negocio embebidas (Audiencia: Desarrolladores)"
            include *
            autolayout
        }

        component ratingVertical.repository {
            title "Componentes - Capa de Persistencia"
            description "Acceso a datos y mapeadores JPA (Audiencia: Desarrolladores)"
            include *
            autolayout
        }

        component ratingVertical.securityConfig {
            title "Componentes - Capa de Seguridad"
            description "Validación de JWT y autorización (Audiencia: Desarrolladores)"
            include *
            autolayout
        }

        component ratingVertical.messagingConfig {
            title "Componentes - Capa de Mensajería"
            description "Configuración de RabbitMQ y publicación de eventos (Audiencia: Desarrolladores)"
            include *
            autolayout
        }

        # ========================================
        # FLUJOS DE NEGOCIO DINÁMICOS
        # ========================================
        dynamic ratingVertical {
            title "Flujo de Negocio - Crear Rating"
            description "Secuencia de pasos cuando un usuario crea un nuevo rating"

            usuario -> frontend "1. Completa formulario de rating"
            frontend -> apiGateway "2. POST /api/ratings con JWT"
            apiGateway -> ratingVertical.apiRest "3. Enruta solicitud"
            ratingVertical.apiRest -> ratingVertical.appService "4. Delega lógica de negocio"
            ratingVertical.appService -> ratingVertical.domain "5. Crea entidad Rating"
            ratingVertical.appService -> ratingVertical.repository "6. Persiste en BD"
            ratingVertical.appService -> ratingVertical.messagingConfig "7. Publica evento"
            ratingVertical.messagingConfig -> rabbitmq "8. Envía RatingActualizadoEvent"
            rabbitmq -> catalogoService "9. Consume evento"
            ratingVertical.apiRest -> frontend "10. Retorna 201 CREATED"
            frontend -> usuario "11. Muestra confirmación"
        }

        dynamic ratingVertical {
            title "Flujo de Negocio - Consultar Ratings de Película"
            description "Secuencia de pasos cuando se consultan todos los ratings de una película"

            usuario -> frontend "1. Navega a página de película"
            frontend -> apiGateway "2. GET /api/ratings/pelicula/{id}"
            apiGateway -> ratingVertical.apiRest "3. Enruta solicitud"
            ratingVertical.apiRest -> ratingVertical.appService "4. Consulta ratings"
            ratingVertical.appService -> ratingVertical.repository "5. Query a BD"
            ratingVertical.repository -> ratingVertical.database "6. Recupera registros"
            ratingVertical.appService -> ratingVertical.appService "7. Calcula estadísticas"
            ratingVertical.apiRest -> frontend "8. Retorna lista de ratings"
            frontend -> usuario "9. Renderiza ratings en UI"
        }

        # ========================================
        # ESTILOS VISUALES
        # ========================================
        styles {
            # Estilos por tipo de elemento
            element "Usuario" {
                background #1e90ff
                color #ffffff
                fontSize 24
                icon https://static.structurizr.com/themes/default/elements/person.png
            }

            element "Administrador" {
                background #ff8c00
                color #ffffff
                fontSize 24
                icon https://static.structurizr.com/themes/default/elements/person.png
            }

            element "Cliente" {
                background #3498db
                color #ffffff
                fontSize 18
                icon https://static.structurizr.com/themes/default/elements/webBrowser.png
            }

            element "SistemaExterno" {
                background #999999
                color #ffffff
                fontSize 18
                shape Box
            }

            element "Infraestructura" {
                background #2c3e50
                color #ffffff
                fontSize 18
                shape Pipe
            }

            element "RatingService" {
                background #6db33f
                color #ffffff
                fontSize 24
                icon https://static.structurizr.com/themes/default/elements/component.png
            }

            # Contenedores
            element "APIContainer" {
                background #3498db
                color #ffffff
                fontSize 16
                shape Box
            }

            element "ServiceContainer" {
                background #9b59b6
                color #ffffff
                fontSize 16
                shape Box
            }

            element "DomainContainer" {
                background #e74c3c
                color #ffffff
                fontSize 16
                shape Box
            }

            element "RepositoryContainer" {
                background #e67e22
                color #ffffff
                fontSize 16
                shape Box
            }

            element "DatabaseContainer" {
                background #95a5a6
                color #ffffff
                fontSize 16
                shape Cylinder
            }

            element "DTOContainer" {
                background #95a5a6
                color #ffffff
                fontSize 16
                shape Box
            }

            element "SecurityContainer" {
                background #2ecc71
                color #ffffff
                fontSize 16
                shape Box
            }

            element "MessagingContainer" {
                background #e74c3c
                color #ffffff
                fontSize 16
                shape Pipe
            }

            # Componentes
            element "ControllerComponent" {
                background #3498db
                color #ffffff
                fontSize 12
            }

            element "ServiceComponent" {
                background #9b59b6
                color #ffffff
                fontSize 12
            }

            element "RepositoryComponent" {
                background #e67e22
                color #ffffff
                fontSize 12
            }

            element "EntityComponent" {
                background #e74c3c
                color #ffffff
                fontSize 12
            }

            element "ValueObjectComponent" {
                background #c0392b
                color #ffffff
                fontSize 12
            }

            element "DTOComponent" {
                background #95a5a6
                color #ffffff
                fontSize 12
            }

            element "SecurityComponent" {
                background #2ecc71
                color #ffffff
                fontSize 12
            }

            element "MessagingComponent" {
                background #e74c3c
                color #ffffff
                fontSize 12
            }

            element "MapperComponent" {
                background #34495e
                color #ffffff
                fontSize 12
            }

            element "ExceptionComponent" {
                background #a93226
                color #ffffff
                fontSize 12
            }

            element "ExternalServiceComponent" {
                background #999999
                color #ffffff
                fontSize 12
            }

            element "EventComponent" {
                background #c0392b
                color #ffffff
                fontSize 12
            }
        }

    }


}

