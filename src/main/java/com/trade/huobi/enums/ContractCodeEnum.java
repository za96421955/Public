package com.trade.huobi.enums;

/**
 * 交易对
 * <p>〈功能详细描述〉</p>
 *
 * @author 陈晨
 * @version 1.0
 * @date 2020/9/8
 */
public enum ContractCodeEnum {
    BTC("BTC-USD", "BTC")
    , ETH("ETH-USD", "ETH")
    , TRX("TRX-USD", "TRX")
    ;

    private final String value;
    private final String desc;

    ContractCodeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public String getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }

    public static ContractCodeEnum get(String value) {
        for (ContractCodeEnum e : values()) {
            if (e.getValue().equals(value)) {
                return e;
            }
        }
        return null;
    }

}


