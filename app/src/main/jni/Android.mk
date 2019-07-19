LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := x264-prebuilt
LOCAL_SRC_FILES := libx264.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../../../../android-ffmpeg/x264
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := avcodec-prebuilt
LOCAL_SRC_FILES := libavcodec.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../../../../android-ffmpeg/ffmpeg
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := avformat-prebuilt
LOCAL_SRC_FILES := libavformat.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../../../../android-ffmpeg/ffmpeg
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := swscale-prebuilt
LOCAL_SRC_FILES := libswscale.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../../../../android-ffmpeg/ffmpeg
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := avutil-prebuilt
LOCAL_SRC_FILES := libavutil.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../../../../android-ffmpeg/ffmpeg
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := ramymedia
LOCAL_SRC_FILES := mp4muxer.c
LOCAL_STATIC_LIBRARIES := \
	swscale-prebuilt \
	avformat-prebuilt \
	avcodec-prebuilt \
	avutil-prebuilt \
	x264-prebuilt
LOCAL_LDLIBS := -llog -lz
include $(BUILD_SHARED_LIBRARY)
