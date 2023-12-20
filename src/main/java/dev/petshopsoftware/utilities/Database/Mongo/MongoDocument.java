package dev.petshopsoftware.utilities.Database.Mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import dev.petshopsoftware.utilities.Database.DocumentReadException;
import dev.petshopsoftware.utilities.Database.DocumentWriteException;
import dev.petshopsoftware.utilities.JSON.JSON;
import org.bson.Document;

import java.util.concurrent.atomic.AtomicBoolean;

public interface MongoDocument extends JSON {
    default Document toDocument(){
        return Document.parse(toJSON().toString());
    }

    default JSON fromDocument(Document document) {
        return fromString(document.toJson());
    }

    default MongoInfo getMongoInfo(){
        if(!getClass().isAnnotationPresent(MongoInfo.class))
            throw new UnsupportedOperationException("This class is missing MongoInfo annotation.");

        return getClass().getAnnotation(MongoInfo.class);
    }

    default void save(MongoConnection mongoConnection) {
        String identifierField = getMongoInfo().identifier();
        Document document = toDocument();
        String id = document.get(identifierField).toString();

        AtomicBoolean saveError = new AtomicBoolean(false);

        MongoCollection<Document> collection = mongoConnection.getCollection(getClass());
        Document replaceResult = collection.findOneAndReplace(new Document(identifierField, id), document);
        if(replaceResult == null){
            InsertOneResult insertResult;
            try{
                insertResult = collection.insertOne(document);
            }catch (RuntimeException e){
                throw new DocumentWriteException(e);
            }
            if(insertResult.getInsertedId() == null)
                throw new DocumentWriteException("Failed saving Object to database.");
        }
    }

    default void save() {
        save(MongoConnection.DEFAULT_CONNECTION);
    }

    default JSON load(MongoConnection mongoConnection, String idField, String id) {
        MongoCollection<Document> collection = mongoConnection.getCollection(getClass());
        Document result = collection.find(new Document().append(idField, id)).first();
        if(result != null)
            return fromDocument(result);

        throw new DocumentReadException("Failed loading Object from database.");
    }

    default JSON load(String idField, String id) {
        return load(MongoConnection.DEFAULT_CONNECTION, idField, id);
    }


    default JSON load(MongoConnection mongoConnection, String id) {
        return load(mongoConnection, getMongoInfo().identifier(), id);
    }

    default JSON load(String id){
        return load(MongoConnection.DEFAULT_CONNECTION, id);
    }

    default void delete(MongoConnection mongoConnection){
        String identifierField = getMongoInfo().identifier();
        Document document = toDocument();
        String id = document.get(identifierField).toString();

        AtomicBoolean deleteError = new AtomicBoolean(false);
        MongoCollection<Document> collection = mongoConnection.getCollection(getClass());
        DeleteResult deleteResult = collection.deleteOne(new Document(identifierField, id));
        if (deleteResult.getDeletedCount() == 0)
            throw new DocumentWriteException("Failed deleting Object from database.");
    }

    default void delete(){
        delete(MongoConnection.DEFAULT_CONNECTION);
    }
}
