package dev.petshopsoftware.utilities;

import dev.petshopsoftware.utilities.HTTP.Server.HTTPServer;

public class PetShopUtilities {
	public static void main(String[] args) {
		new HTTPServer("api", "gigtoolbox.com", 8000)
				.handlers(PetShopUtilities.class)
				.start();
	}
}
