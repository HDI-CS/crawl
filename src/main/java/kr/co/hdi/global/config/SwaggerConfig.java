package kr.co.hdi.global.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import kr.co.hdi.global.dto.CommonResponse;
import lombok.SneakyThrows;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String cookieSchemeName = "cookieAuth";

        Components components = new Components()
                .addSecuritySchemes(cookieSchemeName, new SecurityScheme()
                        .name(cookieSchemeName)
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("JSESSIONID") // 세션 쿠키 이름 (Spring 기본값)
                );

        SecurityRequirement securityRequirement = new SecurityRequirement().addList(cookieSchemeName);

        return new OpenAPI()
                .components(components)
                .addSecurityItem(securityRequirement)
                .addServersItem(new Server().url("/"))
                .info(apiInfo());
    }

    private io.swagger.v3.oas.models.info.Info apiInfo() {
        return new io.swagger.v3.oas.models.info.Info()
                .title("HDI BE API")
                .version("v1")
                .description("HDI BE API 문서 (세션 기반 인증)");
    }

    @Bean
    public OperationCustomizer operationCustomizer() {
        return (operation, handlerMethod) -> {
            addResponseBodyWrapperSchemaExample(operation, CommonResponse.class, "data");
            return operation;
        };
    }

    private void addResponseBodyWrapperSchemaExample(Operation operation, Class<?> type, String wrapFieldName) {
        ApiResponse response200 = operation.getResponses().get("200");
        if (response200 != null) {
            final Content content = response200.getContent();
            if (content != null) {
                content.keySet().forEach(mediaTypeKey -> {
                    final MediaType mediaType = content.get(mediaTypeKey);
                    mediaType.schema(wrapSchema(mediaType.getSchema(), type, wrapFieldName));
                });
            }
        }
    }

    @SneakyThrows
    private <T> Schema<T> wrapSchema(Schema<?> originalSchema, Class<T> type, String wrapFieldName) {
        final Schema<T> wrapperSchema = new Schema<>();

        wrapperSchema.addProperty("status", new Schema<Integer>().example(200));
        wrapperSchema.addProperty("message", new Schema<String>().example("OK"));
        wrapperSchema.addProperty(wrapFieldName, originalSchema);

        return wrapperSchema;
    }
}