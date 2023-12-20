package dev.petshopsoftware.utilities;

import com.fasterxml.jackson.databind.JsonNode;
import dev.petshopsoftware.utilities.HTTP.Server.HTTPMethod;
import dev.petshopsoftware.utilities.HTTP.Server.HTTPResponse;
import dev.petshopsoftware.utilities.HTTP.Server.HTTPServer;
import dev.petshopsoftware.utilities.HTTP.Server.Route;
import dev.petshopsoftware.utilities.JSON.ArrayBuilder;

public class PetShopUtilities {
	public static void main(String[] args) {
		new HTTPServer(8000)
				.handlers(PetShopUtilities.class)
				.start();
//        try {
//            NGINXUtil.setupServerBlock("api", "gigtoolbox.com", "/etc/letsencrypt/live/api.gigtoolbox.com", 3010);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
	}

	@Route(path = "/posts", method = HTTPMethod.GET)
	public static HTTPResponse getPosts(JsonNode data) {
		return HTTPResponse.OK.message("Posts fetched successfully.").data(new ArrayBuilder().build());
	}

	@Route(path = "/posts/:id", method = HTTPMethod.GET)
	public static HTTPResponse getPost(JsonNode data) {
		return HTTPResponse.OK.message("Post'" + data.path("id").asText("NOT FOUND") + "'fetched successfully.").data(new ArrayBuilder().build());
	}
}
