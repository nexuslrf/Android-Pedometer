# Android Pedometer

## Implementation

Based on provided Android codes, we build our own pedometer app. In this app, we can directly see how acceleration changes both in scaler view and graph view. 

To obtain better performance, we directly use `sensor.TYPE_LINEAR_ACCELERATION` API, rather than doing low pass filtering. (actually `TYPE_LINEAR_ACCELERATION` has better effect than our handmade low pass filtering)

To derive the step count based the changes of acceleration, we designed the following algorithm:

1. constantly detecting the peak & valley of acceleration curve.
2. if the peak is found, and the gap between peak and valley is lager than a `accThreshold`while the time interval between 2 adjacent peaks is also large than a `timeThreshold` (here is 0.4s), then a step is detected.
3. for the peak-valley pair smaller than the `accThreshold`, we add the value of this gap to a fix-sized list for dynamically updating `accThreshold` (a simple averaging and resizing process).



For testing, the also include the Android built-in step-counting API implemented by sensor hub, an embedded chipset on SOC, for comparison. Normal walking tests shows almost no difference between 2 metrics.



## Questions

1. Why we need low pass filtering when we measure the acceleration ?
   What are the differences made by the filtering? Make a contrast.

   **Answer:** 

   a. Reason for low pass filtering: 

   * Low pass filter passes low-frequency signals and reduces the amplitude of signals with frequencies higher than the threshold frequency. 
   * We first do low pass filter to smooth the raw data curve，trying to make the filtered data mainly affected by the gravity. 
   * After the low pass filtering, we subtract the smoothed data from the raw data. In this way we can amplify the signals caused by the human relatively high frequency motions.
   * Using uniformed data could help the step calculation later.

   b. Differences:

   * Figures below show the function of low pass filtering. 

   ![compare](compare1.jpg)

   ![compare](compare2.jpg)

2. Can we estimate the stride length using the acceleration oscillogram?

3. What’s the meaning of using `super.onPause()` ?

   If an app needs to be paused, it should first call the superclass method `onPause()` to finish some necessary memory/allocation/scheduling operations to make sure the app could be successfully paused. After that, app can also do other operations related to current class level. 