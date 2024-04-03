package dev.petshopsoftware.utilities.Database.Mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import dev.petshopsoftware.utilities.Logging.Log;
import dev.petshopsoftware.utilities.Logging.Logger;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class MongoConnection {
	protected static final Set<String> indexedCollections = new LinkedHashSet<>();
	protected static MongoConnection INSTANCE = null;

	static {
		java.util.logging.Logger.getLogger("org.mongodb.driver").setLevel(Level.OFF);
		java.util.logging.Logger.getLogger("org.bson").setLevel(Level.OFF);
	}

	private final Logger logger;
	private final MongoClient client;
	private final Map<String, Boolean> cachedMap = new HashMap<>();
	private boolean databaseCached = true;
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

	public Map<String, Boolean> getCachedMap() {
		return cachedMap;
	}

	public boolean isDatabaseCached() {
		return databaseCached;
	}

	public void setDatabaseCached(boolean databaseCached) {
		this.databaseCached = databaseCached;
	}

	public MongoCollection<Document> getCollection(String name) {
		return database.getCollection(name);
	}

	public MongoCollection<Document> getCollection(Class<?> clazz) {
		while (clazz != null && !clazz.isAnnotationPresent(MongoInfo.class))
			clazz = clazz.getSuperclass();
		if (clazz == null)
			throw new IllegalArgumentException("Provided class and its superclasses are missing MongoInfo annotation.");
		MongoInfo mongoInfo = clazz.getAnnotation(MongoInfo.class);
		MongoCollection<Document> collection = getCollection(mongoInfo.collection());
		setupIndexes(collection, mongoInfo);
		return collection;
	}

	public boolean isCached(MongoCollection<Document> collection) {
		return cachedMap.getOrDefault(collection.getNamespace().getFullName(), databaseCached);
	}

	public boolean isCached(Class<?> clazz) {
		return isCached(getCollection(clazz));
	}

	public void setupIndexes(MongoCollection<Document> collection, MongoInfo mongoInfo) {
		String collectionID = collection.getNamespace().getFullName();
		if (indexedCollections.contains(collectionID)) return;
		indexedCollections.add(collectionID);

		logger.info("Setting up indexes in collection " + collectionID + "...");

		Set<String> existingIndexes = new LinkedHashSet<>();
		collection.listIndexes().forEach(document -> existingIndexes.add(document.getString("name")));
		Set<String> addedIndexes = new LinkedHashSet<>();

		MongoInfo.MongoIndexData[] indexes = mongoInfo.indexes();
		for (MongoInfo.MongoIndexData data : indexes) {
			String indexID = generateIndexName(data);

			if (existingIndexes.contains(indexID)) {
				logger.warn("Index " + indexID + " already exists in collection " + collectionID + ".");
				addedIndexes.add(indexID);
				continue;
			}

			Optional<String> outdatedIndexID = existingIndexes.stream().filter(name -> name.startsWith("auto-managed_" + data.field())).findFirst();
			if (outdatedIndexID.isPresent()) {
				logger.debug("Updating index " + indexID + "(from old " + outdatedIndexID.get() + ") in collection " + collectionID + "...");
				try {
					collection.dropIndex(outdatedIndexID.get());
				} catch (Exception e) {
					logger.error(Log.fromException(new RuntimeException("Failed updating " + indexID + "(from old " + outdatedIndexID.get() + ") in collection " + collectionID + ".", e)));
					continue;
				}
			}

			IndexOptions options = new IndexOptions()
					.name(indexID)
					.unique(data.unique())
					.sparse(data.sparse());
			if (data.ttl() > 0)
				options.expireAfter(data.ttl(), TimeUnit.MILLISECONDS);
			if (data.partial())
				options.partialFilterExpression(Filters.exists(data.field()));

			Bson index;
			if (data.text())
				index = Indexes.text(data.field());
			else if (data.ascending())
				index = Indexes.ascending(data.field());
			else
				index = Indexes.descending(data.field());

			try {
				collection.createIndex(index, options);
				addedIndexes.add(indexID);
				logger.info("Index " + indexID + " successfully created in collection " + collectionID + ".");
			} catch (Exception e) {
				logger.error(Log.fromException(new RuntimeException("Failed creating " + indexID + " in collection " + collectionID + ".", e)));
			}
		}

		for (String indexID : existingIndexes) {
			if (!indexID.startsWith("auto-managed_")) continue;
			if (addedIndexes.contains(indexID)) continue;
			try {
				collection.dropIndex(indexID);
				logger.info("Dropping outdated " + indexID + " in collection " + collectionID + ".");
			} catch (Exception e) {
				logger.error(Log.fromException(new RuntimeException("Failed dropping outdated " + indexID + " in collection " + collectionID + ".", e)));
			}
		}

		logger.info("Finished setting up " + indexes.length + " and updating index(es) in collection " + collectionID + ".");
	}

	private String generateIndexName(MongoInfo.MongoIndexData data) {
		StringBuilder baseName = new StringBuilder("auto-managed");
		baseName.append("_").append(data.field().replaceAll("\\.", "_"));
		if (data.unique()) baseName.append("_unique");
		if (data.sparse()) baseName.append("_sparse");
		if (data.text()) baseName.append("_text");
		if (data.partial()) baseName.append("_partial");
		if (data.ttl() > 0) baseName.append("_ttl(").append(data.ttl()).append(")");
		if (data.ascending()) baseName.append("_asc");
		else baseName.append("_desc");
		return baseName.toString();
	}
}
