package dev.petshopsoftware.utilities.JSON;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class ArrayBuilder {
    private final ArrayNode json = JSON.MAPPER.createArrayNode();

    public ArrayBuilder add(ArrayBuilder value) {
        json.add(value.build());
        return this;
    }

    public ArrayBuilder add(ObjectBuilder value) {
        json.add(value.build());
        return this;
    }

    public ArrayBuilder add(Object value) {
        json.add(JSON.MAPPER.convertValue(value, JsonNode.class));
        return this;
    }

    public ArrayNode build() {
        return json.deepCopy();
    }
}
