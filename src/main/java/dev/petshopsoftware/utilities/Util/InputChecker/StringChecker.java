package dev.petshopsoftware.utilities.Util.InputChecker;

public class StringChecker extends InputChecker<String> {
	public StringChecker(String input) {
		super(input);
	}

	public StringChecker min(int length) {
		return (StringChecker) check(input -> input.length() >= length);
	}

	public StringChecker max(int length) {
		return (StringChecker) check(input -> input.length() <= length);
	}

	public StringChecker size(int min, int max) {
		return (StringChecker) check(input -> input.length() >= min && input.length() <= max);
	}

	public StringChecker regex(String exp) {
		if (!exp.startsWith("^")) exp = "^" + exp;
		if (!exp.endsWith("$")) exp = exp + "$";
		String finalExp = exp;
		return (StringChecker) check(input -> input.matches(finalExp));
	}
}
