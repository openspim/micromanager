package edu.valelab.GaussianFit.fitting;

import org.apache.commons.math.exception.MathIllegalArgumentException;
import org.apache.commons.math.exception.util.DummyLocalizable;
import org.apache.commons.math.analysis.MultivariateRealFunction;

/**
 *
 * @author Nico Stuurman
 * 
 * Function as defined in Bo Huang Science paper
 * 
 * minimize the distance D in sqrt wx and sqrt wy space
 * D = sqrt (  square (sqrt wx - sqrt wx, calib) + sqr(sqrt wy - sqrt wx, calib) )
 * 
 * where wx and wy calib are functions depended on z using functions defined in zCalibrator
 *
 * 
 */
/**
 *
 * @author nico
 */
public class MultiVariateZFunction implements MultivariateRealFunction {
   
   private double[] fitFunctionWx_;
   private double[] fitFunctionWy_;
   private double wx_;
   private double wy_;
   
   
   public MultiVariateZFunction (double[] fitFunctionWx, double[] fitFunctionWy,
           double wx, double wy) {
      fitFunctionWx_ = fitFunctionWx;
      fitFunctionWy_ = fitFunctionWy;
      wx_ = wx;
      wy_ = wy;
   }
   
   
   /**
    * 
    * @param - array of double with function parameters where:
    *       0: z

    *       
    * @throws MathIllegalArgumentException
    * @return
    */
   public double value(double[] params) throws MathIllegalArgumentException {
      if (params.length < 1)
         throw new MathIllegalArgumentException(new DummyLocalizable(""));
     
      return funcval(params);
   }
   
   /**
    * actual function evaluation
    * 
    * D = sqrt (  square (sqrt wx - sqrt wx, calib) + 
    *             sqr(sqrt wy - sqrt wx, calib) )
    * 
    */
   public double funcval(double[] params) {
      
      double x = Math.sqrt(wx_) - Math.sqrt(
              MultiVariateZCalibrationFunction.funcval(fitFunctionWx_, params[0]));
      double y = Math.sqrt(wy_) - Math.sqrt(
              MultiVariateZCalibrationFunction.funcval(fitFunctionWy_, params[0]));
      
      return Math.sqrt(x * x + y * y);
   }
   
   
}
