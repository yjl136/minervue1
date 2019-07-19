#include <jni.h>
#include <android/log.h>
#include <libavformat/avformat.h>
#include <libavformat/url.h>

#define  LOG_TAG    "RAMY-MP4Muxer"
#define  LOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

static void mylog(void * avcl, int level, const char* format, va_list vl) {
  __android_log_vprint(ANDROID_LOG_DEBUG, LOG_TAG, format, vl);
}

jint Java_com_ramy_minervue_ffmpeg_MP4Muxer_create(JNIEnv* env, jobject thiz, jstring filePath) {
  av_log_set_level(AV_LOG_DEBUG);
  av_log_set_callback(mylog);
  AVFormatContext* fc;
  // Initialize the library.
  av_register_all();
  avcodec_register_all();
  // Localize file path.
  const char* path = (*env)->GetStringUTFChars(env, filePath, 0);
  // Allocate context with guess from the file name.
  avformat_alloc_output_context2(&fc, NULL, NULL, path);
  if (!fc) {
    LOGE("Fail to open context.");
  }
  // Open file.
  if (avio_open2(&fc->pb, path, AVIO_FLAG_WRITE, NULL, NULL) < 0) {
    LOGE("Fail to open file.");
  }
  (*env)->ReleaseStringUTFChars(env, filePath, path);
  return (int) fc;
}

jint Java_com_ramy_minervue_ffmpeg_MP4Muxer_addVideo(JNIEnv* env, jobject thiz, jint context, jint base, jint width, jint height, jobject buffer, jint offset, jint size) {
  AVFormatContext* fc = (AVFormatContext*) context;
  AVCodec* codec = avcodec_find_encoder(fc->oformat->video_codec);
  AVStream* st = avformat_new_stream(fc, codec);
  st->id = fc->nb_streams - 1;
  AVCodecContext* cc = st->codec;
  avcodec_get_context_defaults3(cc, codec);
  cc->codec_id = fc->oformat->video_codec;
  cc->bit_rate = base / 100 * width * height / 15;
  cc->width = width;
  cc->height = height;
  cc->time_base.den = base;
  cc->time_base.num = 1;
  cc->pix_fmt = PIX_FMT_YUV420P;
  char* extra = (*env)->GetDirectBufferAddress(env, buffer) + offset;
  char* p = av_malloc(size);
  memcpy(p, extra, size);
  cc->extradata = p;
  cc->extradata_size = size;
  if (fc->oformat->flags & AVFMT_GLOBALHEADER) {
    cc->flags |= CODEC_FLAG_GLOBAL_HEADER;
  }
  return fc->nb_streams - 1;
}

jint Java_com_ramy_minervue_ffmpeg_MP4Muxer_addAudio(JNIEnv* env, jobject thiz, jint context, jobject buffer, jint offset, jint size) {
  AVFormatContext* fc = (AVFormatContext*) context;
  AVCodec* codec = avcodec_find_encoder(fc->oformat->audio_codec);
  AVStream* st = avformat_new_stream(fc, codec);
  st->id = 1;
  AVCodecContext* cc = st->codec;
  avcodec_get_context_defaults3(cc, codec);
  cc->codec_id = fc->oformat->audio_codec;
  cc->sample_fmt = AV_SAMPLE_FMT_S16;
  cc->bit_rate = 54 * 1024;
  cc->frame_size = 2048;
  cc->sample_rate = 44100;
  cc->channels = 1;
  char* extra = (*env)->GetDirectBufferAddress(env, buffer) + offset;
  char* p = av_malloc(size);
  memcpy(p, extra, size);
  cc->extradata = p;
  cc->extradata_size = size;
  cc->profile = FF_PROFILE_AAC_LOW;
  if (fc->oformat->flags & AVFMT_GLOBALHEADER) {
    cc->flags |= CODEC_FLAG_GLOBAL_HEADER;
  }
  return fc->nb_streams - 1;
}

void Java_com_ramy_minervue_ffmpeg_MP4Muxer_writeHeader(JNIEnv* env, jobject thiz, jint context) {
  if (avformat_write_header((AVFormatContext*) context, NULL) < 0) {
    LOGE("Fail to write header.");
  } else {
    LOGD("Start writing mp4...");
  }
}

void Java_com_ramy_minervue_ffmpeg_MP4Muxer_writeFrame(JNIEnv *env, jobject thiz, jint context, jint index, jobject buffer, jint offset, jint size, jlong pts) {
  AVFormatContext* fc = (AVFormatContext*) context;
  AVPacket packet = {0};
  AVRational br = {.num = 1, .den = 1000000};
  char* data = (*env)->GetDirectBufferAddress(env, buffer) + offset;
  av_init_packet(&packet);
  packet.data = data;
  packet.size = size;
  packet.stream_index = index;
  packet.pts = av_rescale_q(pts, br, fc->streams[index]->time_base);
  av_interleaved_write_frame(fc, &packet);
}

void Java_com_ramy_minervue_ffmpeg_MP4Muxer_release(JNIEnv* env, jobject thiz, jint context) {
  int i;
  AVFormatContext* oc = (AVFormatContext*) context;
  av_write_trailer(oc);
  avio_close(oc->pb);
  for (i = 0; i < oc->nb_streams; i++) {
    avcodec_close(oc->streams[i]->codec);
  }
  avformat_free_context(oc);
}
