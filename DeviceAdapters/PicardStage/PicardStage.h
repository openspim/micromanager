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

#include "DeviceBase.h"

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

	double GetStepSizeUm();
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

	double GetStepSizeUm();
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

#if 0
class CPicardXYStageAdapter : public MM::XYStage
{
public:
	CPicardXYStageAdapter();
	~CPicardXYStageAdapter();

	// Property API
	virtual unsigned GetNumberOfProperties() const;
	virtual int GetProperty(const char* name, char* value) const;
	virtual int SetProperty(const char* name, const char* value);
	virtual bool HasProperty(const char* name) const;
	virtual bool GetPropertyName(unsigned idx, char* name) const;
	virtual int GetPropertyReadOnly(const char* name, bool& readOnly) const;
	virtual int GetPropertyInitStatus(const char* name, bool& preInit) const;
	virtual int HasPropertyLimits(const char* name, bool& hasLimits) const;
	virtual int GetPropertyLowerLimit(const char* name, double& lowLimit) const;
	virtual int GetPropertyUpperLimit(const char* name, double& hiLimit) const;
	virtual int GetPropertyType(const char* name, MM::PropertyType& pt) const;
	virtual unsigned GetNumberOfPropertyValues(const char* propertyName) const;
	virtual bool GetPropertyValueAt(const char* propertyName, unsigned index, char* value) const;

	// Property sequencing
	virtual int IsPropertySequenceable(const char* name, bool& isSequenceable) const;
	virtual int GetPropertySequenceMaxLength(const char* propertyName, long& nrEvents) const;
	virtual int StartPropertySequence(const char* propertyName);
	virtual int StopPropertySequence(const char* propertyName);
	virtual int ClearPropertySequence(const char* propertyName);
	virtual int AddToPropertySequence(const char* propertyName, const char* value);
	virtual int SendPropertySequence(const char* propertyName);

	virtual bool GetErrorText(int errorCode, char* errMessage) const;
	virtual bool Busy();
	virtual double GetDelayMs() const;
	virtual void SetDelayMs(double delay);
	virtual bool UsesDelay();

	// Library handle management (we just store it so MM can get it easily)
	virtual HDEVMODULE GetModuleHandle() const;
	virtual void SetModuleHandle(HDEVMODULE hLibraryHandle);

	virtual void SetLabel(const char* label);
	virtual void GetLabel(char* name) const;

	virtual void SetModuleName(const char* moduleName);
	virtual void GetModuleName(char* moduleName) const;

	virtual void SetDescription(const char* description);
	virtual void GetDescription(char* description) const;

	virtual int Initialize();
	virtual int Shutdown();

	virtual void GetName(char* name) const;
	virtual void SetCallback(MM::Core* callback);

	// Experimental(?) acquisition API
	virtual int AcqBefore();
	virtual int AcqAfter();
	virtual int AcqBeforeFrame();
	virtual int AcqAfterFrame();
	virtual int AcqBeforeStack();
	virtual int AcqAfterStack();

	// Device discovery. Doesn't do anything yet...
	virtual MM::DeviceDetectionStatus DetectDevice(void);

	// Hub-peripheral relationship
	virtual void SetParentID(const char* parentId);
	virtual void GetParentID(char* parentID) const;

	// XYStage API
	virtual int SetPositionUm(double x, double y);
	virtual int SetRelativePositionUm(double dx, double dy);
	virtual int SetAdapterOriginUm(double x, double y);
	virtual int GetPositionUm(double& x, double& y);
	virtual int GetLimitsUm(double& xMin, double& xMax, double& yMin, double& yMax);
	virtual int Move(double vx, double vy);

	virtual int SetPositionSteps(long x, long y);
	virtual int GetPositionSteps(long& x, long& y);
	virtual int SetRelativePositionSteps(long x, long y);
	virtual int Home();
	virtual int Stop();
	virtual int SetOrigin();
	virtual int GetStepLimits(long& xMin, long& xMax, long& yMin, long& yMax);
	virtual double GetStepSizeXUm();
	virtual double GetStepSizeYUm();

	virtual int IsXYStageSequenceable(bool& isSequenceable) const;
	virtual int GetXYStageSequenceMaxLength(long& nrEvents) const;
	virtual int StartXYStageSequence();
	virtual int StopXYStageSequence();
	virtual int ClearXYStageSequence();
	virtual int AddToXYStageSequence(double positionX, double positionY);
	virtual int SendXYStageSequence();

private:
	CSIABStage *m_pStageX, *m_pStageY;
	char *m_szLabel, *m_szDesc, *m_szModName, *m_szParentId;
	HDEVMODULE m_hModule;
	MM::Core* m_pCallback;
};
#endif

#endif //_PICARDSTAGE_H_
