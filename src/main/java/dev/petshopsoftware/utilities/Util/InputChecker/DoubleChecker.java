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

	public DoubleChecker min(int length) {
		return (DoubleChecker) check(input -> input >= length);
	}

	public DoubleChecker max(int length) {
		return (DoubleChecker) check(input -> input <= length);
	}

	public DoubleChecker range(int min, int max) {
		return (DoubleChecker) check(input -> input >= min && input <= max);
	}

	public DoubleChecker mod(int mod) {
		return (DoubleChecker) check(input -> input % mod == 0);
	}
}
