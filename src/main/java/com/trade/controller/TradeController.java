package com.trade.controller;

import com.trade.analyse.context.TradeContext;
import com.trade.hedge.context.HedgeContext;
import com.trade.huobi.model.Result;
import com.trade.analyse.model.trade.Track;
import com.trade.huobi.enums.ContractLeverRateEnum;
import com.trade.huobi.enums.SymbolEnum;
import com.trade.analyse.service.trade.OrderService;
import com.trade.analyse.service.trade.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 控制器：交易
 * <p>〈功能详细描述〉</p>
 *
 * @author 陈晨
 * @version 1.0
 * @date 2020/9/9
 */
@RestController
@RequestMapping("/trade")
public class TradeController extends BaseController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private TradeService tradeService;

    @GetMapping("/analyse/{symbol}")
    @Description("获取实时分析数据")
    public Result analyse(@PathVariable String symbol) {
        try {
            return Result.buildSuccess(TradeContext.getAnalyse());
        } catch (Exception e) {
            logger.error("[交易] symbol={}, 获取实时分析数据异常, {}", symbol, e.getMessage(), e);
            return Result.buildFail(e.getMessage());
        }
    }

    @PostMapping("/zhang/{symbol}/{leverRate}")
    @Description("获取可用张数")
    public Result zhang(String access, String secret, @PathVariable String symbol, @PathVariable String leverRate) {
        try {
            int volume = orderService.getAvailableVolume(access, secret, SymbolEnum.get(symbol), ContractLeverRateEnum.get(leverRate));
            return Result.buildSuccess(volume);
        } catch (Exception e) {
            logger.error("[交易] symbol={}, leverRate={}, 获取可用张数异常, {}", symbol, leverRate, e.getMessage(), e);
            return Result.buildFail(e.getMessage());
        }
    }

    @PostMapping("/order/{symbol}")
    @Description("委托交易")
    public Track order(String access, String secret, @PathVariable String symbol
            , String leverRate, long basisVolume, BigDecimal incomePricePlan) {
        return TradeContext.getTrack(access).setSecret(secret)
                .setSymbol(SymbolEnum.get(symbol))
                .setLeverRate(ContractLeverRateEnum.get(leverRate))
                .setBasisVolume(basisVolume)
                .setIncomePricePlan(incomePricePlan);
    }

    @PostMapping("/check/{symbol}/{type}")
    @Description("订单检查")
    public Result check(String access, String secret, @PathVariable String symbol, @PathVariable int type) {
        try {
            return tradeService.checkOpen(new Track(access, secret).setSymbol(SymbolEnum.get(symbol)));
        } catch (Exception e) {
            logger.error("[交易] symbol={}, type={}, 订单检查异常, {}", symbol, type, e.getMessage(), e);
            return Result.buildFail(e.getMessage());
        }
    }

    @GetMapping("/changeTrade")
    @Description("交易切换")
    public String changeTrade() {
        HedgeContext.setStopTrade(!HedgeContext.isStopTrade());
        return "isStopTrade: " + HedgeContext.isStopTrade();
    }

}


