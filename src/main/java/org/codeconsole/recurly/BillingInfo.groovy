package org.codeconsole.recurly

import groovy.transform.Canonical
import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilder

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

    static private String makeXml(BillingInfo billingInfo) {
        def output = new StringWriter()
        new MarkupBuilder(output).billing_info(type: 'credit_card') {
            first_name(billingInfo.first_name)
            last_name(billingInfo.last_name)
            if (billingInfo.number) {                            
                number (billingInfo.number)
            }
            if (billingInfo.verification_value) {
                verification_value (billingInfo.verification_value)                                
            }
            if (billingInfo.month) {
                month (billingInfo.month)                                
            }
            if (billingInfo.year) {
                year (billingInfo.year)
            }
            if (billingInfo.address1) {
                address1 (billingInfo.address1)
            }
            if (billingInfo.address2) {
                address2 (billingInfo.address2)
            }
            if (billingInfo.city) {
                city (billingInfo.city)
            }
            if (billingInfo.state) {
                state (billingInfo.state)
            }
            if (billingInfo.zip) {
                zip (billingInfo.zip)
            }
            if (billingInfo.country) {
                country (billingInfo.country)
            }
        }
        output.toString()
    }

    static BillingInfo findByAccountCode(String account_code) {
        fromXml(Recurly.fetchXml("/accounts/${account_code}/billing_info"))
    }

    static BillingInfo updateBillingInfo(String account_code, BillingInfo billing_info) {
        fromXml Recurly.doPostWithXmlResponse("/accounts/${account_code}/billing_info", makeXml(billing_info))
    }        
    
    boolean hasNumber() {
        number
    }
    
    // only return true in boolean tests if there is at least
    // one of last_four, number, first_name or last_name properties present
    boolean asBoolean() {
        last_four || number ||  first_name || last_name
    }
}
