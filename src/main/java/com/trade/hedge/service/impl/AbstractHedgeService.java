package com.trade.hedge.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.trade.BaseService;
import com.trade.analyse.model.trade.Track;
import com.trade.hedge.context.HedgeContext;
import com.trade.hedge.service.HedgeService;
import com.trade.huobi.enums.*;
import com.trade.huobi.model.Result;
import com.trade.huobi.model.contract.Order;
import com.trade.huobi.model.contract.Position;
import com.trade.huobi.model.spot.Kline;
import com.trade.huobi.service.contract.ContractAccountService;
import com.trade.huobi.service.contract.ContractTradeService;
import com.trade.huobi.service.spot.SpotMarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

/**
 * 对冲服务实现
 * <p>〈功能详细描述〉</p>
 *
 * @author 陈晨
 * @version 1.0
 * @date 2020/9/24
 */
public abstract class AbstractHedgeService extends BaseService implements HedgeService {

    @Autowired
    protected SpotMarketService spotMarketService;

    /**
     * @description 开仓
     * <p>〈功能详细描述〉</p>
     *
     * @author 陈晨
     * @date 2020/9/24 15:55
     * @param track, direction, volume
     **/
    protected abstract Result open(Track track, ContractDirectionEnum direction, long volume);

    /**
     * @description 平仓
     * <p>〈功能详细描述〉</p>
     *
     * @author 陈晨
     * @date 2020/9/24 15:55
     * @param track, direction
     **/
    protected abstract Result close(Track track, Position position);

    /**
     * @description 平仓下单
     * <p>〈功能详细描述〉</p>
     *
     * @author 陈晨
     * @date 2020/9/24 16:46
     **/
    protected Result closeOrder(Track track, Position position, long lossVolume) {
        // 1, 止盈平仓（所有张）
        Result result = this.close(track, position);
        logger.info("[{}] track={}, direction={}, price={}, volume={}, result={}, 止盈平仓（所有张）"
                , LOG_MARK, track, position.getDirection(), position.getCostHold(), position.getVolume(), result);
        if (!result.success()) {
            return result;
        }
        // 订单完成检查
        if (!this.orderCompleteCheck(track, result, 0)) {
            logger.info("[{}] track={}, direction={}, 止盈平仓检查, 超时", LOG_MARK, track, position.getDirection());
            return Result.buildFail("止盈平仓检查, 超时");
        }
        // 停止交易, 则不再向下追仓
        if (this.isStopTrade(track, position)) {
            return Result.buildSuccess();
        }

        // 2, 同向开仓（basis张）
        result = this.open(track, ContractDirectionEnum.get(position.getDirection()), track.getBasisVolume());
        logger.info("[{}] track={}, direction={}, result={}, 同向开仓（1张）"
                , LOG_MARK, track, position.getDirection(), result);
        if (!result.success()) {
            return result;
        }
        // 订单完成检查
        if (!this.orderCompleteCheck(track, result, 0)) {
            logger.info("[{}] track={}, direction={}, 同向开仓检查, 超时", LOG_MARK, track, position.getDirection());
            return Result.buildFail("同向开仓检查, 超时");
        }

        // 3, 逆向止损加仓（1张）
        result = this.open(track, ContractDirectionEnum.get(position.getDirection()).getNegate(), lossVolume);
        logger.info("[{}] track={}, direction={}, result={}, 逆向止损加仓（1张）"
                , LOG_MARK, track, ContractDirectionEnum.get(position.getDirection()).getNegate(), result);
        return Result.buildSuccess();
    }

    /**
     * @description 是否停止交易
     * <p>〈功能详细描述〉</p>
     *
     * @author 陈晨
     * @date 2020/9/26 13:30
     * @param track, position
     **/
    protected abstract boolean isStopTrade(Track track, Position position);

    /**
     * @description 订单完成检查
     * <p>〈功能详细描述〉</p>
     *
     * @author 陈晨
     * @date 2020/9/24 20:34
     **/
    protected boolean orderCompleteCheck(Track track, Result result, int count) {
        if (count > 30) {
            return false;
        }
        String orderId = JSONObject.parseObject(result.getData().toString()).getLong("order_id") + "";
        Order order = this.getOrderInfo(track, orderId);
        if (order != null && order.getStatus() == 6) {
            return true;
        }
        this.sleep(1000);
        return this.orderCompleteCheck(track, result, ++count);
    }
    
    /**
     * @description 获取指定订单信息
     * <p>〈功能详细描述〉</p>
     * 
     * @author 陈晨
     * @date 2020/9/26 13:29
     * @param track, orderId
     **/
    protected abstract Order getOrderInfo(Track track, String orderId);

    /**
     * @description 判断是否可以平仓
     * <p>〈功能详细描述〉</p>
     *
     * @author 陈晨
     * @date 2020/9/24 15:45
     **/
    protected boolean isClose(Track track, Position position, BigDecimal incomeMultiple, BigDecimal lastIncomePrice) {
        // 获取现价信息
        Kline kline = spotMarketService.getKlineCurr(SymbolUSDTEnum.getUSDT(track.getSymbol().getValue()));
        if (kline == null) {
            return false;
        }
        // 获取当前收益价格
        BigDecimal incomePrice = this.getIncomePrice(position, kline.getClose());
        // 若未达到计划收益, 则不平仓
        if (incomePrice.compareTo(track.getIncomePricePlan().multiply(incomeMultiple)) < 0) {
            return false;
        }
        logger.info("[{}] direction={}, price={}, curr={}, income={}, 达到计划收益条件, 平仓准备"
                , LOG_MARK, position.getDirection(), position.getCostHold(), kline.getClose(), incomePrice);

        // 若已收益, 则持续追踪收益价格, 直至收益出现回落, 则平仓
        if (lastIncomePrice.compareTo(incomePrice) > 0) {
            return true;
        }
        // N秒检查一次
        this.sleep(750);
        return this.isClose(track, position, incomeMultiple, incomePrice);
    }

    /**
     * @description 获取收益价格
     * <p>〈功能详细描述〉</p>
     *
     * @author 陈晨
     * @date 2020/9/24 15:35
     **/
    protected BigDecimal getIncomePrice(Position position, BigDecimal currPrice) {
        if (ContractDirectionEnum.BUY.getValue().equals(position.getDirection())) {
            // 开多收益: 当前价 > 持仓价 + 收益价
            return currPrice.subtract(position.getCostHold());
        } else {
            // 开空收益: 当前价 < 持仓价 - 收益价
            return position.getCostHold().subtract(currPrice);
        }
    }

}


