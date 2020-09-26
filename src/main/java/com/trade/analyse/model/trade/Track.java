package com.trade.analyse.model.trade;

import com.trade.analyse.context.TradeContext;
import com.trade.huobi.model.contract.Order;
import com.trade.huobi.enums.ContractDirectionEnum;
import com.trade.huobi.enums.ContractLeverRateEnum;
import com.trade.huobi.enums.ContractOffsetEnum;
import com.trade.huobi.enums.SymbolEnum;
import com.trade.analyse.service.trade.TradeService;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单追踪
 * <p>〈功能详细描述〉</p>
 *
 * @author 陈晨
 * @version 1.0
 * @date 2020/9/17
 */
@Getter
@Setter
public class Track implements Serializable {
    private static final long serialVersionUID = 1835677160307123550L;

    private String access;
    private String secret;
    private SymbolEnum symbol;
    private ContractLeverRateEnum leverRate;
    /** 基础张数 */
    private long basisVolume = 1;
    /** 计划收益价格 */
    private BigDecimal incomePricePlan = BigDecimal.ONE;

    /** 是否允许开仓 */
    private boolean isOpenAllow;
    /** 最后分析数据 */
    private Analyse lastAnalyse;
    /** 最后订单ID */
    private String lastOrderId;
    /** 最后开仓时间 */
    private Long lastOpenTime;
    /** 击穿次数 */
    private int breakdownNum;

    /** 最后撤销开仓方向 */
    private ContractDirectionEnum lastCancelDirection;
    /** 最后撤销时间 */
    private Long lastCancelTime;

    public Track(String access, String secret) {
        this.access = access;
        this.secret = secret;
    }

    public Track setAccess(String access) {
        this.access = access;
        return this;
    }

    public Track setSecret(String secret) {
        this.secret = secret;
        return this;
    }

    public Track setSymbol(SymbolEnum symbol) {
        this.symbol = symbol;
        return this;
    }

    public Track setLeverRate(ContractLeverRateEnum leverRate) {
        this.leverRate = leverRate;
        return this;
    }

    public Track setBasisVolume(long basisVolume) {
        this.basisVolume = basisVolume;
        return this;
    }

    public Track setIncomePricePlan(BigDecimal incomePricePlan) {
        this.incomePricePlan = incomePricePlan;
        return this;
    }

    public Analyse getLastAnalyse() {
        if (lastAnalyse == null) {
            lastAnalyse = TradeContext.getAnalyse();
        }
        return lastAnalyse;
    }

    /**
     * @description 清除开仓缓存信息
     * <p>〈功能详细描述〉</p>
     *
     * @author 陈晨
     * @date 2020/9/18 13:58
     **/
    public void clearOpen() {
        this.lastAnalyse = null;
        this.lastOrderId = null;
        this.lastOpenTime = null;
        this.breakdownNum = 0;
    }

    /**
     * @description 清澈撤单缓存
     * <p>〈功能详细描述〉</p>
     *
     * @author 陈晨
     * @date 2020/9/22 15:52
     **/
    public void clearCancel() {
        this.lastCancelDirection = null;
        this.lastCancelTime = null;
    }

    /**
     * @description 近期撤单, 禁止反向开仓检查
     * <p>〈功能详细描述〉</p>
     *
     * @author 陈晨
     * @date 2020/9/22 16:04
     **/
    public boolean cancelDisable(ContractDirectionEnum direction) {
        if (this.lastCancelDirection == null) {
            return false;
        }
        return !direction.equals(this.lastCancelDirection)
                && ((System.currentTimeMillis() - this.lastCancelTime) < TradeService.CANCEL_DISABLE_TIME);
    }

    /**
     * @description 近期止盈平仓, 禁止同向追仓检查
     * <p>〈功能详细描述〉</p>
     *
     * @author 陈晨
     * @date 2020/9/23 13:46
     **/
    public boolean profitDisable(Order order, Analyse analyse) {
        if (order == null || analyse == null
                || !ContractOffsetEnum.CLOSE.getValue().equals(order.getOffset())
                || order.getOffsetProfitloss().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return !order.getDirection().equals(analyse.getDirection().getValue())
                && ((System.currentTimeMillis() - order.getCreateDate()) < TradeService.CHASE_DISABLE_TIME);
    }

    /**
     * @description 近期止损平仓, 禁止逆向追仓检查
     * <p>〈功能详细描述〉</p>
     *
     * @author 陈晨
     * @date 2020/9/24 10:58
     * @param order, analyse
     **/
    public boolean lossDisable(Order order, Analyse analyse) {
        if (order == null || analyse == null
                || !ContractOffsetEnum.CLOSE.getValue().equals(order.getOffset())
                || order.getOffsetProfitloss().compareTo(BigDecimal.ZERO) >= 0) {
            return false;
        }
        return order.getDirection().equals(analyse.getDirection().getValue())
                && ((System.currentTimeMillis() - order.getCreateDate()) < TradeService.CHASE_DISABLE_TIME);
    }

    @Override
    public String toString() {
        return "Track{" +
                "access='" + access + '\'' +
                ", symbol=" + symbol +
                ", leverRate=" + leverRate +
                ", basisVolume=" + basisVolume +
                ", incomePricePlan=" + incomePricePlan +
//                ", isOpenAllow=" + isOpenAllow +
//                ", lastAnalyse=" + lastAnalyse +
//                ", lastOrderId='" + lastOrderId + '\'' +
//                ", lastOpenTime=" + lastOpenTime +
//                ", breakdownNum=" + breakdownNum +
//                ", lastCancelDirection=" + lastCancelDirection +
//                ", lastCancelTime=" + lastCancelTime +
                '}';
    }

}


