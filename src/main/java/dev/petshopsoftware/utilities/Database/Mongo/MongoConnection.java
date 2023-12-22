package dev.petshopsoftware.utilities.Database.Mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import dev.petshopsoftware.utilities.Logging.Log;
import dev.petshopsoftware.utilities.Logging.Logger;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.logging.Level;

public class MongoConnection {
	public static MongoConnection DEFAULT_CONNECTION = null;

	static {
		java.util.logging.Logger.getLogger("org.mongodb.driver").setLevel(Level.OFF);
		java.util.logging.Logger.getLogger("org.bson").setLevel(Level.OFF);
	}

	private MongoClient client;
	private MongoDatabase database;

	public MongoConnection(String mongoURI, String databaseName) {
		try {
			client = MongoClients.create(mongoURI);
			CodecRegistry defaultCodecRegistry = MongoClientSettings.getDefaultCodecRegistry();
			CodecRegistry fromProvider = CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build());
			CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(defaultCodecRegistry, fromProvider);
			database = client.getDatabase(databaseName).withCodecRegistry(pojoCodecRegistry);
			Logger.get("mongo-" + databaseName).info("Mongo connection established.");
		} catch (Exception e) {
			Logger.get("mongo-" + databaseName).fatal(Log.fromException(new RuntimeException("Mongo connection failed.", e)));
			return;
		}
		if (DEFAULT_CONNECTION == null) DEFAULT_CONNECTION = this;
	}

	public MongoClient getClient() {
		return client;
	}

	public MongoDatabase getDatabase() {
		return database;
	}

	public MongoCollection<Document> getCollection(String name) {
		return database.getCollection(name);
	}

	public MongoCollection<Document> getCollection(Class<?> clazz) {
		if (!clazz.isAnnotationPresent(MongoInfo.class))
			throw new IllegalArgumentException("Provided class is missing MongoInfo annotation.");
		return getCollection(clazz.getAnnotation(MongoInfo.class).collection());
	}
}
