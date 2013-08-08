//
//  LaunchServicesWrapper.h
//  SetDefaultApplication
//
//  Created by Stefan BALU on 6/21/13.
//  Copyright (c) 2013 Spigot, Inc. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface LaunchServicesWrapper : NSObject

+ (NSString *)UTIforFileMimeType:(NSString *)mimetype;
+ (NSString *)UTIforFileExtension:(NSString *)extension;

+ (NSString *)defaultApplicationForExtension:(NSString *)extension;
+ (NSString *)defaultApplicationForMimeType:(NSString *)mimetype;
+ (NSString *)defaultApplicationForScheme:(NSString *)scheme;

+ (BOOL)setDefaultApplication:(NSString *)bundleID forExtension:(NSString *)extension;
+ (BOOL)setDefaultApplication:(NSString *)bundleID forMimeType:(NSString *)mimetype;
+ (BOOL)setDefaultApplication:(NSString *)bundleID forScheme:(NSString *)scheme;

@end

