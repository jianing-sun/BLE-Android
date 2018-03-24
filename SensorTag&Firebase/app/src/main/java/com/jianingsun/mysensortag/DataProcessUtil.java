package com.jianingsun.mysensortag;

/**
 * Created by jianingsun on 2018-03-09.
 */

public class DataProcessUtil {

    public DataProcessUtil() {
    }

    public static double sum(double[] in) {
        double out = 0;
        for (int i=0; i<in.length; i++) {
            out += in[i];
        }
        return out;
    }

    public static double norm(double[] in) {
        double out = 0;
        for (int i=0; i<in.length; i++) {
            out += in[i] * in[i];
        }
        return (double)Math.sqrt(out);
    }

    public static double dot3(double[] in1, double[] in2) {
        double out = in1[0] * in2[0] + in1[1] * in2[1] + in1[2] * in2[2];
        return out;
    }

    public static double[] normalize(double[] in) {
        double[] out = new double[in.length];
        double n = norm(in);
        for (int i=0; i<in.length; i++) {
            out[i] = in[i] / n;
        }
        return out;
    }
}
