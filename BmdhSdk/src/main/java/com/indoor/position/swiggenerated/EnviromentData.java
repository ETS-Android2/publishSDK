/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.2
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.indoor.position.swiggenerated;

public class EnviromentData {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected EnviromentData(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(EnviromentData obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  @SuppressWarnings("deprecation")
  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        IPSJNI.delete_EnviromentData(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setBluetoothCheckResult(boolean value) {
    IPSJNI.EnviromentData_bluetoothCheckResult_set(swigCPtr, this, value);
  }

  public boolean getBluetoothCheckResult() {
    return IPSJNI.EnviromentData_bluetoothCheckResult_get(swigCPtr, this);
  }

  public void setBluetoothReferLocation(double[] value) {
    IPSJNI.EnviromentData_bluetoothReferLocation_set(swigCPtr, this, value);
  }

  public double[] getBluetoothReferLocation() {
    return IPSJNI.EnviromentData_bluetoothReferLocation_get(swigCPtr, this);
  }

  public void setStepjingwei(double[] value) {
    IPSJNI.EnviromentData_stepjingwei_set(swigCPtr, this, value);
  }

  public double[] getStepjingwei() {
    return IPSJNI.EnviromentData_stepjingwei_get(swigCPtr, this);
  }

  public void setbluetoothCheckResult(boolean p) {
    IPSJNI.EnviromentData_setbluetoothCheckResult(swigCPtr, this, p);
  }

  public void setbluetoothReferLocation(double[] p) {
    IPSJNI.EnviromentData_setbluetoothReferLocation(swigCPtr, this, p);
  }

  public void setstepjingwei(double[] p) {
    IPSJNI.EnviromentData_setstepjingwei(swigCPtr, this, p);
  }

  public EnviromentData() {
    this(IPSJNI.new_EnviromentData__SWIG_0(), true);
  }

  public EnviromentData(boolean pbluetoothCheckResult, double[] pbluetoothReferLocation, double[] pstepjingwei) {
    this(IPSJNI.new_EnviromentData__SWIG_1(pbluetoothCheckResult, pbluetoothReferLocation, pstepjingwei), true);
  }

}
