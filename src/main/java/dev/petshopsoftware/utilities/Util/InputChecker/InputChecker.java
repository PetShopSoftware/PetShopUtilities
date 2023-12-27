package dev.petshopsoftware.utilities.Util.InputChecker;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class InputChecker<T> {
	private final T input;
	private final Map<Function<T, Boolean>, String> checks = new HashMap<>();

	public InputChecker(T input) {
		this.input = input;
	}

	public InputChecker<T> check(Function<T, Boolean> check, String message) {
		checks.put(check, message);
		return this;
	}

	public InputChecker<T> check(Function<T, Boolean> check) {
		return check(check, "Failed check #%i%.");
	}

	public void matches() throws InvalidParameterException {
		int i = 1;
		for (Map.Entry<Function<T, Boolean>, String> check : checks.entrySet()) {
			if (!check.getKey().apply(input))
				throw new InvalidParameterException(
						check.getValue()
								.replace("%i%", String.valueOf(i))
				);
			i++;
		}
	}
}
