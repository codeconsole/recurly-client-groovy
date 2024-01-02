package org.codeconsole.recurly;

import groovy.util.slurpersupport.GPathResult;

public class RecurlyException extends RuntimeException {
    
    private static final long serialVersionUID = -5928130216780613040L;
    
    private final GPathResult xml;
    private final String field;
    private final String symbol;
    
    
    public RecurlyException(GPathResult xml, String field, String symbol, String message) {
        super(message);
        this.xml = xml;
        this.field = field;
        this.symbol = symbol;
    }
    
    public String getField() {
        return field;
    }
    
    public String getSymbol() {
        return symbol;
    }

    public GPathResult getXml() {
        return xml;
    }
}
