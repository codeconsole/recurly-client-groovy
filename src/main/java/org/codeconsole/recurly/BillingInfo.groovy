package org.codeconsole.recurly

import groovy.transform.Canonical
import groovy.util.slurpersupport.GPathResult

@Canonical
class BillingInfo {
    String first_name	// First name
    String last_name	// Last name
    String company	    // Company name
    String address1	    // Address line 1
    String address2	    // Address line 2
    String city	        // City
    String state	    // State
    String country	    // Country, 2-letter ISO code
    String zip	        // Zip or postal code
    String phone	    // Phone number
    String vat_number	// Customer's VAT Number
    String ip_address	// Customer's IP address when updating their billing information
    String ip_address_country	// Country of IP address, if known by Recurly

    // Credit Card Attributes	Description
    String first_six	// Credit card number, first six digits
    String last_four	// Credit card number, last four digits
    String number       // Credit card number
    String verification_value
    String card_type	// Visa, MasterCard, American Express, Discover, JCB, etc
    String month	    // Expiration month
    String year	        // Expiration year
    String PayPal       // Attribute	Description
    String billing_agreement_id


    static BillingInfo fromXml(GPathResult gPathResult) {
         if (gPathResult?.name() == 'billing_info') {
             BillingInfo billingInfo = new BillingInfo()
             gPathResult.children().each {
                 if (billingInfo.metaClass.hasProperty(billingInfo, it.name())) {
                     billingInfo[it.name()] = it.text()
                 }
             }
             return billingInfo
         }
         null
     }

    static BillingInfo findByAccountCode(String account_code) {
        fromXml(Recurly.fetchXml("/accounts/${account_code}/billing_info"))
    }
    
    boolean hasNumber(){
        number
    }
    
    
}
