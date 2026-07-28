package unrn.rating.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Configuración de Spring Security para validar JWT de Keycloak.
 * El microservicio actúa como OAuth2 Resource Server.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

        @Value("${security.keycloak.client-id:}")
        private String keycloakClientId;

        @Bean
        public WebSecurityCustomizer webSecurityCustomizer() {
                return web -> web.ignoring().requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/info/**");
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

                http
                                .cors(withDefaults())
                                .csrf(AbstractHttpConfigurer::disable);

                http.sessionManagement(sessionManagement -> sessionManagement
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

                http
                                .authorizeHttpRequests(registry -> registry
                                                .requestMatchers("/actuator/health", "/actuator/health/**",
                                                                "/actuator/info", "/actuator/info/**")
                                                .permitAll()
                                        .requestMatchers("/swagger-ui/", "/v3/api-docs/", "/swagger-ui.html").permitAll()
                                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/ratings/**").permitAll()
                                                // Keep other actuator endpoints secured by requiring authentication
                                                .requestMatchers("/actuator/**", "/metrics/**").authenticated()
                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oauth2Configurer -> oauth2Configurer
                                                .jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(
                                                                jwtAuthenticationConverter())));

                return http.build();
        }

        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {
                JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
                converter.setJwtGrantedAuthoritiesConverter(new KeycloakGrantedAuthoritiesConverter(keycloakClientId));
                return converter;
        }
}
