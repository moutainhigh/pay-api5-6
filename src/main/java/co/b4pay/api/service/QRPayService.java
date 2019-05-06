package co.b4pay.api.service;

import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.enums.ChannelType;
import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.common.utils.DateUtil;
import co.b4pay.api.common.utils.HttpsUtils;
import co.b4pay.api.common.utils.WebUtil;
import co.b4pay.api.model.*;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static co.b4pay.api.common.utils.DateUtil.now;

/**
 * mall支付
 *
 * @author zgp
 */
@Service
//@Transactional
public class QRPayService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(QRPayService.class);


    /**
     * 支付链接
     */
    //private static final String MALLPAY_API_DOMAIN = MainConfig.getConfig("MALLPAY_API_DOMAIN");

    private HmacSHA1Signature signature = new HmacSHA1Signature();


    public JSONObject executeReturn(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws BizException {

        logger.info("QRPayService-->executeReturn:" + params);
        BigDecimal totalAmount = new BigDecimal(params.getString("totalAmount"));
        //装换金额为元的单位
        BigDecimal totalMOney = totalAmount.divide(new BigDecimal("100"), 2, BigDecimal.ROUND_UP);
        logger.warn("支付金额为:" + totalMOney);
        Channel channel = getChannel(merchantId, router, totalMOney);// 预校验
        logger.warn("通道:" + channel.getName());
        if (channel.getIp4() == null) {
            channel.setStatus(-1);
            channel.setUpdateTime(now());
            channelDao.save(channel);
            throw new BizException("渠道地址设置异常");
        }
        Map m = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            m.put(parameterName, request.getParameter(parameterName));
        }
        m.put("channelId", channel.getId().toString());
        m.remove("signature");
        try {
            String content = SignatureUtil.getSignatureContent(m, true);
            String sign = signature.sign(content, merchantDao.getOne(merchantId).getSecretKey(), Constants.CHARSET_UTF8);
            m.put("signature", sign);
            String result = HttpsUtils.post(channel.getIp4() + "/pay/qrPayExecute.do", null, m);
            logger.warn("result:" + result);
            return JSONObject.parseObject(result);
        } catch (Exception e) {
            throw new BizException(e.getMessage());
        }

    }

    public JSONObject execute(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws Exception {
        logger.info("qrPayService-->execute:" + params);
        String serverUrl = WebUtil.getServerUrl(request);
        logger.warn("server url :" + serverUrl);
        long time = System.currentTimeMillis();
        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        BigDecimal totalAmount = new BigDecimal(params.getString("totalAmount"));
        //转换为单位为元的金额
        BigDecimal totalMOney = totalAmount.divide(new BigDecimal("100"), 2, BigDecimal.ROUND_UP);
        Channel channel = null;
        if (params.containsKey("channelId")) {
            channel = channelDao.getOne(Long.parseLong(params.getString("channelId")));
        }
        if (channel == null || channel.getStatus() < 0) {
            throw new BizException("渠道异常,请稍后再试！");
        }
        if (channel.getUnitPrice().compareTo(totalMOney) < 0) {
            throw new BizException(String.format("单笔交易不能大于%s元", channel.getUnitPrice()));
        }
        if (channel.getMinPrice().compareTo(totalMOney) > 0) {
            throw new BizException(String.format("单笔交易不能低于%s元", channel.getMinPrice()));
        }
        if (StringUtils.isBlank(channel.getGoodsTypeId())) {
            throw new BizException("渠道商品类别为空");
        }
        List<Goods> goodsList = goodsDao.findByTypeId(Integer.valueOf(channel.getGoodsTypeId()));
        if (goodsList.size() == 0) {
            throw new BizException(String.format("渠道商品类别为空,类别序列：%s)", channel.getGoodsTypeId()));
        }

        MerchantRate merchantRate = merchantRateDao.findByMerchantIdAndRouterId(merchantId, router.getId());
        if (merchantRate == null) {
            throw new BizException(String.format("[%s, %s]商户费率设置异常", merchantId, router.getId()));
        }
        if (totalMOney.subtract(merchantRate.getPayCost()).doubleValue() <= 0) {
            throw new BizException(String.format("[%s]支付金额不能少于%s元", router.getId(), merchantRate.getPayCost()));
        }

        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        String outTradeNo = params.getString("tradeNo");//"tradeprecreate" + System.currentTimeMillis() + (long) (Math.random() * 10000000L);
        Trade byMerchantOrderNotrade = tradeDao.findByMerchantOrderNo(outTradeNo);
        if (byMerchantOrderNotrade != null){
            Merchant merchant2 = merchantDao.getOne(merchantId);
            merchant2.setStatus(-1);
            merchantDao.save(merchant2);
            throw new BizException(String.format("[%s,%s]订单重复,冻结账户!",merchantId,outTradeNo));
        }
        // B4系统订单号
        String tradeId = String.format("%s%s", DateUtil.dateToStr(DateUtil.getTime(), DateUtil.YMdhmsS_noSpli), RandomStringUtils.randomNumeric(15));//交易订单号

        //订单时间
        String orderTime = params.getString("time");
        //回调地址
        String notifyUrl = params.getString("notifyUrl");
        //支付方式
        String payType = params.getString("type");
        int type = Integer.valueOf(payType).intValue();
        //得到所有状态为开始的二维码通道
        List<QRChannel> qrChannelList = qrChannelDao.findByStatus(1);
        //组装响应参数
        JSONObject jsonObject = new JSONObject();
        Long qrcodeid=null;
        qrcode qrcode = null;
        if ("0".equals(payType)){
            //轮询校验后得到二维码
            qrcode = qrCheckOutService.checkout(qrChannelList, totalMOney, type, request);
            String codeData = qrcode.getCodeData();
            qrcodeid=qrcode.getId();
            jsonObject.put("qrcode", codeData);
        }else if ("1".equals(payType)){
            //轮询校验后得到二维码
            qrcode = qrCheckOutService.checkout(qrChannelList, totalMOney, type, request);
            String codeData = qrcode.getCodeData();
            qrcodeid=qrcode.getId();
            jsonObject.put("qrcode", codeData);
        }else if("2".equals(payType)){
            //轮询校验后得到二维码
            qrcode = qrCheckOutService.checkout(qrChannelList, totalMOney, type, request);
            String codeData = qrcode.getCodeData();
            qrcodeid=qrcode.getId();
            jsonObject.put("qrcode", codeData);
        }

        jsonObject.put("out_trade_no", outTradeNo);
        jsonObject.put("msg", "接口调用成功");
        jsonObject.put("code", "10000");
        //根据返回二维码信息中的商户号查询码商通道
        Long qrcodeMerchantId = qrcode.getMerchantId();
        Long id = qrcode.getId();
        QRChannel qrChannel = qrChannelDao.findByMerchantIdaAndId(qrcodeMerchantId);
        BigDecimal serviceCharge = totalMOney.multiply(merchantRate.getCostRate(), new MathContext(2, RoundingMode.HALF_UP)).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_UP).add(merchantRate.getPayCost());
        //String tradeId = String.format("%s%s", DateUtil.dateToStr(DateUtil.getTime(), DateUtil.YMdhmsS_noSpli), RandomStringUtils.randomNumeric(15));//交易订单号
        Trade trade = new Trade();
        trade.setId(tradeId);
        trade.setCostRate(merchantRate.getCostRate());
        trade.setPayCost(merchantRate.getPayCost());
        trade.setTotalAmount(totalMOney);
        trade.setRequestAmount(totalMOney);
        trade.setMerchantId(merchantId);
        trade.setChannelId(channel.getId());
        trade.setQrchannelId(qrChannel.getId());
        trade.setQrcodeId(qrcodeid);
        trade.setServiceCharge(serviceCharge); // 服务费
        trade.setAccountAmount(totalMOney.subtract(serviceCharge));
        trade.setNotifyUrl(notifyUrl);
        trade.setMerchantOrderNo(outTradeNo);
        trade.setRequest(params.toJSONString());
        trade.setResponse("");
        trade.setTime(System.currentTimeMillis() - time);
        trade.setFzStatus(0);
        trade.setTradeState(0);
        trade.setStatus(1);
        trade.setPayOrderNo(tradeId);
        tradeDao.save(trade);
        logger.warn("MALL trade ->" + JSONObject.toJSONString(trade));
        JobTrade jobTrade = new JobTrade();
        jobTrade.setId(trade.getId());
        jobTrade.setStatus(0);
        jobTrade.setCount(0);
        jobTrade.setChannelType(ChannelType.SHPAY);
        jobTrade.setNotifyUrl(notifyUrl);
        jobTradeDao.save(jobTrade);
        logger.info("响应参数为:"+jsonObject.toJSONString());
        qrCheckOutService.timer1(outTradeNo);
        return jsonObject;
    }


}