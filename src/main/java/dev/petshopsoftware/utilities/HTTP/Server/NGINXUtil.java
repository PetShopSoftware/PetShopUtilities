package dev.petshopsoftware.utilities.HTTP.Server;

import dev.petshopsoftware.utilities.Util.ConfigUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NGINXUtil {
	public static String NGINX_FOLDER = "/etc/nginx";
	public static String CERTIFICATES_PATH = "/etc/petshopsoftware/certificates";
	public static String EMAIL = "email@example.com";

	public static void setupServerBlock(String subdomain, String domain, int port) throws IOException {
		if (!isNGINXInstalled())
			throw new UnsupportedOperationException("NGINX could not be found in the system.");

		String sslPath;
		try {
			sslPath = setupSSLCertificate(subdomain, domain, EMAIL);
		} catch (Exception e) {
			throw new UnsupportedOperationException(e);
		}

		String serverBlockFileName = subdomain + "." + domain;
		Path availablePath = Paths.get(NGINX_FOLDER + "/sites-available/" + serverBlockFileName);
		Path enabledPath = Paths.get(NGINX_FOLDER + "/sites-enabled/" + serverBlockFileName);
		boolean reload = false;
		String serverBlockConfig = buildServerBlock(subdomain, domain, sslPath, port);
		if (!Files.exists(availablePath) || !Files.readString(availablePath).trim().equals(serverBlockConfig.trim())) {
			Files.write(availablePath, serverBlockConfig.getBytes());
			reload = true;
		}

		if (!Files.exists(enabledPath) || !Files.isSymbolicLink(enabledPath)) {
			Files.createSymbolicLink(enabledPath, availablePath);
			reload = true;
		}

		if (reload) {
			if (isNGINXRunning()) reloadNGINX();
			else startNGINX();
		}

		if (!isNGINXRunning()) throw new IllegalStateException("NGINX was not started properly.");
	}

	public static String setupSSLCertificate(String subdomain, String domain, String email) {
		if (!isCertBotInstalled())
			throw new UnsupportedOperationException("CertBot could not be found in the system.");
		String fullDomain = subdomain + "." + domain;
		String certificatePath = CERTIFICATES_PATH + "/" + fullDomain;
		try {
			Process process = new ProcessBuilder().command("sudo", "certbot", "certonly", "--webroot", "-w", certificatePath, "-d", fullDomain, "--non-interactive", "--agree-tos", "--email", email).start();
			int exitCode = process.waitFor();
			if (exitCode != 0) throw new IllegalStateException("CertBot exited with code " + exitCode);
		} catch (IOException | InterruptedException | IllegalStateException e) {
			throw new IllegalStateException("Failed to obtain certificate for " + fullDomain, e);
		}
		return certificatePath;
	}

	public static String buildServerBlock(String subdomain, String domain, String sslPath, int port) {
		return ConfigUtil.readFileFromResources("nginx/server_block")
				.replace("%subdomain%", subdomain)
				.replace("%domain%", domain)
				.replace("%ssl_path%", sslPath)
				.replace("%port%", String.valueOf(port));
	}

	public static void reloadNGINX() throws IOException {
		try {
			new ProcessBuilder().command("sudo nginx -s reload").start().waitFor();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static void startNGINX() {
		try {
			new ProcessBuilder("sudo", "systemctl", "start", "nginx").start().waitFor();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isNGINXInstalled() {
		try {
			Process process = new ProcessBuilder().command("nginx -v").start();
			return process.waitFor() == 0;
		} catch (IOException | InterruptedException e) {
			return false;
		}
	}

	public static boolean isNGINXRunning() {
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.command("bash", "-c", "systemctl is-active nginx");
		try {
			Process process = processBuilder.start();
			boolean isRunning;
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line = reader.readLine();
				isRunning = line.equals("active");
			}
			int exitCode = process.waitFor();
			return isRunning && (exitCode == 0);
		} catch (IOException | InterruptedException e) {
			return false;
		}
	}

	public static boolean isCertBotInstalled() {
		try {
			Process process = new ProcessBuilder().command("bash", "-c", "command -v certbot").start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = reader.readLine();
			return line != null && line.contains("certbot");
		} catch (IOException e) {
			return false;
		}
	}
}
