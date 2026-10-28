package br.com.desafio.gestaopessoas.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI peopleManagementApi() {
        var frameworkError = new ObjectSchema()
                .description("Resposta JSON padrão do Spring para falhas de binding/validação ou erro técnico.")
                .addProperty("timestamp", new StringSchema().format("date-time"))
                .addProperty("status", new IntegerSchema().format("int32").example(400))
                .addProperty("error", new StringSchema().example("Bad Request"))
                .addProperty("path", new StringSchema().example("/employees"))
                .required(List.of("timestamp", "status", "error", "path"));
        var badRequest = new ComposedSchema()
                .description("Conforme a origem da falha, a API retorna ApiError ou a resposta padrão do Spring.")
                .addOneOfItem(new Schema<>().$ref("#/components/schemas/ApiError"))
                .addOneOfItem(new Schema<>().$ref("#/components/schemas/FrameworkError"));
        return new OpenAPI()
                .components(new Components().addSchemas("FrameworkError", frameworkError).addSchemas("BadRequest", badRequest))
                .info(new Info()
                .title("Gestão de Pessoas API")
                .version("0.0.1-SNAPSHOT")
                .description("Cadastro e consulta de colaboradores, departamentos e cargos. "
                        + "201/204 confirmam a operação no banco; publicação Kafka e histórico são assíncronos via Outbox."));
    }
}
