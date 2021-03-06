//package co.b4pay.api.controller.pay;
//
//import co.b4pay.api.common.exception.BizException;
//import co.b4pay.api.common.web.BaseController;
//import co.b4pay.api.model.Router;
//import co.b4pay.api.model.base.AjaxResponse;
//import co.b4pay.api.service.RouterService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.bind.annotation.RestController;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
///**
// * 个人收款码controller
// * Created with IntelliJ IDEA
// * Created By AZain
// * Date: 2018-10-27
// * Time: 16:49
// */
//@RestController
//@RequestMapping("pay/personal")
//public class PersonalCodePayController extends BaseController {
//
//    private static final Logger logger = LoggerFactory.getLogger(PersonalCodePayController.class);
//
//    private static final String ROUTER_KEY = "personalCodePay";
//
//    private static final String[] REQUIRED_PARAMS = new String[]{"tradeNo", "amount", "payWay","notifyUrl"};
//
//    @Autowired
//    private RouterService routerService;
//
//    //@RequestMapping(method = RequestMethod.POST)
//    //public AjaxResponse doPost(HttpServletRequest request, HttpServletResponse response) {
//    //    try {
//    //        Router router = routerService.findById(ROUTER_KEY);
//    //        if (router == null || router.getStatus() == -1) {
//    //            throw new RuntimeException(String.format("[%s]路由异常", ROUTER_KEY));
//    //        }
//    //        return personalCodePayService.executeWithFixed(getParams(request));
//    //    } catch (BizException e) {
//    //        logger.warn(e.getMessage());
//    //        return AjaxResponse.failure(e.getMessage());
//    //    } catch (Exception e) {
//    //        logger.error(e.getMessage(), e);
//    //        return AjaxResponse.failure();
//    //    }
//    //}
//
//
//    @Override
//    protected String[] getRequiredParams() {
//        return REQUIRED_PARAMS;
//    }
//
//
//
//}
