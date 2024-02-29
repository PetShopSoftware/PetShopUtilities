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

	public LongChecker min(int length) {
		return (LongChecker) check(input -> input >= length);
	}

	public LongChecker max(int length) {
		return (LongChecker) check(input -> input <= length);
	}

	public LongChecker range(int min, int max) {
		return (LongChecker) check(input -> input >= min && input <= max);
	}

	public LongChecker mod(int mod) {
		return (LongChecker) check(input -> input % mod == 0);
	}
}
