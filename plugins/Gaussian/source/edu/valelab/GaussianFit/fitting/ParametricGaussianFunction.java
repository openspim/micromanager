package edu.valelab.GaussianFit.fitting;

import edu.valelab.GaussianFit.utils.GaussianUtils;
import org.apache.commons.math.analysis.ParametricUnivariateRealFunction;

/**
 *
 * @author nico
 */
public class ParametricGaussianFunction implements ParametricUnivariateRealFunction {
   private int width_;
   private int height_;
   private int mode_;

   public ParametricGaussianFunction(int mode, int width, int height) {
      width_ = width;
      height_ = height;
      mode_ = mode;
   }

   public double value(double d, double[] doubles) {
      double value = 0;
      if (mode_ == 1)
         value =  GaussianUtils.gaussian(doubles, ((int) d) % width_, ((int) d) / width_);
      if (mode_ == 2)
          value =  GaussianUtils.gaussian2DXY(doubles, ((int) d) % width_, ((int) d) / width_);
      if (mode_ == 3)
          value =  GaussianUtils.gaussian2DEllips(doubles, ((int) d) % width_, ((int) d) / width_);
      return value;
   }

   public double[] gradient(double d, double[] doubles) {
      double[] value = {0.0};
      if (mode_ == 1)
         value =  GaussianUtils.gaussianJ(doubles, ((int) d) % width_, ((int) d) / width_);
      if (mode_ == 2)
          value =  GaussianUtils.gaussianJ2DXY(doubles, ((int) d) % width_, ((int) d) / width_);
      if (mode_ == 3)
          value =  GaussianUtils.gaussianJ2DEllips(doubles, ((int) d) % width_, ((int) d) / width_);
      return value;
   }

}
