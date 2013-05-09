package org.codeconsole.recurly

import spock.lang.Specification

class RecurlySpec extends Specification {
    
    static final String EXPIRED = """<?xml version="1.0" encoding="UTF-8"?>
<errors>
  <error field="transaction.account.billing_info.number" symbol="expired">is expired or has an invalid expiration date</error>
  <error field="transaction.account.account_code" symbol="blank">can't be blank</error>
</errors>
"""
    
    static final String CARD_NUMBER_MISSING = """<?xml version="1.0" encoding="UTF-8"?>
<errors>
  <error field="transaction.account.billing_info.number" symbol="required">is required</error>
  <error field="transaction.account.billing_info.zip" symbol="empty">can't be empty</error>
  <error field="transaction.account.account_code" symbol="blank">can't be blank</error>
</errors>
"""
    
    def "Throw proper error when expired"(){
        when:
        Recurly.throwExceptionOnError(Recurly.parseXml(EXPIRED))
        then:
        RecurlyException ex = thrown(RecurlyException)
        ex.message  == 'is expired or has an invalid expiration date'
        ex.symbol   == 'expired'
        ex.field    == 'transaction.account.billing_info.number'
    }
    
    def "Throw proper error when number is missing"(){
        when:
        Recurly.throwExceptionOnError(Recurly.parseXml(CARD_NUMBER_MISSING))
        then:
        RecurlyException ex = thrown(RecurlyException)
        ex.message  == 'is required'
        ex.symbol   == 'required'
        ex.field    == 'transaction.account.billing_info.number'
    }

}
