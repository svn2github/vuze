
// The following ifdef block is the standard way of creating macros which make exporting 
// from a DLL simpler. All files within this DLL are compiled with the AEREG_EXPORTS
// symbol defined on the command line. this symbol should not be defined on any project
// that uses this DLL. This way any other project whose source files include this file see 
// AEREG_API functions as being imported from a DLL, wheras this DLL sees symbols
// defined with this macro as being exported.
#ifdef AEREG_EXPORTS
#define AEREG_API __declspec(dllexport)
#else
#define AEREG_API __declspec(dllimport)
#endif

// This class is exported from the aereg.dll
class AEREG_API CAereg {
public:
	CAereg(void);
	// TODO: add your methods here.
};

extern AEREG_API int nAereg;

AEREG_API int fnAereg(void);

