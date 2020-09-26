package com.trade.hedge.thread;

import com.trade.BaseService;
import com.trade.analyse.context.TradeContext;
import com.trade.analyse.model.trade.Track;
import com.trade.analyse.service.trade.TradeService;
import com.trade.hedge.service.HedgeService;
import com.trade.huobi.model.Result;
import com.trade.huobi.model.contract.Position;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * 对冲追踪
 * <p>
 *     cron: * * * * * * *
 *     cron: 秒 分 时 日 月 周 年
 * </p>
 *
 * @author 陈晨
 * @version 1.0
 * @date 2020/9/16
 */
@Component
public class HedgeTrackScheduler extends BaseService {

    @Autowired
    private HedgeService hedgeService;

    @Scheduled(cron = "0/5 * * * * ?")
    public void run() {
        for (Track track : TradeContext.getTrackList()) {
            if (track == null) {
                continue;
            }
            try {
                // 1, 持仓检查
                Result result = hedgeService.positionCheck(track);
                if (result.success()) {
                    Object[] positions = result.getData();
                    Position up = (Position) positions[0];
                    Position down = (Position) positions[1];
                    // 2, 双向平仓检查
                    hedgeService.closeCheck(track, up, down);
//                    logger.info("[对冲追踪] track={}, up={}, down={}, 双向平仓检查 End", track, up, down);
                } else {
                    logger.info("[对冲追踪] track={}, result={}, 持仓检查未通过, 无持仓信息", track, result);
                }
            } catch (Exception e) {
                logger.error("[对冲追踪] track={}, 异常, {}", track, e.getMessage(), e);
            }
        }
    }

}

