#import <ApplicationServices/ApplicationServices.h>
#import "LaunchServicesWrapper.h"

@implementation LaunchServicesWrapper

+ (NSString *)UTIforFileMimeType:(NSString *)mimetype
{
    return (NSString *)CFBridgingRelease(
        UTTypeCreatePreferredIdentifierForTag(kUTTagClassMIMEType, (CFStringRef)mimetype, NULL)
    );
}

+ (NSString *)UTIforFileExtension:(NSString *)extension
{
    return (NSString *)CFBridgingRelease(
        UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension, (CFStringRef)extension, NULL)
    );
}

+ (NSString *)defaultApplicationForExtension:(NSString *)extension
{
    return (NSString *)CFBridgingRelease(
        LSCopyDefaultRoleHandlerForContentType((CFStringRef)[LaunchServicesWrapper UTIforFileExtension:extension], kLSRolesAll)
    );
}

+ (NSString *)defaultApplicationForMimeType:(NSString *)mimetype
{
    return (NSString *)CFBridgingRelease(
        LSCopyDefaultRoleHandlerForContentType((CFStringRef)[LaunchServicesWrapper UTIforFileMimeType:mimetype], kLSRolesAll)
    );
}

+ (NSString *)defaultApplicationForScheme:(NSString *)scheme
{
    return (NSString *)CFBridgingRelease(
        LSCopyDefaultHandlerForURLScheme((CFStringRef)scheme)
    );
}

+ (BOOL)setDefaultApplication:(NSBundle *)appBundle forExtension:(NSString *)extension
{
    return LSSetDefaultRoleHandlerForContentType(
                                                 (CFStringRef)[LaunchServicesWrapper UTIforFileExtension:extension], kLSRolesAll, (CFStringRef)[appBundle bundleIdentifier]);
}

+ (BOOL)setDefaultApplication:(NSBundle *)appBundle forMimeType:(NSString *)mimetype
{
    return LSSetDefaultRoleHandlerForContentType((CFStringRef)[LaunchServicesWrapper UTIforFileMimeType:mimetype], kLSRolesAll, (CFStringRef)[appBundle bundleIdentifier]);
}

+ (BOOL)setDefaultApplication:(NSBundle *)appBundle forScheme:(NSString *)scheme
{
    return LSSetDefaultHandlerForURLScheme((CFStringRef)scheme, (CFStringRef)[appBundle bundleIdentifier]);
}

@end