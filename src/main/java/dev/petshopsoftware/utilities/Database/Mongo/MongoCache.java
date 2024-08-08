package dev.petshopsoftware.utilities.Database.Mongo;

import com.mongodb.client.MongoCollection;
import dev.petshopsoftware.utilities.Logging.Level;
import dev.petshopsoftware.utilities.Logging.Logger;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class MongoCache {
	public static final long DEFAULT_DURATION = 5;
	public static final TimeUnit DEFAULT_UNIT = TimeUnit.SECONDS;
	private static final Map<String, MongoCache> CACHES = new HashMap<>();

	private final ConcurrentMap<String, Document> cacheMap = new ConcurrentHashMap<>();
	private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
	private final Logger logger;
	private long duration;
	private TimeUnit unit;

	protected MongoCache(MongoCollection<Document> collection, long duration, TimeUnit unit) {
		String id = collection.getNamespace().getFullName();
		this.duration = duration;
		this.unit = unit;
		this.logger = Logger.get("cache-" + id);
		this.logger.setLevel(Level.INFO);
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
		CACHES.put(id, this);
		logger.info("Cache initialized successfully for collection " + id + ".");
	}

	protected MongoCache(MongoCollection<Document> collection) {
		this(collection, DEFAULT_DURATION, DEFAULT_UNIT);
	}

	public static MongoCache getCache(MongoCollection<Document> collection) {
		MongoCache cache = CACHES.get(collection.getNamespace().getFullName());
		if (cache == null) return new MongoCache(collection);
		return cache;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public TimeUnit getUnit() {
		return unit;
	}

	public void setUnit(TimeUnit unit) {
		this.unit = unit;
	}

	public Logger getLogger() {
		return logger;
	}

	public void put(String key, Document value, long duration, TimeUnit unit) {
		if (cacheMap.containsKey(key))
			logger.debug("Updated value for key " + key + ".");
		else
			logger.debug("Cached value for key " + key + ".");
		cacheMap.put(key, value);
		cleaner.schedule(() -> {
			cacheMap.remove(key);
			logger.debug("Cleaned value for key " + key + ".");
		}, duration, unit);
	}

	public void put(String key, Document value) {
		put(key, value, this.duration, this.unit);
	}

	public Document get(String key) {
		Document document = cacheMap.get(key);
		if (document != null)
			logger.debug("Retrieved value for key " + key + ".");
		else
			logger.debug("Value not found for key " + key + ".");
		return cacheMap.get(key);
	}

	public Document remove(String key) {
		logger.debug("Removed value for key " + key + ".");
		return cacheMap.remove(key);
	}

	public void shutdown() {
		cleaner.shutdown();
		logger.info("Cache shutdown successfully.");
	}
}
