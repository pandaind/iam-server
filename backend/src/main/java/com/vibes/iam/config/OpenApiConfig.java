package com.vibes.iam.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "IAM Server API",
        description = "Identity and Access Management Server - A comprehensive IAM solution with JWT authentication, OAuth2 integration, role-based access control, session management, and audit logging.",
        version = "1.0.0",
        contact = @Contact(
            name = "IAM Server Team",
            email = "support@vibes.com",
            url = "https://vibes.com"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8080/api/v1", description = "Development Server"),
        @Server(url = "https://api.vibes.com/api/v1", description = "Production Server")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    description = "JWT Bearer Authentication",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}