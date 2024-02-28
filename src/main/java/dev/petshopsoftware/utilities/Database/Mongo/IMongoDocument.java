package dev.petshopsoftware.utilities.Database.Mongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import dev.petshopsoftware.utilities.Database.DocumentReadException;
import dev.petshopsoftware.utilities.Database.DocumentWriteException;
import dev.petshopsoftware.utilities.JSON.JSON;
import dev.petshopsoftware.utilities.Logging.Log;
import dev.petshopsoftware.utilities.Logging.Logger;
import org.bson.Document;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public interface IMongoDocument extends JSON {
	default Document toDocument() {
		return Document.parse(toJSON().toString());
	}

	default <T extends IMongoDocument> T fromDocument(Document document) {
		return fromString(document.toJson());
	}

	default MongoInfo getMongoInfo() {
		if (!getClass().isAnnotationPresent(MongoInfo.class))
			throw new UnsupportedOperationException("This class is missing MongoInfo annotation.");

		return getClass().getAnnotation(MongoInfo.class);
	}

	default void save(MongoConnection mongoConnection) throws DocumentWriteException {
		String identifierField = getMongoInfo().identifier();
		Document document = toDocument();
		String id = document.get(identifierField).toString();
		AtomicBoolean saveError = new AtomicBoolean(false);
		MongoCollection<Document> collection = mongoConnection.getCollection(getClass());
		Document replaceResult = collection.findOneAndReplace(new Document(identifierField, id), document);
		if (replaceResult == null) {
			InsertOneResult insertResult;
			try {
				insertResult = collection.insertOne(document);
			} catch (RuntimeException e) {
				throw new DocumentWriteException(e);
			}
			if (insertResult.getInsertedId() == null)
				throw new DocumentWriteException("Failed saving " + getClass().getSimpleName() + " to database.");
		}
	}

	default void save() throws DocumentWriteException {
		save(MongoConnection.INSTANCE);
	}

	default <T extends IMongoDocument> T load(MongoConnection mongoConnection, Document filter) throws DocumentReadException {
		MongoCollection<Document> collection = mongoConnection.getCollection(getClass());
		Document result = collection.find(filter).first();
		if (result != null)
			return fromDocument(result);
		throw new DocumentReadException("Failed loading " + getClass().getSimpleName() + " from database.");
	}

	default <T extends IMongoDocument> T load(Document filter) throws DocumentReadException {
		return load(MongoConnection.getInstance(), filter);
	}

	default <T extends IMongoDocument> T load(MongoConnection mongoConnection, String idField, String id) throws DocumentReadException {
		return load(mongoConnection, new Document(idField, id));
	}

	default <T extends IMongoDocument> T load(String idField, String id) throws DocumentReadException {
		return load(MongoConnection.getInstance(), idField, id);
	}

	default <T extends IMongoDocument> T load(MongoConnection mongoConnection, String id) throws DocumentReadException {
		return load(mongoConnection, getMongoInfo().identifier(), id);
	}

	default <T extends IMongoDocument> T load(String id) throws DocumentReadException {
		return load(MongoConnection.getInstance(), id);
	}

	default void delete(MongoConnection mongoConnection) throws DocumentWriteException {
		String identifierField = getMongoInfo().identifier();
		Document document = toDocument();
		String id = document.get(identifierField).toString();
		MongoCollection<Document> collection = mongoConnection.getCollection(getClass());
		DeleteResult deleteResult = collection.deleteOne(new Document(identifierField, id));
		if (deleteResult.getDeletedCount() == 0)
			throw new DocumentWriteException("Failed deleting " + getClass().getSimpleName() + " from database.");
	}

	default void delete() throws DocumentWriteException {
		delete(MongoConnection.INSTANCE);
	}

	default <T extends IMongoDocument> List<T> query(MongoConnection mongoConnection, Document filter, Document sort, int limit, int skip) throws NoSuchMethodException {
		List<T> objects = new LinkedList<>();
		MongoCollection<Document> collection = mongoConnection.getCollection(getClass());
		FindIterable<Document> result = collection.find(filter);
		if (sort != null) result.sort(sort);
		if (limit != -1) result.skip(limit);
		if (skip != -1) result.skip(skip);
		Constructor<T> constructor = (Constructor<T>) getClass().getConstructor();
		for (Document document : result) {
			try {
				T object = constructor.newInstance().fromDocument(document);
				objects.add(object);
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
				Logger.get("main").error(Log.fromException(new RuntimeException("Failed to initialize MongoDocument object.", e)));
			}
		}
		return objects;
	}

	default <T extends IMongoDocument> List<T> query(Document filter, Document sort, int limit, int skip) throws NoSuchMethodException {
		return query(MongoConnection.getInstance(), filter, sort, limit, skip);
	}
}
