#import <Cocoa/Cocoa.h>

#define fprintf


@interface IONotification : NSObject {
}

- (void) setup;
-(void)mount:(id)notification;
-(void)unmount:(id)notification;

void rawDeviceAdded(void *refCon, io_iterator_t iterator);

@end
