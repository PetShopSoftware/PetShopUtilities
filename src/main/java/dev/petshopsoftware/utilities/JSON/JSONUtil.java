package dev.petshopsoftware.utilities.JSON;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JSONUtil {
    public static JsonNode navigate(JsonNode root, String[] path){
        for(String segment : path)
            root = root.path(segment);
        return root;
    }

    public static JsonNode navigate(JsonNode node, String path){
        return navigate(node, path.split("\\."));
    }

    public static void writeAt(JsonNode root, Object value, String[] path) {
        if (path == null || path.length == 0)
            throw new IllegalArgumentException("Path cannot be null or empty.");
        if(!root.isObject())
            throw new IllegalArgumentException("The root node must be an ObjectNode.");

        JsonNode current = root;

        for (int i = 0; i < path.length - 1; i++) {
            JsonNode next = current.path(path[i]);
            if (!next.isObject()) {
                next = JSON.MAPPER.createObjectNode();
                ((ObjectNode) current).set(path[i], next);
            }
            current = next;
        }

        if (current.isObject()) ((ObjectNode) current).set(path[path.length - 1], JSON.MAPPER.convertValue(value, JsonNode.class));
        else throw new IllegalStateException("Cannot set value on a non-object node.");
    }

    public static void writeAt(JsonNode node, Object value, String path){
        writeAt(node, value, path.split("\\."));
    }
}
