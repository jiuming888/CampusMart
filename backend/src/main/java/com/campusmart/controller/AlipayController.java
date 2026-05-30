package com.campusmart.controller;

import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.campusmart.common.Constants;
import com.campusmart.common.Result;
import com.campusmart.config.AlipayConfig;
import com.campusmart.entity.Order;
import com.campusmart.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/alipay")
@CrossOrigin(origins = "http://localhost:5173")
public class AlipayController {

    private static final Logger logger = LoggerFactory.getLogger(AlipayController.class);

    @Autowired
    private AlipayConfig alipayConfig;

    @Autowired
    private AlipayClient alipayClient;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Autowired
    private OrderService orderService;

    @PostMapping("/pay")
    public Map<String, Object> pay(
        @RequestParam Long orderId,
        @RequestParam(required = false) String frontendUrl,
        @RequestParam(required = false) String publicApiUrl
    ) {
        Map<String, Object> result = new HashMap<>();

        try {
            logger.info("收到支付请求，订单ID: {}", orderId);

            Order order = orderService.getById(orderId);
            if (order == null) {
                logger.warn("订单不存在，订单ID: {}", orderId);
                result.put("success", false);
                result.put("message", "订单不存在");
                return result;
            }

            if (!Constants.ORDER_PENDING.equals(order.getStatus())) {
                logger.warn("订单状态不正确，订单ID: {}, 状态: {}", orderId, order.getStatus());
                result.put("success", false);
                result.put("message", "订单状态不正确，无法支付");
                return result;
            }

            // 确保金额格式为两位小数（支付宝要求）
            String amount = String.format("%.2f", order.getTotalAmount());

            logger.info("开始构建支付宝支付请求，订单号: {}, 金额: {}",
                order.getOrderNo(), amount);

            AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
            String effectivePublicApiUrl = Optional.ofNullable(publicApiUrl)
                .filter(url -> !url.isBlank())
                .orElse(this.frontendUrl);
            String effectiveFrontendUrl = Optional.ofNullable(frontendUrl)
                .filter(url -> !url.isBlank())
                .orElse(this.frontendUrl);
            alipayRequest.setReturnUrl(
                UriComponentsBuilder.fromUriString(buildUrl(effectivePublicApiUrl, "/api/alipay/return"))
                    .queryParam("frontendUrl", effectiveFrontendUrl)
                    .build()
                    .encode()
                    .toUriString()
            );
            alipayRequest.setNotifyUrl(buildUrl(effectivePublicApiUrl, "/api/alipay/notify"));

            String bizContent = String.format(
                "{\"out_trade_no\":\"%s\",\"product_code\":\"FAST_INSTANT_TRADE_PAY\",\"total_amount\":\"%s\",\"subject\":\"校园闲置商品-%s\"}",
                order.getOrderNo(),
                amount,
                order.getOrderNo()
            );
            alipayRequest.setBizContent(bizContent);

            logger.info("发送支付请求到支付宝沙箱，业务参数: {}", bizContent);
            AlipayTradePagePayResponse response = alipayClient.pageExecute(alipayRequest, "GET");

            if (response.isSuccess()) {
                logger.info("支付宝支付链接生成成功，订单号: {}", order.getOrderNo());
                String payUrl = response.getBody();
                result.put("success", true);
                result.put("payUrl", payUrl);
            } else {
                logger.error("支付宝支付表单生成失败: code={}, msg={}, subCode={}, subMsg={}",
                    response.getCode(), response.getMsg(), response.getSubCode(), response.getSubMsg());
                result.put("success", false);
                result.put("message", "支付发起失败: " + response.getMsg() + " - " + response.getSubMsg());
            }
        } catch (Exception e) {
            logger.error("支付处理异常，订单ID: {}: ", orderId, e);
            result.put("success", false);
            result.put("message", "支付网关异常，请稍后重试或到订单页面完成支付");
        }
        return result;
    }

    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        try {
            Map<String, String> params = new HashMap<>();
            Map<String, String[]> requestParams = request.getParameterMap();
            for (String name : requestParams.keySet()) {
                String[] values = requestParams.get(name);
                String valueStr = "";
                for (int i = 0; i < values.length; i++) {
                    valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
                }
                params.put(name, valueStr);
            }

            logger.info("收到支付宝异步回调: {}", params);

            boolean signVerified = AlipaySignature.rsaCheckV1(
                params,
                alipayConfig.getAlipayPublicKey(),
                "UTF-8",
                "RSA2"
            );

            if (signVerified) {
                String tradeStatus = params.get("trade_status");
                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    String outTradeNo = params.get("out_trade_no");
                    String tradeNo = params.get("trade_no");
                    logger.info("支付成功，更新订单状态: {}", outTradeNo);

                    Order order = orderService.getByOrderNo(outTradeNo);
                    if (order != null) {
                        orderService.payOrder(order.getId(), tradeNo);
                        logger.info("订单 {} 更新支付状态成功，支付宝交易号: {}", outTradeNo, tradeNo);
                    }
                }
                return "success";
            }
            logger.warn("签名验证失败");
            return "fail";
        } catch (Exception e) {
            logger.error("异步回调处理异常", e);
            return "fail";
        }
    }

    @GetMapping("/return")
    public void returnUrl(
        @RequestParam String out_trade_no,
        @RequestParam(required = false) String frontendUrl,
        HttpServletResponse response
    ) throws IOException {
        logger.info("用户支付完成返回，订单号: {}", out_trade_no);
        String effectiveFrontendUrl = Optional.ofNullable(frontendUrl)
            .filter(url -> !url.isBlank())
            .orElse(this.frontendUrl);
        String redirectUrl = UriComponentsBuilder.fromUriString(buildUrl(effectiveFrontendUrl, "/pay-success"))
            .queryParam("orderNo", out_trade_no)
            .build()
            .encode()
            .toUriString();
        response.sendRedirect(redirectUrl);
    }

    private String buildUrl(String baseUrl, String path) {
        return baseUrl.replaceAll("/+$", "") + path;
    }

    /**
     * 前端主动查询订单支付状态（用于支付页面返回后确认）
     */
    @GetMapping("/query")
    public Result<Map<String, Object>> queryOrderStatus(@RequestParam String orderNo) {
        Map<String, Object> data = new HashMap<>();
        try {
            Order order = orderService.getByOrderNo(orderNo);
            if (order == null) {
                return Result.error("订单不存在");
            }
            // 如果本地已是已支付，直接返回
            if (Constants.ORDER_PAID.equals(order.getStatus()) ||
                Constants.ORDER_SHIPPED.equals(order.getStatus()) ||
                Constants.ORDER_COMPLETED.equals(order.getStatus())) {
                data.put("paid", true);
                data.put("status", order.getStatus());
                return Result.success(data);
            }
            // 向支付宝查询真实状态
            AlipayTradeQueryRequest queryRequest = new AlipayTradeQueryRequest();
            queryRequest.setBizContent("{\"out_trade_no\":\"" + orderNo + "\"}");
            AlipayTradeQueryResponse queryResponse = alipayClient.execute(queryRequest);
            if (queryResponse.isSuccess() && "TRADE_SUCCESS".equals(queryResponse.getTradeStatus())) {
                orderService.payOrder(order.getId(), queryResponse.getTradeNo());
                data.put("paid", true);
                data.put("status", Constants.ORDER_PAID);
                logger.info("支付宝主动查询确认支付成功，订单号: {}", orderNo);
                return Result.success(data);
            } else {
                data.put("paid", false);
                data.put("status", order.getStatus());
                data.put("tradeStatus", queryResponse.getTradeStatus());
                return Result.success(data);
            }
        } catch (Exception e) {
            logger.error("查询订单支付状态异常: ", e);
            return Result.error(e.getMessage());
        }
    }
}
