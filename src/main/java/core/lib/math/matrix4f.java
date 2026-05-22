package core.lib.math;

public class matrix4f {
    private final float[] m = new float[16];

    public matrix4f() {
        identity();
    }

    public matrix4f identity() {
        m[0]  = 1.0f; m[1]  = 0.0f; m[2]  = 0.0f; m[3]  = 0.0f;
        m[4]  = 0.0f; m[5]  = 1.0f; m[6]  = 0.0f; m[7]  = 0.0f;
        m[8]  = 0.0f; m[9]  = 0.0f; m[10] = 1.0f; m[11] = 0.0f;
        m[12] = 0.0f; m[13] = 0.0f; m[14] = 0.0f; m[15] = 1.0f;
        return this;
    }

    public matrix4f translate(float x, float y, float z) {
        matrix4f result = new matrix4f();
        result.m[0]  = m[0];  result.m[1]  = m[1];  result.m[2]  = m[2];  result.m[3]  = m[3];
        result.m[4]  = m[4];  result.m[5]  = m[5];  result.m[6]  = m[6];  result.m[7]  = m[7];
        result.m[8]  = m[8];  result.m[9]  = m[9];  result.m[10] = m[10]; result.m[11] = m[11];
        result.m[12] = m[0] * x + m[4] * y + m[8]  * z + m[12];
        result.m[13] = m[1] * x + m[5] * y + m[9]  * z + m[13];
        result.m[14] = m[2] * x + m[6] * y + m[10] * z + m[14];
        result.m[15] = m[3] * x + m[7] * y + m[11] * z + m[15];
        System.arraycopy(result.m, 0, this.m, 0, 16);
        return this;
    }

    public matrix4f translate(vector3f v) {
        return translate(v.x, v.y, v.z);
    }

    public matrix4f rotateZ(float radians) {
        float c = (float) Math.cos(radians);
        float s = (float) Math.sin(radians);
        matrix4f result = new matrix4f();

        result.m[0]  = m[0] * c + m[4] * s;
        result.m[1]  = m[1] * c + m[5] * s;
        result.m[2]  = m[2] * c + m[6] * s;
        result.m[3]  = m[3] * c + m[7] * s;

        result.m[4]  = m[0] * -s + m[4] * c;
        result.m[5]  = m[1] * -s + m[5] * c;
        result.m[6]  = m[2] * -s + m[6] * c;
        result.m[7]  = m[3] * -s + m[7] * c;

        result.m[8]  = m[8];  result.m[9]  = m[9];  result.m[10] = m[10]; result.m[11] = m[11];
        result.m[12] = m[12]; result.m[13] = m[13]; result.m[14] = m[14]; result.m[15] = m[15];

        System.arraycopy(result.m, 0, this.m, 0, 16);
        return this;
    }

    public matrix4f rotateXYZ(float rx, float ry, float rz) {
        rotateZ(rz);
        return this;
    }

    public matrix4f scale(float x, float y, float z) {
        m[0]  *= x; m[1]  *= x; m[2]  *= x; m[3]  *= x;
        m[4]  *= y; m[5]  *= y; m[6]  *= y; m[7]  *= y;
        m[8]  *= z; m[9]  *= z; m[10] *= z; m[11] *= z;
        return this;
    }

    public matrix4f scale(vector3f v) {
        return scale(v.x, v.y, v.z);
    }

    public matrix4f ortho(float left, float right, float bottom, float top, float near, float far) {
        identity();
        m[0]  = 2.0f / (right - left);
        m[5]  = 2.0f / (top - bottom);
        m[10] = -2.0f / (far - near);
        m[12] = -(right + left) / (right - left);
        m[13] = -(top + bottom) / (top - bottom);
        m[14] = -(far + near) / (far - near);
        return this;
    }

    public matrix4f mul(matrix4f other) {
        matrix4f result = new matrix4f();
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                result.m[row * 4 + col] =
                        this.m[row * 4 + 0] * other.m[0 * 4 + col] +
                                this.m[row * 4 + 1] * other.m[1 * 4 + col] +
                                this.m[row * 4 + 2] * other.m[2 * 4 + col] +
                                this.m[row * 4 + 3] * other.m[3 * 4 + col];
            }
        }
        System.arraycopy(result.m, 0, this.m, 0, 16);
        return this;
    }

    public matrix4f mul(matrix4f other, matrix4f dest) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                dest.m[row * 4 + col] =
                        this.m[row * 4 + 0] * other.m[0 * 4 + col] +
                                this.m[row * 4 + 1] * other.m[1 * 4 + col] +
                                this.m[row * 4 + 2] * other.m[2 * 4 + col] +
                                this.m[row * 4 + 3] * other.m[3 * 4 + col];
            }
        }
        return dest;
    }

    public float[] get(float[] dest) {
        System.arraycopy(m, 0, dest, 0, 16);
        return dest;
    }

    public float[] getArray() {
        float[] copy = new float[16];
        System.arraycopy(m, 0, copy, 0, 16);
        return copy;
    }

    public float get(int row, int col) {
        return m[row * 4 + col];
    }

    public void set(int row, int col, float value) {
        m[row * 4 + col] = value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sb.append(String.format("[%.3f, %.3f, %.3f, %.3f]\n",
                    m[i * 4], m[i * 4 + 1], m[i * 4 + 2], m[i * 4 + 3]));
        }
        return sb.toString();
    }
}