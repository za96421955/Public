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
 * 对冲服务：交割合约实现
 * <p>〈功能详细描述〉</p>
 *
 * @author 陈晨
 * @version 1.0
 * @date 2020/9/24
 */
@Service
public class HedgeServiceImpl extends AbstractHedgeService {

    @Autowired
    private ContractAccountService contractAccountService;
    @Autowired
    private ContractTradeService contractTradeService;

    @Override
    public Result positionCheck(Track track) {
        // 持仓检查
        List<Position> positionList = contractAccountService.getPositionList(track.getAccess(), track.getSecret(), track.getSymbol());
        Position buy = null;
        Position sell = null;
        for (Position position : positionList) {
            if (position == null) {
                continue;
            }
            if (ContractDirectionEnum.BUY.getValue().equals(position.getDirection())) {
                buy = position;
            }
            if (ContractDirectionEnum.SELL.getValue().equals(position.getDirection())) {
                sell = position;
            }
        }
        logger.info("[{}] track={}, isStopTrade={}, buy={}, sell={}, 持仓检查"
                , LOG_MARK, track, HedgeContext.isStopTrade(), buy, sell);

        // 开多下单
        if (!HedgeContext.isStopTrade() && buy == null) {
            Result result = this.open(track, ContractDirectionEnum.BUY, track.getBasisVolume());
            logger.info("[{}] track={}, result={}, 开多下单", LOG_MARK, track, result);
        }
        // 开空下单
        if (!HedgeContext.isStopTrade() && sell == null) {
            Result result = this.open(track, ContractDirectionEnum.SELL, track.getBasisVolume());
            logger.info("[{}] track={}, result={}, 开空下单", LOG_MARK, track, result);
        }
        if (!HedgeContext.isStopTrade() && (buy == null || sell == null)) {
            return Result.buildFail("开多/开空, 无持仓");
        }
        // 0: 多, 1: 空
        return Result.buildSuccess(buy, sell);
    }

    @Override
    public void closeCheck(Track track, Position up, Position down) {
        if (track == null) {
            return;
        }
        // 计算止盈倍数
        BigDecimal upIncomeMultiple = BigDecimal.ONE;
        BigDecimal downIncomeMultiple = BigDecimal.ONE;
        if (up != null && down != null) {
            upIncomeMultiple = down.getVolume().divide(up.getVolume(), new MathContext(2));
            downIncomeMultiple = up.getVolume().divide(down.getVolume(), new MathContext(2));
        }
        if (upIncomeMultiple.compareTo(BigDecimal.ONE) < 0) {
            upIncomeMultiple = BigDecimal.ONE;
        }
        if (downIncomeMultiple.compareTo(BigDecimal.ONE) < 0) {
            downIncomeMultiple = BigDecimal.ONE;
        }
        // 计算止损张数
        long closeUpLossVolume = track.getBasisVolume();
        if (down != null) {
            closeUpLossVolume = down.getVolume().longValue();
        }
        long closeDownLossVolume = track.getBasisVolume();
        if (up != null) {
            closeDownLossVolume = up.getVolume().longValue();
        }

        // 2, 开多平仓检查
        this.closeCheck(track, up, upIncomeMultiple, closeUpLossVolume);
        // 3, 开空平仓检查
        this.closeCheck(track, down, downIncomeMultiple, closeDownLossVolume);
    }

    @Override
    public void closeCheck(Track track, Position position, BigDecimal incomeMultiple, long lossVolume) {
        if (track == null || position == null) {
            return;
        }
        // 判断是否可以平仓
        if (!this.isClose(track, position, incomeMultiple, BigDecimal.ZERO)) {
            return;
        }
        // 平仓下单
        Result result = this.closeOrder(track, position, lossVolume);
        if (result.success()) {
            return;
        }
        // 平仓下单失败, 则全部撤单, 重新下单
        result = contractTradeService.cancelAll(track.getAccess(), track.getSecret(), track.getSymbol());
        logger.info("[{}] track={}, position={}, result={}, 平仓下单失败, 则全部撤单, 重新下单"
                , LOG_MARK, track, position, result);
        this.closeOrder(track, position, lossVolume);
    }

    @Override
    protected Result open(Track track, ContractDirectionEnum direction, long volume) {
        return contractTradeService.order(track.getAccess(), track.getSecret(), track.getSymbol(), ContractTypeEnum.THIS_WEEK
                , null, volume, direction, ContractOffsetEnum.OPEN
                , track.getLeverRate(), ContractOrderPriceTypeEnum.OPTIMAL_5);
    }

    @Override
    protected Result close(Track track, Position position) {
        return contractTradeService.order(track.getAccess(), track.getSecret(), track.getSymbol(), ContractTypeEnum.THIS_WEEK
                , null, position.getVolume().longValue()
                , ContractDirectionEnum.get(position.getDirection()).getNegate(), ContractOffsetEnum.CLOSE
                , track.getLeverRate(), ContractOrderPriceTypeEnum.OPTIMAL_5);
    }

    @Override
    protected boolean isStopTrade(Track track, Position position) {
        // 停止交易, 无持仓 || 平仓张数 > basis, 则不再向下追仓
        if (!HedgeContext.isStopTrade()) {
            return false;
        }
        Position positionCheck = contractAccountService.getPositionInfo(track.getAccess(), track.getSecret(), track.getSymbol());
        return positionCheck == null || position.getVolume().compareTo(BigDecimal.valueOf(track.getBasisVolume())) > 0;
    }

    @Override
    protected Order getOrderInfo(Track track, String orderId) {
        return contractTradeService.getOrderInfo(track.getAccess(), track.getSecret(), track.getSymbol(), orderId);
    }

}


