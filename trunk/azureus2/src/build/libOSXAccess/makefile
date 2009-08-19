SDK = /Developer/SDKs/MacOSX10.4u.sdk
ARCHS = -arch i386 -arch ppc
LFLAGS = -bundle -isysroot $(SDK) $(ARCHS) -framework JavaVM -framework Carbon -framework AppKit -framework IOKit 
CFLAGS = -c $(ARCHS) -I $(SDK)/System/Library/Frameworks/ApplicationServices.framework/Versions/A/Frameworks/AE.framework/Versions/A/Headers


OBJECTS = OSXAccess.o IONotification.o
OUTLIB = libOSXAccess.jnilib

all:
	g++ $(CFLAGS) OSXAccess.mm IONotification.m 
	g++ -o $(OUTLIB) $(LFLAGS) $(OBJECTS)
	rm -f *.o