package dev.petshopsoftware.utilities.Util.InputChecker;

public class LongChecker extends InputChecker<Long> {
	public LongChecker(Long input) {
		super(input);
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
