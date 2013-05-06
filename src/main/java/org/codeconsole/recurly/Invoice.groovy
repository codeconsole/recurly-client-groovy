package org.codeconsole.recurly

import groovy.transform.Canonical
import groovy.util.slurpersupport.GPathResult

@Canonical
class Invoice {
    String state
    String invoice_number
    String po_number
    String vat_number
    String subtotal_in_cents
    String tax_in_cents
    String total_in_cents
    String currency
    String created_at

    Account account

    Double getTotal() {
        Integer.parseInt(total_in_cents) / 100.0
    }

    Date getCreated() {
        Recurly.convertDate(created_at)
    }

    static Invoice fromXml(GPathResult gPathResult) {
        if (gPathResult?.name() == 'invoice') {
            Invoice invoice = new Invoice()
            gPathResult.children().each {
                if (it.name() == "account") {
                    invoice.account = Account.fromXml(Recurly.fetchXml(it.@href.text()[it.@href.text().indexOf('/accounts')..-1]))
                } else if (invoice.metaClass.hasProperty(invoice, it.name())) {
                    invoice[it.name()] = it.text()
                }
            }
            return invoice
        }
        null
    }

    static List<Invoice> findByAccountCode(String account_code) {
        List<Invoice> invoices = []
        Recurly.fetchXml("/accounts/${account_code}/invoices").children().each {
            Invoice invoice = fromXml(it)
            if (invoice) {
                invoices << invoice
            }
        }
        invoices
    }

    static InputStream retrievePdf(String invoice_number) {
        Recurly.fetchPdf("/invoices/${invoice_number}")
    }
}
