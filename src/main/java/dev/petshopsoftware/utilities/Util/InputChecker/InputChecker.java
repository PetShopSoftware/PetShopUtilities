package dev.petshopsoftware.utilities.Util.InputChecker;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class InputChecker<T> {
	protected final Map<Function<T, Boolean>, String> checks = new HashMap<>();
	protected T input;

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

	public T matches() throws InvalidInputException {
		int i = 1;
		for (Map.Entry<Function<T, Boolean>, String> check : checks.entrySet()) {
			if (!check.getKey().apply(input))
				throw new InvalidInputException(
						check.getValue()
								.replace("%i%", String.valueOf(i))
				);
			i++;
		}
		return input;
	}
}
