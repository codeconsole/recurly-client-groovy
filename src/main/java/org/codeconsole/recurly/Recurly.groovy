package org.codeconsole.recurly

import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilder

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest
import org.apache.commons.codec.binary.Base64
import java.text.ParseException
import groovy.util.logging.Log

@Log
public class Recurly {
    public static String apiKey
    public static String privateKey
    
    static String getApiKey(){
        assert apiKey
        apiKey
    }
    
    static String getPrivateKey(){
        assert privateKey
        privateKey
    }

    public String signSubscription(String planCode, String accountCode, Map extras = [:], long timestamp = (System.currentTimeMillis() / 1000 as long)) {
        return sign("subscriptioncreate", ["plan_code":planCode, "account_code":accountCode], extras, timestamp)
    }

    public String signTransaction(long amountInCents, String currency = "USD", String accountCode,  Map extras = [:], long timestamp = (System.currentTimeMillis() / 1000 as long)) {
        return sign("transactioncreate", ["amount_in_cents":amountInCents, "currency":currency, "account_code":accountCode], extras, timestamp)
    }

    public String signBillingInfo(String accountCode, Map extras = [:], long timestamp = (System.currentTimeMillis() / 1000 as long)) {
        return sign("billinginfoupdate", ["account_code":accountCode], extras, timestamp)
    }

    private String collectKeypaths(extras, String prefix = null) {
        (extras instanceof Map)?
            extras.collect { key, value ->
                collectKeypaths(value, (prefix? "${prefix}.${key}" : key.toString()))
            }.flatten().sort().join(",") : prefix
    }
    
    public String sign(String claim, Map params, Map extras, long timestamp = (System.currentTimeMillis() / 1000 as long))  {
        params = (params + extras).sort()
        String message = "[${timestamp},${claim},[${params.collect{"${it.key}:${it.value}"}.join(",")}]]"
        String result = "${sha1hmacSha1(privateKey, message)}-${timestamp}"
        log.fine "Message: ${message}"
        String paths = collectKeypaths(extras)
        paths? [result, paths].join('+') : result
    }

    private String toQuery(object, String key = null) {
        (object instanceof Map)?
            object.collect { k, v -> toQuery(v, key? "${key}[${k}]" : k )}.sort().join("&")  :
            "${URLEncoder.encode(key, 'UTF-8')}=${URLEncoder.encode(object.toString(), 'UTF-8')}"
    }

    private String sha1hmacSha1(String key, String value) {
        Mac mac = Mac.getInstance("HmacSHA1")
        mac.init(new SecretKeySpec(MessageDigest.getInstance("SHA-1").digest(key.getBytes()), "HmacSHA1"))
        toHex(mac.doFinal(value.getBytes()))
    }

    private String hmacSha1(String key, String value) {
        Mac mac = Mac.getInstance("HmacSHA1")
        mac.init(new SecretKeySpec(key.getBytes(), "HmacSHA1"))
        mac.doFinal(value.getBytes()).encodeHex().toString()
    }

    // http://docs.recurly.com/api/recurlyjs/signatures
    public String sign21(Map params, String nonce = generator( (('a'..'z')+('0'..'9')).join(), 32), long timestamp = (System.currentTimeMillis() / 1000 as long)) {
        Map data = (params + [
                nonce : nonce,
                timestamp : timestamp ]).sort()
        String unsigned = toQuery(data)
        return [hmacSha1(privateKey, unsigned), unsigned].join('|')
    }

    public static Date convertDate(String date) {
        String parseDate = date
        try {
            //"2012-04-19T09:50:30Z"
            return Date.parse("yyyy-MM-dd'T'HH:mm:ssz", parseDate.replaceAll("Z", "GMT"))
        } catch (ParseException pe) {
            //"2012-04-19T09:50:30-0700"
            try {
                log.warning("${date}")
                return Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", parseDate.substring(0, parseDate.lastIndexOf(':')) + parseDate.substring(parseDate.lastIndexOf(':') + 1))
            } catch (ParseException pe2) {
                //"2012-04-19T09:50:30-07:00"
                return Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", parseDate)
            }
        }
    }

    // http://stackoverflow.com/questions/8138164/groovy-generate-random-string-from-given-character-set
    private String generator(String alphabet, int n) {
      new Random().with {
        (1..n).collect { alphabet[nextInt(alphabet.length())] }.join()
      } as String
    }

    private static HttpURLConnection createConnection(String url) {
        HttpURLConnection httpConnection = (HttpURLConnection) new URL("https://api.recurly.com/v2${url}").openConnection()
        httpConnection.setReadTimeout(45000)
        httpConnection.setConnectTimeout(45000)
        String encodedLogin = URLEncoder.encode(new Base64().encodeToString("${apiKey}:".getBytes()), 'UTF-8')
        httpConnection.setRequestProperty("Authorization", "Basic ${encodedLogin}")
        httpConnection
    }

    private static HttpURLConnection createXmlConnection(String url) {
        HttpURLConnection httpConnection = createConnection(url)
        httpConnection.setRequestProperty("Accept", "application/xml")
        httpConnection.setRequestProperty("Content-Type", "application/xml; charset=utf-8")
        httpConnection
    }

    public static String doPut(String url) {
        HttpURLConnection httpConnection = createXmlConnection("/${url}")
        httpConnection.setDoOutput(true)
        httpConnection.setRequestMethod("PUT")
        OutputStreamWriter out = new OutputStreamWriter(httpConnection.getOutputStream())
        out.close()
        httpConnection.getResponseCode()
        httpConnection.getResponseMessage()
    }

    public static String doPost(String url, String content) {
        HttpURLConnection httpConnection = createXmlConnection("/${url}")
        httpConnection.setDoOutput(true)
        httpConnection.setRequestMethod("POST")
        OutputStreamWriter out = new OutputStreamWriter(httpConnection.getOutputStream())
        out.write(content)
        out.close()
        httpConnection.getResponseCode()
        httpConnection.getResponseMessage()
    }
    
    public static GPathResult doPutWithXmlResponse(String url, String content) {
        HttpURLConnection httpConnection = createXmlConnection("/${url}")
        httpConnection.setDoOutput(true)
        httpConnection.setRequestMethod("PUT")
        OutputStreamWriter out = new OutputStreamWriter(httpConnection.getOutputStream())
        out.write(content)
        out.close()
        if(httpConnection.responseCode >= 400){
            String text = httpConnection.inputStream.text
            log.warning("Post to $url returned response code $httpConnection.responseCode with message $httpConnection.responseMessage\n\n$text")
            return parseXml(text)
        }
        parseXml httpConnection.inputStream.text
    }
    
    public static GPathResult doPostWithXmlResponse(String url, String content) {
        HttpURLConnection httpConnection = createXmlConnection("/${url}")
        httpConnection.setDoOutput(true)
        httpConnection.setRequestMethod("POST")
        OutputStreamWriter out = new OutputStreamWriter(httpConnection.getOutputStream())
        out.write(content)
        out.close()
        if(httpConnection.responseCode >= 400){
            String text = httpConnection.inputStream.text
            log.warning("Post to $url returned response code $httpConnection.responseCode with message $httpConnection.responseMessage\n\n$text")
            return parseXml(text)
        }
        parseXml httpConnection.inputStream.text
    }

    public static InputStream fetchPdf(String url) {
        HttpURLConnection httpConnection = createConnection("/${url}")
        httpConnection.setDoInput(true)
        httpConnection.setRequestProperty("Accept", "application/pdf")
        httpConnection.getInputStream()
    }

    public static Object fetch(String token) {
        GPathResult result = Recurly.fetchXml("/recurly_js/result/${token}")
        switch (result.name()) {
            case "subscription":
                return Subscription.fromXml(result)
            case "billing_info":
                return BillingInfo.fromXml(result)
            case "invoice":
                return Invoice.fromXml(result)
        }
        null
    }

    public static String fetchData(String url) {
        try {        
            createXmlConnection(url).getContent().text
        } catch (FileNotFoundException fne) {
            return null
        }             
    }

    public static GPathResult fetchXml(String url) {
        parseXml fetchData(url)
    }
    
    public static GPathResult parseXml(String xml) {
        GPathResult result = xml? new XmlSlurper().parseText(xml) : null
        throwExceptionOnError(result)
        return result
    }

    static throwExceptionOnError(GPathResult result) {
        if (result?.name() == "error") {
            if (result.symbol == 'not_found') {
                return null
            }
            if (result.children().size() == 0 && result.text()) {
                throw new RecurlyException(result, result.@field.toString(), result.@symbol.toString(), "${result.text()}".toString())
            }
            throw new RecurlyException(result, result.field.toString(), result.symbol.toString(), "${result.description}: ${result.details}".toString())
        }
        if (result?.name() == "errors"){
            throwExceptionOnError(result.error[0])
        }
        return null
    }
}