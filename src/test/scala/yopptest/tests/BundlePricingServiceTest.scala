package yopptest.tests

import org.scalatest._
import yopptest.BundlePricingDomain._

class BundlePricingServiceTest extends FlatSpec with Matchers {

  // build a test state ( catalog, bundles )

  val appleCatalogItem = CatalogItem("Apple", Price(199))
  val margarineCatalogItem = CatalogItem("Margarine", Price(250))
  val breadCatalogItem = CatalogItem("Bread", Price(300))

  val catalogExample = Seq(appleCatalogItem, margarineCatalogItem, breadCatalogItem)

  val currentBundles = Seq(
    // 1 apple 1.99 , 2 apples 2.15
    BundleTotalPriceDiscount(
      Seq(CartItem(appleCatalogItem, Quantity(2))),
      totalPrice = Price(215)
    ),
    // 1 bread + 2 magarines, the 2nd margarine is free
    BundleDiscountOnItemUnitPrice(
      Seq(
        MaybeDiscountedItem(CartItem(breadCatalogItem, Quantity(1)), optionalUnitPriceOverride = None),
        MaybeDiscountedItem(CartItem(margarineCatalogItem, Quantity(1)), optionalUnitPriceOverride = None),
        // 2nd magarine Free!
        MaybeDiscountedItem(CartItem(margarineCatalogItem, Quantity(1)), optionalUnitPriceOverride = Some(Price(0)))
      )
    )
  )

  val pricingService = new yopptest.BundlePricingService(catalogExample, currentBundles)

  // tests

  "A Bundle Pricing Service" should "find the lowest possible price ; Simple bundle case : 1 apple 1.99 , 2 apples 2.15 " in {

    // 1 apple , no bundle
    pricingService.bundleToLowestPrice(Cart(Seq(
       CartItem(appleCatalogItem, Quantity(1))
     ))) should be (Price(199))

    // 2 apple, bundle !
    pricingService.bundleToLowestPrice(Cart(Seq(
      CartItem(appleCatalogItem, Quantity(2))
    ))) should be (Price(215))

    // 3 apples, we are allowed to use 2 bundles of 2 apples because items can be reused
    // However in this case  2 apples bundle + 1 apple alone is cheaper than 2 bundles
    pricingService.bundleToLowestPrice(Cart(Seq(
      CartItem(appleCatalogItem, Quantity(3))
    ))) should be (Price(215+199))

    // 4 apples, should be grouped into 2 (2 apples) bundles
    pricingService.bundleToLowestPrice(Cart(Seq(
      CartItem(appleCatalogItem, Quantity(4))
    ))) should be (Price(215+215))

    // 5 apples, should be grouped into 2 (2 apples) bundles + 1 apple one (lowest price than 3 (2Apples) bundles
    pricingService.bundleToLowestPrice(Cart(Seq(
      CartItem(appleCatalogItem, Quantity(5))
    ))) should be (Price(215+215+199))

  }

  it should "find the lowest possible price ; More complex bundle case : 1 bread + 2 magarines, the 2nd margarine is free " in {

    // 1 bread , 1 margarine, no bundle just the sum of unit prices
    pricingService.bundleToLowestPrice(Cart(Seq(
      CartItem(breadCatalogItem, Quantity(1)),
      CartItem(margarineCatalogItem, Quantity(1))
    ))) should be (Price(300+250))

    // 1 bread , 2 margarines, bundle, 2nd margarine is free
    pricingService.bundleToLowestPrice(Cart(Seq(
      CartItem(breadCatalogItem, Quantity(1)),
      CartItem(margarineCatalogItem, Quantity(2))
    ))) should be (Price(300+250))

    // 1 bread , 3 margarines, bundle, we still should have the 2nd margarine free, + pay for the 3rd
    pricingService.bundleToLowestPrice(Cart(Seq(
      CartItem(breadCatalogItem, Quantity(1)),
      CartItem(margarineCatalogItem, Quantity(3))
    ))) should be (Price(300+250+250))

  }

  it should "find the lowest possible price ; Complex cart where multiple bundle types can be found " in {

    // a small shopping list with various products, but no bundle
    pricingService.bundleToLowestPrice(Cart(Seq(
      CartItem(appleCatalogItem, Quantity(1)),
      CartItem(breadCatalogItem, Quantity(1)),
      CartItem(margarineCatalogItem, Quantity(1))
    ))) should be (Price(199+300+250))

    // a small shopping list with various products,
    // (2 apples) bundle
    pricingService.bundleToLowestPrice(Cart(Seq(
      CartItem(appleCatalogItem, Quantity(3)),
      CartItem(breadCatalogItem, Quantity(1)),
      CartItem(margarineCatalogItem, Quantity(1))
    ))) should be (Price(215+199+300+250))

    // a small shopping list with various products,
    // (2 apples) bundle + 1 bread, 2 magarines bundle
    pricingService.bundleToLowestPrice(Cart(Seq(
      CartItem(appleCatalogItem, Quantity(3)),
      CartItem(breadCatalogItem, Quantity(1)),
      CartItem(margarineCatalogItem, Quantity(2))
    ))) should be (Price(215+199+300+250))

    // a small shopping list with various products,
    // (2 apples) bundle + 1 bread, 2 magarines bundle, 1 additional single bread alone
    pricingService.bundleToLowestPrice(Cart(Seq(
      CartItem(appleCatalogItem, Quantity(3)),
      CartItem(breadCatalogItem, Quantity(2)),
      CartItem(margarineCatalogItem, Quantity(2))
    ))) should be (Price(215+199+300+250+300))

  }


  it should "throw InvalidCartException if consumer pass a Cart with invalid catalog item(s)" in {
    a[InvalidCartException] should be thrownBy {
      pricingService.bundleToLowestPrice(
        Cart(Seq(
          CartItem(CatalogItem("Pink Apple", Price(300)), Quantity(10))
          // unfortunately Pink apple is no longer in catalog
        ))
      )
    }
  }

}

