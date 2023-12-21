package dev.petshopsoftware.utilities;

import dev.petshopsoftware.utilities.HTTP.Server.*;
import dev.petshopsoftware.utilities.JSON.ArrayBuilder;

public class PetShopUtilities {
	public static void main(String[] args) {
		new HTTPServer("api", "gigtoolbox.com", 8000)
				.handlers(PetShopUtilities.class)
				.start();
	}

	@Route(path = "/posts", method = HTTPMethod.GET)
	public static HTTPResponse getPosts(HTTPData data) {
		return HTTPResponse.OK
				.message("Posts fetched successfully.")
				.data(new ArrayBuilder().build());
	}

	@Route(path = "/posts/:id", method = HTTPMethod.GET)
	public static HTTPResponse getPost(HTTPData data) throws Exception {
		return HTTPResponse.OK
				.message("Post'" + data.pathParams().get("id") + "'fetched successfully.")
				.data(new ArrayBuilder().build());
	}
}
