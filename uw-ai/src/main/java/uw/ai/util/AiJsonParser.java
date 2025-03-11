package uw.ai.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities to perform parsing operations between JSON and Java.
 */
public final class AiJsonParser {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES )
            .disable( SerializationFeature.FAIL_ON_EMPTY_BEANS )
            .addModules( instantiateAvailableModules() )
            .build();

    private AiJsonParser() {
    }

    /**
     * Returns a Jackson {@link ObjectMapper} instance tailored for JSON-parsing
     * operations for tool calling and structured output.
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * Converts a JSON string to a Java object.
     */
    public static <T> T fromJson(String json, Class<T> type) {
        Assert.notNull( json, "json cannot be null" );
        Assert.notNull( type, "type cannot be null" );

        try {
            return OBJECT_MAPPER.readValue( json, type );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException( "Conversion from JSON to %s failed".formatted( type.getName() ), ex );
        }
    }

    /**
     * Converts a JSON string to a Java object.
     */
    public static <T> T fromJson(String json, Type type) {
        Assert.notNull( json, "json cannot be null" );
        Assert.notNull( type, "type cannot be null" );

        try {
            return OBJECT_MAPPER.readValue( json, OBJECT_MAPPER.constructType( type ) );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException( "Conversion from JSON to %s failed".formatted( type.getTypeName() ), ex );
        }
    }

    /**
     * Converts a JSON string to a Java object.
     */
    public static <T> T fromJson(String json, TypeReference<T> type) {
        Assert.notNull( json, "json cannot be null" );
        Assert.notNull( type, "type cannot be null" );

        try {
            return OBJECT_MAPPER.readValue( json, type );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException( "Conversion from JSON to %s failed".formatted( type.getType().getTypeName() ),
                    ex );
        }
    }

    /**
     * Converts a Java object to a JSON string.
     */
    public static String toJson(@Nullable Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString( object );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException( "Conversion from Object to JSON failed", ex );
        }
    }

    /**
     * Convert a Java Object to a typed Object. Based on the implementation in
     * MethodInvokingFunctionCallback.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Object toTypedObject(Object value, Class<?> type) {
        Assert.notNull( value, "value cannot be null" );
        Assert.notNull( type, "type cannot be null" );

        var javaType = ClassUtils.resolvePrimitiveIfNecessary( type );

        if (javaType == String.class) {
            return value.toString();
        } else if (javaType == Byte.class) {
            return Byte.parseByte( value.toString() );
        } else if (javaType == Integer.class) {
            return Integer.parseInt( value.toString() );
        } else if (javaType == Short.class) {
            return Short.parseShort( value.toString() );
        } else if (javaType == Long.class) {
            return Long.parseLong( value.toString() );
        } else if (javaType == Double.class) {
            return Double.parseDouble( value.toString() );
        } else if (javaType == Float.class) {
            return Float.parseFloat( value.toString() );
        } else if (javaType == Boolean.class) {
            return Boolean.parseBoolean( value.toString() );
        } else if (javaType.isEnum()) {
            return Enum.valueOf( (Class<Enum>) javaType, value.toString() );
        }

        String json = AiJsonParser.toJson( value );
        return AiJsonParser.fromJson( json, javaType );
    }

    /**
     * Instantiate well-known Jackson modules available in the classpath.
     * <p>
     * Supports the follow-modules: <code>Jdk8Module</code>, <code>JavaTimeModule</code>,
     * <code>ParameterNamesModule</code> and <code>KotlinModule</code>.
     *
     * @return The list of instantiated modules.
     */
    @SuppressWarnings("unchecked")
    private static List<com.fasterxml.jackson.databind.Module> instantiateAvailableModules() {
        List<com.fasterxml.jackson.databind.Module> modules = new ArrayList<>();
        try {
            Class<? extends com.fasterxml.jackson.databind.Module> jdk8ModuleClass = (Class<? extends com.fasterxml.jackson.databind.Module>) ClassUtils
                    .forName( "com.fasterxml.jackson.datatype.jdk8.Jdk8Module", null );
            com.fasterxml.jackson.databind.Module jdk8Module = BeanUtils.instantiateClass( jdk8ModuleClass );
            modules.add( jdk8Module );
        } catch (ClassNotFoundException ex) {
            // jackson-datatype-jdk8 not available
        }

        try {
            Class<? extends com.fasterxml.jackson.databind.Module> javaTimeModuleClass = (Class<? extends com.fasterxml.jackson.databind.Module>) ClassUtils
                    .forName( "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule", null );
            com.fasterxml.jackson.databind.Module javaTimeModule = BeanUtils.instantiateClass( javaTimeModuleClass );
            modules.add( javaTimeModule );
        } catch (ClassNotFoundException ex) {
            // jackson-datatype-jsr310 not available
        }

        try {
            Class<? extends com.fasterxml.jackson.databind.Module> parameterNamesModuleClass = (Class<? extends com.fasterxml.jackson.databind.Module>) ClassUtils
                    .forName( "com.fasterxml.jackson.module.paramnames.ParameterNamesModule", null );
            com.fasterxml.jackson.databind.Module parameterNamesModule = BeanUtils
                    .instantiateClass( parameterNamesModuleClass );
            modules.add( parameterNamesModule );
        } catch (ClassNotFoundException ex) {
            // jackson-module-parameter-names not available
        }

        return modules;
    }

}
