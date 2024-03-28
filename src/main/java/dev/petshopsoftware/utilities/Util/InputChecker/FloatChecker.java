package dev.petshopsoftware.utilities.Util.InputChecker;

public class FloatChecker extends InputChecker<Float> {
	public FloatChecker(Float input) {
		super(input);
	}

	public FloatChecker(String input) {
		super(0F);
		try {
			this.input = Float.parseFloat(input);
		} catch (NumberFormatException e) {
			throw new InvalidInputException("Input must be a Float.");
		}
	}

	public FloatChecker min(float length, String message) {
		return (FloatChecker) check(input -> input >= length, message);
	}

	public FloatChecker max(float length, String message) {
		return (FloatChecker) check(input -> input <= length, message);
	}

	public FloatChecker range(float min, float max, String message) {
		return (FloatChecker) check(input -> input >= min && input <= max, message);
	}

	public FloatChecker mod(float mod, String message) {
		return (FloatChecker) check(input -> input % mod == 0, message);
	}
}
