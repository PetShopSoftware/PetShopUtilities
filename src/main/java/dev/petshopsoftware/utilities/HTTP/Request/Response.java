package dev.petshopsoftware.utilities.HTTP.Request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import dev.petshopsoftware.utilities.JSON.JSON;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class Response {
    private final Request request;
    private final int statusCode;
    private final String statusMessage;
    private final byte[] rawBody;

    public Response(Request request) {
        this.request = request;
        try {
            statusCode = request.getConnection().getResponseCode();
            statusMessage = request.getConnection().getResponseMessage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.rawBody = rawBody();
    }

    public int statusCode() {
        return this.statusCode;
    }

    public byte[] rawBody() {
        byte[] responseBytes;
        InputStream in;
        try {
            in = request.getConnection().getInputStream();
        } catch (IOException exception) {
            in = request.getConnection().getErrorStream();
        }

        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            int nRead;
            byte[] data = new byte[1024];

            while ((nRead = in.read(data, 0, data.length)) != -1)
                buffer.write(data, 0, nRead);

            buffer.flush();
            responseBytes = buffer.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return responseBytes;
    }

    public JsonNode jsonBody() {
        try {
            return JSON.MAPPER.readTree(body().trim());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String body() {
        return new String(rawBody);
    }

    @Override
    public String toString() {
        StringBuilder message = new StringBuilder();
        message.append(request.getConnection().getRequestMethod()).append(" ").append(request.getConnection().getURL().toString()).append("\n");
//            message.append("Request:").append("\n");
//            connection.getRequestProperties().forEach((key, values) ->
//                    values.forEach(value -> message.append("  ").append(key).append(": ").append(value).append("\n")));
//            try {
//                String prettyBody = JSON.MAPPER.readTree(request.lastBody).toPrettyString();
//                for (String line : prettyBody.split("\n"))
//                    message.append("  ").append(line).append("\n");
//            } catch (JsonProcessingException e) {
//                message.append("  ").append(request.lastBody);
//            }
//            message.append("Response:").append("\n");
        message.append("  ").append(statusCode).append(" ").append(statusMessage).append("\n");
        request.getConnection().getHeaderFields().forEach((key, values) ->
                values.forEach(value -> message.append("  ").append(key).append(": ").append(value).append("\n")));
        try {
            String prettyBody = jsonBody().toPrettyString();
            for (String line : prettyBody.split("\n"))
                message.append("  ").append(line).append("\n");
        } catch (RuntimeException e) {
            message.append("  ").append(body());
        }
        return message.toString();
    }


    //Necessary to be able to use the casting constructor, check LoginResponse for example
    public Request getRequest() {
        return request;
    }

    public static <T extends Response> T fromRequest(Response response, Class<T> responseClass){
        try {
            Constructor<T> constructor = responseClass.getConstructor(response.getClass());
            return constructor.newInstance(response);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
            //Add logging to default logger, something like: Response class not found?
        }

    }
}
