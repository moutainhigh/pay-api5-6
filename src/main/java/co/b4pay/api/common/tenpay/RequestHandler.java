package co.b4pay.api.common.tenpay;

import co.b4pay.api.common.tenpay.util.MD5Util;
import co.b4pay.api.common.tenpay.util.TenpayUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 请求处理类 请求处理类继承此类，重写createSign方法即可。
 *
 * @author miklchen
 */
public class RequestHandler {

    /**
     * 网关url地址
     */
    private String gateUrl;

    /**
     * 密钥
     */
    private String key;

    /**
     * 请求的参数
     */
    private final SortedMap<String, String> parameters;

    /**
     * debug信息
     */
    private String debugInfo;

    protected HttpServletRequest request;

    protected HttpServletResponse response;

    /**
     * 构造函数
     *
     * @param request
     * @param response
     */
    public RequestHandler(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;

        this.gateUrl = "https://gw.tenpay.com/gateway/pay.htm";
        this.key = "";
        this.parameters = new TreeMap<String, String>();
        this.debugInfo = "";
    }

    /**
     * 初始化函数。
     */
    public void init() {
        // nothing to do
    }

    /**
     * 获取入口地址,不包含参数值
     */
    public String getGateUrl() {
        return gateUrl;
    }

    /**
     * 设置入口地址,不包含参数值
     */
    public void setGateUrl(String gateUrl) {
        this.gateUrl = gateUrl;
    }

    /**
     * 获取密钥
     */
    public String getKey() {
        return key;
    }

    /**
     * 设置密钥
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * 获取参数值
     *
     * @param parameter 参数名称
     * @return String
     */
    public String getParameter(String parameter) {
        String s = this.parameters.get(parameter);
        return (null == s) ? "" : s;
    }

    /**
     * 设置参数值
     *
     * @param parameter      参数名称
     * @param parameterValue 参数值
     */
    public void setParameter(String parameter, String parameterValue) {
        String v = "";
        if (null != parameterValue) {
            v = parameterValue.trim();
        }
        this.parameters.put(parameter, v);
    }

    /**
     * 返回所有的参数
     *
     * @return SortedMap
     */
    public SortedMap<String, String> getAllParameters() {
        return this.parameters;
    }

    /**
     * 获取debug信息
     */
    public String getDebugInfo() {
        return debugInfo;
    }

    /**
     * 获取带参数的请求URL
     *
     * @return String
     * @throws UnsupportedEncodingException
     */
    public String getRequestURL() throws UnsupportedEncodingException {

        this.createSign();

        StringBuffer sb = new StringBuffer();
        String enc = TenpayUtil.getCharacterEncoding(this.request, this.response);
        Set<Entry<String, String>> es = this.parameters.entrySet();
        Iterator<Entry<String, String>> it = es.iterator();
        while (it.hasNext()) {
            Entry<String, String> entry = it.next();
            String k = entry.getKey();
            String v = entry.getValue();

            if (!"spbill_create_ip".equals(k)) {
                sb.append(k + "=" + URLEncoder.encode(v, enc) + "&");
            } else {
                sb.append(k + "=" + v.replace("\\.", "%2E") + "&");
            }
        }

        // 去掉最后一个&
        String reqPars = sb.substring(0, sb.lastIndexOf("&"));

        return this.getGateUrl() + "?" + reqPars;

    }

    public void doSend() throws UnsupportedEncodingException, IOException {
        this.response.sendRedirect(this.getRequestURL());
    }

    public String getXmlBody() {
        StringBuffer sb = new StringBuffer();
        Set<Entry<String, String>> es = this.getAllParameters().entrySet();
        Iterator<Entry<String, String>> it = es.iterator();
        while (it.hasNext()) {
            Entry<String, String> entry = it.next();
            String k = entry.getKey();
            String v = entry.getValue();
            if (!"appkey".equals(k)) {
                sb.append("<" + k + ">" + v + "</" + k + ">" + "\r\n");
            }
        }
        return sb.toString();
    }

    /**
     * 创建md5摘要,规则是:按参数名称a-z排序,遇到空值的参数不参加签名。
     */
    protected void createSign() {
        StringBuffer sb = new StringBuffer();
        Set<Entry<String, String>> es = this.parameters.entrySet();
        Iterator<Entry<String, String>> it = es.iterator();
        while (it.hasNext()) {
            Entry<String, String> entry = it.next();
            String k = entry.getKey();
            String v = entry.getValue();
            if (null != v && !"".equals(v) && !"sign".equals(k) && !"key".equals(k)) {
                sb.append(k + "=" + v + "&");
            }
        }
        sb.append("key=" + this.getKey());

        String enc = TenpayUtil.getCharacterEncoding(this.request, this.response);
        String sign = MD5Util.MD5Encode(sb.toString(), enc).toUpperCase();

        this.setParameter("sign", sign);

        // debug信息
        this.setDebugInfo(sb.toString() + " => sign:" + sign);

    }

    /**
     * 设置debug信息
     */
    protected void setDebugInfo(String debugInfo) {
        this.debugInfo = debugInfo;
    }

    protected HttpServletRequest getHttpServletRequest() {
        return this.request;
    }

    protected HttpServletResponse getHttpServletResponse() {
        return this.response;
    }

}