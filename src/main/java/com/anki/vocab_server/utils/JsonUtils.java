package com.anki.vocab_server.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JsonUtils {
    // Thread-safe, reusable Gson instance
    private static final Gson GSON = new GsonBuilder()
            .serializeNulls() // Includes null fields in JSON output
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX") // Standard ISO-8601 date format
            .disableHtmlEscaping() // Prevents converting <, >, &, etc. to Unicode
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE)))
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, typeOfT, context) ->
                    LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE))
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

    // Private constructor to prevent instantiation
    private JsonUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Converts any Java object into its JSON string representation.
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        return GSON.toJson(obj);
    }
    public static String mapToJson(Map<String, Object> obj) {
        return GSON.toJson(obj);
    }
    /**
     * Converts a JSON string into a specific Java object class.
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return GSON.fromJson(json, clazz);
        } catch (JsonSyntaxException e) {
            // Handle or log exception based on your project's logging framework
            return null;
        }
    }

    /**
     * Converts a JSON string into a List of a specific type.
     * Usage: List<User> users = JsonUtils.fromJsonList(jsonString, User.class);
     */
    public static <T> List<T> fromJsonList(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Type type = TypeToken.getParameterized(List.class, clazz).getType();
            return GSON.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Converts a JSON string into a Map.
     * Usage: Map<String, Object> map = JsonUtils.fromJsonMap(jsonString);
     */
    public static Map<String, Object> fromJsonMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return GSON.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            return Collections.emptyMap();
        }
    }

    /**
     * Exposes the underlying Gson instance if advanced customization is needed.
     */
    public static Gson getGson() {
        return GSON;
    }
}
