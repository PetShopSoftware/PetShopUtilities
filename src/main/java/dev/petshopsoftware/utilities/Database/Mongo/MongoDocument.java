package dev.petshopsoftware.utilities.Database.Mongo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import dev.petshopsoftware.utilities.Database.DocumentReadException;
import dev.petshopsoftware.utilities.Database.DocumentWriteException;
import dev.petshopsoftware.utilities.Logging.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public abstract class MongoDocument implements IMongoDocument {
	@JsonIgnore
	private Document initialDocument = null;

	@Override
	public <T extends IMongoDocument> T load(MongoConnection mongoConnection, Document filter) throws DocumentReadException {
		T document = IMongoDocument.super.load(mongoConnection, filter);
		initialDocument = document.toDocument();
		Logger.get("main").info("Fetched the following document:\n" + initialDocument.toJson());
		return document;
	}

	void update(MongoConnection mongoConnection) throws DocumentWriteException {
		if (initialDocument == null) save(mongoConnection);
		List<Bson> actions = new LinkedList<>();
		Document currentDocument = toDocument();
		Set<String> uniqueFields = new HashSet<>(currentDocument.keySet());
		uniqueFields.addAll(initialDocument.keySet());
		for (String key : uniqueFields) {
			if (!currentDocument.containsKey(key))
				actions.add(Updates.unset(key));
			else if (!initialDocument.get(key).equals(currentDocument.get(key)))
				actions.add(Updates.set(key, currentDocument.get(key)));
		}
		if (actions.isEmpty()) return;
		String identifierField = getMongoInfo().identifier();
		MongoCollection<Document> collection = mongoConnection.getCollection(getClass());
		UpdateResult result = collection.updateOne(Filters.eq(identifierField, currentDocument.get(identifierField)), Updates.combine(actions));
		if (result.getModifiedCount() != 1)
			throw new DocumentWriteException("Could not update " + getClass().getSimpleName() + " in database.");
	}

	public void update() throws DocumentWriteException {
		update(MongoConnection.INSTANCE);
	}
}
