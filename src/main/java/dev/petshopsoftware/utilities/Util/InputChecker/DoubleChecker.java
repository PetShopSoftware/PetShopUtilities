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

	public DoubleChecker min(double length, String message) {
		return (DoubleChecker) check(input -> input >= length, message);
	}

	public DoubleChecker max(double length, String message) {
		return (DoubleChecker) check(input -> input <= length, message);
	}

	public DoubleChecker range(double min, double max, String message) {
		return (DoubleChecker) check(input -> input >= min && input <= max, message);
	}

	public DoubleChecker mod(double mod, String message) {
		return (DoubleChecker) check(input -> input % mod == 0, message);
	}
}
