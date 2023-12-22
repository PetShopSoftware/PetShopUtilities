package dev.petshopsoftware.utilities.Responses;

import dev.petshopsoftware.utilities.HTTP.Request.Request;
import dev.petshopsoftware.utilities.HTTP.Request.Response;

public class LoginResponse extends Response {
    //Define custom fields for quick getter/setter access
    String refreshToken;

    public LoginResponse(Request request) {
        super(request);
    }

    public LoginResponse(Response response){
        super(response.getRequest());
        //Can set response properties in constructor
        this.refreshToken = this.jsonBody().get("refresh_token").asText();
    }

    //Method to get a field set by constructor
    public String getRefreshToken() {
        return refreshToken;
    }

    //Or instead can read directly on the method
    /*public String getRefreshToken() {
        return this.jsonBody().get("refresh_token").asText();
    }*/

}
