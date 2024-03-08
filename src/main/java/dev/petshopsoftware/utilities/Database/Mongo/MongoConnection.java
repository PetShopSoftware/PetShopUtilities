package dev.petshopsoftware.utilities.Database.Mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import dev.petshopsoftware.utilities.Logging.Logger;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.logging.Level;

public class MongoConnection {
	protected static MongoConnection INSTANCE = null;

	static {
		java.util.logging.Logger.getLogger("org.mongodb.driver").setLevel(Level.OFF);
		java.util.logging.Logger.getLogger("org.bson").setLevel(Level.OFF);
	}

	private final Logger logger;
	private final MongoClient client;
	private MongoDatabase database;

	public MongoConnection(MongoClient client, String databaseName) {
		this.logger = Logger.get("mongo-" + (databaseName == null ? "undefined" : databaseName));
		this.client = client;
		if (databaseName != null) {
			CodecRegistry defaultCodecRegistry = MongoClientSettings.getDefaultCodecRegistry();
			CodecRegistry fromProvider = CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build());
			CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(defaultCodecRegistry, fromProvider);
			this.database = client.getDatabase(databaseName).withCodecRegistry(pojoCodecRegistry);
		}
		this.logger.info("Mongo connection established.");
		if (INSTANCE == null) INSTANCE = this;
	}

	public MongoConnection(String databaseName) {
		this(getInstance().getClient(), databaseName);
	}

	public MongoConnection(String mongoURI, String databaseName) {
		this(MongoClients.create(mongoURI), databaseName);
	}


	public static MongoConnection getInstance() {
		if (INSTANCE == null)
			throw new NullPointerException("Instance is null. Please initialize a new MongoConnection.");
		return INSTANCE;
	}

	public Logger getLogger() {
		return logger;
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
		while (clazz != null && !clazz.isAnnotationPresent(MongoInfo.class))
			clazz = clazz.getSuperclass();
		if (clazz == null)
			throw new IllegalArgumentException("Provided class and its superclasses are missing MongoInfo annotation.");
		return getCollection(clazz.getAnnotation(MongoInfo.class).collection());
	}
}
