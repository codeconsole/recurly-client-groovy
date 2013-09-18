package org.codeconsole.recurly

import groovy.transform.Canonical
import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilder

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
            if(!account.billing_info){
                account.billing_info = new BillingInfo()
            }
            return account
        }
        null
    }

    static Account findByAccountCode(String account_code) {
        fromXml(Recurly.fetchXml("/accounts/${account_code}"))
    }
    
    static Account updateAccount(Account a){
        fromXml Recurly.doPutWithXmlResponse("/accounts/${a.account_code}", makeXml(a))
    }
    
    static Account createAccount(Account a){
        fromXml Recurly.doPostWithXmlResponse("/accounts/${a.account_code}", makeXml(a))
    }
    
    static private String makeXml(Account a) {
        def output = new StringWriter()
        new MarkupBuilder(output).account {
            account_code(a.account_code)
            first_name(a.first_name)
            last_name(a.last_name)
            email(a.email)
            if (a.billing_info) {
                billing_info(type: 'credit_card') {
                    first_name(a.billing_info.first_name)
                    last_name(a.billing_info.last_name)
                    if(a.billing_info.number){                            
                        number (a.billing_info.number)
                    }
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
        output.toString()
    }
}
