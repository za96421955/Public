package com.trade.hedge.service.impl;

import com.trade.analyse.model.trade.Track;
import com.trade.hedge.context.HedgeContext;
import com.trade.huobi.enums.ContractDirectionEnum;
import com.trade.huobi.enums.ContractOffsetEnum;
import com.trade.huobi.enums.ContractOrderPriceTypeEnum;
import com.trade.huobi.model.Result;
import com.trade.huobi.model.contract.Order;
import com.trade.huobi.model.contract.Position;
import com.trade.huobi.service.swap.SwapAccountService;
import com.trade.huobi.service.swap.SwapTradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 对冲服务：永续合约实现
 * <p>〈功能详细描述〉</p>
 *
 * @author 陈晨
 * @version 1.0
 * @date 2020/9/24
 */
@Service
public class SwapHedgeServiceImpl extends AbstractHedgeService {

    @Autowired
    private SwapAccountService swapAccountService;
    @Autowired
    private SwapTradeService swapTradeService;

    @Override
    protected List<Position> getPositionList(Track track) {
        return swapAccountService.getPositionList(track.getAccess(), track.getSecret(), track.getContractCode());
    }

    @Override
    protected Result open(Track track, ContractDirectionEnum direction, long volume) {
        return swapTradeService.order(track.getAccess(), track.getSecret(), track.getContractCode()
                , null, volume, direction, ContractOffsetEnum.OPEN
                , track.getLeverRate(), ContractOrderPriceTypeEnum.OPTIMAL_5);
    }

    @Override
    protected Result close(Track track, Position position) {
        return swapTradeService.order(track.getAccess(), track.getSecret(), track.getContractCode()
                , null, position.getVolume().longValue()
                , ContractDirectionEnum.get(position.getDirection()).getNegate(), ContractOffsetEnum.CLOSE
                , track.getLeverRate(), ContractOrderPriceTypeEnum.OPTIMAL_5);
    }

    @Override
    protected Result cancel(Track track) {
        return swapTradeService.cancelAll(track.getAccess(), track.getSecret(), track.getContractCode());
    }

    @Override
    protected boolean isStopTrade(Track track, Position position) {
        // 停止交易, 无持仓 || 平仓张数 > basis, 则不再向下追仓
        if (!HedgeContext.isStopTrade()) {
            return false;
        }
        Position positionCheck = swapAccountService.getPositionInfo(track.getAccess(), track.getSecret(), track.getContractCode());
        return positionCheck == null || position.getVolume().compareTo(BigDecimal.valueOf(track.getBasisVolume())) > 0;
    }

    @Override
    protected Order getOrderInfo(Track track, String orderId) {
        return swapTradeService.getOrderInfo(track.getAccess(), track.getSecret(), track.getContractCode(), orderId);
    }

}


