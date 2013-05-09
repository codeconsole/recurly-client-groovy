package org.codeconsole.recurly

import groovy.transform.Canonical
import groovy.util.slurpersupport.GPathResult

@Canonical
class Subscription {
    String plan	            // Nested plan_code and plan name
    String uuid	            // Unique subscription ID
    String state	        // "active", "canceled", "future", "expired", "modified"
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
                 if (subscription.metaClass.hasProperty(subscription, it.name())) {
                     subscription[it.name()] = it.text()
                 }
             }
             return subscription
         }
         null
     }

    static List<Subscription> findByAccountCode(String account_code) {
        List<Subscription> subscriptions = []
        Recurly.fetchXml("/accounts/${account_code}/subscriptions").children().each {
            Subscription subscription  = fromXml(it)
            if (subscription) {
                subscriptions << subscription
            }
        }
        subscriptions
    }

    static String cancelSubscription(String uuid) {
        Recurly.doPut("subscriptions/${uuid}/cancel")
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