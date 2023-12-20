package dev.petshopsoftware.utilities.Content;

public class TextContent extends AbstractContent<String> {
    protected TextContent(String value, String... classList) {
        super(ContentType.TEXT, value, classList);
    }
}
