//
//  IONotification.m
//  USBPrivateDataSample
//
//  Created by Vuze on 8/11/09.
//  Copyright 2009 __MyCompanyName__. All rights reserved.
//

#import "IONotification.h"

#include <IOKit/IOKitLib.h>
#include <IOKit/IOMessage.h>
#include <IOKit/IOCFPlugIn.h>
#include <IOKit/usb/IOUSBLib.h>
#include <IOKit/storage/IOMedia.h>
#include <IOKit/IOBSD.h>
#include <sys/mount.h>
#include <JavaVM/jni.h>

//#define IODISMOUNT 1
//#define IONOTIFYDISMOUNT 1

#define _PATH_DEV       "/dev/"

static IONotificationPortRef gNotifyPort;

void DeviceNotification(void *refCon, io_service_t service, natural_t messageType, void *messageArgument);

/**
 * lookup a disk based on "dev" mount and return fileSystem Status
 *
 **/
static struct statfs * getFileSystemStatusDevMount(char * disk) {
	struct statfs * mountList;
	int mountListCount;
	int mountListIndex;

	mountListCount = getmntinfo(&mountList, MNT_NOWAIT);

	for (mountListIndex = 0; mountListIndex < mountListCount; mountListIndex++) {
		fprintf(stderr, "test looking for %s mounto %s fr %s\n ", disk, mountList[mountListIndex].f_mntonname,
				mountList[mountListIndex].f_mntfromname);
		if (strncmp(mountList[mountListIndex].f_mntfromname, _PATH_DEV, strlen(_PATH_DEV)) == 0) {
			if (strcmp(mountList[mountListIndex].f_mntfromname + strlen(_PATH_DEV), disk) == 0) {
				break;
			}
		}
	}

	return (mountListIndex < mountListCount) ? (mountList + mountListIndex) : (NULL);
}

/**
 * Lookup a disk based on "Volumes" mount point and return filesystem status
 *
 **/
static struct statfs * getFileSystemStatusFromMount(const char * mount) {
	struct statfs * mountList;
	int mountListCount;
	int mountListIndex;

	mountListCount = getmntinfo(&mountList, MNT_NOWAIT);

	for (mountListIndex = 0; mountListIndex < mountListCount; mountListIndex++) {
		//fprintf(stderr, "test mounto %s fr %s\n ",  mountList[mountListIndex].f_mntonname, mountList[mountListIndex].f_mntfromname);
		if (strcmp(mountList[mountListIndex].f_mntonname, mount) == 0) {
			break;
		}
	}

	return (mountListIndex < mountListCount) ? (mountList + mountListIndex) : (NULL);
}

@implementation IONotification

/**
 * Prepare file system info we gathered and pass it to a function that
 * will do something with it (like send it back to Java)
 *
 **/
extern void notify(const char *mount, io_service_t service, struct statfs *fs, bool added);

#ifdef IODISMOUNT
void DeviceRemoved(void *refCon, io_iterator_t iterator) {
	kern_return_t kr;
	io_service_t service;

	while ((service = IOIteratorNext(iterator))) {
		CFTypeRef str_bsd_path = IORegistryEntryCreateCFProperty(service, CFSTR(kIOBSDNameKey), kCFAllocatorDefault, 0);

		if (str_bsd_path != NULL) {
			int len = CFStringGetLength(str_bsd_path) * 2 + 1;
			char s[len];
			CFStringGetCString((CFStringRef) str_bsd_path, s, len, kCFStringEncodingUTF8);

			io_name_t deviceName;
			kern_return_t kr = IORegistryEntryGetName(service, deviceName);
			fprintf(stderr, "DR %s -- %s\n", s, deviceName);
			CFRelease(str_bsd_path);

			struct statfs *fs = getFileSystemStatusDevMount(s);
			const char *mount = (fs == 0) ? 0 : fs->f_mntonname;
			notify(mount, service, fs, false);
		}
		kr = IOObjectRelease(service);
	}
}
#endif

-(int) checkExisting
{
	CFMutableDictionaryRef matchingDict;
	kern_return_t kr;
	io_iterator_t iter;

#ifdef IODISMOUNT
	matchingDict = IOServiceMatching(kIOMediaClass);
	if (matchingDict == NULL) {
		fprintf(stderr, "IOServiceMatching returned NULL.\n");
		return -1;
	}

	kr = IOServiceAddMatchingNotification(gNotifyPort, kIOTerminatedNotification,
			matchingDict, // matching
			DeviceRemoved, // callback
			NULL, &iter);
	io_service_t service;
	while (service = IOIteratorNext(iter)) {
		IOObjectRelease(service);
	}
#endif
#ifdef NOCODE
	matchingDict = IOServiceMatching(kIOMediaClass);
	if (matchingDict == NULL) {
		fprintf(stderr, "IOServiceMatching returned NULL.\n");
		return -1;
	}

	kr = IOServiceAddMatchingNotification(gNotifyPort, kIOMatchedNotification,
			matchingDict, // matching
			rawDeviceAdded, // callback
			NULL, &iter);
	//io_service_t service;
	//while (service = IOIteratorNext(iter)) {
	//	IOObjectRelease(service);
	//}
#endif

	matchingDict = IOServiceMatching(kIOMediaClass);
	kr = IOServiceGetMatchingServices(kIOMasterPortDefault, matchingDict, &iter);
	//[self rawDeviceAdded:iter];
	rawDeviceAdded(0,iter);

	return 0;
}

- (void) setup
{
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

	gNotifyPort = IONotificationPortCreate(kIOMasterPortDefault);
	CFRunLoopSourceRef runLoopSource = IONotificationPortGetRunLoopSource(gNotifyPort);

	CFRunLoopRef runLoop = CFRunLoopGetCurrent();
	CFRunLoopAddSource(runLoop, runLoopSource, kCFRunLoopDefaultMode);

	NSWorkspace *ws = [NSWorkspace sharedWorkspace];
	NSNotificationCenter *center = [ws notificationCenter];
	[center addObserver:self selector:@selector(mount:) name:NSWorkspaceDidMountNotification object:ws];
	[center addObserver:self selector:@selector(unmount:) name:NSWorkspaceDidUnmountNotification object:ws];

	[ self checkExisting ];
}

#ifdef IONOTIFYDISMOUNT
void DeviceNotification(void *refCon, io_service_t service, natural_t messageType, void *messageArgument) {
	io_name_t deviceName;
	kern_return_t kr = IORegistryEntryGetName(service, deviceName);
	fprintf(stderr, "Device %s Not %d.\n", deviceName, messageType);
	if (messageType == kIOMessageServiceIsTerminated || messageType == kIOMessageServiceWasClosed) {
		if (messageType == kIOMessageServiceWasClosed) {
			fprintf(stderr, "Device close.\n");
		} else {
			fprintf(stderr, "Device terminated.\n");
		}

		CFTypeRef str_bsd_path = IORegistryEntryCreateCFProperty(service, CFSTR(kIOBSDNameKey), kCFAllocatorDefault, 0);

		if (str_bsd_path != NULL) {
			int len = CFStringGetLength(str_bsd_path) * 2 + 1;
			char s[len];
			CFStringGetCString((CFStringRef) str_bsd_path, s, len, kCFStringEncodingUTF8);
			CFRelease(str_bsd_path);

			fprintf(stderr, "WTF %s\n", s);

			struct statfs *fs = getFileSystemStatusDevMount(s);
			const char *mount = (fs == 0) ? 0 : fs->f_mntonname;
			notify(mount, service, fs, false);
		}

		if (refCon) {
			IOObjectRelease((io_object_t) refCon);
		}
	}
}
#endif

//- (void)rawDeviceAdded:(io_iterator_t)iterator
void rawDeviceAdded(void *refCon, io_iterator_t iterator) {
	kern_return_t kr;
	io_service_t service;

	while ((service = IOIteratorNext(iterator))) {
		io_name_t deviceName;
		kr = IORegistryEntryGetName(service, deviceName);
		fprintf(stderr, "rDA: %s\n", deviceName);
		CFTypeRef str_bsd_path = IORegistryEntryCreateCFProperty(service, CFSTR(kIOBSDNameKey), kCFAllocatorDefault, 0);

		if (str_bsd_path != NULL) {
			int len = CFStringGetLength(str_bsd_path) * 2 + 1;
			char s[len];
			CFStringGetCString((CFStringRef) str_bsd_path, s, len, kCFStringEncodingUTF8);

			fprintf(stderr, "rDA BSD %s\n", s);
			CFRelease(str_bsd_path);
#ifdef NOCODE
			NSDate *fireDate = [NSDate dateWithTimeIntervalSinceNow:2.0];
			NSTimer *timer = [[NSTimer alloc] initWithFireDate:fireDate
			target:self
			selector:@selector(countedtargetMethod:)
			userInfo:[self userInfo]
			repeats:NO];
#endif
			struct statfs *fs;
			fs = getFileSystemStatusDevMount(s);
			if (fs) {
				notify(fs->f_mntonname, service, fs, true);
#ifdef IONOTIFYDISMOUNT
				io_object_t obj;
				kr = IOServiceAddInterestNotification(gNotifyPort, // notifyPort
						service, // service
						kIOGeneralInterest, // interestType
						DeviceNotification, // callback
						&obj, // refCon
						&obj // notification
						);
#endif
			}
		}
		kr = IOObjectRelease(service);
	}
}

-(void)mount:(id)notification
{
	NSLog(@"mount: %@", notification);
	NSString *path = [[notification userInfo] valueForKey:@"NSDevicePath"];

	// With the path, we can use statfs to get the device name (/mnt/<name>)
	// from device name, we can query UIServiceGetMatchingService and get info (like if it's optical media)

	const char *cPath = [path UTF8String];
	fprintf(stderr, "mount %s\n", cPath);
	struct statfs *fs = getFileSystemStatusFromMount(cPath);
	if (fs) {
		CFMutableDictionaryRef matchingDict;

		matchingDict = IOServiceMatching(kIOMediaClass);
		if (matchingDict == NULL) {
			fprintf(stderr, "IOServiceMatching returned NULL.\n");
			return;
		}

		char *sBSDName = strrchr(fs->f_mntfromname, (int) '/');
		if (sBSDName) {
			sBSDName++;

			fprintf(stderr, "Searching for %s\n", sBSDName);
			CFStringRef bsdname = CFStringCreateWithCString(kCFAllocatorDefault, sBSDName, kCFStringEncodingMacRoman);

			CFDictionarySetValue(matchingDict,
					CFSTR(kIOBSDNameKey),
					bsdname);
		}

		io_service_t service = IOServiceGetMatchingService(kIOMasterPortDefault, matchingDict);
		if (service) {
			notify(cPath, service, fs, true);
#ifdef IONOTIFYDISMOUNT
			io_object_t obj;
			kern_return_t kr = IOServiceAddInterestNotification(gNotifyPort, // notifyPort
					service, // service
					kIOGeneralInterest, // interestType
					DeviceNotification, // callback
					&obj, // refCon
					&obj // notification
			);
#endif
			IOObjectRelease(service);
		}
	}
}

-(void)unmount:(id)notification
{
	NSString *path = [[notification userInfo] valueForKey:@"NSDevicePath"];

	// With the path, we can use statfs to get the device name (/mnt/<name>)
	// from device name, we can query UIServiceGetMatchingService and get info (like if it's optical media)
	NSLog(@"unmount: %@", notification);

	const char *cPath = [path UTF8String];
	fprintf(stderr, "unmount %s\n", cPath);
	struct statfs *fs = getFileSystemStatusFromMount(cPath);
	io_service_t service = 0;

	// Alas, fs will always be null, so service lookup will never run
	// If we stored the NSDevicePath : fs->f_mntfromname mapping, and looked
	// it up, we might have a chance to get the ioservice..
	if (fs) {
		CFMutableDictionaryRef matchingDict;

		matchingDict = IOServiceMatching(kIOMediaClass);
		if (matchingDict == NULL) {
			fprintf(stderr, "IOServiceMatching returned NULL.\n");
			return;
		}

		char *sBSDName = strrchr(fs->f_mntfromname, (int) '/');
		if (sBSDName) {
			sBSDName++;

			CFStringRef bsdname = CFStringCreateWithCString(kCFAllocatorDefault, sBSDName, kCFStringEncodingMacRoman);

			CFDictionarySetValue(matchingDict, CFSTR(kIOBSDNameKey), bsdname);
			service = IOServiceGetMatchingService(kIOMasterPortDefault, matchingDict);
			//fprintf(stderr, "Searching for %s, result = %p\n", sBSDName, service);
		}
	}

	notify(cPath, service, fs, false);
	if (service) {
		IOObjectRelease(service);
	}
}

@end
