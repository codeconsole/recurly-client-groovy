package org.codeconsole.recurly

import groovy.transform.Canonical
import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilder

// https://docs.recurly.com/docs/subscriptions

@Canonical
class Subscription {
    Account account
    String plan	            // plan's title
    String plan_code        // plan's code
    String uuid	            // Unique subscription ID
    String state	        // "active", "canceled", "future", "expired"
    String unit_amount_in_cents	// Unit amount of the subscription
    String quantity	            // Number of units
    String currency	            // 3-letter ISO currency for the subscription
    String activated_at	        // Date the subscription started
    String canceled_at	        // Date the subscription was marked canceled
    String expires_at	        // Date the subscription will end (if state is "canceled"), ended (if state is "expired"), or was modified (if state is "modified")
    String current_period_started_at	// Date the current bill cycle started
    String current_period_ends_at	    // Date the current bill cycle will end
    String trial_started_at	        // Date the trial was started, if applicable
    String trial_ends_at	        // Date the trial ended, if applicable
    String subscription_add_ons	    // Nested list of add-ons on the subscription, if applicable
    String pending_subscription	    // Nested information about a pending subscription change at renewal    String

    public Date getEndDate() {
        Recurly.convertDate(current_period_ends_at)
    }

    static Subscription fromXml(GPathResult gPathResult) {
         if (gPathResult?.name() == 'subscription') {
             Subscription subscription = new Subscription()
             gPathResult.children().each {
                 if (it.name() == 'account') {
                     subscription.account = new Account(account_code: it.@href.text()[it.@href.text().indexOf('accounts/')+'accounts/'.length()..-1])
                 } else if (it.name() == 'plan') {
                     subscription.plan = it.name.text()
                     subscription.plan_code = it.plan_code.text()
                 } else if (subscription.metaClass.hasProperty(subscription, it.name())) {
                     subscription[it.name()] = it.text()
                 }
             }
             return subscription
         }
         null
     }
    
    static private String makeXml(Account a, String plan, String c, boolean doNotRenew = false) {
        makeXml(a, plan, c, null, doNotRenew)
    }
    
    static private String makeXml(Account a, String plan, String c, String couponCode, boolean doNotRenew = false, Integer unitAmountInCents = null) {
        def output = new StringWriter()
        new MarkupBuilder(output).subscription() {
            plan_code(plan)
            currency(c)
            if (doNotRenew) {
                total_billing_cycles(0)
            }
            if (couponCode) {
               coupon_code couponCode 
            }
            if (unitAmountInCents != null) {
                unit_amount_in_cents(unitAmountInCents)
            }
            account {
                account_code(a.account_code)
                first_name(a.first_name)
                last_name(a.last_name)
                email(a.email)
                if(a.billing_info?.hasNumber()){
                    billing_info(type: 'credit_card') {
                        first_name(a.billing_info.first_name)
                        last_name(a.billing_info.last_name)
                        number (a.billing_info.number)
                        if(a.billing_info.verification_value){
                            verification_value (a.billing_info.verification_value)
                        }
                        if(a.billing_info.month){
                            month (a.billing_info.month)
                        }
                        if(a.billing_info.year){
                            year (a.billing_info.year)
                        }
                        if(a.billing_info.address1){
                            address1 (a.billing_info.address1)
                        }
                        if(a.billing_info.address2){
                            address2 (a.billing_info.address2)
                        }
                        if(a.billing_info.city){
                            city (a.billing_info.city)
                        }
                        if(a.billing_info.state){
                            state (a.billing_info.state)
                        }
                        if(a.billing_info.zip){
                            zip (a.billing_info.zip)
                        }
                        if(a.billing_info.country){
                            country (a.billing_info.country)
                        }
                    }
                }
            }
        }
        output.toString()
    }
    
    static Subscription findBySubcriptionId(String uuid){
        fromXml(Recurly.fetchXml("/subscriptions/$uuid"))
    }

    static List<Subscription> findByAccountCode(String account_code) {
        List<Subscription> subscriptions = []
        Recurly.fetchXml("/accounts/${account_code}/subscriptions")?.children()?.each {
            Subscription subscription  = fromXml(it)
            if (subscription) {
                subscriptions << subscription
            }
        }
        subscriptions
    }

    static Subscription convertTrial(String uuid) {
        fromXml Recurly.doPutWithXmlResponse("subscriptions/${uuid}/convert_trial", null)
    }

    static String cancelSubscription(String uuid) {
        Recurly.doPut("subscriptions/${uuid}/cancel")
    }
    
    static Subscription createSubscription(Account account, String plan, String currency, String couponCode = null, boolean doNotRenew = false, Integer unit_amount_in_cents = null) {
        fromXml Recurly.doPostWithXmlResponse('subscriptions', makeXml(account, plan, currency, couponCode, doNotRenew, unit_amount_in_cents))
    }

    static List<String> cancelSubscriptions(String account_code) {
        List<String> results = []
        Subscription.findByAccountCode(account_code).each {
            if (it.state == "active") {
                results << cancelSubscription(apiKey, it.uuid)
            }
        }
        results
    }
}
