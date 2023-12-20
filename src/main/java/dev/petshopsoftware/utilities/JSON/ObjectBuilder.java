package dev.petshopsoftware.utilities.JSON;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ObjectBuilder implements Cloneable{
    private final ObjectNode json;

    public ObjectBuilder(){
        this.json = JSON.MAPPER.createObjectNode();
    }

    public ObjectBuilder with(String key, ObjectBuilder value) {
        json.set(key, value.build());
        return this;
    }

    public ObjectBuilder with(String key, ArrayBuilder value) {
        json.set(key, value.build());
        return this;
    }

    public ObjectBuilder with(String key, Object value) {
        json.set(key, JSON.MAPPER.convertValue(value, JsonNode.class));
        return this;
    }

    public ObjectNode build() {
        return json.deepCopy();
    }
}
