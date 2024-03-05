package dev.petshopsoftware.utilities.Util.InputChecker;

public class StringChecker extends InputChecker<String> {
	public StringChecker(String input) {
		super(input);
	}

	public StringChecker required(String message) {
		return (StringChecker) check(input -> input != null && !input.isEmpty(), message);
	}

	public StringChecker min(int length, String message) {
		return (StringChecker) check(input -> input.length() >= length, message);
	}

	public StringChecker max(int length, String message) {
		return (StringChecker) check(input -> input.length() <= length, message);
	}

	public StringChecker size(int min, int max, String message) {
		return (StringChecker) check(input -> input.length() >= min && input.length() <= max, message);
	}

	public StringChecker regex(String exp, String message) {
		if (!exp.startsWith("^")) exp = "^" + exp;
		if (!exp.endsWith("$")) exp = exp + "$";
		String finalExp = exp;
		return (StringChecker) check(input -> input.matches(finalExp), message);
	}
}
