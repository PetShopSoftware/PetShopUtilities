package dev.petshopsoftware.utilities.Content;

public class CardContent extends AbstractContent<AbstractContent<?>> {
    protected CardContent(AbstractContent<?> value, String... classList) {
        super(ContentType.CARD, value, classList);
    }
}
