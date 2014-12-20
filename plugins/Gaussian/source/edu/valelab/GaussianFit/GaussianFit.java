/**
 * Class that implements Gaussian fitting using the apache commons math library
 *
 * September 2010, Nico Stuurman
 */

package edu.valelab.GaussianFit;

import edu.valelab.GaussianFit.utils.GaussianUtils;
import edu.valelab.GaussianFit.fitting.MultiVariateGaussianFunction;
import edu.valelab.GaussianFit.fitting.MultiVariateGaussianMLE;
import edu.valelab.GaussianFit.fitting.ParametricGaussianFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.direct.NelderMeadSimplex;
import org.apache.commons.math.optimization.direct.SimplexOptimizer;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.apache.commons.math.optimization.SimpleScalarValueChecker;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.fitting.CurveFitter;

import ij.process.ImageProcessor;
import org.apache.commons.math.optimization.VectorialConvergenceChecker;
import org.apache.commons.math.optimization.VectorialPointValuePair;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;
import org.apache.commons.math.optimization.general.NonLinearConjugateGradientOptimizer;

/**
 *
 * @author nico
 */
public class GaussianFit {

   public static final int INT = 0;
   public static final int BGR = 1;
   public static final int XC = 2;
   public static final int YC = 3;
   public static final int S = 4;
   public static final int S1 = 4;
   public static final int S2 = 5;
   public static final int S3 = 6;


   double[] params0_;
   double[] steps_;
   public String [] paramNames_;

   short[] data_;
   int nx_;
   int ny_;
   int count_ = 0;
   int mode_ = 1;
   int fitMode_ = 1;

   NelderMeadSimplex nm_;
   SimpleScalarValueChecker convergedChecker_;
   MultiVariateGaussianFunction mGF_;
   MultiVariateGaussianMLE mGFMLE_;
   NonLinearConjugateGradientOptimizer nlcgo_;
   LevenbergMarquardtOptimizer lMO_;

	boolean estimateByMaximum;
	boolean hardCodedSteps;

   /**
    * Gaussian fit can be run by estimating parameter c (width of Gaussian)
    * as 1 (circle), 2 (width varies in x and y), or 3 (ellipse) parameters
    * 
    * @param mode - fit circle (1) ellipse(2), or ellipse with varying angle (3)
    * @param fitmode - algorithm use: NelderMead (1), Levenberg Marquard (2), 
    *                   NelderMean MLE (3), LevenberMarquard MLE(4)
    */
	public GaussianFit(int mode, int fitMode, boolean estByMax, boolean hardCodeStp) {
      super();
      fitMode_ = fitMode;
      if (mode == 1) {
         mode_ = 1;
         paramNames_ = new String[] {"A", "b", "x_c", "y_c", "sigma"};
      }
      if (mode == 2) {
         mode_ = 2;
         paramNames_ = new String[] {"A", "b", "x_c", "y_c", "sigmaX", "sigmaY"};
      }
      if (mode == 3) {
         mode_ = 3;
         paramNames_ = new String[] {"A", "b", "x_c", "y_c", "sigmaX", "sigmaY", "theta"};
      }
      params0_ = new double [mode_ + 4] ;
      steps_ = new double[mode_ + 4];

      if (fitMode_ == 1) {
//         nm_ = new NelderMeadSimplex(1);
         convergedChecker_ = new SimpleScalarValueChecker(1e-6,-1);
         mGF_ = new MultiVariateGaussianFunction(mode_);
      }
      // Levenberg-Marquardt and weighted Levenberg-Marquardt
      if (fitMode_ == 2 || fitMode == 4) {
         lMO_ = new LevenbergMarquardtOptimizer();
         LMChecker lmChecker = new LMChecker();
         lMO_.setConvergenceChecker(lmChecker);
      }
      if (fitMode_ == 3) {
         nm_ = new NelderMead();
         convergedChecker_ = new SimpleScalarValueChecker(1e-6,-1);
         mGFMLE_ = new MultiVariateGaussianMLE(mode_);
      }
      /*
       * Gradient MLE, not working very well
       *
      if (fitMode_ == 4) {
         mGFMLE_ = new MultiVariateGaussianMLE(mode_);
         nlcgo_ = 
            new NonLinearConjugateGradientOptimizer(ConjugateGradientFormula.FLETCHER_REEVES);
         convergedChecker_ = new SimpleScalarValueChecker(1e-6,-1);
         nlcgo_.setConvergenceChecker(convergedChecker_);
         nlcgo_.setInitialStep(0.0000002);
      }
      */

      estimateByMaximum = estByMax;
      hardCodedSteps = hardCodeStp;
   }

	public GaussianFit(int mode, int fitmode) {
		this(mode, fitmode, false, false);
	}

 
   /**
    * Performs Gaussian Fit on a given ImageProcessor
    * Estimates initial values for the fit and sends of to Apache fitting code
    * Background is estimated by averaging the outer rows and columns
    * Sigma estimate is hardcoded to 1.115 pixels
    * Signal estimate is derived from total signal - total background estimate
    * Steps sizes for the optimizer are set at 0.3 * the estimate values
    *
    * @param siProc - ImageJ ImageProcessor containing image to be fit
    * @param maxIterations - maximum number of iterations for the Nelder Mead
    *   optimization algorithm
    */
   public double[] doGaussianFit (ImageProcessor siProc, int maxIterations) {
      estimateParameters(siProc);

      double[] paramsOut = {0.0};

      if (fitMode_ == 1) {
      	 nm_ = new NelderMeadSimplex(steps_);
//         nm_.setStartConfiguration(steps_);
//         nm_.setConvergenceChecker(convergedChecker_);
//         nm_.setMaxIterations(maxIterations);
	 SimplexOptimizer opt = new SimplexOptimizer(convergedChecker_);
	 opt.setSimplex(nm_);
         mGF_.setImage((short[]) siProc.getPixels(), siProc.getWidth(), siProc.getHeight());
         try {
//            RealPointValuePair result = nm_.optimize(mGF_, GoalType.MINIMIZE, params0_);
	      RealPointValuePair result = opt.optimize(maxIterations, mGF_, GoalType.MINIMIZE, params0_);
            paramsOut = result.getPoint();
         } catch (java.lang.OutOfMemoryError e) {
            throw(e);
         } catch (Exception e) {
            ij.IJ.log(" " + e.toString());
            //e.printStackTrace();
         }
      }

      if (fitMode_ == 2 || fitMode_ == 4) {
         
         // lMO_.setMaxIterations(maxIterations);
         CurveFitter cF = new CurveFitter(lMO_);
         short[] pixels = (short[]) siProc.getPixels();
         if (fitMode_ == 2) {
            for (int i = 0; i < pixels.length; i++) {
               cF.addObservedPoint(i, (int) pixels[i] & 0xffff);
            }
         }
         if (fitMode_ == 4) {
            for (int i = 0; i < pixels.length; i++) {
               double factor = ((int) pixels[i] & 0xffff) ;
               cF.addObservedPoint(1 / factor, i, (int) pixels[i] & 0xffff);
            }
         }
         try {
            paramsOut = cF.fit(maxIterations, new ParametricGaussianFunction( mode_, siProc.getWidth(), siProc.getHeight() ),
                    params0_);
//         } catch (FunctionEvaluationException ex) {
//            Logger.getLogger(GaussianFit.class.getName()).log(Level.SEVERE, null, ex);
//         } catch (OptimizationException ex) {
//            Logger.getLogger(GaussianFit.class.getName()).log(Level.SEVERE, null, ex);
         } catch (IllegalArgumentException ex) {
            Logger.getLogger(GaussianFit.class.getName()).log(Level.SEVERE, null, ex);
         } catch(Exception ex) {
            Logger.getLogger(GaussianFit.class.getName()).log(Level.INFO, "Exceeded max number of iterations", ex);
         }
      }
      
      // Simplex-MLE
      if (fitMode_ == 3) {
         nm_.setStartConfiguration(steps_);
         nm_.setConvergenceChecker(convergedChecker_);
         nm_.setMaxIterations(maxIterations);
         mGFMLE_.setImage((short[]) siProc.getPixels(), siProc.getWidth(), siProc.getHeight());
         try {
            RealPointValuePair result = nm_.optimize(mGFMLE_, GoalType.MINIMIZE, params0_);
            paramsOut = result.getPoint();
         } catch (java.lang.OutOfMemoryError e) {
            throw(e);
         } catch (Exception e) {
            ij.IJ.log(" " + e.toString());
            //e.printStackTrace();
         }
      }
      
      /*
       * not working very well....
      // gradient-MLE
      if (fitMode_ == 4) {
         nlcgo_.setMaxIterations(maxIterations);
         mGFMLE_.setImage((short[]) siProc.getPixels(), siProc.getWidth(), siProc.getHeight());
         try {
            RealPointValuePair result = nlcgo_.optimize(mGFMLE_, GoalType.MINIMIZE, params0_);
            paramsOut = result.getPoint();
         } catch (java.lang.OutOfMemoryError e) {
            throw(e);
         } catch (Exception e) {
            ij.IJ.log(" " + e.toString());
         }
      }
      */
         
     
      
      if (mode_ == 3) {
         if (paramsOut.length > S3) {
            double[] prms = GaussianUtils.ellipseParmConversion(paramsOut[S1], paramsOut[S2], paramsOut[S3]);
            paramsOut[S1] = prms[1];
            paramsOut[S2] = prms[2];
            paramsOut[S3] = prms[0];
         }
      }

      return paramsOut;
   }


   private void estimateParameters(ImageProcessor siProc) {
      short[] imagePixels = (short[]) siProc.getPixels();

      // Hard code estimate for sigma (expressed in pixels):
      params0_[S] = 0.9;
      if (mode_ >= 2) {
         params0_[S2] = 0.9;
      }
      if (mode_ == 3) {
         params0_[S1] = 1;
         params0_[S2] = 0;
         params0_[S3] = 1;
      }
      double bg = 0.0;
      int n = 0;
      int lastRowOffset = (siProc.getHeight() - 1) * siProc.getWidth();
      for (int i = 0; i < siProc.getWidth(); i++) {
         bg += (imagePixels[i] & 0xffff);
         bg += (imagePixels[i + lastRowOffset] & 0xffff);
         n += 2;
      }
      for (int i = 1; i < siProc.getHeight() - 1; i++) {
         bg += (imagePixels[i * siProc.getWidth()] & 0xffff);
         bg += (imagePixels[(i + 1) * siProc.getWidth() - 1] & 0xffff);
         n += 2;
      }
      params0_[BGR] = (bg /= n);
      // estimate signal by subtracting background from total intensity
      // print("Total signal: " + ti + "Estimate: " + params0_[0]);
      // estimate center of mass
      double mt = 0.0;
      double mx = 0.0;
      double my = 0.0;
      for (int i = 0; i < siProc.getHeight() * siProc.getWidth(); i++) {
         // mx += (imagePixels[i] - params0_[4]) * (i % siProc.getWidth() );
         // my += (imagePixels[i] - params0_[4]) * (Math.floor (i /
         // siProc.getWidth()));
         double weight = (imagePixels[i] & 0xffff) - bg;

         if(params0_[INT] < weight) {
            params0_[INT] = weight;
            params0_[XC] = i % siProc.getWidth();
            params0_[YC] = i / siProc.getWidth();
         }

         if(!estimateByMaximum) {
            mt += (int) weight;
            mx += (int) weight * (i % siProc.getWidth());
            my += (int) weight * (i / siProc.getWidth());
         }
      }
      if(!estimateByMaximum) {
         params0_[INT] = mt / (2 * Math.PI * params0_[S] * params0_[S]);
         params0_[XC] = mx / mt;
         params0_[YC] = my / mt;
      };
      // ij.IJ.log("Centroid: " + mx/mt + " " + my/mt);
      // set step size during estimate
      if(hardCodedSteps) {
         steps_[INT] = 0.5; //params0_[INT]*0.3;
         steps_[BGR] = params0_[BGR]*0.3;
         steps_[XC] = 0.1;
         steps_[YC] = 0.1;
      }

      for (int i = (hardCodedSteps ? S : 0); i < params0_.length; ++i) {
         steps_[i] = params0_[i] * 0.3;
         if (steps_[i] == 0)
            steps_[i] = 0.1;
      }
   }
   
   
   private class LMChecker implements VectorialConvergenceChecker {
      int iteration_ = 0;
      boolean lastResult_ = false;
      public boolean converged(int i, VectorialPointValuePair previous, VectorialPointValuePair current) {
         if (i == iteration_)
            return lastResult_;
         
         iteration_ = i;
         double[] p = previous.getPoint();
         double[] c = current.getPoint();
         
         if ( Math.abs(p[INT] - c[INT]) < 10  &&
                 Math.abs(p[BGR] - c[BGR]) < 2 &&
                 Math.abs(p[XC] - c[XC]) < 0.1 &&
                 Math.abs(p[YC] - c[YC]) < 0.1 &&
                 Math.abs(p[S] - c[S]) < 5 ) {
            lastResult_ = true;
            return true;
         }
         
         lastResult_ = false;
         return false;
      }
   }

}
