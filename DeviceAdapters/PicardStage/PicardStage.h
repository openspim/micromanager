///////////////////////////////////////////////////////////////////////////////
// FILE:          PicardStage.h
// PROJECT:       Micro-Manager
// SUBSYSTEM:     DeviceAdapters
//-----------------------------------------------------------------------------
// DESCRIPTION:   The drivers for the Picard Industries USB stages
//                Based on the CDemoStage and CDemoXYStage classes
//                
// AUTHOR:        Johannes Schindelin
//
// COPYRIGHT:     Johannes Schindelin, 2011
//
// LICENSE:       This file is distributed under the BSD license.
//                License text is included with the source distribution.
//
//                This file is distributed in the hope that it will be useful,
//                but WITHOUT ANY WARRANTY; without even the implied warranty
//                of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//                IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//                CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//                INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.

#ifndef _PICARDSTAGE_H_
#define _PICARDSTAGE_H_

#include "../../MMDevice/DeviceBase.h"

//////////////////////////////////////////////////////////////////////////////
// CSIABZStage class
//////////////////////////////////////////////////////////////////////////////

class CSIABTwister: public CStageBase<CSIABTwister>
{
public:
    CSIABTwister();
    ~CSIABTwister();

    bool Busy();
    double GetDelayMs() const;
    void SetDelayMs(double delay);
    bool UsesDelay();
	int Initialize();
	int Shutdown();
	void GetName(char* name) const;

	int SetPositionUm(double pos);
	int Move(double velocity);
	int SetAdapterOriginUm(double d);
	int GetPositionUm(double& pos);
	int SetPositionSteps(long steps);
	int GetPositionSteps(long& steps);
	int SetOrigin();
	int GetLimits(double& lower, double& upper);
	int IsStageSequenceable(bool& isSequenceable) const;
	int GetStageSequenceMaxLength(long& nrEvents) const;
	int StartStageSequence() const;
	int StopStageSequence() const;
	int ClearStageSequence();
	int AddToStageSequence(double position);
	int SendStageSequence() const; 
	bool IsContinuousFocusDrive() const;
private:
	int OnSerialNumber(MM::PropertyBase* pProp, MM::ActionType eAct);

	int serial_;
	int velocity_;
	void *handle_;
};

//////////////////////////////////////////////////////////////////////////////
// CSIABZStage class
//////////////////////////////////////////////////////////////////////////////

class CSIABStage : public CStageBase<CSIABStage>
{
public:
    CSIABStage();
    ~CSIABStage();

    bool Busy();
    double GetDelayMs() const;
    void SetDelayMs(double delay);
    bool UsesDelay();
	int Initialize();
	int Shutdown();
	void GetName(char* name) const;

	int SetPositionUm(double pos);
	int SetRelativePositionUm(double d);
	int Move(double velocity);
	int SetAdapterOriginUm(double d);
	int GetPositionUm(double& pos);
	int SetPositionSteps(long steps);
	int GetPositionSteps(long& steps);
	int SetOrigin();
	int GetLimits(double& lower, double& upper);
	int IsStageSequenceable(bool& isSequenceable) const;
	int GetStageSequenceMaxLength(long& nrEvents) const;
	int StartStageSequence() const;
	int StopStageSequence() const;
	int ClearStageSequence();
	int AddToStageSequence(double position);
	int SendStageSequence() const; 
	bool IsContinuousFocusDrive() const;
private:
	int OnSerialNumber(MM::PropertyBase* pProp, MM::ActionType eAct);
	int OnVelocity(MM::PropertyBase* pProp, MM::ActionType eAct);

	int serial_;
	int velocity_;
	void *handle_;
};

//////////////////////////////////////////////////////////////////////////////
// CSIABDemoStage class
// Simulation of the single axis stage
//////////////////////////////////////////////////////////////////////////////

class CSIABXYStage : public CXYStageBase<CSIABXYStage>
{
public:
    CSIABXYStage();
    ~CSIABXYStage();

    bool Busy();
    double GetDelayMs() const;
    void SetDelayMs(double delay);
    bool UsesDelay();
	int Initialize();
	int Shutdown();
	void GetName(char* name) const;

	int SetPositionUm(double x, double y);
	int SetRelativePositionUm(double dx, double dy);
	int SetAdapterOriginUm(double x, double y);
	int GetPositionUm(double& x, double& y);
	int GetLimitsUm(double& xMin, double& xMax, double& yMin, double& yMax);
	int Move(double vx, double vy);

	int SetPositionSteps(long x, long y);
	int GetPositionSteps(long& x, long& y);
	int SetRelativePositionSteps(long x, long y);
	int Home();
	int Stop();
	int SetOrigin();//jizhen, 4/12/2007
	int GetStepLimits(long& xMin, long& xMax, long& yMin, long& yMax);
	double GetStepSizeXUm();
	double GetStepSizeYUm();
	int IsXYStageSequenceable(bool& isSequenceable) const;

protected:
	// This is how it SHOULD have been done. Why, why why why!? WHY
	// WOULD YOU MAKE THIS PRIVATE!?
	virtual void GetOrientation(bool& mirrorX, bool& mirrorY);

private:
	int OnSerialNumberX(MM::PropertyBase* pProp, MM::ActionType eAct);
	int OnSerialNumberY(MM::PropertyBase* pProp, MM::ActionType eAct);
	int OnMinX(MM::PropertyBase* pProp, MM::ActionType eAct);
	int OnMaxX(MM::PropertyBase* pProp, MM::ActionType eAct);
	int OnMinY(MM::PropertyBase* pProp, MM::ActionType eAct);
	int OnMaxY(MM::PropertyBase* pProp, MM::ActionType eAct);
	int OnVelocityX(MM::PropertyBase* pProp, MM::ActionType eAct);
	int OnVelocityY(MM::PropertyBase* pProp, MM::ActionType eAct);

	int serialX_, serialY_;
	int velocityX_, velocityY_;
	void *handleX_, *handleY_;
	int minX_, maxX_;
	int minY_, maxY_;
};

#endif //_PICARDSTAGE_H_
