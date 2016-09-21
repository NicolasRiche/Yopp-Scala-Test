package yopptest

import yopptest.BundlePricingDomain.{CartItem, Quantity}

object BundlePricingUtil {

 def groupCartItem(cartItems: Seq[CartItem]): Seq[CartItem] = {
    // check if all items are well grouped and group them
    // Example (Apple, qt 1) (Apple, qt 1) become (Apple, qt 2)
    cartItems.groupBy(_.catalogItem).map{
      case (item, toGroupItems) => CartItem(item, Quantity(toGroupItems.map(_.quantity.value).sum))
    }.toSeq
  }

}
