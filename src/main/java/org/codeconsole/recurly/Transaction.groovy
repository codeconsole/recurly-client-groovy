package org.codeconsole.recurly

import groovy.transform.Canonical
import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilder

@Canonical
class Transaction {
    String uuid	        // Unique transaction ID
    String action       // "purchase", "authorization", or "refund"
    String currency     //	3-letter currency for the transaction
    String amount_in_cents	// Total transaction amount in cents
    String tax_in_cents	// Amount of tax or VAT within the transaction, in cents
    String status	    // "success", "failed", or "void"
    String reference	// Transaction reference from your payment gateway
    String source	    // Source of the transaction. Possible values: transaction for one-time transactions, subscription for subscriptions, billing_info for updating billing info.
    String test	        // True if test transaction
    String voidable	    // True if the transaction may be voidable, accuracy depends on your gateway
    String refundable	// True if the transaction may be refunded
    String gcvv_result	// CVV result, if applicable
    String avs_result	// AVS result, if applicable
    String avs_result_street	// AVS result for the street address, line 1
    String avs_result_postal	// AVS result for the postal code
    String created_at	// Date the transaction took place

    //  details	     Nested account and billing information submitted at the time of the transaction. When writing a client library, do not map these directly to Account or Billing Info objects.

    Account account
    Invoice invoice

    static Transaction createTransaction(Account account, String amount_in_cents, String currency, String description = null) {
        fromXml Recurly.doPostWithXmlResponse('/transactions', makeXml(account, amount_in_cents, currency, description))
    }

    static private String makeXml(Account a, String amt, String c, String desc = null) {
        def output = new StringWriter()
        new MarkupBuilder(output).transaction() {
            amount_in_cents(amt)
            currency(c)
            if(desc){
                description desc
            }
            account() {
                account_code(a.account_code)
                if (a.billing_info) {
                    first_name(a.billing_info.first_name)
                    last_name(a.billing_info.last_name)
                    number (a.billing_info.number)
                    verification_value (a.billing_info.verification_value)
                    month (a.billing_info.month)
                    year (a.billing_info.year)
                }
            }
        }
        output.toString()
    }

    static Transaction fromXml(GPathResult gPathResult) {
        if(gPathResult == null){
            return null
        }
         if (gPathResult?.name() == 'transaction') {
             Transaction transaction = new Transaction()
             gPathResult.children().each {
                 if (it.name() == "account") {
                     transaction.account = Account.fromXml(Recurly.fetchXml(it.@href.text()[it.@href.text().indexOf('/accounts')..-1]))
                 } else if (it.name() == "invoice") {
                     transaction.invoice = Invoice.fromXml(Recurly.fetchXml(it.@href.text()[it.@href.text().indexOf('/invoices')..-1]), transaction.account)
                 } else if (transaction.metaClass.hasProperty(transaction, it.name())) {
                     transaction[it.name()] = it.text()
                 }
             }
             return transaction
         }
         null
     }

    static Transaction findByUuid(String uuid) {
        fromXml(Recurly.fetchXml("/transactions/${uuid}"))
    }
}
