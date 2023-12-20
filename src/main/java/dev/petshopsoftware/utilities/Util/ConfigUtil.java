package dev.petshopsoftware.utilities.Util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

public class ConfigUtil {
    public static String readFileFromResources(String filePath) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        Objects.requireNonNull(ConfigUtil.class.getResourceAsStream("/" + filePath)),
                        StandardCharsets.UTF_8
                )
        )) {
            StringBuilder contentBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                contentBuilder.append(line).append("\n");
            return contentBuilder.toString();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static JsonNode readConfigFromResources(String name) {
        try {
            String contents = readFileFromResources(name + ".json");
            return new ObjectMapper().readTree(contents);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static JsonNode saveJSONConfig(String name, JsonNode jsonObject) {
        try {
            File file = new File(name + ".json");
            Path parentDir = file.toPath().getParent();
            if (!Files.exists(parentDir)) Files.createDirectories(parentDir);
            new ObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(file, jsonObject);
            return jsonObject;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static JsonNode readJSONConfig(String name) {
        File dataFolder = new File(System.getProperty("user.dir"));
        if (!dataFolder.exists()) dataFolder.mkdirs();
        try {
            return new ObjectMapper()
                    .readValue(
                            new File(dataFolder, name + ".json"),
                            JsonNode.class
                    );
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static JsonNode initializeJSONConfig(boolean devMode, String name, JsonNode jsonObject) {
        File configFile = new File(name + ".json");
        if (!devMode && configFile.exists()) {
            JsonNode existingConfig = readJSONConfig(name);
            JsonNode mergedConfig = deepMerge(jsonObject, existingConfig);
            return saveJSONConfig(name, mergedConfig);
        } else return saveJSONConfig(name, jsonObject);
    }

    public static JsonNode initializeJSONConfig(boolean devMode, String name, String resource) {
        try {
            JsonNode json = new ObjectMapper()
                    .readTree(readFileFromResources(resource + ".json"));
            return initializeJSONConfig(devMode, name, json);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static JsonNode initializeJSONConfig(boolean devMode, String name) {
        return initializeJSONConfig(devMode, name, name);
    }

    public static JsonNode deepMerge(JsonNode source, JsonNode target) {
        if (!source.isObject() || !target.isObject()) return source;

        ObjectNode targetObject = (ObjectNode) target;
        ObjectNode sourceObject = (ObjectNode) source;
        Iterator<String> targetFieldNames = targetObject.fieldNames();
        List<String> fieldsToRemove = new ArrayList<>();
        while (targetFieldNames.hasNext()) {
            String fieldName = targetFieldNames.next();
            if (!sourceObject.has(fieldName))
                fieldsToRemove.add(fieldName);
        }
        for (String fieldToRemove : fieldsToRemove)
            targetObject.remove(fieldToRemove);
        Iterator<Entry<String, JsonNode>> sourceFields = sourceObject.fields();
        while (sourceFields.hasNext()) {
            Entry<String, JsonNode> sourceField = sourceFields.next();
            String key = sourceField.getKey();
            JsonNode sourceValue = sourceField.getValue();
            if (sourceValue.isObject() && targetObject.has(key) && targetObject.get(key).isObject())
                deepMerge(sourceValue, targetObject.get(key));
            else if (targetObject.has(key) && sourceValue.getNodeType() != JsonNodeType.NULL && !sourceValue.getNodeType().equals(targetObject.get(key).getNodeType()))
                targetObject.set(key, sourceValue);
            else if (!targetObject.has(key))
                targetObject.set(key, sourceValue);
        }
        return targetObject;
    }
}
