package com.trade.hedge.service;

import com.trade.analyse.model.trade.Track;
import com.trade.huobi.model.Result;
import com.trade.huobi.model.contract.Position;

import java.math.BigDecimal;

/**
 * 对冲服务
 * <p>〈功能详细描述〉</p>
 *
 * @author 陈晨
 * @version 1.0
 * @date 2020/9/24
 */
public interface HedgeService {
    String LOG_MARK = "对冲服务";

    /**
     * @description 持仓检查
     * <p>〈功能详细描述〉</p>
     *
     * @author 陈晨
     * @date 2020/9/24 15:05
     * @param track
     **/
    Result positionCheck(Track track);

    /**
     * @description 双向平仓检查
     * <p>〈功能详细描述〉</p>
     *
     * @author 陈晨
     * @date 2020/9/25 14:13
     * @param track, up, down
     **/
    void closeCheck(Track track, Position up, Position down);

    /**
     * @description 平仓检查
     * <p>〈功能详细描述〉</p>
     *
     * @author 陈晨
     * @date 2020/9/24 15:05
     * @param track, position, incomeMultiple, lossVolume
     **/
    void closeCheck(Track track, Position position, BigDecimal incomeMultiple, long lossVolume);

}


