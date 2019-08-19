package com.weibinhwb.mycamera;

/**
 * Created by weibin on 2019/8/19
 */


public class YuvHelper {

    static {
        System.loadLibrary("native-lib");
    }

    public native String function();

    public native void rotateYUVDegree90(byte[] input, byte[] output, int width, int height);

    public static native void nv21ToI420(byte[] nv21Bytes, byte[] i420Bytes, int width, int height);

    public static native void i420Rotate(byte[] srcI420, byte[] desI420, int width, int height, int degree);
}
