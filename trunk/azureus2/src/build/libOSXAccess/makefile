
105_SDK = /Developer/SDKs/MacOSX10.5.sdk
105_ARCHS = -arch i386 -arch ppc
105_LFLAGS = -bundle -isysroot $(105_SDK) $(105_ARCHS) -framework JavaVM -framework Carbon -framework AppKit -framework IOKit -mmacosx-version-min=10.4
105_CFLAGS = -c $(105_ARCHS) -I $(104_SDK)/System/Library/Frameworks/ApplicationServices.framework/Versions/A/Frameworks/AE.framework/Versions/A/Headers

105_64_SDK = /Developer/SDKs/MacOSX10.5.sdk
105_64_ARCHS = -arch x86_64
105_64_LFLAGS = -bundle -isysroot $(105_64_SDK) $(105_64_ARCHS) -framework JavaVM -framework Carbon -framework AppKit -framework IOKit -mmacosx-version-min=10.4
105_64_CFLAGS = -c $(105_64_ARCHS) -I $(104_SDK)/System/Library/Frameworks/ApplicationServices.framework/Versions/A/Frameworks/AE.framework/Versions/A/Headers


OBJECTS = OSXAccess.o IONotification.o
OUTLIB = libOSXAccess

all:
	echo 105 105_64 lipo


105:
	g++ $(105_CFLAGS) OSXAccess.mm IONotification.m  -DCARBON
	g++ -o $(OUTLIB)_105.jnilib $(105_LFLAGS) $(OBJECTS)
	rm -f *.o

105_64:
	g++ $(105_64_CFLAGS) OSXAccess.mm IONotification.m  -DCARBON
	g++ -o $(OUTLIB)_105_64.jnilib $(105_64_LFLAGS) $(OBJECTS)
	rm -f *.o
	
lipo:
	lipo -create libOSXAccess_105.jnilib libOSXAccess_105_64.jnilib -output libOSXAccess.jnilib