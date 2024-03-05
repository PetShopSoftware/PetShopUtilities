package dev.petshopsoftware.utilities.Util.InputChecker;

public class DoubleChecker extends InputChecker<Double> {
	public DoubleChecker(Double input) {
		super(input);
	}

	public DoubleChecker(String input) {
		super(0D);
		try {
			this.input = Double.parseDouble(input);
		} catch (NumberFormatException e) {
			throw new InvalidInputException("Input must be a Double.");
		}
	}

	public DoubleChecker min(int length, String message) {
		return (DoubleChecker) check(input -> input >= length, message);
	}

	public DoubleChecker max(int length, String message) {
		return (DoubleChecker) check(input -> input <= length, message);
	}

	public DoubleChecker range(int min, int max, String message) {
		return (DoubleChecker) check(input -> input >= min && input <= max, message);
	}

	public DoubleChecker mod(int mod, String message) {
		return (DoubleChecker) check(input -> input % mod == 0, message);
	}
}
