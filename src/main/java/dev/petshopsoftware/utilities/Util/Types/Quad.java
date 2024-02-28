package dev.petshopsoftware.utilities.Util.Types;

public class Quad<V1, V2, V3, V4> {
	private final V1 v1;
	private final V2 v2;
	private final V3 v3;
	private final V4 v4;

	public Quad(V1 v1, V2 v2, V3 v3, V4 v4) {
		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;
		this.v4 = v4;
	}

	public V1 getV1() {
		return v1;
	}

	public V2 getV2() {
		return v2;
	}

	public V3 getV3() {
		return v3;
	}

	public V4 getV4() {
		return v4;
	}
}
