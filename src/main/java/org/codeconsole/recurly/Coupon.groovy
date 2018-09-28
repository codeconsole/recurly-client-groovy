package org.codeconsole.recurly

import groovy.transform.Canonical
import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilder

@Canonical
class Coupon {
	String coupon_code
	String redeem_by_date
	String duration // forever, single_use, or temporal.

	String name
	String discount_type // percent or dollars.
	String discount_in_cents // 2_00
	String discount_percent // 10

	Double applyDiscount(double amount, String plan = null) {
		if (plan && !isApplicable(plan)) {
			return amount
		}
		switch(discount_type) {
			case 'percent':
				return (1.0 - (Double.parseDouble(discount_percent) / 100)) * amount
			case 'dollars':	
				return amount - (Double.parseDouble(discount_in_cents.replaceAll(/_/,'') ) / 100) 			
			break
		}
		amount
 	}

 	boolean isApplicable(String plan) {
 		appliesToAllPlans() || (plan && plan_codes.indexOf(plan) != -1)
 	}
 
	String applies_to_all_plans //= false 
	String plan_codes // %w(gold platinum)

	String redemption_resource // account or subscription - Limit redemption to specific subscription on account.
	String max_redemptions_per_account // = 1 Limit redemptions per account to a specific number.
	String applies_to_non_plan_charges // = true Discount should include one-time charges.

	String description
	String invoice_description
	
	String max_redemptions
	
	boolean appliesToAllPlans() {
		applies_to_all_plans == 'true'
	}

    static Coupon fromXml(GPathResult gPathResult) {
    	println gPathResult.toString()
         if (gPathResult?.name() == 'coupon') {
             Coupon coupon = new Coupon()
             gPathResult.children().each {
                 if (coupon.metaClass.hasProperty(coupon, it.name())) {
                     coupon[it.name()] = it.text()
                 }
             }
             return coupon
         }
         null
    }

    static Coupon findByCouponCode(String coupon_code) {
        fromXml(Recurly.fetchXml("/coupons/${coupon_code}"))
    }

    static List<Coupon> list() {
        List<Coupon> coupons = []
        Recurly.fetchXml("/coupons").children().each {
            Coupon coupon = fromXml(it)
            if (coupon) {
                coupons << coupon
            }
        }
        coupons
    }
}
