package dev.petshopsoftware.utilities.Requests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.petshopsoftware.utilities.HTTP.Request.Request;
import dev.petshopsoftware.utilities.HTTP.Server.HTTPMethod;
import dev.petshopsoftware.utilities.JSON.ObjectBuilder;

public class LoginRequest extends Request {
    //Can set static fields about the request such as url, method, etc
    private static final String url = "theurl";
    private static final HTTPMethod method = HTTPMethod.POST;
    private final ObjectNode body;

    //Constructor that take in the whole JSON body
    public LoginRequest(String proxy, ObjectNode loginData) {
        super(url, proxy);
        this.method(method);
        this.body = loginData;
        super.body(this.body);
    }

    //Constructor that takes in per parameter
    public LoginRequest(String proxy, String username, String password) {
        super(url, proxy);
        this.method(method);
        this.body = new ObjectBuilder()
                .with("username", username)
                .with("password", password)
                .build();
        super.body(this.body);
    }

    //Set individual properties, then update using super.body()
    public void setUsername(String username){
        this.body.put("username", username);
        super.body(this.body);
    }


}
