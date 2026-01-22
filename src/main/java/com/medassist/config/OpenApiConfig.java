package com.medassist.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.servers.Servers;

@OpenAPIDefinition(
        info = @Info(
                description = "MedAssist Api Documentation",
                title = "MedAssist",
                version = "1.0",
                termsOfService = "Terms Of Service",
                license = @License(
                        name = "MedAssist license",
                        url = "https://med-assist-xi-liart.vercel.app/"
                ),
                contact = @Contact(
                        name = "Henry",
                        email = "fakorodehenry@gmail.com"
                )
        ),
        servers = {
                @Server(
                        description = "Local Development",
                        url = "http://localhost:8080"
                ),
                @Server(
                        description = "Production Development",
                        url = "https://medassist-23zx.onrender.com"
                )
        },
        security = @SecurityRequirement(
                name = "JWT Token"
        )
)
@SecurityScheme(
        name = "JWT Token",
        description = "MedAssist Authorization Mechanism",
        type = SecuritySchemeType.HTTP,
        in =  SecuritySchemeIn.HEADER,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class OpenApiConfig {
}
