package dev.petshopsoftware.utilities.Content;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.petshopsoftware.utilities.JSON.JSON;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@JsonDeserialize(using = AbstractContent.ContentDeserializer.class)
public abstract class AbstractContent<T> implements JSON {
    public static class ContentDeserializer extends JsonDeserializer<AbstractContent<?>> {
        @Override
        public AbstractContent<?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            return AbstractContent.read(jsonParser.getCodec().readTree(jsonParser));
        }
    }

    public ContentType type;
    public T value;
    @JsonProperty("class")
    public List<String> classList;

    protected AbstractContent(ContentType type, T value, String... classList) {
        this.type = type;
        this.value = value;
        this.classList = Arrays.asList(classList);
    }

    public static AbstractContent<?> read(JsonNode contentJSON) {
        try {
            ContentType type = ContentType.valueOf(contentJSON.get("type").asText());
            String[] classList;
            if (contentJSON.has("class"))
                classList = MAPPER.convertValue(contentJSON.get("class"), new TypeReference<>() {
                });
            else classList = new String[0];

            if (type == ContentType.BLOCK) {
                List<JsonNode> contentJSONList = new ArrayList<>();
                contentJSON.get("value").forEach(contentJSONList::add);
                List<AbstractContent<?>> contentList = contentJSONList.stream().map(AbstractContent::read).collect(Collectors.toList());
                return new BlockContent(contentList, classList);
            } else if (type == ContentType.TEXT) {
                return new TextContent(contentJSON.get("value").asText(), classList);
            } else if (type == ContentType.CARD) {
                return new CardContent(read(contentJSON.get("value")), classList);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse content:\n" + contentJSON.toPrettyString(), e);
        }
        throw new RuntimeException("Parsed invalid content:\n" + contentJSON.toPrettyString());
    }
}
