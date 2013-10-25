package rapid_evolution.audio;

import java.util.Vector;
import rapid_evolution.RapidEvolution;
import rapid_evolution.SongColor;
import rapid_evolution.audio.ellipticalFilter;
import rapid_evolution.audio.Hanning;
import rapid_evolution.audio.FastFourierTransform;
import rapid_evolution.ui.OptionsUI;
import javax.sound.sampled.AudioInputStream;

import org.apache.log4j.Logger;

import com.mixshare.rapid_evolution.audio.codecs.AudioDecoder;
import com.mixshare.rapid_evolution.thread.Task;

public class SubBandSeperator {
    
    private static Logger log = Logger.getLogger(SubBandSeperator.class);
    
  public boolean colormode = false;
  public double samplerate = 0.0;
  public double effectivesamplerate = 0.0;
  double minbpm = 0.0;
  double maxbpm = 0.0;
  public int decimationsize = 64;
  float thresholdtime = 60.0f;
  double seconds = 0.0;
  double lowenergy = 0.0;
  double band1energy = 0.0;
  double band2energy = 0.0;
  double band3energy = 0.0;
  double band4energy = 0.0;
  double highenergy = 0.0;
  int energysegmentsize = 0;
  Task task = null;
  
  public SubBandSeperator(float in_samplerate, double in_minbpm, double in_maxbpm, double tracktime, Task task) {
      this.task = task;
    samplerate = in_samplerate;
    seconds = tracktime;

    double segments = tracktime / thresholdtime;
    double actualsegments = Math.floor(segments);
    double overflow = segments - actualsegments;
    overflow /= actualsegments;
    overflow += 1.0;
    if (tracktime > thresholdtime) {
      thresholdtime *= overflow;
      decimationsize = (int)Math.floor((((double)decimationsize) * overflow * (samplerate / 44100.0)));
    } else {
      decimationsize = (int)Math.floor((((double)decimationsize) * (samplerate / 44100.0) * (tracktime / thresholdtime)));
    }
    effectivesamplerate = samplerate / ((double)decimationsize);
    
    energysegmentsize = (int)(effectivesamplerate * thresholdtime);
    lowpassdata = new double[energysegmentsize];
    band1data = new double[energysegmentsize];
    band2data = new double[energysegmentsize];
    band3data = new double[energysegmentsize];
    band4data = new double[energysegmentsize];
    highpassdata = new double[energysegmentsize];
    finallowpassdata = new double[energysegmentsize - 1];
    finalband1data = new double[energysegmentsize - 1];
    finalband2data = new double[energysegmentsize - 1];
    finalband3data = new double[energysegmentsize - 1];
    finalband4data = new double[energysegmentsize - 1];
    finalhighpassdata = new double[energysegmentsize - 1];
   
    colorchunksize = (int)(((double)colorchunksize) * samplerate / ((double)44100.0f));
    lowvalues = new float[colorchunksize];
    band1values = new float[colorchunksize];
    band2values = new float[colorchunksize];
    band3values = new float[colorchunksize];
    band4values = new float[colorchunksize];
    highvalues = new float[colorchunksize];
    scolor = new SongColor(colorchunksize, (float)samplerate, tracktime, 6);
    minbpm = in_minbpm;
    maxbpm = in_maxbpm;
    log.debug("SubBandSeperator(): decimation rate: " + decimationsize);
    log.debug("SubBandSeperator(): segment length: " + String.valueOf(thresholdtime) + " seconds");
/*
    sixth-order lowpass elliptic filters (MATLAB):
     samplerate = 44100
     LOWPASS: [a,b] = ellip(6,3,40,200/22050);
     BAND1: [a,b] = ellip(3,3,40,[200 400]/22050);
     BAND2: [a,b] = ellip(3,3,40,[400 800]/22050);
     BAND3: [a,b] = ellip(3,3,40,[800 1600]/22050)
     BAND4: [a,b] = ellip(3,3,40,[1600 3200]/22050);
     HIGHPASS: [a,b] = ellip(6,3,40,3200/22050,'high')
*/
    if (samplerate == 44100.0f) {
      // 0 - 200hz
      lowpassfilter = new ellipticalFilter(new double[] { 0.0099,   -0.0596,    0.1488,   -0.1983,    0.1488,   -0.0596,   0.0099 }, new double[] { 1.0000,   -5.9824,   14.9136,  -19.8306,   14.8340,   -5.9186,    0.9841 });
      // 200hz - 400hz
      band1filter = new ellipticalFilter(new double[] { 0.0008,   -0.0031,    0.0039,        0,   -0.0039,    0.0031,   -0.0008 }, new double[] { 1.0000,   -5.9777,   14.8941,  -19.7994,   14.8107,   -5.9109,    0.9833 });
      // 400hz - 800hz
      band2filter = new ellipticalFilter(new double[] { 0.0016,   -0.0062,    0.0077,   -0.0000,   -0.0077,    0.0062,   -0.0016 }, new double[] { 1.0000,   -5.9445,   14.7453,  -19.5360,   14.5807,   -5.8125,    0.9669 });
      // 800hz - 1600hz
      band3filter = new ellipticalFilter(new double[] { 0.0031,   -0.0120,    0.0147,    0.0000,   -0.0147,    0.0120,   -0.0031 }, new double[] { 1.0000,   -5.8459,   14.3244, -18.8305,   14.0062,   -5.5891,    0.9349 });
      // 1600hz - 3200hz
      band4filter = new ellipticalFilter(new double[] { 0.0062,   -0.0222,    0.0257,   -0.0000,   -0.0257,    0.0222,   -0.0062 }, new double[] { 1.0000,   -5.5249,   13.0378,  -16.8004,   12.4644,  -5.0499,    0.8740 });
      // 3200hz - samplerate
      highpassfilter = new ellipticalFilter(new double[] { 0.3918,   -2.2137,    5.3423,   -7.0405,    5.3423,   -2.2137,    0.3918 }, new double[] { 1.0000,   -4.3595,    8.5011,   -9.3963,    6.2685,   -2.4280,    0.4444 });
    } else if (samplerate == 48000.0f) {
      lowpassfilter = new ellipticalFilter(new double[] { 0.0099,   -0.0596,    0.1489,   -0.1985,    0.1489,   -0.0596,    0.0099 }, new double[] { 1.0000,   -5.9839,   14.9211,  -19.8450,   14.8478,   -5.9253,    0.9853 });
      band1filter = new ellipticalFilter(new double[] { 0.0007 ,  -0.0029 ,   0.0036 ,  -0.0000 ,  -0.0036 ,   0.0029 ,  -0.0007 }, new double[] { 1.0000,   -5.9799,   14.9043 , -19.8181 ,  14.8276 ,  -5.9185 ,   0.9846 });
      band2filter = new ellipticalFilter(new double[] { 0.0014,   -0.0057 ,   0.0071 ,   0.0000 ,  -0.0071 ,   0.0057,   -0.0014 }, new double[] { 1.0000,   -5.9506,   14.7724,  -19.5830,   14.6208  , -5.8291 ,   0.9695 });
      band3filter = new ellipticalFilter(new double[] { 0.0028 ,  -0.0111 ,   0.0136 ,  -0.0000 ,  -0.0136 ,   0.0111,   -0.0028 }, new double[] { 1.0000,   -5.8648,   14.4036,  -18.9606,   14.1094,   -5.6277,    0.9400 });
      band4filter = new ellipticalFilter(new double[] { 0.0057,   -0.0207,    0.0243 ,   0.0000,   -0.0243,    0.0207,   -0.0057 }, new double[] { 1.0000,   -5.5879,   13.2831,  -17.1787,   12.7455,   -5.1450 ,   0.8836 });
      highpassfilter = new ellipticalFilter(new double[] {0.4135 ,  -2.3589,    5.7244 ,  -7.5578,    5.7244,   -2.3589 ,   0.4135 }, new double[] {  1.0000,   -4.5260,    9.0508 , -10.1755,    6.8322,   -2.6290,    0.4665 });
    } else if (samplerate == 64000.0f) {
      lowpassfilter = new ellipticalFilter(new double[] { 0.0100,   -0.0597,    0.1492,   -0.1989,    0.1492,   -0.0597  ,  0.0100 }, new double[] { 1.0000 ,  -5.9882 ,  14.9418 , -19.8851  , 14.8867 ,  -5.9441  ,  0.9890 });
      band1filter = new ellipticalFilter(new double[] { 0.0005 ,  -0.0022 ,   0.0027,   -0.0000,   -0.0027 ,   0.0022 ,  -0.0005 }, new double[] { 1.0000,   -5.9858 ,  14.9316 , -19.8686 ,  14.8740 ,  -5.9397,    0.9885 });
      band2filter = new ellipticalFilter(new double[] { 0.0011,   -0.0043,    0.0053,   -0.0000,   -0.0053 ,   0.0043 ,  -0.0011 }, new double[] { 1.0000 ,  -5.9664 ,  14.8428 , -19.7071 ,  14.7284 ,  -5.8748 ,   0.9771 });
      band3filter = new ellipticalFilter(new double[] { 0.0021,   -0.0084 ,   0.0104 ,   0.0000,   -0.0104,    0.0084,   -0.0021 }, new double[] { 1.0000,   -5.9122,   14.6051,  -19.2964,   14.3808,   -5.7320 ,   0.9546 });
      band4filter = new ellipticalFilter(new double[] { 0.0043,   -0.0161,    0.0195,    0.0000,   -0.0195,    0.0161,   -0.0043 }, new double[] { 1.0000,   -5.7435,   13.9032,  -18.1515 ,  13.4792 ,  -5.3987 ,   0.9114 });
      highpassfilter = new ellipticalFilter(new double[] { 0.4798,   -2.7992 ,   6.8820,   -9.1253,    6.8820,   -2.7992,    0.4798 }, new double[] {  1.0000,   -4.9753,   10.6491,  -12.5445 ,   8.6042 ,  -3.2761  ,  0.5465 });
    } else if (samplerate == 88200.0f) {
      lowpassfilter = new ellipticalFilter(new double[] { 0.0100,   -0.0598,    0.1494,  -0.1992,    0.1494,   -0.0598,    0.0100 }, new double[] { 1.0000,   -5.9916,   14.9583,  -19.9175,   14.9183,   -5.9596,    0.9920 });
      band1filter = new ellipticalFilter(new double[] { 0.0004 ,  -0.0017,    0.0021 ,  -0.0000 ,  -0.0021  ,  0.0017  , -0.0004 }, new double[] { 1.0000,   -5.9894,   14.9484 , -19.9001 ,  14.9033 ,  -5.9533 ,   0.9910 });
      band2filter = new ellipticalFilter(new double[] { 0.0008 ,  -0.0031 ,   0.0039  ,       0 ,  -0.0039   , 0.0031 ,  -0.0008 }, new double[] { 1.0000 ,  -5.9777,   14.8941,  -19.7994 ,  14.8107 ,  -5.9109 ,   0.9833 });
      band3filter = new ellipticalFilter(new double[] { 0.0016,   -0.0062,    0.0077,   -0.0000,   -0.0077,    0.0062,   -0.0016 }, new double[] { 1.0000,   -5.9445,   14.7453,  -19.5360,   14.5807,   -5.8125,    0.9669 });
      band4filter = new ellipticalFilter(new double[] { 0.0031 ,  -0.0120,    0.0147  ,  0.0000 ,  -0.0147,    0.0120  , -0.0031 }, new double[] { 1.0000 ,  -5.8459,   14.3244,  -18.8305,   14.0062,   -5.5891,    0.9349 });
      highpassfilter = new ellipticalFilter(new double[] { 0.5394,   -3.1889,    7.9026,  -10.5061 ,   7.9026,   -3.1889 ,   0.5394 }, new double[] {  1.0000,   -5.3149,   11.9704,  -14.6285,   10.2438,   -3.9055,    0.6354 });
    } else if (samplerate == 96000.0f) {
      lowpassfilter = new ellipticalFilter(new double[] { 0.0100 ,  -0.0598 ,   0.1495 ,  -0.1993 ,   0.1495  , -0.0598 ,   0.0100 }, new double[] { 1.0000,   -5.9923  , 14.9618  ,-19.9243 ,  14.9250 ,  -5.9629 ,   0.9926 });
      band1filter = new ellipticalFilter(new double[] { 0.0004 ,  -0.0014 ,   0.0018,   -0.0000  , -0.0018  ,  0.0014 ,  -0.0004 }, new double[] { 1.0000 ,  -5.9911,   14.9567 , -19.9158 ,  14.9182 ,  -5.9603  ,  0.9923 });
      band2filter = new ellipticalFilter(new double[] { 0.0007,   -0.0029 ,   0.0036,   -0.0000 ,  -0.0036,    0.0029,   -0.0007 }, new double[] { 1.0000,   -5.9799,   14.9043 , -19.8181 ,  14.8276,   -5.9185 ,   0.9846 });
      band3filter = new ellipticalFilter(new double[] { 0.0014,   -0.0057,    0.0071,    0.0000 ,  -0.0071 ,   0.0057,   -0.0014 }, new double[] { 1.0000 ,  -5.9506 ,  14.7724,  -19.5830 ,  14.6208 ,  -5.8291,    0.9695 });
      band4filter = new ellipticalFilter(new double[] { 0.0028,   -0.0111,    0.0136,   -0.0000,   -0.0136  ,  0.0111 ,  -0.0028 }, new double[] { 1.0000,   -5.8648 ,  14.4036,  -18.9606,   14.1094,   -5.6277,   0.9400 });
      highpassfilter = new ellipticalFilter(new double[] { 0.5527 ,  -3.2750,    8.1270 , -10.8094,    8.1270  , -3.2750 ,   0.5527 }, new double[] {  1.0000,   -5.3834 ,  12.2492 , -15.0844,   10.6141,   -4.0528,    0.6576 });
    } else if (samplerate == 192000.0f) {
      lowpassfilter = new ellipticalFilter(new double[] { 0.0100 ,  -0.0599 ,   0.1498 ,  -0.1997  ,  0.1498  , -0.0599 ,   0.0100 }, new double[] { 1.0000 ,  -5.9962  , 14.9812 , -19.9626  , 14.9628  , -5.9815  ,  0.9963 });
      band1filter = new ellipticalFilter(new double[] { 0.0001805,   -0.0007219 ,   0.0009023 ,   0.0000 ,  -0.0009023,    0.0007219,   -0.0001805 }, new double[] {  1.0000 ,  -5.9958,   14.9795 , -19.9596 ,  14.9602,   -5.9804  ,  0.9961 });
      band2filter = new ellipticalFilter(new double[] { 0.0004,   -0.0014,    0.0018,   -0.0000,   -0.0018,    0.0014,   -0.0004 }, new double[] { 1.0000,   -5.9911 ,  14.9567,  -19.9158,   14.9182 ,  -5.9603 ,   0.9923 });
      band3filter = new ellipticalFilter(new double[] { 0.0007,   -0.0029,    0.0036,   -0.0000,   -0.0036,    0.0029,   -0.0007 }, new double[] { 1.0000 ,  -5.9799 ,  14.9043,  -19.8181,   14.8276 ,  -5.9185,   0.9846 });
      band4filter = new ellipticalFilter(new double[] { 0.0014,   -0.0057,    0.0071,    0.0000,   -0.0071,    0.0057,   -0.0014 }, new double[] { 1.0000,   -5.9506,   14.7724 , -19.5830  , 14.6208 ,  -5.8291 ,   0.9695 });
      highpassfilter = new ellipticalFilter(new double[] { 0.6299,   -3.7680,    9.4026,  -12.5291,    9.4026,   -3.7680,    0.6299 }, new double[] {  1.0000,   -5.7329,   13.7453,  -17.6439,   12.7906 ,  -4.9659,    0.8069 });
    } else if (samplerate == 32000.0f) {
      lowpassfilter = new ellipticalFilter(new double[] { 0.0099 ,  -0.0594  ,  0.1482 ,  -0.1976  ,  0.1482 ,  -0.0594  ,  0.0099 }, new double[] { 1.0000,   -5.9749,   14.8779 , -19.7621,   14.7684 ,  -5.8874 ,   0.9781 });
      band1filter = new ellipticalFilter(new double[] { 0.0011 ,  -0.0043,    0.0053 ,  -0.0000 ,  -0.0053 ,   0.0043 ,  -0.0011 }, new double[] {  1.0000 ,  -5.9664,   14.8428 , -19.7071 ,  14.7284 ,  -5.8748 ,   0.9771 });
      band2filter = new ellipticalFilter(new double[] { 0.0021 ,  -0.0084 ,   0.0104,    0.0000 ,  -0.0104  ,  0.0084  , -0.0021 }, new double[] { 1.0000 ,  -5.9122,   14.6051,  -19.2964 ,  14.3808,   -5.7320,    0.9546 });
      band3filter = new ellipticalFilter(new double[] {0.0043,   -0.0161,    0.0195 ,   0.0000 ,  -0.0195,   0.0161,   -0.0043 }, new double[] { 1.0000,   -5.7435,   13.9032,  -18.1515,   13.4792 ,  -5.3987,    0.9114 });
      band4filter = new ellipticalFilter(new double[] { 0.0088 ,  -0.0282,    0.0303,   -0.0000,   -0.0303 ,   0.0282 ,  -0.0088 }, new double[] { 1.0000,   -5.1795,   11.7446,  -14.8519,   11.0383,   -4.5755,    0.8306 });
      highpassfilter = new ellipticalFilter(new double[] {0.3027,   -1.6156 ,   3.7764,   -4.9252,    3.7764 ,  -1.6156 ,   0.3027 }, new double[] {  1.0000 ,  -3.5477,    6.1446,   -6.2447,    4.0839  , -1.6494,   0.3747 });
    } else if (samplerate == 22050.0f) {
      lowpassfilter = new ellipticalFilter(new double[] { 0.0099 ,  -0.0591  ,  0.1473 ,  -0.1963  ,  0.1473   ,-0.0591 ,   0.0099 }, new double[] { 1.0000,   -5.9617  , 14.8154  ,-19.6443  , 14.6576 ,  -5.8353,    0.9684 });
      band1filter = new ellipticalFilter(new double[] { 0.0016,   -0.0062,    0.0077,   -0.0000,   -0.0077,    0.0062,   -0.0016 }, new double[] {  1.0000 ,  -5.9445 ,  14.7453,  -19.5360,   14.5807  , -5.8125 ,   0.9669 });
      band2filter = new ellipticalFilter(new double[] { 0.0031,   -0.0120,    0.0147,    0.0000,   -0.0147 ,   0.0120,   -0.0031 }, new double[] { 1.0000,   -5.8459,   14.3244,  -18.8305,   14.0062,   -5.5891,    0.9349 });
      band3filter = new ellipticalFilter(new double[] { 0.0062,   -0.0222 ,   0.0257,   -0.0000 ,  -0.0257,    0.0222 ,  -0.0062 }, new double[] { 1.0000,   -5.5249 ,  13.0378 , -16.8004,   12.4644,  -5.0499 ,   0.8740 });
      band4filter = new ellipticalFilter(new double[] { 0.0137,   -0.0342,    0.0292,    0.0000,   -0.0292,    0.0342,   -0.0137 }, new double[] { 1.0000,   -4.4407,    9.2477,  -11.2579,    8.4523,   -3.7083,    0.7640 });
      highpassfilter = new ellipticalFilter(new double[] {0.1950,   -0.9003,    1.9537,   -2.4868,    1.9537,   -0.9003,    0.1950 }, new double[] {  1.0000,   -2.1311 ,   3.3013,   -2.6898,    1.9248 ,  -0.7401,    0.3390 });
    } else if (samplerate == 16000.0f) {
      lowpassfilter = new ellipticalFilter(new double[] { 0.0099 ,  -0.0588  ,  0.1462 ,  -0.1945 ,   0.1462 ,  -0.0588 ,   0.0099 }, new double[] { 1.0000 ,  -5.9441  , 14.7334 , -19.4924 ,  14.5176  , -5.7711   , 0.9567 });
      band1filter = new ellipticalFilter(new double[] { 0.0021 ,  -0.0084 ,   0.0104 ,   0.0000  , -0.0104  ,  0.0084  , -0.0021 }, new double[] {  1.0000,   -5.9122 ,  14.6051,  -19.2964 ,  14.3808 ,  -5.7320  ,  0.9546 });
      band2filter = new ellipticalFilter(new double[] { 0.0043,   -0.0161 ,   0.0195 ,   0.0000,   -0.0195,    0.0161,   -0.0043 }, new double[] {1.0000  , -5.7435,   13.9032,  -18.1515,   13.4792,   -5.3987 ,   0.9114 });
      band3filter = new ellipticalFilter(new double[] { 0.0088,   -0.0282 ,   0.0303,   -0.0000,   -0.0303,    0.0282,   -0.0088 }, new double[] { 1.0000 ,  -5.1795  , 11.7446 , -14.8519,   11.0383,   -4.5755 ,   0.8306 });
      band4filter = new ellipticalFilter(new double[] { 0.0211,   -0.0341,    0.0128,    0.0000,   -0.0128,    0.0341,   -0.0211 }, new double[] { 1.0000,   -3.3082,    6.0989,   -6.9568 ,   5.3972,   -2.5789 ,   0.6903 });
      highpassfilter = new ellipticalFilter(new double[] {0.1128,   -0.3846,    0.7456,   -0.9055,    0.7456,   -0.3846,    0.1128 }, new double[] {  1.0000,   -0.3646 ,   1.9163 ,  -0.0596,    1.2131,    0.1268,    0.3638 });
    } else if (samplerate == 11025.0f) {
      lowpassfilter = new ellipticalFilter(new double[] { 0.0099 ,  -0.0584 ,   0.1441,   -0.1913  ,  0.1441  , -0.0584 ,   0.0099 }, new double[] { 1.0000 ,  -5.9113 ,  14.5843 , -19.2227 ,  14.2755,   -5.6635 ,   0.9377 });
      band1filter = new ellipticalFilter(new double[] { 0.0031 ,  -0.0120  ,  0.0147 ,   0.0000,   -0.0147 ,   0.0120  , -0.0031 }, new double[] {  1.0000 ,  -5.8459,   14.3244,  -18.8305,   14.0062 ,  -5.5891 ,   0.9349 });
      band2filter = new ellipticalFilter(new double[] { 0.0062,   -0.0222  ,  0.0257,   -0.0000,   -0.0257 ,   0.0222 ,  -0.0062 }, new double[] {1.0000,   -5.5249,   13.0378,  -16.8004,   12.4644,   -5.0499 ,   0.8740 });
      band3filter = new ellipticalFilter(new double[] { 0.0137,   -0.0342,    0.0292 ,   0.0000,   -0.0292,    0.0342  , -0.0137 }, new double[] { 1.0000 ,  -4.4407 ,   9.2477 , -11.2579 ,   8.4523 ,  -3.7083,    0.7640 });
      band4filter = new ellipticalFilter(new double[] { 0.0387,   -0.0157,   -0.0426 ,  -0.0000  ,  0.0426,    0.0157 ,  -0.0387 }, new double[] { 1.0000,   -1.1027,    2.3379,   -1.6635,    2.0160,   -0.7670 ,   0.5839 });
      highpassfilter = new ellipticalFilter(new double[] {0.0452,   -0.0336,    0.1106 ,  -0.0695 ,   0.1106 ,  -0.0336  ,  0.0452 }, new double[] { 1.0000,    2.3854,    4.1750 ,   4.4000 ,   3.4071,    1.6417 ,   0.4785 });
    } else if (samplerate == 8000.0f) {
      lowpassfilter = new ellipticalFilter(new double[] { 0.0100 ,  -0.0579  ,  0.1413 ,  -0.1869 ,   0.1413 ,  -0.0579 ,   0.0100 }, new double[] { 1.0000 ,  -5.8651 ,  14.3796 , -18.8628 ,  13.9623,   -5.5293  ,  0.9152 });
      band1filter = new ellipticalFilter(new double[] { 0.0043,   -0.0161 ,   0.0195,    0.0000,   -0.0195 ,   0.0161 ,  -0.0043 }, new double[] { 1.0000  , -5.7435  , 13.9032 , -18.1515,   13.4792,  -5.3987  ,  0.9114 });
      band2filter = new ellipticalFilter(new double[] { 0.0088,   -0.0282,    0.0303,   -0.0000  , -0.0303,    0.0282  , -0.0088 }, new double[] {1.0000,   -5.1795,   11.7446,  -14.8519,   11.0383,   -4.5755 ,   0.8306 });
      band3filter = new ellipticalFilter(new double[] { 0.0211,   -0.0341,    0.0128,    0.0000,   -0.0128 ,   0.0341 ,  -0.0211 }, new double[] { 1.0000,   -3.3082,    6.0989,   -6.9568 ,   5.3972,   -2.5789  ,  0.6903 });
      band4filter = new ellipticalFilter(new double[] { 0.0722,    0.0301,   -0.1263,   -0.0000 ,   0.1263 ,  -0.0301,   -0.0722 }, new double[] { 1.0000,    1.6345,    2.2614,    2.0921,    1.9091 ,   0.9907,    0.4736 });
      highpassfilter = new ellipticalFilter(new double[] {0.0156 ,   0.0519 ,   0.0966  ,  0.1145,    0.0966 ,   0.0519 ,   0.0156 }, new double[] { 1.0000,    4.9327 ,  10.7889 ,  13.2654,    9.6417,    3.9257   , 0.7018 });
    } else if (samplerate ==  6000.0f) {
      lowpassfilter = new ellipticalFilter(new double[] { 0.0102,   -0.0573,    0.1375 ,  -0.1809,    0.1375,   -0.0573   , 0.0102 }, new double[] { 1.0000 ,  -5.7998,   14.0979 , -18.3811 ,  13.5563 ,  -5.3620  ,  0.8886 });
      band1filter = new ellipticalFilter(new double[] { 0.0057,   -0.0207   , 0.0243 ,   0.0000 ,  -0.0243 ,   0.0207 ,  -0.0057 }, new double[] { 1.0000,   -5.5879,   13.2831,  -17.1787,   12.7455 ,  -5.1450,    0.8836 });
      band2filter = new ellipticalFilter(new double[] { 0.0123,   -0.0332,    0.0306 ,  -0.0000 ,  -0.0306 ,   0.0332 ,  -0.0123 }, new double[] {1.0000,   -4.6525,    9.9276,  -12.2176,    9.1398,   -3.9428,    0.7809 });
      band3filter = new ellipticalFilter(new double[] { 0.0333  , -0.0226 ,  -0.0257 ,  -0.0000 ,   0.0257 ,   0.0226,   -0.0333 }, new double[] { 1.0000 ,  -1.7025 ,   3.0449 ,  -2.7871,    2.6253  , -1.2201 ,   0.6101 });
      band4filter = null;
      highpassfilter = null;
    } else {
      // throw unsupported samplerate msg
      //throw new Exception();
    }
    hwindow = new Hanning(samplerate);
  }

  private double[] extra = null;
  private int colorchunksize = 1024;
  private float[] lowvalues = null;
  private float[] band1values = null;
  private float[] band2values = null;
  private float[] band3values = null;
  private float[] band4values = null;
  private float[] highvalues = null;
  int colorindex = 0;

  public void send(double[] data) throws Exception {
    int count = 0;
    while (count + decimationsize <= data.length) {
        if (RapidEvolution.instance.terminatesignal || task.isCancelled()) return;        
      double newlowpass = 1.0;
      double newband1pass = 1.0;
      double newband2pass = 1.0;
      double newband3pass = 1.0;
      double newband4pass = 1.0;
      double newhighpass = 1.0;
      int thisdecimationsize = decimationsize;
      if (extra != null) {
        for (int i = 0; i < extra.length; ++i) {
            if (RapidEvolution.instance.terminatesignal || task.isCancelled()) return;
          double val = extra[i];
          double lowpassval = lowpassfilter.process(val);
          double band1val = band1filter.process(val);
          double band2val = band2filter.process(val);
          double band3val = band3filter.process(val);
          double band4val = band4filter.process(val);
          double highpassval = highpassfilter.process(val);
          if (colormode) {
            lowenergy += lowpassval * lowpassval;
            band1energy += band1val * band1val;
            band2energy += band2val * band2val;
            band3energy += band3val * band3val;
            band4energy += band4val * band4val;
            highenergy += highpassval * highpassval;
            lowvalues[colorindex] = (float)lowpassval;
            band1values[colorindex] = (float)band1val;
            band2values[colorindex] = (float)band2val;
            band3values[colorindex] = (float)band3val;
            band4values[colorindex] = (float)band4val;
            highvalues[colorindex] = (float)highpassval;
            colorindex++;
            if (colorindex >= colorchunksize) {
              samplesanalyzed++;
              ProcessSurface(lowvalues, 0);
              ProcessSurface(band1values, 1);
              ProcessSurface(band2values, 2);
              ProcessSurface(band3values, 3);
              ProcessSurface(band4values, 4);
              ProcessSurface(highvalues, 5);
              colorindex = 0;
            }
          }
          if (lowpassval < 0) lowpassval = 0;
          newlowpass += lowpassval * lowpassval;
          if (band1val < 0) band1val = 0;
          newband1pass += band1val * band1val;
          if (band2val < 0) band2val = 0;
          newband2pass += band2val * band2val;
          if (band3val < 0) band3val = 0;
          newband3pass += band3val * band3val;
          if (band4val < 0) band4val = 0;
          newband4pass += band4val * band4val;
          if (highpassval < 0) highpassval = 0;
          newhighpass += highpassval * highpassval;
        }
        thisdecimationsize -= extra.length;
        extra = null;
      }
      if (RapidEvolution.instance.terminatesignal || task.isCancelled()) return;
      for (int i = 0; i < thisdecimationsize; ++i) {
          if (RapidEvolution.instance.terminatesignal || task.isCancelled()) return;
        double val = data[i + count];
        double lowpassval = lowpassfilter.process(val);
        double band1val = band1filter.process(val);
        double band2val = band2filter.process(val);
        double band3val = band3filter.process(val);
        double band4val = band4filter.process(val);
        double highpassval = highpassfilter.process(val);
        if (colormode) {
          lowenergy += lowpassval * lowpassval;
          band1energy += band1val * band1val;
          band2energy += band2val * band2val;
          band3energy += band3val * band3val;
          band4energy += band4val * band4val;
          highenergy += highpassval * highpassval;
          lowvalues[colorindex] = (float)lowpassval;
          band1values[colorindex] = (float)band1val;
          band2values[colorindex] = (float)band2val;
          band3values[colorindex] = (float)band3val;
          band4values[colorindex] = (float)band4val;
          highvalues[colorindex] = (float)highpassval;
          colorindex++;
          if (colorindex >= colorchunksize) {
            samplesanalyzed++;
            ProcessSurface(lowvalues, 0);
            ProcessSurface(band1values, 1);
            ProcessSurface(band2values, 2);
            ProcessSurface(band3values, 3);
            ProcessSurface(band4values, 4);
            ProcessSurface(highvalues, 5);
            colorindex = 0;
          }
        }
        if (lowpassval < 0) lowpassval = 0;
        newlowpass += lowpassval * lowpassval;
        if (band1val < 0) band1val = 0;
        newband1pass += band1val * band1val;
        if (band2val < 0) band2val = 0;
        newband2pass += band2val * band2val;
        if (band3val < 0) band3val = 0;
        newband3pass += band3val * band3val;
        if (band4val < 0) band4val = 0;
        newband4pass += band4val * band4val;
        if (highpassval < 0) highpassval = 0;
        newhighpass += highpassval * highpassval;
      }
      lowpassdata[lpindex++] = Math.log(newlowpass);
      band1data[b1index++] = Math.log(newband1pass);
      band2data[b2index++] = Math.log(newband2pass);
      band3data[b3index++] = Math.log(newband3pass);
      band4data[b4index++] = Math.log(newband4pass);
      highpassdata[hpindex++] = Math.log(newhighpass);

      if (lpindex >= lowpassdata.length) {
          log.debug("send(): -> " + String.valueOf(thresholdtime) + " second segment:");
          ExtractAndProcess();
        }
      
      count += thisdecimationsize;
    }
    if (count < data.length) {
      extra = new double[data.length - count];
      int index = 0;
      while (count < data.length) extra[index++] = data[count++];
    }

  }

  int samplesanalyzed = 0;
  public SongColor scolor = null;
  float[][] previousfftdata = { null, null, null, null, null, null };
  float[] avgenergy = { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f };
  Vector[] energytrail = { new Vector(), new Vector(), new Vector(), new Vector(), new Vector(), new Vector() };
  Vector[] zerocrossingtrail = { new Vector(), new Vector(), new Vector(), new Vector(), new Vector(), new Vector() };
  Vector[] fluxtrail = { new Vector(), new Vector(), new Vector(), new Vector(), new Vector(), new Vector() };
  Vector[] rollofftrail = { new Vector(), new Vector(), new Vector(), new Vector(), new Vector(), new Vector() };
  Vector[] centroidtrail = { new Vector(), new Vector(), new Vector(), new Vector(), new Vector(), new Vector() };
  int[] centroidsamplesanalyzed = { 0, 0, 0, 0, 0, 0 };
    
  float maxw = Float.MIN_VALUE;
  float avgw = 0.0f;
  float totalw = 0.0f;
  int numw = 0;
  private void ProcessSurface(float[] wdata, int index) {
    //process wdata

      if (RapidEvolution.instance.terminatesignal || task.isCancelled()) return;
      
    int zerocrossings = 0;
    float totalenergy = wdata[0] * wdata[0];
    float abswdata = Math.abs(wdata[0]);
    if (abswdata > maxw) maxw = abswdata;
    totalw += abswdata;
    ++numw;
    for (int i = 1; i < wdata.length; ++i) {
      if ((wdata[i] > 0) && (wdata[i - 1] <= 0)) zerocrossings++;
      else if ((wdata[i] <= 0) && (wdata[i - 1] > 0)) zerocrossings++;
      totalenergy += wdata[i] * wdata[i];
      abswdata = Math.abs(wdata[i]);
      totalw += abswdata;
      if (abswdata > maxw) maxw = abswdata;
      ++numw;
    }
    avgenergy[index] += totalenergy;
    energytrail[index].add(new Float(totalenergy));
    scolor.avgzerocrossings[index] += zerocrossings;
    zerocrossingtrail[index].add(new Integer(zerocrossings));

    FastFourierTransform fft = new FastFourierTransform();
    if (RapidEvolution.instance.terminatesignal || task.isCancelled()) return;
    
    float[] fftdata = fft.doFFT(wdata);
    float centroidnumerator = 0.0f;
    float centroiddenominator = 0.0f;
    float flux = 0.0f;
    for (int i = 0; i < fftdata.length / 2; ++i) {
      float frequency = (((float)i) / ((float)fftdata.length)) * ((float)samplerate);
      float magnitude = Math.abs(fftdata[i]);
      centroidnumerator += frequency * magnitude;
      centroiddenominator += magnitude;
      if (previousfftdata[index] != null) {
        flux += Math.abs(Math.abs(previousfftdata[index][i]) - magnitude);
      }
    }
    scolor.avgflux[index] += flux;
    fluxtrail[index].add(new Float(flux));

    float rolloffvalue = centroiddenominator * 0.85f;
    int r = 0;
    float min = centroiddenominator;
    float rolloffdenominator = 0.0f;
    for (int i = 0; i < fftdata.length / 2; ++i) {
      float magnitude = Math.abs(fftdata[i]);
      rolloffdenominator += magnitude;
      float diff = Math.abs(rolloffdenominator - rolloffvalue);
      if (diff < min) {
        r = i;
        min = diff;
      }
    }
    scolor.avgrolloff[index] += r;
    rollofftrail[index].add(new Integer(r));
    if (centroiddenominator != 0.0f) {
      double centroid = centroidnumerator / centroiddenominator;
      scolor.avgcentroid[index] += centroid;
      centroidtrail[index].add(new Float(centroid));
      centroidsamplesanalyzed[index]++;
    }
    previousfftdata[index] = fftdata;

  }

  private void ExtractAndProcess() throws Exception {
    double detectedbpm = 0.0;
    EnvelopeExtract(lowpassdata, hwindow, finallowpassdata);
    lpindex = 0;
    detectedbpm = CalculateSpectralSum(minbpm, finallowpassdata);
    log.debug("ExtractAndProcess(): lowpass bpm: " + String.valueOf(detectedbpm));
    if (RapidEvolution.instance.terminatesignal || task.isCancelled()) throw new Exception();
    
    EnvelopeExtract(band1data, hwindow, finalband1data);
    b1index = 0;
    detectedbpm = CalculateSpectralSum(minbpm, finalband1data);
    log.debug("ExtractAndProcess(): band1 bpm: " + String.valueOf(detectedbpm));
    if (RapidEvolution.instance.terminatesignal || task.isCancelled()) throw new Exception();

    EnvelopeExtract(band2data, hwindow, finalband2data);
    b2index = 0;
    detectedbpm = CalculateSpectralSum(minbpm, finalband2data);
    log.debug("ExtractAndProcess(): band2 bpm: " + String.valueOf(detectedbpm));
    if (RapidEvolution.instance.terminatesignal || task.isCancelled()) throw new Exception();

    EnvelopeExtract(band3data, hwindow, finalband3data);
    b3index = 0;
    detectedbpm = CalculateSpectralSum(minbpm, finalband3data);
    log.debug("ExtractAndProcess(): band3 bpm: " + String.valueOf(detectedbpm));
    if (RapidEvolution.instance.terminatesignal || task.isCancelled()) throw new Exception();

    EnvelopeExtract(band4data, hwindow, finalband4data);
    b4index = 0;
    detectedbpm = CalculateSpectralSum(minbpm, finalband4data);
    log.debug("ExtractAndProcess(): band4 bpm: " + String.valueOf(detectedbpm));
    if (RapidEvolution.instance.terminatesignal || task.isCancelled()) throw new Exception();

    EnvelopeExtract(highpassdata, hwindow, finalhighpassdata);
    hpindex = 0;
    detectedbpm = CalculateSpectralSum(minbpm, finalhighpassdata);
    log.debug("ExtractAndProcess(): highpass bpm: " + String.valueOf(detectedbpm));
    if (RapidEvolution.instance.terminatesignal || task.isCancelled()) throw new Exception();

    detectedbpm = getOverallBpm();
    log.debug("ExtractAndProcess(): current bpm: " + String.valueOf(detectedbpm));

    segmentbpms.add(new Double(detectedbpm));
    int iter = segmentbpms.size() - 2;
    boolean matchfound = true;
    int matchesread = 1;
    while (matchfound && (iter >= 0) && (matchesread < earlydetectthreshold) && (detectedbpm != 0.0)) {
      double d = ((Double)segmentbpms.get(iter)).doubleValue();
      if (d != detectedbpm) matchfound = false;
      iter--;
      matchesread++;
    }
    if (matchesread < earlydetectthreshold) matchfound = false;
    if (matchfound) lockedon = true;
  }

  public boolean lockedon = false;
  public int earlydetectthreshold = 4;
  public Vector segmentbpms = new Vector();

  private ellipticalFilter lowpassfilter = null;
  private ellipticalFilter band1filter = null;
  private ellipticalFilter band2filter = null;
  private ellipticalFilter band3filter = null;
  private ellipticalFilter band4filter = null;
  private ellipticalFilter highpassfilter = null;

  private Hanning hwindow = null;

  public double[] lowpassdata = null;
  int lpindex = 0;
  public double[] band1data = null;
  int b1index = 0;
  public double[] band2data = null;
  int b2index = 0;
  public double[] band3data = null;
  int b3index = 0;
  public double[] band4data = null;
  int b4index = 0;
  public double[] highpassdata = null;
  int hpindex = 0;

  public double[] finallowpassdata = null;
  public double[] finalband1data = null;
  public double[] finalband2data = null;
  public double[] finalband3data = null;
  public double[] finalband4data = null;
  public double[] finalhighpassdata = null;

  Vector results = null;
  int firstblocksize = -1;
  boolean normalized = false;

  public void normalize() throws Exception {
    if (normalized) return;
    if (results == null) ExtractAndProcess();
    Vector newresults = new Vector();
    for (int i = 0; i < results.size(); ++i) {
      BPMRecord record = (BPMRecord)results.get(i);
      if (i == 0) {
        BPMRecord record2 = (BPMRecord)results.get(i + 1);
        BPMRecord record3 = (BPMRecord)results.get(results.size() - 1);
        BPMRecord newrecord = new BPMRecord();
        newrecord.bpm = record.bpm;
        newrecord.score = record.score + record2.score * 0.5 + record3.score * 0.5;
        newresults.add(newrecord);
      } else if (i == results.size() - 1) {
        BPMRecord record2 = (BPMRecord)results.get(results.size() - 2);
        BPMRecord record3 = (BPMRecord)results.get(0);
        BPMRecord newrecord = new BPMRecord();
        newrecord.bpm = record.bpm;
        newrecord.score = record.score + record2.score * 0.5 + record3.score * 0.5;
        newresults.add(newrecord);
      } else {
        BPMRecord record2 = (BPMRecord)results.get(i - 1);
        BPMRecord record3 = (BPMRecord)results.get(i + 1);
        BPMRecord newrecord = new BPMRecord();
        newrecord.bpm = record.bpm;
        newrecord.score = record.score + record2.score * 0.5 + record3.score * 0.5;
        newresults.add(newrecord);
      }
    }
    results = newresults;
    normalized = true;
  }

  
  public int getBeatIntensity() throws Exception {

      normalize();

      /*
      double max = 0.0;
      double min = 0.0;
      double detectedbpm = 0.0;
      double minbpm1 = 0.0;
      double maxbpm1 = 0.0;
      for (int i = 0; i < results.size(); ++i) {
        BPMRecord record = (BPMRecord)results.get(i);
        if (i == 0) {
            min = record.score;
        } else {
            if (record.score < min) min = record.score;
        }
        if (record.score > max) {
          detectedbpm = getRoundedBpm(results, i);
          max = record.score;
          minbpm1 = record.bpm * 0.975;
          maxbpm1 = record.bpm * 1.025;
        }
      }

      double max2 = 0.0;
      double minbpm2 = 0.0;
      double maxbpm2 = 0.0;
      double bpm2 = 0.0;
      for (int i = 0; i < results.size(); ++i) {
        BPMRecord record = (BPMRecord)results.get(i);
        if ((record.score > max2) && ((record.bpm <= minbpm1) || (record.bpm >= maxbpm1))) {
          max2 = record.score;
          bpm2 = record.bpm;
          minbpm2 = record.bpm * 0.98;
          maxbpm2 = record.bpm * 1.02;
        }
      }

      double max3 = 0.0;
      double minbpm3 = 0.0;
      double maxbpm3 = 0.0;
      double bpm3 = 0.0;
      for (int i = 0; i < results.size(); ++i) {
        BPMRecord record = (BPMRecord)results.get(i);
        if ((record.score > max3) && ((record.bpm <= minbpm1) || (record.bpm >= maxbpm1)) && ((record.bpm <= minbpm2) || (record.bpm >= maxbpm2))) {
          max3 = record.score;
          bpm3 = record.bpm;
          minbpm3 = record.bpm * 0.99;
          maxbpm3 = record.bpm * 1.01;
        }
      }

     double accuracy = 0.0;
     try {
         double total_score = 0.0;   
         for (int i = 0; i < results.size(); ++i) {
             BPMRecord record = (BPMRecord)results.get(i);
             total_score += record.score;       
         }
         double average = total_score / results.size();       
         double range = (max - min);
         double total_sqr_diff = 0.0;
         for (int i = 0; i < results.size(); ++i) {
             BPMRecord record = (BPMRecord)results.get(i);
             total_sqr_diff += (record.score - average) * (record.score - average);
         }
         double std_dev = Math.sqrt(total_sqr_diff / results.size());
//         accuracy = (max - average) / range * (max - average - std_dev) / (max - average);
         accuracy = (max - max2) / (max - average);
        
     } catch (Exception e) {        }   
      
      return (int)(accuracy * 100.0f);
     */
            
      // normalize energy of wave data
      avgw = totalw / numw;
      
      Vector peaks = new Vector();
      int beat_intensity = 0;
      double totalamp = 0.0;
      double totalpeak = 0.0;
      double min = Double.MAX_VALUE;
      for (int i = 0; i < results.size(); ++i) {
        BPMRecord record = (BPMRecord)results.get(i);
        if (record.score < min) min = record.score;
        totalamp += record.score;
        BPMRecord record2 = null;
        BPMRecord record3 = null;
        if (i == 0) {
          record2 = (BPMRecord)results.get(i + 1);
          record3 = (BPMRecord)results.get(results.size() - 1);
        } else if (i == results.size() - 1) {
          record2 = (BPMRecord)results.get(results.size() - 2);
          record3 = (BPMRecord)results.get(0);
        } else {
          record2 = (BPMRecord)results.get(i - 1);
          record3 = (BPMRecord)results.get(i + 1);
        }
        boolean ispeak = false;
        if ((record2.score <= record.score) && (record3.score <= record.score)) ispeak = true;
        if (ispeak) {
            totalpeak += record.score;
          if (peaks.size() == 0) {
            peaks.add(record);
          } else {
            boolean inserted = false;
            int j = 0;
            while (!inserted && (j < peaks.size())) {
              BPMRecord r = (BPMRecord)peaks.get(j);
              if (record.score > r.score) {
                inserted = true;
                peaks.insertElementAt(record, j);
              }
              ++j;
            }
            if (!inserted) peaks.add(record);
          }
        }
      }
      double average = totalamp / results.size();
      double peak_average = totalpeak / peaks.size();
      double total_sqr_diff = 0.0;
      for (int i = 0; i < results.size(); ++i) {
          BPMRecord record = (BPMRecord)results.get(i);
          total_sqr_diff += (record.score - average) * (record.score - average);
      }      
      double std_dev = Math.sqrt(total_sqr_diff / results.size());
      double top_peak_total = 0.0;
      for (int i = 0; i < peaks.size(); ++i) {
          BPMRecord record = (BPMRecord)peaks.get(i);
          if (record.score >= (peak_average + std_dev)) top_peak_total += record.score;
      }
      double total_flux = 0.0;
      double total_energy = 0.0;
      for (int i = 0; i < scolor.avgflux.length; ++i) {
          total_flux += scolor.avgflux[i];
          total_energy += avgenergy[i];
      }
      double avg_flux = total_flux / scolor.avgflux.length;
      double avg_energy = total_energy / avgenergy.length;
      try {
          double amplitude0 = ((BPMRecord)peaks.get(0)).score;
          double bpm0 = ((BPMRecord)peaks.get(0)).bpm;
          double amplitude1 = ((BPMRecord)peaks.get(1)).score;
          double bpm1 = ((BPMRecord)peaks.get(1)).bpm;
          double amplitudex = ((BPMRecord)peaks.get(peaks.size() - 1)).score;
          double bpmx = ((BPMRecord)peaks.get(peaks.size() - 1)).bpm;
          log.debug("getBeatIntensity(): # peaks=" + peaks.size() + ", peak average=" + peak_average);
          log.debug("getBeatIntensity(): 1st peak score=" + amplitude0 + " (bpm=" + bpm0 + ")");
          log.debug("getBeatIntensity(): 2nd peak score=" + amplitude1 + " (bpm=" + bpm1 + ")");
          log.debug("getBeatIntensity(): min peak score=" + amplitudex + " (bpm=" + bpmx + ")");
//          log.debug("getBeatIntensity(): top peaks total=" + top_peak_total);
          log.debug("getBeatIntensity(): min score=" + min);
          double norm_flux = avg_flux / avg_energy;
          log.debug("getBeatIntensity(): avg flux=" + avg_flux + ", avg energy=" + avg_energy + ", normalized flux=" + norm_flux);
          int flux_cap = 2500;
          if (norm_flux > flux_cap) norm_flux = flux_cap;
          double flux_ratio = avg_flux / avgw; //((double)norm_flux) / flux_cap;
          double flux_ratio_cap = 25.0;
          if (flux_ratio > flux_ratio_cap) flux_ratio = flux_ratio_cap;
          double norm_total = totalamp - (results.size() * min);
          flux_ratio /= flux_ratio_cap;
          log.debug("getBeatIntensity(): average sample value=" + avgw + ", max=" + maxw + ", flux ratio=" + flux_ratio);
          log.debug("getBeatIntensity(): average score=" + average + ", std dev=" + std_dev + ", sum=" + totalamp + ", normalized sum=" + norm_total);
//          beat_intensity = (int)((amplitude0 - amplitude1) / (amplitude0 - amplitudex) * 100.0);
          double beat_strength = ((amplitude0 - average) / (amplitude0 - min) * 100.0);
          double spectral_intensity = flux_ratio * 100.0;
          log.debug("getBeatIntensity(): beat strength=" + beat_strength + ", spectral intensity=" + spectral_intensity);
          beat_intensity = (int)((beat_strength + spectral_intensity) / 2.0);
//          beat_intensity = (int)((top_peak_total - average) / top_peak_total * 100.0);
//          beat_intensity = (int)((amplitude0 - peak_average) / (amplitude0 - amplitudex) * 100.0);
          log.debug("getBeatIntensity(): beat intensity=" + beat_intensity);
      } catch (Exception e) { log.error("getBeatIntensity(): error", e);  }      
      return beat_intensity;
  }
  
  boolean beatproperties_determined = false;
  public void determineBeatProperties(SongColor scolor) throws Exception {
      if (beatproperties_determined) return;
      beatproperties_determined = true;
    normalize();

    for (int z = 0; z < 6; ++z) {
    avgenergy[z] /= energytrail[z].size();
    int lowenergy = 0;
    for (int i = 0; i < energytrail[z].size(); ++i) {
      float val = ((Float)energytrail[z].get(i)).floatValue();
      if (val < avgenergy[z]) lowenergy++;
    }

    scolor.avgcentroid[z] /= (float)centroidsamplesanalyzed[z];
    scolor.avgrolloff[z] /= samplesanalyzed;
    scolor.avgflux[z] /= samplesanalyzed;
    scolor.avgzerocrossings[z] /= samplesanalyzed;
    scolor.lowenergy[z] = ((float)lowenergy) / ((float)energytrail[z].size()) * 100.0f;

    for (int i = 0; i < centroidtrail[z].size(); ++i) {
      float val = ((Float)centroidtrail[z].get(i)).floatValue();
      scolor.variancecentroid[z] += (val - scolor.avgcentroid[z]) * (val - scolor.avgcentroid[z]);
    }
    scolor.variancecentroid[z] /= centroidtrail[z].size();
    scolor.variancecentroid[z] = (float)Math.sqrt(scolor.variancecentroid[z]);

    for (int i = 0; i < rollofftrail[z].size(); ++i) {
      int val = ((Integer)rollofftrail[z].get(i)).intValue();
      scolor.variancerolloff[z] += (val - scolor.avgrolloff[z]) * (val - scolor.avgrolloff[z]);
    }
    scolor.variancerolloff[z] /= rollofftrail[z].size();
    scolor.variancerolloff[z] = (int)Math.sqrt(scolor.variancerolloff[z]);

    for (int i = 0; i < zerocrossingtrail[z].size(); ++i) {
      int val = ((Integer)zerocrossingtrail[z].get(i)).intValue();
      scolor.variancezerocrossing[z] += (val - scolor.avgzerocrossings[z]) * (val - scolor.avgzerocrossings[z]);
    }
    scolor.variancezerocrossing[z] /= zerocrossingtrail[z].size();
    scolor.variancezerocrossing[z] = (int)Math.sqrt(scolor.variancezerocrossing[z]);

    for (int i = 0; i < fluxtrail[z].size(); ++i) {
      float val = ((Float)fluxtrail[z].get(i)).floatValue();
      scolor.varianceflux[z] += (val - scolor.avgflux[z]) * (val - scolor.avgflux[z]);
    }
    scolor.varianceflux[z] /= fluxtrail[z].size();
    scolor.varianceflux[z] = (float)Math.sqrt(scolor.varianceflux[z]);

    }


    Vector peaks = new Vector();
    double totalamp = 0.0;
    for (int i = 0; i < results.size(); ++i) {
      BPMRecord record = (BPMRecord)results.get(i);
      totalamp += record.score;
      BPMRecord record2 = null;
      BPMRecord record3 = null;
      if (i == 0) {
        record2 = (BPMRecord)results.get(i + 1);
        record3 = (BPMRecord)results.get(results.size() - 1);
      } else if (i == results.size() - 1) {
        record2 = (BPMRecord)results.get(results.size() - 2);
        record3 = (BPMRecord)results.get(0);
      } else {
        record2 = (BPMRecord)results.get(i - 1);
        record3 = (BPMRecord)results.get(i + 1);
      }
      boolean ispeak = false;
      if ((record2.score <= record.score) && (record3.score <= record.score)) ispeak = true;
      if (ispeak) {
        if (peaks.size() == 0) {
          peaks.add(record);
        } else {
          boolean inserted = false;
          int j = 0;
          while (!inserted && (j < peaks.size())) {
            BPMRecord r = (BPMRecord)peaks.get(j);
            if (record.score > r.score) {
              inserted = true;
              peaks.insertElementAt(record, j);
            }
            ++j;
          }
          if (!inserted) peaks.add(record);
        }
      }
    }
    try {
      scolor.period0 = ((BPMRecord)peaks.get(0)).bpm;
      scolor.amplitude0 = ((BPMRecord)peaks.get(0)).score / totalamp * 100.0;
      scolor.ratioperiod1 = ((BPMRecord)peaks.get(1)).bpm / ((BPMRecord)peaks.get(0)).bpm;
      if (scolor.ratioperiod1 > 1.0) scolor.ratioperiod1 = 1.0 / scolor.ratioperiod1;
      scolor.amplitude1 = ((BPMRecord)peaks.get(1)).score / totalamp * 100.0;
      scolor.ratioperiod2 = ((BPMRecord)peaks.get(2)).bpm / ((BPMRecord)peaks.get(0)).bpm;
      if (scolor.ratioperiod2 > 1.0) scolor.ratioperiod2 = 1.0 / scolor.ratioperiod2;
      scolor.amplitude2 = ((BPMRecord)peaks.get(2)).score / totalamp * 100.0;
      scolor.ratioperiod3 = ((BPMRecord)peaks.get(3)).bpm / ((BPMRecord)peaks.get(0)).bpm;
      if (scolor.ratioperiod3 > 1.0) scolor.ratioperiod3 = 1.0 / scolor.ratioperiod3;
      scolor.amplitude3 = ((BPMRecord)peaks.get(3)).score / totalamp * 100.0;
    } catch (Exception e) { log.error("determineBeatProperties(): error", e);  }
    double totalenergy = lowenergy + band1energy + band2energy + band3energy + band4energy  + highenergy;
    scolor.energy[0] = lowenergy / totalenergy * 100.0;
    scolor.energy[1] = band1energy / totalenergy * 100.0;
    scolor.energy[2] = band2energy / totalenergy * 100.0;
    scolor.energy[3] = band3energy / totalenergy * 100.0;
    scolor.energy[4] = band4energy / totalenergy * 100.0;
    scolor.energy[5] = highenergy / totalenergy * 100.0;
  }

  private double getRoundedBpm(Vector records, int index) {
    BPMRecord thisrecord = (BPMRecord)records.get(index);
    BPMRecord nextrecord = (index + 1) < records.size() ? (BPMRecord)records.get(index + 1) : null;
    BPMRecord lastrecord = (index - 1 >= 0) ? (BPMRecord)records.get(index - 1) : null;
    double bpmdiff = 0.0;
    if (nextrecord != null) bpmdiff = Math.abs(thisrecord.bpm - nextrecord.bpm);
    if (lastrecord != null) {
      double thisdiff = Math.abs(thisrecord.bpm - lastrecord.bpm);
      if (thisdiff > bpmdiff) bpmdiff = thisdiff;
    }
    double bpm = ((BPMRecord)records.get(index)).bpm;
    int decimalplaces = Bpm.extractDecimalPlaces(String.valueOf(bpmdiff));
    return Bpm.round(bpm, decimalplaces);
  }

  public DetectedBpm getBpm(AudioDecoder decoder) throws Exception {
    normalize();

    double max = 0.0;
    double min = 0.0;
    double detectedbpm = 0.0;
    log.debug("getBpm(): final count:");
    double minbpm1 = 0.0;
    double maxbpm1 = 0.0;
    for (int i = 0; i < results.size(); ++i) {
      BPMRecord record = (BPMRecord)results.get(i);
      log.debug("getBpm(): bpm: " + String.valueOf(record.bpm) + ", score: " + String.valueOf(record.score));
      if (i == 0) {
          min = record.score;
      } else {
          if (record.score < min) min = record.score;
      }
      if (record.score > max) {
        detectedbpm = getRoundedBpm(results, i);
        max = record.score;
        minbpm1 = record.bpm * 0.975;
        maxbpm1 = record.bpm * 1.025;
      }
    }

    double max2 = 0.0;
    double minbpm2 = 0.0;
    double maxbpm2 = 0.0;
    double bpm2 = 0.0;
    for (int i = 0; i < results.size(); ++i) {
      BPMRecord record = (BPMRecord)results.get(i);
      if ((record.score > max2) && ((record.bpm <= minbpm1) || (record.bpm >= maxbpm1))) {
        max2 = record.score;
        bpm2 = record.bpm;
        minbpm2 = record.bpm * 0.98;
        maxbpm2 = record.bpm * 1.02;
      }
    }

    double max3 = 0.0;
    double minbpm3 = 0.0;
    double maxbpm3 = 0.0;
    double bpm3 = 0.0;
    for (int i = 0; i < results.size(); ++i) {
      BPMRecord record = (BPMRecord)results.get(i);
      if ((record.score > max3) && ((record.bpm <= minbpm1) || (record.bpm >= maxbpm1)) && ((record.bpm <= minbpm2) || (record.bpm >= maxbpm2))) {
        max3 = record.score;
        bpm3 = record.bpm;
        minbpm3 = record.bpm * 0.99;
        maxbpm3 = record.bpm * 1.01;
      }
    }

    log.debug("getBpm(): *** results:");
    log.debug("getBpm(): 1st: " + String.valueOf(detectedbpm) + "bpm");
    log.debug("getBpm(): 2nd: " + String.valueOf(bpm2) + "bpm");
    log.debug("getBpm(): 3rd: " + String.valueOf(bpm3) + "bpm");

   double accuracy = 0.0;
   try {
       double total_score = 0.0;   
       for (int i = 0; i < results.size(); ++i) {
           BPMRecord record = (BPMRecord)results.get(i);
           total_score += record.score;       
       }
       double average = total_score / results.size();       
       double range = (max - min);
       accuracy = (max - average) / range;
   } catch (Exception e) {        }   
   
    if (OptionsUI.instance.bpmdetectionquality.getValue() == 0) return new DetectedBpm(detectedbpm, accuracy, getBeatIntensity());

//     double tempbpm = GetBpmFromFile2(detectbpmoutputfile, minbpm1, maxbpm1, minbpm2, maxbpm2, minbpm3, maxbpm3);
   double tempbpm = Bpm.GetBpmFromFile2(decoder, minbpm1, maxbpm1, 0.0,0.0,0.0,0.0, task);
   if (tempbpm != 0.0) detectedbpm = tempbpm;

    return new DetectedBpm(detectedbpm, accuracy, getBeatIntensity());
  }

  public double getOverallBpm() {
    double max = 0.0;
    double detectedbpm = 0.0;
    for (int i = 0; i < results.size(); ++i) {
      BPMRecord record = (BPMRecord)results.get(i);
      if (record.score > max) {
        detectedbpm = record.bpm;
        max = record.score;
      }
    }
    return detectedbpm;
  }

  public double CalculateSpectralSum(double minbpm, double[] finaldata) {
    boolean firsttime = false;
    if (results == null) {
      firsttime = true;
      results = new Vector();
    }
    int power = 1;
    int size = (int)Math.pow(2.0, power);
    while (size <= finaldata.length) {
      power++;
      size = (int)Math.pow(2.0, power);
    }
    power++;
    size = (int)Math.pow(2.0, power);
    int diff = size - finaldata.length;
    diff /= 2;

    float[] paddeddata = new float[size];
    for (int i = 0; i < diff; ++i) paddeddata[i] = 0.0f;
    for (int i = 0; i < finaldata.length; ++i) {
      paddeddata[i + diff] = (float) finaldata[i];
    }
    for (int i = diff + finaldata.length; i < size; ++i) paddeddata[i] = 0.0f;
    FastFourierTransform fft = new FastFourierTransform();
    paddeddata = fft.doFFT(paddeddata);

    float bpm = 0.0f;
    int block = 1;
    double maxvalue = 0.0f;
    float returnbpm = 0.0f;

    bpm = ((float)block) / ((float)paddeddata.length) * ((float)effectivesamplerate) * 60.0f;
    while (bpm <= maxbpm) {
      if (bpm >= minbpm) {
        int tempblock = block;
        double value = 0.0;
        int count = 0;
        double norm = 0.0;
        while ((tempblock < paddeddata.length / 2) && (count < 3)) {
          value += Math.abs(paddeddata[tempblock]);
          norm += 1;
          if (count == 0) {
            int tblock2 = tempblock / 2;
            int tblock3 = tempblock / 4;
            int tblock4 = tempblock / 8;
            if (tblock2 >= 1) {
              double tblock2a = ((double)tempblock) / 2.0;
              int tblock2b = tblock2;
              double alpha = 1.0;
              double beta = 0.0;
              if (tblock2a > (double)tblock2) {
                tblock2b = tblock2 + 1;
                beta = tblock2a - (double)tblock2;
                alpha = 1.0 - beta;
              } else if (tblock2a < (double)tblock2) {
                tblock2b = tblock2 - 1;
                beta = ((double)tblock2) - tblock2a;
                alpha = 1.0 - beta;
              }
              value += Math.abs(paddeddata[tblock2]) * alpha;
              value += Math.abs(paddeddata[tblock2b]) * beta;
              norm += 1;
            }
            if (tblock3 >= 1) {
              double tblock3a = ((double)tempblock) / 4.0;
              int tblock3b = tblock3;
              double alpha = 1.0;
              double beta = 0.0;
              if (tblock3a > (double)tblock3) {
                tblock3b = tblock3 + 1;
                beta = tblock3a - (double)tblock3;
                alpha = 1.0 - beta;
              } else if (tblock3a < (double)tblock3) {
                tblock3b = tblock3 - 1;
                beta = ((double)tblock3) - tblock3a;
                alpha = 1.0 - beta;
              }
              value += Math.abs(paddeddata[tblock3]) * alpha;
              value += Math.abs(paddeddata[tblock3b]) * beta;
              norm += 1;
            }
            if (tblock4 <= -1) {
              double tblock4a = ((double)tempblock) / 8.0;
              int tblock4b = tblock4;
              double alpha = 1.0;
              double beta = 0.0;
              if (tblock4a > (double)tblock4) {
                tblock4b = tblock4 + 1;
                beta = tblock4a - (double)tblock4;
                alpha = 1.0 - beta;
              } else if (tblock4a < (double)tblock4) {
                tblock4b = tblock4 - 1;
                beta = ((double)tblock4) - tblock4a;
                alpha = 1.0 - beta;
              }
              value += Math.abs(paddeddata[tblock4]) * alpha;
              value += Math.abs(paddeddata[tblock4b]) * beta;
              norm += 1;
            }
          }
          tempblock *= 2;
          count++;
        }
        value /= norm;
        if (Math.abs(value) > maxvalue) {
          maxvalue = Math.abs(value);
          returnbpm = bpm;
        }
//          if (RapidEvolution.debugmode) System.out.println("bpm: " + String.valueOf(bpm) + ", value: " + String.valueOf(value));
        if (firsttime) {
          BPMRecord bpmrecord = new BPMRecord();
          bpmrecord.bpm = bpm;
          bpmrecord.score = value;
          results.add(bpmrecord);
          if (firstblocksize == -1) firstblocksize = block;
        } else {
          BPMRecord bpmrecord = (BPMRecord)results.get(block - firstblocksize);
          bpmrecord.score += value;
        }
      }
      block++;
      bpm = ((float)block) / ((float)paddeddata.length) * ((float)effectivesamplerate) * 60.0f;
    }
    return returnbpm;
  }

  public double combFilter(double minbpm, double maxbpm) {
    int periodT = 1;
    double max = 0.0;
    double detectedbpm = 0.0;
    double amplitude = 1.0;
    boolean firsttime = false;
    if (results == null) {
      firsttime = true;
      results = new Vector();
    }
    double bpm = (effectivesamplerate / ((double)periodT)) * 60.0;
    while (bpm >= minbpm) {
      if (bpm <= maxbpm) {
        double lowtotal = 0.0;
        double lowmaxtotal = 0.0;
        double band1total = 0.0;
        double band1maxtotal = 0.0;
        double band2total = 0.0;
        double band2maxtotal = 0.0;
        double band3total = 0.0;
        double band3maxtotal = 0.0;
        double band4total = 0.0;
        double band4maxtotal = 0.0;
        double hightotal = 0.0;
        double highmaxtotal = 0.0;
        double normcount = 0.0;

//          double alpha = Math.pow(0.5, ((double)finallowpassdata.size()) / ((double)periodT));
        double alpha = 0.9;
        double[] lowoutput = new double[finallowpassdata.length];
        double[] band1output = new double[finallowpassdata.length];
        double[] band2output = new double[finallowpassdata.length];
        double[] band3output = new double[finallowpassdata.length];
        double[] band4output = new double[finallowpassdata.length];
        double[] highoutput = new double[finallowpassdata.length];
        for (int i = 0; i < finallowpassdata.length; ++i) {
          alpha = Math.pow(0.5, ((double)i) / ((double)periodT));
          if (i == 0) {
            lowtotal = (1.0 - alpha) * amplitude;
            lowoutput[i] = lowtotal;
            band1total = (1.0 - alpha) * amplitude;
            band1output[i] = band1total;
            band2total = (1.0 - alpha) * amplitude;
            band2output[i] = band2total;
            band3total = (1.0 - alpha) * amplitude;
            band3output[i] = band3total;
            band4total = (1.0 - alpha) * amplitude;
            band4output[i] = band4total;
            hightotal = (1.0 - alpha) * amplitude;
            highoutput[i] = hightotal;
          } else {
            if ((i - periodT) >= 0) {
              lowtotal = alpha * lowoutput[i - periodT] + (1.0 - alpha) * finallowpassdata[i];
              band1total = alpha * band1output[i - periodT] + (1.0 - alpha) * finalband1data[i];
              band2total = alpha * band2output[i - periodT] + (1.0 - alpha) * finalband1data[i];
              band3total = alpha * band3output[i - periodT] + (1.0 - alpha) * finalband1data[i];
              band4total = alpha * band4output[i - periodT] + (1.0 - alpha) * finalband1data[i];
              hightotal = alpha * highoutput[i - periodT] + (1.0 - alpha) * finalhighpassdata[i];
              normcount += 1.0;
 //             if (lowtotal > lowmaxtotal) lowmaxtotal = lowtotal;
//                if (band1total > band1maxtotal) band1maxtotal = band1total;
//                if (band2total > band2maxtotal) band2maxtotal = band2total;
//                if (band3total > band3maxtotal) band3maxtotal = band3total;
//                if (band4total > band4maxtotal) band4maxtotal = band4total;
//                if (hightotal > highmaxtotal) highmaxtotal = hightotal;
              lowmaxtotal += lowtotal;
              band1maxtotal += band1total;
              band2maxtotal += band2total;
              band3maxtotal += band3total;
              band4maxtotal += band4total;
              highmaxtotal += hightotal;
            } else {
              lowtotal = (1.0 - alpha) * finallowpassdata[i];
              band1total = (1.0 - alpha) * finalband1data[i];
              band2total = (1.0 - alpha) * finalband1data[i];
              band3total = (1.0 - alpha) * finalband1data[i];
              band4total = (1.0 - alpha) * finalband1data[i];
              hightotal = (1.0 - alpha) * finalhighpassdata[i];
            }
            lowoutput[i] = lowtotal;
            band1output[i] = band1total;
            band2output[i] = band2total;
            band3output[i] = band3total;
            band4output[i] = band4total;
            highoutput[i] = hightotal;
          }
        }
        double maxtotal = lowmaxtotal + band1maxtotal + band2maxtotal + band3maxtotal + band4maxtotal + highmaxtotal;
//          maxtotal /= normcount;
        log.debug("combFilter(): bpm: " + String.valueOf(bpm) + ", score: " + String.valueOf(maxtotal));
        if (firsttime) {
          BPMRecord bpmrecord = new BPMRecord();
          bpmrecord.bpm = bpm;
          bpmrecord.score = maxtotal;
          results.add(bpmrecord);
          if (firstblocksize == -1) firstblocksize = periodT;
        } else {
          BPMRecord bpmrecord = (BPMRecord)results.get(periodT - firstblocksize);
          bpmrecord.score += maxtotal;
        }
        if (maxtotal > max) {
          detectedbpm = bpm;
          max = maxtotal;
        }
      }
      periodT++;
      bpm = (effectivesamplerate / ((double)periodT)) * 60.0;
    }
    return detectedbpm;
  }

  class BPMRecord {
    public BPMRecord() { }
    public double score;
    public double bpm;
  }

  private double[] EnvelopeExtract(double[] decimateddata, Hanning hwindow, double[] finaldata) {
    double avgdiff = 0.0;
    for (int i = 0; i < decimateddata.length - 1; ++i) {
      double val = decimateddata[i];
      double val2 = decimateddata[i + 1];
      double diff = ((val2) - (val));
      if (diff < 0.0) diff = 0.0;
      finaldata[i] = diff;
      avgdiff += diff;
    }
    avgdiff /= (decimateddata.length - 1);
    double standarddeviation = 0.0;
    for (int i = 0; i < finaldata.length; ++i) {
      standarddeviation += (finaldata[i] - avgdiff) * (finaldata[i] - avgdiff);
    }
    standarddeviation /= finaldata.length;
    standarddeviation = Math.sqrt(standarddeviation);
    for (int i = 0; i < finaldata.length; ++i) {
      if (!(Math.abs(finaldata[i]) >= standarddeviation * 1.5)) finaldata[i] = 0;
    }
    return finaldata;
  }
}
