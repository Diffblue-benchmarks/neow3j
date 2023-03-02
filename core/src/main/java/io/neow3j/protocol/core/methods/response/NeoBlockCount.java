package io.neow3j.protocol.core.methods.response;

import io.neow3j.protocol.core.Response;

import java.math.BigInteger;

public class NeoBlockCount extends Response<BigInteger> {

    public BigInteger getBlockIndex() {
        return getResult();
    }

}
