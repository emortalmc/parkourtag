package dev.emortal.minestom.parkourtag.utils;

import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

public final class PTQuaternion {

	private double x;
	private double y;
	private double z;
	private double w;

	public PTQuaternion(@NotNull PTQuaternion q) {
		this(q.x, q.y, q.z, q.w);
	}

	public PTQuaternion(double x, double y, double z, double w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	public void set(@NotNull PTQuaternion q) {
		this.x = q.x;
		this.y = q.y;
		this.z = q.z;
		this.w = q.w;
	}

	public PTQuaternion(@NotNull Vec axis, double angle) {
		this.set(axis, angle);
	}

	public double norm() {
		return Math.sqrt(this.dot(this));
	}

	public double getW() {
		return this.w;
	}

	public double getX() {
		return this.x;
	}

	public double getY() {
		return this.y;
	}

	public double getZ() {
		return this.z;
	}

	/**
	 * @param axis
	 *            rotation axis, unit vector
	 * @param angle
	 *            the rotation angle
	 * @return this
	 */
	public @NotNull PTQuaternion set(@NotNull Vec axis, double angle) {
		double s = Math.sin(angle / 2);
		this.w = Math.cos(angle / 2);
		this.x = axis.x() * s;
		this.y = axis.y() * s;
		this.z = axis.z() * s;
		return this;
	}

	public @NotNull PTQuaternion mulThis(@NotNull PTQuaternion q) {
		double nw = this.w * q.w - this.x * q.x - this.y * q.y - this.z * q.z;
		double nx = this.w * q.x + this.x * q.w + this.y * q.z - this.z * q.y;
		double ny = this.w * q.y + this.y * q.w + this.z * q.x - this.x * q.z;
		this.z = this.w * q.z + this.z * q.w + this.x * q.y - this.y * q.x;
		this.w = nw;
		this.x = nx;
		this.y = ny;
		return this;
	}

	public @NotNull PTQuaternion scaleThis(double scale) {
		if (scale != 1) {
			this.w *= scale;
			this.x *= scale;
			this.y *= scale;
			this.z *= scale;
		}
		return this;
	}

	public @NotNull PTQuaternion divThis(double scale) {
		if (scale != 1) {
			this.w /= scale;
			this.x /= scale;
			this.y /= scale;
			this.z /= scale;
		}
		return this;
	}

	public double dot(@NotNull PTQuaternion q) {
		return this.x * q.x + this.y * q.y + this.z * q.z + this.w * q.w;
	}

	public boolean equals(@NotNull PTQuaternion q) {
		return this.x == q.x && this.y == q.y && this.z == q.z && this.w == q.w;
	}

	public @NotNull PTQuaternion interpolateThis(@NotNull PTQuaternion q, double t) {
		if (!this.equals(q)) {
			double d = this.dot(q);
			double qx, qy, qz, qw;

			if (d < 0f) {
				qx = -q.x;
				qy = -q.y;
				qz = -q.z;
				qw = -q.w;
				d = -d;
			} else {
				qx = q.x;
				qy = q.y;
				qz = q.z;
				qw = q.w;
			}

			double f0, f1;

			if ((1 - d) > 0.1f) {
				double angle = Math.acos(d);
				double s = Math.sin(angle);
				double tAngle = t * angle;
				f0 = Math.sin(angle - tAngle) / s;
				f1 = Math.sin(tAngle) / s;
			} else {
				f0 = 1 - t;
				f1 = t;
			}

			this.x = f0 * this.x + f1 * qx;
			this.y = f0 * this.y + f1 * qy;
			this.z = f0 * this.z + f1 * qz;
			this.w = f0 * this.w + f1 * qw;
		}

		return this;
	}

	public @NotNull PTQuaternion normalizeThis() {
		return this.divThis(this.norm());
	}

	public @NotNull PTQuaternion interpolate(@NotNull PTQuaternion q, double t) {
		return new PTQuaternion(this).interpolateThis(q, t);
	}

	/**
	 * Converts this Quaternion into a matrix, returning it as a float array.
	 */
	public float[] toMatrix() {
		float[] matrixs = new float[16];
		this.toMatrix(matrixs);
		return matrixs;
	}

	/**
	 * Converts this Quaternion into a matrix, placing the values into the given array.
	 * @param matrixs 16-length float array.
	 */
	public void toMatrix(float[] matrixs) {
		matrixs[3] = 0.0f;
		matrixs[7] = 0.0f;
		matrixs[11] = 0.0f;
		matrixs[12] = 0.0f;
		matrixs[13] = 0.0f;
		matrixs[14] = 0.0f;
		matrixs[15] = 1.0f;

		matrixs[0] = (float) (1.0f - (2.0f * ((this.y * this.y) + (this.z * this.z))));
		matrixs[1] = (float) (2.0f * ((x * y) - (z * w)));
		matrixs[2] = (float) (2.0f * ((x * z) + (y * w)));
		
		matrixs[4] = (float) (2.0f * ((x * y) + (z * w)));
		matrixs[5] = (float) (1.0f - (2.0f * ((x * x) + (z * z))));
		matrixs[6] = (float) (2.0f * ((y * z) - (x * w)));
		
		matrixs[8] = (float) (2.0f * ((x * z) - (y * w)));
		matrixs[9] = (float) (2.0f * ((y * z) + (x * w)));
		matrixs[10] = (float) (1.0f - (2.0f * ((x * x) + (y * y))));
	}
}