package org.codeconsole.recurly

import groovy.transform.Canonical
import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilder

@Canonical
class Redemption {
    Boolean single_use
    Integer total_discounted_in_cents
    String currency
    String created_at

    static Redemption fromXml(GPathResult gPathResult) {
        if (gPathResult?.name() == 'redemption') {
            Redemption redemption = new Redemption()
            gPathResult.children().each {
                if (it.name() == "single_use") {
                    redemption.single_use = Boolean.valueOf(it.text())
                } else if (it.name() == "total_discounted_in_cents") {
                    redemption.total_discounted_in_cents = Integer.valueOf(it.text())
                } else if (redemption.metaClass.hasProperty(redemption, it.name())) {
                    redemption[it.name()] = it.text()
                }
            }
            return redemption
        }
        null
    }

    static Redemption redeemCoupon(String couponCode, String accountCode, String curr = "USD") {
        StringWriter output = new StringWriter()
        new MarkupBuilder(output).redemption {
            account_code    accountCode
            currency        curr
        }
        fromXml Recurly.doPostWithXmlResponse("coupons/${couponCode}/redeem", output.toString())
    }
}
