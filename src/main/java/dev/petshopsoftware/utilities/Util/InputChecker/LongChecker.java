package dev.petshopsoftware.utilities.Util.InputChecker;

public class LongChecker extends InputChecker<Long> {
	public LongChecker(Long input) {
		super(input);
	}

	public LongChecker(String input) {
		super(0L);
		try {
			this.input = Long.parseLong(input);
		} catch (NumberFormatException e) {
			throw new InvalidInputException("Input must be a Long.");
		}
	}

	public LongChecker min(int length, String message) {
		return (LongChecker) check(input -> input >= length, message);
	}

	public LongChecker max(int length, String message) {
		return (LongChecker) check(input -> input <= length, message);
	}

	public LongChecker range(int min, int max, String message) {
		return (LongChecker) check(input -> input >= min && input <= max, message);
	}

	public LongChecker mod(int mod, String message) {
		return (LongChecker) check(input -> input % mod == 0, message);
	}
}
