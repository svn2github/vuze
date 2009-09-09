
CO_SDK = /Developer/SDKs/MacOSX10.5.sdk
CO_ARCHS = -arch i386
# -arch x86_64
#CO_LFLAGS = -bundle -isysroot $(CO_SDK) $(CO_ARCHS) -framework JavaVM -framework Carbon -framework AppKit -framework IOKit -mmacosx-version-min=10.5
CO_LFLAGS = -bundle -isysroot $(CO_SDK) $(CO_ARCHS) -framework JavaVM -framework Carbon -framework AppKit -framework IOKit
##CO_CFLAGS = -c $(CO_ARCHS)
CO_CFLAGS = -c $(CA_ARCHS) -I $(CA_SDK)/System/Library/Frameworks/ApplicationServices.framework/Versions/A/Frameworks/AE.framework/Versions/A/Headers

CA_SDK = /Developer/SDKs/MacOSX10.4u.sdk
CA_ARCHS = -arch i386 -arch ppc
CA_LFLAGS = -bundle -isysroot $(CA_SDK) $(CA_ARCHS) -framework JavaVM -framework Carbon -framework AppKit -framework IOKit 
CA_CFLAGS = -c $(CA_ARCHS) -I $(CA_SDK)/System/Library/Frameworks/ApplicationServices.framework/Versions/A/Frameworks/AE.framework/Versions/A/Headers

104_SDK = /Developer/SDKs/MacOSX10.4u.sdk
104_ARCHS = -arch i386 
#-arch ppc
# -arch x86_64
104_LFLAGS = -bundle -isysroot $(104_SDK) $(104_ARCHS) -framework JavaVM -framework Carbon -framework AppKit -framework IOKit -mmacosx-version-min=10.4
104_CFLAGS = -c $(104_ARCHS) -I $(104_SDK)/System/Library/Frameworks/ApplicationServices.framework/Versions/A/Frameworks/AE.framework/Versions/A/Headers

105_SDK = /Developer/SDKs/MacOSX10.5.sdk
105_ARCHS = -arch i386
# -arch x86_64
105_LFLAGS = -bundle -isysroot $(105_SDK) $(105_ARCHS) -framework JavaVM -framework Carbon -framework AppKit -framework IOKit -mmacosx-version-min=10.5
105_CFLAGS = -c $(105_ARCHS) -I $(104_SDK)/System/Library/Frameworks/ApplicationServices.framework/Versions/A/Frameworks/AE.framework/Versions/A/Headers




OBJECTS = OSXAccess.o IONotification.o
OUTLIB = libOSXAccess

all:
	echo cocoa 104 104_64 105 carbon


cocoa:
	g++ $(CO_CFLAGS) OSXAccess.mm IONotification.m  -DCARBON
	g++ -o $(OUTLIB)_Cocoa.jnilib $(CO_LFLAGS) $(OBJECTS)
	rm -f *.o
	
104:
	g++ $(104_CFLAGS) OSXAccess.mm IONotification.m  -DCARBON
	g++ -o $(OUTLIB)_104.jnilib $(104_LFLAGS) $(OBJECTS)
	rm -f *.o
	

104_64:
	g++ $(104_CFLAGS)  OSXAccess.mm IONotification.m  -DCARBON
	g++ -o $(OUTLIB)_104.jnilib $(104_LFLAGS) $(OBJECTS)
	rm -f *.o
	
105:
	g++ $(105_CFLAGS) OSXAccess.mm IONotification.m  -DCARBON
	g++ -o $(OUTLIB)_105.jnilib $(105_LFLAGS) $(OBJECTS)
	rm -f *.o
	
carbon:
	g++-4.0 $(CA_CFLAGS) OSXAccess.mm IONotification.m -DCARBON 
	g++-4.0 -o $(OUTLIB)_Carbon.jnilib $(CA_LFLAGS) $(OBJECTS)
	rm -f *.o