package dev.petshopsoftware.utilities.Util.InputChecker;

public class FloatChecker extends InputChecker<Float> {
	public FloatChecker(Float input) {
		super(input);
	}

	public FloatChecker min(int length) {
		return (FloatChecker) check(input -> input >= length);
	}

	public FloatChecker max(int length) {
		return (FloatChecker) check(input -> input <= length);
	}

	public FloatChecker range(int min, int max) {
		return (FloatChecker) check(input -> input >= min && input <= max);
	}

	public FloatChecker mod(int mod) {
		return (FloatChecker) check(input -> input % mod == 0);
	}
}