package dev.petshopsoftware.utilities.Util.InputChecker;

public class DoubleChecker extends InputChecker<Double> {
	public DoubleChecker(Double input) {
		super(input);
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
