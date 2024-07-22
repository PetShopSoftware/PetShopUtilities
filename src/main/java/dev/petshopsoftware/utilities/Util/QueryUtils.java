package dev.petshopsoftware.utilities.Util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.Function;
import com.mongodb.client.model.Filters;
import dev.petshopsoftware.utilities.Database.Mongo.IMongoDocument;
import dev.petshopsoftware.utilities.Database.Mongo.MongoConnection;
import dev.petshopsoftware.utilities.HTTP.Server.HTTPData;
import dev.petshopsoftware.utilities.HTTP.Server.HTTPResponse;
import dev.petshopsoftware.utilities.HTTP.Server.HTTPResponseException;
import dev.petshopsoftware.utilities.JSON.JSON;
import dev.petshopsoftware.utilities.JSON.ObjectBuilder;
import dev.petshopsoftware.utilities.Util.InputChecker.IntegerChecker;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class QueryUtils {
	public static <T extends IMongoDocument> PaginatedQueryResult<T> makePaginatedQuery(MongoConnection mongoConnection, T document, Bson filter, Bson sort, int page, int limit) throws HTTPResponseException {
		long totalItems = mongoConnection.getCollection(document.getClass()).countDocuments(filter);
		long totalPages = limit > 0 ? (int) Math.ceil((double) totalItems / limit) : 1;
		List<T> documents = new LinkedList<>();
		if (totalItems > 0)
			try {
				documents = document.query(mongoConnection, filter, sort, limit, page * limit);
			} catch (NoSuchMethodException e) {
				throw new HTTPResponseException(HTTPResponse.INTERNAL_ERROR);
			}

		return new PaginatedQueryResult<>(page, limit, totalItems, totalPages, documents);
	}

	public static <T extends IMongoDocument> PaginatedQueryResult<T> makePaginatedQuery(T document, Bson filter, Bson sort, int page, int limit) throws HTTPResponseException {
		return makePaginatedQuery(MongoConnection.getInstance(), document, filter, sort, page, limit);
	}

	public static <T extends IMongoDocument> PaginatedQueryResult<T> makePaginatedQuery(MongoConnection mongoConnection, T document, Bson filter, Bson sort, HTTPData data) throws HTTPResponseException {
		int page = new IntegerChecker(data.queryParams().getOrDefault("page", "0")).min(0, "Invalid page value.").matches();
		int limit = new IntegerChecker(data.queryParams().getOrDefault("limit", "20")).min(-1, "Invalid limit value.").matches();
		return makePaginatedQuery(mongoConnection, document, filter, sort, page, limit);
	}

	public static <T extends IMongoDocument> PaginatedQueryResult<T> makePaginatedQuery(T document, Bson filter, Bson sort, HTTPData data) throws HTTPResponseException {
		return makePaginatedQuery(MongoConnection.getInstance(), document, filter, sort, data);
	}

	public static <T extends IMongoDocument> PaginatedQueryResult<T> makePaginatedQuery(MongoConnection mongoConnection, T document, HTTPData data) throws HTTPResponseException {
		return makePaginatedQuery(mongoConnection, document, Filters.empty(), new Document(), data);
	}

	public static <T extends IMongoDocument> PaginatedQueryResult<T> makePaginatedQuery(T document, HTTPData data) throws HTTPResponseException {
		return makePaginatedQuery(MongoConnection.getInstance(), document, data);
	}

	public static class PaginatedQueryResult<T extends IMongoDocument> implements JSON {
		private final int page;
		private final int limit;
		@JsonProperty("total_items")
		private final long totalItems;
		@JsonProperty("total_pages")
		private final long totalPages;
		@JsonIgnore
		private final List<T> originalItems;
		private List<JsonNode> items;

		public PaginatedQueryResult(int page, int limit, long totalItems, long totalPages, List<T> items) {
			this.page = page;
			this.limit = limit;
			this.totalItems = totalItems;
			this.totalPages = totalPages;
			this.originalItems = items;
		}

		public PaginatedQueryResult<T> map(Function<T, JsonNode> modifier) {
			this.items = originalItems.stream().map(modifier::apply).collect(Collectors.toList());
			return this;
		}

		public HTTPResponse response() {
			if (this.items == null)
				this.items = originalItems.stream().map(JSON::toJSON).collect(Collectors.toList());

			return HTTPResponse.OK
					.message("Query executed successfully.")
					.data(new ObjectBuilder()
							.with("page", page)
							.with("limit", limit)
							.with("total_items", totalItems)
							.with("total_pages", totalPages)
							.with("items", items)
							.build());
		}

		public List<T> getOriginalItems() {
			return originalItems;
		}

		public List<JsonNode> getItems() {
			return items;
		}
	}
}
