# Scala test

## How to consume the pricing service :
- Instantiate the service with current catalog & bundles
( src/main/scala/BundlePricingServiceTest is the entry point class for consumer )
```scala
val pricingService = new yopptest.BundlePricingService(catalogExample, currentBundles)
```

- Then call bundleToLowestPrice(cart: Cart) to get the lowest possible price (using bundles)
pricingService.bundleToLowestPrice(...)

- Cart object and other domain objects are in yopptest.BundlePricingDomain

Usage example in tests: src/test/scala/BundlePricingServiceTest

## Running tests
```
sbt test
```

## TODO
- Package as JVM lib
- Java interop