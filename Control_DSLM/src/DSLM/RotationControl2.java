/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package DSLM;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import mmcorej.CMMCore;


/**
 *
 * @author Gualda
 */
public class RotationControl2 {
    private CMMCore core_;
    private String StateDev_;

    public RotationControl2(CMMCore core, String StateDev){
    core_ = core;
    StateDev_=StateDev;
    }
    String leftMove(String Step, String PositionIni) {
       
    NumberFormat formatter = new DecimalFormat("#.000");
        try{
            if ("360".equals(Step)) {	
                        core_.setState(StateDev_, 47);                               
            }
            if ("180".equals(Step)) {	
                        core_.setState(StateDev_, 3);                               
            }
            if ("90".equals(Step)) {
                        core_.setState(StateDev_, 5);
            }
            if ("45".equals(Step)) {
                        core_.setState(StateDev_, 7);
            }
            if ("18".equals(Step)) {
                        core_.setState(StateDev_, 9);
            }
            if ("9".equals(Step)) {
                        core_.setState(StateDev_, 11);
            }
           if ("1.8".equals(Step)) {
                        core_.setState(StateDev_, 13);
            }
           if ("0.9".equals(Step)) {
                        core_.setState(StateDev_, 41);
            }
           if ("0.45".equals(Step)) {
                        core_.setState(StateDev_, 43);
            } 
           if ("0.225".equals(Step)) {
                        core_.setState(StateDev_, 45);
            } 
           if ("5.4".equals(Step)) {
                        core_.setState(StateDev_, 15);
            }
       float STEP=Float.parseFloat(Step);
       float aux2=Float.parseFloat(PositionIni);
       aux2=aux2+STEP;                             
       if (aux2>=360){aux2=aux2-360;}
       String PositionFinal=formatter.format(aux2).replaceAll(",", ".");//Float.toString(aux2);

       return PositionFinal;       
      }
      catch (Exception ex) {
                        Logger.getLogger(DSLMcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
                        return("ERROR");
                    }             
    }
    
    String rightMove(String Step, String PositionIni) {
      
         NumberFormat formatter = new DecimalFormat("#.000");

        try{
           if ("360".equals(Step)) {	
                        core_.setState(StateDev_, 46);                               
            }
            if ("180".equals(Step)) {
                        core_.setState(StateDev_, 2);
            }
            if ("90".equals(Step)) {
                        core_.setState(StateDev_, 4);
            }
            if ("45".equals(Step)) {
                        core_.setState(StateDev_, 6);
            }
            if ("18".equals(Step)) {
                        core_.setState(StateDev_, 8);
            }
            if ("9".equals(Step)) {
                        core_.setState(StateDev_, 10);
            }
            if ("1.8".equals(Step)) {
                        core_.setState(StateDev_, 12);
            }
          
            if ("0.9".equals(Step)) {
                        core_.setState(StateDev_, 40);
            }
            if ("0.45".equals(Step)) {
                        core_.setState(StateDev_, 42);
            } 
            if ("0.225".equals(Step)) {
                        core_.setState(StateDev_, 44);
            } 
            if ("5.4".equals(Step)) {
                        core_.setState(StateDev_, 14);
            }
       float STEP=Float.parseFloat(Step);
       float aux2=Float.parseFloat(PositionIni);
        aux2=aux2-STEP;
       if (aux2<0){aux2=aux2+360;}
       //String PositionFinal=Integer.toString(aux2);   
       String PositionFinal=formatter.format(aux2).replaceAll(",", ".");
       return PositionFinal;
        }
      catch (Exception ex) {
                        Logger.getLogger(DSLMcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
                        return("ERROR");
                    }
        }

    }
    
  