#include <jni.h>
#include <opus.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

static OpusEncoder *as_encoder(jlong h) { return (OpusEncoder *)(intptr_t)h; }
static OpusDecoder *as_decoder(jlong h) { return (OpusDecoder *)(intptr_t)h; }

JNIEXPORT jlong JNICALL Java_com_haoze_nexus_audio_OpusNative_createEncoder(JNIEnv *env, jclass cls, jint rate, jint channels, jint bitrate) {
    (void)env; (void)cls;
    int err = OPUS_OK;
    OpusEncoder *e = opus_encoder_create(rate, channels, OPUS_APPLICATION_AUDIO, &err);
    if (!e || err != OPUS_OK) return 0;
    if (opus_encoder_ctl(e, OPUS_SET_BITRATE(bitrate)) != OPUS_OK ||
        opus_encoder_ctl(e, OPUS_SET_INBAND_FEC(1)) != OPUS_OK ||
        opus_encoder_ctl(e, OPUS_SET_DTX(1)) != OPUS_OK ||
        opus_encoder_ctl(e, OPUS_SET_PACKET_LOSS_PERC(5)) != OPUS_OK) { opus_encoder_destroy(e); return 0; }
    return (jlong)(intptr_t)e;
}

JNIEXPORT jbyteArray JNICALL Java_com_haoze_nexus_audio_OpusNative_encode(JNIEnv *env, jclass cls, jlong handle, jbyteArray pcm, jint frameSamples) {
    (void)cls;
    OpusEncoder *e = as_encoder(handle); if (!e || !pcm || frameSamples <= 0) return NULL;
    jsize n = (*env)->GetArrayLength(env, pcm); if (n != frameSamples * 2 * (jint)sizeof(int16_t)) return NULL;
    jbyte *in = (*env)->GetByteArrayElements(env, pcm, NULL); if (!in) return NULL;
    unsigned char out[4000]; int encoded = opus_encode(e, (const opus_int16 *)in, frameSamples, out, sizeof(out));
    (*env)->ReleaseByteArrayElements(env, pcm, in, JNI_ABORT); if (encoded < 0) return NULL;
    jbyteArray result = (*env)->NewByteArray(env, encoded); if (result) (*env)->SetByteArrayRegion(env, result, 0, encoded, (jbyte *)out); return result;
}

JNIEXPORT jboolean JNICALL Java_com_haoze_nexus_audio_OpusNative_setEncoderBitrate(JNIEnv *env, jclass cls, jlong handle, jint bitrate) { (void)env; (void)cls; return as_encoder(handle) && opus_encoder_ctl(as_encoder(handle), OPUS_SET_BITRATE(bitrate)) == OPUS_OK; }
JNIEXPORT void JNICALL Java_com_haoze_nexus_audio_OpusNative_destroyEncoder(JNIEnv *env, jclass cls, jlong handle) { (void)env; (void)cls; if (handle) opus_encoder_destroy(as_encoder(handle)); }

JNIEXPORT jlong JNICALL Java_com_haoze_nexus_audio_OpusNative_createDecoder(JNIEnv *env, jclass cls, jint rate, jint channels) { (void)env; (void)cls; int err=OPUS_OK; OpusDecoder *d=opus_decoder_create(rate, channels, &err); return (d && err==OPUS_OK) ? (jlong)(intptr_t)d : 0; }
static jbyteArray decode_common(JNIEnv *env, OpusDecoder *d, const unsigned char *data, int len, int frame) { opus_int16 pcm[5760*2]; int n=opus_decode(d, data, len, pcm, frame, 0); if(n<0) return NULL; jbyteArray out=(*env)->NewByteArray(env,n*2*2); if(out) (*env)->SetByteArrayRegion(env,out,0,n*2*2,(jbyte*)pcm); return out; }
JNIEXPORT jbyteArray JNICALL Java_com_haoze_nexus_audio_OpusNative_decode(JNIEnv *env, jclass cls, jlong handle, jbyteArray frame, jboolean fec) { (void)cls; OpusDecoder *d=as_decoder(handle); if(!d||!frame) return NULL; jsize n=(*env)->GetArrayLength(env,frame); jbyte *p=(*env)->GetByteArrayElements(env,frame,NULL); if(!p)return NULL; opus_int16 pcm[5760*2]; int decoded=opus_decode(d,(unsigned char*)p,n,pcm,960,fec?1:0); (*env)->ReleaseByteArrayElements(env,frame,p,JNI_ABORT); if(decoded<0)return NULL; jbyteArray out=(*env)->NewByteArray(env,decoded*4); if(out)(*env)->SetByteArrayRegion(env,out,0,decoded*4,(jbyte*)pcm); return out; }
JNIEXPORT jbyteArray JNICALL Java_com_haoze_nexus_audio_OpusNative_decodePlc(JNIEnv *env, jclass cls, jlong handle, jint frameSamples) { (void)cls; OpusDecoder *d=as_decoder(handle); return d ? decode_common(env,d,NULL,0,frameSamples) : NULL; }
JNIEXPORT void JNICALL Java_com_haoze_nexus_audio_OpusNative_destroyDecoder(JNIEnv *env, jclass cls, jlong handle) { (void)env; (void)cls; if(handle) opus_decoder_destroy(as_decoder(handle)); }
