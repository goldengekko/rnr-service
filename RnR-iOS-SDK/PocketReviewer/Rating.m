//
//  Rating.m
//
//  Created by Mattias Levin on 5/31/12.
//  Copyright (c) 2012 Wadpam. All rights reserved.
//

#import "Rating.h"
#import "ObjectMapper.h"


#define RATING_SCALE 20

// Implementation
@implementation Rating


ANNOTATE_PROPERTY_FOR_KEY(itemId, id)
@synthesize itemId = itemId_;

ANNOTATE_PROPERTY_FOR_KEY(totalSumOfRatings, ratingSum)
@synthesize totalSumOfRatings = totalSumOfRatings_;

ANNOTATE_PROPERTY_FOR_KEY(numberOfRatings, ratingCount)
@synthesize numberOfRatings = numberOfRatings_;


// Release instance variables
- (void)dealloc {
  [itemId_ release];
  [super dealloc];
}


// Create autoreleased Rating object
+ (Rating*)rating {
  return [[[Rating alloc] init] autorelease];
}


// Getter for average rating
- (float) averageRating {
  return (float)self.totalSumOfRatings / (float)self.numberOfRatings / RATING_SCALE;
}


// Custom description
- (NSString*)description {
  return [NSString stringWithFormat:@"Id: %@, averate ratng: %f, number of ratings: %d", 
          self.itemId, self.averageRating, self.numberOfRatings];
}


@end
