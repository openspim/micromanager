/*
 *This pluging controls the SPIM/DSLM/OPT Microscope
 * 
 */
package DSLM;

import ij.IJ;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import mmcorej.CMMCore;
import mmcorej.PropertyType;
import mmcorej.StrVector;
import org.micromanager.MMStudioMainFrame;
import org.micromanager.api.AcquisitionEngine;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.JavaUtils;

/**
 *
 * @author Dr. Gualda
 */
public class DSLM_controlsDialog extends javax.swing.JFrame implements ActionListener, Runnable{
    
    private DSLMcontrolsPlugin plugin_;
    private CMMCore core_;
    
    private RotationControl2 RC_;
   // private StackControl SC_;
    private ShutterControl ShC_;
    private FilterControl FC_;
    private final Preferences prefs_;
    private String DIALOG_POSITION = "dialogPosition";
    
    private MMStudioMainFrame app_;
    private AcquisitionEngine acq_;
    
///////////////  Creates new form DSLM CONTROL Dialog //////////////////////
    
    DSLM_controlsDialog(DSLMcontrolsPlugin plugin, CMMCore core, AcquisitionEngine acq, ScriptInterface app) throws FileNotFoundException, IOException {
        plugin_ = plugin;
        core_=core;
        acq_=acq;
        app_=(MMStudioMainFrame) app;
        initComponents();
        AcquireButton.addActionListener(this);
        prefs_ = Preferences.userNodeForPackage(this.getClass());
    }

 /////////////////// ACQUISITION OF THE IMAGES  ///////////////////////////
    
    public void actionPerformed(ActionEvent e){
  // Create a thread and execute the kill-time-code
  // without blockiing the window
          
    Thread worker = new Thread(this);    
    worker.start();  // this calls the method run()
  }

    public void run(){
        acq_.clear();
        acq_.setSaveFiles(false);
               
        int numFrames=1;
        int slices=1;
        int channels=1;
        int positions=1;
            
        String focusdevice=core_.getFocusDevice();
        String xyStage = core_.getXYStageDevice();
        core_.setAutoShutter(false);

        //////////// This is only to detect the Focus device position property!!!! ///////
        // IF appears in metadata this can be deleted ////
       StrVector devicePropertyNames;  
       String position_property="";
       NumberFormat formatter2 = new DecimalFormat("#.####");
        try {
            devicePropertyNames = core_.getDevicePropertyNames(focusdevice);
     
             for(int x=0;x<devicePropertyNames.size();x++){
                 if (!core_.isPropertyReadOnly(focusdevice, devicePropertyNames.get(x))){
                //double position = core_.getPosition(focusdevice);
                String property_value = core_.getProperty(focusdevice, devicePropertyNames.get(x));
                String Position=formatter2.format(core_.getPosition(focusdevice)).replaceAll(",", ".");
               if (property_value.compareTo(Position)==0){ position_property=devicePropertyNames.get(x);}
              MessageTextArea.setText(property_value+"  "+Position);
              //core_.sleep(5000);                      
                                       }
             }
           } catch (Exception ex) {
            Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
             MessageTextArea.setText("Error in device detection");
        }
        /////////////////////////////////////////////////
        
//////////////// Mode Configuration  /////////////        
        
 String Mode=(String) ModeComboBox.getSelectedItem();    

///////////   Save Files Configuration //////////////             
        String Filename="test";
        //String Rootname="D:\\DSLM_DATA\\SPIM_V10";
        String Rootname=RootDirectoryField.getText();
        boolean issaved=false;
        
        if(SaveCheckBox.isSelected()){
            Rootname=RootDirectoryField.getText();
            Filename=FileNameField.getText();
            MessageTextArea.setText(Rootname+"\\" +Filename);
            
            issaved=true;
            
            acq_.setDirName(Filename);
            acq_.setRootName(Rootname);
            acq_.setSaveFiles(true);
            acq_.setComment(MessageTextArea.getText());
        }

//////////  Sample Rotation Configuration  ///////////   
   String RotPosition=AnglePositionField.getText();
   String RotStep=(String)RotationComboBox.getSelectedItem();
   if ("DSLM/SPIM".equals(Mode)){     
        if(ShutterRotationCheckBox.isSelected()==true){acq_.keepShutterOpenForChannels(false);}
        else{acq_.keepShutterOpenForChannels(true);}
        float RStep=Float.parseFloat(RotStep);// parsedouble(RotStep);
        positions=1;
        if (RStep==0){positions=1;}
        else {positions =(int) ((int) 360/RStep);}
   } 
     else if ("OPT".equals(Mode)){
         MessageTextArea.setText("OPT Mode");
  
  }

///////////////  Stack Configuration  //////////////////   
     
        NumberFormat formatter = new DecimalFormat("#.0000");
        double ZINI=0;
        double ZSTEP=0.5;
        
   if ("DSLM/SPIM".equals(Mode)){
        double bottom=Double.parseDouble(StartField.getText().replaceAll(",", "."));
        double top=Double.parseDouble(EndField.getText().replaceAll(",", "."));
        double step=Double.parseDouble(StepField.getText().replaceAll(",", "."));
        boolean absolute=true;
        String ZStep=StepField.getText();
        ZSTEP=Double.parseDouble(ZStep.replaceAll(",", "."));
        String ZIni=StartField.getText();
        ZINI=Double.parseDouble(ZIni.replaceAll(",", "."));
        acq_.enableZSliceSetting(false);
       
        if(ShutterStackCheckBox.isSelected()==true){acq_.keepShutterOpenForStack(false);}
        else{acq_.keepShutterOpenForStack(true);}
        
        if (StackCheckBox.isSelected()){
            acq_.enableZSliceSetting(true);
            acq_.setSlices(bottom,top,step,absolute);
                                   
            double aux=(top-bottom)/step;
            slices=(int)Math.ceil(aux)+1;
            ZIni=StartField.getText();
            ZINI=Double.parseDouble(ZIni.replaceAll(",", "."));
        }
        else{
            //acq_.setSingleFrame(true);//
            acq_.enableZSliceSetting(false);
            //acq_.setSlices(bottom,bottom,step,absolute);
            slices=1;
            ZIni=PositionField.getText();
            ZINI=Double.parseDouble(ZIni.replaceAll(",", "."));
        }   
   }
  else if ("OPT".equals(Mode)){
        if(ShutterRotationCheckBox.isSelected()==true){acq_.keepShutterOpenForChannels(false);}
        else{acq_.keepShutterOpenForChannels(true);}
        float RStep=Float.parseFloat(RotStep);
        slices=1;
        if (RStep==0){slices=1;}
        else {slices =(int) ((int) 360/RStep);}
        MessageTextArea.setText("Slices are: "+Integer.toString(slices));
        core_.sleep(2000);
  }

        
////////////////  Time Lapse Configuration  ////////////////////   
        Object numFramesS=TimeFrames.getValue();
        numFrames=((Integer) numFramesS).intValue();
        double interval=Double.parseDouble(IntervalTime.getText());
        String units=(String)IntervalUnits.getSelectedItem();
        acq_.enableFramesSetting(false);
        double intervalms=0;
        
        if (TimeLapseCheckBox.isSelected()){
            //acq_.setSingleFrame(false);
            acq_.enableFramesSetting(true);
           if ("s".equals(units)) {intervalms=interval*1000;}
           else if ("min".equals(units)) {intervalms=interval*60000;}
           else if ("ms".equals(units)) {intervalms=interval;}            
            acq_.setFrames(numFrames,intervalms);
        }
        else{
            numFrames=1;
            acq_.enableFramesSetting(true);
            acq_.setFrames(numFrames,interval);
            //acq_.setSingleFrame(true);
        }
                      
//////////// Channels configuration    /////////////////// 
       String channelname[] = new String[6];
       String Excitation[] = new String[6];
       String Emission[] = new String[6];
       String Exposure[] = new String[6];
       String Excitation_filters[]= new String[6];
       String Emission_filters[]= new String[6];
       
       for(int n=0;n<6;n++){
                Excitation_filters[n]=(String)Excitation_ComboBox.getItemAt(n);
                Emission_filters[n]=(String)Emission_ComboBox.getItemAt(n);
                    }
        if (Channel_CheckBox.isSelected()){
            channels=0;
                for(int n=0;n<6;n++){
                    if(((Boolean)Channels_table.getValueAt(n, 4)).booleanValue()==true){                           
                        channelname[channels]=(String)Channels_table.getValueAt(n, 0);
                        Excitation[channels]=(String)Channels_table.getValueAt(n, 1);
                        Emission[channels]=(String)Channels_table.getValueAt(n, 2);                       
                        Exposure[channels]=(String)Channels_table.getValueAt(n, 3);
                        channels=channels+1;                         
                    }               
                }
            MessageTextArea.setText("Number of channels is"+Integer.toString(channels));
            
        }
        else{
             channels=1;
             channelname[0] = "Channel_0";
            try {               
                Exposure[0]=Double.toString(core_.getExposure());
            } catch (Exception ex) {
                MessageTextArea.setText("ERROR IN GETTING EXPOSURE");
                Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
             Excitation[0]=(String)Excitation_ComboBox.getSelectedItem();
             Emission[0]=(String)Emission_ComboBox.getSelectedItem();            
        }
   
          
         
////////////////  ACQUISITION IN DSLM/SPIM Mode //////////////////                  
    if ("DSLM/SPIM".equals(Mode)){
            try {
                boolean isshown=true; 
                app_.openAcquisition(Filename, Rootname, numFrames, channels, slices, positions, isshown, issaved);

   /////////////////////  RENAME CHANNELS  //////////////////
                
        int[] Excitation_Filters_Index={0,0,0,0,0,0};
        int[] Emission_Filters_Index={0,0,0,0,0,0};
               for(int n=0;n<channels;n++){                     
                        app_.setChannelName(Filename, n, channelname[n]);
                                                  
                     for(int m=0;m<6;m++){
                                if(Excitation_filters[m].equalsIgnoreCase(Excitation[n])){
                                    Excitation_Filters_Index[n]=m;
                                }
                                 if(Emission_filters[m].equalsIgnoreCase(Emission[n])){
                                    Emission_Filters_Index[n]=m;
                                }
                       }  
            }
            
   /////////////////////  RENAME POSITION LIST  //////////////////

          /*  int numpos=0;
            String NUMPOS;
            PositionList pl = new PositionList();

                for(int n=0;n<positions;n++){                    
                if (positions==2){  
                        String [] positionname={"000","180"};                    
                        MultiStagePosition msp = new MultiStagePosition(xyStage, n, n, focusdevice, ZINI);    
                        String N =Integer.toString(n);
                        msp.setLabel(positionname[n]);//
                        pl.addPosition(msp);                 
                        app_.setPositionList(pl);         
                } 
                else if (positions==1){
                        String [] positionname={"000"};                    
                        MultiStagePosition msp = new MultiStagePosition(xyStage, n, n, focusdevice, ZINI);    
                        String N =Integer.toString(n);
                        msp.setLabel(positionname[n]);//
                        pl.addPosition(msp);                 
                        app_.setPositionList(pl);          
                }
                else if (positions==4){
                        String [] positionname={"000","090","180","270"};
                        MultiStagePosition msp = new MultiStagePosition(xyStage, n, n, focusdevice, ZINI);    
                        String N =Integer.toString(n);
                        msp.setLabel(positionname[n]);//
                        pl.addPosition(msp);                 
                        app_.setPositionList(pl);    
                } 
                else if (positions==8){
                        String [] positionname={"000","045","090","135","180","225","270","315"};
                        MultiStagePosition msp = new MultiStagePosition(xyStage, n, n, focusdevice, ZINI);    
                        String N =Integer.toString(n);
                        msp.setLabel(positionname[n]);//
                        pl.addPosition(msp);                 
                        app_.setPositionList(pl);    
                }
            }
                        numpos=pl.getNumberOfPositions();
                        NUMPOS=Integer.toString(numpos);
                       MultiStagePosition msp2= pl.getPosition(1);
                      String Label= msp2.getLabel();
                        //MessageTextArea.setText("number of positions:"+NUMPOS+Label);
                      */
//////////////////////  ACQUIRE  /////////////////////////////////////
                
                
            //String Pos;
            //Pos=SC_.setPos(ZIni);
        try {
           core_.setPosition(focusdevice,ZINI);
           core_.sleep(100);
           String Position_new=formatter.format(core_.getPosition(focusdevice));
           PositionField.setText(Position_new);
        } catch (Exception ex) {
            Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("ERROR IN GOING TO INITIAL POSITION");
        }
            long TIni;
            long TEnd;
            long TotalT;
            long Intervalms=(long)intervalms;
                         
       for (int n=0;n<(numFrames);n++){
                TIni=System.currentTimeMillis();            
                   if (Global.shutter==0){ShC_.openshutter(); Global.shutter=1;}
                    for(int m=0;m<(positions);m++){
                        if (Global.shutter==0){ShC_.openshutter(); Global.shutter=1;}
                for (int o=0; o<(channels);o++){
                          //Change filter
                          String CIEx=FC_.ChangeExcitationFilter(Excitation_Filters_Index[o]);
                          Excitation_ComboBox.setSelectedIndex(Excitation_Filters_Index[o]);
                          String CIEm=FC_.ChangeEmissionFilter(Emission_Filters_Index[o]);
                          Emission_ComboBox.setSelectedIndex(Emission_Filters_Index[o]);                           
                         try {
                            core_.setExposure(Double.parseDouble(Exposure[o]));
                        } catch (Exception ex) {
                            MessageTextArea.setText("ERROR IN SETTING EXPOSURE");
                            Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
                        }
                          MessageTextArea.setText("The filter is: "+Integer.toString(Emission_Filters_Index[o]+1)+"/n"+"The CIEm is: "+ CIEm);
                          //core_.sleep(1000); 
                          
                        for(int l=0;l<(slices);l++){
                            if (ShutterStackCheckBox.isSelected()==true){ShC_.openshutter();Global.shutter=1;MessageTextArea.setText("shutter ON");}  
                            else{MessageTextArea.setText("shutter OFF");}
                            if(app_.acquisitionExists(Filename)){
                                    app_.snapAndAddImage(Filename, n, o, l, m);  //n,m,l
                            }
                            else {n=numFrames;m=positions;l=slices;}
                                if(l!=slices-1){
                                    if (ShutterStackCheckBox.isSelected()==true){ShC_.closeshutter();Global.shutter=0;}
                                    try {
                                        double position = core_.getPosition(focusdevice); 
                                        double position_new=position+ZSTEP;
                                       
                                        String Position_new0=Double.toString(position_new).replace(",",".");//
                                        MessageTextArea.setText(Position_new0);
                                        

                                       
                                       ///position_property="Set position (microns)";
                                        core_.setProperty(focusdevice,position_property,Position_new0);//
                                        
                                        core_.sleep(50);//
                                        //core_.setPosition(focusdevice,position_new);
                                        String Position_new=formatter.format(core_.getPosition(focusdevice));
                                        PositionField.setText(Position_new);
                                      // app_.setStagePosition(position_new);
                                        } catch (Exception ex) {
                                            Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
                                            MessageTextArea.setText("ERROR IN MOVING");
                                        }
                                }

                            }
                        
                         try {
                            core_.setPosition(focusdevice,ZINI);
                            core_.sleep(100);
                             String Position_new=formatter.format(core_.getPosition(focusdevice));
                            PositionField.setText(Position_new);
                        } catch (Exception ex) {
                             Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
                        }
                } //channels
                        if(positions>1){ShC_.openshutter();Global.shutter=1;}
                       
                        RotPosition=RC_.leftMove(RotStep,RotPosition);                        
                        AnglePositionField.setText(RotPosition);
                         if(ShutterRotationCheckBox.isSelected()==true){ShC_.closeshutter();Global.shutter=0;}
                        }
                    
                    TEnd=System.currentTimeMillis();
                    TotalT=TEnd-TIni;
                    MessageTextArea.setText(String.valueOf(TotalT));
                   if (StackCheckBox.isSelected()==true){ShC_.closeshutter();Global.shutter=0;MessageTextArea.setText("shutter OFF");}
                   if (ShutterRotationCheckBox.isSelected()==true){ShC_.closeshutter();Global.shutter=0;MessageTextArea.setText("shutter OFF");}
                   else if(ShutterRotationCheckBox.isSelected()==false){ShC_.openshutter();Global.shutter=1;MessageTextArea.setText("shutter ON");}
                   if (TotalT<Intervalms && n!=(numFrames-1)){
                            app_.sleep(Intervalms-TotalT);
                        }
                    //wait until
                   
                    }
                ShC_.closeshutter();Global.shutter=0;MessageTextArea.setText("shutter OFF");
                app_.closeAcquisition(Filename);
                FC_.ChangeExcitationFilter(Excitation_Filters_Index[0]);
                Excitation_ComboBox.setSelectedIndex(Excitation_Filters_Index[0]);
                FC_.ChangeEmissionFilter(Emission_Filters_Index[0]);
                Emission_ComboBox.setSelectedIndex(Emission_Filters_Index[0]);
            
            } catch (Exception ex) {
                Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
                MessageTextArea.setText("Error in DSLM/SPIM Mode");
            }
        }//END IF
    
////////////////  ACQUISITION IN OPT Mode (Slices are angles)  //////////////////   
    else if("OPT".equals(Mode)){
          try {
                boolean isshown=true; //slices, positions              
                app_.openAcquisition(Filename, Rootname, numFrames, channels, slices, isshown, issaved);
   /////////////////////  RENAME CHANNELS  //////////////////

        int[] Excitation_Filters_Index={0,0,0,0,0,0};
        int[] Emission_Filters_Index={0,0,0,0,0,0};
               for(int n=0;n<channels;n++){                     
                        app_.setChannelName(Filename, n, channelname[n]);
                       // app_.setChannelColor(Filename, n, channelcolor[n]);    
                        
                     for(int m=0;m<6;m++){
                                if(Excitation_filters[m].equalsIgnoreCase(Excitation[n])){
                                    Excitation_Filters_Index[n]=m;
                                }
                                 if(Emission_filters[m].equalsIgnoreCase(Emission[n])){
                                    Emission_Filters_Index[n]=m;
                                }
                       }  
            }                
                
            long TIni;
            long TEnd;
            long TotalT;
            long Intervalms=(long)intervalms;

//////////////////////  ACQUIRE  /////////////////////////////////////
       
           for (int n=0;n<(numFrames);n++){
                TIni=System.currentTimeMillis();               
                      for (int o=0; o<(channels);o++){
                          //Change filter                           
                          String CIEx=FC_.ChangeExcitationFilter(Excitation_Filters_Index[o]);
                          String CIEm=FC_.ChangeEmissionFilter(Emission_Filters_Index[o]);
                        try {
                            core_.setExposure(Double.parseDouble(Exposure[o]));
                        } catch (Exception ex) {
                            MessageTextArea.setText("ERROR IN SETTING EXPOSURE");
                            Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
                        }
                          MessageTextArea.setText("The filter is: "+Integer.toString(Emission_Filters_Index[o]+1)+"/n"+"The CIEm is: "+ CIEm);
                        
              
                        ShC_.openshutter();    
                        Global.shutter=1;                    
                        for(int l=0;l<(slices);l++){

                            
                            if(app_.acquisitionExists(Filename)){
                                    app_.sleep(150);
                                    app_.snapAndAddImage(Filename, n, o, l);  //n,m,l                                     
                            }
                            else {n=numFrames;l=slices;}
                                if(l!=slices-1){
                                    //Pos=SC_.setPosUp(Pos,ZStep);                                       
                                }                                

                            RotPosition=RC_.leftMove(RotStep,RotPosition);                   
                            AnglePositionField.setText(RotPosition);
                            }//slices
                            ShC_.closeshutter(); 
                            Global.shutter=0;
                    }//channels
                                     
                    TEnd=System.currentTimeMillis();
                    TotalT=TEnd-TIni;
                    //MessageTextArea.setText(String.valueOf(TotalT));
                   
                   if (TotalT<Intervalms && n!=(numFrames-1)){
                            app_.sleep(Intervalms-TotalT);
                        }
                    //wait until
                   
                    }//Frames

                
            app_.closeAcquisition(Filename);
                    
           } catch (Exception ex) {
                Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
                MessageTextArea.setText("Error in OPT Mode");
            }           
        }//END ELSE
   }

   
        

    
 //FROM HERE
     private void initComponents() throws FileNotFoundException, IOException {
  
 /// DEVICE DEFINITION  /////    
    String focusdevice=core_.getFocusDevice();
    String xyStage = core_.getXYStageDevice();    
    String ShutterDev=core_.getShutterDevice();
    String StateDev="Arduino_SR-Switch";
    String StateDev_FW="Arduino_FW-Switch";
    String ShutterDev_FW="Arduino_FW-Shutter";
         
           try {
            core_.setProperty(StateDev,"State","32");     
            core_.setProperty(ShutterDev,"OnOff","1");
            core_.setProperty(ShutterDev,"OnOff","0");
       } catch (Exception ex) {
            Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
        
         
         RC_=new RotationControl2(core_,StateDev);
        //SC_=new StackControl(core_);
        ShC_=new ShutterControl(core_,StateDev, ShutterDev);
        FC_=new FilterControl(core_,StateDev_FW, ShutterDev_FW);
        
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("OPEN SPIN MICROSCOPY (SPIM/DSLM/OPT) CONTROL");
        setResizable(false);
        DSLM_controlsDialog.this.setSize(600,606);
       
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        //MAIN ACTION BUTTONS
        
        AcquireButton = new javax.swing.JButton();
        ShutterButton = new javax.swing.JButton();
        
        AcquireButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        AcquireButton.setText("Acquire!");
       // AcquireButton.addActionListener(new java.awt.event.ActionListener() {
           // public void actionPerformed(java.awt.event.ActionEvent evt) {
           //     AcquireButtonActionPerformed(evt);
           // }
        //});

        ShutterButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        ShutterButton.setText("Shutter");
        ShutterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                try {
                    ShutterButtonActionPerformed(evt);
                } catch (Exception ex) {
                    Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
                    MessageTextArea.setText("Error in Shutter");  
                }
            }
        });
        
        /////// MODE SELECTION  //////
        
        ModePanel = new javax.swing.JPanel();
        ModeComboBox = new javax.swing.JComboBox();
        ModeLabel = new javax.swing.JLabel();
        
         ModePanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        ModeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "DSLM/SPIM", "OPT" }));
        ModeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                try {
                    ModeActionPerformed(evt);
                } catch (Exception ex) {
                    Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
                    MessageTextArea.setText("Error in MODE");  
                }
            }
        });
        

        ModeLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        ModeLabel.setText("Mode");

    javax.swing.GroupLayout ModePanelLayout = new javax.swing.GroupLayout(ModePanel);
        ModePanel.setLayout(ModePanelLayout);
        ModePanelLayout.setHorizontalGroup(
            ModePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ModePanelLayout.createSequentialGroup()
                .addComponent(ModeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(ModeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        ModePanelLayout.setVerticalGroup(
            ModePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ModePanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(ModePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ModeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ModeLabel))
                .addContainerGap())
        );
        
        
        
        //Time Lapse options
        TimeLapsePanel = new javax.swing.JPanel();
        TimeLapseCheckBox = new javax.swing.JCheckBox();
        TimeFrames = new javax.swing.JSpinner();
        IntervalTime = new javax.swing.JTextField();
        IntervalUnits = new javax.swing.JComboBox();
        NumberLabel = new javax.swing.JLabel();
        IntervalLabel = new javax.swing.JLabel();
        
        TimeLapsePanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        TimeLapseCheckBox.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        TimeLapseCheckBox.setSelected(false);
        TimeLapseCheckBox.setText("Time Lapse");

        TimeLapseCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TimeLapseCheckBoxActionPerformed(evt);
            }
        });
        
        NumberLabel.setText("Number");
        NumberLabel.setEnabled(false);
        IntervalLabel.setText("Interval");
        IntervalLabel.setEnabled(false);
             
        TimeFrames.setToolTipText("Number of time frames");
        TimeFrames.setName("");
        TimeFrames.setValue(2);
        TimeFrames.setEnabled(false);

        IntervalTime.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        IntervalTime.setText("100");
        IntervalTime.setEnabled(false);
        IntervalTime.setToolTipText("Interval between stack of images (in ms, s or min)");
        
        IntervalUnits.setEnabled(false);
        IntervalUnits.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ms", "s", "min" }));

        javax.swing.GroupLayout TimeLapsePanelLayout = new javax.swing.GroupLayout(TimeLapsePanel);
        TimeLapsePanel.setLayout(TimeLapsePanelLayout);
        TimeLapsePanelLayout.setHorizontalGroup(
            TimeLapsePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(TimeLapsePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(TimeLapsePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(TimeLapsePanelLayout.createSequentialGroup()
                        .addComponent(NumberLabel)
                        .addGap(18, 18, 18)
                        .addComponent(TimeFrames))
                    .addGroup(TimeLapsePanelLayout.createSequentialGroup()
                        .addComponent(IntervalLabel)
                        .addGap(18, 18, 18)
                        .addComponent(IntervalTime, javax.swing.GroupLayout.DEFAULT_SIZE, 48, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(IntervalUnits, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(14, 14, 14))
            .addGroup(TimeLapsePanelLayout.createSequentialGroup()
                .addComponent(TimeLapseCheckBox)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        TimeLapsePanelLayout.setVerticalGroup(
            TimeLapsePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(TimeLapsePanelLayout.createSequentialGroup()
                .addComponent(TimeLapseCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(TimeLapsePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, TimeLapsePanelLayout.createSequentialGroup()
                        .addComponent(NumberLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(IntervalLabel))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, TimeLapsePanelLayout.createSequentialGroup()
                        .addComponent(TimeFrames, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(TimeLapsePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(IntervalUnits, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(IntervalTime))))
                .addContainerGap())
        );
        
       //Stack Options
        
        StackPanel = new javax.swing.JPanel();
        StackCheckBox = new javax.swing.JCheckBox();
        StartField = new javax.swing.JTextField();
        SetStartButton = new javax.swing.JButton();
        EndField = new javax.swing.JTextField();
        SetEndButton = new javax.swing.JButton();
        StepField = new javax.swing.JTextField();
        StartLabel = new javax.swing.JLabel();
        EndLabel = new javax.swing.JLabel();
        StepLabel = new javax.swing.JLabel();
        StackComboBox = new javax.swing.JComboBox();
        ShutterStackCheckBox = new javax.swing.JCheckBox();
        
        StackPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        StackCheckBox.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        StackCheckBox.setSelected(false);
        StackCheckBox.setText("Stack of Images");

        StackCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StackCheckBoxActionPerformed(evt);
            }
        });


        StartLabel.setText("Start (microns)");
        StartLabel.setEnabled(false);
        EndLabel.setText("End (microns)");
        EndLabel.setEnabled(false);
        StepLabel.setText("Steps (microns)");
        StepLabel.setEnabled(false);
        StackComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] {  "Absolute Z", "Relative Z" }));
        StackComboBox.setEnabled(false);
        ShutterStackCheckBox.setText("Use Shutter");
        ShutterStackCheckBox.setEnabled(false);
        ShutterStackCheckBox.setSelected(false);
        
        StartField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        StartField.setText("0");
        //String StackPosition=SC_.getPos();
        String StackPosition="ERROR";
        try {            
            double pos=core_.getPosition(focusdevice);
            NumberFormat formatter = new DecimalFormat("#.0000");
            StackPosition=formatter.format(pos);
        } catch (Exception ex) {
            Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
        StartField.setText(StackPosition);
        StartField.setEnabled(false);
        StartField.setToolTipText("Starting position the stack in microns");

        EndField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        EndField.setText("10");
        EndField.setText(StackPosition);
        EndField.setEnabled(false);
        EndField.setToolTipText("Finishing position of the stack in microns");

        StepField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        StepField.setText("5");
        StepField.setEnabled(false);
        StepField.setToolTipText("Steps in micros between images of the stack");

        SetEndButton.setText("Set");
        SetEndButton.setEnabled(false);
            SetEndButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SetEndButtonActionPerformed(evt);
            }
        });
        SetStartButton.setText("Set");
        SetStartButton.setEnabled(false);
            SetStartButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SetStartButtonActionPerformed(evt);
            }
        });
        
        javax.swing.GroupLayout StackPanelLayout = new javax.swing.GroupLayout(StackPanel);
        StackPanel.setLayout(StackPanelLayout);
        StackPanelLayout.setHorizontalGroup(
            StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(StackPanelLayout.createSequentialGroup()
                .addComponent(StackCheckBox)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(StackPanelLayout.createSequentialGroup()
                .addGroup(StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(StackPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(StepLabel)
                            .addComponent(EndLabel)
                            .addComponent(StartLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addGroup(StackPanelLayout.createSequentialGroup()
                                    .addComponent(StartField, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(SetStartButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGroup(StackPanelLayout.createSequentialGroup()
                                    .addComponent(EndField, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(SetEndButton)))
                            .addComponent(StepField, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(StackComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(StackPanelLayout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addComponent(ShutterStackCheckBox)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        StackPanelLayout.setVerticalGroup(
            StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(StackPanelLayout.createSequentialGroup()
                .addComponent(StackCheckBox)
                .addGap(9, 9, 9)
                .addGroup(StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(StartField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(SetStartButton))
                    .addComponent(StartLabel, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(EndField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(SetEndButton))
                    .addComponent(EndLabel, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(StepField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(StepLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(StackComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ShutterStackCheckBox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        
        //Rotation options 
        
        RotationPanel = new javax.swing.JPanel();
        RotationSampleLabel = new javax.swing.JLabel();
        RotationComboBox = new javax.swing.JComboBox();
        RotationLabel = new javax.swing.JLabel();
        ShutterRotationCheckBox = new javax.swing.JCheckBox();
        RotationComboBox.setEnabled(true);
        RotationLabel.setEnabled(true);
        ShutterRotationCheckBox.setEnabled(true);
        ShutterRotationCheckBox.setSelected(false);
        
        RotationPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        RotationSampleLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        RotationSampleLabel.setText("Rotation");
        
        RotationLabel.setText("Rotation angle");
        ShutterRotationCheckBox.setText("Use Shutter");
        
        RotationComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "0","45", "90", "180","360","1.8","0.9","0.45","0.225" }));
        RotationComboBox.setToolTipText("Select the rotation angle for the automatic recording");
                 
        javax.swing.GroupLayout RotationPanelLayout = new javax.swing.GroupLayout(RotationPanel);
        RotationPanel.setLayout(RotationPanelLayout);
        RotationPanelLayout.setHorizontalGroup(
            RotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RotationPanelLayout.createSequentialGroup()
                .addGroup(RotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, RotationPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(RotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(RotationPanelLayout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(ShutterRotationCheckBox))
                            .addGroup(RotationPanelLayout.createSequentialGroup()
                                .addComponent(RotationLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(RotationComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(RotationPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(RotationSampleLabel)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        RotationPanelLayout.setVerticalGroup(
            RotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RotationPanelLayout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(RotationSampleLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(RotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(RotationComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(RotationLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 9, Short.MAX_VALUE)
                .addComponent(ShutterRotationCheckBox))
        );

        //Save options
        SaveImagesPanel = new javax.swing.JPanel();
        SaveCheckBox = new javax.swing.JCheckBox();
        RootDirectoryField = new javax.swing.JTextField();
        RootLabel = new javax.swing.JLabel();
        DirectoryButton = new javax.swing.JButton();
        FileNameField = new javax.swing.JTextField();
        FileLabel = new javax.swing.JLabel();
        fc = new javax.swing.JFileChooser();


        
        SaveImagesPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        SaveCheckBox.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        SaveCheckBox.setSelected(true);
        SaveCheckBox.setText("Save Images");
        SaveCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveCheckBoxActionPerformed(evt);
            }
        });
        
        DirectoryButton.setText("...");
        DirectoryButton.setEnabled(true);
        DirectoryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DirectoryButtonActionPerformed(evt);
            }
        });
                
        RootDirectoryField.setText("D:\\DSLM_DATA\\SPIM_V10");
        RootDirectoryField.setEnabled(true);
        RootLabel.setText("Root Directory");
        RootLabel.setEnabled(true);
        FileNameField.setText("Sample");
        FileNameField.setEnabled(true);
        FileLabel.setText("File name");
        FileLabel.setEnabled(true);
       
       javax.swing.GroupLayout SaveImagesPanelLayout = new javax.swing.GroupLayout(SaveImagesPanel);
        SaveImagesPanel.setLayout(SaveImagesPanelLayout);
        SaveImagesPanelLayout.setHorizontalGroup(
            SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SaveImagesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(SaveCheckBox)
                    .addGroup(SaveImagesPanelLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(RootLabel)
                            .addComponent(FileLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(RootDirectoryField, javax.swing.GroupLayout.DEFAULT_SIZE, 246, Short.MAX_VALUE)
                            .addComponent(FileNameField))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(DirectoryButton)))
                .addGap(13, 13, 13))
        );
        SaveImagesPanelLayout.setVerticalGroup(
            SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SaveImagesPanelLayout.createSequentialGroup()
                .addComponent(SaveCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(RootDirectoryField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(RootLabel)
                    .addComponent(DirectoryButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(FileNameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(FileLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        
      
      // MESSAGE PANEL
        
        Message_Pannel = new javax.swing.JPanel();
        Message_scroll_pane = new javax.swing.JScrollPane();
        MessageTextArea = new javax.swing.JTextArea();
        MessageLabel = new javax.swing.JLabel();
        
         Message_Pannel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        Message_Pannel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N

        MessageTextArea .setColumns(20);
        MessageTextArea .setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        MessageTextArea .setRows(2);
        Message_scroll_pane.setViewportView(MessageTextArea);

        MessageLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        MessageLabel.setText("Messages");

         javax.swing.GroupLayout Message_PannelLayout = new javax.swing.GroupLayout(Message_Pannel);
        Message_Pannel.setLayout(Message_PannelLayout);
        Message_PannelLayout.setHorizontalGroup(
            Message_PannelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Message_PannelLayout.createSequentialGroup()
                .addGroup(Message_PannelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(MessageLabel)
                    .addComponent(Message_scroll_pane, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        Message_PannelLayout.setVerticalGroup(
            Message_PannelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, Message_PannelLayout.createSequentialGroup()
                .addComponent(MessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Message_scroll_pane, javax.swing.GroupLayout.DEFAULT_SIZE, 65, Short.MAX_VALUE))
        );
        

     //STAGE CONTROL
        StageControlPanel = new javax.swing.JPanel();
        UPUPButton = new javax.swing.JButton();
        UPButton = new javax.swing.JButton();
        DownButton = new javax.swing.JButton();
        DownDownButton = new javax.swing.JButton();
        PositionField = new javax.swing.JTextField();
        PositionLabel = new javax.swing.JLabel();
        UpUpField = new javax.swing.JTextField();
        UpField = new javax.swing.JTextField();
        StageControlLabel = new javax.swing.JLabel();
        
        StageControlPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        PositionLabel.setText("Position");
        StageControlLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        StageControlLabel.setText("Stage Control");
        
        UPUPButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        UPUPButton.setText("˄˄");
        UPUPButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UPUPButtonActionPerformed(evt);
            }
        });

        UPButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        UPButton.setText("˄");
        UPButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UPButtonActionPerformed(evt);
            }
        });

        DownButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        DownButton.setText("˅");
        DownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DownButtonActionPerformed(evt);
            }
        });

        DownDownButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        DownDownButton.setText("˅˅");
        DownDownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DownDownButtonActionPerformed(evt);
            }
        });

        PositionField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        PositionField.setText("0");
        //String StackPosition=SC_.getPos();
        PositionField.setText(StackPosition);
        PositionField.setEditable(false);
        PositionField.setBackground(new java.awt.Color(230, 230, 230));
        
        UpUpField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        UpUpField.setText("100");

        UpField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        UpField.setText("10");

       javax.swing.GroupLayout StageControlPanelLayout = new javax.swing.GroupLayout(StageControlPanel);
        StageControlPanel.setLayout(StageControlPanelLayout);
        StageControlPanelLayout.setHorizontalGroup(
            StageControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, StageControlPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(StageControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(StageControlLabel)
                    .addGroup(StageControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, StageControlPanelLayout.createSequentialGroup()
                            .addComponent(UpUpField, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(UPUPButton))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, StageControlPanelLayout.createSequentialGroup()
                            .addComponent(UpField, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(StageControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(UPButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(DownButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(DownDownButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, StageControlPanelLayout.createSequentialGroup()
                            .addComponent(PositionLabel)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(PositionField, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        StageControlPanelLayout.setVerticalGroup(
            StageControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(StageControlPanelLayout.createSequentialGroup()
                .addComponent(StageControlLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(StageControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(UPUPButton)
                    .addComponent(UpUpField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(StageControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(UPButton)
                    .addComponent(UpField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(StageControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(PositionField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(PositionLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(DownButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(DownDownButton)
                .addContainerGap(15, Short.MAX_VALUE))
        );

       //ROTATION CONTROL
        
        RotationControlPanel = new javax.swing.JPanel();
        RotationControlLabel = new javax.swing.JLabel();
        LeftButton = new javax.swing.JButton();
        RightButton = new javax.swing.JButton();
        AnglePositionField = new javax.swing.JTextField();
        AngleLabel = new javax.swing.JLabel();
        MakeZeroButton = new javax.swing.JButton();
        StepAngleComboBox = new javax.swing.JComboBox();
        StepAngleLabel = new javax.swing.JLabel();  
        LineCheckBox = new javax.swing.JCheckBox();
        
        RotationControlPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        RotationControlLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        RotationControlLabel.setText("Rotation Control");
        AnglePositionField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        AnglePositionField.setText("180.000");
        AngleLabel.setText("Angle");
        StepAngleLabel.setText("Step");
       
        MakeZeroButton.setText("Make Zero");
        MakeZeroButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MakeZeroButtonActionPerformed(evt);
            }
        });

        LeftButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        LeftButton.setText("<");
        LeftButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                try {
                    LeftButtonActionPerformed(evt);
                } catch (Exception ex) {
                    Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
                    MessageTextArea.setText("Error in LeftButton");  
                }
            }
        });

        RightButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        RightButton.setText(">");
        RightButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                try {
                    RightButtonActionPerformed(evt);
                } catch (Exception ex) {
                    Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
                    MessageTextArea.setText("Error in RightButton");  
                }
            }
        });

        StepAngleComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] {  "0","45", "90", "180","360","18","9","1.8","0.9","0.45","0.225"  }));
        
        LineCheckBox.setText("Line");
        LineCheckBox.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        LineCheckBox.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        LineCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LineCheckBoxActionPerformed(evt);
            }
        });

         javax.swing.GroupLayout RotationControlPanelLayout = new javax.swing.GroupLayout(RotationControlPanel);
        RotationControlPanel.setLayout(RotationControlPanelLayout);
        RotationControlPanelLayout.setHorizontalGroup(
            RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, RotationControlPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(LeftButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(AngleLabel)
                    .addGroup(RotationControlPanelLayout.createSequentialGroup()                       
                        .addComponent(AnglePositionField, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(RightButton)))
                .addGap(1, 1, 1))
            .addGroup(RotationControlPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(RotationControlPanelLayout.createSequentialGroup()
                        .addComponent(RotationControlLabel)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(RotationControlPanelLayout.createSequentialGroup()
                        .addGroup(RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(RotationControlPanelLayout.createSequentialGroup()
                                .addGap(8, 8, 8)
                                .addComponent(StepAngleLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(StepAngleComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, RotationControlPanelLayout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(MakeZeroButton)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(LineCheckBox)))
                .addContainerGap())
        );
        RotationControlPanelLayout.setVerticalGroup(
            RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RotationControlPanelLayout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addComponent(RotationControlLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(RotationControlPanelLayout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addComponent(AngleLabel)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(RotationControlPanelLayout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(LeftButton)
                            .addComponent(AnglePositionField)
                            .addComponent(RightButton))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(RotationControlPanelLayout.createSequentialGroup()
                        .addGroup(RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(StepAngleLabel)
                            .addComponent(StepAngleComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(MakeZeroButton))
                    .addComponent(LineCheckBox)))
        );
         // GALVO CONTROLS ////
        
        GalvoControlPanel = new javax.swing.JPanel();
        GalvoCheckBox = new javax.swing.JCheckBox();
        GAmpTextField = new javax.swing.JTextField();
        GAmpLabel = new javax.swing.JLabel();
        GFreqTextField = new javax.swing.JTextField();
        GFreqLabel = new javax.swing.JLabel();
        GResetButton = new javax.swing.JButton();
        GHomeButton = new javax.swing.JButton();
        ChannelControlPanel = new javax.swing.JPanel();
        
         GalvoControlPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        GalvoCheckBox.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        GalvoCheckBox.setText("Galvo");
        GalvoCheckBox.setSelected(false);
        GalvoCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GalvoCheckBoxActionPerformed(evt);
            }
        });

        GAmpTextField.setText("2");
        GAmpTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        GAmpLabel.setText(" Amp. (V)");
        GFreqTextField.setText("100");
        GFreqTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        GFreqLabel.setText("Freq. (Hz)");
        GAmpTextField.setEnabled(false);
        GAmpLabel.setEnabled(false);
        GFreqTextField.setEnabled(false);
        GFreqLabel.setEnabled(false);                
                        
        GResetButton.setText("   MOVE   ");
        GResetButton.setFont(new java.awt.Font("Tahoma", 1, 8)); // NOI18N    
        GResetButton.setEnabled(false);
        GResetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GResetButtonActionPerformed(evt);
            }
        });
        
        GHomeButton.setText("  SET  ");
        GHomeButton.setFont(new java.awt.Font("Tahoma", 1, 8)); // NOI18N  
        GHomeButton.setEnabled(false);
        GHomeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GHomeButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout GalvoControlPanelLayout = new javax.swing.GroupLayout(GalvoControlPanel);
        GalvoControlPanel.setLayout(GalvoControlPanelLayout);
        GalvoControlPanelLayout.setHorizontalGroup(
            GalvoControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(GalvoControlPanelLayout.createSequentialGroup()
                .addComponent(GalvoCheckBox)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(GalvoControlPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(GalvoControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(GalvoControlPanelLayout.createSequentialGroup()
                        .addComponent(GResetButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(GHomeButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(GalvoControlPanelLayout.createSequentialGroup()
                        .addGroup(GalvoControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(GFreqLabel)
                            .addComponent(GAmpLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(GalvoControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(GAmpTextField)
                            .addComponent(GFreqTextField))
                        .addContainerGap())))
        );
        GalvoControlPanelLayout.setVerticalGroup(
            GalvoControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(GalvoControlPanelLayout.createSequentialGroup()
                .addComponent(GalvoCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(GalvoControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(GAmpTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(GAmpLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(GalvoControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(GFreqTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(GFreqLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(GalvoControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(GResetButton, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(GHomeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(5, 5, 5))
        );
        
        
        //FILTER CONTROLS
        
        ChannelControlPanel = new javax.swing.JPanel();
        ChannelControlPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        Channel_CheckBox = new javax.swing.JCheckBox();
        Channel_CheckBox.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        Channel_CheckBox.setText("Channels");
        
        Channel_CheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ChannelCheckBoxActionPerformed(evt);
            }
        });
  
String[] Excitation_filters={ "Empty01", "488/10", "568/10", "488/568", "647/10", "Empty02" };
String[] Emission_filters={ "535/70", "580/25", "620/90", "640/25", "SP 700", "700/75" };
String Channels[]=new String [6];
////////////////////////////// READ FROM FILE ///////////
         try{    
            String current= System.getProperty("user.dir");
            File file = new File(current + "\\mmplugins\\Config_EX_file.txt");
            //create FileReader object from File object
            FileReader fr = new FileReader(file);
            //create BufferedReader object from FileReader to read file line by line
            BufferedReader reader = new BufferedReader(fr);
        //// READ EXCITATION FILTERS ////    
            String line = reader.readLine();
           for(int nn=0;nn<6;nn++){         
            line = reader.readLine();
            Excitation_filters[nn]=line;
           }
         //// READ EMISSION FILTERS ////    
            line = reader.readLine();
           for(int nn=0;nn<6;nn++){         
            line = reader.readLine();
            Emission_filters[nn]=line;
           }
            line= reader.readLine();
            line= reader.readLine();
           for(int nn=0;nn<6;nn++){         
            line = reader.readLine();
            Channels[nn]=line;
           }

           
         }catch (FileNotFoundException e) {        
            MessageTextArea.setText("Error in Config_Ex_file");
        } catch (IOException e) {
            MessageTextArea.setText("Error in Config_Ex_file");
        }
         
           String[] CH0=Channels[0].split(":");
           String[] CH1=Channels[1].split(":");
           String[] CH2=Channels[2].split(":");
           String[] CH3=Channels[3].split(":");
           String[] CH4=Channels[4].split(":");
           String[] CH5=Channels[5].split(":");
////////////////////////////////////////////////
         
        Excitation_ComboBox = new javax.swing.JComboBox(Excitation_filters);
        Emission_ComboBox = new javax.swing.JComboBox(Emission_filters);
        
        Excitation_ComboBox2 = new javax.swing.JComboBox(Excitation_filters);
        Emission_ComboBox2 = new javax.swing.JComboBox(Emission_filters);
       
        Excitation_label = new javax.swing.JLabel();
        Emission_label = new javax.swing.JLabel();
        
        ChannelScrollPane = new javax.swing.JScrollPane();
        Channels_table = new javax.swing.JTable();
        ChannelScrollPane.setViewportView(Channels_table);
        Channels_table.setEnabled(false);
        ChannelScrollPane.setEnabled(false);
        Channels_table.setVisible(false);
                  
        Emission_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Emission_ComboBoxActionPerformed(evt);
            }
        });
        
        Excitation_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Excitation_ComboBoxActionPerformed(evt);
            }
        });
        Excitation_label.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        Excitation_label.setText("Excitation");

        Emission_label.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        Emission_label.setText("Emission");

        Channels_table.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        //String[] tonto=Channels[0].split(":");
        Channels_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {CH0[0],CH0[1],CH0[2],CH0[3],Boolean.parseBoolean(CH0[4])},
                {CH1[0],CH1[1],CH1[2],CH1[3],Boolean.parseBoolean(CH1[4])},
                {CH2[0],CH2[1],CH2[2],CH2[3],Boolean.parseBoolean(CH2[4])},
                {CH3[0],CH3[1],CH3[2],CH3[3],Boolean.parseBoolean(CH3[4])},
                {CH4[0],CH4[1],CH4[2],CH4[3],Boolean.parseBoolean(CH4[4])},
                {CH5[0],CH5[1],CH5[2],CH5[3],Boolean.parseBoolean(CH5[4])}
                /*{"GFP", Excitation_filters[1], Emission_filters[0], "100", true},              
                {"RFP", Excitation_filters[2], Emission_filters[2], "100", true},
                {"YFP", Excitation_filters[0], Emission_filters[2],"100", false},
                {"CFP", Excitation_filters[1], Emission_filters[3], "100", false},
                {"Other1", Excitation_filters[2], Emission_filters[4], "100", false},
                {"Other2", Excitation_filters[3], Emission_filters[5], "100", false}*/             
                
            },
            new String [] {
                "Name", "Excitation", "Emission", "Exposure", "ON/OFF"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.String.class, java.lang.Boolean.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        
        Channels_table.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        Channels_table.setRowHeight(13);

        
        Channels_table.getColumnModel().getColumn(1).setCellEditor(new javax.swing.DefaultCellEditor(Excitation_ComboBox2));
        Channels_table.getColumnModel().getColumn(2).setCellEditor(new javax.swing.DefaultCellEditor(Emission_ComboBox2));
        

        javax.swing.GroupLayout ChannelControlPanelLayout = new javax.swing.GroupLayout(ChannelControlPanel);
        ChannelControlPanel.setLayout(ChannelControlPanelLayout);
        ChannelControlPanelLayout.setHorizontalGroup(
            ChannelControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ChannelControlPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(Channel_CheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ChannelScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 321, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(ChannelControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(ChannelControlPanelLayout.createSequentialGroup()
                        .addComponent(Excitation_label)
                        .addGap(18, 18, 18)
                        .addComponent(Emission_label))
                    .addGroup(ChannelControlPanelLayout.createSequentialGroup()
                        .addComponent(Excitation_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Emission_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(0, 13, Short.MAX_VALUE))
        );
        ChannelControlPanelLayout.setVerticalGroup(
            ChannelControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(ChannelScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(ChannelControlPanelLayout.createSequentialGroup()
                .addComponent(Channel_CheckBox)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(ChannelControlPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ChannelControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Excitation_label)
                    .addComponent(Emission_label))
                .addGap(5, 5, 5)
                .addGroup(ChannelControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Excitation_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Emission_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(28, Short.MAX_VALUE))
        );


        
     //// GENERAL LAYOUT
         
                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(StageControlPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(RotationControlPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(ModePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(TimeLapsePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(RotationPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(StackPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(GalvoControlPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(AcquireButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(ShutterButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(SaveImagesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Message_Pannel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(ChannelControlPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(RotationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(StageControlPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(TimeLapsePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                            .addComponent(AcquireButton, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(ShutterButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(GalvoControlPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(StackPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(ChannelControlPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(SaveImagesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(Message_Pannel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(RotationControlPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(ModePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(66, Short.MAX_VALUE))
        );
        
         java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-1000)/2, (screenSize.height-100)/2, (screenSize.width+0)/2, (screenSize.height+0)/2);
        DSLM_controlsDialog.this.setSize(738,410);
    }// </editor-fold>


    // Variables declaration - do not modify
    private javax.swing.JButton AcquireButton;
    private javax.swing.JButton ShutterButton;

    /// SAVE IMAGES PANEL
    private javax.swing.JPanel SaveImagesPanel;
    private javax.swing.JCheckBox SaveCheckBox;
    private javax.swing.JButton DirectoryButton;
    private javax.swing.JTextField RootDirectoryField;
    private javax.swing.JLabel RootLabel;
    private javax.swing.JLabel FileLabel;
    private javax.swing.JTextField FileNameField;
    private javax.swing.JFileChooser fc; 
    
    /// STAGE CONTROL PANEL
    private javax.swing.JPanel StageControlPanel;
    private javax.swing.JLabel StageControlLabel;
    private javax.swing.JButton DownButton;
    private javax.swing.JButton DownDownButton;
    private javax.swing.JButton UPButton;
    private javax.swing.JButton UPUPButton;
    private javax.swing.JTextField UpField;
    private javax.swing.JTextField UpUpField;
    private javax.swing.JTextField PositionField;
    private javax.swing.JLabel PositionLabel;  
    
    // TIME LAPSE PANEL
    private javax.swing.JPanel TimeLapsePanel; 
    private javax.swing.JSpinner TimeFrames;
    private javax.swing.JCheckBox TimeLapseCheckBox;
    private javax.swing.JLabel IntervalLabel;
    private javax.swing.JTextField IntervalTime;
    private javax.swing.JComboBox IntervalUnits; 
    private javax.swing.JLabel NumberLabel;
    
    // ROTATION PANEL   
    private javax.swing.JPanel RotationPanel;
    private javax.swing.JLabel RotationSampleLabel;
    private javax.swing.JComboBox RotationComboBox;
    private javax.swing.JLabel RotationLabel;
    private javax.swing.JCheckBox ShutterRotationCheckBox;
    
    // STACK PANEL
    private javax.swing.JPanel StackPanel;
    private javax.swing.JTextField StartField;
    private javax.swing.JLabel StartLabel;
    private javax.swing.JTextField EndField;
    private javax.swing.JLabel EndLabel;
    private javax.swing.JTextField StepField;
    private javax.swing.JLabel StepLabel;
    private javax.swing.JButton SetEndButton;
    private javax.swing.JButton SetStartButton;
    private javax.swing.JCheckBox ShutterStackCheckBox;
    private javax.swing.JCheckBox StackCheckBox;
    private javax.swing.JComboBox StackComboBox;

    //ROTATION CONTROL PANEL
    private javax.swing.JPanel RotationControlPanel;    
    private javax.swing.JLabel RotationControlLabel;
    private javax.swing.JCheckBox LineCheckBox;
    private javax.swing.JButton MakeZeroButton;
    private javax.swing.JButton RightButton;
    private javax.swing.JButton LeftButton;
    private javax.swing.JLabel StepAngleLabel;
    private javax.swing.JComboBox StepAngleComboBox;
    private javax.swing.JLabel AngleLabel;
    private javax.swing.JTextField AnglePositionField; 
   
    /// GALVO CONTROL PANEL
    private javax.swing.JLabel GAmpLabel;
    private javax.swing.JTextField GAmpTextField;
    private javax.swing.JLabel GFreqLabel;
    private javax.swing.JTextField GFreqTextField;
    private javax.swing.JButton GResetButton;
    private javax.swing.JCheckBox GalvoCheckBox;
    private javax.swing.JPanel GalvoControlPanel;
    private javax.swing.JButton GHomeButton;
    
    // MODE PANEL
    private javax.swing.JComboBox ModeComboBox;
    private javax.swing.JLabel ModeLabel;
    private javax.swing.JPanel ModePanel;
    
    // CHANNELS PANEL
    private javax.swing.JPanel ChannelControlPanel;
    private javax.swing.JScrollPane ChannelScrollPane;
    private javax.swing.JCheckBox Channel_CheckBox;
    private javax.swing.JComboBox Excitation_ComboBox;
    private javax.swing.JLabel Excitation_label;
    private javax.swing.JComboBox Excitation_ComboBox2;
    private javax.swing.JComboBox Emission_ComboBox2; 
    private javax.swing.JComboBox Emission_ComboBox;
    private javax.swing.JLabel Emission_label;
    private javax.swing.JTable Channels_table;
    
    //MESSAGE PANEL
    private javax.swing.JPanel Message_Pannel;
    private javax.swing.JLabel MessageLabel;
    private javax.swing.JScrollPane Message_scroll_pane;
    private javax.swing.JTextArea MessageTextArea;   
    // End of variables declaration


  
//ACTIONS TO BE PERFORMED BY THE PLUGIN 
    
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
       JavaUtils.putObjectInPrefs(prefs_, DIALOG_POSITION, this.getLocation());
       acq_.shutdown();
       plugin_.dispose();
    }
  
    private void DirectoryButtonActionPerformed (java.awt.event.ActionEvent evt) {
        String Rootname=acq_.getRootName();
        RootDirectoryField.setText(Rootname);
        //int returnVal = fc.showDialog(this, "Run Application");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal=fc.showOpenDialog(this);
        
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
                    //.getCurrentDirectory();
            Rootname=file.getPath();
            RootDirectoryField.setText(Rootname);
            acq_.setRootName(Rootname);
        }
        //acq_.setSaveFiles(rootPaneCheckingEnabled);
        
    }
    
    private void StackCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {
        if (StackCheckBox.isSelected()){
            StartField.setEnabled(true);
            StackPanel.setEnabled(true);
            SetStartButton.setEnabled(true);
            EndField.setEnabled(true);
            SetEndButton.setEnabled(true);
            StepField.setEnabled(true);
            StartLabel.setEnabled(true);
            EndLabel.setEnabled(true);
            StepLabel.setEnabled(true);
            StackComboBox.setEnabled(true);
            ShutterStackCheckBox.setEnabled(true);
        
        }
        else{
            StartField.setEnabled(false);
            StackPanel.setEnabled(false);
            SetStartButton.setEnabled(false);
            EndField.setEnabled(false);
            SetEndButton.setEnabled(false);
            StepField.setEnabled(false);
            StartLabel.setEnabled(false);
            EndLabel.setEnabled(false);
            StepLabel.setEnabled(false);
            StackComboBox.setEnabled(false);
            ShutterStackCheckBox.setEnabled(false);
        }
    }
    
    private void TimeLapseCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {                                                  
        if (TimeLapseCheckBox.isSelected()){
            TimeFrames.setEnabled(true);
            IntervalTime.setEnabled(true);
            IntervalUnits.setEnabled(true);
            NumberLabel.setEnabled(true);
            IntervalLabel.setEnabled(true);
        }
        else{
            TimeFrames.setEnabled(false);
            IntervalTime.setEnabled(false);
            IntervalUnits.setEnabled(false);
            NumberLabel.setEnabled(false);
            IntervalLabel.setEnabled(false); 
        }
    }  
    
    private void SaveCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {                                             
            if (SaveCheckBox.isSelected()){  
                RootDirectoryField.setEnabled(true);
                RootLabel.setEnabled(true);
                DirectoryButton.setEnabled(true);
                FileNameField.setEnabled(true);
                FileLabel.setEnabled(true);
            }
            else{
                RootDirectoryField.setEnabled(false);
                RootLabel.setEnabled(false);
                DirectoryButton.setEnabled(false);
                FileNameField.setEnabled(false);
                FileLabel.setEnabled(false);
            }               
            }
        
    private void ChannelCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {                                             
            if (Channel_CheckBox.isSelected()){                
                Channels_table.setEnabled(true);
                ChannelScrollPane.setEnabled(true);
                Channels_table.setVisible(true);
                //Channels_table.setBackground(Color.white);
            }
            else{
                Channels_table.setEnabled(false);
                ChannelScrollPane.setEnabled(false);
                Channels_table.setVisible(false);
                //Channels_table.setBackground(Color.gray);
            }   

            }
             
    private void GalvoCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {
          if (GalvoCheckBox.isSelected()){  
                GAmpTextField.setEnabled(true);
                GAmpLabel.setEnabled(true);
                GFreqTextField.setEnabled(true);
                GFreqLabel.setEnabled(true);
                GResetButton.setEnabled(true);
                GHomeButton.setEnabled(true);
                try {
                    core_.setProperty("Arduino_SR-Switch","State","16");
                    core_.setProperty("Arduino_SR-Shutter","OnOff","1");
                    core_.setProperty("Arduino_SR-Shutter","OnOff","0");
                } catch (Exception ex) {
                    Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
                    MessageTextArea.setText("ERROR IN GALVO START");
                }
            }
            else{
                GAmpTextField.setEnabled(false);
                GAmpLabel.setEnabled(false);
                GFreqTextField.setEnabled(false);
                GFreqLabel.setEnabled(false);
                GResetButton.setEnabled(false);
                GHomeButton.setEnabled(false);
                try {
                    core_.setProperty("Arduino_SR-Switch","State","16");
                    core_.setProperty("Arduino_SR-Shutter","OnOff","1");
                    core_.setProperty("Arduino_SR-Shutter","OnOff","0");
                } catch (Exception ex) {
                    Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
                    MessageTextArea.setText("ERROR IN GALVO START");
                }
            }  
    }
    
    private void UPUPButtonActionPerformed(java.awt.event.ActionEvent evt) {                                           
        ////String PositionOld=PositionField.getText();
        //String PositionOld=SC_.getPos();
        //CommentsTextArea.setText(PositionOld);
        
        String Step=UpUpField.getText();
        //String StackPosition=SC_.setPosUp(PositionOld,Step);
        //PositionField.setText(StackPosition);
        
          String focusdevice=core_.getFocusDevice();
          NumberFormat formatter = new DecimalFormat("#.0000");
          double STEP=Double.parseDouble(Step);
        try {
           double position = core_.getPosition(focusdevice);           
           double position_new=position+STEP;
           core_.setPosition(focusdevice,position_new);
           core_.sleep(500);
           String Position_new=formatter.format(core_.getPosition(focusdevice));  
           PositionField.setText(Position_new);
        } catch (Exception ex) {
            Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
        }

    }       
    
    private void UPButtonActionPerformed(java.awt.event.ActionEvent evt) {                                           
        ////String PositionOld=PositionField.getText();
        //String PositionOld=SC_.getPos();
        //CommentsTextArea.setText(PositionOld);
        
        String Step=UpField.getText();
        //String StackPosition=SC_.setPosUp(PositionOld,Step);
        //PositionField.setText(StackPosition);
        
          String focusdevice=core_.getFocusDevice();
          NumberFormat formatter = new DecimalFormat("#.0000");
          double STEP=Double.parseDouble(Step);
        try {
           double position = core_.getPosition(focusdevice);           
           double position_new=position+STEP;
           core_.setPosition(focusdevice,position_new);
           core_.sleep(500);
           String Position_new=formatter.format(core_.getPosition(focusdevice));          
           PositionField.setText(Position_new);
        } catch (Exception ex) {
            Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }  
    
    private void DownButtonActionPerformed(java.awt.event.ActionEvent evt) {                                           
 
        //String PositionOld=SC_.getPos();
        //CommentsTextArea.setText(PositionOld);
        
        String Step=UpField.getText();
        //String StackPosition=SC_.setPosDown(PositionOld,Step);
        //PositionField.setText(StackPosition);
                  String focusdevice=core_.getFocusDevice();
          NumberFormat formatter = new DecimalFormat("#.0000");
          double STEP=Double.parseDouble(Step);
        try {
           double position = core_.getPosition(focusdevice);           
           double position_new=position-STEP;
           core_.setPosition(focusdevice,position_new);
           core_.sleep(500);
           String Position_new=formatter.format(core_.getPosition(focusdevice));  
           PositionField.setText(Position_new);
        } catch (Exception ex) {
            Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }  
    
    private void DownDownButtonActionPerformed(java.awt.event.ActionEvent evt) {                                           

        //String PositionOld=SC_.getPos();
        //CommentsTextArea.setText(PositionOld);
        
        String Step=UpUpField.getText();
        //String StackPosition=SC_.setPosDown(PositionOld,Step);
        //PositionField.setText(StackPosition);
          String focusdevice=core_.getFocusDevice();
          NumberFormat formatter = new DecimalFormat("#.0000");
          double STEP=Double.parseDouble(Step);
        try {
           double position = core_.getPosition(focusdevice);           
           double position_new=position-STEP;
           core_.setPosition(focusdevice,position_new);
           core_.sleep(500);
           String Position_new=formatter.format(core_.getPosition(focusdevice));  
           PositionField.setText(Position_new);
        } catch (Exception ex) {
            Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }  
    
    private void LeftButtonActionPerformed(java.awt.event.ActionEvent evt) throws Exception {
        String Step;
        String Position;
        
        ShC_.openshutter();
        Global.shutter=1;
        
        Step=(String)StepAngleComboBox.getSelectedItem();
        Position=AnglePositionField.getText();        
        Position=RC_.leftMove(Step,Position);
        AnglePositionField.setText(Position);

    }
    
    private void RightButtonActionPerformed(java.awt.event.ActionEvent evt) throws Exception {
        String Step;
        String Position;
        
        ShC_.openshutter();
        Global.shutter=1;
        
        Step=(String)StepAngleComboBox.getSelectedItem();
        Position=AnglePositionField.getText();
        Position=RC_.rightMove(Step,Position);
        AnglePositionField.setText(Position);

    }
    
    private void MakeZeroButtonActionPerformed(java.awt.event.ActionEvent evt) {

        AnglePositionField.setText("0");

    }
    
      private void ModeActionPerformed(java.awt.event.ActionEvent evt) {    
           String Mode=(String) ModeComboBox.getSelectedItem();  
          if("DSLM/SPIM".equals(Mode)){
                StackCheckBox.setEnabled(true);
                /*StartField.setEnabled(true);
                StackPanel.setEnabled(true);
                SetStartButton.setEnabled(true);
                EndField.setEnabled(true);
                SetEndButton.setEnabled(true);
                StepField.setEnabled(true);
                StartLabel.setEnabled(true);
                EndLabel.setEnabled(true);
                StepLabel.setEnabled(true);
                StackComboBox.setEnabled(true);
                ShutterStackCheckBox.setEnabled(true);*/

                GalvoCheckBox.setEnabled(true);
               /* GAmpTextField.setEnabled(true);
                GAmpLabel.setEnabled(true);
                GFreqTextField.setEnabled(true);
                GFreqLabel.setEnabled(true);
                GResetButton.setEnabled(true);
                GHomeButton.setEnabled(true);*/
           }
           
          else if("OPT".equals(Mode)){
                StackCheckBox.setSelected(false);
                StackCheckBox.setEnabled(false);
                StartField.setEnabled(false);
                StackPanel.setEnabled(false);
                SetStartButton.setEnabled(false);
                EndField.setEnabled(false);
                SetEndButton.setEnabled(false);
                StepField.setEnabled(false);
                StartLabel.setEnabled(false);
                EndLabel.setEnabled(false);
                StepLabel.setEnabled(false);
                StackComboBox.setEnabled(false);
                ShutterStackCheckBox.setEnabled(false);

                GalvoCheckBox.setSelected(false);
                GalvoCheckBox.setEnabled(false);
                GAmpTextField.setEnabled(false);
                GAmpLabel.setEnabled(false);
                GFreqTextField.setEnabled(false);
                GFreqLabel.setEnabled(false);
                GResetButton.setEnabled(false);
                GHomeButton.setEnabled(false);
           }
      }
    private void LineCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {

        if (LineCheckBox.isSelected()){
        IJ.makeLine(app_.getImageWidth()/2, 0, app_.getImageWidth()/2, app_.getImageHeight());
        }
        else {
            IJ.makeLine(0, 0, 0, 0);
        }
    }
    
    private void SetStartButtonActionPerformed(java.awt.event.ActionEvent evt) {

        //String Position=SC_.getPos();
          String focusdevice=core_.getFocusDevice();
          NumberFormat formatter = new DecimalFormat("#.0000");
          
        try {
           double position = core_.getPosition(focusdevice);           
           String Position=formatter.format(position);
           StartField.setText(Position);
        } catch (Exception ex) {
            Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void SetEndButtonActionPerformed(java.awt.event.ActionEvent evt) {

        //String Position=SC_.getPos();
                   String focusdevice=core_.getFocusDevice();
          NumberFormat formatter = new DecimalFormat("#.0000");
          
        try {
           double position = core_.getPosition(focusdevice);           
           String Position=formatter.format(position);
           EndField.setText(Position); 
        } catch (Exception ex) {
            Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void GResetButtonActionPerformed(java.awt.event.ActionEvent evt) {
             
        String AMP=GAmpTextField.getText();
        double amp=Double.parseDouble(AMP);
        String FREQ=GFreqTextField.getText();
        double freq=Double.parseDouble(FREQ);
        try {
            core_.setProperty("Arduino_SR-Switch","State","16");
            core_.setProperty("Arduino_SR-Shutter","OnOff","1");
            core_.setProperty("Arduino_SR-Shutter","OnOff","0");
            core_.setProperty("Arduino_GM-DAC1","Volts",amp);
            core_.setProperty("Arduino_GM-DAC1","Frequency",freq); 
            ShC_.openshutter();
            Global.shutter=1;

        } catch (Exception ex) {
            Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("ERROR IN RESSET");
        }
        
    }
    
    private void GHomeButtonActionPerformed(java.awt.event.ActionEvent evt) {
                String AMP=GAmpTextField.getText();
            double amp=Double.parseDouble(AMP);
            String FREQ=GFreqTextField.getText();
            double freq=0;
                try {
                    core_.setProperty("Arduino_SR-Switch","State","16");
                    core_.setProperty("Arduino_SR-Shutter","OnOff","1");
                    core_.setProperty("Arduino_SR-Shutter","OnOff","0");
                    core_.setProperty("Arduino_GM-DAC1","Volts",amp);
                    core_.setProperty("Arduino_GM-DAC1","Frequency",freq); 
                    ShC_.openshutter();
                    
                    Global.shutter=1;
                } catch (Exception ex) {
                    Logger.getLogger(DSLM_controlsDialog.class.getName()).log(Level.SEVERE, null, ex);
                    MessageTextArea.setText("ERROR IN HOME");
                }
    }
       
    private void Emission_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {
             
          int Current_Item=Emission_ComboBox.getSelectedIndex();
          String CI=FC_.ChangeEmissionFilter(Current_Item);
          MessageTextArea.setText("The Current Emission Filter is:  "+ CI);
             
      }
      
    private void Excitation_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {
             
          int Current_Item=Excitation_ComboBox.getSelectedIndex();
          String CI=FC_.ChangeExcitationFilter(Current_Item);
          MessageTextArea.setText("The Current Excitation Filter is:  "+ CI);
             
      }
      
    private void ShutterButtonActionPerformed (java.awt.event.ActionEvent evt) throws Exception { 
                
        if (Global.shutter==0){
           ShC_.openshutter();
           Global.shutter=1;
           MessageTextArea.setText("shutter ON");
        }
        else if (Global.shutter==1){
           ShC_.closeshutter();
           Global.shutter=0;  
           MessageTextArea.setText("shutter OFF");
        }
    }

    public void setPlugin(DSLMcontrolsPlugin plugin) {
      plugin_ = plugin;
   }
    
}

