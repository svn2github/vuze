
#include "stdafx.h"
#include "ntddstor.h"
#include <windows.h>
#include <winioctl.h>
#include <stdio.h>

#include "org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface.h"

BOOL GetDriveGeometry(HANDLE hDevice, DISK_GEOMETRY *pdg)
{
	BOOL bResult;                 // results flag
	DWORD junk;                   // discard results

	bResult = DeviceIoControl(hDevice,  // device to be queried
		IOCTL_DISK_GET_DRIVE_GEOMETRY,  // operation to perform
		NULL, 0, // no input buffer
		pdg, sizeof(*pdg),     // output buffer
		&junk,                 // # bytes returned
		(LPOVERLAPPED) NULL);  // synchronous I/O

	return (bResult);
}
BOOL GetStorageProperty(HANDLE hDevice, PSTORAGE_DEVICE_DESCRIPTOR *p)
{
	DWORD junk;                   // discard results

	STORAGE_PROPERTY_QUERY Query;   // input param for query

	// specify the query type
	Query.PropertyId = StorageDeviceProperty;
	Query.QueryType = PropertyStandardQuery;


	BOOL res = DeviceIoControl(hDevice,                     // device handle
		IOCTL_STORAGE_QUERY_PROPERTY,             // info of device property
		&Query, sizeof(STORAGE_PROPERTY_QUERY),  // input data buffer
		*p, (*p)->Size,               // output data buffer
		&junk,                           // out's length
		(LPOVERLAPPED)NULL);

	return (res);
}

JNIEXPORT jobject JNICALL Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_getAvailableDrives
(JNIEnv *env, jclass cla)
{
	DISK_GEOMETRY pdg;            // disk drive geometry structure
	BOOL bResult;                 // generic results flag
	ULONGLONG DiskSize;           // size of the drive, in bytes

	HANDLE hDevice;

	// create List
	jclass clsArrayList = env->FindClass("java/util/ArrayList");
	jmethodID constArrayList = env->GetMethodID(clsArrayList, "<init>", "()V");
	jobject arrayList = env->NewObject(clsArrayList, constArrayList, "");
	jmethodID methAdd = env->GetMethodID(clsArrayList, "add", "(Ljava/lang/Object;)Z");


	// each bit returned is one drive, starting with "A:"
	DWORD dwLogicalDrives = GetLogicalDrives();

	for ( int nDrive = 0; nDrive<32; nDrive++ )
	{
		if ( (dwLogicalDrives & (1 << nDrive)) == 0 ) {
			continue;
		}

		// Do an aditional check by using GetDriveGeometry.  If it fails, then there's
		// no "disk" in the drive

		WCHAR drive[100];

		wsprintfW(drive, L"\\\\.\\%C:", 'a' + nDrive);
		hDevice = CreateFileW((LPCWSTR) drive,  // drive to open
			0,                // no access to the drive
			FILE_SHARE_READ | // share mode
			FILE_SHARE_WRITE,
			NULL,             // default security attributes
			OPEN_EXISTING,    // disposition
			0,                // file attributes
			NULL);            // do not copy file attributes

		char drive2[4];
		wsprintfA(drive2, "%C:\\", 'a' + nDrive);

		if (hDevice == INVALID_HANDLE_VALUE) // cannot open the drive
		{
			continue;
		}


		bResult = GetDriveGeometry (hDevice, &pdg);

		if (bResult)
		{
			// Create File
			jclass cls = env->FindClass("java/io/File");
			jmethodID constructor = env->GetMethodID(cls, "<init>", "(Ljava/lang/String;)V");
			jobject object = env->NewObject(cls, constructor, env->NewStringUTF(drive2));

			// add to list

			env->CallBooleanMethod( arrayList, methAdd, object );
		}

		CloseHandle(hDevice);
	}
	return arrayList;
}

void addToMap(JNIEnv *env, jobject hashMap, jmethodID methPut, jclass clsLong, jmethodID longInit, char *key, jlong val) {
	jobject longObj = env->NewObject(clsLong, longInit, val);
	env->CallObjectMethod(hashMap, methPut, env->NewStringUTF(key), longObj);
}

void addToMap(JNIEnv *env, jobject hashMap, jmethodID methPut, jclass clsLong, jmethodID longInit, char *key, char *val) {
	env->CallObjectMethod(hashMap, methPut, env->NewStringUTF(key), env->NewStringUTF(val));
}

JNIEXPORT jobject JNICALL Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_getDriveInfo
(JNIEnv *env, jclass cla, jchar driveLetter)
{
	jclass clsHashMap = env->FindClass("java/util/HashMap");
	jmethodID constHashMap = env->GetMethodID(clsHashMap, "<init>", "()V");
	jobject hashMap = env->NewObject(clsHashMap, constHashMap, "");
	jmethodID methPut = env->GetMethodID(clsHashMap, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

	jclass clsLong = env->FindClass("java/lang/Long");
	jmethodID longInit = env->GetMethodID(clsLong, "<init>", "(J)V");

	DISK_GEOMETRY pdg;            // disk drive geometry structure
	BOOL bResult;                 // generic results flag
	ULONGLONG DiskSize;           // size of the drive, in bytes

	HANDLE hDevice;


	WCHAR drive[100];

	wsprintfW(drive, L"\\\\.\\%C:", driveLetter);
	hDevice = CreateFileW((LPCWSTR) drive,  // drive to open
		0,                // no access to the drive
		FILE_SHARE_READ | // share mode
		FILE_SHARE_WRITE, 
		NULL,             // default security attributes
		OPEN_EXISTING,    // disposition
		0,                // file attributes
		NULL);            // do not copy file attributes

	WCHAR drive2[4];
	wsprintfW(drive2, L"%C:\\", driveLetter);
	DWORD uType = GetDriveTypeW(drive2);


	addToMap(env, hashMap, methPut, clsLong, longInit, "DriveType", (jlong) uType);

	if (hDevice == INVALID_HANDLE_VALUE) // cannot open the drive
	{
		return hashMap;
	}


	bResult = GetDriveGeometry (hDevice, &pdg);

	if (bResult) 
	{
		LONGLONG diskSize = pdg.Cylinders.QuadPart * pdg.TracksPerCylinder *
			pdg.SectorsPerTrack * pdg.BytesPerSector;
		addToMap(env, hashMap, methPut, clsLong, longInit, "MediaType", (jlong) pdg.MediaType);
		addToMap(env, hashMap, methPut, clsLong, longInit, "DiskSize", (jlong) diskSize);
	}

	char OutBuf[1024] = {0};  // good enough, usually about 100 bytes
	PSTORAGE_DEVICE_DESCRIPTOR pDevDesc = (PSTORAGE_DEVICE_DESCRIPTOR)OutBuf;
	pDevDesc->Size = sizeof(OutBuf);

	bResult = GetStorageProperty(hDevice, &pDevDesc);

	if (bResult) {
		addToMap(env, hashMap, methPut, clsLong, longInit, "BusType", (jlong) pDevDesc->BusType);
		addToMap(env, hashMap, methPut, clsLong, longInit, "DeviceType", (jlong) pDevDesc->DeviceType);
		addToMap(env, hashMap, methPut, clsLong, longInit, "Removable", (jlong) pDevDesc->RemovableMedia);

		if (pDevDesc->VendorIdOffset != 0) {
			addToMap(env, hashMap, methPut, clsLong, longInit, "VendorID", &OutBuf[pDevDesc->VendorIdOffset]);
		}
		if (pDevDesc->ProductIdOffset != 0) {
			addToMap(env, hashMap, methPut, clsLong, longInit, "ProductID", &OutBuf[pDevDesc->ProductIdOffset]);
		}
		if (pDevDesc->ProductRevisionOffset != 0) {
			addToMap(env, hashMap, methPut, clsLong, longInit, "ProductRevision", &OutBuf[pDevDesc->ProductRevisionOffset]);
		}
		if (pDevDesc->SerialNumberOffset != 0) {
			addToMap(env, hashMap, methPut, clsLong, longInit, "SerialNumber", &OutBuf[pDevDesc->SerialNumberOffset]);
		}

	}

	STORAGE_BUS_TYPE t;

	CloseHandle(hDevice);
	return hashMap;
}