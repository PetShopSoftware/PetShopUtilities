package dev.petshopsoftware.utilities.Content;

import java.util.List;

public class BlockContent extends AbstractContent<List<AbstractContent<?>>> {
    protected BlockContent(List<AbstractContent<?>> value, String... classList) {
        super(ContentType.BLOCK, value, classList);
    }
}
