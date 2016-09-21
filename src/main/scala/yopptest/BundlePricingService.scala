package yopptest

import yopptest.BundlePricingDomain._
import com.typesafe.scalalogging.StrictLogging

case class BundledCart(bundles: Seq[BundlePromotion], notInBundleItems: Seq[CartItem]) {
  def totalPrice: Price = Price(bundles.map(_.totalDiscountedPrice.value).sum + notInBundleItems.map(x => x.catalogItem.unitPrice.value * x.quantity.value).sum)
}

/**
  *
  */
class BundlePricingService(catalog: Seq[CatalogItem], bundlePromotions: Seq[BundlePromotion]) extends StrictLogging {

  /**
    * Group cart item in bundle to get the lowest possible cart price
    * @return cart price in cents, example 2250 => $22.50
    */
  def bundleToLowestPrice(cart: Cart): Price = bundlelizeCart(cart).totalPrice


  private def bundlelizeCart(cart: Cart): BundledCart = {
    // check if the cart is valid (for example each item is well from the catalog)
    if(!isCartValid(cart)) throw new InvalidCartException("Cart is invalid")

    // bundles
    // First we look which bundles can be applied to the cart
    // The algorithm will work only with those bundles as over aren't applicable on cart
    val availableBundles = bundlePromotions.filter{promo =>
      promo.cartItems.forall{item =>
        // each cart item should have at least the quantity required by the bundle
        cart.cartItems.exists(it => it.catalogItem == item.catalogItem && it.quantity.value >= item.quantity.value)
      }
    }

    /**
      * Recursively transform the Cart with "alone" items to a Cart with bundle items
      */
    def bundlelizeCartRecursive(currentCart: BundledCart) : BundledCart = {

      def whichCartItemABundleWillRemoveFromCart(bundlePromotion: BundlePromotion) = {
        val removeFromCart = currentCart.notInBundleItems.flatMap { aloneCartItem =>
          bundlePromotion.cartItems.filter(_.catalogItem == aloneCartItem.catalogItem).map { bundleCartItem =>
            val maxQuantityRemoved = bundleCartItem.quantity.value // bundle quantity
            CartItem(aloneCartItem.catalogItem, Quantity(aloneCartItem.quantity.value.min(maxQuantityRemoved)))
          }
        }
        removeFromCart
      }

      /**
        * Compute the discount of one bundle in current cart
        * A bundle wont mandatory reduce the total price, it depends of the item prices it removes
        * from the cart (not bundled items)
        *
        * return discount : if < 0 , bundle reduce the cart price, if > 0 it increases it
        */
      def bundleGain(potentialBundle: BundlePromotion): Int = {
        val removeFromCart = whichCartItemABundleWillRemoveFromCart(potentialBundle)
        val aloneItemsPrice = removeFromCart.map(x => x.catalogItem.unitPrice.value * x.quantity.value).sum
        val bundlePrice = potentialBundle.totalDiscountedPrice.value
        bundlePrice - aloneItemsPrice // if < 0 , bundle reduce the cart price, if > 0 it increases it
      }

      availableBundles.map(potentialBundle => (potentialBundle, bundleGain(potentialBundle)))
        // We look for bundle which will generate the biggest discount
        .sortBy(_._2).headOption match {
        case Some((potentialBundle, discount)) if discount < 0 =>
          logger.debug(s"$potentialBundle reduce the price by ${Math.abs(discount)}, add it to cart")
          val updatedCart = {
            val toRemoveItems = whichCartItemABundleWillRemoveFromCart(potentialBundle)
            val aloneItems = currentCart.notInBundleItems.flatMap { currentCartItem =>
              toRemoveItems.find(toRemove => currentCartItem.catalogItem == toRemove.catalogItem) match {
                case Some(toRemove) => // remove the quantity took by the bundle
                  val newQuantity = currentCartItem.quantity.value - toRemove.quantity.value
                  if (newQuantity > 0) Some(currentCartItem.copy(quantity = Quantity(newQuantity)))
                  else None // if quantity become 0, we remove the item from cart
                case None => Some(currentCartItem) // stay the same
              }
            }
            BundledCart(currentCart.bundles :+ potentialBundle, aloneItems)
          }
          logger.debug("Updated cart : " + updatedCart)
          bundlelizeCartRecursive(updatedCart)
        case _ =>
          logger.debug("No bundle reduce the price, end of recursion")
          logger.debug("Final bundled cart : " + currentCart)
          currentCart
      }
    }

    // recursively transform the Cart with "alone" items to a Cart with bundle items
    bundlelizeCartRecursive(BundledCart(
      bundles = Seq(), // we start with no bundles
      cart.cartItems   // all items are alone at first recursion
    ))

  }


  private def isCartValid(cart: Cart): Boolean =
    cart.cartItems.forall{cartItem =>
      if(catalog.contains(cartItem.catalogItem)) true
      else {
        logger.warn(s"${cartItem.catalogItem} isn't in catalog => Invalid cart")
        false
      }
    }

}
