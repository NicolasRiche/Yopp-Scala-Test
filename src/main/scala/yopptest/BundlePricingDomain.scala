package yopptest


object BundlePricingDomain {

  case class CatalogItem(name: String, unitPrice: Price)

  class Cart(cartItemsParam: Seq[CartItem]) {
    val cartItems = BundlePricingUtil.groupCartItem(cartItemsParam)
  }
  object Cart { def apply(cartItemsParam: Seq[CartItem]) = new Cart(cartItemsParam) }

  class InvalidCartException(msg:String) extends Exception(msg)

  case class CartItem(catalogItem: CatalogItem, quantity: Quantity)

  // Each bundle should implement this trait
  sealed trait BundlePromotion {
    def cartItems: Seq[CartItem] // the items of the bundle, example 2 apples + 1 margarine, it's how we detect the bundle pattern
    def totalDiscountedPrice: Price // total price of the bundle, we don't need unit price of each item,
                                    // total price is enough as it's this value which determine if a bundle
                                    // reduce or increase the price of the car
  }

  // discount on total price
  // example 1 apple $2.00 , 2 apples for $3.00
  class BundleTotalPriceDiscount(
   cartItemsParams: Seq[CartItem], // how we detect the pattern
   totalPrice: Price // the discount
   ) extends BundlePromotion {
    def cartItems = BundlePricingUtil.groupCartItem(cartItemsParams)
    def totalDiscountedPrice = totalPrice
    override def toString = cartItems + " => Total " + totalDiscountedPrice
  }
  object BundleTotalPriceDiscount { def apply(cartItemsParams: Seq[CartItem],totalPrice: Price) = new BundleTotalPriceDiscount(cartItemsParams, totalPrice) }

  // discount on specific item(s) (we override the unit price) into a promotion
  // example a loaf of bread “A” purchased with two sticks of margarine “B” and
  // the second stick of margarine is free (e.g. $0)
  class BundleDiscountOnItemUnitPrice(discountedItems: Seq[MaybeDiscountedItem]) extends BundlePromotion {
    def cartItems = BundlePricingUtil.groupCartItem(discountedItems.map(_.cartItem))
    def totalDiscountedPrice = {
      val itemsPrices = discountedItems.map{itemWrapper => itemWrapper.optionalUnitPriceOverride match {
          case Some(discountedPrice) =>
            discountedPrice.value * itemWrapper.cartItem.quantity.value
          case None => // normal price
            itemWrapper.cartItem.catalogItem.unitPrice.value * itemWrapper.cartItem.quantity.value
        }
      }
      Price(itemsPrices.sum)
    }
    override def toString = discountedItems.toString()
  }
  object BundleDiscountOnItemUnitPrice { def apply(discountedItems: Seq[MaybeDiscountedItem]) = new BundleDiscountOnItemUnitPrice(discountedItems) }
  case class MaybeDiscountedItem(cartItem: CartItem, optionalUnitPriceOverride: Option[Price])


  // Value types

  class Quantity private (val value: Int) extends AnyVal { //  1 <= Integer <= 99
    override def toString = "Qt " + value.toString
  }
  object Quantity {
    def apply(value: Int): Quantity = {
      if(value >= 1 && value <= 99) new Quantity(value)
      else throw new Exception(value + " isn't a valid Quantity : 1 <= Integer <= 99")
    }
  }

  class Price private (val value: Int) extends AnyVal { //  0 <= Integer
    // as price is in cents example 2250 => $22.50, need to convert in nice looking price
    override def toString = value.toString.dropRight(2) + "." + value.toString.takeRight(2)
  }
  object Price {
    def apply(value: Int): Price = {
      if(value >= 0) new Price(value)
      else throw new Exception(value + " isn't a valid Price : 0 <= Integer")
    }
  }


}
