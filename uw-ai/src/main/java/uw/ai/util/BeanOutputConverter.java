package uw.ai.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.NonNull;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

/**
 * A utility class for converting LLM output to a specific type.
 * @param <T>
 */
public class BeanOutputConverter<T> {

    private final Logger logger = LoggerFactory.getLogger( BeanOutputConverter.class );

    /**
     * The target class type reference to which the output will be converted.
     */
    private final Type type;

    /**
     * The object mapper used for deserialization and other JSON operations.
     */
    private final ObjectMapper objectMapper;

    /**
     * Holds the generated JSON schema for the target type.
     */
    private String jsonSchema;

    /**
     * Constructor to initialize with the target type's class.
     *
     * @param clazz The target type's class.
     */
    public BeanOutputConverter(Class<T> clazz) {
        this( ParameterizedTypeReference.forType( clazz ) );
    }

    /**
     * Constructor to initialize with the target type's class, a custom object mapper, and
     * a line endings normalizer to ensure consistent line endings on any platform.
     *
     * @param clazz        The target type's class.
     * @param objectMapper Custom object mapper for JSON operations. endings.
     */
    public BeanOutputConverter(Class<T> clazz, ObjectMapper objectMapper) {
        this( ParameterizedTypeReference.forType( clazz ), objectMapper );
    }

    /**
     * Constructor to initialize with the target class type reference.
     *
     * @param typeRef The target class type reference.
     */
    public BeanOutputConverter(ParameterizedTypeReference<T> typeRef) {
        this( typeRef.getType(), null );
    }

    /**
     * Constructor to initialize with the target class type reference, a custom object
     * mapper, and a line endings normalizer to ensure consistent line endings on any
     * platform.
     *
     * @param typeRef      The target class type reference.
     * @param objectMapper Custom object mapper for JSON operations. endings.
     */
    public BeanOutputConverter(ParameterizedTypeReference<T> typeRef, ObjectMapper objectMapper) {
        this( typeRef.getType(), objectMapper );
    }

    /**
     * Constructor to initialize with the target class type reference, a custom object
     * mapper, and a line endings normalizer to ensure consistent line endings on any
     * platform.
     *
     * @param type         The target class type.
     * @param objectMapper Custom object mapper for JSON operations. endings.
     */
    private BeanOutputConverter(Type type, ObjectMapper objectMapper) {
        Objects.requireNonNull( type, "Type cannot be null;" );
        this.type = type;
        this.objectMapper = objectMapper != null ? objectMapper : getObjectMapper();
        generateSchema();
    }

    /**
     * Cleans the given text by removing leading and trailing whitespace, triple backticks, and "json" identifier.
     * @param text
     * @return
     */
    public String cleanJson(String text) {
        // Remove leading and trailing whitespace
        text = text.trim();

        // Check for and remove triple backticks and "json" identifier
        if (text.startsWith( "```" ) && text.endsWith( "```" )) {
            // Remove the first line if it contains "```json"
            String[] lines = text.split( "\n", 2 );
            if (lines[0].trim().equalsIgnoreCase( "```json" )) {
                text = lines.length > 1 ? lines[1] : "";
            } else {
                text = text.substring( 3 ); // Remove leading ```
            }

            // Remove trailing ```
            text = text.substring( 0, text.length() - 3 );

            // Trim again to remove any potential whitespace
            text = text.trim();
        }
        return text;
    }

    /**
     * Parses the given text to transform it to the desired target type.
     *
     * @param text The LLM output in string format.
     * @return The parsed output in the desired target type.
     */
    @SuppressWarnings("unchecked")
    public T convert(@NonNull String text) {
        try {
            return (T) this.objectMapper.readValue( text, this.objectMapper.constructType( this.type ) );
        } catch (JsonProcessingException e) {
            logger.error(
                    "Could not parse the given text to the desired target type: \"{}\" into {}", text, this.type );
            throw new RuntimeException( e );
        }
    }

    /**
     * Provides the expected format of the response, instructing that it should adhere to
     * the generated JSON schema.
     *
     * @return The instruction format string.
     */
    public String getFormat() {
        String template = """
                请仅使用JSON格式返回信息。
                请不要包含其他的解释和说明信息，仅输出RFC8259兼容的JSON信息。
                请不要包含任何markdown code格式标签，请移除类似```json的markdown指令。
                请严格按照以下的JSON Schema格式返回信息：
                ```%s```
                """;
        return String.format( template, this.jsonSchema );
    }

    /**
     * Provides the generated JSON schema for the target type.
     *
     * @return The generated JSON schema.
     */
    public String getJsonSchema() {
        return this.jsonSchema;
    }

    public Map<String, Object> getJsonSchemaMap() {
        try {
            return this.objectMapper.readValue( this.jsonSchema, Map.class );
        } catch (JsonProcessingException ex) {
            logger.error( "Could not parse the JSON Schema to a Map object", ex );
            throw new IllegalStateException( ex );
        }
    }

    /**
     * Configures and returns an object mapper for JSON operations.
     *
     * @return Configured object mapper.
     */
    protected ObjectMapper getObjectMapper() {
        return JsonMapper.builder()
                .addModules( AiJsonParser.instantiateAvailableModules() )
                .configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false )
                .build();
    }

    /**
     * Generates the JSON schema for the target type.
     */
    private void generateSchema() {
        JacksonModule jacksonModule = new JacksonModule( JacksonOption.RESPECT_JSONPROPERTY_REQUIRED,
                JacksonOption.RESPECT_JSONPROPERTY_ORDER );
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
                com.github.victools.jsonschema.generator.SchemaVersion.DRAFT_2020_12,
                com.github.victools.jsonschema.generator.OptionPreset.PLAIN_JSON )
                .with( jacksonModule )
                .with( Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT );
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator( config );
        JsonNode jsonNode = generator.generateSchema( this.type );
        ObjectWriter objectWriter = this.objectMapper.writer( new DefaultPrettyPrinter()
                .withObjectIndenter( new DefaultIndenter().withLinefeed( System.lineSeparator() ) ) );
        try {
            this.jsonSchema = objectWriter.writeValueAsString( jsonNode );
        } catch (JsonProcessingException e) {
            logger.error( "Could not pretty print json schema for jsonNode: {}", jsonNode );
            throw new RuntimeException( "Could not pretty print json schema for " + this.type, e );
        }
    }
}
