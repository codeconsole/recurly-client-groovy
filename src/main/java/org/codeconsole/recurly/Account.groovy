package org.codeconsole.recurly

import groovy.transform.Canonical
import groovy.util.slurpersupport.GPathResult

@Canonical
class Account {
    String account_code
    String state // "active" or "closed"
    String username
    String email
    String first_name
    String last_name
    String company_name
    String accept_language
    String hosted_login_token
    String created_at

    BillingInfo billing_info

    boolean isActive() {
        state == "active"
    }

    static Account fromXml(GPathResult gPathResult) {
        if (gPathResult?.name() == 'account') {
            Account account = new Account()
            gPathResult.children().each {
                if (it.name() == "billing_info") {
                    account.billing_info = BillingInfo.fromXml(Recurly.fetchXml(it.@href.text()[it.@href.text().indexOf('/accounts')..-1]))
                } else if (account.metaClass.hasProperty(account, it.name())) {
                    account[it.name()] = it.text()
                }
            }
            return account
        }
        null
    }

    static Account findByAccountCode(String account_code) {
        fromXml(Recurly.fetchXml("/accounts/${account_code}"))
    }
}
