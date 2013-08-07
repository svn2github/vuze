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

+ (BOOL)setDefaultApplication:(NSBundle *)appBundle forExtension:(NSString *)extension;
+ (BOOL)setDefaultApplication:(NSBundle *)appBundle forMimeType:(NSString *)mimetype;
+ (BOOL)setDefaultApplication:(NSBundle *)appBundle forScheme:(NSString *)scheme;

@end

