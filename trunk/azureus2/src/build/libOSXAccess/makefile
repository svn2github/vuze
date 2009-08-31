
CO_SDK = /Developer/SDKs/MacOSX10.5.sdk
CO_ARCHS = -arch i386 -arch x86_64
CO_LFLAGS = -bundle -isysroot $(CO_SDK) $(CO_ARCHS) -framework JavaVM -framework Carbon -framework AppKit -framework IOKit -mmacosx-version-min=10.5
CO_CFLAGS = -c $(CO_ARCHS)

CA_SDK = /Developer/SDKs/MacOSX10.4u.sdk
CA_ARCHS = -arch i386 -arch ppc
CA_LFLAGS = -bundle -isysroot $(CA_SDK) $(CA_ARCHS) -framework JavaVM -framework Carbon -framework AppKit -framework IOKit 
CA_CFLAGS = -c $(CA_ARCHS) -I $(CA_SDK)/System/Library/Frameworks/ApplicationServices.framework/Versions/A/Frameworks/AE.framework/Versions/A/Headers

OBJECTS = OSXAccess.o IONotification.o
OUTLIB = libOSXAccess.jnilib

all:
	echo  make cocoa or make carbon
	
cocoa:
	g++ $(CO_CFLAGS) OSXAccess.mm IONotification.m 
	g++ -o $(OUTLIB) $(CO_LFLAGS) $(OBJECTS)
	rm -f *.o
	
carbon:
	g++-4.0 $(CA_CFLAGS) OSXAccess.mm IONotification.m -DCARBON 
	g++-4.0 -o $(OUTLIB) $(CA_LFLAGS) $(OBJECTS)
	rm -f *.o