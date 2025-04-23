package uw.ai.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import org.springframework.util.Assert;
import uw.common.dto.ResponseData;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.stream.Stream;

/**
 * Utilities to generate JSON Schemas from Java types and method signatures. It's designed
 * to work well in the context of tool calling and structured outputs, aiming at ensuring
 * consistency and robustness across different model providers.
 * <p>
 * Metadata such as descriptions and required properties can be specified using one of the
 * following supported annotations:
 * <p>
 * <ul>
 * <li>{@code @ToolParam(required = ..., description = ...)}</li>
 * <li>{@code @JsonProperty(required = ...)}</li>
 * <li>{@code @JsonClassDescription(...)}</li>
 * <li>{@code @JsonPropertyDescription(...)}</li>
 * <li>{@code @Schema(required = ..., description = ...)}</li>
 * <li>{@code @Nullable}</li>
 * </ul>
 * <p>
 * If none of these annotations are present, the default behavior is to consider the
 * property as required and not to include a description.
 * <p>
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public final class AiToolSchemaGenerator {

    private static final SchemaGenerator TYPE_SCHEMA_GENERATOR;

    /*
     * Initialize JSON Schema generators.
     */
    static {
        Module jacksonModule = new JacksonModule( JacksonOption.RESPECT_JSONPROPERTY_REQUIRED );
        Module openApiModule = new Swagger2Module();

        SchemaGeneratorConfigBuilder schemaGeneratorConfigBuilder =
                new SchemaGeneratorConfigBuilder( SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON )
                        .with( jacksonModule )
                        .with( openApiModule )
                        .with( Option.EXTRA_OPEN_API_FORMAT_VALUES )
                        .with( Option.PLAIN_DEFINITION_KEYS );
        // 排除认证字段。
        schemaGeneratorConfigBuilder.forFields().withIgnoreCheck( f -> (f.getDeclaredName().equals( "saasId" )|| f.getDeclaredName().equals( "userId" )||f.getDeclaredName().equals( "userType" )||f.getDeclaredName().equals( "userInfo" )));
        SchemaGeneratorConfig typeSchemaGeneratorConfig = schemaGeneratorConfigBuilder.build();
        TYPE_SCHEMA_GENERATOR = new SchemaGenerator( typeSchemaGeneratorConfig );
    }

    private AiToolSchemaGenerator() {
    }

    /**
     * Generate a JSON Schema for a method's input parameters.
     */
    public static String generateForMethodInput(Method method, SchemaOption... schemaOptions) {
        Type[] paramTypes = method.getGenericParameterTypes();
        if (paramTypes.length == 1){
            return generateForType( paramTypes[0], schemaOptions);
        }
        return null;
    }

    /**
     * Generate a JSON Schema for a method's output parameters.
     */
    public static String generateForMethodOutput(Method method, SchemaOption... schemaOptions) {
        Type returnType = method.getGenericReturnType();

        if (returnType instanceof ParameterizedType parameterizedType) {
            if (parameterizedType.getRawType() == ResponseData.class) {
                returnType = parameterizedType.getActualTypeArguments()[0];
                return generateForType( returnType, schemaOptions);
            }
        }
        return null;
    }

    /**
     * Generate a JSON Schema for a class type.
     */
    public static String generateForType(Type type, SchemaOption... schemaOptions) {
        Assert.notNull( type, "type cannot be null" );
        ObjectNode schema = TYPE_SCHEMA_GENERATOR.generateSchema( type );
        if ((type == Void.class) && !schema.has( "properties" )) {
            schema.putObject( "properties" );
        }
        processSchemaOptions( schemaOptions, schema );
        return schema.toPrettyString();
    }

    // Based on the method in ModelOptionsUtils.
    public static void convertTypeValuesToUpperCase(ObjectNode node) {
        if (node.isObject()) {
            node.fields().forEachRemaining( entry -> {
                JsonNode value = entry.getValue();
                if (value.isObject()) {
                    convertTypeValuesToUpperCase( (ObjectNode) value );
                } else if (value.isArray()) {
                    value.elements().forEachRemaining( element -> {
                        if (element.isObject() || element.isArray()) {
                            convertTypeValuesToUpperCase( (ObjectNode) element );
                        }
                    } );
                } else if (value.isTextual() && entry.getKey().equals( "type" )) {
                    String oldValue = node.get( "type" ).asText();
                    node.put( "type", oldValue.toUpperCase() );
                }
            } );
        } else if (node.isArray()) {
            node.elements().forEachRemaining( element -> {
                if (element.isObject() || element.isArray()) {
                    convertTypeValuesToUpperCase( (ObjectNode) element );
                }
            } );
        }
    }

    private static void processSchemaOptions(SchemaOption[] schemaOptions, ObjectNode schema) {
        if (Stream.of( schemaOptions ).noneMatch( option -> option == SchemaOption.ALLOW_ADDITIONAL_PROPERTIES_BY_DEFAULT )) {
            schema.put( "additionalProperties", false );
        }
        if (Stream.of( schemaOptions ).anyMatch( option -> option == SchemaOption.UPPER_CASE_TYPE_VALUES )) {
            convertTypeValuesToUpperCase( schema );
        }
    }

    /**
     * Options for generating JSON Schemas.
     */
    public enum SchemaOption {

        /**
         * Allow an object to contain additional key/values not defined in the schema.
         */
        ALLOW_ADDITIONAL_PROPERTIES_BY_DEFAULT,

        /**
         * Convert all "type" values to upper case.
         */
        UPPER_CASE_TYPE_VALUES;

    }

}
