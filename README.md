# Scala test

## How to consume the pricing service :
- Create a Bundle pricing service current catalog & bundles
( src/main/scala/BundlePricingServiceTest is the entry point class for consumer )

```scala
// need to pass the catalog and current bundles promotions.
val pricingService = new yopptest.BundlePricingService(catalog: Seq[CatalogItem], currentBundles : Seq[BundlePromotion])
```

- Then call bundleToLowestPrice(cart: Cart) to get the lowest possible price (using bundles)
```scala
pricingService.bundleToLowestPrice(...)
```

- Cart object and other domain objects are in yopptest.BundlePricingDomain

- Usage examples in tests: src/test/scala/BundlePricingServiceTest

## Running tests
```
sbt test
```
