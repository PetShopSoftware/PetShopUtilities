package dev.petshopsoftware.utilities.Util.InputChecker;

public class IntegerChecker extends InputChecker<Integer> {
	public IntegerChecker(Integer input) {
		super(input);
	}

	public IntegerChecker(String input) {
		super(0);
		try {
			this.input = Integer.parseInt(input);
		} catch (NumberFormatException e) {
			throw new InvalidInputException("Input must be an Integer.");
		}
	}

	public IntegerChecker min(int length) {
		return (IntegerChecker) check(input -> input >= length);
	}

	public IntegerChecker max(int length) {
		return (IntegerChecker) check(input -> input <= length);
	}

	public IntegerChecker range(int min, int max) {
		return (IntegerChecker) check(input -> input >= min && input <= max);
	}

	public IntegerChecker mod(int mod) {
		return (IntegerChecker) check(input -> input % mod == 0);
	}
}
