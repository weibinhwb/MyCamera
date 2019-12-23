//
// Created by weibin on 2019/8/19.
//
#include <jni.h>
#include <string>
#include <libyuv/rotate.h>
#include <libyuv.h>


extern "C" JNIEXPORT jstring JNICALL
Java_com_weibinhwb_mycamera_YuvHelper_function(JNIEnv *env, jobject instance) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


extern "C"
JNIEXPORT void JNICALL
Java_com_weibinhwb_mycamera_YuvHelper_rotateYUVDegree90(JNIEnv *env, jobject instance, jbyteArray input_,
                                                        jbyteArray output_, jint width, jint height) {
    jbyte *input = env->GetByteArrayElements(input_, NULL);
    jbyte *output = env->GetByteArrayElements(output_, NULL);


    env->ReleaseByteArrayElements(input_, input, 0);
    env->ReleaseByteArrayElements(output_, output, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_weibinhwb_mycamera_YuvHelper_nv21ToI420(JNIEnv *env, jclass type,
                                                 jbyteArray nv21Bytes_,
                                                 jbyteArray i420Bytes_,
                                                 jint width, jint height) {
    jbyte *nv21Bytes = env->GetByteArrayElements(nv21Bytes_, NULL);
    jbyte *i420Bytes = env->GetByteArrayElements(i420Bytes_, NULL);

    /*
     * int NV21ToI420(const uint8* src_y, int src_stride_y,
               const uint8* src_vu, int src_stride_vu,
               uint8* dst_y, int dst_stride_y,
               uint8* dst_u, int dst_stride_u,
               uint8* dst_v, int dst_stride_v,
               int width, int height);
     * */
    jint src_y_size = width * height;
    jint src_u_size = (width >> 1) * (height >> 1);

    jbyte *nv21_y_data = nv21Bytes;
    jbyte *nv21_vu_data = nv21Bytes + src_y_size;

    jbyte *i420_y_data = i420Bytes;
    jbyte *i420_u_data = i420Bytes + src_y_size;
    jbyte *i420_v_data = i420Bytes + src_y_size + src_u_size;

    libyuv::NV21ToI420((const uint8 *) nv21_y_data, width,
                       (const uint8 *) nv21_vu_data, width,
                       (uint8 *) i420_y_data, width,
                       (uint8 *) i420_u_data, (width >> 1),
                       (uint8 *) i420_v_data, (width >> 1),
                       width, height);

    env->ReleaseByteArrayElements(nv21Bytes_, nv21Bytes, 0);
    env->ReleaseByteArrayElements(i420Bytes_, i420Bytes, 0);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_weibinhwb_mycamera_YuvHelper_i420Rotate(JNIEnv *env, jclass type,
                                                 jbyteArray srcI420,
                                                 jbyteArray desI420,
                                                 jint width, jint height, jint degree) {
    jbyte *srcBytes = env->GetByteArrayElements(srcI420, NULL);
    jbyte *desBytes = env->GetByteArrayElements(desI420, NULL);

    jint src_y_size = width * height;
    jint src_u_size = (width >> 1) * (height >> 1);

    jbyte *src_y_data = srcBytes;
    jbyte *src_u_data = srcBytes + src_y_size;
    jbyte *src_v_data = srcBytes + src_y_size + src_u_size;

    jbyte *des_y_data = desBytes;
    jbyte *des_u_data = desBytes + src_y_size;
    jbyte *des_v_data = desBytes + src_y_size + src_u_size;

    if (degree == libyuv::kRotate90 || degree == libyuv::kRotate270) {
        libyuv::I420Rotate((const uint8 *) src_y_data, width,
                           (const uint8 *) src_u_data, width >> 1,
                           (const uint8 *) src_v_data, width >> 1,
                           (uint8 *) des_y_data, height,
                           (uint8 *) des_u_data, height >> 1,
                           (uint8 *) des_v_data, height >> 1,
                           width, height,
                           (libyuv::RotationMode) degree);
    }

    env->ReleaseByteArrayElements(srcI420, srcBytes, 0);
    env->ReleaseByteArrayElements(desI420, desBytes, 0);
}