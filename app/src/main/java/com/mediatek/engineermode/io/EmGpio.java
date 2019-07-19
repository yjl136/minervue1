package com.mediatek.engineermode.io;
public class EmGpio {
    public static native int getGpioMaxNumber();

    public static native boolean gpioInit();

    public static native boolean gpioUnInit();

    public static native boolean setGpioInput(int gpioIndex);

    public static native boolean setGpioOutput(int gpioIndex);

    public static native boolean setGpioDataHigh(int gpioIndex);

    public static native boolean setGpioDataLow(int gpioIndex);

    public static native int getCurrent(int hostNumber);

    public static native boolean setCurrent(int hostNumber, int currentDataIdx,
            int currentCmdIdx);

    static {
        System.loadLibrary("em_gpio_jni");

    }
}
